# RACM - Resource Access Control Manager

RACM is the central authorization and resource management service for SciServer.
It manages users, groups, resources, access control, compute jobs, and storage.

## Building

```sh
cd components/java
./gradlew :racm:modelJar && ./gradlew :racm:build
```

## Running Locally

RACM can be run locally via the Spring Boot main class
`org.sciserver.springapp.RACMApplication`. This starts an embedded Tomcat server
without needing an external servlet container.

### Prerequisites

- Java 17 JDK
- SQL Server with a `racm` database
- Two Java agent JARs (required by AspectJ load-time weaving and EclipseLink):
  - `aspectjweaver-1.9.22.1.jar` (available in Gradle cache after building)
  - `spring-instrument-5.3.31.jar` (download from Maven Central, store in `racm/lib/` which is gitignored)

### Configuration

Create `src/main/resources/application.properties` (gitignored) with your local
configuration. Use `helm/sciserver/files/racm-application.yaml` as a reference
for all available properties. At minimum you need:

```properties
spring.application.name=racm
server.servlet.context-path=/racm
spring.mvc.pathmatch.matching-strategy=ant-path-matcher

spring.datasource.url=jdbc:sqlserver://localhost;DatabaseName=RACM
spring.datasource.username=racm_user
spring.datasource.password=<your-password>

spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp
spring.main.allow-circular-references=true
spring.flyway.enabled=false

eclipselink.logging.level=WARNING

org.sciserver.racm.login.loginPortalUrl=http://localhost:8080/login-portal/
org.sciserver.racm.login.login-admin.username=admin
org.sciserver.racm.login.login-admin.password=<password>

org.sciserver.racm.admin.username=__racm__
org.sciserver.racm.admin.password=<password>
org.sciserver.racm.admin.email=racm@do.not.send
```

### JVM Arguments

Both Java agents must be passed as VM arguments when launching:

```
-javaagent:<path-to>/aspectjweaver-1.9.22.1.jar
-javaagent:<path-to>/racm/lib/spring-instrument-5.3.31.jar
```

Without these, `@EnableLoadTimeWeaving` and EclipseLink entity enhancement will
fail on startup.

### Verifying

The app starts on `http://localhost:8080/racm/`. Example endpoints:
- `GET /racm/ugm/rest/publicgroups` — list public groups
- `GET /racm/actuator/health` — health check

### Eclipse Setup

To debug in Eclipse:

1. **File > Import > Gradle > Existing Gradle Project**, browse to `components/java/`
2. Right-click `RACMApplication.java` in the `sciserver-java-racm` project > **Debug As > Java Application**
3. In **Run > Debug Configurations > Arguments > VM arguments**, add both `-javaagent` entries above

## Notes

- `jpa-config.properties` is loaded separately by EclipseLink via `RACMDatabaseConfiguration`
- External services (Login Portal, File Service, Logging) may not be available
  locally — the app will start but those features won't work
- The `aop.xml` excludes `org.springframework.boot.jdbc` from AspectJ weaving
  to avoid Oracle DataSource class errors
