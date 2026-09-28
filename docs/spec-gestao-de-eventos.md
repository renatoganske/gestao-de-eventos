# Especificação Técnica — Gestão de Trabalhos de Fotógrafo (`gestao-de-eventos`)

**Versão:** 1.0
**Stack:** Spring Boot 3.3.0 · Java 21 · PostgreSQL
**Escopo:** app de uso pessoal para rastrear em qual HD está cada trabalho fotográfico entregue, e cruzar isso com clientes, eventos, locais e profissionais para métricas de negócio.

---

## 1. Visão Geral

O sistema resolve dois problemas:

1. **Localização física**: em qual HD está gravado um determinado trabalho (ensaio, aniversário, casamento, evento).
2. **Inteligência de negócio**: cruzar dados de evento × local × profissional × cliente de forma livre e composável — o objetivo não é um conjunto fixo de métricas pré-definidas, e sim permitir que Renato combine filtros (tipo, local, profissional, período, HD, status) para descobrir correlações. "Quantos casamentos de dia fiz num local X" é um exemplo de pergunta possível, não uma métrica fixa a implementar isoladamente.

Não é um produto multi-tenant nem tem camada de autenticação de terceiros — é uma ferramenta pessoal. Guardrails e arquitetura abaixo refletem esse escopo: **simplicidade deliberada**, não descuido.

---

## 2. Arquitetura

### 2.1 Estilo

**Monólito modular em camadas** (Controller → Service → Repository → Entity). Não há justificativa de escala, time ou domínio que sustente microserviços, CQRS ou hexagonal aqui — YAGNI se aplica.

### 2.2 Fluxo de dependência (unidirecional, de cima para baixo)

```
Controller (interface + impl)
        │  delega, não decide
        ▼
Service (@Service, regra de negócio, @Transactional em escrita)
        │  usa
        ▼
Repository (Spring Data JPA)
        │  persiste
        ▼
Entity (JPA, toResponseDto() / toEntity())
```

Regras de fluxo:

- Controller **nunca** chama Repository diretamente.
- Service **nunca** devolve Entity para o Controller — sempre DTO.
- Repository **nunca** contém regra de negócio (apenas queries e derivações Spring Data / `@Query`).

### 2.3 Estrutura de pacotes

```
com.renato.gestaodeeventos
 ├─ controllers/            (interfaces I*Controller — rota + Swagger)
 │   └─ impl/                (implementação — só delega ao service)
 ├─ services/                (regra de negócio, @Transactional)
 ├─ repositories/            (Spring Data JPA)
 ├─ entities/                (JPA, toResponseDto/toEntity)
 ├─ dtos/                     (pacote plano — sem subpacotes request/response; records de entrada com toEntity() e de saída convivem aqui)
 ├─ enums/                    (EventType, DeliveryStatus, HdStatus)
 └─ exceptions/                (NotFoundException abstrata + subclasses por recurso + DomainExceptionHandler — ver seção 7)
```

### 2.4 Padrão Controller (interface + impl)

Mantido como já estabelecido no `Cliente`:

- `I<Nome>Controller` — `@RequestMapping`, `@Validated`, anotações Swagger (`@Tag`, `@Operation`).
- `<Nome>Controller` (`@Component`, em `controllers/impl/`) — implementa a interface, só delega ao service.

`Cliente`/`Customer` é o template de referência até que todos os recursos (`Event`, `Hd`, `Professional`, `EventVenue`) tenham a stack completa.

---

## 3. Modelo de Domínio

### 3.1 Entidades e ajustes decididos

**`Hd` (`TB_HD`)** — precisa dizer onde está e quanto espaço tem, não só "existe":

| Atributo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| name | String | not null — renomeado de `nome` pela GDE-15 |
| capacityGb | Integer | renomear de `capacidade` — já nasce em inglês (GDE-4) |
| usedSpaceGb | Integer | atualizado ao vincular/desvincular `Event` |
| physicalLocation | String | gaveta, estante, "com fulano" |
| serialNumber | String | rastreabilidade em caso de falha |
| acquisitionDate | LocalDate | renomeado de `dataAquisicao` pela GDE-15 |
| status | `HdStatus` (enum) | `ACTIVE`, `FULL`, `DEFECTIVE`, `ARCHIVED` — valores em inglês (ver ADR-0009) |
| events | List\<Event\> | `@OneToMany`, mapped by `hd` |

