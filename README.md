# Gestão de Eventos

[![CI](https://github.com/renatoganske/gestao-de-eventos/actions/workflows/ci.yml/badge.svg)](https://github.com/renatoganske/gestao-de-eventos/actions/workflows/ci.yml)

Sistema web fullstack para gestão de trabalhos de fotografia de eventos: rastreia **em qual HD externo está gravado cada evento entregue** e cruza dados de eventos, clientes, locais e profissionais para responder perguntas de negócio de forma livre.

É um projeto pessoal, mas com um problema real e um usuário real: eu sou fotógrafo de eventos e o volume de trabalho é grande. Os HDs são preenchidos por espaço disponível, não por ordem cronológica, então resgatar um trabalho antigo significava abrir disco por disco. O sistema resolve isso e está em produção.

## O que ele faz

- **Localização física:** dado um cliente ou evento, aponta o HD e onde ele está guardado. O código do evento (ex.: `RG0123`) liga o registro à pasta gravada no disco.
- **Controle de HDs:** capacidade nominal e real, espaço usado, alerta de "perto da capacidade" e barras de uso.
- **Cadastros:** clientes, eventos, locais, profissionais (com tipo e especialidades), com criação rápida ("+ Novo") dentro do formulário de evento.
- **Inteligência de negócio:** busca por filtros combináveis (tipo, local, profissional, período, HD, status), sem métricas fixas.
- **Acesso protegido:** login com JWT.

## Decisões de engenharia

Cada decisão relevante tem um ADR em [`docs/adr/`](docs/adr/) com contexto, alternativas avaliadas e o custo aceito. As que mais dizem sobre como eu trabalho:

**Consultas composáveis em vez de um endpoint por métrica** ([ADR-0005](docs/adr/0005-metricas-via-filtros-composaveis.md)). Eu não queria uma lista fechada de indicadores, queria descobrir correlações. `EventFilter` são `Predicate`s combináveis, expostos em `GET /events/search`; `ProfessionalFilter` repete o padrão. Uma pergunta nova de negócio não vira código novo.

**O modelo de dados expressa a regra de negócio** ([ADR-0022](docs/adr/0022-professional-type-e-specialty-tags.md), [ADR-0016](docs/adr/0016-event-type-tabela-lookup.md), [ADR-0002](docs/adr/0002-event-professional-entidade-associacao.md)).
- Tipo de evento saiu de enum para tabela de lookup quando, usando o sistema em produção, percebi que precisava cadastrar tipos sem deploy.
- Um profissional tem um tipo e várias especialidades: são duas relações diferentes (`@ManyToOne` e `@ManyToMany`), e a cardinalidade fica garantida pelo schema, não por validação solta. Descartei uma entidade `Tag` genérica por isso.
- `Event`↔`Professional` virou entidade de associação para registrar o papel de cada profissional em cada evento.

**Escrita da associação com armadilhas tratadas de propósito** ([ADR-0021](docs/adr/0021-escrita-associacao-event-professional.md)).
- Replace-all com repositório explícito, sem `cascade`/`orphanRemoval`. Um `flush()` entre o delete e o insert evita que o Hibernate reordene as operações e viole a PK.
- No update, `null` mantém a equipe e `[]` esvazia, para que um cliente que não conhece o campo não apague dados por omissão.
- Profissional duplicado no payload vira comportamento definido, não um 500 vazando de constraint.
- Mantive `roleInEvent` como texto livre, mesmo depois de avaliar uma tabela de lookup, e registrei o débito.

**Evolução de schema segura** ([ADR-0006](docs/adr/0006-adotar-flyway.md), [ADR-0010](docs/adr/0010-squash-migrations-antes-do-primeiro-deploy.md)). Flyway com `ddl-auto=validate`. Reescrever migrations só era permitido até o primeiro deploy; depois, nunca mais. A migration do `Professional` faz backfill dos dados existentes em vez de assumir que produção está vazia só porque meu banco local estava.

**Segurança proporcional ao risco** ([ADR-0017](docs/adr/0017-autenticacao-spring-security-jwt.md), [ADR-0020](docs/adr/0020-jwt-em-sessionstorage.md)). Spring Security + JWT, BCrypt, usuário único, sem fluxo de "esqueci minha senha" (recuperação por runbook). Descartei um provedor externo de auth. O token fica em `sessionStorage` com o risco de XSS registrado como **risco aceito**, com mitigações, em vez de escondido.

**Contrato de API sem vazar a entidade** ([ADR-0011](docs/adr/0011-response-dtos-sem-entidade-jpa-direta.md)). DTOs de resposta nunca embutem entidade JPA: coleções viram `*SummaryDto` só com escalares (sem ciclos por construção) e referências viram id. Conversão manual, sem MapStruct.

**Releases automatizados a partir de um erro real** ([ADR-0023](docs/adr/0023-release-automatico-release-please.md)). Um release grande (#50) acumulou o frontend inteiro e gerou 22 conflitos, causados por merge com squash. Passei a usar Conventional Commits + release-please e defini a regra de merge: squash nas features para `develop`, merge commit de `develop` para `main`.

**Consistência antes de novidade** ([ADR-0007](docs/adr/0007-ingles-idioma-padrao-migracao-pt-en.md)). Encontrei uma migração PT→EN pela metade e decidi terminar antes de crescer o código (entidades, DTOs, tabelas, colunas e enums). O Flyway só entrou depois, para a baseline já nascer em inglês ([ADR-0008](docs/adr/0008-sequenciamento-gde15-antes-gde14.md)).

**Débito técnico como decisão registrada, não como surpresa** ([ADR-0004](docs/adr/0004-tratamento-erro-debito-tecnico.md), [ADR-0001](docs/adr/0001-casamento-fields-sem-subtipo.md), [ADR-0003](docs/adr/0003-sem-biblioteca-fp-externa.md)). Campos de casamento no `Event` genérico em vez de herança, tratamento de erro adiado e depois resolvido com exceções por recurso e um `@RestControllerAdvice`, e nenhuma biblioteca funcional externa: Java 21 puro (Streams, `Optional`, `record`, `switch` de padrões).

## Arquitetura

```
Controller (interface + impl) → Service → Repository (Spring Data JPA) → Entity
```

- A interface do controller carrega rota e documentação OpenAPI; a implementação só delega ao service.
- Regras puras ficam isoladas de I/O e testáveis sem mock (ex.: `HdCapacityPolicy`, que usa a capacidade real quando existe e a nominal caso contrário).
- Exceções específicas por recurso (`404`), integridade violada vira `409`, tudo tratado num handler global.
- Frontend React + TypeScript + Vite (SPA) que fala com a API REST ([ADR-0012](docs/adr/0012-stack-frontend-react-ts-vite.md)).

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security (JWT), Flyway, Lombok |
| Banco | PostgreSQL |
| Frontend | React, TypeScript, Vite |
| Testes | JUnit + Spring Test (backend); Vitest + React Testing Library (frontend, [ADR-0019](docs/adr/0019-stack-teste-frontend-vitest.md)) |
| API | OpenAPI/Swagger (`springdoc`) |
| Infra | Docker (multi-stage), GitHub Actions, release-please, deploy em Render + Neon |

## Qualidade e processo

- **Testes:** cerca de 200 no backend e 165 no frontend, rodando no CI a cada push/PR, incluindo testes que sobem o contexto Spring contra um Postgres efêmero.
- **Fluxo:** cada tarefa é um card no Jira (história de usuário, contexto técnico e critérios de aceite), vira uma branch com prefixo semântico, tem testes e passa por PR. Eu reviso e faço o merge de todos.
- **Escopo controlado:** nada de escopo novo entra sem proposta e aprovação prévia.
- **Decisão arquitetural = ADR:** uma decisão por arquivo, com contexto, alternativas e custo aceito.
- **Uso de IA:** desenvolvo com apoio de agentes de IA (Claude Code). A direção do produto, as decisões e a aprovação de cada PR são minhas.

## Rodando localmente

Pré-requisitos: JDK 21, Docker e Node.js.

1. Suba o Postgres local:

   ```bash
   docker compose up -d
   ```

2. Backend (o perfil `dev` é o padrão e já aponta para o Postgres acima):

   ```bash
   cd gestao-de-eventos
   ./mvnw spring-boot:run
   ```

   API em `http://localhost:8080`; Swagger UI em `http://localhost:8080/swagger-ui.html` (docs raw em `/api-docs`). Os endpoints da API exigem login; o Swagger só fica aberto fora de produção.

3. Frontend (proxy de `/api` para `:8080`):

   ```bash
   cd frontend
   npm install --legacy-peer-deps
   npm run dev
   ```

   O `--legacy-peer-deps` é necessário; detalhes em [`frontend/README.md`](frontend/README.md).

### Testes

```bash
cd gestao-de-eventos && ./mvnw test   # precisa do Postgres do passo 1
cd frontend && npm test
```

### JDK 21, não a versão default do sistema

Se o `java` do `PATH` resolver para outra versão (por exemplo JDK 25), o annotation processing do Lombok quebra silenciosamente e o build falha com erros do tipo "cannot find symbol: builder()". Aponte `JAVA_HOME` para um JDK 21 antes de rodar o `mvnw`.

## Ambientes

Configuração separada por [Spring profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html) ([ADR-0014](docs/adr/0014-spring-profiles-dev-prod.md)):

- **`dev`** (padrão): aponta para o Postgres do `docker-compose.yml`.
- **`prod`**: lê `DATASOURCE_URL`, `DATASOURCE_USERNAME` e `DATASOURCE_PASSWORD` do ambiente; ativado pela imagem Docker (`SPRING_PROFILES_ACTIVE=prod`).

## Docker

```bash
docker build -t gestao-de-eventos -f gestao-de-eventos/Dockerfile gestao-de-eventos

docker run -p 8080:8080 \
  -e DATASOURCE_URL=jdbc:postgresql://<host>:5432/gestaodeeventosdb \
  -e DATASOURCE_USERNAME=<usuario> \
  -e DATASOURCE_PASSWORD=<senha> \
  gestao-de-eventos
```

## CI/CD e releases

- **CI** ([ADR-0013](docs/adr/0013-ci-github-actions.md)): GitHub Actions roda `./mvnw clean test` a cada push/PR para `develop` e `main`, com Postgres efêmero.
- **Releases** ([ADR-0023](docs/adr/0023-release-automatico-release-please.md)): o release-please lê os Conventional Commits da `main`, abre um PR de release que atualiza `pom.xml` e `CHANGELOG.md` e, ao ser mergeado, cria a tag e a GitHub Release.

## Estrutura do repositório

```
gestao-de-eventos/    projeto Maven do backend (a raiz do Maven é esta subpasta)
frontend/             SPA React + TypeScript + Vite
docs/                 spec técnica, PRD, modelo de entidades e ADRs
docker-compose.yml    Postgres local para desenvolvimento
```

## Documentação

- [`docs/spec-gestao-de-eventos.md`](docs/spec-gestao-de-eventos.md): especificação técnica (arquitetura, modelo de domínio, convenções)
- [`docs/prd.md`](docs/prd.md): PRD do produto
- [`docs/entidades.md`](docs/entidades.md): entidades e atributos
- [`docs/adr/`](docs/adr/): decisões arquiteturais, uma por arquivo
- Histórico de versões: [GitHub Releases](https://github.com/renatoganske/gestao-de-eventos/releases)
