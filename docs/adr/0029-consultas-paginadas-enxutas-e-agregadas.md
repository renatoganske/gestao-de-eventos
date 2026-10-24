# ADR-0029: Busca de eventos paginada e enxuta, listas de referência sem eventos embutidos e endpoints agregados

**Data:** 2026-10-24
**Status:** Aceito (revisa a [ADR-0027](0027-busca-de-eventos-no-banco.md); mantém o modelo composável da [ADR-0005](0005-metricas-via-filtros-composaveis.md))

## Contexto

A ADR-0027 resolveu o N+1: a lista de eventos caiu de 1.813 para 40 queries. Mesmo assim, o Renato continua achando a aplicação "realmente muito lenta" em produção (Render + Neon, planos gratuitos). A proposta inicial dele era trocar o filtro composto por endpoints específicos.

O filtro composto não é o gargalo: ele já roda como uma única query com `EXISTS` no banco. O que sobrou é **volume de resposta** e **requisições desnecessárias**. Medição no banco local (605 eventos, 443 clientes, 105 profissionais, 38 locais, 9 HDs), em 2026-10-24:

| Requisição | Tamanho | Usado por |
|---|---|---|
| `GET /events/search` (sem filtro) | **473 KB** | lista de Eventos (carga inicial) |
| `GET /customers` | **296 KB** (70 KB sem a lista de eventos de cada cliente) | lista de Clientes, formulário de evento |
| `GET /hds` | **164 KB** (2 KB sem a lista de eventos de cada HD) | lista de HDs, filtro e coluna HD da lista de Eventos, formulário |
| `GET /professionals` | 62 KB (27 KB sem `eventProfessionals`) | lista de Profissionais, filtro, formulário |
| `GET /event-venues` | 23 KB (8 KB sem eventos) | lista de Locais, filtro, formulário |

- **A lista de Eventos baixa cerca de 730 KB para abrir:** a busca completa mais HDs, profissionais e locais para os filtros. O formulário de evento baixa uns 520 KB só para preencher selects.
- **O Dashboard faz 3 buscas completas** (eventos do mês, pendentes, futuros) só para usar `.length` e `.slice(0, 5)` no navegador.
- **A busca textual dispara 2 requisições** (`customerName` e `eventCode`) e junta o resultado no front.
- **Embutir eventos nas listas de referência é desperdício:** os `toResponseDto()` de `Hd`, `Customer`, `EventVenue` e `Professional` ainda serializam a lista de eventos de cada item. O frontend não lê esse campo desde o GDE-58 (`eventCount`). Era o item "fora desta decisão" da ADR-0027.

No Render free (fração de vCPU), serializar e transferir centenas de KB por tela custa caro. Cada ida e volta custa uns 250 ms medidos de fora (`401` sem token, servidor aquecido), então o número de requisições em série também pesa. As requisições autenticadas não foram medidas em produção, porque a senha não é guardada (ver memória `never-persist-credentials`). Os números acima são locais e servem para comparar antes e depois, não para estimar o tempo absoluto em produção.

## Decisão

**Manter o filtro composto** (`EventSpecifications`, ADR-0027) e reduzir o que cada tela pede.

**1. `GET /api/events/search` paginado, enxuto e ordenado no servidor**
- Aceita `page` (base 0), `size` (padrão 50, máximo 500) e `sort=<campo>,<asc|desc>`, além de todos os filtros atuais.
- Novo parâmetro `q`: casa o **código do evento OU o nome de qualquer cliente**, sem diferenciar maiúsculas. Substitui as duas requisições do front. `customerName` e `eventCode` continuam aceitos.
- **Resposta:** um `PageDto<EventListItemDto>` próprio (`content`, `page`, `size`, `totalElements`, `totalPages`). Não serializamos o `PageImpl` do Spring, cujo formato JSON não é estável entre versões.
- O `EventListItemDto` tem só o que a lista mostra: id, código, nome, data, tipo, status, id e nome do HD, clientes (contratante primeiro), locais com papel e as contagens de clientes e profissionais (para a mensagem de exclusão). A conversão fica na entidade (`Event.toListItemDto()`), como manda a convenção do projeto.
- **Ordenação por chave de coluna, nunca por caminho de propriedade:** aceitamos `code`, `customer`, `venues`, `type`, `date`, `hd` e `status`, que o service traduz para expressões JPA. Um campo desconhecido devolve `400`.
  - `customer` ordena pelo nome do cliente contratante.
  - `venues` ordena pelo primeiro local em ordem alfabética.
  - Os dois via subquery escalar, para não multiplicar linhas.
  - O desempate é sempre código e depois id, para a paginação ser estável. O padrão é `date,desc` (GDE-62).
