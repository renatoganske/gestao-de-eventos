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

Config is split by Spring profile (ADR-0014): `application.properties` holds settings shared across environments, `application-dev.properties` (active by default) has the local Postgres connection (`jdbc:postgresql://localhost:5432/gestaodeeventosdb`, user `postgres`), and `application-prod.properties` reads `DATASOURCE_URL`/`DATASOURCE_USERNAME`/`DATASOURCE_PASSWORD` from the environment instead. `spring.jpa.hibernate.ddl-auto=validate` — Flyway owns the schema (see `db/migration/`), Hibernate only checks entities match it. There's a commented-out property in `application-dev.properties` to disable the datasource auto-configuration for running without a DB.

A `docker-compose.yml` at the repo root brings up a matching Postgres instance (`docker compose up -d`) — this is the standard way to get a local DB running, rather than installing Postgres manually. `gestao-de-eventos/Dockerfile` (ADR-0015) builds a production image (multi-stage, `SPRING_PROFILES_ACTIVE=prod` by default); `.github/workflows/ci.yml` (ADR-0013) runs `./mvnw clean test` against an ephemeral Postgres on every push/PR to `develop`/`main`.

**Build with JDK 21, not whatever `java` resolves to by default.** This machine's default `JAVA_HOME`/`PATH` point at JDK 25, which silently breaks Lombok 1.18.32 (the version Spring Boot 3.3.0 pins) — annotation processing no-ops with no warning, so `@Builder`/`@Getter`/`@Setter`/`@RequiredArgsConstructor` never generate, and compilation fails with confusing "cannot find symbol: builder()/setX()" errors. Point `JAVA_HOME` at a JDK 21 install (e.g. `C:\Program Files\Java\jdk-21` on this machine) before running any `mvnw` command.

Swagger/OpenAPI UI is served at `/swagger-ui.html` (raw docs at `/api-docs`) once the app is running.

## Architecture

**Controller split into interface + impl.** Each REST resource has an `I<Name>Controller` interface in `controllers/` carrying `@RequestMapping`, `@Validated`, and all Swagger/OpenAPI annotations (`@Tag`, `@Operation`), and a `@Component` implementation in `controllers/impl/` that implements the interface and only delegates to a service. When adding a new endpoint, add the route + Swagger docs on the interface method, and the actual logic call in the impl class.

**Layering:** Controller (interface + impl) → Service → Repository (Spring Data JPA) → Entity. Services are `@Service @RequiredArgsConstructor`, use constructor-injected repositories, and mark write operations `@Transactional`. Not-found cases use the `exceptions/NotFoundException` hierarchy — a resource-specific subclass per entity (`CustomerNotFoundException`, `EventNotFoundException`, `EventVenueNotFoundException`, `HdNotFoundException`, `ProfessionalNotFoundException`) caught by a global `exceptions/DomainExceptionHandler` (`@RestControllerAdvice`, added in GDE-7) that maps them to a `404` `ApiErrorDto`. New services should throw the specific resource exception, not a generic `RuntimeException`; the original `CustomerService` still throws plain `RuntimeException` for its not-found cases — that's documented debt, not the pattern to copy.

**DTO conversion lives on the entity/DTO, not in a mapper class.** Entities expose a `toResponseDto()` (or `toDTO()`) method that builds the response DTO; "create" DTOs (records) expose a `toEntity()` method that builds the entity via its Lombok `@Builder`. There is no MapStruct/ModelMapper — follow this manual conversion convention for new entities.

**Naming: the PT→EN migration (GDE-15) is finished.** Decided 2026-09-22 (ADR in `docs/spec-gestao-de-eventos.md` §9). Entity classes, DTOs, repositories, services, and controllers are all in English now (`Customer`/`CustomerRepository`/`ICustomerController`+`CustomerController`/`CustomerService`, `Event`/`EventRepository`, `EventVenue`/`EventVenueRepository`, `Professional`/`ProfessionalRepository`), and DB table names (`@Entity(name = "TB_CUSTOMER")`, `TB_EVENT`, `TB_EVENT_VENUE`, etc.) and `@Column` names have been renamed to English too. There's no more Portuguese naming debt to track here.

**Which resources have a full controller/service is a moving target — check `controllers/` (and its `impl/` subpackage) and `services/` for the current, authoritative list rather than trusting an enumeration in this doc.** Use the `Customer` stack (`ICustomerController` / `CustomerController` / `CustomerService`) as the template when building out a new resource's controller/service.

**Entity relationships:** `Event` is the central entity — `@ManyToOne` to `Customer`, `Hd`, and `EventVenue`. The `Event`↔`Professional` relationship is not a plain `@ManyToMany`; GDE-6 replaced it with an `EventProfessional` association entity (composite key `EventProfessionalId`, table `TB_EVENT_PROFESSIONAL`) carrying a `roleInEvent` field, with `Event.eventProfessionals` as the `@OneToMany` side. All entity IDs are `UUID` with `GenerationType.AUTO`.

## Development workflow

Task tracking lives on Jira, board **"Gestão de Eventos"** (key `GDE`). An `atlassian` MCP server is connected (site `renatoganskejr.atlassian.net`) — create/edit/transition cards directly via the `mcp__atlassian__*` tools rather than drafting them as text. Issue type is `Tarefa`; every card needs a user story ("Como… quero… para que…"), technical context, and an acceptance-criteria checklist — not a terse technical description.

For every task pulled from the board:

1. **Branch from `develop`** using a semantic prefix matching the change type: `feat/`, `fix/`, `chore/`, `refactor/`, `test/`, `docs/` (e.g. `feat/event-crud`, `fix/application-properties-encoding`). This now applies to `frontend/` work too — decided 2026-09-25, once the backend merged to `main`: the `frontend` integration branch (used 2026-09-24–2026-09-25 to isolate the early bootstrap, GDE-20/PR #24) is retired once its batch merge into `develop` lands; new frontend branches target `develop` directly like everything else.
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
