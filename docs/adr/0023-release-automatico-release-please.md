# ADR-0023: Release automático com release-please (substitui a ADR-0018)

**Data:** 2026-09-29
**Status:** Aceito (substitui a [ADR-0018](0018-versionamento-releases-tag-automatica.md))

## Contexto

A ADR-0018 escolheu o nível "leve": Renato editava a versão no `pom.xml` à mão e um job de CI só espelhava esse valor como tag git. Ela própria registrou o gatilho para revisitar: "se a cadência de releases crescer a ponto do bump manual doer, `release-please` fica como evolução natural".

Esse gatilho chegou. Com a esteira de CI/CD e o deploy no Render, a intenção é publicar em lotes pequenos e frequentes (o primeiro release grande, #50, acumulou o frontend inteiro e gerou 22 conflitos). Decidir o bump e editar o `pom.xml` a cada release é um passo mecânico que se repetiria sempre, e o job `tag-release` nunca criou GitHub Release nem changelog.

## Decisão

Adotar o **release-please** (GitHub Action `googleapis/release-please-action@v4`), em modo manifest:

- O workflow `.github/workflows/release-please.yml` roda a cada push na `main`, lê os commits convencionais desde a última release e abre/atualiza um **PR de release**.
- O PR de release altera a versão em `gestao-de-eventos/pom.xml` (via `extra-files`, xpath `//project/version`) e gera o `CHANGELOG.md`. Ao ser mergeado, cria a tag `vX.Y.Z` e a **GitHub Release**.
- Pacote único na raiz (`.`, `release-type: simple`) em vez de `gestao-de-eventos/`: assim commits que só tocam `frontend/` ou `docs/` também contam. O tag continua `vX.Y.Z` (`include-component-in-tag: false`), compatível com a `v1.0.0` existente.
- O manifest (`.release-please-manifest.json`) parte de `1.0.0`; `bootstrap-sha` aponta para o commit da `v1.0.0`.
- O job `tag-release` do `ci.yml` é **removido** (a tag passa a ser criada pelo release-please).

Regra de bump (Conventional Commits, pelo título do commit na `develop`): `feat` → minor; `fix` → patch; `feat!` ou rodapé `BREAKING CHANGE:` → major. `chore`, `docs`, `test`, `style` e `refactor` não geram versão por si só.

Convenção de merge:

| PR | Método |
|---|---|
| feature/fix/chore → `develop` | **Squash** — o título do PR vira o commit, e é ele que o release-please lê |
| `develop` → `main` | **Merge commit** — preserva o ancestral comum (squash foi a causa dos conflitos do #50) e mantém cada commit visível ao release-please |
| PR de release do release-please → `main` | Qualquer método |

Para isso o ruleset `main-protection` teve a regra "Require linear history" removida.

## Racional

- Elimina o passo manual sem trocar o modelo mental: a versão continua no `pom.xml`, só que quem edita é o PR de release, revisado e mergeado por Renato (nenhuma release sai sem clique dele).
- Ganha changelog e GitHub Release sem código próprio para manter — a alternativa de um workflow customizado que bumpa ao abrir o PR exigiria PAT/GitHub App (commits de `GITHUB_TOKEN` não disparam CI), push do bot na `develop` e teria de reimplementar changelog e Release.
- Trade-off aceito: o bump acontece **depois** do merge na `main` (PR de release extra), não ao abrir o PR; e `chore`/`docs` não bumpam. Se algo precisar virar patch, o commit usa o prefixo `fix:`.
- A `develop` não precisa ser sincronizada com o commit de release: `pom.xml` e `CHANGELOG.md` só mudam na `main`, então o merge seguinte `develop` → `main` não conflita, a menos que alguém edite esses arquivos na `develop`. A `develop` fica com a versão defasada no `pom.xml`, o que é só cosmético.

## Riscos

- O repositório precisa permitir "Allow GitHub Actions to create and approve pull requests" (Settings → Actions → General); sem isso o release-please não abre o PR.
- PRs criados com `GITHUB_TOKEN` não disparam workflows: o PR de release não roda a CI. Hoje o ruleset da `main` não exige status checks, então não bloqueia; se passar a exigir, será preciso PAT ou GitHub App.
- Se o release-please não localizar a `v1.0.0` como última release, o `bootstrap-sha` limita o escopo do primeiro changelog.
- Squash por engano em `develop` → `main` recria os conflitos e faz o release-please enxergar um único commit.

Ref.: ADR-0013, ADR-0018 (substituída).
