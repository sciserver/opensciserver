# SciServer integration tests

Imported from the SciServer integration-test repository. The runner exposes a
versioned catalog, dependency plan, and structured JSON report for the CLI and
manually launched pods. Generalized execution through the administrator web UI
is deferred while test-service is adapted for toy-data generation and cleanup.

## Provenance

Imported from [sciserver/sciserver-integ-tests](https://github.com/sciserver/sciserver-integ-tests)
at commit [`5c4a039a379a6e06ee752bcac04698599880b4e5`](https://github.com/sciserver/sciserver-integ-tests/commit/5c4a039a379a6e06ee752bcac04698599880b4e5).
The import into `tests/integration/` preserves the original Git history and
authorship through a non-squashed Git subtree merge.

## Required test infrastructure

A reachable test-service with its account broker configured is required for all
execution. See [service setup](../../components/java/test-service/README.md#account-broker).
There is no fallback that registers unmanaged accounts. Discovery and planning
remain offline and need no broker or deployment credentials.

Broker reservations are independent of execution Jobs. CLI execution stays on
your machine and needs no Kubernetes access. The broker keeps durable ownership
in separate suspended Jobs, which never run pods. It holds dedicated cleanup
administrator credentials; runners receive only temporary account credentials
and a run-scoped capability. Retain the broker records and stable signing key.

The deployment must permit registration without email verification. Fileservice
tests additionally expect Storage/persistent and Temporary/scratch user volumes;
compute execution requires an accessible domain and notebook image. Missing
capabilities produce failures rather than silently skipped tests.

## Service and test-data dependencies

The dependency maps live in separate documents so the graphs and lifecycle
details can grow without obscuring these runner instructions:

- [Service dependencies](service-dependencies.md): core APIs,
  databases, UI dependencies, optional integrations and chart startup ordering.
- [Toy-data dependencies and lifecycle](toydata/README.md):
  accounts, groups, memberships, permissions, volumes, files and execution;
  population/cleanup order; ownership tracking; existing API cleanup gaps; and
  an inventory of what each current test creates.

The [concrete toy-data plan](toydata/README.md#proposed-concrete-toy-data-layers)
has three layers: RACM users/groups, NFS-backed fileservice volumes and known
content, and two compute domains on the deployment's existing Kubernetes cluster
for testing access separation.

Population and depopulation tooling is not implemented on this branch yet.
Logout and deletion of a Kubernetes Job do not remove application data. In
particular, deleting a login account does not cascade across RACM, fileservice
and compute; cleanup must preserve owner access until its dependent operations
finish. See the lifecycle document before designing new data-creating tests.

## CLI

Install runner dependencies and discover tests:

```sh
python3 -m pip install -r tests/integration/requirements.txt
python3 tests/integration/runner.py --list
python3 tests/integration/runner.py --plan --test fileservice.upload
```

Put your current SciServer administrator session token in a protected file, then
run locally against the account broker:

```sh
export TEST_REPORT_PATH="$PWD/results/report.json"
python3 tests/integration/runner.py \
  --broker-url https://sciserver.example.org/test/test-service \
  --admin-token-file /path/to/protected-session-token \
  --test racm.user
```

Omit `--test` for the full suite, or repeat it to select multiple cases. `TEST_IDS`
is a JSON array alternative. An explicit empty selection is invalid. Use this
runner rather than invoking nose2 directly: it owns fixtures, ordering and cleanup.
The broker assigns the run ID and target base URL; an explicitly configured
`SCISERVER_BASE_URL` must match it.

## Dependencies and fixtures

Catalog entries declare `fixtures` independently of `dependsOn`:

- Ordinary authenticated tests use a brokered `session` fixture.
- Registration tests reserve their own identity and exercise the normal API.
- Logout/revoked-token tests use a separate `logout` fixture, preserving its
  invalidated token without affecting other tests.
- Fileservice upload/tree depend on volume discovery; revoked-token depends on
  logout; compute creation also depends on compute preflight.

Selecting `racm.user` runs that test with its fixture, without running registration
or token tests first. Failed hard prerequisites block dependents; fixture errors
are reported on tests that need them. Independent tests continue.

## Pod execution

The service no longer launches runner Jobs. To launch the manual Job template,
first reserve
an independent run through the same admin API (requires curl, jq and envsubst):

```sh
export BROKER_SERVICE_URL=https://sciserver.example.org/test/test-service
reservation=$(curl --fail-with-body -sS -X POST \
  -H "X-Auth-Token: $(cat /path/to/protected-session-token)" \
  "$BROKER_SERVICE_URL/api/broker/runs")
export TEST_RUN_ID=$(printf '%s' "$reservation" | jq -r .runId)
export TEST_ACCOUNT_BROKER_TOKEN=$(printf '%s' "$reservation" | jq -r .token)
export TEST_ACCOUNT_BROKER_URL="$BROKER_SERVICE_URL/account-broker/$TEST_RUN_ID"
export SCISERVER_BASE_URL=$(printf '%s' "$reservation" | jq -r .baseUrl)
export TEST_IMAGE=YOUR_REGISTRY/integration-tests:YOUR_TAG

envsubst '${TEST_IMAGE} ${SCISERVER_BASE_URL} ${TEST_RUN_ID} ${TEST_ACCOUNT_BROKER_URL} ${TEST_ACCOUNT_BROKER_TOKEN}' \
  < tests/integration/k8s/job.yaml | kubectl -n YOUR_NAMESPACE create -f -
```

The generated Job name is not the broker run ID. Pods need network access to the
broker and deployment; desktop `/etc/hosts` entries are not inherited. HTTPS
certificates must be trusted by both runner and service. Rebuild the runner and
test-service images together when changing the contract. The root Makefile's
`images` and `publish-images` targets include the runner image.

## Reports and cleanup

Reports default to `/results/report.json`. Exit codes: 0 passed, 1 test failure,
2 test/runner/cleanup error, 3 reporting delivery failure. The test-service reporting callback has been removed. For a separately supplied
receiver, optional runner callback reporting uses `TEST_REPORT_CALLBACK_URL` and `TEST_REPORT_CALLBACK_TOKEN`; it is
independent of the required fixture broker and preserves the underlying outcome.
See the [contract](contract/README.md) and [broker protocol](contract/account-broker.md).

The runner releases all account reservations after execution, including failures.
The broker retries abandoned/expired runs. Files, groups and containers have
per-test cleanup hooks that also run on assertion failures. Forced pod termination
can prevent those hooks. RACM records, provisioned volumes and audit history are
not erased by account deletion; this is not yet a zero-impact suite. Partial
registration that creates a project without a verifiable user remains pending
for manual ownership review rather than risking deletion of unrelated resources.

## Focused checks

```sh
python3 -m pip install -r tests/integration/runner_checks/requirements.txt
python3 -m unittest discover -s tests/integration/runner_checks -p 'check_*.py'
```

These use fake APIs and never mutate a live deployment.
