# ADR-0004: Tratamento de erro fica como débito técnico documentado

**Data:** 2026-09-22
**Status:** Aceito (débito técnico intencional, não bloqueante)

## Contexto

Hoje o projeto lança `RuntimeException` genérica em casos de "não encontrado", sem `@ControllerAdvice` nem hierarquia de exceções específica. A spec técnica (§7) já propõe uma solução mínima (`DomainError` sealed, exceções específicas por recurso, `@ControllerAdvice` único mapeando para 404/400).

## Decisão

Adiar a implementação da proposta da §7 — continuar com `RuntimeException` genérica por ora.

## Racional

Prioridade é fechar o CRUD de `Event`/`Hd` primeiro. É débito técnico pré-existente, não bloqueante para continuar as próximas entidades — mas **deve ser feito antes de expor a API para qualquer uso além do próprio Renato**.

Implementação (quando priorizada): task **GDE-7**.

Ref.: `docs/spec-gestao-de-eventos.md` §7
