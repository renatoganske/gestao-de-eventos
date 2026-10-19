# ADR-0025: O evento tem vários clientes (uma pessoa por cadastro), um deles marcado como contratante

**Data:** 2026-10-19
**Status:** Aceito

## Contexto

Desde o baseline (`V1`), `Event` tem um único `@ManyToOne Customer` (`tb_event.customer_id`). Na importação do acervo dos HDs (centenas de eventos de 2010 a 2025), a realidade não coube nisso: casamento tem duas pessoas, aniversário infantil tem pais, e um mesmo pai aparece sozinho em outro evento. A saída foi cadastrar casais e pais como **um** cliente com os dois nomes ("Lidiane Lacerda e Caio Auada", "Sandra Bonacci e Jair Bonacci").

Isso prejudica justamente o uso que motivou o acervo:

- **Busca:** procurar "Jair Bonacci" só acha o evento se o texto combinado contiver exatamente esse trecho; a mesma pessoa vira cadastros diferentes conforme a combinação ("Rafael e Rafaela Schroeder", "Rafaela Schroeder").
- **Histórico por pessoa:** não há como listar todos os eventos de alguém que aparece ora sozinho, ora em casal, ora como pai.
- **Quem pagou:** em aniversário infantil, quem aparece (a criança, os pais) e quem contratou são informações diferentes, e o modelo só guardava uma.

Renato decidiu (GDE-49): cada pessoa tem o próprio cadastro; o evento associa várias; uma delas é marcada como contratante. Empresas e instituições (Colégio Siloé, Nissan Barigui CWB) continuam como um cadastro só.

## Decisão

**Entidade de associação `EventCustomer`** (tabela `tb_event_customer`, chave composta `EventCustomerId(eventId, customerId)`, coluna `contracting boolean not null`), no mesmo formato de `EventProfessional` ([ADR-0002](0002-event-professional-entidade-associacao.md)). `Event.customer` dá lugar a `Event.eventCustomers` (`@OneToMany(mappedBy = "event")`, sem cascade); `Customer.events` passa a ser `Customer.eventCustomers`, e o `CustomerResponseDto` continua devolvendo a lista de eventos do cliente.

**Escrita igual à da equipe ([ADR-0021](0021-escrita-associacao-event-professional.md)):** `CreateEventDto.customers` (`List<EventCustomerAssignmentDto(customerId, contracting)>`), embutido no evento, replace-all via repositório explícito com `flush()` entre o delete e o insert; `null` mantém os clientes atuais, `[]` remove todos; cliente repetido no payload vira uma linha só (contratante se qualquer ocorrência for); `deleteEvent` apaga as associações antes do evento.

**No máximo um contratante, em duas camadas:** validação no `CreateEventDto` (`@AssertTrue`, vira `400` com mensagem) e índice único parcial no banco (`create unique index ... on tb_event_customer (event_id) where contracting`), para que nenhum caminho de escrita fora do serviço consiga gravar dois.

**Leitura:** `EventDto.customers` (`EventCustomerSummaryDto(customerId, customerName, contracting)`), com o contratante primeiro. O nome vai junto para a listagem de eventos não precisar buscar todos os clientes só para traduzir ids. `EventFilter.byCustomerName` casa se **qualquer** cliente do evento tiver o nome buscado.

**Migration `V7`:** cria a tabela, copia cada `tb_event.customer_id` não nulo como uma linha `contracting = true` e remove a coluna. Separar os cadastros combinados existentes ("Fulano e Ciclano") **não** é feito pela migration (ver custos).

## Racional

| Alternativa | Por que não |
|---|---|
| Manter um cliente por evento e cadastrar casais combinados | É o problema descrito no contexto |
| `@ManyToMany` puro (`Set<Customer>`) | Não comporta o atributo "contratante"; o projeto já trocou um `@ManyToMany` puro por entidade de associação pelo mesmo motivo (ADR-0002) |
| Manter `customer_id` como contratante + `@ManyToMany` para os demais | Duas fontes para a mesma informação ("clientes do evento"), com regra de sincronização entre elas e busca que precisa olhar os dois lugares |
| Entidade "Família/Grupo" que agrupa pessoas | Mais um conceito para cadastrar e manter; o agrupamento real já é o evento |
| **Associação com flag de contratante (escolhida)** | Uma tabela, padrão já conhecido no código, busca e histórico por pessoa resolvidos |

**Custos aceitos:**

- **Quebra de contrato da API:** `customerId` sai de `CreateEventDto` e `EventDto`. Frontend e backend sobem juntos na mesma imagem; o único outro cliente é o script de importação dos HDs, ajustado junto. Por isso o PR é `feat!`.
- **Cadastros combinados continuam combinados até serem separados à mão:** separar "X e Y" automaticamente erraria em instituições ("Colégio Siloé"), em anotações ("Ana (mãe do Matheus)") e em sobrenomes compartilhados. Depois do merge, a separação é feita por lista revisada pelo Renato e aplicada pela API.
- **Evento sem contratante é permitido:** muitos eventos importados não têm essa informação; exigir um contratante bloquearia o cadastro.
- **Papel por cliente** (noivo, mãe, aniversariante) fica fora: só "contratante" foi pedido. Se vier, entra como coluna nova em `tb_event_customer`.

Ref.: GDE-49
