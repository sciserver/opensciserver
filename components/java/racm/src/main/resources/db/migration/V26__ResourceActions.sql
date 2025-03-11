/*
-- return resources accessible to the speficied user, owned by the given context.
-- return also the actions the user has privilege to exercise, as a comma-separated string
select *
	 from racm.[userResourceActionsOnContext]('02b47648-fe39-4a55-b567-d60dcd4ee3a0','mtaghiza') t1
	order by 1
*/


CREATE or ALTER function racm.[userResourceActionsOnContext] (@cid varchar(128), @username varchar(255))
returns @rt table (resourceId bigint, resourceUUID varchar(255), actions varchar(1024))
begin
declare @temp table (resourceContextUUID varchar(40), resourceId bigint,  resourceUUID varchar(255), [action] varchar(32))

insert into @temp
select distinct resourceContextUUID, resourceId, resourceUUID, action 
from racm.useractions(@username)

insert into @rt  
select distinct resourceid, resourceUUID,
STUFF((Select ','+action
         from @temp t1
        where T1.resourceid=T2.resourceid
		FOR XML PATH('')),1,1,'') 
 from @temp t2
 where t2.resourceContextUUID=@cid

return 

end

GO 

/*
select *
  from racm.userFileserviceResources('02b47648-fe39-4a55-b567-d60dcd4ee3a0','gerard') t1
 order by type,resourceid
*/
create or alter function racm.userFileserviceResources(@rcUUID varchar(40), @u varchar(32))
returns table as
return

with v as (
select 'R' as [type],id,id as rootVolumeId,name,description,pathOnFileSystem as [path], name as displayName, null as owner
,       null as url,containsSharedVolumes,resourceId
  from rootvolume rv 
union
select 'D',id, null,name,description,pathOnFileSystem,displayName, null as owner, url,0,resourceId
  from datavolume dv 
union
select 'U',uv.id,uv.rootVolumeId,uv.name, uv.description, uv.relativePath, uv.name, u.username as owner,null,0,uv.resourceId
  from uservolume uv join t_User u on u.id=uv.ownerId
)
select v.[type],v.id,ua.resourceUUID,v.name,v.description,v.path,v.displayName, v.owner,v.url, v.containsSharedVolumes, ua.actions
,      v.resourceId,v.rootVolumeId,ar.containerId as owningResourceId
  from v
  join racm.userresourceactionsoncontext(@rcUUID,@u) ua
    on ua.resourceid=v.resourceid
  left outer join t_AssociatedResource ar on ar.resourceId=ua.resourceid and ar.ownership='OWNED'
union
select 'RZ',rv.id,r.uuid,rv.name,rv.description,rv.pathOnFileSystem, rv.name as displayName, null,null 
,      rv.containsSharedVolumes,null,rv.resourceId,rv.containerId,null
  from rootvolume rv 
  join t_resource r on r.id=rv.resourceId 
  join resourcecontext rc on rc.id=r.containerId and rc.uuid=@rcUUID

GO
  

/*
select *
  from racm.userContextResourceShares('02b47648-fe39-4a55-b567-d60dcd4ee3a0','arik') t1
 order by resourceid,scientityid
*/
create or alter function racm.userContextResourceShares(@rcUUID varchar(40), @u varchar(32))
returns table as
return
with a as (
select distinct ua.resourceid,ua.resourceUUID
,      CASE WHEN u.id is not null then 'USER' when ug.id is not null then 'GROUP' when sa.id is not null then 'SERVICE' else 'UNKNOWN' end as scientityType
,      CASE WHEN u.id is not null then u.id when ug.id is not null then ug.id when sa.id is not null then sa.id else null end as scientityid
,      CASE WHEN u.id is not null then u.username when ug.id is not null then ug.name when sa.id is not null then sarc.uuid else null end as scientityName
,      a.[name] as [action]
from racm.userresourceactionsoncontext(@rcuuid,@u) ua
  inner join privilege p on p.containerId = ua.resourceId 
  inner join[action] a on a.id=p.actionId
  left outer join t_user u on u.id=p.scisEntityId and u.visibility='PUBLIC'
  left outer join t_usergroup ug on ug.id=p.scisEntityId and ug.accessibility = 'PRIVATE'
  left outer join t_ServiceAccount sa on sa.id=p.scisEntityId 
  left outer join t_ResourceContext sarc on sarc.accountId=sa.id 
)
select * from a where scientityid is not null
GO