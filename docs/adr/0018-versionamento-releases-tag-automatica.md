# ADR-0018: Versionamento de release via tag git automática (nível "leve")

**Data:** 2026-09-25
**Status:** Aceito

## Contexto

Com o primeiro deploy real se aproximando, faltava uma convenção para responder "o que exatamente está rodando em produção?". Hoje o `pom.xml` está congelado em `0.0.1-SNAPSHOT` desde o início do projeto e nunca foi versionado de fato.

Vale separar três eixos que "versionamento" costuma misturar, para não superdimensionar a solução:

1. **Schema do banco** — já resolvido pelo Flyway; ver ADR-0006, ADR-0008 e ADR-0010 (regra pós-primeiro-deploy: nenhuma migration aplicada pode ser reescrita).
2. **Contrato da API** (`/api/v1/...`, versionamento por header, etc.) — hoje desnecessário: o único consumidor é o frontend do próprio projeto, evoluindo junto. Introduzir isso agora seria overengineering.
3. **Versão de release/artefato** (o que este ADR resolve) — identificar qual commit corresponde a qual versão publicada.

Duas alternativas de automação plena foram avaliadas e descartadas por serem ferramenta/dependência nova para o estágio atual do projeto (mantenedor único, pré-lançamento):

- **`jgitver`** (plugin Maven): deriva a versão do `pom.xml` a partir do histórico de tags git a cada build, sem edição manual — mas ainda exige decidir/criar a tag certa manualmente, só desloca o problema.
- **`release-please`** (GitHub Action): interpreta commits convencionais para decidir sozinho o bump de versão (major/minor/patch) e abre PR de release automaticamente — mais poderoso, mas é uma ferramenta nova no pipeline, desproporcional ao volume de releases atual.

## Decisão

Adotar o nível "leve": a versão do artefato continua sendo **decidida e editada manualmente por Renato no `pom.xml`** (convenção SemVer: `MAJOR.MINOR.PATCH`), sem nenhuma ferramenta nova. Um novo job `tag-release` em `.github/workflows/ci.yml` (ver ADR-0013) automatiza só a etapa mecânica: ao detectar um `push` em `main` (nunca em PR) com os testes passando, ele lê a versão do `pom.xml` via `mvn help:evaluate` e:

- Se a versão terminar em `-SNAPSHOT`, não faz nada — nenhuma release foi "cortada" ainda.
- Se a tag `vX.Y.Z` correspondente já existir, não faz nada — idempotente, não falha em reprocessamento.
- Caso contrário, cria e publica a tag `vX.Y.Z` automaticamente.

O Render (plataforma de deploy) não precisa de nenhuma configuração de versão — ele builda direto do Dockerfile a cada push e já rastreia deploys pelo commit no próprio dashboard, independente dessa tag. A tag existe para rastreabilidade do lado do repositório (git), não é lida pelo Render nem pela aplicação em runtime.

## Racional

Resolve a lacuna real (saber qual commit = qual versão publicada) sem introduzir tooling novo: nenhuma dependência adicional, nenhum PR automático de release, nenhuma decisão de bump tirada das mãos de Renato. O único automatismo é a tag em si — mecânico, sem julgamento envolvido, exatamente o tipo de passo que vale eliminar do fluxo manual. Se a cadência de releases crescer a ponto do bump manual doer, `jgitver` ou `release-please` ficam como evolução natural, sem precisar desfazer nada feito aqui.

Implementação: task **GDE-34**.

Ref.: ADR-0006, ADR-0008, ADR-0010, ADR-0013, ADR-0015.
