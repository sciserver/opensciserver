.. _fileservice:

FileService
===========

The SciServer FileService is a stand-alone Spring Boot application with a `REST API <https://apps.sciserver.org/fileservice/swagger-ui/index.html>`_ that allows 
the interaction with the SciServer file system. This includes listing, downloading, uploading, deleting and moving files and/or folders, and giving access to data resources such as user or service volumes, 
among other functions.

In :ref:`compute`, users can find and save data in folders under a particular ``basePath``, namely ``/home/idies/workspace``. Under this path, 
users can find 2 types of ``topVolumes``, namely ``RootVolumes`` and ``DataVolumes``, usually mounted on NFS shares:

1)  ``RootVolumes``: 

    Under these volumes users can create their own folders, called ``UserVolumes``, and share them with other SciServer users or groups.
    The path of those in the file system is ``/home/idies/workspace/<RootVolumeName>/<UserName>/<UserVolumeName>``.

    By default, users are able to access 2 ``Root Volumes`` with distinct names:

    a) The ``Storage`` root volume has a limit on the total amount of data over all ``userVolumes`` each user creates under it.
       This limit over all ``userVolumes`` created by a single user, is set by the SciServer Quota Manager service (REF).
       Users are always given a ``UserVolume`` under ``Storage`` by default, named ``persistent``.


    b) The ``Temporary``` root volume has no limit in the amount of data users can store in the ``User volumes`` they create under it, 
       although data is periodically deleted by REF.
       Users are always given a ``UserVolume`` under ``Temporary`` by default, named ``scratch``.

    Users can create and share ``userVolumes`` in Files tab in the SciServer Dashboard or methods in the SciScript libraries, 
    which in turn call the respective endpoints in with calls to the FileService `REST API <https://apps.sciserver.org/fileservice/swagger-ui/index.html>`_.


2)  ``DataVolumes``: these ``topVolumes`` in general contain big science data sets that SciServer admins make available to the public or a specific group of users.
     The path of those in the file system is ``/home/idies/workspace/<DataVolumeName>``.


3)  ``ServiceVolumes``: follow the same pattern in the file system as ``RootVolumes``, although the creation or deletion of the respective ``UserVolumes`` 
     is not managed by SciServer users, but by separate SciServer services such as CourseWWare (REF).


The names and metadata about top volumes, including their patch to the mounted NFS file system, need to be registered by calls to RACM's STOREM API (REF), 
before these volumes can be given access to and be available to users.

The FileService API contains several endpoints for checking the status of the application, 
including a `PING endpoint <https://apps.sciserver.org/fileservice/swagger-ui/index.html#/api-controller/getPing>`_ 
for verifying the ta the application is running, 
and the `HEALTH endpoint <https://apps.sciserver.org/fileservice/swagger-ui/index.html#/api-controller/getHealthReport>`_ 
for checking whether files can be written and deleted from the Root and Data Volumes registered in the FileService.

