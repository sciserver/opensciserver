# Administrator test service

A Java 17 Spring Boot backend for the version 1 runner contract owned
by [`tests/integration`](../../../tests/integration/contract/README.md). An admin page is available in the existing Vue dashboard. Python tests remain
independent of this service.

## API

All `/api/**` operations require an explicit `X-Auth-Token` header for a SciServer
administrator. Session cookies alone do not authorize requests. No cross-origin
access is enabled. Use HTTPS outside a local test environment.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/catalog` | Launch image discovery with `--list`. |
| POST | `/api/plans` | Launch dependency resolution with `--plan`. |
| POST | `/api/runs` | Launch selected tests, or the full suite. |
| GET | `/api/runs` | List discovery, plan and execution operations. |
| GET | `/api/runs/{id}` | Inspect orchestration state and provenance. |
| GET | `/api/runs/{id}/result` | Retrieve schema-validated catalog, plan or report. |
| GET | `/api/runs/{id}/logs` | Retrieve up to 2,000 lines / 1 MiB of runner logs. |
| DELETE | `/api/runs/{id}` | Request cancellation; retain Job and result history. |

POST plan/run accepts `{}` for the full suite, or
`{"tests":["fileservice.upload"]}`. Explicit empty lists are rejected.
Unknown IDs are reported by the image, which owns selection and graph semantics.
API callers cannot supply images, targets, namespaces, commands or Job templates.
POST operations return HTTP 202 and a run ID; poll its state, then fetch its
result. Discovery and planning are themselves short-lived Jobs, so catalog and
plan metadata come from the configured image rather than a second Java registry.
Do not use the old integration image from before the contract runner was added.
Pin the runner image digest to keep discovery/planning/execution reproducible.

State is `queued`, `running`, `finished`, `timed_out`, `cancelling` or `cancelled`.
`running` means the Job controller reports an active pod; it may still be pulling
an image. **`finished` is not a test outcome.** A process that exits 1 or 2 also
finishes its Job; read the report's `outcome` and counts. Missing reports return
409, never a synthetic pass/failure. Invalid or mismatched reports return 502.
The full Kubernetes condition list is included so the UI can explain deadline,
backoff and other infrastructure failures. Cancellation suspends the Job,
terminates active pods and cannot be undone through this API. A cancellation
request is not confirmation that the pod has already stopped.

Authorization uses the existing `:clients:racm` library on every request.
`RACMClient.getGroupsUserIsMemberOf(token)` supplies accepted/owned memberships;
the caller must belong to the exact `admin` group. Pending invitations and
ownership of unrelated groups do not grant access. `getUser(token)` supplies
the trusted requester identity. No separate compute-domain query is required.
This is stricter than RACM's current `UserProfile.isAdmin()` SQL, which checks
for a membership row without filtering its status: this service intentionally
does not treat a pending admin invitation as authorization.

Missing/invalid tokens return 401, non-admins 403, and RACM outages/malformed
responses 503. Upstream 401/403 responses are preserved without exposing their
bodies. A service-local `ServiceRacmClient` subclass uses the shared client's
existing protected adapter hook to configure bounded timeouts and disable redirects,
preventing redirected requests from forwarding the session token. It reuses
inherited RACM methods and response handling; the shared client sources remain
unchanged.

The token is neither stored in a Job nor passed to the runner. Discovery and
plan results use schemas copied from `tests/integration/contract` at build time.

## Build and test

Use JDK 17, as in the repository CI/container images:

```sh
cd components/java
# Existing RACM client convention: its models JAR must be built first.
./gradlew :racm:modelJar
./gradlew :test-service:build
```

From the repository root, `make test-service.image REPO=... VTAG=...` builds the
service image after the jar is built. Local builds require `SOURCE_REPOSITORY`
to identify the image's source repository; there is no default. Export it before
building, or pass it as a Make variable:

```sh
export SOURCE_REPOSITORY=https://github.com/OWNER/REPOSITORY
make test-service.image REPO=... VTAG=...
```

CI supplies this URL from `github.server_url` and `github.repository`.
A clean, targeted service build needs the
separate `:racm:modelJar` step above; the shared client's file dependency does not
automatically schedule it. The root `make java` already builds the models first.
The new component participates in the existing `java`, `images` and `publish-images` targets. The runner image must
also be rebuilt to include the versioned runner added in this branch.

The Java tests fake RACM and Kubernetes; they never launch real tests or mutate
a deployment. They cover admin rejection, fixed Jobs, selection limits, isolation
between owners, cancellation, missing reports and failed test reports after a
service restart.

## Helm deployment (opt-in)

The service is disabled by default. Enable it while retaining your other values:

```yaml
testService:
  enable: true