**`Event` (`TB_EVENTO`)** — `type` vira enum; adiciona status de entrega e tamanho:

| Atributo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| eventCode | String | chave de correlação com nome de pasta/arquivo no HD — renomeado de `codigoDoEvento` pela GDE-15 |
| type | `EventType` (`@ManyToOne`) | tabela de lookup desde a ADR-0016 — deixou de ser enum fixo para poder cadastrar tipos novos via API sem deploy; renomeado de `tipo` pela GDE-15 |
| name | String | not null — renomeado de `nome` pela GDE-15 |
| eventDate | LocalDate | renomeado de `dataDoEvento` pela GDE-15 |
| daytimeWedding | Boolean | **nullable, só relevante se `type = WEDDING`** — ver 3.2; renomeado de `casamentoDeDia` pela GDE-15 |
| outdoorWedding | Boolean | idem; renomeado de `casamentoExterno` pela GDE-15 |
| guestCount | Long | renomeado de `quantidadeDeConvidados` pela GDE-15 |
| description | String | renomeado de `descricao` pela GDE-15 |
| amount | Double | renomeado de `valor` pela GDE-15 |
| sizeGb | Integer | alimenta `Hd.usedSpaceGb` — já nasce em inglês |
| deliveryStatus | `DeliveryStatus` (enum) | `PENDING`, `DELIVERED`, `ARCHIVED` — valores em inglês (ver ADR-0009); já nasce em inglês |
| hd | Hd | `@ManyToOne` |
| eventVenue | EventVenue | `@ManyToOne` |
| customer | Customer | `@ManyToOne` |
| professionals | List\<EventProfessional\> | ver 3.3; renomeado de `profissionais` pela GDE-15 |

**`Customer` e `EventVenue`**: mantidos como estão — já atendem às métricas descritas (cidade/estado para "casamentos por local").

**`Professional` (`TB_PROFESSIONAL`)** — `type` e `specialty` eram texto livre; viraram lookup pela ADR-0022, porque texto livre não permite filtro confiável (ex.: "cerimonialista" grafado de formas diferentes não bate numa busca):

| Atributo | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| name | String | not null |
| type | `ProfessionalType` (`@ManyToOne`) | um por profissional (cardinalidade 1) — tabela de lookup própria (`TB_PROFESSIONAL_TYPE`), CRUD em `/api/professional-types`; texto livre até a ADR-0022 |
| contact | String | texto livre |
| specialtyTags | Set\<SpecialtyTag\> | vários por profissional (cardinalidade N) — `@ManyToMany` via `TB_PROFESSIONAL_SPECIALTY`, CRUD em `/api/specialty-tags`; texto livre único até a ADR-0022 |
| otherInfo | String | observações livres |
| eventProfessionals | List\<EventProfessional\> | ver 3.3 |

### 3.2 Decisão explícita: campos específicos de casamento no `Event` genérico

`daytimeWedding`/`outdoorWedding` (`casamentoDeDia`/`casamentoExterno` antes da GDE-15) ficam no `Event` (não em subtipo/tabela separada) por decisão consciente de simplicidade — modelar subtipo (herança JPA, tabela própria) seria over-engineering para o volume e o problema atuais. **Débito técnico aceito e documentado**, não ignorado: se um dia surgir um segundo tipo de evento com atributos próprios, reavaliar.

### 3.3 Join `Event`↔`Professional` vira entidade própria

Troca de `@ManyToMany` puro por `EventProfessional` (`TB_EVENT_PROFESSIONAL`), permitindo registrar em que função o profissional atuou. É tabela nova — nasce inteiramente em inglês, sem débito para o GDE-15:

```java
public record EventProfessionalId(UUID eventId, UUID professionalId) implements Serializable {}

@Entity(name = "TB_EVENT_PROFESSIONAL")
public class EventProfessional {
    @EmbeddedId
    private EventProfessionalId id;

    @ManyToOne @MapsId("eventId")
    private Event event;

    @ManyToOne @MapsId("professionalId")
    private Professional professional;

    @Column(name = "role_in_event")
    private String roleInEvent; // "segundo fotógrafo", "videomaker", "drone"
}
```

