# ADR-0028: A contagem de eventos das listas vem de uma query agregada por tipo, não das coleções

**Data:** 2026-10-20
**Status:** Aceito (GDE-58)

## Contexto

As telas de Clientes, Profissionais, Locais e HDs precisam mostrar e ordenar pela quantidade de eventos de cada item. Havia duas saídas (decididas no card): contar no frontend a partir de `GET /events`, ou entregar o número pela API. A primeira obriga cada aba a baixar todos os eventos e duplica a regra de negócio no cliente. A segunda tem o risco inverso, já visto na ADR-0027: calcular `eventCount` como `coleção.size()` carrega a coleção de eventos de cada item, ou seja, o N+1 que o GDE-52 acabou de eliminar.

## Decisão

- Os DTOs de resposta de Customer, Professional, EventVenue e Hd ganham `eventCount` (inteiro, campo aditivo: quem não tem eventos tem `0`).
- Cada repositório expõe duas queries JPQL sobre a **tabela de associação** (ou sobre `TB_EVENT`, no caso do HD, que é `@ManyToOne` direto): `countEventsGroupedBy<Tipo>()`, um `GROUP BY` que devolve `EventCountProjection` (`id`, `total`), e `countEventsBy<Tipo>Id(id)`, para as respostas de item único.
- O service de lista chama o `GROUP BY` **uma vez** e consulta o mapa por id (`EventCountProjection.toMap` / `countOf`); quem não aparece no mapa tem 0. O número de queries da contagem é fixo (uma por lista), independente de quantos itens existem.
- `toResponseDto(int eventCount)` recebe a contagem pronta: a entidade não sabe contá-la sem tocar na coleção. Criação devolve `0`.
- Os services de leitura passam a `@Transactional(readOnly = true)` (do Spring, não o do Jakarta, que não tem `readOnly`).

## Racional

- A contagem fica numa regra só, no banco, e o frontend só exibe e ordena (`sortValue` do `Table`).
- Uma query por lista é o custo mínimo e não depende do volume, o que importa em Render + Neon, onde cada ida e volta custa.
- A projeção em interface com `toMap` evita repetir o mesmo código de agregação nos quatro services; o próximo recurso que precisar de "quantos X tem" segue o mesmo molde.
- Não foi feito `@Formula`/subselect na entidade: esconderia uma subquery em toda leitura da entidade, inclusive onde a contagem não é usada.
