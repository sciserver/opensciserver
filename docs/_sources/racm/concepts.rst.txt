.. _racm_concepts:

========
Concepts
========

RACM is the authorization component of SciServer, it provides support for defining permissions to access "resources". 
RACM supports questions like "what *actions* can a *user* perform on a certain *resource*", or "what are all *resources* accessible 
to a certain *user*"?
Other components in or related to SciServer can use RACM to store information on the resources they manage to make decisions 
on whether a user can execute certain functionality. These components will implicitly map these functionalities to the
resources and actions 

So what is a "resource"? In SciServer it is any thing/entity/object that an "action" can be performed on in somee SciServer component 
and in particular where one may restrict the rights to execute these actions. To show some examples: 

* a DATABASE is a resource managed by a service for accessing relational databases. 
  Actions on it are for example QUERY and UPDATE with obvious semantics.
* a USER VOLUME is a RESOURCE that represents a POSIX folder in a file system that is created by the user.
  Its actions are READ, WRITE, DELETE.
* a COMPUTE IMAGE is a RESOURCE managed by our SciServer compute component. 
  It represents a Docker (or other) container image and an action is CREATE CONTAINER.

ALl of these RESOURCEs also have the GRANT action, which allows a USER to grant privileges to execute ACTIONs on the RESOURCE.

Individual USERs can be granted access, but RACM also supports the creation of (USER) GROUPs and assigning of PRIVILEGEs to those. USERs can be invited to a GROUP and inherit the PRIVILEGEs assigned to it.

RESOURCEs live in a RESOURCE CONTEXT. These often represent a web application or sciserver component that manages the entities represented b the RESOURCE. Examples are
- SciQuery, managing DATABASEs
- ComputeDomains, managing COMPUTE IMAGEs
- FileService, managing USER and DATA VOLUMES.