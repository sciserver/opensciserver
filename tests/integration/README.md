Integration tests for sciserver.

## Provenance

Imported from [sciserver/sciserver-integ-tests](https://github.com/sciserver/sciserver-integ-tests)
at commit [`5c4a039a379a6e06ee752bcac04698599880b4e5`](https://github.com/sciserver/sciserver-integ-tests/commit/5c4a039a379a6e06ee752bcac04698599880b4e5).
The import into `tests/integration/` preserves the original Git history and
authorship through a non-squashed Git subtree merge.

## About

These tests exercise a deployed SciServer through HTTP APIs. They can run
manually, from CI, or as a Kubernetes Job; Jenkins is not required.

The versioned runner interface is owned here: see [the contract](contract/README.md),
[the test catalog](contract/catalog.json), and [the report schema](contract/report.schema.json).
The runner expands selected tests' prerequisites, enforces dependencies, and orders
logout after selected session consumers. Consumers do not need to know Python
method names or nose2 ordering.

## Caveats

* The registration tests do not support email verification in
  registration, if that is required they will fail! No test presently
  for logging in from supplied credentials

* Tests presently do not excersize UIs, just a set of functional API
  calls. Can consider adding Selenium tests for browser emulation
  in the future.

## Running tests

Run these commands from `tests/integration/`. The requirements are listed in
requirements.txt, so if needed first install via pip:

```
pip install -r requirements.txt
```

Use the contract runner for both full and individual runs:

```sh
export SCISERVER_BASE_URL=https://kubetest.sciserver.org/ak-1
export TEST_REPORT_PATH="$PWD/results/report.json"

python runner.py --list
python runner.py --plan --test fileservice.upload
python runner.py --test fileservice.upload
# Or run the full suite:
python runner.py
```

Selection can also be supplied as a JSON array in `TEST_IDS`, e.g.
`TEST_IDS='["racm.user","identity.validate"]'`. Each run has fresh local state;
authenticated selections automatically include registration and token checks.
A failed prerequisite blocks its dependents while independent tests continue.
The runner emits readable diagnostics and a structured JSON report. Exit codes
are 0 (passed), 1 (failed/blocked), 2 (test or runner error), and 3 (callback/receipt failure). The direct nose2
invocation is legacy and does not enforce this contract or produce its report.

## Running against a Helm deployment from Kubernetes

### 1. Check the deployment

After installing SciServer with Helm, wait for its workloads to become ready
and verify its ingress URL. Substitute your release and namespace:

```sh
helm status YOUR_RELEASE -n YOUR_NAMESPACE
kubectl get pods -n YOUR_NAMESPACE
kubectl get ingress -n YOUR_NAMESPACE
```

Use the deployment base URL, including any installation prefix and without a
trailing slash. For example, `https://sciserver.example.org/test` makes the tests
call `/test/racm/...`, `/test/fileservice/...`, etc. The hostname must resolve
and be reachable from the Job's pod. Desktop `/etc/hosts` entries are not
inherited by pods. HTTPS certificates must be trusted by the test container.

The imported suite assumes more than a migrated RACM database:

- Registration is enabled without email verification.
- New users receive the `Storage/persistent` and `Temporary/scratch` volumes
  expected by `test_api.py`.
- An accessible compute domain has an available notebook image.

A Helm install alone does not guarantee those prerequisites. Run against a
development/test deployment: the suite registers a user, uploads a file,
creates a group, and starts a compute container. Logout invalidates the session
but does not delete the user, file, or group. Automatic cleanup of that data
is still future work.

### 2. Build and publish the test image

The root Makefile includes the test image in both `images` and
`publish-images`. GitHub Actions therefore publishes it alongside the component
images for pull requests, main-branch builds, and releases. To build and push
it manually from the OpenSciServer root:

```sh
export TEST_IMAGE=YOUR_REGISTRY/YOUR_PROJECT/integration-tests:YOUR_UNIQUE_TAG
docker build --platform linux/amd64 -t "$TEST_IMAGE" tests/integration
docker push "$TEST_IMAGE"
```

Or use the same Makefile target as CI:

```sh
make integration-tests.image REPO=YOUR_REGISTRY/YOUR_PROJECT VTAG=YOUR_UNIQUE_TAG
make integration-tests.push REPO=YOUR_REGISTRY/YOUR_PROJECT VTAG=YOUR_UNIQUE_TAG
```

Authenticate to the registry first if required. Match the image architecture
to your nodes; amd64 matches the project's current image builds. A desktop-only
image is not automatically available on Kubernetes nodes in other VMs.
For a private registry, create an image pull secret in the test namespace and
reference it in the Job's `spec.template.spec.imagePullSecrets`.

The same image can run directly from a terminal. Mount a writable results
directory to retain its structured report:

```sh
mkdir -p results
docker run --rm -v "$PWD/results:/results" \
  -e SCISERVER_BASE_URL=https://sciserver.example.org/test \
  "$TEST_IMAGE"
```

### 3. Launch a Job

The checked-in `k8s/job.yaml` is a small `envsubst` template. It runs
independently of Helm; rerunning tests does not require another upgrade.
Set the image, deployment URL, and namespace, then render and create it:

```sh
export TEST_NAMESPACE=YOUR_NAMESPACE
export TEST_IMAGE=YOUR_REGISTRY/YOUR_PROJECT/integration-tests:YOUR_UNIQUE_TAG
export SCISERVER_BASE_URL=https://sciserver.example.org/test

: "${TEST_NAMESPACE:?Set TEST_NAMESPACE}"
: "${TEST_IMAGE:?Set TEST_IMAGE}"
: "${SCISERVER_BASE_URL:?Set SCISERVER_BASE_URL}"

test_job=$(
  envsubst '${TEST_IMAGE} ${SCISERVER_BASE_URL}' < k8s/job.yaml |
    kubectl -n "$TEST_NAMESPACE" create -f - -o name
)
kubectl -n "$TEST_NAMESPACE" wait --for=condition=Ready pod \
  -l "job-name=${test_job#job.batch/}" --timeout=120s
kubectl -n "$TEST_NAMESPACE" logs -f "$test_job" -c tests
```

Run these commands from `tests/integration/`. `envsubst` is provided by the
`gettext-base` package on Ubuntu. If your registry is private, add the
appropriate `imagePullSecrets` entry to `k8s/job.yaml` or patch the rendered
manifest before creating the Job.

If the pod is not created yet, retry the wait. Readiness may also time out if
the pod finishes quickly or cannot start; inspect the Job and pod, and try the
logs command regardless. `generateName` gives each run a distinct name; use
`create`, not `apply`.

The runner resolves the catalog dependency graph and executes the selected
tests (the full suite by default). Set `TEST_IDS` in the Job environment to
select individual IDs; merely exporting it locally does not change the template. Retries are disabled to avoid repeating mutations and
registering more users. The 30-minute deadline bounds the run, including HTTP
requests that currently have no explicit timeout.

### 4. Inspect results and remove the Job

The sample Job uses temporary local storage. Without a reporting callback, logs
and exit status are available but its report file cannot be copied out after the
container exits. The test service supplies a generic callback URL and temporary
bearer token automatically; a CLI runner can optionally provide the same
`TEST_REPORT_CALLBACK_URL` and `TEST_REPORT_CALLBACK_TOKEN` configuration. See
[callback delivery](contract/README.md#optional-report-callback).

Following logs does not establish success. Check Job conditions and the
container exit code:

```sh
kubectl -n "$TEST_NAMESPACE" describe "$test_job"
kubectl -n "$TEST_NAMESPACE" get pods \
  -l "job-name=${test_job#job.batch/}" \
  -o jsonpath='{range .items[*]}{.metadata.name}{": "}{.status.containerStatuses[0].state.terminated.exitCode}{"\n"}{end}'
```

Successful tests exit zero; failing tests exit nonzero. Scheduling, image-pull,
and deadline failures are infrastructure failures and may not have a test
exit code. Inspect pod events if the runner never starts.

The contract runner writes JSON to `/results/report.json`, and a small delivery
receipt to `/results/delivery.json`. It does not produce the legacy nose2 HTML
report. A standalone Job without a callback supports console diagnostics and
exit status; a configured callback delivers the JSON before the container exits.
The private session file is temporary and must never be published as an artifact.

Jobs are retained for log inspection. Remove this run when finished:

```sh
kubectl -n "$TEST_NAMESPACE" delete "$test_job"
```

Deleting the Job removes its pod, not the application data created by tests.