- Continuamos com batch fetch (ADR-0027) e sem `JOIN FETCH` de coleção. Por isso a paginação acontece no banco: sem o aviso `HHH000104` (paginação em memória) e com número de queries constante por página.

**2. Listas de referência sem eventos embutidos**
- `HdDto`, `CustomerResponseDto` e `EventVenueDto` perdem `events`; `ProfessionalDto` perde `eventProfessionals`.
- A contagem continua em `eventCount` (ADR-0028). Quem precisa dos eventos de um item usa `/events/search?hdId=` (ou `customerId`, `venueId`, `professionalId`), que já existe.

**3. Endpoints agregados para telas que só precisam de pouco dado**
- `GET /api/dashboard/summary` → `{ eventsThisMonth, pendingDeliveries, hdsNearCapacity }`. Usa `count(Specification)` com os mesmos critérios de hoje e a regra de capacidade já existente.
- `GET /api/events/upcoming?limit=5` → próximos eventos (data ≥ hoje, crescente), no formato `EventListItemDto`.
- `GET /api/{customers,professionals,event-venues,hds}/options` → `[{id, name}]` por projeção JPQL, ordenado por nome, para selects e filtros.

## Racional

| Alternativa | Por que não |
|---|---|
| **Um endpoint por pergunta/tela, sem filtro composto** (proposta inicial) | Não ataca a causa (volume de resposta) e devolve o problema que a ADR-0005 resolveu: cada combinação nova de filtros vira endpoint novo. A lista de Eventos combina 10 filtros refletidos na URL (GDE-61) |
| Manter a lista completa e otimizar só no front (cache, memo) | O custo está em serializar e transferir no servidor. Cachear no navegador não diminui a primeira carga nem o que o Render precisa gerar |
| Cache no servidor (Redis / 2º nível) | Mais um componente para operar e invalidar a cada escrita, num sistema de 1 usuário. Esconde o volume em vez de reduzi-lo |
| Novo endpoint paginado mantendo `/search` como lista completa | Duas implementações da mesma busca. O único consumidor da lista completa fora do front são os scripts de importação, que ficam fora do repo e são ajustados junto |
| Serializar `Page<T>` do Spring direto | Formato instável entre versões (o Spring Data 3.3 avisa isso em log). Um DTO próprio de 5 campos é contrato nosso |
| Ordenar `customer` / `venues` por `JOIN` | Multiplica linhas (um evento com 2 locais aparece 2 vezes) e quebra a contagem da página. A subquery escalar mantém uma linha por evento |
| **Paginação + DTO enxuto + agregados, mantendo o filtro composto (escolhida)** | Ataca o volume onde ele nasce, mantém a liberdade de combinar filtros e muda pouco código: os mesmos `Specification`, o mesmo batch fetch |

**Custos aceitos:**

- **Quebra de contrato** em `/events/search` (lista → página) e nas listas de referência (sem `events`). Os consumidores são o frontend, ajustado no GDE-68, e os scripts em `C:\workspace\importacao-hds`. Atenção: `limpa_orfaos.py` usa `not c.get("events")` para achar clientes sem evento. Com o campo removido, essa condição vale para **todos** os clientes. O script precisa passar a usar `eventCount == 0` **antes** do deploy.
- **A ordenação passa a ser do servidor,** porque o front só tem a página atual. `customer` e `venues` usam a regra acima (contratante / primeiro local), que o Renato aceitou e pode diferir um pouco da ordem atual feita no navegador.
- **Os deploys precisam ser coordenados:** como o frontend é servido pelo próprio backend (`build:backend`), front e back saem no mesmo artefato. Basta mergear GDE-66, 67 e 68 juntos antes do próximo release.

**A medir depois do deploy:** com `HIBERNATE_STATISTICS=true` no Render (ADR-0027), o tempo das telas de Eventos, Dashboard e formulário. Se continuar lento com respostas pequenas, o próximo suspeito é a infraestrutura (cold start do Render free, região Render × Neon), não o código.

Ref.: GDE-65 (esta ADR), GDE-66 (busca paginada), GDE-67 (agregados e listas enxutas), GDE-68 (frontend)
