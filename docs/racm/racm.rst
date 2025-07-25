.. SciServer documentation master file, created by
   sphinx-quickstart on Fri Jul 19 12:50:47 2024.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. _racm:
   
Resource Access Control Management (RACM)
=========================================

.. toctree::
   :maxdepth: 1
   :caption: Contents:

   concepts
   datamodel
   representations
   role
   api   

This part of the documentation describes the Resource Access Control Management component in SciServer, referred to as RACM.
RACM stores information about (the existence of) general SciServer *resources* and the rights to execute certain actions on those resources as they are 
assigned to users or groups of users.

The links below point to documentation describing the data model and the database that stores this information, the object-relational mapping (ORM) layer for interacting with the database
and the API for interacting with RACM itself through REST calls. Here we describe the role RACM plays in the running of SciServer.

**History and background**

RACM is based on an abstract data model expressed in UML and described in :ref:`racm_datamodel`.
The UML model was designed using a modelling tool, `MagicDraw <https://www.magicdraw.com/>`_ Community Edition v12.1, and was stored as an 
`XMI <https://www.omg.org/spec/XMI/>`_ file. 
We used an automated pipeline to derive alternate "representations" of the model for use in particular software contexts. 
That pipeline, named VO-URP, was originally developed for the `Simulation Data Model <https://ivoa.net/documents/SimDM/20120503/index.html>`_
specification effort in the `IVOA <https://ivoa.net>`_. VO-URP consisted of a number of XSLT scripts that first transform the XMI 
to a simpler XML representation and follow on scripts that transform that representation to implementation contexts such as Java class libraries,
XML Schema definition files (XSD) and table and view definitions in a relational database. 

We decided that reliance on an old modeling tool long out of support and a legacy pipeline are not suitable for an open source project.
Instead the main representations Java and DDL files will fromo now on have to updated by hand.
Note that this was already the case for the database schema files. Updates to the model will have to be migrated carefully in
existing databases and the scripts generated by the pipeline are not sufficient.
The github respository for the vo-urp pipeline that we used originally will be made available with the Open SciServer repo, but that is
mainly for illustration and historical reasons.

Interestingly, VO-URP did find new life in the IVOA standard for a data modelling language, `VO-DML <https://ivoa.net/documents/VODML/20180910/index.html>`_.
Its specification language is directly derived from VO-URP's "intermediate specification". And its sugggested mapping to serialization meta-models
is directly related to the representation pipleine in VO-URP.
   
