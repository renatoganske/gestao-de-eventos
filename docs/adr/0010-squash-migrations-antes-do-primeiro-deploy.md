# ADR-0010: Squash de migrations é permitido antes do primeiro deploy real

**Data:** 2026-09-22
**Status:** Aceito

## Contexto

`V1__baseline.sql` (GDE-14) foi gerada a partir do schema existente em `develop` num momento anterior ao merge de GDE-4 (campos de `Hd`) e GDE-6 (`EventProfessional`). Como essas duas tasks mergearam depois, a baseline ficou desatualizada assim que nasceu: `tb_hd` sem os campos novos, `tb_event.id` como `varchar(36)` em vez de `uuid` (GDE-16), e o join table antigo de `Event`/`Professional` no lugar de `tb_event_professional`.

A correção inicial (GDE-16 + achado de schema) foi feita como `V2__sync_hd_fields_and_event_professional.sql`, empilhada sobre a `V1` errada. Isso levanta a pergunta: faz sentido carregar "a V1 nasceu errada, a V2 conserta" como histórico permanente, num projeto que ainda não tem nenhum ambiente real (produção, CI, ou até um segundo desenvolvedor) rodando essas migrations?

## Decisão

Enquanto o projeto não tiver um primeiro deploy real (produção, ou qualquer ambiente compartilhado cujo `flyway_schema_history` precise ser preservado), é permitido **reescrever migrations já commitadas** para consolidá-las, em vez de empilhar uma migration de correção sobre outra que nasceu errada. Nesse caso específico: `V1__baseline.sql` foi reescrita para refletir o schema correto pós-GDE-4/GDE-6/GDE-16 diretamente, e `V2__sync_hd_fields_and_event_professional.sql` foi removida.

Isso exige recriar (ou resetar o volume de) o Postgres local/dos containers de teste, já que o checksum da migration muda — mas nenhum desses bancos tem dado a preservar (ADR-0008 já estabeleceu esse mesmo raciocínio para o schema local antes do GDE-14).

**A partir do primeiro deploy real**, essa margem desaparece: qualquer ambiente que já tenha aplicado uma migration com um checksum específico não pode mais tê-la reescrita — daí em diante, toda correção de schema vira uma migration nova (`V(n+1)`), nunca uma edição de migration já aplicada em produção.

## Racional

Manter uma "baseline errada + patch" como história permanente de um projeto pré-lançamento é debt sem necessidade — ninguém depende do checksum da V1 hoje além de bancos locais/efêmeros, todos recriáveis sem custo. Squash agora, enquanto é grátis, evita carregar esse ruído indefinidamente.

Implementação: task **GDE-16** (PR consolidado).

Ref.: `docs/adr/0006-adotar-flyway.md`, `docs/adr/0008-sequenciamento-gde15-antes-gde14.md`
