ALTER TABLE [dbo].[t_ResourceContext] ADD [serviceToken] VARCHAR(255) NULL;
GO

ALTER VIEW [ResourceContext] AS
	SELECT ID,
		publisherDID,
		DTYPE,
		[racmEndpoint],
		[uuid],
		[label],
		[description],
		[serviceToken],
		contextClassId
	FROM t_ResourceContext
GO