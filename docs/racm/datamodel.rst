.. _racm_datamodel:

===============
RACM Data Model
===============
RACM stores information about resources and the pemrissions users and groups have on these in a relational database, 
and provides the code to interact with this database through REST APIs. 

The database and the code for interacting with it are derived directly from a UML model that is illustrated in the image below

.. image:: _static/RACM-UML-core.png
   :align: center
   
   
SciServerEntity: User, UserGroup and ServciceAccount
====================================================

ContextClass: ResourceType, Action and Role
===========================================

ResourceContext: Resource, AssociateResource and AssociatedSciEntity
====================================================================

AccessControl: Privilege and RoleAssignment
===========================================
 

Other components of the data model
==================================
All SciServer components rely on RACM for any access control they want to enact on the resources they manage.
For some of these the RACM model has been extended beyond the core, to facilitate handling of their resources.
In particular the Filesservice, Compute and the Compute Batch Jobs have explicit components in the model. These are described here. 

ComputeDomain and COMPM
-----------------------

JOBM
----

Files service
-------------

   