ALTER view [racm].[ActionAssignments] as 
select rc.UUID as resourceContextUUID 
  ,      rc.racmEndpoint as resourceContextAPIEndpoint 
  ,      rc.label as resourceContextLabel
  ,      r.publisherDID as resourcePubDID 
  ,      r.id as resourceId, r.uuid as resourceUUID, r.name as resourceName, cc.name as contextClass 
  ,      rt.name as resourceType, ro.name as role, a.name as action 
  ,      a.id as actionId, a.category as actionCategory 
  ,      ras.scisEntityId as scisId
  from resourcecontext rc
  inner join resource r on r.containerId = rc.id inner join roleassignment ras on ras.containerId = r.id 
  inner join role ro on ro.id=ras.roleid inner join roleaction ra on ro.id=ra.containerId 
  inner join action a on ra.actionId=a.id inner join resourcetype rt on rt.id=a.containerId 
  inner join contextclass cc on cc.id=rt.containerId
 union 
select rc.UUID as resourceContextUUID 
,      rc.racmEndpoint 
,      rc.label 
,      r.publisherDID 
,      r.id, r.uuid, r.name, cc.name, rt.name 
,      null, a.name, a.id, a.category, p.scisEntityId 
  from resourcecontext rc 
  inner join resource r on r.containerId = rc.id 
  inner join privilege p on p.containerId = r.id 
  inner join action a on p.actionId=a.id 
  inner join resourcetype rt on rt.id=a.containerId 
  inner join contextclass cc on cc.id=rt.containerId  

GO

exec sp_refreshsqlmodule 'racm.useractions'

GO