-- GDE-38: capacidade real (utilizavel apos formatacao) do HD; capacity_gb segue nominal.
alter table tb_hd add column real_capacity_gb integer;
