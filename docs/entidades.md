# Entidades

Documentação das entidades JPA do projeto e seus atributos. Todos os nomes de tabela, coluna e campo estão em inglês (migração PT→EN concluída, GDE-15) e o schema é gerenciado pelo Flyway (`db/migration/`). Todos os IDs são `UUID`, exceto onde indicado. O modelo de domínio completo e as decisões por trás dele estão em `spec-gestao-de-eventos.md` (seção 3).

## Customer (`TB_CUSTOMER`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null |
| contact | String | |
| address | String | |
| notes | String | |
| events | List\<Event> | `@OneToMany`, mapped by `customer` |

## Event (`TB_EVENT`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| eventCode | String | chave de correlação com a pasta gravada no HD (ex.: `RG0123`) |
| type | EventType | `@ManyToOne` — tabela de lookup (ADR-0016) |
| name | String | not null |
| eventDate | LocalDate | |
| daytimeWedding | Boolean | só relevante se o tipo for casamento (ADR-0001) |
| outdoorWedding | Boolean | idem |
| guestCount | Long | |
| description | String | |
| amount | Double | |
| sizeGb | Integer | alimenta `Hd.usedSpaceGb` |
| deliveryStatus | DeliveryStatus | enum: `PENDING`, `DELIVERED`, `ARCHIVED` |
| hd | Hd | `@ManyToOne` |
| eventVenue | EventVenue | `@ManyToOne` |
| customer | Customer | `@ManyToOne` |
| eventProfessionals | List\<EventProfessional> | `@OneToMany`, mapped by `event` (ADR-0002) |

## EventType (`TB_EVENT_TYPE`)

Tabela de lookup dos tipos de evento, criável via API e pelo botão "+ Novo" do formulário de evento (GDE-39).

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null, unique |

## Hd (`TB_HD`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null |
| capacityGb | Integer | capacidade **nominal** (ex.: 1000) |
| realCapacityGb | Integer | capacidade **real/utilizável** reportada pelo SO (ex.: 931). Opcional; positiva e, se informada junto da nominal, não maior que ela (GDE-38, migration V5) |
| usedSpaceGb | Integer | |
| physicalLocation | String | |
| serialNumber | String | |
| acquisitionDate | LocalDate | |
| status | HdStatus | enum: `ACTIVE`, `FULL`, `DEFECTIVE`, `ARCHIVED` |
| events | List\<Event> | `@OneToMany`, mapped by `hd` |

> **Capacidade efetiva:** onde se calcula uso ou alerta de "perto da capacidade" (`HdCapacityPolicy`, barras de uso no frontend), vale `realCapacityGb` quando preenchida e `capacityGb` caso contrário.

## Professional (`TB_PROFESSIONAL`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null |
| type | ProfessionalType | `@ManyToOne` — tabela de lookup (ADR-0022) |
| contact | String | |
| specialtyTags | Set\<SpecialtyTag> | `@ManyToMany`, join table `TB_PROFESSIONAL_SPECIALTY` (ADR-0022) |
| otherInfo | String | |
| eventProfessionals | List\<EventProfessional> | `@OneToMany`, mapped by `professional` |

## ProfessionalType (`TB_PROFESSIONAL_TYPE`) e SpecialtyTag (`TB_SPECIALTY_TAG`)

Tabelas de lookup de tipo e de especialidades de profissional.

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null, unique |

## EventProfessional (`TB_EVENT_PROFESSIONAL`)

Entidade de associação entre `Event` e `Professional` (ADR-0002).

| Atributo | Tipo | Observações |
|---|---|---|
| id | EventProfessionalId | chave composta (`event`, `professional`) |
| event | Event | `@ManyToOne` |
| professional | Professional | `@ManyToOne` |
| roleInEvent | String | papel do profissional no evento (ex.: segundo fotógrafo) |

## EventVenue (`TB_EVENT_VENUE`)

| Atributo | Tipo | Observações |
|---|---|---|
| id | UUID | PK |
| name | String | not null |
| address | String | |
| city | String | |
| state | String | texto no banco; o frontend oferece um select com as 27 UFs (GDE-42), sem validação no backend. Valores legados fora da lista continuam válidos |
| type | String | |
| events | List\<Event> | `@OneToMany`, mapped by `eventVenue` |

## AppUser

Usuário de autenticação (Spring Security + JWT, ADR-0017; tabela criada na migration V3).
