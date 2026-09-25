# ADR-0002: `Event`↔`Professional` vira entidade de associação `EventProfessional`

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

O relacionamento entre `Event` e `Professional` era um `@ManyToMany` puro (tabela `evento_profissional`), sem forma de registrar em que função o profissional atuou naquele evento específico (ex.: "segundo fotógrafo" no casamento X, "videomaker" no casamento Y para a mesma pessoa).

## Decisão

Substituir o `@ManyToMany` puro por uma entidade de associação própria, `EventProfessional` (`TB_EVENT_PROFESSIONAL`), com `@EmbeddedId` composto (`eventId` + `professionalId`) e um campo `roleInEvent`.

## Racional

A métrica "quantos casamentos com o profissional X" não exige isso, mas registrar a função foi identificado como pedido futuro de negócio. Como é tabela nova, nasce inteiramente em inglês — sem adicionar débito ao GDE-15.

Implementação: task **GDE-6**.

Ref.: `docs/spec-gestao-de-eventos.md` §3.3
