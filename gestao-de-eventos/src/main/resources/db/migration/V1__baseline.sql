-- Baseline schema for gestao-de-eventos, generated from the (already English-renamed,
-- post-GDE-15) JPA entities via Hibernate's schema-generation-to-script tooling.
-- Hd.capacidade is intentionally still Portuguese here — GDE-4 owns renaming/retyping it.

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
    capacidade integer,
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
    id VARCHAR(36) not null,
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

create table event_professional (
    event_id VARCHAR(36) not null,
    professional_id uuid not null
);

alter table if exists tb_event
    add constraint fk_event_hd foreign key (hd_id) references tb_hd;

alter table if exists tb_event
    add constraint fk_event_event_venue foreign key (event_venue_id) references tb_event_venue;

alter table if exists tb_event
    add constraint fk_event_customer foreign key (customer_id) references tb_customer;

alter table if exists event_professional
    add constraint fk_event_professional_event foreign key (event_id) references tb_event;

alter table if exists event_professional
    add constraint fk_event_professional_professional foreign key (professional_id) references tb_professional;
