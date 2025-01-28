-- if multiple rows are returned there is a problem, as a resource can only be owned by one other resource
--  this should be logged when detected
create function racm.isResourceOwnedByAnotherResource(@uuid varchar(32))
 returns table 
as
return 
select ar.containerId as owningResourceId
,      r.id as ownedResourceId
  from Resource r 
  join AssociatedResource ar on ar.resourceId=r.id and ar.ownership='OWNED' 
 where r.uuid=@uuid
 GO
 
-- if returned owningServiceId and resourceRCId are the same, then the resource containing the resource also has a resource owning it.
-- note, if multiple rows are returned there is a problem. only one resource can own another!
--  this should be logged when detected
create function racm.isResourceOwnedByThisService(@serviceToken varchar(255), @uuid varchar(128))
 returns table 
as
return 
select rc.uuid as owningServiceUUID, rc.id as owningServiceId
,      owr.uuid as owningResourceUUID
,      r.containerId as resourceRCId
  from Resource r 
  join AssociatedResource ar on ar.resourceId=r.id and ar.ownership='OWNED' 
  join Resource owr on owr.id=ar.containerId
  join ResourceContext rc on rc.id=owr.containerId
  join t_ServiceAccount sa on sa.id=rc.accountId
 where r.uuid=@uuid 
   and sa.serviceToken=@serviceToken

GO

create function racm.canServiceDoAction(@resourceuuid varchar(128), @action varchar(255), @serviceToken varchar(255))
 returns table 
return 
select sa.id as serviceId, p.id as privilegeId
  from t_ServiceAccount sa
    join privilege p
	on p.scisEntityId = sa.id
	join [t_Action] a
	on a.id=p.actionId
	and a.name=@action
	join t_resource r
	on r.id=p.containerId
	and r.uuid=@resourceuuid
 where sa.serviceToken=@serviceToken
 GO 