Esta seção descreve só a entidade. Até a ADR-0021 (2026-09-28), só existia o lado de leitura — nunca houve caminho de escrita (`TB_EVENT_PROFESSIONAL` sempre esteve vazia). A ADR-0021 fechou a escrita via `CreateEventDto.professionals` (embutido, replace-all); `roleInEvent` continua texto livre por decisão consciente do Renato (débito registrado na própria ADR).

---

## 4. Convenções (já estabelecidas — manter)

- **Inglês é o idioma padrão da aplicação.** A migração PT→EN foi finalizada pela task dedicada **GDE-15** (mergeada em `develop`): entidades, DTOs, repositories, services e controllers estão todos em inglês, e os nomes de tabela/coluna no banco também já foram renomeados. Não há mais débito de nomenclatura PT→EN a rastrear; todo campo/classe novo já nasce em inglês.
- **Conversão DTO↔Entity é manual**, via `toResponseDto()`/`toDTO()` na entidade e `toEntity()` no DTO de criação (record + `@Builder` Lombok na entidade). Não introduzir MapStruct/ModelMapper.
- **Flyway está em uso (GDE-14, mergeado); `ddl-auto=validate`.** Toda alteração de schema (novos campos, nova entidade, renomeações) precisa vir acompanhada de um script de migration versionado em `src/main/resources/db/migration`, não apenas da mudança na entidade — o boot falha na validação do Hibernate caso contrário.
- **IDs sempre `UUID`, `GenerationType.AUTO`.**

---

## 5. Programação Funcional — Diretrizes

Java/Spring não é uma linguagem funcional pura, e **entidades JPA não podem ser imutáveis** (Hibernate exige setters/proxy). O objetivo aqui não é fingir Haskell — é reduzir mutação e efeitos colaterais onde isso não conflita com JPA, e tornar o código mais declarativo.

### 5.1 Onde aplicar (com exemplos)

**a) DTOs são sempre imutáveis (`record`)** — já é a convenção do projeto; manter sem exceção. Nunca criar um DTO como classe mutável.

**b) Substituir `null` por `Optional<T>` nas assinaturas de service que podem não encontrar algo**, mas *nunca* como campo de entidade ou DTO (`Optional` não é serializável e não deve vazar para fora do service):

```java
// Service
public Optional<EventResponseDto> findById(UUID id) {
    return repository.findById(id).map(Event::toResponseDto);
}

// Controller impl
public ResponseEntity<EventResponseDto> get(UUID id) {
    return service.findById(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new EventNotFoundException(id));
}
```

**c) Stream API para toda transformação/filtragem de coleção** — proibido `for` imperativo com `list.add()` quando o resultado é uma projeção/filtro de outra coleção. Exemplo puramente ilustrativo do idioma (não é uma métrica fixa a implementar — combinações de filtro reais devem usar o mecanismo composável da seção 5.1-f):

```java
public long countDaytimeWeddingsByVenue(UUID venueId) {
    return eventRepository.findByEventVenueId(venueId).stream()
        .filter(e -> e.getType() == EventType.WEDDING)
        .filter(e -> Boolean.TRUE.equals(e.getCasamentoDeDia()))
        .count();
}
```

**d) `switch` de padrões (Java 21) em vez de cadeias de `if/else` sobre enum/tipo**, especialmente para `EventType` e `DeliveryStatus`:

```java
String descricaoStatus = switch (event.getDeliveryStatus()) {
    case PENDENTE -> "Aguardando gravação em HD";
    case ENTREGUE -> "Entregue ao cliente";
    case ARQUIVADO -> "Arquivado";
};
```

**e) Funções puras para cálculo, isoladas de I/O.** Regra de negócio que não depende de repositório (ex.: calcular se um HD está perto da capacidade) vira método estático/utilitário puro, testável sem mock:

```java
public final class HdCapacityPolicy {
    private HdCapacityPolicy() {}

    public static boolean isNearCapacity(Hd hd) {
        return hd.getUsedSpaceGb() >= hd.getCapacityGb() * 0.9;
    }
}
```

O service chama essa função pura e só ele lida com o efeito colateral (salvar, notificar etc.).

