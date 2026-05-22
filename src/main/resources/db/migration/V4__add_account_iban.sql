alter table accounts add column iban varchar(34);

update accounts
set iban = 'TR62' || '00062' || lpad(id::text, 17, '0')
where iban is null;

alter table accounts alter column iban set not null;

alter table accounts add constraint uk_accounts_iban unique (iban);
