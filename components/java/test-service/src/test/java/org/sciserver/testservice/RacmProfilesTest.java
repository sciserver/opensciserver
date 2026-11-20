package org.sciserver.testservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/** Relational tests include cascades that must be blocked before deleting a profile. */
class RacmProfilesTest {
    private static final String USER = "test_user_0123456789abcdef0123456789abcdef";
    private String url;
    private Settings settings;
    private RacmProfiles profiles;

    @BeforeEach
    void prepare() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
        settings = new Settings(new MockEnvironment().withProperty("test.namespace", "test")
                .withProperty("test.ledger-image", "test:example").withProperty("test.base-url", "http://example.test")
                .withProperty("test.racm-url", "http://racm")
                .withProperty("test.broker-signing-key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
                .withProperty("test.racm-cleanup-enabled", "true")
                .withProperty("test.racm-jdbc-url", "jdbc:sqlserver://unused")
                .withProperty("test.racm-jdbc-user", "cleanup").withProperty("test.racm-jdbc-password", "secret"));
        profiles = new RacmProfiles(settings, () -> DriverManager.getConnection(url));
        sql("CREATE SCHEMA dbo");
        sql("CREATE TABLE dbo.t_SciserverEntity (ID BIGINT PRIMARY KEY, DTYPE VARCHAR(32))");
        sql("CREATE TABLE dbo.t_User (ID BIGINT PRIMARY KEY REFERENCES dbo.t_SciserverEntity(ID),"
                + " username VARCHAR(128), userId VARCHAR(128), partyId BIGINT)");
        sql("CREATE TABLE dbo.t_UserGroup (ID BIGINT PRIMARY KEY REFERENCES dbo.t_SciserverEntity(ID),"
                + " name VARCHAR(128), ownerId BIGINT REFERENCES dbo.t_SciserverEntity(ID) ON DELETE CASCADE)");
        sql("CREATE TABLE dbo.t_Member (ID BIGINT PRIMARY KEY,"
                + " containerId BIGINT REFERENCES dbo.t_SciserverEntity(ID),"
                + " scisEntityId BIGINT REFERENCES dbo.t_SciserverEntity(ID) ON DELETE CASCADE)");
        for (String table : new String[] {"t_AccessControl", "t_Job", "t_UserVolume", "t_ActionExecution"}) {
            sql("CREATE TABLE dbo." + table + " (ID BIGINT PRIMARY KEY,"
                    + " targetId BIGINT REFERENCES dbo.t_SciserverEntity(ID) ON DELETE CASCADE)");
        }
        sql("CREATE TABLE dbo.t_ContextClass (ID BIGINT PRIMARY KEY,"
                + " creatorId BIGINT REFERENCES dbo.t_SciserverEntity(ID) ON DELETE SET NULL)");
        sql("INSERT INTO dbo.t_SciserverEntity VALUES (1, 'User'), (2, 'UserGroup'), (3, 'UserGroup')");
        sql("INSERT INTO dbo.t_User VALUES (1, '" + USER + "', 'keystone-id', NULL)");
        sql("INSERT INTO dbo.t_UserGroup VALUES (2, 'public', NULL), (3, 'admin', NULL)");
        sql("INSERT INTO dbo.t_Member VALUES (10, 2, 1)");
    }

    private void sql(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                java.sql.Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(url);
                java.sql.Statement statement = connection.createStatement();
                java.sql.ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM dbo." + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    void previewDoesNotMutateAndDeleteIsIdempotentWithoutDeletingPublicGroup() throws Exception {
        assertEquals("deletable", profiles.inspect(USER, "keystone-id").get("state"));
        assertEquals(1, count("t_User"));
        profiles.delete(USER, "keystone-id");
        profiles.delete(USER, "keystone-id");
        assertEquals("absent", profiles.inspect(USER, "keystone-id").get("state"));
        assertEquals(0, count("t_User"));
        assertEquals(0, count("t_Member"));
        assertEquals(2, count("t_UserGroup"));
        assertEquals(2, count("t_SciserverEntity"));
    }

