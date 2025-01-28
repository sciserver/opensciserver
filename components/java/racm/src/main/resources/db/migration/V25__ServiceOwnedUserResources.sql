create or alter function racm.serviceOwnedUserResources(@username varchar(255)) 
 returns table 
as
return 
with ua as (
select distinct contextClass, resourceType, resourceContextUUID
,       resourceUUID, resourceId, resourcePubDID
  from racm.userActions(@username)
)
select ua.*
,     ar.usage
,     owr.name as owningResourceName
,     owr.uuid as owningResourceUUID
,     owrt.name as owningResourceType
,     owrc.uuid as owningResourceContextUUID
,     owrc.racmEndpoint as owningResourceContextEndpoint
,     owcc.name as owningContextClassName
  from ua
inner join AssociatedResource ar on ar.resourceId=ua.resourceId
   and ar.ownership='OWNED'
 inner join resource owr on owr.id=ar.containerId
 inner join resourcetype owrt on owr.resourceTypeId=owrt.id
 inner join resourcecontext owrc on owrc.id=owr.containerId
 inner join ContextClass owcc on owcc.id=owrc.contextClassId

 GO