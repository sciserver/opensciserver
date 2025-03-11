alter function racm.canUserGrant (@cid varchar(128), @rid varchar(128), @username varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)select a.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.actionassignments a 
	   on a.scisId = e.id 
	  and a.resourceContextUUID=@cid
	  and a.resourcePubDID=@rid
	  and a.actionCategory='G'
go

alter function racm.canUserDoAction (@resourceuuid varchar(128), @action varchar(255), @username varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)select a.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.actionassignments a 
	   on a.scisId = e.id 
	  and a.resourceUUID=@resourceuuid
	  and a.action=@action

go

drop function racm.canUserDoActionDEPRECATED
go

alter function racm.canUserDoRootAction (@username varchar(255), @action varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)select a.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.actionassignments a 
	   on a.scisId = e.id 
	 inner join racm.RootActions ra
	   on a.actionId=ra.actionId
	  and ra.action=@action

go

alter function racm.userActionsOnContext (@cid varchar(128), @username varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)
select a.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.actionassignments a on a.scisId = e.id and a.resourceContextUUID=@cid

go

-- possible view defining actions assigned to scisentities through relations between resources etc

alter function racm.userActions(@username varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)
select a.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.actionassignments a on a.scisId=e.id

go

-- find docker images user has access to
alter function racm.AccessibleDockerImages(@username varchar(255)) 
returns table
as
return
select di.name as imageName,a.* 
  from racm.userActions(@username) a
  ,    dockerimage di
  where di.resourceId=a.resourceId

go

-- find volume containers a user has access to
alter function racm.AccessibleVolumeContainers(@username varchar(255)) 
returns table
as
return
select vc.name as containerName,a.* 
  from racm.userActions(@username) a
  ,    VolumeContainer vc
  where vc.resourceId=a.resourceId

go

alter function racm.doesUserHaveRole (@cid varchar(128), @rid varchar(128), @role varchar(255), @username varchar(255))
returns table as
return 
with entities as (
-- to avoid cycles, use sentinel as in http://stackoverflow.com/questions/11041797/tsql-cte-how-to-avoid-circular-traversal
-- Here do not use user.id in sentinel, as groups and users might have same id occur in each.
-- Q: is that actually true? both are scisentity-s
  select id, username, Sentinel = CAST('' AS VARCHAR(MAX)), 'U' as privEntity
    from [user] u
   where u.username=@username
  union all
  select g.id, g.name , Sentinel + convert(varchar,g.id)+'|', 'G'
    from entities e
	,    racm.truemember m
	,    usergroup g
   where m.scisEntityId=e.id
	 and g.id=m.containerid
	 and CHARINDEX(convert(varchar,g.id),Sentinel)=0
)select ra.*, e.username, e.id, e.privEntity 
  from entities e 
     inner join racm.RoleAssignments ra
	   on ra.scisId = e.id 
	  and ra.resourceContextUUID=@cid
	  and ra.resourcePubDID=@rid
	  and ra.role=@role

go

alter function racm.mountableUserVolumes(@username varchar(255) )
returns table as
return 

select rvcd.containerId as computeDomainId 
,      uv.id as userVolumeId
,      a.resourceUUID
,      rv.containsSharedVolumes as isShareable
,      rvcd.path as rvPath
,      uv.relativePath as uvRelativePath 
,      uv.name as displayName
,      rvcd.publisherDID
,      u.username as owner
,      u.userId as ownerId
,      uv.description
,      fs.apiEndpoint as fileServiceAPIEndpoint
,      rv.name as rootVolumeName
,      a.action as action
  from racm.useractions(@username) a
    inner join UserVolume uv
      on uv.resourceId = a.resourceId
    inner join [User] u
	  on u.id=uv.ownerId
    inner join RootVolume rv
      on rv.id = uv.rootVolumeId
    inner join FileService fs
	  on rv.containerId=fs.id
    inner join RootVolumeOnComputeDomain rvcd
      on rvcd.rootVolumeId=rv.id 
GO

alter function racm.groupResourcesForUser(@username varchar(255)) returns table as 
return
select distinct ug.id as groupid
,      ug.name as groupName
,      ga.contextClass
,      ga.resourceContextAPIEndpoint
,      ga.resourceType
,      ga.resourceName
,      ga.resourcePubDID
,      ga.resourceId
,      ga.action
  from [User] u
   inner join Member m
   on m.scisEntityId=u.id
   inner join UserGroup ug
   on ug.id=m.containerId
   and ug.name != 'public'
   cross apply racm.groupActions(ug.id) ga
   join racm.userActions(@username) ua
   on ua.resourceId=ga.resourceid
   where u.username=@username 

GO

alter function racm.groupFriends(@username varchar(255)) returns table as 
return 
select ug.id as groupId
,      mo.memberRole
,      o.id as memberuserid
,      o.username as memberName
,      o.contactEmail as memberEmail
,      p.fullName
,      p.affiliation
  from [user] u
     inner join racm.truemember m
	 on m.scisEntityId=u.id
	 inner join UserGroup ug
	 on m.containerId = ug.id
	 inner join racm.TrueMember mo
	 on mo.containerId=ug.id
	 inner join [User] o
	 on o.id=mo.scisEntityId
	 left outer join Party p
	 on p.id=o.partyId
  where u.username=@username
  and ug.name != 'public'
GO

alter function racm.userGroups(@username varchar(255)) returns table as
return
with gs as (
select distinct ug.id
  from [user] u
    inner join racm.TrueMember m
	on m.scisEntityId = u.id
	inner join usergroup ug
	on m.containerId=ug.id
	and ug.name != 'public'
where u.username=@username
)
select ug.id,ug.name, ug.description
   from gs, usergroup ug where ug.id=gs.id
go