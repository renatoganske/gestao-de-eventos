# ADR-0022: `Professional.type` e `Professional.specialty` deixam de ser texto livre

**Data:** 2026-09-28
**Status:** Aceito

## Contexto

`Professional` tinha dois campos de categorização em texto livre: `type` (a função do profissional — "Decoradora", "Segundo fotógrafo") e `specialty` (o nicho em que atua — "Casamentos", "Eventos corporativos"). Ao testar o CRUD de profissionais, Renato percebeu que texto livre não permite filtro confiável: "cerimonialista de 15 anos" só apareceria numa busca por quem escreveu exatamente essa frase — variações de escrita ("Cerimonialista" vs "cerimonialista") ou um typo qualquer ficariam invisíveis.

`Professional` não tinha nenhum endpoint de busca até este momento (diferente de `Event`, que já tem `GET /events/search` via `EventFilter` composável, ADR-0005).

## Decisão

Os dois campos viram lookup, mas em **formatos diferentes**, porque têm cardinalidades diferentes:

- **`type`** (um profissional tem uma função) vira `@ManyToOne ProfessionalType` — mesmo padrão da ADR-0016 (`EventType`): tabela de lookup simples, CRUD completo (`/api/professional-types`), cadastrável sem deploy. A cardinalidade "no máximo um tipo" fica garantida pela própria coluna de FK, sem precisar de validação adicional.
- **`specialty`** (um profissional pode ter várias especialidades — confirmado por Renato) vira `@ManyToMany Set<SpecialtyTag>` via tabela de junção (`tb_professional_specialty`), com CRUD próprio pra `SpecialtyTag` (`/api/specialty-tags`). Sem entidade de associação: como não há atributo extra na relação (nenhum "desde quando", nenhum "nível"), o Hibernate gerencia o `@JoinTable` sozinho ao reatribuir o `Set` — nenhuma das armadilhas de `@EmbeddedId`+`record`+`@MapsId` da ADR-0021 se aplica aqui, porque essa complexidade só existe quando a relação carrega dado próprio.

Foi avaliada uma alternativa: uma única entidade `Tag` genérica com um campo `kind` (TYPE/SPECIALTY) compartilhada pelos dois usos. Descartada — economizaria código, mas trocaria a garantia estrutural de "um tipo só" (que vem de graça da FK) por uma regra de validação solta, e cardinalidades diferentes (1 vs N) são o sinal de que são duas relações diferentes, não uma reaproveitada.

**Busca:** `ProfessionalFilter` (mirror do `EventFilter`, mesmo padrão de `Predicate` composável da ADR-0005) com `byType`/`bySpecialtyTag`, expostos em `GET /api/professionals/search`.

**Delete protegido contra uso.** Apagar um `ProfessionalType`/`SpecialtyTag` em uso por algum profissional agora falha com `409` (`ResourceInUseException`, novo, mapeado no `DomainExceptionHandler`) em vez de estourar violação de FK como 500. O mesmo bug existia em `EventType.deleteEventType` desde a ADR-0016 e nunca havia sido notado — corrigido junto, com a mesma exceção compartilhada.

**Migration `V4` com backfill.** Diferente da suposição inicial (nenhum dado real a preservar, baseada só no banco local), a migration faz backfill de verdade: cada valor distinto de `type`/`specialty` já em uso vira uma linha de lookup antes das colunas de texto serem dropadas — não dá pra assumir que o banco de produção (Neon) está vazio só porque o local está.

## Racional

A escolha de formatos diferentes para `type` e `specialty` prioriza deixar o modelo de dados expressar a regra de negócio (cardinalidade) em vez de empurrar isso para validação em código — é mais barato e mais difícil de esquecer de checar. O custo aceito é dobrar o volume de código de lookup (duas entidades+CRUDs em vez de uma só), mas cada metade replica um padrão já existente (`EventType` para `type`, e o próprio desenho novo de `specialty` não introduz nada que a ADR-0021 não tenha já mapeado como território simples — a ausência de atributo extra é exatamente o que evita a complexidade que lá foi necessária).

A correção do `EventType` foi bundled nesta mesma decisão porque é a mesma classe de bug (delete sem checar uso, atenuado só pela tabela estar vazia) e usa exatamente a mesma exceção nova — resolver os dois de uma vez evita deixar metade do problema consertado.

Ref.: `docs/adr/0016-event-type-tabela-lookup.md`, `docs/adr/0021-escrita-associacao-event-professional.md`, `docs/adr/0005-metricas-via-filtros-composaveis.md`.