**f) Composição de filtros de busca com `Function`/`Predicate`** em vez de um método de repositório por combinação de filtro. **Este é o mecanismo preferido para atender à necessidade de negócio de filtros livres/correlações ad-hoc** (ver PRD, seção 5) — cada novo critério vira um `EventFilter` combinável, em vez de um método/endpoint novo por combinação:

```java
public interface EventFilter extends Predicate<Event> {
    static EventFilter byType(EventType type) {
        return e -> type == null || e.getType() == type;
    }
    static EventFilter byVenue(UUID venueId) {
        return e -> venueId == null || e.getEventVenue().getId().equals(venueId);
    }
    static EventFilter byProfessional(UUID professionalId) { /* ... */ return e -> true; }
    static EventFilter byPeriod(LocalDate from, LocalDate to) { /* ... */ return e -> true; }
    // novos critérios entram aqui como mais um factory method, combináveis entre si
}
// uso: events.stream().filter(byType(WEDDING).and(byVenue(id)).and(byPeriod(from, to)))
```

Um endpoint de consulta genérico (ex.: `GET /events/search` aceitando os parâmetros opcionais tipo/local/profissional/período/HD/status) que monta a combinação de `EventFilter` a partir da query string é preferível a criar um endpoint/métrica fixo por pergunta de negócio.

### 5.2 Onde **não** forçar

- Entidades JPA continuam mutáveis (setters do Hibernate) — não tentar `record` em `@Entity`.
- Não introduzir bibliotecas de FP pesadas (Vavr, functional-java) para um projeto pessoal deste porte — os idiomas nativos do Java 21 (Streams, Optional, `switch` de padrões, `record`) são suficientes. Reavaliar apenas se a complexidade de tratamento de erro (seção 7) crescer muito.

---

## 6. Guardrails (restrições inegociáveis de desenvolvimento)

1. **Não fazer rename em massa PT↔EN** como efeito colateral de qualquer tarefa. Mudança de nomenclatura é a task própria e explícita **GDE-15**.
2. **Controller nunca contém lógica de negócio** — só validação de entrada (`@Validated`) e delegação ao service.
3. **Toda operação de escrita no service é `@Transactional`.**
4. **Toda transformação de coleção usa Stream API** — não aceitar `for` com `.add()` manual em código novo (seção 5.1-c).
5. **Nenhum campo `Optional` em entidade ou DTO** — `Optional` só em retorno de método de service.
6. **Nova entidade/enum passa primeiro pela seção 3 deste documento antes de virar código** — mudança de schema não documentada aqui é débito técnico não rastreado.
7. **Sem MapStruct/ModelMapper/Lombok `@Data`** (Lombok `@Builder`/`@Getter`/`@Setter` pontuais são aceitos, seguindo o que já existe).
8. **Sem dependência nova sem necessidade concreta** — este é um projeto pessoal de baixo volume; qualquer lib adicionada (fila, cache, etc.) exige justificar por que o problema não se resolve com o que já está no stack.
9. **`Cliente`/`Customer` é o template obrigatório** ao construir controller/service para `Event`, `Hd`, `Professional`, `EventVenue` — não inventar um padrão novo por recurso.
10. **Toda decisão que desviar deste documento deve ser registrada na seção 9 (ADR)**, não apenas codificada silenciosamente.
11. **Nenhum escopo novo (feature, entidade/campo, endpoint, dependência) entra sem aprovação explícita do Renato.** Sugestões e ideias são bem-vindas — devem ser propostas e aguardar validação antes de virar código, nunca implementadas como efeito colateral de outra tarefa.

---

## 7. Tratamento de Erros (implementado na GDE-7, mergeada em `develop`)

A implementação real diverge um pouco da proposta original desta seção (mantida abaixo, riscada, para histórico): em vez de uma `sealed interface DomainError` com `NotFoundError`/`ValidationError` como records, o projeto usa uma classe abstrata `exceptions/NotFoundException` com uma subclasse por recurso, e um `@RestControllerAdvice` único:

```java
public abstract class NotFoundException extends RuntimeException {
    protected NotFoundException(String resource, UUID id) {
        super("%s not found with id: %s".formatted(resource, id));
    }
}
// subclasses por recurso: CustomerNotFoundException, EventNotFoundException,
// EventVenueNotFoundException, HdNotFoundException, ProfessionalNotFoundException
```

