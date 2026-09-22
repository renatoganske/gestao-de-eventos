-- GDE-4: V1__baseline.sql was generated before this ticket landed, so it still
-- has Hd's old Portuguese field. Bring the column names in line with the
-- Hd entity as it exists today.
alter table tb_hd rename column capacidade to capacity_gb;
alter table tb_hd add column used_space_gb integer;
alter table tb_hd add column physical_location varchar(255);
alter table tb_hd add column serial_number varchar(255);

-- GDE-6: the old Event<->Professional @ManyToMany join table was superseded
-- by the TB_EVENT_PROFESSIONAL association entity, but V1__baseline.sql still
-- creates the old table (it also predates GDE-6). Drop it before recreating
-- the relationship properly below.
alter table if exists event_professional drop constraint if exists fk_event_professional_event;
alter table if exists event_professional drop constraint if exists fk_event_professional_professional;
drop table if exists event_professional;

-- GDE-16: Event.id was the only entity id using VARCHAR(36) instead of uuid,
-- which made it impossible to add a real foreign key from
-- tb_event_professional to tb_event.
alter table tb_event alter column id type uuid using id::uuid;

-- GDE-6: create the EventProfessional association table matching the
-- EventProfessional/EventProfessionalId entities.
create table tb_event_professional (
    event_id uuid not null,
    professional_id uuid not null,
    role_in_event varchar(255),
    primary key (event_id, professional_id)
);

alter table tb_event_professional
    add constraint fk_event_professional_event foreign key (event_id) references tb_event;

alter table tb_event_professional
    add constraint fk_event_professional_professional foreign key (professional_id) references tb_professional;
