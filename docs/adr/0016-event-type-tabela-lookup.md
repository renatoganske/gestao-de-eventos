# ADR-0016: `Event.type` migra de enum fixo para tabela de lookup (`EventType`)

**Data:** 2026-09-24
**Status:** Aceito

## Contexto

`EventType` era um enum fixo (`PHOTO_SHOOT`, `BIRTHDAY`, `WEDDING`, `OTHER`, ver ADR-0009). Ao usar o Swagger já em produção, Renato identificou a necessidade de cadastrar tipos de evento que o enum não cobre (`WEDDING_PHOTO_SHOOT`, `BATIZADO`, `PROFESSIONAL_PHOTO_SHOOT`, entre outros que ainda vão surgir) — e quer liberdade para continuar adicionando tipos sem depender de mim para alterar código.

Três opções foram avaliadas:

1. **Manter enum, adicionar valores conforme necessário.** Type-safe e simples de ler, mas cada tipo novo exige mudança de código + deploy — exatamente o atrito que motivou a mudança.
2. **Campo de texto livre (`String`).** Liberdade total, sem deploy. Mas sem integridade: `"Casamento"`, `"casamento"` e `"Wedding"` viram três tipos diferentes por acidente de digitação, e isso contamina os filtros/métricas compostas que já são a forma preferida de consulta neste projeto (ADR-0005) — "quantos eventos por tipo" deixa de ser confiável.
3. **Tabela de lookup (`EventType` como entidade própria, com FK em `Event`).** Dá a liberdade de cadastrar tipos novos via API, sem deploy, mantendo integridade referencial (sem risco de typo) e metadado centralizado num único lugar.

## Decisão

Adotar a opção 3. `EventType` vira uma entidade (`TB_EVENT_TYPE`, `id` UUID + `name` único), com CRUD completo (`IEventTypeController`/`EventTypeController`/`EventTypeService`/`EventTypeRepository`), seguindo o mesmo template usado por `EventVenue`/`Professional`. `Event.type` deixa de ser `@Enumerated(EnumType.STRING)` e passa a ser `@ManyToOne` apontando para `EventType`. O enum `enums.EventType` é removido.

Como o projeto já passou do primeiro deploy real (Render + Neon, ver ADR-0014/0015), essa mudança de schema **não pode reescrever `V1__baseline.sql`** — vai como `V2__event_type_lookup_table.sql`, nova migration versionada (ver ADR-0010, que já previa esse corte assim que existisse um ambiente real). A migration cria `tb_event_type`, semeia os quatro valores que o enum já tinha (preservando compatibilidade com dados existentes), faz backfill de `tb_event.event_type_id` a partir do `type` textual antigo, e só então remove a coluna `type`.

## Racional

A tabela de lookup é o único das três formatos que atende as duas exigências em tensão: liberdade de cadastro (o motivo original da mudança) e integridade de dado (necessária para os filtros/métricas compostas da ADR-0005 continuarem confiáveis). O custo aceito é real mas conhecido — uma tabela a mais, um join a mais nas queries de `Event`, e um CRUD extra a manter — e é pequeno comparado ao de perder a confiabilidade das métricas por tipos de evento com nomes inconsistentes.

Ref.: `docs/adr/0005-metricas-via-filtros-composaveis.md`, `docs/adr/0009-enum-values-em-ingles.md`, `docs/adr/0010-squash-migrations-antes-do-primeiro-deploy.md`, `docs/adr/0011-response-dtos-sem-entidade-jpa-direta.md`
