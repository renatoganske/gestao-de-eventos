# ADR-0015: Containerização do backend via Dockerfile multi-stage

**Data:** 2026-09-23
**Status:** Aceito

## Contexto

Não havia forma de empacotar a aplicação para rodar fora de uma máquina com JDK 21 e Maven instalados manualmente. Com o merge de `develop` em `main` se aproximando, o backend precisa de um artefato de deploy.

## Decisão

`gestao-de-eventos/Dockerfile`, multi-stage:

1. **Build**: `eclipse-temurin:21-jdk`, usa o `mvnw` do próprio projeto (não uma imagem `maven:*` genérica) para garantir a mesma versão de Maven que já é usada localmente e no CI, roda `clean package -DskipTests` (os testes já rodam no CI — ver ADR-0013 — não precisam rodar de novo na build da imagem).
2. **Runtime**: `eclipse-temurin:21-jre` (sem JDK completo, imagem menor), só o `.jar` gerado. `SPRING_PROFILES_ACTIVE=prod` como padrão (ver ADR-0014), porta `8080` exposta.

O build da imagem **não** entra na esteira de CI por enquanto (ADR-0013) — só é validado manualmente/no momento do deploy.

Frontend (React+Vite) fica de fora deste Dockerfile por ora — ainda não foi mergeado em `develop`. Quando entrar, avaliar se vira um estágio adicional deste mesmo Dockerfile (build do frontend + cópia para `src/main/resources/static`, conforme já usa a task `build:backend` do frontend) ou uma imagem separada — decisão para quando a ponte fizer sentido de verdade.

**Achado ao validar o build:** `gestao-de-eventos/mvnw` estava commitado com quebra de linha CRLF (checkout feito em Windows), o que quebra silenciosamente com `/bin/sh: ./mvnw: not found` dentro de qualquer imagem Linux — não é específico deste Dockerfile, teria quebrado em qualquer container que rodasse o wrapper. Corrigido normalizando o arquivo para LF e adicionando `.gitattributes` (`gestao-de-eventos/mvnw text eol=lf`) para não regredir em checkouts futuros no Windows.

## Racional

Pedido direto do Renato como parte da preparação para o primeiro merge `develop` → `main`. Multi-stage mantém a imagem final pequena (sem toolchain de build) sem exigir um segundo build system além do Maven já usado no projeto.

Implementação: task **GDE-29**.
