# ADR-0013: CI via GitHub Actions (build + testes)

**Data:** 2026-09-23
**Status:** Aceito

## Contexto

O projeto não tinha nenhuma esteira de CI. `CLAUDE.md` já exigia rodar `./mvnw test` manualmente antes de todo PR ("Run the full test suite before opening a PR"), mas isso dependia de disciplina humana/do agente — nada impedia um PR quebrado de ser aberto ou mesmo mergeado. Com o merge de `develop` em `main` se aproximando, faz sentido automatizar esse gate mínimo.

## Decisão

Adicionar `.github/workflows/ci.yml` rodando `./mvnw clean test` em todo push/PR para `develop` e `main`, com um serviço Postgres efêmero no próprio job (mesmas credenciais do `docker-compose.yml` local), já que parte da suíte (`GestaoDeEventosApplicationTests`, `EventServiceIntegrationTest`, `CreateCustomerValidationIntegrationTest`) sobe o contexto Spring completo e precisa de um banco real — não é possível mockar isso.

JDK 21 é fixado explicitamente via `actions/setup-java` (`temurin`/`21`) — o projeto já documentou que a versão errada de JDK quebra o annotation processing do Lombok silenciosamente (ver nota em `CLAUDE.md`), então o CI não pode depender da imagem `ubuntu-latest` trazer a versão certa por acaso.

Build de imagem Docker **não** entra no CI por enquanto — escopo desta ADR é só o gate de build+teste. Ver ADR-0015 sobre o Dockerfile em si.

## Racional

Pedido direto do Renato como parte da preparação para o primeiro merge `develop` → `main`. Escopo mínimo deliberado (só build+teste, sem lint/cobertura/deploy) — qualquer coisa além disso é decisão nova a propor, não a assumir aqui.

Implementação: task **GDE-29**.
