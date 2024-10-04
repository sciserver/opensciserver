.. _sciscript:

SciScript Python and R client libraries
=======================================

The SciScript `Python <https://github.com/sciserver/sciscript-python>`_  and `R <https://github.com/sciserver/sciscript-r>`_  
libraries can be imported within pyhton/R code for the direct interaction with SciServer components and resources.
These libraries wrap HTTP requests to the REST APIs of SciServer components, such as CasJobs, FileService, JOBM, SciQuery, or LoginPortal, 
passing along the SciServer authentication token in the request header to ensure authentication (and authorization). 

Although the SciScript libraries can be installed remotely by any user on their own computer, 
they are more useful when users import these libraries within their code when running it in SciServer Compute, 
as server-side calls through the fast local network reduces data transfer times. 
For example, one can use the CasJobs or SciQuery modules to run SQL queries and quickly retrieve the results 
tables as suitable data frame objects for further analysis within the code.

When imported within code being run in a SciServer Compute container, the SciScript library automatically authenticates the user 
by reading its auth token under ``/home/idies/keystone.token`` and passing it along with the HTTP requests to SciServer APIs. 
On the other hand, running SciScript in a remote computer required users to explicitly login with their SciServer credentials 
using its ``Authentication.login`` method before being able to interact with other SciServer components.