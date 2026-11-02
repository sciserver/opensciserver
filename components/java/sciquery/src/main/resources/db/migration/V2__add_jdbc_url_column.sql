-- add jdbcURL column to DBContext database
alter table DBContext add jdbcURL varchar(1024)

alter table DBDomain add constraint pk_dbdomain primary key(domain_id)

alter table DBContext add constraint fk_dbcontext_domain_id FOREIGN KEY(domain_id)
      REFERENCES DBDomain (domain_id)
      ON DELETE CASCADE

GO