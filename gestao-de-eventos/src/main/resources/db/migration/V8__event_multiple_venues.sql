-- GDE-51 / ADR-0026: o evento passa a ter varios locais (cerimonia, recepcao, making-of...),
-- cada um com um papel opcional, em vez de um unico tb_event.event_venue_id.
create table tb_event_event_venue (
    event_id uuid not null,
    event_venue_id uuid not null,
    role varchar(255),
    primary key (event_id, event_venue_id)
);

alter table if exists tb_event_event_venue
    add constraint fk_event_event_venue_event foreign key (event_id) references tb_event;

alter table if exists tb_event_event_venue
    add constraint fk_event_event_venue_venue foreign key (event_venue_id) references tb_event_venue;

-- Preserva o vinculo atual: o unico local de cada evento continua ligado a ele, sem papel
-- (nao ha como saber se era a cerimonia ou a festa).
insert into tb_event_event_venue (event_id, event_venue_id, role)
select id, event_venue_id, null
from tb_event
where event_venue_id is not null;

alter table tb_event drop constraint fk_event_event_venue;
alter table tb_event drop column event_venue_id;
