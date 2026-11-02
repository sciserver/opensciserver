-- DBDomain
ALTER TABLE DBContext DROP CONSTRAINT fk_dbcontext_domain_id;
ALTER TABLE DBDomain DROP CONSTRAINT pk_dbdomain;
GO

ALTER TABLE DBDomain DROP COLUMN domain_id;
ALTER TABLE DBDomain ADD id bigint Identity(1,1) NOT NULL;
ALTER TABLE DBDomain ADD CONSTRAINT pk_dbdomain_id PRIMARY KEY (id);
GO

ALTER TABLE DBDomain ADD racm_id bigint NULL;
GO

UPDATE DBDomain SET racm_id = racm_domain_uuid;
GO

ALTER TABLE DBDomain ALTER COLUMN racm_id bigint NOT NULL;
ALTER TABLE DBDomain ADD CONSTRAINT uq_dbdomain_racm_id UNIQUE (racm_id);
GO

EXEC sp_rename 'DBDomain.racm_domain_uuid', 'racm_uuid', 'COLUMN';  -- These values are incorrect for now, refresh after app startup
GO

-- DBContext
ALTER TABLE DBContext ADD CONSTRAINT pk_dbcontext_id PRIMARY KEY (id);
ALTER TABLE DBContext ADD CONSTRAINT fk_dbcontext_domain_id FOREIGN KEY (domain_id) REFERENCES DBDomain (id) ON DELETE CASCADE;
GO

EXEC sp_rename 'DBContext.racm_resource_uuid', 'racm_uuid', 'COLUMN';
GO

ALTER TABLE DBContext ALTER COLUMN racm_uuid varchar(64) NOT NULL;
GO

ALTER TABLE DBContext ADD CONSTRAINT uq_dbcontext_racm_uuid UNIQUE (racm_uuid);
GO

ALTER TABLE DBContext ADD racm_id bigint NULL;
GO

-- Need to verify the existing data with this script
UPDATE DBContext SET racm_id = 4 WHERE racm_uuid = '1ae29820-d957-4ae9-a37f-af71d891e390';
UPDATE DBContext SET racm_id = 14 WHERE racm_uuid = '0705454a-8dc0-4ad3-aeec-9ea9546e3d3f';
UPDATE DBContext SET racm_id = 17 WHERE racm_uuid = 'aa6d534e-f4fd-45c5-ac23-4e6a34eaf1e6';
UPDATE DBContext SET racm_id = 16 WHERE racm_uuid = '3a2b3a91-b536-4bec-a439-4aa061ab7f04';
UPDATE DBContext SET racm_id = 15 WHERE racm_uuid = '15fd6852-f684-4401-91a4-fec76bb1aeab';
GO

ALTER TABLE DBContext ALTER COLUMN racm_id bigint NOT NULL;
ALTER TABLE DBContext ADD CONSTRAINT uq_dbcontext_racm_id UNIQUE (racm_id);
GO

ALTER TABLE DBContext ADD racm_owner varchar(64) NULL;
GO

-- DBSchema
CREATE TABLE dbo.DBSchema (
    id bigint Identity(1,1) NOT NULL,
    database_id bigint NOT NULL,
	racm_id bigint NOT NULL,
    racm_uuid varchar(64) NOT NULL,
    schema_name varchar(255) NOT NULL,
	description varchar(max) NULL
);
GO

ALTER TABLE DBSchema ADD CONSTRAINT pk_dbschema_id PRIMARY KEY (id);
ALTER TABLE DBSchema ADD CONSTRAINT uq_dbschema_racm_id UNIQUE (racm_id);
ALTER TABLE DBSchema ADD CONSTRAINT uq_dbschema_racm_uuid UNIQUE (racm_uuid);

ALTER TABLE DBSchema ADD CONSTRAINT fk_dbschema_database_id FOREIGN KEY (database_id) REFERENCES DBContext (id) ON DELETE CASCADE;
GO
