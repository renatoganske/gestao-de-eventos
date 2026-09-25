# ADR-0019: Stack de teste do frontend — Vitest + React Testing Library + jsdom

**Data:** 2026-09-25
**Status:** Aceito

## Contexto

GDE-32 (tela de login e gestão de sessão JWT) é a primeira task de frontend com critério de aceite explícito de testes automatizados (login bem-sucedido, credenciais inválidas, redirect por token ausente/expirado). Até aqui o frontend (ADR-0012) não tinha nenhuma dependência de teste — só Vite, React, TypeScript e oxlint (lint). É preciso decidir a stack antes de escrever o primeiro teste, já que qualquer opção introduz dependências novas e convenções que as próximas tasks de tela (GDE-21 a GDE-26) vão herdar.

Duas opções avaliadas:

1. **Vitest + React Testing Library + jsdom.** Vitest reaproveita a config do Vite já existente (`vite.config.ts`), roda sobre o mesmo pipeline esbuild/rollup do dev server e do build, e expõe uma API (`describe`/`it`/`expect`/mocks) deliberadamente compatível com Jest.
2. **Jest + React Testing Library.** Runner mais estabelecido historicamente, mas exige um pipeline de transform próprio (Babel ou `ts-jest`) paralelo ao do Vite — duas ferramentas de build diferentes para o mesmo código.

## Decisão

Adotar **Vitest + React Testing Library + jsdom** como stack de teste do frontend, para GDE-32 e para as próximas tasks de tela.

- `vitest` roda em `jsdom` (ambiente DOM simulado, necessário para testar componentes React sem browser real).
- `@testing-library/react` (+ `@testing-library/user-event`) para testar componentes pelo comportamento visível ao usuário (o que a tela mostra e como reage a interação), não por detalhe de implementação.
- Script `npm test` (`vitest run`) adicionado ao `package.json`, rodado como parte do `npm run build:backend`/CI do frontend quando esse pipeline existir.

## Racional

Vitest elimina uma configuração de build paralela: mesma resolução de módulos, mesmo suporte a TypeScript/JSX que o Vite já usa em dev e produção, então um teste que passa reflete o mesmo comportamento de bundling que vai para produção. A API compatível com Jest mantém o material de treino/exemplos disponível para gerar código de teste correto (mesmo critério já usado em ADR-0012 para escolher React+TypeScript: reduzir o risco de código sutilmente errado gerado por IA). React Testing Library é adotada independente do runner escolhido, por ser o padrão de fato para testar comportamento de componente React em vez de estado interno — evita testes frágeis que quebram em qualquer refactor que não mude o comportamento observável.

Footprint de ferramental novo: `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom` (matchers de asserção de DOM).

Ref.: ADR-0012 (stack de frontend); GDE-32 (primeira task a exigir testes de frontend).
