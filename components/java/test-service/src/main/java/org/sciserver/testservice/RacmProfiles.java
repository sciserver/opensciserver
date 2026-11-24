package org.sciserver.testservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Narrow SQL cleanup: only a broker-proven identity and its automatic public membership. */
@Component
public class RacmProfiles {
    @FunctionalInterface
    interface Connections {
        Connection open() throws SQLException;
    }

    private final Settings settings;
    private final Connections connections;

    /** Open bounded SQL Server connections only when cleanup is explicitly enabled. */
    @Autowired
    public RacmProfiles(Settings settings) {
        this(settings, () -> {
            Properties properties = new Properties();
            properties.setProperty("user", settings.racmJdbcUser);
            properties.setProperty("password", settings.racmJdbcPassword);
            properties.setProperty("loginTimeout", "5");
            properties.setProperty("socketTimeout", "10000");
            return DriverManager.getConnection(settings.racmJdbcUrl, properties);
        });
    }

    RacmProfiles(Settings settings, Connections connections) {
        this.settings = settings;
        this.connections = connections;
    }

    /** Preview performs the same checks as deletion but always rolls back. */
    public Map<String, Object> inspect(String username, String keystoneId) {
        return transact(username, keystoneId, false);
    }

    /** Disabled mode preserves the existing Keystone-only broker behavior. */
    public void delete(String username, String keystoneId) {
        if (settings.racmCleanupEnabled) {
            transact(username, keystoneId, true);
        }
    }

    private PreparedStatement statement(Connection connection, String sql, Object... values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setQueryTimeout(10);
        for (int index = 0; index < values.length; index++) {
            statement.setObject(index + 1, values[index]);
        }
        return statement;
    }

    private String identifier(String name) throws SQLException {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new SQLException("Unsupported schema identifier");
        }
        return "[" + name + "]";
    }

    private long count(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = statement(connection, sql, values);
                ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    /** Enumerate even CASCADE/SET NULL references: they must not silently erase unrelated data. */
    private void references(Connection connection, String table, long id, boolean profile,
            Map<String, Long> blockers) throws SQLException {
        try (ResultSet keys = connection.getMetaData().getExportedKeys(connection.getCatalog(), "dbo", table)) {
            while (keys.next()) {
                String child = keys.getString("FKTABLE_NAME");
                String schema = keys.getString("FKTABLE_SCHEM");
                String column = keys.getString("FKCOLUMN_NAME");
                String parentColumn = keys.getString("PKCOLUMN_NAME");
                if (!"ID".equalsIgnoreCase(parentColumn)) {
                    throw new SQLException("Unsupported identity reference");
                }
                if (profile && "dbo".equalsIgnoreCase(schema)
                        && "t_User".equalsIgnoreCase(child) && "ID".equalsIgnoreCase(column)) {
                    continue;
                }
                String filter = "";
                if (profile && "dbo".equalsIgnoreCase(schema)
                        && "t_Member".equalsIgnoreCase(child) && "scisEntityId".equalsIgnoreCase(column)) {
                    filter = " AND containerId NOT IN (SELECT ID FROM dbo.t_UserGroup WHERE name = 'public')";
                }
                long found = count(connection, "SELECT COUNT(*) FROM " + identifier(schema) + "."
                        + identifier(child) + " WHERE " + identifier(column) + " = ?" + filter, id);
                if (found > 0) {
                    blockers.put(schema + "." + child + "." + column, found);
                }
            }
        }
    }

    private Map<String, Object> cleanup(Connection connection, String username, String keystoneId,
            boolean delete) throws SQLException {
        // With metadata visibility, discover future FK references too; disabled constraints are unsafe.
        if (connection.getMetaData().getDatabaseProductName().contains("Microsoft SQL Server")) {
            if (count(connection, "SELECT HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'VIEW DEFINITION')") != 1
                    || count(connection, "SELECT COUNT(*) FROM sys.foreign_keys WHERE is_disabled = 1"
                    + " OR is_not_trusted = 1") != 0
                    || count(connection, "SELECT COUNT(*) FROM sys.triggers WHERE is_disabled = 0 AND parent_id IN"
                    + " (OBJECT_ID('dbo.t_User'), OBJECT_ID('dbo.t_SciserverEntity'),"
                    + " OBJECT_ID('dbo.t_Member'))") != 0) {
                throw new SQLException("Complete trusted reference metadata required");
            }
        }
        long id;
        Map<String, Long> blockers = new LinkedHashMap<>();
        try (PreparedStatement statement = statement(connection,
                "SELECT ID, username, partyId FROM dbo.t_User WHERE userId = ?", keystoneId);
                ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) {
                // A reused name with a different immutable ID is a conflict, not an absent profile.
                if (count(connection, "SELECT COUNT(*) FROM dbo.t_User WHERE username = ?", username) != 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "RACM identity mismatch");
                }
                return Map.of("state", "absent", "blockers", Map.of());
            }
            id = rows.getLong("ID");
            if (!username.equals(rows.getString("username"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "RACM identity mismatch");
            }
            if (rows.getObject("partyId") != null) {
                blockers.put("dbo.t_User.partyId", 1L);
            }
            if (rows.next()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ambiguous RACM identity");
            }
        }
        if (count(connection, "SELECT COUNT(*) FROM dbo.t_SciserverEntity WHERE ID = ? AND DTYPE = 'User'", id) != 1
                || count(connection, "SELECT COUNT(*) FROM dbo.t_User WHERE username = ?", username) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RACM identity mismatch");
        }
        references(connection, "t_SciserverEntity", id, true, blockers);
        references(connection, "t_User", id, false, blockers);
        List<Long> members = new ArrayList<>();
        try (PreparedStatement statement = statement(connection,
                "SELECT m.ID FROM dbo.t_Member m JOIN dbo.t_UserGroup g ON g.ID = m.containerId"
                + " WHERE m.scisEntityId = ? AND g.name = 'public'", id);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                members.add(rows.getLong(1));
            }
        }
        for (long member : members) {
            references(connection, "t_Member", member, false, blockers);
        }
        if (delete && !blockers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "RACM profile has blocking references; inspect preview");
        }
        if (delete) {
            for (long member : members) {
                execute(connection, "DELETE FROM dbo.t_Member WHERE ID = ?", member);
            }
            execute(connection, "DELETE FROM dbo.t_User WHERE ID = ?", id);
            execute(connection, "DELETE FROM dbo.t_SciserverEntity WHERE ID = ?", id);
        }
        return Map.of("state", blockers.isEmpty() ? (delete ? "deleted" : "deletable") : "blocked",
                "profileId", id, "publicMemberships", members.size(), "blockers", blockers);
    }

    private void execute(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = statement(connection, sql, id)) {
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Identity changed during deletion");
            }
        }
    }

    private Map<String, Object> transact(String username, String keystoneId, boolean delete) {
        if (!settings.racmCleanupEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RACM cleanup is disabled");
        }
        if (username == null || !username.matches("test_user_[a-f0-9]{32}")
                || keystoneId == null || !keystoneId.matches("[A-Za-z0-9-]{1,128}")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Broker identity required for RACM cleanup");
        }
        try (Connection connection = connections.open()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Map<String, Object> result = cleanup(connection, username, keystoneId, delete);
                if (delete) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "RACM cleanup unavailable; retry pending");
        }
    }
}
