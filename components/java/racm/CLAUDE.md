# RACM Component

## Code Style
- Java 17 project
- ALWAYS use Gradle for package dependencies and managing the environment
- Always use SIMPLEST code and structure, don't over-engineer
- Don't create unnecessary files. When creating new version of a file, archive or delete the legacy file
- DO NOT ADD UNNECESSARY FEATURES! Keep code simple

## Building
- Gradle root is at `components/java/` (where `settings.gradle` lives)
- Build racm and deps: `cd components/java && ./gradlew :racm:modelJar && ./gradlew :racm:build`
- RACM's `build.gradle` excludes `spring-boot-starter-logging` to avoid log4j bridge conflicts

## Eclipse Setup
- Import as Gradle project from `components/java/` — creates separate projects per subproject
- Buildship prefs `connection.project.dir` must point to the Gradle root (`..` for depth-1, `../..` for depth-2 subprojects)
- Do NOT use the legacy flat `.classpath` at root (renamed to `.classpath.bak`)

## Debugging in Eclipse
- Run/debug `org.sciserver.springapp.RACMApplication` as Java Application
- Requires two `-javaagent` VM arguments in Debug Configuration:
  - `-javaagent:<path>/aspectjweaver-1.9.22.1.jar` (AspectJ load-time weaving, found in Gradle cache)
  - `-javaagent:<path>/spring-instrument-<version>.jar` (Spring classloader instrumentation, stored in `racm/lib/`; version should match Spring Framework)
- The `lib/` folder is gitignored and holds JARs needed only for local Eclipse debugging
- Without these agents, `@EnableLoadTimeWeaving` and EclipseLink entity enhancement will fail

## Configuration
- `application.properties` consolidates all config for local development (server, login, admin, DB, URLs, etc.)
- `jpa-config.properties` is loaded separately by EclipseLink via `RACMDatabaseConfiguration`
- `persistence.xml` lists all JPA entity classes
- In production, Helm generates `racm-application.yaml` with all config (see `helm/sciserver/files/racm-application.yaml`)

## Key Architecture
- Spring Boot with EclipseLink JPA (not Hibernate)
- AspectJ for `@Transactional` support (`AdviceMode.ASPECTJ`)
- SQL Server database
- Auth: header, cookie, and query param filters via Spring Security
- Depends on: `springutils:logging-interceptor`, `springutils:authenticator`, `clients:auth`
