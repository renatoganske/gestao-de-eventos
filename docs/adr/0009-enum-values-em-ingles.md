# ADR-0009: Valores de enum em inglês, não em português

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

A spec técnica §3.1 originalmente listava os valores dos enums `EventType`, `DeliveryStatus` e `HdStatus` em português (`ENSAIO`, `ANIVERSARIO`, `CASAMENTO`, `EVENTO_DIVERSO` / `PENDENTE`, `ENTREGUE`, `ARQUIVADO` / `ATIVO`, `CHEIO`, `DEFEITO`, `ARQUIVADO`), espelhando o vocabulário de negócio do PRD (glossário §2). Ao implementar a task GDE-3, foi levantado que isso conflita com a convenção geral de que código novo nasce em inglês (ADR-0007) — um valor de enum é, na prática, um campo/constante de código, não apenas rótulo de UI.

## Decisão

Os valores dos três enums ficam em inglês:
- `EventType`: `PHOTO_SHOOT`, `BIRTHDAY`, `WEDDING`, `OTHER`
- `DeliveryStatus`: `PENDING`, `DELIVERED`, `ARCHIVED`
- `HdStatus`: `ACTIVE`, `FULL`, `DEFECTIVE`, `ARCHIVED`

## Racional

Pedido explícito do Renato ao decidir entre as duas opções levantadas — prioriza consistência com a convenção geral de inglês no código sobre espelhar o vocabulário de negócio do PRD diretamente nos identificadores. O vocabulário em português continua existindo no PRD (glossário §2) como linguagem de negócio; a tradução para os valores de enum é responsabilidade da camada de apresentação, se um dia isso for exposto a alguém que só lê português.

Spec técnica §3.1 e a task GDE-3 (implementação em `docs/spec-gestao-de-eventos.md`, task já mergeada na branch `feat/domain-enums`) foram atualizados para refletir os valores em inglês.

Ref.: `docs/spec-gestao-de-eventos.md` §3.1, `docs/adr/0007-ingles-idioma-padrao-migracao-pt-en.md`
