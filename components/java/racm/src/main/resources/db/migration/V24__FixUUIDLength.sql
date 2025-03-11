-- early version of this function used varchar(32) which is too short.
alter function racm.isResourceOwnedByAnotherResource(@uuid varchar(128))
 returns table 
as
return 
select ar.containerId as owningResourceId
,      r.id as ownedResourceId
  from Resource r 
  join AssociatedResource ar on ar.resourceId=r.id and ar.ownership='OWNED' 
 where r.uuid=@uuid
 GO