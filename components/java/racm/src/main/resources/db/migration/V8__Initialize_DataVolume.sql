INSERT [dbo].[t_ResourceType]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description])
	VALUES (N'ResourceType', 1, NULL,
		(SELECT id FROM t_ContextClass WHERE name=N'FileService')
	, N'FileService.DataVolume', NULL);

INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT t_ResourceType.id FROM t_ResourceType
		INNER JOIN t_ContextClass ON t_ResourceType.containerId = t_ContextClass.id
		WHERE t_ResourceType.name = '__rootcontext__'
			AND t_ContextClass.name = 'FileService')
	, N'registerDataVolume', N'Create a new Data Volume', N'C');

-- Allow fileservice admins to register new data volumes
INSERT [dbo].[t_RoleAction]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId])
	VALUES (N'RoleAction', 1, NULL,
		(SELECT t_Role.id FROM t_Role
		INNER JOIN t_ResourceType ON t_Role.containerId = t_ResourceType.id
		INNER JOIN t_ContextClass ON t_ResourceType.containerId = t_ContextClass.id
		WHERE t_ResourceType.name = '__rootcontext__'
			AND t_ContextClass.name = 'FileService'
			AND t_Role.name = 'fs_admin')
		, 
		(SELECT t_Action.id FROM t_Action
		INNER JOIN t_ResourceType ON t_Action.containerId = t_ResourceType.id
		INNER JOIN t_ContextClass ON t_ResourceType.containerId = t_ContextClass.id
		WHERE t_ResourceType.name = '__rootcontext__'
			AND t_ContextClass.name = 'FileService'
			AND t_Action.name = 'registerDataVolume'));

-- Actions on Data Volumes
INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT id FROM t_ResourceType WHERE name=N'FileService.DataVolume')
	, N'read', N'Read a Data Volume', N'R');
INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT id FROM t_ResourceType WHERE name=N'FileService.DataVolume')
	, N'write', N'Write to a Data Volume', N'U');
INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT id FROM t_ResourceType WHERE name=N'FileService.DataVolume')
	, N'delete', N'Delete a Data Volume', N'D');
INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	VALUES (N'Action', 1, NULL, 
	(SELECT id FROM t_ResourceType WHERE name=N'FileService.DataVolume')
	, N'grant', N'Grant access to a Data Volume', N'G');
GO