- Exceções de "não encontrado" usam exceção (é o caso idiomático em Spring), sempre **específicas** por recurso (`CustomerNotFoundException`, `EventNotFoundException`, `EventVenueNotFoundException`, `HdNotFoundException`, `ProfessionalNotFoundException`), nunca `RuntimeException` genérica — a única exceção é o `CustomerService` original, que ainda lança `RuntimeException` diretamente; isso é débito documentado, não o padrão a seguir em código novo.
- Um único `exceptions/DomainExceptionHandler` (`@RestControllerAdvice`, estende `ResponseEntityExceptionHandler`) mapeia `NotFoundException` para `404` e erros de validação de `@Valid`/`@Validated` (via `handleMethodArgumentNotValid`) para `400`, ambos como `ApiErrorDto`. Não há um tipo `ValidationError` separado — a mensagem de validação é montada a partir dos `FieldError`/`ObjectError` do `BindingResult`.
- Novos recursos devem seguir esse padrão: uma subclasse de `NotFoundException` por entidade, nunca `RuntimeException` genérica.

<details>
<summary>Proposta original (histórico, pré-GDE-7)</summary>

```java
public sealed interface DomainError permits NotFoundError, ValidationError {}
public record NotFoundError(String resource, UUID id) implements DomainError {}
public record ValidationError(String field, String message) implements DomainError {}
```

</details>

---

## 8. Testes (mínimo aceitável)

- Funções puras (seção 5.1-e) e composições de `Predicate` (5.1-f): teste unitário simples, sem mocks — é o ganho concreto de isolar lógica de I/O.
- Service: teste unitário com repositório mockado para o caminho feliz e o caminho "não encontrado".
- Sem exigência de cobertura de integração completa neste momento — projeto pessoal, prioridade é a entidade `Event`/`Hd` funcionando corretamente.

---

## 9. ADR — Decisões Registradas

Cada decisão arquitetural vive em seu próprio documento, um por ADR, em `docs/adr/` (formato: Data/Status/Contexto/Decisão/Racional). Esta seção é só o índice — não duplicar o conteúdo aqui.

| ADR | Decisão |
|---|---|
| [0001](adr/0001-casamento-fields-sem-subtipo.md) | Manter `daytimeWedding`/`outdoorWedding` no `Event` genérico, sem subtipo |
| [0009](adr/0009-enum-values-em-ingles.md) | Valores de enum (`EventType`/`DeliveryStatus`/`HdStatus`) em inglês, não em português |
| [0002](adr/0002-event-professional-entidade-associacao.md) | `Event`↔`Professional` vira entidade `EventProfessional` com `roleInEvent` |
| [0003](adr/0003-sem-biblioteca-fp-externa.md) | Sem biblioteca de FP externa (Vavr etc.) |
| [0004](adr/0004-tratamento-erro-debito-tecnico.md) | Tratamento de erro fica com débito técnico documentado, não resolvido agora |
| [0005](adr/0005-metricas-via-filtros-composaveis.md) | Métricas de negócio via `EventFilter` composável / endpoint de busca genérico, não endpoints fixos |
| [0006](adr/0006-adotar-flyway.md) | Adotar Flyway para migrations; `ddl-auto` passa de `update` para `validate` (GDE-14) |
| [0007](adr/0007-ingles-idioma-padrao-migracao-pt-en.md) | Inglês vira idioma padrão da aplicação; migração PT→EN existente será finalizada (GDE-15) |
| [0008](adr/0008-sequenciamento-gde15-antes-gde14.md) | Sequenciamento: GDE-15 (rename PT→EN) roda antes de GDE-14 (Flyway) |
| [0010](adr/0010-squash-migrations-antes-do-primeiro-deploy.md) | Squash de migrations é permitido até o primeiro deploy real; depois disso, nunca mais reescrever migration já aplicada |
| [0011](adr/0011-response-dtos-sem-entidade-jpa-direta.md) | DTOs de resposta nunca embutem entidade JPA: coleção vira `List<XSummaryDto>`, referência singular vira `UUID` |
| [0012](adr/0012-stack-frontend-react-ts-vite.md) | Frontend em React + TypeScript + Vite (SPA), monorepo em `frontend/`, build estático servido pelo próprio Spring Boot |
| [0013](adr/0013-ci-github-actions.md) | CI via GitHub Actions rodando `./mvnw clean test` em push/PR para `develop`/`main`, com serviço Postgres no job |
| [0014](adr/0014-spring-profiles-dev-prod.md) | Configuração separada por Spring profiles: `dev` (padrão local) e `prod` (variáveis de ambiente) |
| [0015](adr/0015-dockerfile-backend.md) | Backend containerizado via Dockerfile multi-stage (build com `mvnw`, runtime só com JRE) |
| [0016](adr/0016-event-type-tabela-lookup.md) | `Event.type` migra de enum fixo para tabela de lookup (`EventType`), cadastrável via API sem deploy |
| [0017](adr/0017-autenticacao-spring-security-jwt.md) | Autenticação via Spring Security + JWT, usuário único, sem self-service de "esqueci minha senha" |
| [0018](adr/0018-versionamento-releases-tag-automatica.md) | Versionamento de release: tag git automática no merge para `main` |
| [0019](adr/0019-stack-teste-frontend-vitest.md) | Stack de teste do frontend: Vitest + React Testing Library + jsdom |
| [0020](adr/0020-jwt-em-sessionstorage.md) | Token JWT do frontend fica em `sessionStorage`, não em cookie `httpOnly` |
| [0021](adr/0021-escrita-associacao-event-professional.md) | Escrita da associação `Event`↔`Professional`: embutida no `CreateEventDto`, replace-all, sem cascade JPA |
| [0022](adr/0022-professional-type-e-specialty-tags.md) | `Professional.type` vira lookup (`ProfessionalType`, 1-pra-N) e `Professional.specialty` vira tags (`SpecialtyTag`, N-pra-N) |

