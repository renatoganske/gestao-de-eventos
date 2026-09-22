# ADR-0007: Inglês como idioma padrão; finalizar migração PT→EN

**Data:** 2026-09-22
**Status:** Aceito (em andamento)

## Contexto

Entidades e DTOs já foram renomeados para inglês (`Customer`, `Event`, `EventVenue`, `Professional`), mas `ClienteRepository`/`ClienteController`/`ClienteService` ainda operam sobre a entidade `Customer`, `EventoRepository` sobre `Event`, `LocalDoEventoRepository` sobre `EventVenue`, e os nomes de tabela/coluna no banco (`TB_CLIENTE`, `TB_EVENTO`, etc.) permanecem em português mesmo onde o campo Java já é inglês.

## Decisão

Inglês é o idioma padrão da aplicação a partir de agora. A migração PT→EN existente será finalizada — não fica mais incompleta por tempo indeterminado.

Falta: `ClienteRepository`→`CustomerRepository`, `EventoRepository`→`EventRepository`, `LocalDoEventoRepository`→`EventVenueRepository`, `ProfissionalRepository`→`ProfessionalRepository`, `ClienteService`→`CustomerService`, `IClienteController`→`ICustomerController`, `ClienteController`→`CustomerController`, `ClienteResponseDto`→`CustomerResponseDto`, e os nomes de tabela/coluna no banco.

## Racional

Pedido explícito do Renato — repositories/services/controllers e nomes de tabela/coluna ainda em português divergem do próprio código novo (que já nasce em inglês). Não fazer rename em massa como efeito colateral de tarefa não relacionada — é a task própria e explícita **GDE-15**.

Ref.: `CLAUDE.md`, `docs/spec-gestao-de-eventos.md` §4, §6.1
