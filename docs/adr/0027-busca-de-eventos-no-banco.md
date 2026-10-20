# ADR-0027: A busca de eventos filtra no banco e as associações são carregadas em lote

**Data:** 2026-10-20
**Status:** Aceito (revisa a [ADR-0005](0005-metricas-via-filtros-composaveis.md))

## Contexto

Com o import dos HDs a base local chegou a 599 eventos, e as telas ficaram lentas em produção (Render + Neon, planos gratuitos). A medição com as estatísticas do Hibernate, feita no banco local com a base importada, mostrou que o problema é o número de queries por requisição, não o volume de dados:

| Requisição | Queries | Tempo local |
|---|---|---|
| `GET /events/search` (lista inicial da tela Eventos, 599 eventos) | 1.813 | 1,4 s |
| `GET /events/search?customerName=a` (busca por texto) | 1.655 | 1,4 s |
| `GET /professionals` (dropdown, 105 profissionais) | 220 | 0,23 s |

Duas causas se somavam:

- **N+1:** `Event.toDTO()` percorre três coleções lazy (`eventVenues`, `eventCustomers`, `eventProfessionals`), cada uma carregada com uma query própria por evento, e os to-ones eager (`type`, `hd`, `venue`, `customer`, `professional`) geravam selects avulsos. Os `toResponseDto()` de locais, profissionais e HDs repetem o padrão com a lista de eventos de cada item.
- **Filtro em memória:** a ADR-0005 implementou os filtros composáveis como `Predicate<Event>` (`EventFilter`), aplicados depois de um `findAll()` da base inteira. Era razoável com poucos eventos; com centenas, todo filtro que olha uma coleção (cliente, local, profissional) força a carga dessa coleção para **todos** os eventos.

Localmente cada query custa menos de 1 ms. Em produção cada uma paga a ida e volta entre Render e Neon, então o tempo cresce com o **número** de queries. É isso que transforma 1.813 queries numa espera de dezenas de segundos.

## Decisão

**1. Carga em lote das associações:** `spring.jpa.properties.hibernate.default_batch_fetch_size=50`, em todos os ambientes. As coleções lazy e os to-ones passam a ser carregados em lotes (`WHERE id IN (...)`, até 50 por query) em vez de um por entidade. Não muda código nem contrato e vale para todas as entidades, inclusive as listas de referência.

**2. Filtros como `Specification<Event>` (`EventSpecifications`), executados pelo banco.** O contrato da ADR-0005 continua: cada critério é um factory method, a composição é livre (`Specification.allOf(...)`), um critério vazio devolve `null` e não restringe nada, e o endpoint `GET /events/search` não muda. O que muda é **onde** o filtro roda. O `EventFilter` em memória foi removido, para não haver duas implementações da mesma regra.

