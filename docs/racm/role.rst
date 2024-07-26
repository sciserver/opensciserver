The role of RACM in SciServer
=============================
RACM is the authorization component of SciServer, it provides support for defining permissions to access "resources". 
RACM supports questions like "what ACTIONS can a USER do on a certain RESOURCE", or "what are all RESOURCES accessible 
to a certain USER"?
Other components in or related to SciServer can use RACM to to store information on the resources they manage to make decisions on whether a user can perform certain actions

So what is a RESOURCE? In SciServer it is any thing/entity/object that ACTION can be executed on in somee SciServer component 
and in particular where one may restrict the rights to execute these actions. Best to show some examples: 
- a DATABASE is a resource and actions on it are for example QUERY and UPDATE with obvious semantics.
- a USER VOLUME is a RESOURCE and actions are READ, WRITE, DELETE.
- a COMPUTE IMAGE is a RESOURCE and an action is CREATE CONTAINEER.

ALl of these RESOURCEs also have the GRANT action, which allows a USER to grant privileges to execute ACTIONs on the RESOURCE.

Individual USERs can be granted access, but RACM also supports the creation of (USER) GROUPs and assigning of PRIVILEGEs to those. USERs can be invited to a GROUP and inherit the PRIVILEGEs assigned to it.

RESOURCEs live in a RESOURCE CONTEXT. These often represent a web application or sciserver component that manages the entities represented b the RESOURCE. Examples are
- SciQuery, managing DATABASEs
- ComputeDomains, managing COMPUTE IMAGEs
- FileService, managing USER and DATA VOLUMES.
