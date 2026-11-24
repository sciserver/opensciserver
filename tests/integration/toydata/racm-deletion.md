# Guarded RACM profile deletion

Implemented in test-service, without changing RACM, login-portal or the shared
client. It is opt-in direct SQL access, separate from ordinary user-facing APIs.
This first operation removes only a broker-proven profile with no unexpected
references. It does not implement general cascading account erasure or an
Ansible provider.

## Dependency investigation

The RACM model uses joined inheritance: a person has a `dbo.t_User` row and a
`dbo.t_SciserverEntity` row with the same numeric ID. The `t_User.userId` value is
the separate Keystone identity. Memberships refer to the base entity; the public
membership is created lazily on first authenticated RACM access.

Several schema references make a plain base-entity deletion unsafe:

| Reference | Schema behavior / cleanup policy |
| --- | --- |
| `t_User.ID` | Inheritance link; explicitly delete the person row before its base entity. |
| `t_Member.scisEntityId` | Cascades; explicitly remove only membership in the built-in public group. Other memberships, including admin, block. |
| `t_UserGroup.ownerId` | Cascades into owned group records; block rather than implicitly deleting groups and their relationships. |
| `t_UserVolume.ownerId` | References the owner; block. Metadata deletion would not remove filesystem content anyway. |
| `t_AccessControl.scisEntityId` | Cascades into privileges/role assignments; block pending explicit removal of those grants. |
| `t_Job.submitterId` | Cascades into job records; block, including terminal history. |
| `t_ActionExecution.userId` | Cascades into execution records; block. |
| `t_ContextClass.creatorId` | Sets creator to NULL; block to avoid modifying unrelated resource definitions. |
| `t_AssociatedSciEntity.sciEntityId` | Cascades; block, including service/resource ownership associations. |
| `t_ServiceAccount.ID`, `t_UserGroup.ID` | Other entity subtypes; never treat as a person to delete. |
| `t_User.partyId` | Outgoing optional Party reference; block rather than guess whether Party data is shared or orphaned. |

The implementation discovers exported foreign keys on the base entity, person
and removable membership rows at runtime, counting matching references even
when the database would silently cascade or set NULL. New FK relationships
therefore block rather than silently cascading. Names/identifiers from database
metadata are validated and quoted; API values are bound parameters. This detects
SQL-enforced references, not arbitrary logical references in other databases,
files, caches, or external systems.

Sources: [base schema](../../../components/java/racm/src/main/resources/db/migration/V1__Base_version.sql),
[associated entities](../../../components/java/racm/src/main/resources/db/migration/V4__Add_Associated_Resources_and_SciServerEntities.sql),
[service accounts](../../../components/java/racm/src/main/resources/db/migration/V13__Add_Service_Accounts.sql),
and [implementation](../../../components/java/test-service/src/main/java/org/sciserver/testservice/RacmProfiles.java).

## Ownership and API

No endpoint accepts an arbitrary username or user ID for deletion. The existing
broker first proves control of an account and persists its exact Keystone IDs.
Cleanup targets that record and requires both the recorded username and immutable
Keystone ID to match the RACM person. Only the broker's generated
`test_user_<32 lowercase hex digits>` identities are accepted. Declarative datasets
with other names/ownership records are future work; a matching prefix alone does
not authorize deletion through the service.

An administrator can preview a verified account without creating any RACM state:

```text
GET /api/broker/runs/{runId}/accounts/{scope}/racm-deletion
X-Auth-Token: <current SciServer admin session>
```

Scopes are the existing `session`, `registration`, and `logout` reservations.
The identity must already have been recorded by the broker. Responses identify
`absent`, `deletable`, or `blocked`, the numeric profile ID if present, public
membership count and blocking table/column counts. Missing credentials are 401;
non-admins are 403; unverified records/identity conflicts are 409; disabled cleanup
or unavailable/unsupported database metadata is 503. Preview always rolls back;
it is advisory, not a durable authorization to delete later.

Releasing the existing broker run (`DELETE /account-broker/{runId}` with its
Bearer capability) invokes the same checks inside a new serializable transaction.
It removes public membership rows, then the person and base-entity rows, committing
only if every operation succeeds. Missing profiles are retryable success, except
when the name now belongs to a different Keystone identity. Any blocker or SQL
failure prevents this deletion and keeps account cleanup pending.

With cleanup enabled, the provider checks/cleans RACM before deleting the Keystone
user/project so blockers preserve owner access. It checks RACM again after provider
deletion to catch a profile recreated by an already in-flight request. This is
**not a cross-service transaction**. Clients must stop using the fixture before
release; accepted/in-flight requests and authentication caches mean this is not
a substitute for maintenance mode. The broker audits closed runs and retries
partial cleanup using its retained identity record. Other databases and filesystem
contents are untouched.

## Configuration

Existing broker credentials/configuration are still required. Enable separately:

```yaml
testService:
  racmCleanup:
    enabled: true
```

The chart defaults the JDBC URL/user to the configured RACM database and references
the existing `<fullname>-racm-db` Secret's `password` key. For the internal database
this is normally `SA`, with broad administrator privileges. A separate account can
be supplied using `racmCleanup.user`, `passwordSecret`, `passwordKey`, and optionally
`jdbcUrl`. No password is rendered into the JDBC URL. Test-service does not run
RACM/Flyway migrations or create tables.

The database principal needs database `VIEW DEFINITION`, SELECT on the relevant
reference tables, and DELETE on `dbo.t_Member`, `dbo.t_User`, and
`dbo.t_SciserverEntity`. SQL Server ownership-chaining/grant details must be
validated for the chosen account. Cleanup refuses disabled or untrusted foreign
keys and enabled triggers on the tables it deletes, rather than assuming
metadata describes all side effects. Do not run schema migrations concurrently. Connection
and statement timeouts are bounded; errors returned to clients omit database
connection details.

Outside Helm use `TEST_RACM_CLEANUP_ENABLED=true`, `TEST_RACM_JDBC_URL`,
`TEST_RACM_JDBC_USER`, and `TEST_RACM_JDBC_PASSWORD` from protected configuration.
Disabled mode preserves the existing Keystone-only cleanup behavior; preview
reports that RACM cleanup is disabled.

## Verification and limits

Relational tests use H2 in SQL Server compatibility mode with representative
foreign keys and cascades. They cover preview, repeat deletion, identity mismatch,
protected names, owned groups, memberships, grants/jobs/volumes, unknown FK
references, SET NULL behavior, and rollback after partially executed deletes.
Provider tests verify that RACM blockers prevent Keystone deletion; HTTP wiring
tests verify the preview's authentication boundary.

These tests do not emulate SQL Server locking or validate its metadata permissions
and JDBC driver behavior. A live SQL Server rehearsal remains required before
enabling this against an important deployment. Checkpoint/restore tooling is not
yet implemented.
