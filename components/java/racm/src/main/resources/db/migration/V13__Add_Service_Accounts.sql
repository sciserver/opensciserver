CREATE TABLE t_ServiceAccount (
	ID BIGINT NOT NULL,
	[serviceToken] VARCHAR(255) NOT NULL
);
ALTER TABLE t_ServiceAccount ADD CONSTRAINT pk_t_ServiceAccount_ID PRIMARY KEY(ID);

ALTER TABLE [dbo].[t_ResourceContext] ADD [accountId] BIGINT NULL;
ALTER TABLE [dbo].[t_ResourceContext] DROP COLUMN [serviceToken];
GO

CREATE INDEX ix_t_ResourceContext_account ON t_ResourceContext(accountId);
GO

ALTER TABLE t_ServiceAccount ADD CONSTRAINT fk_t_ServiceAccount_extends
	FOREIGN KEY (ID) REFERENCES t_SciserverEntity(ID);
GO

CREATE VIEW [ServiceAccount] AS
	SELECT b.*,
		t.[serviceToken]
	FROM t_ServiceAccount t,
		[SciserverEntity] b
	WHERE b.ID = t.ID
GO

ALTER VIEW [ResourceContext] AS
	SELECT ID,
		publisherDID,
		DTYPE,
		[racmEndpoint],
		[uuid],
		[label],
		[description],
		[accountId],
		contextClassId
	FROM t_ResourceContext
GO