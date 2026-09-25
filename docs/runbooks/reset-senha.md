# Runbook: recuperar acesso (senha esquecida)

Ver `docs/adr/0017-autenticacao-spring-security-jwt.md` — este projeto não tem
fluxo de "esqueci minha senha" por e-mail. Recuperação é manual, direto no
banco, porque você (único usuário) já tem acesso ao Postgres.

## 1. Gerar o hash BCrypt da nova senha

Não existe endpoint para isso — usa o mesmo `spring-security-crypto` que o
Maven já baixou para o build, via `jshell`. Não precisa instalar nada novo.

```bash
# Windows (Git Bash) / Linux / Mac — acha o jar no repositório local do Maven
find ~/.m2/repository/org/springframework/security/spring-security-crypto -name "*.jar" | grep -v sources
```

Copie o caminho retornado e abra o `jshell` apontando pra ele:

```bash
jshell --class-path "<caminho-do-jar-encontrado-acima>"
```

Dentro do `jshell`:

```java
var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
System.out.println(encoder.encode("sua-nova-senha-aqui"));
```

Copie o hash impresso (começa com `$2a$` ou `$2b$`).

## 2. Atualizar a senha no banco (Neon)

Conecte no Postgres do Neon (console web do Neon, ou `psql` com a connection
string de `DATASOURCE_URL`/`DATASOURCE_USERNAME`/`DATASOURCE_PASSWORD`) e rode:

```sql
update tb_app_user
set password_hash = '<hash-gerado-no-passo-1>'
where username = 'admin';
```

Se por algum motivo a tabela estiver vazia (usuário nunca foi criado — ver
`AdminUserSeeder`), insira em vez de atualizar:

```sql
insert into tb_app_user (id, username, password_hash)
values (gen_random_uuid(), 'admin', '<hash-gerado-no-passo-1>');
```

## 3. Testar

Faça `POST /api/auth/login` com o novo usuário/senha (Swagger UI, exige o
próprio login funcionando antes de conseguir usar o resto da API — ver
ADR-0017) e confirme que retorna um JWT.

## Por que não existe reset por e-mail

Decisão registrada na ADR-0017: como há um único usuário com acesso direto à
infraestrutura, um fluxo self-service por e-mail adicionaria uma dependência
nova (SMTP ou vendor de e-mail transacional) sem resolver um problema real —
o "esqueci minha senha" de quem já é dono do banco é sempre resolvível por
acesso direto, como acima.
