.. _compm:

Compute and Query Jobs
======================

Although interactive Jupyter Notebook sessions are very convenient for exploring
data sets and developing/creating analysis pipelines, the
synchronous nature of these sessions makes them less suited
for executing larger computational and data-intensive jobs 
that take a longer time to complete.
Also, bigger demands on compute power, memory, and storage might not 
necessarily fit well within the allocated resources for 
less demanding interactive compute sessions, such as those 
for teaching a class, for example.

As a solution, SciServer incorporates several components that allow users to run 
asynchronous jobs on dedicated hardware supporting more demanding 
computational and data-intensive workflows. SciServer supports so far 2 classes of jobs: computational jobs that 
run in Docker containers in :ref:`compute` (Docker Jobs), and asynchronous SQL queries that run in Relational Databases (RDB Jobs) 
and managed by :ref:`sciquery`.

A job can be submitted and run in a particular :ref:`racm_compute_domain`, which in the case of Docker jobs it correspond 
to a group of hardware nodes in :ref:`compute` (such as Virtual Machines or Kubernetes nodes)
with predefined and guaranteed compute, memory and storage resources 
allocated for the Docker Containers that is spawned on any on the nodes to run the job.
These domains might be are available to all users or only a selected group of them,
depending on the respective access controls in :ref:`racm`.

When running a job, a job working directory (aka `resultsFolderURI`) is created by 
default in the SciServer file system (if not explicitly defined by the user), 
and is used to store job metadata and/or results. The default location of this directory falls 
under the ``jobs`` user volume, under the ``Temporary`` root volume, 
which has no data size quota in case the job results size is big.



**Compute-Manager**

The ``Compute-Manager`` or ``COMPM`` is a Spring Boot service that manages the life-cycle of jobs. There is a one-to-one relation between a 
an individual instance of a COMPM, and the Compute domain it is associated/registered to.
There are 2 types of COMPMs: Docker COMPM, for managing Docker Jobs, and RDB COMPMs for managing SQL Query jobs for :ref:`sciquery`.
COMPMs run a main (master) thread, which spawns worker treads that are individually in charge of running one job at a time. The main thread 
has an infinite loop, that each time gets new jobs from the JOBM API, and stores them in a queue in memory. Whenever a worker thread is 
available for a new job, it takes it from the queue and starts processing it. Docker COMPMs spawn DockerJob worker threads, whereas RDB COMPMs spawn RDBJob worker threads.
The main thread is also in charge of getting the list of cancelled jobs from the JOBM API.

Each COMPM needs to be registered in SciServer's RACM database and matched 
to the corresponding Compute Domain it manages the jobs for.
In the case of Docker COMPMs, SciServer admins can register them interactively on the `RACM UI <https://apps.sciserver.org/racm/compm/mvc/new>`_, 
whereas RDB COMPMs are registered with an HTTP POST request to `RACM's REST API <https://apps.sciserver.org/racm/jobm/rest/dbcompm>`_.
In both cases, admins need to specify the job timeout and the maximum number of concurrently running jobs per user, 
and are returned the Universally Unique Identifier (UUID) or ``CompmID`` for the new COMPM .
This CompmID needs to be later saved in the COMPM configuration, so that it can be passed as the ``X-Service-Auth-ID`` entry 
in request header when calling the RACM/JOBM or SciServer-Compute APIs as a means of authenticating itself.


**Configuring, Building and Running COMPMs**

The configuration variables for the Compute-Manager are placed in the ``config.properties`` and ``log4j2.xml`` files under 
``/src/main/resources/``. Example instances of those can be found under ``/conf.examples/``.

Both COMPM types share common configuration variables, stored in the ``config.properties`` file:

1) ``jobType``: the COMPM type. Can be ``docker`` or ``rdb``.

2)  The REST API URLs of JOBM and the FileService. For the latter, one should also include variables defining the path to the default job directory:
    ``basePath`` ,  ``rootVolume``, ``userVolume``, and ``jobsFolderRelativePath``.

3)  ``compmId``: unique identifier of this compm as registered in RACM. 
    Is is used for authenticating itself when communicating with other SciServer APIs.

4) ``numWorkers``: The number of worker treads, and same as the maximum number of jobs a COMPM can manage at a time.

5) ``idleTimeBetweenJobs``: idle time interval (measured in seconds) at the end of each JobWorker's cycle where the worker asks the main thread for a new job.

6) ``idleTimeBetweenJobmRequests``: idle time interval (measured in seconds) at the end of the main thread's loop, after it asks JOBM for new jobs and new messages for its managed jobs.

7) ``idleTimeBetweenFailedRequests``: idle time interval (measured in seconds) at the end of loops that repeatedly make calls to a REST API in case the API is unresponsive at the time.

