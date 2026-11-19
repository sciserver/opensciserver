# SciQuery REST API

Stand-alone Spring Boot service that lets users run SQL against the relational
databases registered in SciServer, browse their metadata, and upload tables into
them. See `docs/sciquery/sciquery.rst` for what it does and how it fits together
with RACM, the RDB compute manager and the FileService.

The SQL parser and table ingestor it relies on live alongside it in
[`../sciquery-utils`](../sciquery-utils).

## Building

From `components/java`:

```sh
./gradlew :racm:modelJar   # once; :clients:racm reads the RACM model classes from it
./gradlew :sciquery:build
```

That produces `build/libs/sciquery.jar`. The container image is built from the
repository root with `make sciquery.image`, which expects that jar to be in
place (`make java` builds it).

## Running locally

The service reads its configuration from the file named by the
`SCISERVER_PROPERTIES_FILE` environment variable, falling back to an
`application.properties` on the classpath. `src/main/resources/application.properties.example`
lists every property with its meaning, and `conf-example/log4j2.xml` is a sample
log4j2 configuration.

It needs a SQL Server database of its own for its registry; flyway creates and
migrates the schema from `src/main/resources/db/migration` on startup.

## Deploying

The Helm chart deploys SciQuery and its registry database under
`helm/sciserver/templates/sciquery/`, configured by the `sciquery` and
`sciqueryDb` sections of `helm/sciserver/values.yaml`.

It is off by default (`sciquery.enable: false`). Turning it on requires values
that only exist once RACM has been bootstrapped and the databases SciQuery
federates have been registered — a service token, an admin group id, a default
database server id, and the domain and server holding the MyDBs. There is no
bootstrapper that can derive these, so they have to be supplied.
