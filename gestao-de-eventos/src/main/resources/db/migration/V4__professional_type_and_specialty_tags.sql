-- ADR-0022: Professional.type e Professional.specialty deixam de ser texto livre.
--
-- `type` (cardinalidade 1) vira `ProfessionalType`, mesmo padrao de lookup da
-- ADR-0016 (EventType): tabela nova + FK. `specialty` (cardinalidade N) vira
-- `SpecialtyTag`, relacao N-para-N via tabela de juncao, ja que um profissional
-- pode ter varias especialidades.
--
-- Faz backfill dos dois a partir do texto existente em vez de so dropar as
-- colunas -- nao da pra assumir que o banco de producao (Neon) esta vazio
-- so porque o local esta.

create table tb_professional_type (
    id uuid not null,
    name varchar(255) not null,
    primary key (id)
);

alter table tb_professional_type
    add constraint uk_professional_type_name unique (name);

create table tb_specialty_tag (
    id uuid not null,
    name varchar(255) not null,
    primary key (id)
);

alter table tb_specialty_tag
    add constraint uk_specialty_tag_name unique (name);

create table tb_professional_specialty (
    professional_id uuid not null,
    specialty_tag_id uuid not null,
    primary key (professional_id, specialty_tag_id)
);

alter table tb_professional_specialty
    add constraint fk_professional_specialty_professional foreign key (professional_id) references tb_professional;

alter table tb_professional_specialty
    add constraint fk_professional_specialty_tag foreign key (specialty_tag_id) references tb_specialty_tag;

-- Backfill de `type`: uma linha de lookup por valor distinto ja usado.
insert into tb_professional_type (id, name)
select gen_random_uuid(), distinct_type
from (select distinct type as distinct_type from tb_professional where type is not null and trim(type) <> '') t;

alter table tb_professional
    add column professional_type_id uuid;

update tb_professional p
set professional_type_id = pt.id
from tb_professional_type pt
where p.type = pt.name;

alter table tb_professional
    add constraint fk_professional_professional_type foreign key (professional_type_id) references tb_professional_type;

alter table tb_professional
    drop column type;

-- Backfill de `specialty`: uma tag por valor distinto ja usado, associada a
-- cada profissional que tinha aquele valor. `specialty` sempre foi um campo
-- unico (nao ha convencao de lista/virgula em uso), entao o valor inteiro
-- vira uma unica tag -- nao ha separacao heuristica por virgula aqui.
insert into tb_specialty_tag (id, name)
select gen_random_uuid(), distinct_specialty
from (select distinct specialty as distinct_specialty from tb_professional where specialty is not null and trim(specialty) <> '') t;

insert into tb_professional_specialty (professional_id, specialty_tag_id)
select p.id, st.id
from tb_professional p
join tb_specialty_tag st on p.specialty = st.name
where p.specialty is not null and trim(p.specialty) <> '';

alter table tb_professional
    drop column specialty;
