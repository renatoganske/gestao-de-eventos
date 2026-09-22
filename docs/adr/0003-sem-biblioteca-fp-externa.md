# ADR-0003: Sem biblioteca de programação funcional externa

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

A spec técnica define diretrizes de programação funcional (DTOs imutáveis, `Optional` em retorno de service, Stream API, `switch` de padrões, funções puras, composição de `Predicate`). Bibliotecas como Vavr ou functional-java poderiam reforçar esses idiomas.

## Decisão

Não introduzir Vavr, functional-java ou equivalente. Usar apenas os idiomas nativos do Java 21 (Streams, `Optional`, `switch` de padrões, `record`).

## Racional

São suficientes para as diretrizes definidas e para o porte do projeto (app pessoal, baixo volume). Reavaliar apenas se a complexidade de tratamento de erro crescer muito. Também alinhado ao guardrail geral de "sem dependência nova sem necessidade concreta".

Ref.: `docs/spec-gestao-de-eventos.md` §5.2, §6.8
