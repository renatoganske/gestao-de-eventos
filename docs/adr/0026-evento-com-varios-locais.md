# ADR-0026: O evento tem vários locais, cada um com um papel opcional

**Data:** 2026-10-20
**Status:** Aceito

## Contexto

Desde o baseline (`V1`), `Event` tem um único `@ManyToOne EventVenue` (`tb_event.event_venue_id`). Os briefings dos casamentos importados do acervo mostram que isso não descreve o evento real: a cerimônia acontece numa igreja ("Capela da Catedral", "Noviciado Nossa Senhora de Fátima") e a festa em outro lugar ("Tênis Clube Joinville", "Quintal VIP"), às vezes com o making-of num terceiro. Na importação foi preciso escolher um só (ficou a recepção), e o outro local se perdeu nas notas.

Consequências práticas:

- **Busca por local incompleta:** procurar os eventos de uma igreja não acha os casamentos cuja festa foi em outro lugar, porque só a festa foi registrada.
- **Informação perdida:** o local que não coube no campo único não fica em lugar nenhum do sistema.

Renato pediu (GDE-51) que o evento aceite mais de um local.

## Decisão

**Entidade de associação `EventEventVenue`** (tabela `tb_event_event_venue`, chave composta `EventEventVenueId(eventId, eventVenueId)`, coluna `role varchar(255)` opcional), no mesmo formato de `EventProfessional` ([ADR-0002](0002-event-professional-entidade-associacao.md)) e `EventCustomer` ([ADR-0025](0025-evento-com-varios-clientes.md)). `Event.eventVenue` dá lugar a `Event.eventVenues` (`@OneToMany(mappedBy = "event")`, sem cascade); `EventVenue.events` passa a ser `EventVenue.eventEventVenues`, e o `EventVenueDto` continua devolvendo a lista de eventos do local.

**Papel em texto livre e opcional** ("Cerimônia", "Recepção", "Making-of", "Cerimônia e recepção"), pelo mesmo motivo do `roleInEvent` da equipe (ADR-0021): ninguém pediu filtro por papel, e um local sem papel continua sendo informação válida (é o caso de todos os eventos anteriores à migração).

**Escrita igual à de clientes e equipe (ADR-0021/ADR-0025):** `CreateEventDto.venues` (`List<EventVenueAssignmentDto(venueId, role)>`), embutido no evento, replace-all via repositório explícito com `flush()` entre o delete e o insert; `null` mantém os locais atuais, `[]` remove todos; local repetido no payload vira uma linha só, com o papel da última ocorrência; local inexistente = `404`; `deleteEvent` apaga as associações antes do evento.

**Leitura:** `EventDto.venues` (`EventVenueSummaryDto(venueId, venueName, role)`), **ordenado pelo nome do local**. O nome vai junto pelo mesmo motivo dos clientes (a listagem não precisa buscar todos os locais só para traduzir ids). `EventFilter.byVenue` casa se **qualquer** local do evento for o buscado; o parâmetro `venueId` da busca não muda.

**Migration `V8`:** cria a tabela, copia cada `tb_event.event_venue_id` não nulo como uma linha com `role = null` e remove a coluna (e a FK `fk_event_event_venue`). Validada no banco local antes do PR: os 45 pares evento + local de antes e depois da migração são idênticos.

## Racional

| Alternativa | Por que não |
|---|---|
| Manter um local e anotar os outros na descrição | É o problema descrito no contexto: o local anotado não entra na busca |
| Colunas fixas `ceremony_venue_id` / `reception_venue_id` | Engessa o modelo no casamento; não serve para making-of, ensaio em dois lugares ou festa sem cerimônia, e cada papel novo vira migration |
| `@ManyToMany` puro (`Set<EventVenue>`) | Não comporta o papel de cada local; o projeto já trocou `@ManyToMany` puro por associação pelo mesmo motivo (ADR-0002, ADR-0025) |
| Papel como tabela de lookup | Mais um cadastro para manter sem uso imediato (ninguém filtra por papel hoje); a ADR-0021 fez a mesma escolha para a equipe |
| **Associação com papel opcional em texto livre (escolhida)** | Uma tabela, padrão já usado duas vezes no código, busca por qualquer local resolvida |

**Ordem por nome, não por inclusão:** a associação não tem coluna de ordem e o banco não garante a ordem das linhas sem `ORDER BY`; ordenar no `toDTO()` deixa a resposta estável (inclusive nos testes) sem coluna nova. Se a ordem cronológica (making-of → cerimônia → festa) virar necessidade, entra como coluna `position` em `tb_event_event_venue`.

**Custos aceitos:**

- **Quebra de contrato da API:** `eventVenueId` sai de `CreateEventDto` e `EventDto`. Frontend e backend sobem juntos na mesma imagem; o único outro cliente é o script de importação dos HDs (fora do repositório), ajustado junto. Por isso o PR é `feat!`.
- **Eventos migrados ficam sem papel:** não há como saber se o local antigo era a cerimônia ou a festa. O papel pode ser preenchido depois pela tela ou pela reimportação dos briefings.
- **Excluir um local em uso continua dando `409`:** antes era a FK de `tb_event`, agora é a de `tb_event_event_venue`; o `DomainExceptionHandler` já trata as duas da mesma forma.
- **O mesmo local não aparece duas vezes no evento** (a chave é evento + local). Cerimônia e festa no mesmo lugar viram uma linha com papel "Cerimônia e recepção".

Ref.: GDE-51
