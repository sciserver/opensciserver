.. _racm_role:

The Role in SciServer
=====================

RACM has a central role in SciServer.
(Almost) every other SciServer service, for example the :ref:`fileservice`, :ref:`compute`, or :ref:`sciquery`, 
will interact with RACM for almost every request that is made of them to check whether the user making the request
is allowed to perform it. In general this requires knowledge of the user that is making the request, 
a resource used in the request and the action that is requested to be executed on the resource.

For example if a user wants to upload a file to a user volume in the :ref:`file service <fileservice>`, the file service
will want to check whether the user has the permission to perfoem the 'write' action on the resource represented by the user volume.

To make this mapping the service must be represented in the RACM database as a :ref:`ResourceContext <racm_resourcecontext>` 

