-- ADR-0016: Event.type deixa de ser um enum fixo e vira uma tabela de lookup,
-- permitindo cadastrar novos tipos de evento via API sem precisar de deploy.
--
-- Esta e a primeira migration versionada desde o primeiro deploy real (Render + Neon,
-- ver ADR-0014/0015) -- a partir de agora, mudanca de schema nunca mais edita
-- V1__baseline.sql (ver ADR-0010).

create table tb_event_type (
    id uuid not null,
    name varchar(255) not null,
    primary key (id)
);

alter table tb_event_type
    add constraint uk_event_type_name unique (name);

-- Preserva os quatro valores que o enum EventType ja tinha, para nao perder
-- compatibilidade com quem ja consumia a API antes desta migration.
insert into tb_event_type (id, name)
values
    (gen_random_uuid(), 'PHOTO_SHOOT'),
    (gen_random_uuid(), 'BIRTHDAY'),
    (gen_random_uuid(), 'WEDDING'),
    (gen_random_uuid(), 'OTHER');

-- Cobre qualquer valor de tb_event.type que ja exista e nao esteja nos quatro acima
-- (nao deveria acontecer, ja que o enum so permitia esses quatro, mas evita perder
-- dado se algum evento real ja foi criado com um type fora do esperado).
insert into tb_event_type (id, name)
select gen_random_uuid(), distinct_type
from (select distinct type as distinct_type from tb_event where type is not null) t
where distinct_type not in (select name from tb_event_type);

alter table tb_event
    add column event_type_id uuid;

update tb_event e
set event_type_id = et.id
from tb_event_type et
where e.type = et.name;

alter table tb_event
    drop column type;

alter table tb_event
    add constraint fk_event_event_type foreign key (event_type_id) references tb_event_type;
