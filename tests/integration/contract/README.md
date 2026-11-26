# Integration runner contract, version 1

This directory owns the interface between test images and consumers, including
CLI tools, the future test service, and its web UI. It does not require a
particular test framework, language, HTTP service, or Kubernetes client inside
an image. The Python implementation in `../runner.py` is the reference runner.

## Discovery and selection

Run the image with `--list` to emit `catalog.json` as JSON on stdout and exit
without contacting SciServer or creating resources. `catalog.schema.json`
defines its shape. IDs are stable public identifiers; Python implementation
paths are private runner metadata, not invocation parameters for consumers.
Names and descriptions are display text. Consumers should use the catalog
from the exact image they will execute, not a potentially newer checkout.

Run with repeated `--test ID` arguments to select tests, or supply `TEST_IDS`
as a JSON array of IDs. CLI selection takes precedence over the environment.
Omitting selection runs the whole catalog; an empty selection or unknown ID
is an error, never an accidental full-suite run. `--plan` resolves the same
selection without running it, producing an object with `contractVersion`,
`requested`, and an ordered `tests` array containing catalog entries plus a
`selected` boolean. The UI can preview included prerequisites using this plan.
`plan.schema.json` defines this response.

`dependsOn` contains hard prerequisites. Their transitive closure is included
automatically and each must pass before a dependent can run. If any prerequisite
fails, errors, skips, or is blocked, the dependent is `blocked`; `blockedBy`
names the direct prerequisites that did not pass. Other tests continue.

`after` is ordering only: it never adds tests to the selection and does not
require success. Logout is ordered after all included session consumers so
its token invalidation cannot disrupt them; it can still run after unrelated
failures. Registration and token validation are hard prerequisites where
needed. Upload and tree tests require volume discovery. Cycles, duplicate IDs,
and missing dependencies are configuration errors, rejected before execution.
Execution is sequential in v1. Catalog order breaks otherwise unconstrained
ties; consumers must not rely on alphabetical Python test ordering.

## Invocation and configuration

The default image entrypoint performs one run and exits. It must not prompt or
require stdin. `SCISERVER_BASE_URL` is required for execution: an HTTP(S) URL
including the deployment prefix, without credentials, query, or fragment.
`TEST_RUN_ID` is an optional nonempty opaque ID assigned by the caller; the
runner generates one when absent. `TEST_REPORT_PATH` defaults to
`/results/report.json` and must point to a writable file location. For a local
Python invocation, set it to a suitable local path.

Each run uses fresh private temporary state. It never reuses a previous run's
generated user or token. Tests may create remote users, files, groups, and
compute containers; successful logout does not clean these up. Cleanup is
still separate future work. Selecting one authenticated test therefore also
creates a new user through its prerequisites. Logout runs only if selected
explicitly or as a prerequisite, or as part of the default full suite.

## Results

`report.schema.json` is the normative JSON shape. A completed execution writes
one UTF-8 JSON report atomically to `TEST_REPORT_PATH` before exiting. A valid
report includes timestamps, run ID, requested IDs, ordered individual results,
and counts for all five statuses. `selected: false` identifies automatically
included prerequisites. Tests not in the resolved selection are absent, not
skipped. Blocked tests have zero execution and name their blocking prerequisites.
`durationSeconds` measures runner processing, not scheduling or image pulling.
`detail` is nullable diagnostic text, never HTML for direct rendering.

Test statuses are `passed`, `failed` (assertion), `error` (exception), `skipped`
(framework skip), and `blocked`. Skipped prerequisites block their dependents.
An all-skipped selection has outcome `passed` but does not imply coverage;
consumers must display counts and skips. No assertions are converted to skips
just because the deployment lacks a service.

Aggregate outcome and exit code:

| Outcome | Exit | Meaning |
| --- | --- | --- |
| `passed` | 0 | No failed, errored, or blocked tests. |
| `failed` | 1 | Assertion failures or blocked tests, without errors. |
| `error` | 2 | Test exceptions or runner/configuration errors. |
| Unchanged | 3 | Callback delivery or delivery-receipt failure; inspect the report outcome separately. |

Errors take precedence over failures. `runnerError` distinguishes a runner
failure from individual test errors. Counts must equal result status totals;
IDs are unique and result order follows the resolved plan. Examples cover
passing, failing, blocked, and runner-error reports. Consumers should reject
unsupported contract versions rather than guessing their meaning.

