# ADR-0005: Métricas de negócio via filtros composáveis, não endpoints fixos

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

Renato quer liberdade para combinar filtros livremente sobre `Event` (tipo, local, profissional, período, HD, status) e descobrir correlações — não uma lista fechada de métricas pré-definidas (ver `docs/prd.md` §5). A abordagem ingênua seria criar um endpoint/método novo por combinação de filtro que surgir.

## Decisão

Priorizar um mecanismo de consulta composável e genérico: `EventFilter` como `Predicate<Event>` combinável (`byType(...).and(byVenue(...)).and(byPeriod(...))`), exposto por um endpoint de busca genérico (ex.: `GET /events/search`) que monta a combinação a partir de parâmetros opcionais na query string.

## Racional

Novas perguntas de negócio devem poder ser respondidas combinando filtros existentes, sem exigir código novo a cada pergunta nova. Cada novo critério vira apenas mais um factory method em `EventFilter`.

Implementação: task **GDE-12**.

Ref.: `docs/spec-gestao-de-eventos.md` §5.1-f, `docs/prd.md` §5
