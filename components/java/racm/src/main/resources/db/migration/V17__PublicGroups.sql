
create function racm.PublicGroups(@userid bigint)
 returns table 
as
return 
select ug.id,ug.name, ug.description, me.memberrole as myrole, me.status as mystatus
,      u.id as adminId,m.memberrole, u.username,u.contactEmail
  from UserGroup ug
		left outer join member me on me.containerid=ug.id and me.scisEntityId=@userid
       join Member m on m.containerId=ug.id and m.memberRole in ('OWNER','ADMIN') and m.status in ('OWNER','ACCEPTED')
	   join [User] u on u.id=m.scisEntityId
 where ug.accessibility='PUBLIC'
GO


create view racm.PublicGroupResources as
select distinct ug.id as groupId, r.id as resourceId,r.publisherDID,r.uuid as resourceUUID
,      r.name as resourceName, r.description as resourceDescription
,      rt.name as resourcetype, cc.name as contextClass
,      rc.racmEndpoint, rc.description as resourceContextDescription, rc.uuid as resourceContextUUID
  from usergroup ug 
    inner join AccessControl ac
	  on ac.scisEntityId=ug.id
   inner join Resource r
      on r.id=ac.containerId
  inner join resourcetype rt
     on rt.id=r.resourceTypeId
  inner join resourceContext rc
  on rc.id=r.containerId
  inner join ContextClass cc
    on cc.id=rc.contextClassId
 where ug.accessibility='PUBLIC'
go

-- TODO add updates to Resource with name/description/publisherDID for all resource types.
update r
   set r.name=uv.name
   ,   r.description=uv.description
from t_resource r
  join uservolume uv
  on uv.resourceId=r.id
GO

update r
   set r.name=dv.displayname
   ,   r.description=dv.description
   ,   r.publisherDID=dv.name
from t_resource r
  join datavolume dv
  on dv.resourceId=r.id
GO