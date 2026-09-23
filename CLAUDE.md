# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 3.3.0 / Java 21 backend for managing photography event bookings ("gestao de trabalhos de fotografos"): customers, events, event venues, HDs (storage drives), and professionals. The Maven project root is the `gestao-de-eventos/` subdirectory (not the repo root). All commands below assume that directory as the working directory.

## Commands

Run from `gestao-de-eventos/`:

- Build: `./mvnw clean install` (`mvnw.cmd` on native Windows shells)
- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=GestaoDeEventosApplicationTests`

The app requires a running PostgreSQL instance matching `src/main/resources/application.properties` (`jdbc:postgresql://localhost:5432/gestaodeeventosdb`, user `postgres`). `spring.jpa.hibernate.ddl-auto=update` — schema is generated/updated from entities, no migration tool is in use. There's a commented-out property to disable the datasource auto-configuration for running without a DB.

**Build with JDK 21, not whatever `java` resolves to by default.** This machine's default `JAVA_HOME`/`PATH` point at JDK 25, which silently breaks Lombok 1.18.32 (the version Spring Boot 3.3.0 pins) — annotation processing no-ops with no warning, so `@Builder`/`@Getter`/`@Setter`/`@RequiredArgsConstructor` never generate, and compilation fails with confusing "cannot find symbol: builder()/setX()" errors. Point `JAVA_HOME` at a JDK 21 install (e.g. `C:\Program Files\Java\jdk-21` on this machine) before running any `mvnw` command.

Swagger/OpenAPI UI is served at `/swagger-ui.html` (raw docs at `/api-docs`) once the app is running.

## Architecture

**Controller split into interface + impl.** Each REST resource has an `I<Name>Controller` interface in `controllers/` carrying `@RequestMapping`, `@Validated`, and all Swagger/OpenAPI annotations (`@Tag`, `@Operation`), and a `@Component` implementation in `controllers/impl/` that implements the interface and only delegates to a service. When adding a new endpoint, add the route + Swagger docs on the interface method, and the actual logic call in the impl class.

**Layering:** Controller (interface + impl) → Service → Repository (Spring Data JPA) → Entity. Services are `@Service @RequiredArgsConstructor`, use constructor-injected repositories, and mark write operations `@Transactional`. Not-found cases currently throw plain `RuntimeException` — there is no global `@ControllerAdvice`/exception handler yet, so new code should follow the existing pattern unless asked to introduce one.

**DTO conversion lives on the entity/DTO, not in a mapper class.** Entities expose a `toResponseDto()` (or `toDTO()`) method that builds the response DTO; "create" DTOs (records) expose a `toEntity()` method that builds the entity via its Lombok `@Builder`. There is no MapStruct/ModelMapper — follow this manual conversion convention for new entities.

**Naming: English is the standard going forward; the PT→EN migration is being finished, not left incomplete.** Decided 2026-09-22 (ADR in `docs/spec-gestao-de-eventos.md` §9). Entity classes and DTOs are already in English (`Customer`, `Event`, `EventVenue`, `Professional`), but `ClienteRepository`/`ClienteController`/`ClienteService` still operate on the `Customer` entity, `EventoRepository` operates on `Event`, `LocalDoEventoRepository` operates on `EventVenue`, and DB table names (`@Entity(name = "TB_CLIENTE")`, `TB_EVENTO`, etc.) and `@Column` names remain in Portuguese even where the Java field is now English (e.g. `EventVenue.name` maps to column `nome`). Finishing this (repository/service/controller renames, DB table/column renames) is its own dedicated task (GDE-15) — don't do it as a side effect of unrelated work. New fields/code added before GDE-15 lands should already be named in English rather than adding to the Portuguese debt.

**Only the Customer (`Cliente`) resource has a full controller/service.** `Event`, `Hd`, `Professional`, and `EventVenue` currently have entities, DTOs, and repositories, but no controller or service yet — use the `Cliente` stack (`IClienteController` / `ClienteController` / `ClienteService`) as the template when building these out.

**Entity relationships:** `Event` is the central entity — `@ManyToOne` to `Customer`, `Hd`, and `EventVenue`, and `@ManyToMany` to `Professional` (join table `evento_profissional`). All entity IDs are `UUID` with `GenerationType.AUTO`.

## Development workflow

Task tracking lives on Jira, board **"Gestão de Eventos"** (key `GDE`). An `atlassian` MCP server is connected (site `renatoganskejr.atlassian.net`) — create/edit/transition cards directly via the `mcp__atlassian__*` tools rather than drafting them as text. Issue type is `Tarefa`; every card needs a user story ("Como… quero… para que…"), technical context, and an acceptance-criteria checklist — not a terse technical description.

For every task pulled from the board:

1. **Branch from `develop`** using a semantic prefix matching the change type: `feat/`, `fix/`, `chore/`, `refactor/`, `test/`, `docs/` (e.g. `feat/event-crud`, `fix/application-properties-encoding`).
2. **Write unit tests** covering the change — no task is done without tests.
3. **Run the full test suite** (`./mvnw test`) before opening a PR; if anything is broken (by this change or pre-existing), fix it as part of the task.
4. **Open a PR** and stop — wait for Renato's review, approval, and merge. Never merge your own PR.
5. Only start the next task once Renato says the previous one was merged and tells you to pull the next one. Don't chain tasks autonomously.
6. Any decision that changes architecture (new dependency/tool, schema-management approach, cross-cutting convention) gets its own ADR document in `docs/adr/` (`NNNN-slug.md`, one file per decision: Data/Status/Contexto/Decisão/Racional), written through the `software-architect` skill's lens — don't just fold it into other docs in passing.

**Parallel agents in isolated worktrees.** When multiple tasks run in parallel via `isolation: "worktree"`, the worktree has repeatedly (not once) been provisioned at the repository's very first commit instead of the current branch tip — a harness-level quirk, reported upstream, not something this repo can fix. Before doing anything else in an isolated worktree — before writing a file, before running a build — check `git log --oneline -3`: if it shows only `Initial commit` instead of recent project history, the worktree is on the wrong base. Fix it first: `git fetch origin && git checkout -b <branch-name> origin/develop`. Skipping this check wastes a full implementation cycle discovering the same problem the hard way.

## Scope & approval

Renato likes hearing new ideas, but new scope always needs his sign-off before it's built. Concretely:

- New features, entities/fields, schema changes, dependencies, or endpoints beyond what was explicitly asked for must be **proposed and approved before implementation** — never add them silently as a side effect of another task, however small or "obviously good" they seem.
- If you spot an opportunity, gap, or improvement while working, say so and wait for confirmation instead of building it.
- This applies to `docs/prd.md` and `docs/spec-gestao-de-eventos.md` too — don't write new scope into those docs unprompted; propose it first.
