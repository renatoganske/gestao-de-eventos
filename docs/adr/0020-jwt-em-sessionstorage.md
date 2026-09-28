# ADR-0020: Token JWT armazenado em `sessionStorage` no frontend

**Data:** 2026-09-25
**Status:** Aceito

## Contexto

GDE-32 exige decidir onde o frontend guarda o token JWT emitido pelo login (ADR-0017/GDE-31), com validação explícita de segurança antes de implementar. Duas abordagens foram avaliadas com apoio do `/anthropic-skills:cyber-security`:

1. **`sessionStorage`** (proposto originalmente no card): acessível via JavaScript, o client HTTP central (`src/api/client.ts`) lê e anexa o token em `Authorization: Bearer` em toda chamada. Sobrevive a refresh de página, não sobrevive a fechar a aba.
2. **Cookie `httpOnly`**: inacessível a JavaScript (imune a exfiltração via XSS), mas exige mudança no backend — o `JwtAuthenticationFilter` atual só lê `Authorization: Bearer` (`HttpHeaders.AUTHORIZATION`), e `SecurityConfig` desabilita CSRF com `SessionCreationPolicy.STATELESS`, assumindo bearer token sem cookie. Adotar cookie `httpOnly` significa reabrir essa configuração (emitir `Set-Cookie` no login, mudar o filtro para ler do cookie, reativar proteção CSRF) — uma mudança de arquitetura de backend, não só de frontend.

O risco que `httpOnly` mitiga é exfiltração do token via XSS. Nesta aplicação, o vetor de XSS realista é dependência npm comprometida (supply chain) — não há conteúdo gerado por terceiros nem `dangerouslySetInnerHTML` previsto; é um único usuário (Renato) inserindo os próprios dados, com React escapando por padrão.

## Decisão

Manter o token JWT em `sessionStorage`, com estas mitigações como parte do escopo de GDE-32 e como regra permanente para o frontend:

- Não usar `dangerouslySetInnerHTML` em nenhuma tela; se algum dia for necessário renderizar HTML vindo de fora, sanitizar antes (ex.: DOMPurify).
- Token de expiração curta, já decidido em ADR-0017 (reduz a janela de uso de um token roubado).
- `npm audit`/Dependabot no pipeline do frontend para pegar dependências comprometidas cedo.

**Risco aceito:** um XSS bem-sucedido (via dependência comprometida) consegue ler `sessionStorage` e exfiltrar o token. Cookie `httpOnly` eliminaria esse vetor específico, mas exigiria reabrir uma decisão de backend já fechada (GDE-31) por um ganho marginal neste perfil de risco (usuário único, sem conteúdo de terceiros, sem SSR).

## Racional

O custo de mudar para cookie `httpOnly` (reabrir `SecurityConfig`, reativar CSRF, mudar o contrato de autenticação já em produção) não se justifica pelo perfil de risco real deste app: usuário único, painel interno, sem superfície de XSS por conteúdo de terceiros. `sessionStorage` (em vez de `localStorage`) já limita o alcance de um eventual vazamento — o token some ao fechar a aba e não é compartilhado entre abas. Se o app crescer para multi-usuário ou passar a renderizar conteúdo de terceiros, esta decisão deve ser revisitada.

Ref.: ADR-0017 (autenticação JWT); `JwtAuthenticationFilter`/`SecurityConfig` (backend, `gestao-de-eventos/src/main/java/.../configs/security/`); GDE-32.
