# ADR-0011: DTOs de resposta nunca embutem entidade JPA diretamente

**Data:** 2026-09-23
**Status:** Aceito

## Contexto

Achado em três revisões independentes (PRs #8, #9, #10 — GDE-10, GDE-11, GDE-6, 2026-09-22): vários DTOs de resposta carregam a entidade JPA relacionada crua em vez de uma representação leve:

* `CustomerResponseDto.events: List<Event>`
* `EventVenueDto.events: List<Event>`
* `HdDto.events: List<Event>`
* `ProfessionalDto.eventProfessionals: List<EventProfessional>` (que por sua vez carrega `Event`/`Professional` inteiros)
* `EventDto.eventProfessionals: List<EventProfessional>`
* `EventDto.hd: Hd`, `EventDto.eventVenue: EventVenue`, `EventDto.customer: Customer`

Nenhuma entidade tem `@JsonManagedReference`/`@JsonBackReference`/`@JsonIdentityInfo` para cortar ciclo, e o projeto não usa `jackson-datatype-hibernate5` (guardrail #8 da spec: sem dependência nova sem necessidade concreta). Como `Event`, `EventVenue` e `Hd` já têm controller (`GDE-8`, `GDE-11`, `GDE-9`, todos mergeados), qualquer `GET` que retorne uma dessas entidades com o relacionamento bidirecional preenchido serializa em loop (ex.: `Event → Hd → events → Event → Hd → ...`) e estoura `StackOverflowError`. Isso já está ativo em produção, não é um risco futuro.

## Decisão

Nenhum DTO de resposta expõe uma entidade JPA (nem coleção de entidades) diretamente. Dois casos, duas representações:

1. **Coleção (`@OneToMany`)** — vira `List<XSummaryDto>`, um DTO leve com só campos escalares (sem nenhum relacionamento aninhado). Isso quebra qualquer ciclo por construção, porque um Summary DTO literalmente não tem como referenciar de volta a entidade que o contém.
   * `EventSummaryDto(id, eventCode, type, name, eventDate, deliveryStatus)` — usado em `CustomerResponseDto.events`, `EventVenueDto.events`, `HdDto.events`.
   * `EventProfessionalSummaryDto(eventId, eventCode, eventName, professionalId, professionalName, roleInEvent)` — usado em `ProfessionalDto.eventProfessionals`, `EventDto.eventProfessionals`.
2. **Referência singular (`@ManyToOne`)** — vira apenas o `UUID` do id referenciado (`EventDto.hdId`, `EventDto.eventVenueId`, `EventDto.customerId`), não um DTO aninhado. Quem já está navegando a partir de um evento específico consegue buscar o recurso relacionado completo pelo próprio endpoint dele (`GET /api/hds/{id}` etc.); criar mais um tipo de DTO só para embutir um único registro não paga o custo de manutenção.

## Racional

Um Summary DTO com só escalares garante ausência de ciclo sem precisar de anotação Jackson de corte de ciclo nem de biblioteca adicional (`jackson-datatype-hibernate5`), e ainda entrega contexto útil (nome, data) para quem está navegando uma lista — evitar isso e usar `List<UUID>` puro obrigaria round-trips extras para cada item.

Para referência singular, ida por `id` é suficiente: o consumidor já sabe o contexto (está olhando um evento específico) e o ganho de embutir nome/endereço não paga o custo de mais um tipo `*SummaryDto` por relação `@ManyToOne`.

Implementação: task **GDE-19**. Corrigidos os 4 DTOs citados na revisão mais `HdDto.events`, que tem o mesmo problema mas não foi citado explicitamente — mesmo bug, mesmo endpoint já em produção (`GET /api/hds/{id}`), mesma decisão de design aplicada.

Ref.: `docs/spec-gestao-de-eventos.md` §6 (guardrail 7 — sem MapStruct/ModelMapper — e guardrail 8 — sem dependência nova sem necessidade concreta)
