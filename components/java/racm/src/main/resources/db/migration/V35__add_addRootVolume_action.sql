-- Adds an 'addRootVolume' action to the root context of the DockerComputeDomain context class,
-- so that mounting a root volume on a compute domain can be authorized independently of the
-- blanket 'admin' role, and grants it to the existing compute domain admin role so that current
-- administrators keep the ability they have today.
--
-- Note: t_RoleAction.containerId is the ROLE id; actionId is the ACTION id.
-- The t_Role join is anchored to the resource type, not just the role name: V1 seeds several
-- roles named 'admin' on different resource types, and matching on name alone would bind this
-- action to the wrong one, silently.
--
-- Inserts target the t_ tables rather than the unprefixed views, because the views omit OPTLOCK.
--
-- Both statements are guarded with NOT EXISTS so the script is safe to re-run.

INSERT [dbo].[t_Action]
    ([DTYPE], [OPTLOCK], [publisherDID], [containerId], [name], [description], [category])
    SELECT 'Action', 1, NULL, rt.ID, 'addRootVolume',
           'Action of mounting a root volume on this compute domain.', 'C'
      FROM t_ContextClass cc
      INNER JOIN t_ResourceType rt
              ON rt.containerId = cc.ID
             AND rt.[name] = '__rootcontext__'
     WHERE cc.[name] = 'DockerComputeDomain'
       AND NOT EXISTS (SELECT 1
                         FROM t_Action a
                        WHERE a.containerId = rt.ID
                          AND a.[name] = 'addRootVolume')

INSERT [dbo].[t_RoleAction]
    ([DTYPE], [OPTLOCK], [publisherDID], [containerId], [actionId])
    SELECT 'RoleAction', 1, NULL, r.ID, a.ID
      FROM t_ContextClass cc
      INNER JOIN t_ResourceType rt
              ON rt.containerId = cc.ID
             AND rt.[name] = '__rootcontext__'
      INNER JOIN t_Role r
              ON r.containerId = rt.ID
             AND r.[name] = 'admin'
      INNER JOIN t_Action a
              ON a.containerId = rt.ID
             AND a.[name] = 'addRootVolume'
     WHERE cc.[name] = 'DockerComputeDomain'
       AND NOT EXISTS (SELECT 1
                         FROM t_RoleAction ra
                        WHERE ra.containerId = r.ID
                          AND ra.actionId = a.ID)
