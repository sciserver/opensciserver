CREATE TABLE dbo.DBDomain (
	[domain_id] [bigint]  identity(1,1) NOT NULL,
	racm_domain_uuid varchar(64) NOT NULL unique,  -- resource context UUID
	displayName varchar(128) not null ,
	description varchar(max) null,
)

GO


-- must have sufficient information to make a connection to the database, e.g. create a jdbc url etc
CREATE TABLE dbo.DBContext(
	id bigint identity(1,1) not null,
	[domain_id] [bigint] NOT NULL,
	racm_resource_uuid  varchar(64) NULL,  -- the pubdid in racm
	[context_name] [varchar](255) NOT NULL , -- e.g. '[mydb:manu25]' DR16
	[description] [varchar](max) NOT NULL,
	[host] [varchar](255) NOT NULL, -- MUST NOT have a port
	[port] integer null,
	[db_name] [varchar](255) NOT NULL,  -- name of database, e.g. mydb_86318336
	[vendor] [varchar](16) NOT NULL,  -- MSSQL MYSQL POSTGRES DB2 ORACLE (see racm enum)
	[vendor_version] [varchar](255) NULL,  -- e.g. 'SQL Server 2019 EE'
	[readonly_user_name] [varchar](255) NOT NULL,
	[readonly_user_password] [varchar](255) NOT NULL,
	[readwrite_user_name] [varchar](255) NULL,
	[readwrite_user_password] [varchar](255) NULL
) ON [PRIMARY]

GO

