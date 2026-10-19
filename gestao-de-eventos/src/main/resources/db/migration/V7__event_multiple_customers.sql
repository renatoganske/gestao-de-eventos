-- GDE-49 / ADR-0025: o evento passa a ter varios clientes (cada pessoa com o proprio
-- cadastro), um deles marcado como contratante, em vez de um unico tb_event.customer_id.
create table tb_event_customer (
    event_id uuid not null,
    customer_id uuid not null,
    contracting boolean not null default false,
    primary key (event_id, customer_id)
);

alter table if exists tb_event_customer
    add constraint fk_event_customer_event foreign key (event_id) references tb_event;

alter table if exists tb_event_customer
    add constraint fk_event_customer_customer foreign key (customer_id) references tb_customer;

-- No maximo um contratante por evento.
create unique index uk_event_customer_contracting
    on tb_event_customer (event_id)
    where contracting;

-- Preserva o vinculo atual: o unico cliente de cada evento vira o contratante.
insert into tb_event_customer (event_id, customer_id, contracting)
select id, customer_id, true
from tb_event
where customer_id is not null;

alter table tb_event drop constraint fk_event_customer;
alter table tb_event drop column customer_id;
