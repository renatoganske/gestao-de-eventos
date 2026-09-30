# ADR-0024: O tipo de evento define se seus eventos usam os campos de casamento

**Data:** 2026-09-29
**Status:** Aceito

## Contexto

Os campos `daytimeWedding` (diurno) e `outdoorWedding` (ao ar livre) só fazem sentido para casamento e ficam no `Event` genérico, sem subtipo ([ADR-0001](0001-casamento-fields-sem-subtipo.md)). O tipo do evento é uma tabela de lookup editável, para que novos tipos possam ser cadastrados sem deploy ([ADR-0016](0016-event-type-tabela-lookup.md)).

Só que o vínculo entre *"este tipo usa os campos de casamento"* e o tipo em si era o **nome** escrito fixo no frontend: `WEDDING_TYPE_NAME = 'WEDDING'`, usado pelo formulário de evento e, desde o GDE-47, pelo filtro da busca. Na prática, o tipo ser uma tabela editável era só metade verdade: um tipo cadastrado como "Casamento" ou "Mini Wedding" **não** ativava os campos, nem no formulário nem no filtro. Quem cadastra o tipo não tem como saber disso pela tela.

## Decisão

O próprio tipo passa a dizer isso: `EventType` ganha `hasWeddingFields` (`boolean`, não nulo, padrão `false`; coluna `has_wedding_fields`).

- **Migration `V6`:** adiciona a coluna com `default false` e marca `true` o tipo chamado `WEDDING` (semeado pela V2), para o comportamento existente não mudar.
- **API:** `EventTypeDto` devolve `hasWeddingFields`; `CreateEventTypeDto` aceita o campo como **opcional**. No `POST`, omitido significa `false`. No `PUT`, omitido significa **manter o valor atual**, para que renomear um tipo por um payload que só traz o nome nunca o desmarque em silêncio (mesma convenção do `null` = "não mexe" da [ADR-0021](0021-escrita-associacao-event-professional.md)).
- **Frontend:** o formulário de evento e o filtro de busca mostram os campos de casamento quando `selectedType.hasWeddingFields` é verdadeiro, sem olhar o nome. `WEDDING_TYPE_NAME` é removida. O "+ Novo" do tipo ganha uma caixa "usa campos de casamento".

## Racional

**Alternativas avaliadas:**

| Alternativa | Por que não |
|---|---|
| Manter o nome fixo `WEDDING` | Mantém o problema: os tipos que o usuário cadastra não funcionam como ele espera, e renomear o tipo no CRUD quebra o formulário em silêncio |
| Trocar a constante por uma lista de nomes (`WEDDING`, `Casamento`, `Mini Wedding`...) | Continua acoplando código a dado de usuário: cada tipo novo exigiria deploy, o que a ADR-0016 existe para evitar |
| Subtipo `Wedding` de `Event` (herança JPA) | Reabre a ADR-0001 sem necessidade: casamento continua sendo o único tipo com atributos próprios |
| **Marca no próprio tipo (escolhida)** | Uma coluna, sem deploy para tipo novo, o dado mora onde o usuário o edita, e a regra deixa de depender de texto |

**Custos aceitos:**

- Um tipo de casamento já cadastrado **com outro nome** (por exemplo "Casamento") nasce desmarcado e precisa ser marcado uma vez. Como o frontend só lista e cria tipos (não edita), isso é feito por `PUT /api/event-types/{id}` ou recriando o tipo. Uma tela de gerência de tipos fica como card futuro, se incomodar.
- Se o tipo de casamento de um ambiente **não** se chamar exatamente `WEDDING`, a `V6` não o marca. Nesse caso, marcá-lo pela API logo após o deploy.
- Tipos que já têm eventos com flags preenchidas e são desmarcados depois continuam com esses valores gravados no `Event`; a marca só controla se os campos são **exibidos e filtráveis**, não apaga dado.

**Relação com o GDE-47:** o filtro por diurno / ao ar livre (GDE-47, [ADR-0005](0005-metricas-via-filtros-composaveis.md)) já mostrava seus controles condicionados ao tipo ser casamento. Esta decisão troca só a **origem** dessa informação (nome → marca), sem mudar o comportamento dos filtros: um filtro ativo continua casando apenas `true`/`false` explícito, e a flag `null` ("não aplicável") fica de fora.

Ref.: GDE-48
