# ADR-0017: Autenticação via Spring Security + JWT, usuário único, sem self-service de reset de senha

**Data:** 2026-09-24
**Status:** Aceito

## Contexto

O backend está em produção real (Render + Neon, ADR-0014/0015), acessível publicamente pela internet, embora seja usado por uma única pessoa (Renato). Isso motivou revisitar se autenticação é necessária mesmo em app single-user: um endpoint aberto na internet pública, ainda que sem múltiplos usuários, já é superfície de risco real (descoberta da URL, scraping, exposição do CRUD de clientes/eventos).

Duas abordagens foram avaliadas com apoio dos skills `/software-architect` e `/anthropic-skills:cyber-security`:

1. **Serviço de auth gerenciado (Clerk).** Reduz código próprio (hash de senha, JWT, refresh, MFA de graça), mas introduz um vendor externo adicional — mais uma conta/API key a gerenciar além de Render e Neon —, acopla a disponibilidade do login à do serviço externo, e normalmente traz SDK de frontend, que ainda não foi aprovado (frontend segue "proposto", aguardando ADR-0012).
2. **Spring Security + JWT implementado no próprio backend**, com escopo mínimo (usuário único, sem múltiplos papéis, sem self-registration).

Do ponto de vista de segurança, a classe de vulnerabilidade que costuma justificar preferir um serviço gerenciado — enumeração de contas, credential stuffing em massa, envenenamento de fluxo de reset de senha por e-mail, escalada de privilégio entre usuários — não se aplica a um app com exatamente um usuário fixo. O risco residual (gestão do secret do JWT, hash de senha, expiração de token) existe nas duas abordagens por igual e é risco de configuração, não de "quem hospeda a autenticação".

Foi levantada também a necessidade de recuperação de senha ("esqueci minha senha"). Como Renato tem acesso direto à infraestrutura (é dono do banco Neon), a recuperação não depende de um fluxo self-service por e-mail: um `UPDATE` direto na tabela de usuário, trocando o hash da senha, sempre resolve o esquecimento — sem exigir credencial SMTP ou vendor de e-mail transacional adicional. Essa alternativa foi preferida a implementar um fluxo completo de reset por e-mail.

## Decisão

Adotar Spring Security + JWT, com escopo deliberadamente mínimo:

- Um único usuário fixo (linha na tabela, sem self-registration, sem múltiplos papéis/permissões).
- Senha armazenada com hash via `BCryptPasswordEncoder` (padrão do Spring Security, não implementação própria de criptografia).
- Endpoint de login que emite um JWT de expiração curta; sem fluxo de refresh token (reautenticar é barato para 1 usuário).
- Filtro de autenticação exigindo o token em todos os endpoints, incluindo travar Swagger UI (`/swagger-ui.html`), `/api-docs` e Actuator em produção (hoje abertos — gap pré-existente, independente desta decisão).
- Secret do JWT via variável de ambiente, seguindo o mesmo padrão já usado para `DATASOURCE_*` em `application-prod.properties`.
- **Sem fluxo self-service de "esqueci minha senha"** (sem envio de e-mail, sem vendor de e-mail transacional). Recuperação de acesso é feita via runbook manual: Renato conecta direto no Neon e atualiza o hash da senha via SQL.
- Clerk foi descartado para este escopo — não introduzir vendor de auth externo.

## Racional

A superfície de risco que motiva soluções de auth gerenciadas ou fluxos de reset por e-mail existe para cenários multi-usuário; aqui ela não se aplica. Implementar com Spring Security (biblioteca madura e amplamente auditada, não "rolar cripto própria") mantém o projeto sem depender de mais um serviço externo, coerente com a filosofia de simplicidade já adotada no projeto (monólito modular, sem infraestrutura além do necessário). O custo aceito — não ter recuperação de senha self-service — é baixo porque o único usuário tem acesso direto ao banco, e o ganho é evitar tanto um vendor de auth quanto uma peça de infraestrutura de e-mail que não teriam outro uso no projeto.

Ref.: `docs/adr/0006-adocao-flyway.md`, `docs/adr/0008-...md` (banco local sem dado a preservar), `docs/adr/0014-spring-profiles-dev-prod.md`, `docs/adr/0015-dockerfile-backend.md`.
