# ADR-0012: Stack de frontend — React + TypeScript + Vite (SPA)

**Data:** 2026-09-23
**Status:** Aceito

## Contexto

O backend (`gestao-de-eventos/`) tem CRUD completo (`Customer`, `Event`, `Hd`, `Professional`, `EventVenue`), busca composável (`EventFilter`, GDE-12) e alerta de capacidade de HD (GDE-13), mas nenhuma interface web. Renato quer uma aplicação web com pelo menos três telas — cadastro de evento, busca/filtro de eventos, listagem de eventos —, aberta a mais telas, com requisito explícito de interface moderna, intuitiva e de fácil usabilidade. Time é uma pessoa (Renato) trabalhando com Claude Code como par.

## Decisão

- **Linguagem/framework:** React + TypeScript, sem framework de SSR (não Next.js).
- **Build tool:** Vite.
- **Tipo de aplicação:** SPA client-side pura, consumindo a API REST existente via `fetch` num client HTTP fino e tipado por recurso (um módulo por DTO, espelhando `EventDto`/`CreateEventDto`/enums em `interface`/`type`).
- **Localização no repo:** `frontend/` na raiz, irmão de `gestao-de-eventos/` (mesmo repo, mesmo fluxo de PR/board GDE).
- **Dev loop:** proxy do Vite (`server.proxy`, `/api` → `localhost:8080`) — sem CORS a configurar no backend em desenvolvimento.
- **Produção:** build estático do Vite copiado para `gestao-de-eventos/src/main/resources/static` — artefato único, sem CORS em produção (mesma origem).
- **Sem lib de data-fetching (React Query/TanStack Query) no MVP** — YAGNI; reavaliar se o boilerplate de loading/erro manual incomodar na prática.

## Racional

Sem necessidade de SSR/SEO (painel interno de uso pessoal), Next.js pagaria complexidade de roteamento/infra sem benefício. Entre React/Vue/Svelte, React+TypeScript é a combinação com maior cobertura de treino/exemplos disponíveis — critério decisivo numa dupla dev+IA, porque reduz o risco de código sutilmente errado gerado pela IA e maximiza o material disponível para o Renato resolver problemas sozinho. TypeScript casa com os DTOs já fortemente tipados do backend, evitando a classe de bug mais comum em SPA (campo renomeado no backend quebra a tela em runtime sem avisar). Monorepo (`frontend/` na raiz) evita sincronizar duas branches/dois fluxos de PR para um time de uma pessoa. Servir o build estático a partir do próprio Spring Boot elimina CORS em produção e mantém um único artefato deployável — sem introduzir hospedagem/infra nova.

Footprint de ferramental novo introduzido por esta decisão: Node.js LTS, npm, Vite, React, TypeScript, ESLint.

Riscos identificados: formulário de evento tem 13 campos (incluindo referências a `Hd`/`EventVenue`/`Customer`) — se `useState` manual por campo virar boilerplate excessivo, `react-hook-form` é uma adição pontual a avaliar depois, não decidida agora. `CreateEventDto` espera objetos `Hd`/`EventVenue`/`CreateCustomerDto` inteiros (não IDs) no create — o formulário precisa popular esses objetos a partir de selects, alinhar com o design das telas antes de implementar.

Implementação: task a criar no board GDE, uma vez definidas as telas com o `ux-interface-designer`.

Ref.: `docs/spec-gestao-de-eventos.md` §9
