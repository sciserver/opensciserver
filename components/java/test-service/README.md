# Administrator test service

The Java 17 Spring Boot service is being narrowed to toy-data generation and
unwinding. Account brokerage and administrator authorization remain. Dataset
operations and the operation journal are not yet implemented. Guarded RACM
profile deletion is available as an opt-in broker cleanup step. See the [toy-data design](../../../tests/integration/toydata/README.md).

Generalized catalog discovery, planning, execution Jobs and report callbacks/storage
have been removed. The existing dashboard route explains the transition instead
of calling removed APIs. The Python runner remains usable from the CLI or manually
launched pods using the account broker.

## Account broker

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/broker/runs` | Administrator creates a reservation. |
| GET | `/account-broker/{id}` | Retrieve its run context. |
| POST | `/account-broker/{id}/accounts/{scope}` | Reserve a session, registration or logout identity. |
| POST | `/account-broker/{id}/accounts/{scope}/registered` | Record the registered identity's ownership. |
| DELETE | `/account-broker/{id}` | Release accounts and request cleanup. |

The admin endpoint requires an explicit `X-Auth-Token` and accepted/owned membership
in RACM's exact `admin` group, checked through the existing RACM client. Cookies
and broker capabilities cannot authorize it. Broker endpoints instead require the
run-scoped Bearer capability. Invalid sessions return 401, non-admins 403, and
upstream availability or malformed responses 503. Use HTTPS outside local development.

Ownership survives restarts in suspended Kubernetes Jobs that never start pods.
The broker retries expired/abandoned reservations and deletes verified, recorded
identities. Retain these Jobs and the stable signing key until cleanup completes.
Unverifiable partial registration remains pending for review. This does not erase
files, volumes or history. RACM profiles are removed only when guarded SQL
cleanup is enabled and no unexpected references remain. Keep a single service replica while
cleanup coordination is process-local.

Configure a dedicated Keystone administrator for cleanup; RACM admin membership
alone is insufficient. Inject `TEST_ACCOUNT_ADMIN_USER` and
`TEST_ACCOUNT_ADMIN_PASSWORD` from a protected Secret through your deployment
overlay, along with `TEST_ACCOUNT_BROKER_ENABLED=true` and
`TEST_ACCOUNT_KEYSTONE_URL=http://YOUR_RELEASE-keystone:5000/v3`. Runners receive
only temporary fixture credentials and run-scoped capabilities. Other services
remain unchanged. See the [broker protocol](../../../tests/integration/contract/account-broker.md)
and [CLI instructions](../../../tests/integration/README.md).

## Helm and configuration

`testService.enable` controls deployment. Helm supplies namespace, target URL,
internal RACM URL, ledger image and broker signing key. Ownership Jobs use the
service image as a required Kubernetes template field; the service no longer
needs a runner image. Its Role permits Jobs get/list/create/patch, with no pod or
log access. RBAC cannot restrict Job creation by label; protect the service account.

`testService.accountLeaseSeconds` defaults to 1,980, matching the former runner
lease duration. It bounds legacy reservations, not future datasets. The historical
`testService.reportingSecret` value and default `*-reporting` Secret name remain
to preserve existing signing keys. Helm reuses the key on upgrades and retains
the Secret on uninstall. Offline rendering should use an explicitly managed
Secret because it cannot look up the deployed key. No PVC is currently required;
the future durable dataset journal is separate work.

For local development configure `TEST_NAMESPACE`, `TEST_LEDGER_IMAGE`,
`TEST_BASE_URL`, `TEST_RACM_URL`, and `TEST_BROKER_SIGNING_KEY` (base64 text encoding
at least 32 random bytes). The latter replaces `TEST_REPORT_SIGNING_KEY`; Helm
supplies the new name. Optional settings include `TEST_ACCOUNT_LEASE_SECONDS`,
`TEST_OWNER`, `TEST_KUBERNETES_URL`, `TEST_TOKEN_FILE`, `TEST_CA_FILE`, and
`TEST_SERVICE_CONTEXT_PATH`.

## Build and test

Use JDK 17 and build the shared client's models JAR first:

```sh
cd components/java
./gradlew :racm:modelJar :test-service:clean :test-service:build
```

Tests fake RACM, Keystone and Kubernetes. They cover authorization, ownership,
cleanup retries/restarts and removal of runner routes without mutating a deployment.
The service no longer copies runner schemas or uses a schema validator.

From the repository root:

```sh
export SOURCE_REPOSITORY=https://github.com/OWNER/REPOSITORY
make test-service.image REPO=YOUR_REGISTRY/YOUR_PROJECT VTAG=YOUR_TAG
```

CI supplies the source URL. Existing Java/image build targets include the service.

## Guarded RACM profile cleanup

Enable `testService.racmCleanup.enabled` to remove dependency-free, broker-owned
RACM profiles during account release. It is disabled by default and requires
explicit SQL Server credentials (Helm can reference the existing database Secret).
The admin preview is `GET /api/broker/runs/{id}/accounts/{scope}/racm-deletion`.
It reports blockers without deleting data; release rechecks inside a transaction.
See the [dependency investigation and configuration](../../../tests/integration/toydata/racm-deletion.md)
for limits, credential settings, and the required SQL Server rehearsal. There is
no arbitrary-user delete endpoint and no automatic cascade through owned groups,
volumes, jobs or grants.
