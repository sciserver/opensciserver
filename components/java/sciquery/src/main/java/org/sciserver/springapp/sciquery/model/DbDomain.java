package org.sciserver.springapp.sciquery.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sciserver.racm.jobm.model.DatabaseContextModel;
import org.sciserver.racm.jobm.model.RDBComputeDomainModel;

/**
 * This class represents the request body containing the information 
 * needed for registering a SciQuery RDB Compute Domain in RACM.
 */
@Entity
@Table(name = "DBDomain")
@Schema(description = "This class represents the request body containing "
                    + "the information needed for registering a SciQuery RDB Compute Domain in RACM.")
public class DbDomain {
    private static final Logger LOG = LogManager.getLogger();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "racm_id", nullable = false, unique = true)
    @Schema(description = "Unique ID in RACM.")
    private Long racmId;

    @JsonView(JsonModelViews.Internal.class)
    @Column(name = "racm_uuid", nullable = false, unique = true)
    @Schema(description = "UUID of this domain in RACM.")
    private String racmUuid;

    @NotNull
    @Schema(description = "Name of this domain, as displayed to the users.")
    private String displayName;

    @Schema(description = "Description of this domain.")
    private String description;

    @JsonView(JsonModelViews.Internal.class)
    @Column(name = "service_token")
    @Schema(description = "Service token.")
    private String serviceToken;

    @JsonManagedReference("context2domain")
    @OneToMany(mappedBy = "dbDomain", cascade = CascadeType.ALL)  //, fetch = FetchType.EAGER)
    @MapKey(name = "contextName")
    @Schema(description = "Map of database contexts registered in this domain. Map key is the database context name.")
    private Map<String, DbContext> dbContexts = new HashMap<>();

    /**
     * Default constructor required by JPA.
     */
    protected DbDomain() { }

    /**
     * Constructor a DbDomain.
     *
     * @param racmId RACM ID
     * @param racmUuid RACM UUID
     * @param displayName Display name
     */
    public DbDomain(Long racmId, String racmUuid, String displayName) {
        this.racmId = racmId;
        this.racmUuid = racmUuid;
        this.displayName = displayName;
    }

    public DbDomain(RDBComputeDomainModel rdbComputeDomainModel, DbServer dbServer) {
        updateFromRDBComputeDomainModel(rdbComputeDomainModel, dbServer);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public Map<String, DbContext> getDbContexts() {
        return dbContexts;
    }

    public void setDbContexts(Map<String, DbContext> dbContexts) {
        this.dbContexts = dbContexts;
    }

    /**
     * Updates this DbDomain from an RDBComputeDomainModel object.
     *
     * @param rdbComputeDomainModel the RDB compute domain model to update from
     * @param dbServer the database server associated with this domain
     */
    public void updateFromRDBComputeDomainModel(RDBComputeDomainModel rdbComputeDomainModel, DbServer dbServer) {
        this.setRacmId(rdbComputeDomainModel.getId());
        this.setRacmUuid(rdbComputeDomainModel.getRacmUUID());
        this.setDisplayName(rdbComputeDomainModel.getName());
        this.setDescription(rdbComputeDomainModel.getDescription());

        DbContext dbContext = null;
        for (DatabaseContextModel databaseContextModel : rdbComputeDomainModel.getDatabaseContexts()) {
            // Look up matching DbContext from this DbDomain
            dbContext = getDbContexts().get(databaseContextModel.getName());

            // If the RACM RDB database context exists in this DbDomain
            if (dbContext != null) {
                LOG.debug("updateFromRDBComputeDomainModel: vendor=" + rdbComputeDomainModel.getVendor());
                dbContext.updateFromDatabaseContextModel(databaseContextModel);

                // Vendor only exists in RACM at the RDB compute domain level
                if (rdbComputeDomainModel.getVendor() != null) {
                    dbContext.getDbServer().setVendor(rdbComputeDomainModel.getVendor());
                }

            // Else the RACM RDB compute domain is new to this DbDomain
            } else {
                // Create new DbContext w/placeholder values for required fields
                dbContext = new DbContext(databaseContextModel, this, DbContext.PLACEHOLDER, DbContext.PLACEHOLDER,
                        DbContext.PLACEHOLDER);
                dbContext.setDbServer(dbServer);

                this.getDbContexts().put(databaseContextModel.getName(), dbContext);
                LOG.info("New database context, " + databaseContextModel.getId() 
                        + ", detected for RDB compute domain " 
                        + rdbComputeDomainModel.getId() + "!  Configuration required!");
            }
        }
    }

    // Only used for creating a new RDB compute domain model for submission to RACM, which is why the id and racmUUID
    // fields of the RDBComputeDomainModel object are not set from the racmId and racmUuid fields of the DbDomain
    // object.
    /**
     * Converts this DbDomain to an RDBComputeDomainModel for submission to RACM.
     *
     * @param apiEndpoint the API endpoint for the compute domain
     * @param vendor the database vendor
     * @return the RDBComputeDomainModel representation of this DbDomain
     */
    public RDBComputeDomainModel toRDBComputeDomainModel(String apiEndpoint, String vendor) {
        RDBComputeDomainModel rdbComputeDomainModel = new RDBComputeDomainModel();

        rdbComputeDomainModel.setName(this.getDisplayName());
        rdbComputeDomainModel.setDescription(this.getDescription());
        // Unclear if this is the correct relationship in all cases, but seems to be reflected in current entries
        rdbComputeDomainModel.setPublisherDID(this.getDisplayName());
        rdbComputeDomainModel.setApiEndpoint(apiEndpoint);
        rdbComputeDomainModel.setVendor(vendor);

        List<DatabaseContextModel> dbContextModels = new ArrayList<DatabaseContextModel>();
        for (DbContext dbContext : this.getDbContexts().values()) {
            dbContextModels.add(dbContext.toDatabaseContextModel());
        }
        rdbComputeDomainModel.setDatabaseContexts(dbContextModels);

        return rdbComputeDomainModel;
    }
}
