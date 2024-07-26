.. SciServer documentation master file, created by
   sphinx-quickstart on Fri Jul 19 12:50:47 2024.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Resource Access Control Management (RACM)
=========================================

This part of the documentation describes the Resource Access Control Management component in SciServer, referred to as RACM.
RACM stores information about (the existence of) general SciServer *resources* and the rights to execute certain actions on those resources as they are 
assigned to users or groups of users.

The links below point to documentation describing the data model and the database that stores this information, the object-relational mapping (ORM) layer for interacting with the database
and the API for interacting with RACM itself through REST calls. Here we describe the role RACM plays in the running of SciServer.

History
-------
RACM is based on an abstract data model expressed in UML and described in :ref:`racm_datamodel`.
The UML model was designed in using MagicDraw Community Edition v12.1 and stored as an XMI file. 
We used an automated pipeline to derive alternate "representations" of the UML, such as Java classes and Data Definition Language (DDL) scripts
to create tables and views in a relational database. 
That pipeline, named VO-URP, was originally developed for the Simulation Data Model effort in the IVOA.

.. toctree::
   :maxdepth: 2
   :caption: Contents:

   role.rst
   datamodel.rst
   database.rst
   orm.rst
   api.rst
   
   
