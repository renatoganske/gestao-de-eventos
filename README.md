# Gestão de Eventos

Sistema pessoal para gestão de trabalhos de fotografia: rastreia em qual HD externo está gravado cada evento entregue, e cruza dados de eventos, clientes, locais e profissionais para responder perguntas de negócio de forma livre — filtros composáveis, não uma lista fechada de métricas.

## Stack

- **Backend:** Spring Boot 3.3.0 / Java 21, Spring Data JPA, Flyway, PostgreSQL
- **Docs da API:** OpenAPI/Swagger (`springdoc`)
- **Build:** Maven (via `mvnw`, sem instalação própria de Maven necessária)

## Estrutura do repositório

```
gestao-de-eventos/    projeto Maven do backend (raiz do Maven é esta subpasta, não a raiz do repo)
docs/                  especificação técnica, PRD e ADRs (decisões arquiteturais)
docker-compose.yml     Postgres local para desenvolvimento
```

## Rodando localmente

Pré-requisitos: JDK 21 e Docker.

1. Suba o Postgres local:

   ```bash
   docker compose up -d
   ```

2. Rode a aplicação (o perfil `dev` é o padrão e já aponta para o Postgres acima):

   ```bash
   cd gestao-de-eventos
   ./mvnw spring-boot:run
   ```

3. A API sobe em `http://localhost:8080`; Swagger UI em `http://localhost:8080/swagger-ui.html` (docs raw em `/api-docs`).

### Rodando os testes

```bash
cd gestao-de-eventos
./mvnw test
```

A suíte inclui testes que sobem o contexto Spring completo contra o Postgres do passo 1 — ele precisa estar rodando.

### JDK 21, não a versão default do sistema

Se o `java` do seu `PATH` resolver para uma versão diferente de 21 (por exemplo JDK 25), o annotation processing do Lombok quebra silenciosamente e o build falha com erros confusos do tipo "cannot find symbol: builder()". Aponte `JAVA_HOME` para uma instalação de JDK 21 antes de rodar qualquer comando `mvnw`.

## Ambientes

Configuração separada por [Spring profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html):

- **`dev`** (ativo por padrão): `application-dev.properties` — aponta para o Postgres local do `docker-compose.yml`, com credenciais fixas.
- **`prod`**: `application-prod.properties` — lê `DATASOURCE_URL`, `DATASOURCE_USERNAME` e `DATASOURCE_PASSWORD` de variáveis de ambiente. Ativado automaticamente pela imagem Docker (`SPRING_PROFILES_ACTIVE=prod`).

## Docker

```bash
docker build -t gestao-de-eventos -f gestao-de-eventos/Dockerfile gestao-de-eventos

docker run -p 8080:8080 \
  -e DATASOURCE_URL=jdbc:postgresql://<host>:5432/gestaodeeventosdb \
  -e DATASOURCE_USERNAME=<usuario> \
  -e DATASOURCE_PASSWORD=<senha> \
  gestao-de-eventos
```

## CI

GitHub Actions roda `./mvnw clean test` a cada push/PR para `develop` e `main` (`.github/workflows/ci.yml`), com um serviço Postgres efêmero equivalente ao do `docker-compose.yml`. A imagem Docker não é construída no CI por enquanto.

## Documentação

- [`docs/spec-gestao-de-eventos.md`](docs/spec-gestao-de-eventos.md) — especificação técnica (arquitetura, modelo de domínio, convenções, guardrails)
- [`docs/prd.md`](docs/prd.md) — PRD do produto
- [`docs/adr/`](docs/adr/) — decisões arquiteturais registradas, uma por arquivo
