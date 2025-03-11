INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
	SELECT 'Action', 1, NULL, t_ResourceType.id, 'grant', 'Grant permissions on this file system', 'G'
		FROM t_ContextClass
		INNER JOIN t_ResourceType ON t_ResourceType.containerId = t_ContextClass.id and t_ResourceType.name = '__rootcontext__'
		WHERE t_ContextClass.name = 'FileService'

INSERT [dbo].[t_RoleAction]
([DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId])
	SELECT 'RoleAction', 1, NULL, t_Role.id, t_Action.id
		FROM t_ContextClass
		INNER JOIN t_ResourceType ON t_ResourceType.containerId = t_ContextClass.id and t_ResourceType.name = '__rootcontext__'
		INNER JOIN t_Role ON t_Role.containerId = t_ResourceType.id and t_Role.name = 'fs_admin'
		INNER JOIN t_Action ON t_Action.containerId = t_ResourceType.id and t_Action.name = 'grant'
		WHERE t_ContextClass.name = 'FileService'

-- Remove unused registerUserVolume permission
DELETE t_RoleAction
	FROM t_RoleAction
		INNER JOIN t_Role ON t_RoleAction.containerId = t_Role.id and t_Role.name = 'fs_admin'
		INNER JOIN t_ResourceType ON t_Role.containerId = t_ResourceType.id and t_ResourceType.name = '__rootcontext__'
		INNER JOIN t_ContextClass ON t_ResourceType.containerId = t_ContextClass.id and t_ContextClass.name = 'FileService'
		INNER JOIN t_Action ON t_Action.containerId = t_ResourceType.id and t_Action.name = 'registerUserVolume'
		WHERE t_RoleAction.containerId = t_Role.id and t_RoleAction.actionId = t_Action.id
DELETE t_Action
	FROM t_Action
		INNER JOIN t_ResourceType ON t_Action.containerId = t_ResourceType.id and t_ResourceType.name = '__rootcontext__'
		INNER JOIN t_ContextClass ON t_ResourceType.containerId = t_ContextClass.id and t_ContextClass.name = 'FileService'
		WHERE t_Action.name = 'registerUserVolume'