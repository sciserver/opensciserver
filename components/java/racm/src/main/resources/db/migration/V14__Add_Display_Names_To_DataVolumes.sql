ALTER TABLE [dbo].[t_DataVolume] ADD [displayName] VARCHAR(255) NULL;
GO

ALTER VIEW [DataVolume] AS
	SELECT ID
		,publisherDID
		,DTYPE
		,[name]
		,[description]
		,[displayName]
		,[pathOnFileSystem]
		,[url]
		,resourceId
		,fileServiceId
	FROM t_DataVolume
GO