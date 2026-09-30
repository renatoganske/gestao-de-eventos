-- GDE-48 / ADR-0024: o tipo de evento passa a dizer se seus eventos usam os campos de
-- casamento (diurno / ao ar livre), em vez de o codigo depender do nome fixo 'WEDDING'.
alter table tb_event_type
    add column has_wedding_fields boolean not null default false;

-- Preserva o comportamento atual: o tipo 'WEDDING' (semeado na V2) ja usava esses campos.
-- Outros tipos de casamento (ex.: 'Casamento', 'Mini Wedding') sao marcados pela API/tela.
update tb_event_type
set has_wedding_fields = true
where name = 'WEDDING';
