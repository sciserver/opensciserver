ALTER TABLE t_DataVolume ALTER COLUMN [description] VARCHAR(MAX) NULL;
ALTER TABLE t_DataVolume ALTER COLUMN [url] VARCHAR(255) NULL;

INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT id FROM t_ResourceType WHERE name=N'FileService.DataVolume')
	, N'edit', N'Edit the metadata of a Data Volume', N'U');