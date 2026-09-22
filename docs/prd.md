# prod.md — Contexto de Negócio: Gestão de Trabalhos de Fotógrafo

**Projeto técnico relacionado:** `gestao-de-eventos` (ver `spec-gestao-de-eventos.md` para arquitetura/guardrails)
**Propósito deste documento:** descrever o negócio em si — problema, usuário, regras, fluxo e métricas — separado das decisões técnicas, para que qualquer pessoa (ou IA) trabalhando no código entenda *por que* o sistema existe antes de mexer em *como* ele é construído.

---

## 1. Visão de Produto

Renato é fotógrafo de eventos (ensaios, aniversários, casamentos, eventos diversos) e entrega os trabalhos gravados em HDs externos. Como o volume de trabalho é grande, os HDs são preenchidos por ordem de espaço disponível, não por ordem cronológica ou por cliente — então, quando precisa resgatar um trabalho antigo, não sabe em qual HD ele está e perde tempo procurando.

**Problema central:** localizar rapidamente em qual mídia física está um trabalho entregue.
**Problema secundário (oportunidade):** já que os dados do evento estão centralizados, usá-los para responder perguntas de negócio (quantos casamentos, com quem, onde) sem precisar vasculhar contratos ou planilhas.

**Quem usa:** só o Renato, hoje. Não é um produto para outros fotógrafos (ver decisão registrada — sem evidência de mercado para isso além do uso pessoal).

---

## 2. Glossário de Domínio (linguagem de negócio, não técnica)

| Termo | Significado no negócio |
|---|---|
| **Cliente** | Pessoa ou família que contratou o serviço fotográfico. Pode ter mais de um evento ao longo do tempo (ex.: ensaio de gestante → depois aniversário de 1 ano do bebê). |
| **Evento** | Um trabalho fotográfico específico e delimitado no tempo: um ensaio, um aniversário, um casamento ou outro tipo de evento. |
| **Tipo de evento** | Classifica o evento: Ensaio, Aniversário, Casamento, Evento Diverso. |
| **Casamento de dia** | Casamento cuja cerimônia/festa ocorre durante o dia (não à noite) — relevante para negócio porque muda logística de luz/horário e é um recorte que Renato quer filtrar. |
| **Casamento externo** | Casamento realizado fora de um espaço de eventos fechado (ex.: ao ar livre, sítio, praia) — outro recorte relevante de negócio. |
| **Local do Evento** | Onde o evento aconteceu (buffet, sítio, igreja, salão). Tem endereço, cidade, estado e tipo de espaço. |
| **Profissional** | Outro prestador de serviço envolvido no evento, que não é o próprio Renato — ex.: outro fotógrafo (segundo fotógrafo), videomaker, operador de drone, cerimonialista. |
| **Função no evento** | O papel que um Profissional específico exerceu naquele evento (ex.: "segundo fotógrafo" no casamento X, mas "videomaker" no casamento Y — a mesma pessoa pode ter funções diferentes em eventos diferentes). |
| **HD** | Mídia física (disco externo) onde o material de um ou mais eventos está gravado. Tem capacidade limitada e localização física própria (não fica sempre no mesmo lugar). |
| **Código do evento** | Identificador que correlaciona o registro do evento no sistema com o nome da pasta/arquivo gravado fisicamente no HD — é o elo entre "o sistema diz que está aqui" e "a pasta no disco realmente está lá". Padrão usado por Renato: prefixo `RG` + número sequencial (ex.: `RG0123`). |
| **Status de entrega** | Onde o trabalho está no ciclo de vida: Pendente (ainda não gravado em HD/entregue), Entregue (cliente já recebeu), Arquivado (entregue e guardado a longo prazo). |

---

## 3. Regras de Negócio

1. Todo evento pertence a exatamente um cliente, ocorre em exatamente um local e é gravado em exatamente um HD (não é dividido entre discos).
2. Um evento pode ter zero ou mais profissionais associados, cada um com sua própria função naquele evento específico.
3. Um HD guarda vários eventos ao longo do tempo, sem ordem cronológica — a mídia é escolhida por espaço disponível no momento da gravação, não por data ou tipo de evento. Essa é a causa raiz do problema que o sistema resolve.
4. `Casamento de dia` e `Casamento externo` só fazem sentido quando o tipo do evento é Casamento — para os demais tipos, esses campos ficam vazios.
5. O valor cobrado (`valor`) e a quantidade de convidados são informações do evento, não do cliente (o mesmo cliente pode ter eventos de valores/portes diferentes).

---

## 4. Fluxo de Trabalho (do evento à recuperação)

```
1. Evento acontece → Renato fotografa
2. Material é editado e gravado em algum HD com espaço disponível
3. Evento é cadastrado no sistema: cliente, local, profissionais envolvidos, HD de destino
4. Material é entregue ao cliente → status muda para "Entregue"
5. Passado um tempo → status pode virar "Arquivado"
6. (Necessidade recorrente) Renato precisa resgatar um trabalho antigo
   → consulta o sistema pelo nome do cliente/evento → sistema aponta o HD e a localização física dele
```

O ponto 6 é o motivo de o sistema existir — todo o resto do modelo (cliente, local, profissional) existe para enriquecer a busca e gerar métricas, não é o problema original.

---

## 5. Métricas de Negócio Desejadas

**Isto não é uma lista fechada.** Os itens abaixo são exemplos ilustrativos do tipo de pergunta que Renato faz, não um checklist fixo de métricas a implementar. O que Renato quer, de fato, é **liberdade para combinar filtros livremente e descobrir correlações** nos dados — tipo de evento, local, profissional, período, HD, status de entrega — sem precisar que cada combinação vire uma métrica cadastrada previamente.

Exemplos do tipo de pergunta (não exaustivo):

- Quantos casamentos de dia fiz em um determinado local?
- Quantos casamentos fiz com um determinado profissional (e em que função)?
- Em qual HD está o material de um evento específico?
- Quantos eventos de cada tipo por período; quais locais mais recorrentes; quais HDs estão perto da capacidade.

Isso implica, do lado técnico, priorizar um mecanismo de consulta/filtro **composável e genérico** (ver spec técnica, seção 5.1-f) em vez de um endpoint fixo por métrica — novas perguntas devem poder ser respondidas combinando filtros existentes, sem exigir código novo a cada pergunta nova que surgir.

---

## 6. Fora de Escopo (negócio)

Isto **não** é um sistema de gestão financeira, contratos ou relacionamento com cliente (CRM completo). Não cobre:

- Cobrança, parcelamento ou emissão de nota fiscal
- Assinatura de contrato ou termos com o cliente
- Envio de galeria/portal para o cliente
- Comunicação automática (e-mail/WhatsApp) com cliente ou profissional

Se algum desses itens virar necessidade real no futuro, deve entrar aqui antes de virar tarefa técnica.

---

## 7. Dados/Decisões em Aberto (a preencher conforme surgir)

- [ ] Quantos HDs Renato tem ativos hoje, e quais já têm eventos gravados a migrar para o sistema (carga inicial de dados)?
- [ ] Quantos eventos por mês, em média — ajuda a dimensionar se o schema atual aguenta o volume sem ajuste
- [ ] Lista real dos tipos de "Profissional" que costumam aparecer (para eventualmente virar categorias fixas em vez de texto livre)
- [ ] Confirmar se um evento *nunca* fica dividido entre dois HDs (regra 1 acima) — se um dia isso mudar, o modelo `Event.hd` (1:N) precisa virar N:N
