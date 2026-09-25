# Entidades

Documentação das entidades JPA do projeto e seus atributos.

## Customer (`TB_CLIENTE`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| nome | String | not null |
| contato | String | |
| endereco | String | |
| observacoes | String | |
| events | List\<Event> | `@OneToMany`, mapped by `customer` |

## Event (`TB_EVENTO`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| codigoDoEvento | String | |
| tipo | String | |
| nome | String | not null |
| dataDoEvento | LocalDate | |
| casamentoDeDia | Boolean | |
| casamentoExterno | Boolean | |
| quantidadeDeConvidados | Long | |
| descricao | String | |
| valor | Double | |
| hd | Hd | `@ManyToOne` |
| eventVenue | EventVenue | `@ManyToOne` |
| customer | Customer | `@ManyToOne` |
| profissionais | List\<Professional> | `@ManyToMany`, join table `evento_profissional` |

## Hd (`TB_HD`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| nome | String | not null |
| capacidade | Integer | |
| dataAquisicao | LocalDate | |
| status | String | |
| events | List\<Event> | `@OneToMany`, mapped by `hd` |

## Professional (`TB_PROFISSIONAL`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| nome | String | not null |
| tipo | String | |
| contato | String | |
| especialidade | String | |
| outrasInformacoes | String | |
| events | List\<Event> | `@ManyToMany`, mapped by `profissionais` |

## EventVenue (`TB_LOCAL_DO_EVENTO`)

| Atributo | Tipo | Coluna no banco | Observações |
|---|---|---|---|
| id | UUID | id | PK |
| name | String | nome | not null |
| adress | String | endereco | |
| city | String | cidade | |
| state | String | estado | |
| type | String | tipo | |
| events | List\<Event> | — | `@OneToMany`, mapped by `eventVenue` |

> Nota: os nomes de colunas de `EventVenue` seguem em português enquanto os campos Java já foram migrados para inglês — reflexo da migração PT→EN em andamento no projeto (ver `CLAUDE.md`).