Nova decisão arquitetural → novo arquivo `docs/adr/NNNN-slug.md` (próximo número sequencial) + uma linha nova nesta tabela.

---

## 10. Próximos Passos Técnicos

Backlog completo com critérios de aceite, dependências e prioridade vive no Jira (board "Gestão de Eventos", key `GDE`). Resumo:

- [x] GDE-1 Corrigir encoding de `application.properties` (concluído 2026-09-22, direto em `develop`) — além do encoding, foi encontrado e corrigido um segundo bloqueador: o `JAVA_HOME` padrão da máquina aponta para JDK 25, que quebra silenciosamente o annotation processing do Lombok 1.18.32 (nenhum `builder()`/getter/setter é gerado). Ver nota em `CLAUDE.md` — build/testes precisam rodar com JDK 21.
- [x] GDE-2 Testes unitários para `CustomerService` (renomeado de `ClienteService` pela GDE-15)
- [x] GDE-15 Finalizar migração de nomenclatura PT→EN — concluído em `develop`: entidades, DTOs, repositories, services, controllers e os nomes de tabela/coluna no banco já estão todos em inglês
- [x] GDE-14 Introduzir Flyway (`ddl-auto` → `validate`)
- [x] GDE-3 Criar enums `EventType`, `DeliveryStatus`, `HdStatus`
- [x] GDE-4 Adicionar `physicalLocation`, `usedSpaceGb`, `serialNumber`, `capacityGb` em `Hd`
- [ ] GDE-5 Adicionar `sizeGb`, `deliveryStatus` em `Event`
- [x] GDE-6 Criar entidade `EventProfessional` substituindo o `@ManyToMany` puro
- [x] GDE-7 Exceções específicas + `@RestControllerAdvice` (seção 7) — implementado
- [ ] GDE-8..11 Controller (interface+impl) + Service para `Event`, `Hd`, `Professional`, `EventVenue`, usando `Customer` como template — `Professional` (GDE-10) e `EventVenue` (GDE-11) já têm controller+service completos; `Event` (GDE-8) e `Hd` (GDE-9) ainda faltam
- [ ] GDE-12 `EventFilter` (5.1-f) e endpoint de busca genérico (`GET /events/search`) — em vez de endpoints fixos por métrica
- [ ] GDE-13 `HdCapacityPolicy` (função pura) e endpoint de alerta "HD perto da capacidade"
