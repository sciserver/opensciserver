# compute-manager (COMPM)

COMPM is the agent that runs jobs (as opposed to interactive containers) for a
single compute domain. A `docker` COMPM runs Compute jobs in containers, an
`rdb` COMPM runs SciQuery SQL jobs. See `docs/compm/compm.rst` for details.

## Build

COMPM depends on the `logging` project and on the RACM models jar, so build it
from `components/java`:

```
./gradlew :racm:modelJar
./gradlew :compm:build
```

This creates a fat jar in `build/libs/compm.jar`. The docker image is built
from the repository root with `make compm.image`.

## Configuration

Configuration is read from the file named by the `SCISERVER_PROPERTIES_FILE`
environment variable, or else from `config.properties` on the classpath (copy
`conf.examples/config.properties` to `src/main/resources` and edit it). The DB
settings are only needed by `rdb` COMPMs.

Logging goes to stdout by default, see `src/main/resources/log4j2.xml`.
`conf.examples/log4j2-file.xml` logs to a rotating file instead.

## Registration

Each COMPM must be registered in RACM, which gives the COMPM UUID to set as
`Compm.compmId`:

- docker COMPMs: click "Register new COMPM" at `{racm url}/compm/mvc/list` and
  choose the compute domain the COMPM will run jobs for.
- rdb COMPMs: POST to `{racm url}/jobm/rest/dbcompm`.

## Deploying with helm

COMPMs are deployed by the sciserver helm chart, one deployment per entry in
`compm.instances`. None are deployed by default, since they first need to be
registered. After registering, add for example:

```yaml
compm:
  instances:
    small-jobs:
      compmId: "{UUID from RACM}"
      jobType: docker
      domainId: 3
```

and upgrade the release. The RACM, fileservice, login portal and logging URLs
are filled in from the release. See the `compm` section of
`helm/sciserver/values.yaml` for all settings.

## Running locally

```
SCISERVER_PROPERTIES_FILE=/path/to/config.properties java -jar build/libs/compm.jar
```