```

**No PVC, StorageClass, collector, or shared filesystem is needed.** Runner Jobs
write their report and delivery receipt to a pod-local `emptyDir`, then POST the
report through the optional callback defined by `tests/integration`. Discovery
and planning still use their JSON stdout results, retrieved from pod logs.

Helm automatically creates a reporting-key Secret and reuses it on upgrades.
The Secret is retained when the service is disabled/uninstalled. It is stable
configuration, not persistent report storage. Alternatively set
`testService.reportingSecret` to an existing Secret with a `signing-key` value
containing base64-encoded random bytes (at least 32 decoded bytes). For example,
create it with `kubectl create secret generic NAME --from-literal=signing-key="$(openssl rand -base64 32)"`.
Normal `helm upgrade` can look up and reuse the generated key; offline `helm template` rendering cannot discover an existing Secret, so use an explicitly
managed Secret when applying independently rendered manifests.

The backend issues an opaque HMAC-SHA256 credential scoped to the deployment,
run ID and expiry. It is passed only to that runner, not returned by admin APIs
or printed. It authorizes only `POST /reporting/{id}`; administrator session
headers do not authorize this endpoint, and reporting credentials do not
permit any administrator API operation. The signing key is never sent to pods.
Tokens expire after the execution deadline plus `callbackWindowSeconds` (120
by default). Jobs reserve an additional 60 seconds for the runner's delivery
budget. Each callback is also checked against the owned execution Job.

The inbox validates schema and identity, accepts identical retries, and rejects
conflicting replacements while the original is retained. Payloads are capped at
1 MiB, and the inbox retains at most `retainedReports` (50 by default, maximum
100), for `retentionSeconds` (3600 by default, maximum 86400). Oldest entries are
evicted at capacity. Memory bounds count serialized bytes per report; a report
plus JSON parsing adds transient overhead. No external database is involved.

A service restart loses received reports and deduplication knowledge. Kubernetes
retains Jobs and runner termination receipts; a receipt showing accepted delivery
with no cached report is reported as `lost`. An in-flight callback can still be
accepted after restart if the signing key is unchanged. A run whose runner died
without a report is distinct from assertions failing. Exit 3 identifies a runner
whose delivery/receipt failed; the report's test outcome remains unchanged.
`reportDelivery` is `pending`, `received`, `failed`, `lost`, `unavailable`, or
`not_applicable`. `unavailable` means no report or definitive receipt is known;
it is not automatically classified as a test failure. A lost acknowledgement may
cause exit 3 even though the service has the report; received data takes priority.

The chart supplies the image repository/tag, deployment base URL, internal RACM
URL, namespace, context path and image pull secret. Default target is the same
SciServer release; requests cannot select a different target. Tests are mutating
operations: use a development deployment.

For a deployment at `https://sciserver.example.org/test`, the API prefix is
`https://sciserver.example.org/test/test-service/api`. For example:

```sh
API=https://sciserver.example.org/test/test-service/api
# Supply an administrator's current SciServer session token.
read -rs -p 'SciServer token: ' TOKEN; echo
curl -H "X-Auth-Token: $TOKEN" -X POST "$API/catalog"
curl -H "X-Auth-Token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tests":["fileservice.upload"]}' "$API/plans"
curl -H "X-Auth-Token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tests":["fileservice.upload"]}' "$API/runs"
# Use an ID returned by a POST:
curl -H "X-Auth-Token: $TOKEN" "$API/runs/RUN_ID"
curl -H "X-Auth-Token: $TOKEN" "$API/runs/RUN_ID/result"
```

## Operational boundaries

A single service replica serializes admission and enforces `maxActive` (default
2), counting discovery and planning as well as tests. Keep one replica until
there is distributed admission control. Job deadlines default to 30 minutes;
automatic retries are disabled to avoid repeating mutating tests. Pods have no
mounted service-account token, no extra capabilities, and run as UID 10001.
Only the service receives a service-account token. Its namespace-scoped Role
allows Jobs get/list/create/patch, pods get/list, and pod logs get; it cannot
read Secrets or exec into pods. Kubernetes RBAC cannot restrict Job creation by
label, so the service account remains powerful within its namespace. Only trusted
operators should configure runner images or access its service account or signing key.

Jobs and requester attribution survive service restarts; accepted reports do not.
Discovery/plan results use pod logs, which are lost if their pods are deleted.
Operators still own Job/log retention and cleanup of remote users/files/groups
created by tests. The UI shows orchestration state, delivery state and test
outcome separately. `/actuator/health` is public for probes and reports JVM
health, not downstream availability. Run one service replica: report retention
and admission control are process-local; multiple replicas need a shared inbox
or sticky callback routing and distributed admission, neither provided here.

For development outside Kubernetes, configure `TEST_NAMESPACE`, `TEST_IMAGE`,
`TEST_BASE_URL`, `TEST_RACM_URL`, `TEST_CALLBACK_URL`, `TEST_REPORT_SIGNING_KEY`,
`TEST_KUBERNETES_URL`, `TEST_TOKEN_FILE` and `TEST_CA_FILE`. Keep the same signing
key across restarts and use a callback URL reachable from pods. Other knobs
include `TEST_CALLBACK_WINDOW_SECONDS`, `TEST_RETAINED_REPORTS`,
`TEST_RETENTION_SECONDS`, `TEST_OWNER`, `TEST_PULL_SECRET`,
`TEST_DEADLINE_SECONDS`, `TEST_MAX_ACTIVE` and `TEST_SERVICE_CONTEXT_PATH`.

## Dashboard admin page

The existing Vue dashboard exposes **Admin → Integration Tests** using its normal
router and session-token conventions. For the example deployment above, the
page is `/test/dashboard/admin/integration-tests`. The page discovers the
catalog, previews dependencies, launches full or selected suites, follows progress,
and displays structured results, execution conditions and logs. Recent operations
can be reopened, and active operations can be cancelled. Log retrieval failure
does not prevent reading a persisted result. Test failures are shown separately
from orchestration state; a finished Job is not presented as a passing test.

The page uses the dashboard's current session token in `X-Auth-Token`; authorization
remains enforced by the Java service. API requests stay on the dashboard origin
and preserve its deployment prefix. No React UI or HTTPS migration is required.
A disabled/unreachable backend produces an explanatory message rather than a
stuck loading indicator. Deploy updated dashboard and backend images, and enable
the service to use this page on the cluster.

Focused UI checks, after installing the dashboard's development dependencies:

```sh
cd components/ui/dashboard
npm run check:integration-tests
```

These compile the Vue template and check API routing, token handling, unavailable
logs/results, and response/polling cleanup after navigation without live HTTP.
