.. _racm_api:

RACM API
========

RACM exposes a REST API for managing users, groups, resources and the rights to act on them,
and for brokering access to compute domains and file services.

The API is documented with OpenAPI and published as a browsable reference:

`RACM REST API <https://apps.sciserver.org/racm/swagger-ui/index.html>`_

The reference is generated from the service itself, so it always matches the deployed version.
A local instance serves the same pages at ``http://localhost:8080/racm/swagger-ui/index.html``.

Authentication
--------------

Most endpoints require a SciServer user token, passed as an ``X-Auth-Token`` header and obtained
from the login portal. Use the **Authorize** button in the reference to supply a token once; the
*Try it out* controls then include it automatically.

Some endpoints are called by other SciServer components rather than by end users, and require a
service token passed as ``X-Service-Auth-ID``. These are marked in the reference with a
``serviceToken`` requirement. A few, such as the file service volume endpoints, require both.

Endpoint groups
---------------

The reference groups endpoints by purpose. Of these, jobs and compute domains, users and groups,
access control and file services are intended for end users and their clients; the compute domain
manager and file service integration groups exist for other SciServer components.

.. _racm_jobm_api:

JOBM API
========

Job submission and monitoring, and discovery of the compute domains, images and volumes a user may
use. See the *Jobs and compute domains* and *Relational database jobs* groups in the reference.

.. _racm_storem_api:

STOREM API
==========

Registration of file services and the volumes they expose. See the *File services* group in the
reference; the remaining file service groups are called by FileService instances rather than by
users.