- Os filtros por coleção (`byVenue`, `byProfessional`, `byCustomerName`) usam `EXISTS` sobre a tabela de associação, não `JOIN`. Um evento com dois clientes que casam aparece uma vez só, sem `DISTINCT`, e os filtros se combinam sem multiplicar linhas.
- Os filtros de texto (`byCustomerName`, `byEventCode`) usam `lower(...) LIKE` com escape de `%`, `_` e `\`, para manter o comportamento do `contains` anterior, que tratava esses caracteres como literais.
- `byPeriod` exclui eventos sem data quando qualquer limite é informado. As flags de casamento só casam com `true`/`false` explícitos, porque a igualdade do SQL já descarta `null`. Os dois comportamentos são iguais aos de antes.

**3. Leituras do `EventService` com `@Transactional(readOnly = true)`** (`getAllEvents`, `getEventById`, `searchEvents`). Antes, o lazy loading do `toDTO()` só funcionava porque o open-in-view mantinha a sessão aberta até a resposta. Agora a sessão é explícita no service. O import passou de `jakarta.transaction.Transactional` para o do Spring, que tem `readOnly`; as escritas mantêm a mesma semântica de rollback.

**4. Estatísticas do Hibernate controladas por variável de ambiente:** `HIBERNATE_STATISTICS` (padrão `true` em dev e `false` em prod). Em produção liga e desliga pelo painel do Render, sem deploy, para medir com a latência real. O placeholder `${...}` dá à variável um nome curto: sem ele o override teria que ser `SPRING_JPA_PROPERTIES_HIBERNATE_GENERATE_STATISTICS`, em que o relaxed binding não distingue o `_` de `generate_statistics` do separador entre níveis. As chaves escritas no próprio `.properties`, como `default_batch_fetch_size`, mantêm o `_` e não precisam de placeholder.

### Resultado (banco local, mesma base)

| Requisição | Antes | Depois |
|---|---|---|
| `GET /events/search` (599 eventos) | 1.813 queries / 1,40 s | 40 queries / 0,08 s |
| `GET /events/search?customerName=a` | 1.655 / 1,44 s | 37 / 0,08 s |
| `GET /events/search?eventCode=20` | 61 / 0,07 s | 7 / 0,01 s |
| `GET /events/search?deliveryStatus=PENDING` (dashboard) | 43 / 0,10 s | 7 / 0,01 s |
| `GET /professionals` | 220 / 0,23 s | 9 / 0,02 s |
| `GET /event-venues` | 40 / 0,05 s | 3 / 0,01 s |

Para confirmar a equivalência, as duas implementações rodaram lado a lado sobre a base importada em 186 buscas (cada tipo, local, profissional e HD existente, status, flags, períodos, textos com `%`/`_`/acentos e combinações). Os eventos devolvidos e o corpo de cada um foram idênticos em todas; a única diferença era a ordem da equipe, que nunca teve ordem definida e agora é ordenada por nome no `toDTO()`, como locais e clientes.

## Racional

| Alternativa | Por que não |
|---|---|
| `JOIN FETCH` / `@EntityGraph` nas três coleções | As três são `List` (bags): buscar duas ou mais juntas lança `MultipleBagFetchException`. Trocar para `Set` evita o erro, mas gera produto cartesiano (locais × clientes × equipe por evento) |
| Projeções DTO direto no JPQL | Seria o mais rápido, mas abandona a conversão `toDTO()` na entidade (convenção do projeto) e duplica o mapeamento para cada tela. Não se justifica no volume atual |
| Manter `EventFilter` em memória e só fazer batch fetch | Resolve o N+1, mas cada busca continua lendo a base inteira, e o custo cresce com cada evento importado |
| Cache (Redis ou de segundo nível) | Esconde o N+1 em vez de resolver e acrescenta um componente para manter e invalidar |
| Paginação | Não é necessária com o filtro no banco e centenas de eventos. Fica para quando a base chegar a milhares |
| **Batch fetch + `Specification` + leitura transacional (escolhida)** | Uma linha de configuração resolve o N+1 em todo o sistema; a `Specification` mantém exatamente o modelo composável da ADR-0005 e só lê os eventos que casam |

**Custos aceitos:**

- **As regras dos filtros só são testáveis contra o banco.** O `EventFilterTest` (unitário) deu lugar ao `EventSpecificationsIntegrationTest`, que roda no Postgres local junto com os dados importados. Por isso as asserções verificam se cada fixture está ou não no resultado, nunca a lista exata.
- **`lower(...) LIKE '%x%'` não usa índice.** Com centenas de eventos isso é desprezível. Se a base crescer muito, a saída é um índice trigram (`pg_trgm`), decidido em ADR própria.

**Fora desta decisão** (candidatos a cards próprios): `ProfessionalFilter` ainda filtra em memória; os `toResponseDto()` de locais, profissionais e HDs ainda serializam a lista de eventos de cada item só para preencher dropdowns; os demais services continuam dependendo do open-in-view para o lazy loading.

Ref.: GDE-52