    @Test
    void knownAndFutureCascadesAndSetNullReferencesBlockWithoutMutation() throws Exception {
        for (String table : new String[] {"t_AccessControl", "t_Job", "t_UserVolume",
            "t_ActionExecution", "t_ContextClass"}) {
            sql("INSERT INTO dbo." + table + " VALUES (20, 1)");
            assertEquals("blocked", profiles.inspect(USER, "keystone-id").get("state"));
            assertEquals(409, assertThrows(ResponseStatusException.class,
                    () -> profiles.delete(USER, "keystone-id")).getStatus().value());
            assertEquals(1, count(table));
            assertEquals(1, count("t_User"));
            sql("DELETE FROM dbo." + table);
        }
        sql("CREATE TABLE dbo.future_dependency (ID BIGINT REFERENCES dbo.t_User(ID) ON DELETE CASCADE)");
        sql("INSERT INTO dbo.future_dependency VALUES (1)");
        assertEquals("blocked", profiles.inspect(USER, "keystone-id").get("state"));
        assertThrows(ResponseStatusException.class, () -> profiles.delete(USER, "keystone-id"));
        assertEquals(1, count("future_dependency"));
    }

    @Test
    void ownedGroupsAdminMembershipAndPartyLinksBlock() throws Exception {
        sql("UPDATE dbo.t_UserGroup SET ownerId = 1 WHERE ID = 3");
        assertThrows(ResponseStatusException.class, () -> profiles.delete(USER, "keystone-id"));
        sql("UPDATE dbo.t_UserGroup SET ownerId = NULL WHERE ID = 3");
        sql("INSERT INTO dbo.t_Member VALUES (11, 3, 1)");
        assertThrows(ResponseStatusException.class, () -> profiles.delete(USER, "keystone-id"));
        sql("DELETE FROM dbo.t_Member WHERE ID = 11");
        sql("UPDATE dbo.t_User SET partyId = 7");
        Map<?, ?> blockers = (Map<?, ?>) profiles.inspect(USER, "keystone-id").get("blockers");
        assertEquals(1L, blockers.get("dbo.t_User.partyId"));
    }

    @Test
    void referencesToPublicMembershipAlsoBlock() throws Exception {
        sql("CREATE TABLE dbo.member_audit (memberId BIGINT REFERENCES dbo.t_Member(ID) ON DELETE CASCADE)");
        sql("INSERT INTO dbo.member_audit VALUES (10)");
        assertEquals("blocked", profiles.inspect(USER, "keystone-id").get("state"));
        assertThrows(ResponseStatusException.class, () -> profiles.delete(USER, "keystone-id"));
        assertEquals(1, count("member_audit"));
        assertEquals(1, count("t_Member"));
    }

    @Test
    void identityMismatchAndUnmanagedNamesCannotDelete() {
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> profiles.delete(USER, "different-id")).getStatus().value());
        assertThrows(ResponseStatusException.class, () -> profiles.delete("__racm__", "keystone-id"));
        assertThrows(ResponseStatusException.class,
                () -> profiles.delete("test_user_ffffffffffffffffffffffffffffffff", "keystone-id"));
    }

    @Test
    void failureAfterChildDeletesRollsBackWholeTransaction() throws Exception {
        RacmProfiles failing = new RacmProfiles(settings, () -> {
            Connection connection = spy(DriverManager.getConnection(url));
            doThrow(new SQLException("simulated failure"))
                    .when(connection).prepareStatement("DELETE FROM dbo.t_SciserverEntity WHERE ID = ?");
            return connection;
        });
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> failing.delete(USER, "keystone-id")).getStatus().value());
        assertEquals(1, count("t_User"));
        assertEquals(1, count("t_Member"));
    }
}
