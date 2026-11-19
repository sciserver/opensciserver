package org.sciserver.springapp.sciquery.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.sciserver.racm.jobm.model.DatabaseContextModel;

/**
 * This class represents the information needed for creating a database context.
 */
@Entity
@Table(name = "DBContext")
@Schema(description = "This class represents the information needed for creating a database context.")
public class DbContext {
    public static String PLACEHOLDER = "SET_ME_UP";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID of this database context in the SciQuery database. "
                        + "Automatically generated identity column.")
    private Long id;

    @JsonBackReference("context2domain")
    @ManyToOne
    @JoinColumn(name = "domain_id", nullable = false)
    @Schema(description = "SciQuery Id of the RDB Compute Domain this database context belongs to.")
    private DbDomain dbDomain;

    @Column(name = "racm_id", nullable = false, unique = true)
    @Schema(description = "Id of this database context in RACM.")
    private Long racmId;

    @JsonView(JsonModelViews.Internal.class)
    @Schema(description = "UUID of this database context in RACM.")
    @Column(name = "racm_uuid", nullable = false, unique = true)
    private String racmUuid;

    @NotNull
    @Column(name = "context_name")
    @Schema(description = "Name of this database context, as shown to the users.")
    private String contextName;

    @NotNull
    @Schema(description = "Description of this database context.")
    private String description;

    @NotNull
    @Column(name = "db_name")
    @Schema(description = "Name of this database context, as stored in the database server.")
    private String dbName;

    @JsonView(JsonModelViews.Internal.class)
    @NotNull
    @Column(name = "readonly_user_name")
    @Schema(description = "Name of the database server user that has only read permissions on this database.")
    private String readonlyUserName;

    @JsonView(JsonModelViews.Internal.class)
    @NotNull
    @Column(name = "readonly_user_password")
    @Schema(description = "Database password for the read-only db user.")
    private String readonlyUserPassword;

    @JsonView(JsonModelViews.Internal.class)
    @Column(name = "readwrite_user_name")
    @Schema(description = "Name of the database server user that has both read and write permissions on this database.")
    private String readwriteUserName;

    @JsonView(JsonModelViews.Internal.class)
    @Column(name = "readwrite_user_password")
    @Schema(description = "Database password for the read/write db user.")
    private String readwriteUserPassword;

    @JsonView(JsonModelViews.Internal.class)
    @Column(length = 1024)
    @Schema(description = "JDBC URL pointing to this database context.")
    private String jdbcUrl;

    @JsonView(JsonModelViews.Internal.class)
    @Column(name = "racm_owner", length = 64)
    @Schema(description = "Name of SciServer entity (user) that owns this database context.")
    private String racmOwner;

    @JsonManagedReference
    @OneToMany(mappedBy = "dbContext", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKey(name = "schemaName")
    @Schema(description = "Database schemas.")
    private Map<String, DbSchema> dbSchemas = new HashMap<>();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "server_id", nullable = false)
    @Schema(description = "Database Server that contains this database.")
    private DbServer dbServer;

    // Used for creating new DbContext instances through the REST API
    @Transient
    private Long serverId;

    protected DbContext() { }

    /**
     * Constructor of `DbContext` class.
     *
     * @param dbDomain database domain.
     * @param racmId ID as known in RACM.
     * @param racmUuid UUID as known in RACM.
     * @param contextName name of the database context.
     * @param description description of the database context.
     * @param dbName name of the database.
     * @param readonlyUserName name of the read-only user.
     * @param readonlyUserPassword password of the read-only user.
     */
    public DbContext(DbDomain dbDomain, Long racmId, String racmUuid, String contextName,
                     String description, String dbName, String readonlyUserName, String readonlyUserPassword) {
        this.dbDomain = dbDomain;
        this.racmId = racmId;
        this.racmUuid = racmUuid;
        this.contextName = contextName;
        this.description = description;
        this.dbName = dbName;
        this.readonlyUserName = readonlyUserName;
        this.readonlyUserPassword = readonlyUserPassword;
    }

    /**
     * Constructor of `DbContext` class.
     *
     * @param databaseContextModel database context object.
     * @param dbDomain database domain object.
     * @param dbName name of the database.
     * @param readonlyUserName name of the read-only user.
     * @param readonlyUserPassword password of the read-only user.
     */
    public DbContext(DatabaseContextModel databaseContextModel, DbDomain dbDomain, String dbName,
                     String readonlyUserName, String readonlyUserPassword) {

        updateFromDatabaseContextModel(databaseContextModel);

        this.dbDomain = dbDomain;
        this.dbName = dbName;
        this.readonlyUserName = readonlyUserName;
        this.readonlyUserPassword = readonlyUserPassword;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DbDomain getDbDomain() {
        return dbDomain;
    }

    public void setDbDomain(DbDomain dbDomain) {
        this.dbDomain = dbDomain;
    }

    public Long getRacmId() {
        return racmId;
    }

    public void setRacmId(Long racmId) {
        this.racmId = racmId;
    }

    public String getRacmUuid() {
        return racmUuid;
    }

    public void setRacmUuid(String racmUuid) {
        this.racmUuid = racmUuid;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getReadonlyUserName() {
        return readonlyUserName;
    }

    public void setReadonlyUserName(String readonlyUserName) {
        this.readonlyUserName = readonlyUserName;
    }

    public String getReadonlyUserPassword() {
        return readonlyUserPassword;
    }

    public void setReadonlyUserPassword(String readonlyUserPassword) {
        this.readonlyUserPassword = readonlyUserPassword;
    }

    public String getReadwriteUserName() {
        return readwriteUserName;
    }

    public void setReadwriteUserName(String readwriteUserName) {
        this.readwriteUserName = readwriteUserName;
    }

    public String getReadwriteUserPassword() {
        return readwriteUserPassword;
    }

    public void setReadwriteUserPassword(String readwriteUserPassword) {
        this.readwriteUserPassword = readwriteUserPassword;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getRacmOwner() {
        return racmOwner;
    }

    public void setRacmOwner(String racmOwner) {
        this.racmOwner = racmOwner;
    }

    public Map<String, DbSchema> getDbSchemas() {
        return dbSchemas;
    }

    public void setDbSchemas(Map<String, DbSchema> dbSchemas) {
        this.dbSchemas = dbSchemas;
    }

    public DbServer getDbServer() {
        return dbServer;
    }

    public void setDbServer(DbServer dbServer) {
        this.dbServer = dbServer;
    }

    @JsonProperty
    public String getVendor() {
        // Included for rendering to /info/domain endpoint
        return (dbServer != null) ? dbServer.getVendor() : null;
    }

    @JsonIgnore
    private void setVendor(String vendor) { }

    @JsonProperty
    public String getVendorVersion() {
        // Included for rendering to /info/domain endpoint
        return (dbServer != null) ? dbServer.getVendorVersion() : null;
    }

    @JsonIgnore
    private void setVendorVersion(String vendorVersion) { }

    @JsonIgnore
    public Long getServerId() {
        // Used for creating new DbContext instances through the REST API
        return serverId;
    }

    @JsonProperty
    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    /**
     * Updates the current object's fields with values from a DatabaseContextModel object.
     *
     * @param databaseContextModel Contains values for Updates the current object's fields.
     */
    public void updateFromDatabaseContextModel(DatabaseContextModel databaseContextModel) {
        this.setRacmId(databaseContextModel.getId());
        this.setRacmUuid(databaseContextModel.getRacmUUID());
        this.setContextName(databaseContextModel.getName());
        this.setDescription(databaseContextModel.getDescription());
    }

    // Only used for creating a new database context model for submission to RACM, which is why the id and racmUUID
    // fields of the DatabaseContextModel object are not set from the racmId and racmUuid fields of the DbContext
    // object.
    /**
     * Used for creating a new database context model for submission to RACM.
     *
     * @return An instance of `DatabaseContextModel`.
     */
    public DatabaseContextModel toDatabaseContextModel() {
        DatabaseContextModel dbContextModel = new DatabaseContextModel();

        dbContextModel.setName(this.getContextName());
        dbContextModel.setDescription(this.getDescription());
        // Unclear if this is the correct relationship in all cases, but seems to be reflected in current entries
        dbContextModel.setPublisherDID(this.getContextName());

        return dbContextModel;
    }
}
