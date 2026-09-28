# ADR-0021: Escrita da associação `Event`↔`Professional` — embutida no evento, replace-all, sem cascade JPA

**Data:** 2026-09-28
**Status:** Aceito

## Contexto

A ADR-0002 já havia trocado o `@ManyToMany` puro entre `Event` e `Professional` por uma entidade de associação própria (`EventProfessional`, `TB_EVENT_PROFESSIONAL`, chave composta `EventProfessionalId`, campo `roleInEvent`). Isso deu o lado de **leitura**: `Event.eventProfessionals` (`@OneToMany(mappedBy = "event")`, sem cascade), `EventDto`/`ProfessionalDto` expondo `EventProfessionalSummaryDto`, e `EventFilter.byProfessional` já em uso na busca composta (ADR-0005).

O que nunca existiu é o lado de **escrita**: `CreateEventDto` não tinha campo para profissionais, não havia `EventProfessionalRepository`, e `EventService` nunca criava, atualizava ou apagava uma linha de `EventProfessional`. `TB_EVENT_PROFESSIONAL` estava — e sempre esteve — vazia. Renato pediu para poder informar, ao criar ou editar um evento, quais profissionais atuaram e com que papel, com o objetivo declarado de extrair métricas de negócio sobre isso no futuro (ex.: quais profissionais atuam mais, em que tipo de evento).

Fechar essa lacuna expôs duas armadilhas que não existiam enquanto a tabela estava vazia:

1. **`deleteEvent` quebraria por violação de FK.** `EventService.deleteEvent` chama `eventRepository.delete(event)` direto; sem cascade no `@OneToMany` e sem `ON DELETE CASCADE` na FK (`V1__baseline.sql`), apagar um evento com equipe cadastrada estoura `DataIntegrityViolationException`. Nunca acontecia porque nunca havia linha para violar a FK — essa feature é exatamente o que ativa o bug.
2. **`EventProfessionalId` é um `record`, o que impede `@MapsId` de funcionar como seria natural.** `@MapsId` escreve o id derivado da associação nos campos da entidade; um `record` é imutável, então o Hibernate não tem como preencher os componentes da chave automaticamente. A chave composta precisa ser construída explicitamente (`new EventProfessionalId(eventId, professionalId)`) depois que o evento já foi salvo e tem id — e isso não aparece em teste com repositório mockado, só em um teste que efetivamente faz flush contra o banco.

## Decisão

**Forma de escrita: embutida em `CreateEventDto`, não sub-recurso dedicado.** `CreateEventDto` ganha `List<EventProfessionalAssignmentDto> professionals` (cada item: `professionalId` obrigatório + `roleInEvent` texto livre opcional). A alternativa de um sub-recurso (`POST/DELETE /api/events/{id}/professionals`) foi descartada: a UI é um único formulário com um botão "Salvar evento", a lista tem poucos itens, e a alternativa fragmentaria a transação — se a chamada de associação falhasse separada da de criação do evento, um evento sem equipe ficaria indistinguível de um evento cuja gravação de equipe falhou, contaminando exatamente o dado que a métrica futura depende.

**Semântica no update: `null` deixa a equipe como está, `[]` esvazia.** Como `CreateEventDto` serve tanto `create` quanto `update`, um cliente que não conhece o campo — o próprio Swagger UI, por exemplo — não pode apagar a equipe por omissão. Isso torna a semântica do `PUT` sobre esse campo específico não-uniforme com o resto do DTO (que é substituição pura), mas evitar perda de dado silenciosa pesou mais.

**Sincronização: replace-all via repositório explícito, não `cascade`/`orphanRemoval` no `@OneToMany`.** Cada `createEvent`/`updateEvent` apaga (`EventProfessionalRepository.deleteByEvent_Id`) e recria as linhas a partir da lista submetida, com um `flush()` explícito entre as duas operações — sem isso, o Hibernate pode reordenar o insert antes do delete na mesma transação e violar a PK que está sendo reaproveitada. Cascade em `@OneToMany` com `@EmbeddedId` de `record` + `@MapsId` é onde moram as piores armadilhas do Hibernate (merge de entidade destacada, orphan removal que não dispara quando a coleção é substituída em vez de mutada); repositório explícito é o padrão que o resto do `EventService` já usa (`resolveHd`/`resolveEventVenue`/`resolveCustomer`) e mantém a escrita testável e óbvia. Replace-all (em vez de diff incremental) é suficiente porque a lista é pequena (1–5 pessoas) e a tabela de associação não tem coluna de auditoria — se um dia ganhar `created_at`, esta decisão precisa ser revisitada, porque replace-all destruiria esse histórico.

**Profissional duplicado no payload: mantém o último papel.** A PK é `(event_id, professional_id)`, então o mesmo profissional não pode ter dois papéis no mesmo evento. Em vez de deixar isso estourar como violação de constraint (500), o serviço deduplica mantendo a última ocorrência — uma escolha deliberada de tornar um payload duplicado em comportamento definido, não em erro de infraestrutura vazando pra API.

**`deleteEvent` apaga a associação antes do evento.** Alternativa seria `ON DELETE CASCADE` na FK via migration, o que seria mais robusto contra qualquer caminho de escrita futuro que não passe pelo `EventService`, mas esconderia o comportamento na camada de banco em vez de no código que já é dono dessa regra. Ficou explícito no serviço, junto do `flush()` que a mesma armadilha de ordenação exige.

**`roleInEvent` continua texto livre e opcional — decisão explícita de Renato, contra a recomendação inicial.** Foi avaliada uma tabela de lookup (`ProfessionalRole`, mesmo padrão da ADR-0016) para preservar a agregação por papel que a métrica futura precisa; o argumento a favor era que o momento mais barato para normalizar é agora, com `TB_EVENT_PROFESSIONAL` vazia. Renato optou por manter texto livre e sem obrigatoriedade, aceitando conscientemente que "Fotógrafo", "fotografo" e "2º fotógrafo" virem grupos distintos numa eventual métrica por papel — dívida técnica registrada abaixo, não desconhecida.

**Sem migration nova.** `TB_EVENT_PROFESSIONAL` já existe desde `V1__baseline.sql`, com a PK composta e `role_in_event` nullable — a entidade já validava contra ela.

## Racional

A tensão central era atomicidade: a mesma razão que já levou a preferir DTOs embutidos para `hdId`/`eventVenueId`/`customerId` em vez de sub-recursos (evitar um evento em estado parcial) se aplica com mais força aqui, porque o dado que está sendo protegido é exatamente o que Renato quer analisar depois. Um mecanismo de escrita mais simples (cascade automático do JPA) foi descartado não por purismo arquitetural, mas porque a combinação específica deste projeto — `@EmbeddedId` como `record` + `@MapsId` — torna esse cascade não-trivial e não coberto por teste com mock; o caminho explícito é mais código, mas é o único que um teste de integração real consegue provar que funciona.

## Débito técnico registrado

`roleInEvent` como texto livre impede agregação confiável por papel sem normalização posterior (matching fuzzy em dado já sujo). Decisão consciente de Renato em 2026-09-28, revisitável se a métrica por papel virar prioridade.

Ref.: `docs/adr/0002-event-professional-entidade-associacao.md`, `docs/adr/0005-metricas-via-filtros-composaveis.md`, `docs/adr/0011-response-dtos-sem-entidade-jpa-direta.md`, `docs/adr/0016-event-type-tabela-lookup.md`.
