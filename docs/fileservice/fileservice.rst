.. _fileservice:

FileService
===========

The SciServer FileService is a stand-alone Spring Boot application with a 
`REST API <https://apps.sciserver.org/fileservice/swagger-ui/index.html>`_ that allows the interaction with 
the SciServer file system. This includes listing, downloading, uploading, deleting and moving files and/or folders, 
and giving access to data resources such as user or service volumes, among other functions.

A new FileService instance needs to be registered in SciServer by making an HTTP request to RACM's :ref:`racm_storem_api`.
As a result, the FileService instance will be assigned a UUID, used for uniqqhely identifying the FileServie, and a Service Token. 
This token needs to be saved in the FiLeService configuration, and is passed as the ``X-Service-Auth-ID`` entry 
in request header when the FileService makes HTTP calls to the :ref:`racm_storem_api`, as a mean for authenticating itself.

In :ref:`compute`, users can find and save data in folders under a particular ``basePath``, namely ``/home/idies/workspace``. 
Under this path, users can find 2 types of ``topVolumes``, namely ``RootVolumes`` and ``DataVolumes``, usually mounted on NFS shares:

1)  ``RootVolumes``: 

    Under these volumes users can create their own folders, called ``UserVolumes``, and share them with other SciServer users or groups.
    The path of those in the file system is ``/home/idies/workspace/<RootVolumeName>/<UserName>/<UserVolumeName>``.

    By default, users are able to access two ``Root Volumes`` with distinct names:

    a) The ``Storage`` root volume, with a limit on the total amount of data over all ``userVolumes`` each user creates under it.
       This limit over all ``userVolumes`` created by a single user, is physically set on the file system by the stand-alone 
       SciServer Quota-Manager application, after the FileService sends to it a request to create a new userVolume.
       If the FileService configuration is missing information about the Quota-Manager, then it will simply locally create a UserVolume folder 
       without a quota. Users are given a ``UserVolume`` under ``Storage`` by default, named ``persistent``.


    b) The ``Temporary`` root volume has no limit in the amount of data users can store in the ``User volumes`` they create under it, 
       although data is periodically deleted by a stand-alone crawler application (if it is installed).
       Users are always given a ``UserVolume`` under ``Temporary`` by default, named ``scratch``.

    Users can create and share ``userVolumes`` in Files tab in the SciServer Dashboard or methods in the SciScript libraries, 
    which in turn call the respective endpoints in with calls to the FileService `REST API <https://apps.sciserver.org/fileservice/swagger-ui/index.html>`_.
   
2)  ``DataVolumes``:

    These ``topVolumes`` in general contain big science data sets that SciServer admins make available to the public or a specific group of users.
    The path of those in the file system is ``/home/idies/workspace/<DataVolumeName>``.

3)  ``ServiceVolumes``:
 
    They Follow the same pattern in the file system as ``RootVolumes``, although the creation or deletion of the respective ``UserVolumes`` 
    is not managed by SciServer users, but by separate SciServer services such as CourseWare.

The names and metadata about ``topVolumes``, including their base path on the mounted NFS file system, need to be registered by calls to RACM's :ref:`racm_storem_api`, 
before these volumes can be given access to and be available to users. The FileService makes periodical calls to STOREM in order to get information about 
all available and possibly newly registered ``topVolumes``, in order to automatically refresh that information on its local memory cache.


The FileService API contains several endpoints for checking the status of the application, 
including a `PING endpoint <https://apps.sciserver.org/fileservice/swagger-ui/index.html#/api-controller/getPing>`_ 
for verifying the ta the application is running, 
and the `HEALTH endpoint <https://apps.sciserver.org/fileservice/swagger-ui/index.html#/api-controller/getHealthReport>`_ 
for checking whether files can be written and deleted from each of the existing rootVolumes and dataVolumes.


**Configuring, Building and Running the SciServer FileService**

The configuration variables for the FileService are placed in the ``applications.properties`` and ``log4j2.xml`` files under 
``/src/main/resources/``. Example instances of those can be found under ``/conf-example/``.

Some important variables in the ``applications.properties`` file include:

1) The URLs pointing to the RACM and LoginPortal REST APIs. If user volume quotas are desired, also include 
   the Quota-Manager's URL and authentication password.
   
2) ``RACM.resourcecontext.uuid``: the UUID of the FileService resource context instance as registered in RACM, 
   which is sent along several HTTP requests to the APIs in order to uniquely identify the FileService instance.

3) ``RACM.resourceContext.serviceToken``: unique authentication token the FileService instance, used for authenticating itself as a Service in RACM.

4) ``File-service.default.uservolumes``: a JSON structure with metadata associated to the default user volumes that are automatically 
   created for all SciServer users.

5) RabbitMQ settings: For logging, activity and error messages can be sent to and queued on an external RabbitMQ instance in 
   order to be subsequently logged. One must set the RabbitMQ host, exchange and queue names.

On the other hand, ``log4j2.xml`` file contains configuration in relation to logging messages to a file.


Since the SciServer FileService source code is integrated with `Gradle <https://gradle.org>`_ , 
one can build and run it locally by executing the respective Gradle targets in Visual Studio/Eclipse, or explicitly by executing ``./gradlew build`` or ``./gradlew run``
on the base level of the project directory. For running it in a production-grade environment, refer to the SciServer Kubernetes setup.
