# SciServer's Resource Access Control Manager (RACM)

## Overview
RACM is the authorization component of SciServer.
It supports questions like "what ACTIONS can a USER do on a certain RESOURCE", 
or "what are all RESOURCES accessible to a certain USER"?

Other components in or related to SciServer can use RACM to to store information on the resources they manage 
to make decisions on whether a user can perform certain actions

RACM goes beyond a simple ROLE model 

### Resources
So what is a RESOURCE?
In SciServer it is any thing/entity/object that ACTION can be executed on in SciServer and in particular where one may restrict the rights to execute these actions.
Best by example: 

* a DATABASE is a resource and actions on it are for example QUERY and UPDATE with obvious semantics.
* a USER VOLUME is a RESOURCE and actions are READ, WRITE, DELETE.
* a COMPUTE IMAGE is a RESOURCE and an action is CREATE CONTAINEER.

ALl of these RESOURCEs also have the GRANT action, which allows a USER to grant privileges to execute ACTIONs on the RESOURCE.

Individual USERs can be granted access, but RACM also supports the creation of (USER) GROUPs and assigning of PRIVILEGEs to those.
USERs can be invited to a GROUP and inherit the PRIVILEGEs assigned to it.

RESOURCEs live in a RESOURCE CONTEXT. These often represent a web application or sciserver component that manages the entities represented b the RESOURCE.
Examples are
* SciQuery, managing DATABASEs
* ComputeDomains, managing COMPUTE IMAGEs
* FileService, managing USER and DATA VOLUMES.

## Data model

RACM is based on a formal data model illustrated by the following UML diagram.
<img src="datamodel/RACM-UML-core.png" alt="UML diagram of the core components of the RACM data model" width="800"/>
The UML is for now formally represented by an XMI [REF] file produced by the old community edition 11 of the MagicDraw UML modelling tool [REF].

Originally this XMI was the source from which Java classes, table definition files and documentation were generated
using an updated version of the VO-URP framework developed by Lemson & Bourges (LINK) for the Simulation Data Model in the IVOA [REF].

In Open SciServer we will use this UML model mainly for documentation purposes.
We assume that extensions to the model will be represented in code, database and documentation by hand rather than by code generation.
The actual structure of the mapping will be documented below.   

### SciServerEntity: User, UserGroup and ServciceAccount


### ContextClass: ResourceType, Action and Role


### ResourceContext: Resource, AssociateResource and AssociatedSciEntity


### AccessControl: Privilege and RoleAssignment
 

## Other components of the data model
All SciServer components rely on RACM for their authorization [GL is this correct way of stating this?]. 
For some of these the RACM model has been extended beyond the core, to facilitate handling of their resources.
IN particular the Filesservice, Compute and the Compute Batch JObs have explicit components in the model. These are described here. 

### ComputeDomain and COMPM

### JOBM

### STOREM


## Mapping the data model

### mapping to relational database schema

### mapping to Java


## Custom usage of the data model
The standard way by which we assume new services can make use of RACM is by mapping 
their components to resources and actions in the RACM model.
It generally includes defining a new ContextClass and sometimes special management of the description fields to add specific metadata on 
Resources. We describe three examples here, CasJobs, the old database access component in SciServer and its 
successor (to-be)SciQuery. And Courseware, a component to facilitate usage of SciServer in the class room.
   
### CasJobs


### SciQuery


### Courseware

