# Test broker protocol (version 1)

The broker is required integration-test infrastructure implemented inside
**test-service**. Login-portal, Keystone, RACM, the dashboard and chart remain
unchanged. Both CLI and web execution use the same runner and protocol.

## Run reservation

An administrator POSTs to `<test-service root>/api/broker/runs` with the current
SciServer session in `X-Auth-Token`. A 201 response contains `contractVersion: "1"`,
`runId` (UUID), `brokerPath`, `token` (a narrow run capability), and `baseUrl`.
Only this front door needs an admin session. The web backend reserves a run before
launching its execution Job; the CLI runner reserves one with `--broker-url` and
`--admin-token-file`. Neither broker access nor CLI execution requires an execution
Job or Kubernetes credentials on the caller's machine.

The runner receives `TEST_ACCOUNT_BROKER_URL=<root>/account-broker/<runId>` and
`TEST_ACCOUNT_BROKER_TOKEN=<capability>`. All broker requests below use
`Authorization: Bearer <capability>` and must not follow redirects. This credential
cannot authorize admin APIs or report submission. GET the broker URL to validate
context: the response contains the run ID, base URL and expiry. Any explicitly
configured `TEST_RUN_ID` or `SCISERVER_BASE_URL` must agree with this context.
There is no unmanaged-account fallback. `--list` and `--plan` remain offline.

## Fixtures

The catalog and plan declare `fixtures` separately from test dependencies:

- `session`: authenticated account shared by ordinary API tests in one run.
- `logout`: separate account/session used by logout and revoked-token tests.
- The registration test explicitly reserves a `registration` account instead
  of provisioning the other tests' fixture.

POST `<broker URL>/accounts/<scope>` reserves a unique identity before returning
`contractVersion: "1"`, `runId`, `username`, `email`, `password`, `state`, and
`expiresAt` (Unix epoch milliseconds). The runner uses those credentials with
the **unchanged normal registration API**. It then POSTs to
`<broker URL>/accounts/<scope>/registered`. The broker independently proves
control using the generated password and stores exact Keystone identifiers;
the runner cannot supply arbitrary accounts for deletion.

Fixture setup is not a test case or a hard test prerequisite. Selecting `racm.user`
does not select `identity.register` or `identity.token`. The runner caches fixture
sessions by scope. It never reauthenticates the logout fixture between logout and
the revoked-token test. Fixture setup errors affect their consumers; independent
tests/fixtures continue.

Finally DELETE the broker URL to close the run and release **all** its account
fixtures. A 200 response with `state: "deleted"` confirms account/project cleanup.
Release is idempotent and remains authorized after expiry and user logout.
Setup failures and assertion errors still trigger release. Cleanup failure makes
the runner outcome an error and leaves a durable obligation for retry.

## Durable ownership

Each broker run has a separate suspended Kubernetes Job named `test-broker-<id>`.
It never runs a pod. Execution Jobs are separate records; removing an execution
Job does not remove broker ownership. Existing get/list/create/patch Job permissions
suffice; no new database, PVC, ConfigMap permissions or service are needed.

Ledger annotations hold each fixture's generated identity, nonce, exact verified
IDs and lifecycle state. Passwords are reconstructed using the stable signing key;
passwords, user tokens and cleanup administrator credentials are not stored in the
ledger. Updates use resourceVersion to detect conflicts. Keep one service replica,
as already required by admission control and report retention.

Before issuing credentials, the broker refuses pre-existing names. Deletion needs
independent password proof and matching name/domain/default-project IDs, not a
naming prefix. Once identifiers are saved, partial deletions can be retried after
the user is gone. The worker retries closed or expired runs every minute, with a
bounded batch and older attempts first. Completed records are retained and audited
periodically to detect late registration completion.

**Retain broker Jobs and the stable signing key.** Deleting a broker Job loses its
ledger. Rotating the key before unconfirmed registrations resolve loses their
reconstructable passwords. Disabling the service/broker stops retries; restarting
with the same storage and key resumes them. Execution Job retention is independent.

## Existing cleanup limits

Registration can create a project before its user. A failure between those steps
leaves no user/password with which to prove project ownership. The broker retains
that obligation for manual review and refuses unsafe deletion. Fully transactional
tracking requires registration-service support, outside this change's scope.

Account cleanup removes Keystone accounts/projects and their grants/sessions. It
does not erase RACM records/memberships, provisioned fileservice volumes, other
services' resources, or audit history. Existing per-test hooks handle normal file,
group and container cleanup, but those resources still need brokers for crash
recovery. Do not use this account broker for CasJobs/external-identity provisioning;
those additional identities are outside its cleanup scope.

## Dedicated lifecycle check

The additive `lifecycle` scope is reserved for `identity.lifecycle`; it never
invalidates ordinary session or logout fixtures. A runner may release just that
fixture using `DELETE /account-broker/{id}/accounts/lifecycle`, and inspect it via
`GET /account-broker/{id}/accounts/lifecycle/state`. Both require the run capability
and resolve exact provider IDs exclusively from the recorded ownership proof.
Inspection returns Keystone user/project absence booleans and RACM SQL preview
state, without authenticating as the fixture or recreating a profile. The run
context includes `racmCleanupEnabled`, so the check fails before registration
when cleanup is disabled. Normal end-of-run release still retries all scopes.
