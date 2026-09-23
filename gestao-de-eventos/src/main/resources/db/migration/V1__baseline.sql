-- Baseline schema for gestao-de-eventos, generated from the JPA entities as they
-- exist post GDE-15 (PT->EN rename), GDE-4 (Hd fields), GDE-6 (EventProfessional
-- association entity) and GDE-16 (Event.id as uuid).
--
-- This project has no production deployment and no environment with real data
-- yet, so this single baseline is kept in sync with the entities directly
-- instead of layering a V2/V3/... on top of a baseline that was already wrong
-- the day it was written. Once a real environment exists, this stops being an
-- option and new schema changes go into their own versioned migration instead
-- (see docs/adr/0010-squash-migrations-antes-do-primeiro-deploy.md).

create table tb_customer (
    id uuid not null,
    name varchar(255) not null,
    contact varchar(255),
    address varchar(255),
    notes varchar(255),
    primary key (id)
);

create table tb_hd (
    id uuid not null,
    name varchar(255) not null,
    capacity_gb integer,
    used_space_gb integer,
    physical_location varchar(255),
    serial_number varchar(255),
    acquisition_date date,
    status varchar(255),
    primary key (id)
);

create table tb_event_venue (
    id uuid not null,
    name varchar(255) not null,
    address varchar(255),
    city varchar(255),
    state varchar(255),
    type varchar(255),
    primary key (id)
);

create table tb_professional (
    id uuid not null,
    name varchar(255) not null,
    type varchar(255),
    contact varchar(255),
    specialty varchar(255),
    other_info varchar(255),
    primary key (id)
);

create table tb_event (
    id uuid not null,
    event_code varchar(255),
    type varchar(255),
    name varchar(255) not null,
    event_date date,
    daytime_wedding boolean,
    outdoor_wedding boolean,
    guest_count bigint,
    description varchar(255),
    amount float(53),
    hd_id uuid,
    event_venue_id uuid,
    customer_id uuid,
    primary key (id)
);

create table tb_event_professional (
    event_id uuid not null,
    professional_id uuid not null,
    role_in_event varchar(255),
    primary key (event_id, professional_id)
);

alter table if exists tb_event
    add constraint fk_event_hd foreign key (hd_id) references tb_hd;

alter table if exists tb_event
    add constraint fk_event_event_venue foreign key (event_venue_id) references tb_event_venue;

alter table if exists tb_event
    add constraint fk_event_customer foreign key (customer_id) references tb_customer;

alter table if exists tb_event_professional
    add constraint fk_event_professional_event foreign key (event_id) references tb_event;

alter table if exists tb_event_professional
    add constraint fk_event_professional_professional foreign key (professional_id) references tb_professional;
