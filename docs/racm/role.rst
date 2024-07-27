.. _racm_role:

The Role of RACM in SciServer
=============================

RACM has a central role in SciServer.
(Almost) every other SciServer components, for example the :ref:`fileservice`, :ref:`compute`, or :ref:`sciquery`, 
will interact with RACM for almost every request that is made of them to check whether the user making the request
is allowed to perform it.

