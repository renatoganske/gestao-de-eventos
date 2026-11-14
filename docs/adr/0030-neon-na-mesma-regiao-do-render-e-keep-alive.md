# ADR-0030: Banco Neon na mesma região do Render (Oregon) e keep-alive em horário comercial

**Data:** 2026-11-14
**Status:** Aceito (complementa a [ADR-0029](0029-consultas-paginadas-enxutas-e-agregadas.md), que apontava a infraestrutura como o próximo suspeito)

## Contexto

Mesmo com as consultas enxutas da ADR-0029 em produção (PR #109, deploy de 2026-10-25), o Renato segue achando a aplicação lenta. A ADR-0029 já dizia: se continuar lento com respostas pequenas, o suspeito é a infraestrutura.

Conferindo os painéis em 2026-11-14:

| Componente | Plano | Região |
|---|---|---|
| Render (web service `gestao-de-eventos`, Docker) | Free | **Oregon (EUA, costa oeste)** |
| Neon (projeto `gestao-de-eventos-db`, Postgres) | Free | **AWS sa-east-1 (São Paulo)** |

Backend e banco estão em continentes diferentes. Cada query paga uma ida e volta Oregon ↔ São Paulo (ordem de 150 a 200 ms), e uma tela com várias queries em série soma segundos só de rede. É o custo "uns 250 ms por round-trip" registrado na ADR-0029, que não depende de volume de dados nem de código.

Outros fatos relevantes (documentação oficial dos planos e painéis, 2026-11-14):

- **Render free** dorme após 15 minutos sem tráfego de entrada; subir leva cerca de 1 minuto. São 750 horas gratuitas por mês por workspace, e serviço dormindo não consome horas. Qualquer requisição de entrada conta como tráfego.
- **Neon free** dorme após 5 minutos de inatividade (não dá para desligar), com 100 CU-horas por projeto por mês. O uso atual é mínimo (0,41 CU-h desde 31/10; 33 MB).
- O Render **não tem região no Brasil**. O Neon não permite trocar a região de um projeto existente.
- `JwtAuthenticationFilter` só consulta o banco quando recebe um token `Bearer`; uma requisição sem token volta `401` sem tocar no Postgres.

## Decisão

1. **Criar um novo projeto Neon na região AWS us-west-2 (Oregon)**, a mesma do Render, migrar os dados para ele e apontar o backend para o novo banco (`DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD` no painel do Render). O projeto antigo (São Paulo) fica como rollback por alguns dias e depois é removido.
2. **Keep-alive do Render em dias úteis e horário comercial**: um `GET` sem token a uma rota protegida (resposta `401`, sem consulta ao banco), a cada 10 minutos, de segunda a sexta, no fuso `America/Sao_Paulo`, por um agendador externo gratuito (cron-job.org). Sem alteração de código.
3. A senha do banco novo só existe no painel do Render e no gerenciador de senhas do Renato; nunca em arquivo, commit, memória ou PR.

## Racional

- Co-locar app e banco troca cada round-trip de ~150 a 200 ms por 1 a 2 ms. É a correção da causa, e as demais opções (reescrever o backend, trocar de linguagem) não atacam essa distância.
- Manter o Render em Oregon e mover o banco é o caminho possível: o Render não tem região no Brasil, e o Neon não troca região no lugar.
- O ping de horário comercial usa cerca de 240 horas por mês das 750 gratuitas e evita o cold start de ~1 minuto durante o uso. Como a rota protegida responde `401` sem consultar o banco, o Neon continua dormindo quando ninguém usa, preservando as CU-horas.
- Sem endpoint novo e sem mudança de código: nada de superfície nova na API pública.

## Consequências

- Janela curta de indisponibilidade no corte (alguns minutos); o corte só acontece com confirmação do Renato.
- O trajeto navegador → Render continua cruzando o continente (Oregon ↔ Brasil). Não há como evitar no plano e na região atuais.
- Se o Render reiniciar o serviço (permitido no plano free), o ping apenas reduz, não elimina, os cold starts.
- Se o desempenho continuar insuficiente depois de medir, as opções seguintes são hospedagem com região no Brasil ou VM própria (por exemplo Oracle Cloud Always Free), com o custo de operar a máquina.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Reescrever o backend em outra linguagem | Semanas de trabalho; só reduz o cold start da JVM, não a distância até o banco |
| Manter o banco em São Paulo e só pingar o Render | O ping não reduz o custo de cada query |
| Pingar rota que consulta o banco | Mantém o Neon acordado 24 horas e consome CU-horas sem necessidade |
| Mover o Render para outra região | O Render não tem região no Brasil; nenhuma delas fica perto do banco em São Paulo |
