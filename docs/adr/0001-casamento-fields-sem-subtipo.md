# ADR-0001: Manter campos específicos de casamento no `Event` genérico

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

`casamentoDeDia` e `casamentoExterno` só fazem sentido quando `Event.tipo = CASAMENTO`. A alternativa seria modelar um subtipo de evento (herança JPA com `@Inheritance`, ou tabela própria `TB_CASAMENTO`) para não deixar campos "vazios" no caso genérico.

## Decisão

Manter `casamentoDeDia`/`casamentoExterno` como campos nullable no próprio `Event`, sem subtipo/tabela separada.

## Racional

Modelar um subtipo seria over-engineering para o volume e o problema atuais (app pessoal, baixo volume de eventos). É débito técnico aceito e documentado, não ignorado: se um dia surgir um segundo tipo de evento com atributos próprios, reavaliar.

Ref.: `docs/spec-gestao-de-eventos.md` §3.2
