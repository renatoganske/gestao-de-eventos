# ADR-0014: Separação de configuração via Spring profiles (dev/prod)

**Data:** 2026-09-23
**Status:** Aceito

## Contexto

`application.properties` era um arquivo único com credenciais de Postgres local hardcoded (`postgres`/`1234`, `localhost:5432`). Isso é adequado para desenvolvimento, mas inviável para produção — não dá para versionar credenciais reais em texto plano, e não havia como rodar a aplicação em outro ambiente (ex.: container em produção) sem editar o arquivo.

## Decisão

Dividir a configuração em três arquivos, via [Spring profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html):

- `application.properties` — configuração comum a todos os ambientes (Swagger, JPA/Hibernate, nome da aplicação) e `spring.profiles.active=dev` como padrão.
- `application-dev.properties` — a configuração que já existia (Postgres local hardcoded). Ativa por padrão, preservando o comportamento atual de quem já desenvolve localmente sem precisar configurar nada a mais.
- `application-prod.properties` — lê `DATASOURCE_URL`, `DATASOURCE_USERNAME` e `DATASOURCE_PASSWORD` de variáveis de ambiente. Ativado sobrescrevendo `SPRING_PROFILES_ACTIVE=prod` (a imagem Docker já faz isso por padrão — ver ADR-0015).

Nenhuma outra propriedade foi duplicada entre os profiles; tudo que não muda entre dev e prod (Swagger, `ddl-auto=validate`, dialect) permanece no arquivo comum.

## Racional

Pedido direto do Renato como parte da preparação para o primeiro merge `develop` → `main`. Variáveis de ambiente para credenciais de produção é o mecanismo padrão do Spring Boot — sem necessidade de vault/secret manager para o volume e o estágio atual do projeto (avaliar se isso mudar quando houver deploy real).

Implementação: task **GDE-29**.
