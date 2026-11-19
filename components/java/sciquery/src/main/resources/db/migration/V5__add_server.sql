-- DBServer
CREATE TABLE dbo.DBServer (
    id bigint Identity(1,1) NOT NULL,
    server_name varchar(255) NOT NULL,
	description varchar(max) NULL,
	host varchar(255) NOT NULL, -- MUST NOT have a port
	port integer NULL,
	vendor varchar(16) NOT NULL,  -- MSSQL MYSQL POSTGRES DB2 ORACLE (see racm enum)
	vendor_version varchar(255) NULL
);
GO

ALTER TABLE DBServer ADD CONSTRAINT pk_dbserver_id PRIMARY KEY (id);
ALTER TABLE DBServer ADD CONSTRAINT uq_dbserver_host UNIQUE (host);
GO

-- Populate DBServer table from existing DBContext entries
INSERT INTO DBServer (server_name, host, port, vendor, vendor_version)
    SELECT Distinct host, host, port, vendor, vendor_version
    FROM DBContext;

-- Add column to DBContext as FK to DBServer
ALTER TABLE DBContext ADD server_id bigint NULL;
ALTER TABLE DBContext ADD CONSTRAINT fk_dbcontext_server_id FOREIGN KEY (server_id) REFERENCES DBServer (id) ON DELETE CASCADE;
GO

-- Populate DBContext's server_id column
UPDATE c SET c.server_id = (SELECT s.id FROM DBServer As s WHERE s.host = c.host) FROM DBContext As c;

-- Change DBContext.server_id to NOT NULL
ALTER TABLE DBContext ALTER COLUMN server_id bigint NOT NULL;

-- Drop columns from DBContext
ALTER TABLE dbo.DBContext DROP COLUMN host, port, vendor, vendor_version;
GO
