-- GDE-5: adds sizeGb and deliveryStatus to Event, to track the job's
-- lifecycle and feed the HD used-space calculation. tb_event.type already
-- exists as varchar (storing the enum name as a string), so no ALTER is
-- needed for it here -- only the Java annotation changes from a plain
-- String to @Enumerated(EnumType.STRING) EventType.

alter table tb_event add column size_gb integer;
alter table tb_event add column delivery_status varchar(255);