8) ``maxTries``: max number tries before exiting after failed requests for submitting a job to the SciServer-Compute REST API.

9) ``maxNumberOfJobs``: maximum number of jobs asked to JOBM for COMPM to manage at a time.

10) ``minNumberOfJobs``: minimum number of jobs that COMPM can reach in its local queue, before COMPM starts asking JOBM for a new batch of available jobs.

11) RabbitMQ settings: For logging, activity and error messages can be sent to and queued on an external RabbitMQ instance in 
   order to be subsequently logged. One must set the RabbitMQ host, exchange and queue names.

On the other hand, ``log4j2.xml`` file contains configuration in relation to logging messages to a file.


Additionally, Docker COMPMs have the following extra configuration variable:

1) Compute.domainUrl: base url pointing to the SciServer-Compute REST API, associated to the Docker Compute Domain that COMPM is registered to.

whereas RDB COMPMs additionally require:

1) ``sciquery_db_jdbc_url``: JDBC URL, pointing to the SciQuery database.

2) ``sciquery_db_conn_pool_size``: pool size of the connection to the SciQuery database.

3) ``result_fetch_size``: batch size (number of rows) fetched at a time from a SQL query result set.

4) ``numRowsPerFlush``: batch size (number of rows) written at a time in an output writer (e.g. when writing to a CSV file).

5) ``httpRequestTimeout``: timeout for establishing an http connection to the SciServer FileService, 
   when using it to write a SQL query output result into the SciServer file system.

6) ``dbConnectionTimeout``: timeout for the table row insert statement for the case of writing a query result set into a database (might be deprecated).

Since the COMPM source code is integrated with `Gradle <https://gradle.org>`_ , 
one can build and run it locally by executing the respective Gradle targets in Visual Studio/Eclipse, or explicitly by executing ``./gradlew build`` or ``./gradlew run``
on the base level of the project directory. For running it in a production-grade environment, refer to the SciServer Kubernetes setup.




**Docker Jobs Life Cycle**

There are 2 types of Docker Jobs:

1) **Script Jobs**: involve any shell command given as input by the user. This command is automatically written in the ``parameters.txt`` file under the job directory.

2) **Notebook Jobs**: these job use the ``nbconvert`` command in Jupyter to execute all cells in a Jupyter Notebook, whose path in the SciServer file system is given as input by the user. 
   In case the notebook takes input parameters, these parameters can be passed to the job object during submission time, 
   and are automatically written into  ``parameters.txt`` file in the jobs directory, so that the Jupyter Notebook can easily read it during execution time.

In both cases, the standard output and error are automatically written into the ``stdout.txt`` and ``stderr.txt`` files 
under the job directory.


Running a Docker job requires a particular set of interactions between several SciServer components, as shown in the UML 
Sequence Diagram in :numref:`DockerJobLifeCycle` below and detailed as follows:

1) Client:
    Users can try the `JOBS section <https://apps.sciserver.org/compute/jobs>`_  in the SciServer-Compute UI to run a Docker job. 
    Alternatively, users can use the Jobs modules in the `SciScript-Python <https://github.com/sciserver/sciscript-python>`_  
    and `SciScript-R <https://github.com/sciserver/sciscript-r>`_ :ref:`sciscript` in order to execute jobs 
    from a script or Jupyter Notebook, allowing thus programmatic interactions.

2) :ref:`racm_jobm_api`: REST API used by clients to submit and cancel jobs, and
   to get a list of running jobs and their status. JOBM stores the list of all jobs 
   in the RACM registry database.

3) COMPM (Compute Manager): Stand-alone service that manages the life-cycle of a job. 
   This involves:

   a) Continuously getting new available jobs from JOBM and storying them in its local queue in memory.

   b) Automatically creating job directories in the SciServer file system for each new job through calls to the SciServer-FileService API, and copying job metadata into it.

   c) Sending the job definition for execution, by means of calling to the REST API of :ref:`compute` to spawn a Docker container where the job runs.

   d) Getting the jobs status from the SciServer-Compute API and setting status messages in jobs that have finished.

   e) Deleting the Docker container once the job is finished by means of a call to the SciServer-Compute API.
   
   f) Periodically updating job definition and status on JOBM by calls to its API.

4) :ref:`fileservice` `REST API <https://apps.sciserver.org/fileservice/swagger-ui/index.html>`_ : called by COMPM to create a job directory in the SciServer file system,
   and for copying the jobs definition and metadata into it.


5) :ref:`compute` REST API: called by COMPM to spawn a Docker container that runs the job, for getting the status of the container, and for deleting the container once the job is finished.




.. figure:: _static/DockerJobLifeCycle.drawio.png
   :align: center
   :name: DockerJobLifeCycle

