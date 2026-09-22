# ADR-0008: Sequenciamento — GDE-15 (rename PT→EN) roda antes de GDE-14 (Flyway)

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

Tanto o rename PT→EN (ADR-0007 / GDE-15) quanto a adoção do Flyway (ADR-0006 / GDE-14) estão no backlog. A ordem entre elas afeta se a baseline do Flyway nasce em português (exigindo depois uma migration de rename) ou já em inglês.

## Decisão

GDE-15 roda antes de GDE-14. O rename PT→EN continua usando `ddl-auto=update` (sem migration versionada); só depois disso o Flyway é introduzido, com baseline já em inglês.

## Racional

Banco local do Renato ainda não tem dados (confirmado 2026-09-22) — nada a preservar, então não há motivo para criar a baseline do Flyway em português só para renomear em seguida.

Ref.: `docs/spec-gestao-de-eventos.md` §9; memória do projeto `project_english_standard.md`