Stdout/stderr contain readable progress and failure diagnostics, not a report
transport. No consumer should parse framework logs to infer structured results.
The adapter redacts the generated user's password and token in diagnostics;
test authors must also avoid logging secrets, credentials, and raw sensitive
responses. User-facing services should restrict log/report access to authorized
callers. Render diagnostic strings as text.

## Lifecycle and transport responsibilities

The caller enforces deadlines and cancellation. Killing the process, an OOM,
an image pull failure, an unwritable report path, or node loss can leave no
report. Missing reports are never success and must not be manufactured as
assertion failures. The service owns scheduling, requester identity, timeout,
cancellation, transport errors, and Job state, separately from test outcomes.
V1 does not promise an incremental report or graceful cancellation report.

For Docker, mount a writable host directory at `/results` to retain the files.
For Kubernetes, an `emptyDir` is sufficient when a callback is configured. The
runner needs no knowledge of Kubernetes or a particular test service. Without a
callback or external file collection, a stopped container's report is not
retrievable through `kubectl cp`/`exec`; logs and exit status remain available.
No shared or persistent filesystem is required by this contract.

## Optional report callback

Execution always writes the final report locally first. Callback delivery is an
optional version-1 extension; existing invocations without callback settings
keep their 0/1/2 outcome exit codes. `--list` and `--plan` remain side-effect-free
JSON stdout operations: they do not invoke callbacks or require a deployment URL.
Consumers collect those bounded JSON outputs from logs rather than report POSTs.

| Environment variable | Meaning |
| --- | --- |
| `TEST_REPORT_CALLBACK_URL` | Optional HTTP(S) endpoint to POST the unchanged report. No URL credentials, query or fragment. |
| `TEST_REPORT_CALLBACK_TOKEN` | Optional printable ASCII bearer credential; requires a callback URL. |
| `TEST_REPORT_CALLBACK_TIMEOUT_SECONDS` | Total delivery retry budget, 1–300 seconds, default 60. |
| `TEST_REPORT_DELIVERY_PATH` | Delivery receipt file, default `delivery.json` beside the report. Must differ from the report path. |

The POST uses `Content-Type: application/json` and, if supplied,
`Authorization: Bearer <token>`. Only HTTP 200, 201 or 204 acknowledges acceptance.
Acceptance means the receiver has taken responsibility for the report, not that
it durably persisted it. An asynchronous 202 is not an acknowledgement. Redirects
are never followed, so credentials cannot be forwarded to a different endpoint.
Use HTTPS outside a trusted internal development network; the runner uses normal
certificate verification.

The same JSON bytes and run ID are reused on retry. Retry network/transport
failures, 408, 429 and 5xx within the budget, with exponential delays capped at
five seconds and individual network timeouts capped at ten seconds. Other HTTP
responses, including expired/invalid credentials, conflicts and oversize reports,
are terminal delivery failures. No credential, callback URL, remote error body
or transport exception text is printed to logs or added to results. The maximum
callback payload is 1 MiB; larger reports remain in their local file but cannot
be delivered through this extension.

The receiver authenticates the credential and its run scope, validates the
report schema and run ID, and acknowledges an identical retry without duplicate
processing. A different report for an already-retained run must be rejected
with 409. Token issuance/format, receiver storage and retention are outside the
runner contract. An idempotency guarantee can only last as long as the receiver
retains the accepted report or a deduplication record; a restart or eviction can
remove that knowledge in a deliberately memory-only receiver.

After delivery attempts, write a small `delivery.schema.json` receipt containing
the run ID, unchanged test outcome, and `reportDelivery`: `not_configured`,
`received`, or `failed`. This file can serve as a Kubernetes termination message;
it contains no bearer credential. With successful delivery or no callback,
exit 0/1/2 follows the report outcome. **Exit 3 means delivery/receipt failure**,
independent of the test outcome. The final test report is never rewritten to turn
a successful test into a failure because upload failed. A runner killed before
completion may produce neither report nor receipt, so consumers must also
inspect execution state.

Jobs do not retry automatically, to avoid repeating mutations. Each invocation
must use a fresh Job name. Treat image tags such as `pr-146` as mutable; use an
image digest for reproducible catalog/execution matching. New incompatible
semantics require a new contract version. The service and UI consume this
contract; they do not own it.

## Local validation

Install `../requirements.txt` and `../runner_checks/requirements.txt`, then run
from `tests/integration`:

```sh
python -m unittest discover -s runner_checks -p 'check_*.py' -v
```

These checks use fake HTTP responses, not a live deployment, and cover graph
validation, selection, blocking, logout ordering, real unittest adaptation,
report schemas, and exit codes.
