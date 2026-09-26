# Gestão de Eventos — Frontend

SPA em React + TypeScript + Vite, consumindo a API REST do backend em `../gestao-de-eventos`. Stack decidida na [ADR-0012](../docs/adr/0012-stack-frontend-react-ts-vite.md).

## Desenvolvimento

Suba o backend primeiro (`../gestao-de-eventos`, porta `8080`), depois:

```bash
npm install --legacy-peer-deps
npm run dev
```

**`--legacy-peer-deps` é obrigatório em `npm install`/`npm ci` nesta máquina.** npm 9.7.1 tem um bug conhecido no resolvedor de árvore (`Cannot read properties of null (reading 'edgesOut')`, arborist) que quebra com o grafo de peer dependencies opcionais do Vitest (`msw`, `@vitest/ui`, providers de browser). Sem a flag, tanto `npm install` quanto `npm ci` falham. `package-lock.json` foi gerado com a flag — `npm ci` também precisa dela para instalar a partir do lockfile sem reclamar de entradas "missing".

Abre em `http://localhost:5173`. Chamadas para `/api/*` são redirecionadas para `http://localhost:8080` pelo proxy do Vite (`vite.config.ts`) — **não precisa configurar CORS no backend** para desenvolver.

### Rodando contra a API de produção (Render)

Para testar o frontend local com os dados reais já deployados, sem subir o backend localmente, aponte o alvo do proxy do Vite para a API de produção em vez do `localhost:8080` (GDE-35):

```bash
cp .env.example .env.local
# edite .env.local e descomente/preencha VITE_API_TARGET com a URL da API em produção
npm run dev
```

Ou, sem arquivo, direto no comando (Git Bash/macOS/Linux):

```bash
VITE_API_TARGET=https://gestao-de-eventos-6jp9.onrender.com npm run dev
```

Como o alvo é lido pelo processo do Vite (proxy server-side), e não pelo navegador, isso continua sem exigir nenhuma configuração de CORS no backend — mesmo princípio do ADR-0012, só que apontando para outro host. `.env.local` é ignorado pelo git (`*.local`), então essa configuração fica só na sua máquina.

## Build de produção

```bash
npm run build:backend
```

Gera o build em `dist/` e copia o conteúdo para `../gestao-de-eventos/src/main/resources/static`. O Spring Boot serve esses arquivos estáticos diretamente — front e API ficam na mesma origem em produção, sem CORS e sem host separado.

Use `npm run build` sozinho se só quiser gerar `dist/` sem copiar (ex.: inspecionar o build).

## Estrutura

```
src/
  api/          cliente HTTP para a API REST (fetch tipado)
  auth/         login, sessão (JWT em sessionStorage), rota protegida — GDE-32
  components/   componentes de UI reutilizáveis (design system — GDE-21)
  hooks/        hooks compartilhados (ex.: useFormDraft — persistência de rascunho de formulário)
  pages/        uma tela por rota
  styles/       estilos globais / tokens
  test/         setup global dos testes (Vitest)
  types/        tipos TypeScript espelhando os DTOs do backend
```

## Lint

```bash
npm run lint
```

Usa [Oxlint](https://oxc.rs) (padrão do template atual do Vite) em vez de ESLint clássico — mesma finalidade (lint de JS/TS/React), mais rápido, sem dependência adicional.

## Testes

```bash
npm test
```

Vitest + React Testing Library + jsdom (ADR-0019) — reaproveita a config do Vite (`vite.config.ts`, bloco `test`). Testes ficam ao lado do código (`Componente.test.tsx`), não numa pasta `__tests__` separada.
