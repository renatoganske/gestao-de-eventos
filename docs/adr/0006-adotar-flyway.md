# ADR-0006: Adotar Flyway para migrations

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

O schema hoje é gerado/atualizado implicitamente pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`), sem histórico versionado ou auditável de alterações.

## Decisão

Adotar Flyway. `ddl-auto` passa de `update` para `validate`. A partir daí, toda alteração de schema (novos enums, nova entidade, renomeações) precisa vir acompanhada de um script de migration versionado em `src/main/resources/db/migration`, não apenas da mudança na entidade.

## Racional

Pedido explícito do Renato — schema deixa de ser gerado implicitamente e passa a ter histórico versionado e auditável.

**Sequenciamento:** esta ADR depende da ADR-0007 (rename PT→EN) já estar concluída — ver ADR-0008.

Implementação: task **GDE-14**.

Ref.: `docs/spec-gestao-de-eventos.md` §4, §9
