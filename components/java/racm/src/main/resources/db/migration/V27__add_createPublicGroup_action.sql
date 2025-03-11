INSERT [dbo].[t_Action]
	([DTYPE], [OPTLOCK], publisherDID, [containerId], [name], [description], [category])
	SELECT 'Action', 1, 'System.__rootcontext__.createPublicGroup', rt.id, 'createPublicGroup', 'Promote a PRIVATE group to PUBLIC, thus creating a science domain', 'C'
		FROM t_ContextClass cc
		INNER JOIN t_ResourceType rt ON rt.containerId = cc.id and rt.name = '__rootcontext__'
		WHERE cc.name = 'System'
		  AND NOT EXISTS (select a.id from t_action a where a.containerid=rt.id and a.name='createPublicGroup')
