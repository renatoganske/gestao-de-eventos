-- ADR-0017: autenticacao via Spring Security + JWT, usuario unico, sem
-- self-service de "esqueci minha senha" -- recuperacao de acesso e via
-- runbook manual (UPDATE direto nesta tabela), nao por um fluxo de e-mail.

create table tb_app_user (
    id uuid not null,
    username varchar(255) not null,
    password_hash varchar(255) not null,
    primary key (id)
);

alter table tb_app_user
    add constraint uk_app_user_username unique (username);
