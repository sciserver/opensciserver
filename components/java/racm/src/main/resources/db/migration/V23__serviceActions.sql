-- Returns allowed actions for service account in a given user volume.


CREATE FUNCTION [racm].[serviceActions](@fsUUID varchar(255),  @uvPubdid varchar(256), @serviceToken varchar(255))
RETURNS TABLE
AS
RETURN
(
    select r.ID as resourceId
         ,  a.name
    from dbo.ResourceContext fs
    , dbo.Resource r
    , dbo.Privilege p
    , dbo.ServiceAccount s
    , dbo.[Action] a
    where fs.uuid  = @fsUUID
    and r.publisherDID = @uvPubdid
    and  r.containerId = fs.ID
    and r.id = p.containerId 
    and p.scisEntityId = s.ID
    and a.ID = p.actionId
    and s.serviceToken = @serviceToken
);
 GO 