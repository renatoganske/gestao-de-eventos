# Gestão de Eventos — Frontend

SPA em React + TypeScript + Vite, consumindo a API REST do backend em `../gestao-de-eventos`. Stack decidida na [ADR-0012](../docs/adr/0012-stack-frontend-react-ts-vite.md).

## Desenvolvimento

Suba o backend primeiro (`../gestao-de-eventos`, porta `8080`), depois:

```bash
npm install
npm run dev
```

Abre em `http://localhost:5173`. Chamadas para `/api/*` são redirecionadas para `http://localhost:8080` pelo proxy do Vite (`vite.config.ts`) — **não precisa configurar CORS no backend** para desenvolver.

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
  components/   componentes de UI reutilizáveis (design system — GDE-21)
  pages/        uma tela por rota
  styles/       estilos globais / tokens
  types/        tipos TypeScript espelhando os DTOs do backend
```

## Lint

```bash
npm run lint
```

Usa [Oxlint](https://oxc.rs) (padrão do template atual do Vite) em vez de ESLint clássico — mesma finalidade (lint de JS/TS/React), mais rápido, sem dependência adicional.
