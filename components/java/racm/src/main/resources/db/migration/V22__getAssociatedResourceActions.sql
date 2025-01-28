  -- Return all actions on an associated resource of a given resource for a given user.
 create function racm.associatedResourceActions(@uuid varchar(255), @username varchar(255))
 returns table 
as
return 
with a as (
  select distinct ua.resourceid,ua.action,ar.usage,ar.ownership,ua.resourceType,ua.resourceUUID
  from resource r
  join AssociatedResource ar on r.id=ar.containerId
  join racm.userActions(@username) ua on ua.resourceid=ar.resourceId
  where r.uuid=@uuid
)
select a.resourceid,a.action,a.usage,a.ownership,a.resourceType,a.resourceUUID
,      r.description, r.name as resource
 from a
  join resource r on r.id=a.resourceId
