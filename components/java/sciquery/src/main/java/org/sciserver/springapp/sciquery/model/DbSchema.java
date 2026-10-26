package org.sciserver.springapp.sciquery.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * This class represents the information needed for creating the schema table in the SciQuery database.
 */
@Entity
@Schema(description = "This class represents the information needed for creating the schema table "
                    + "in the SciQuery database.")
@Table(name = "DBSchema")
public class DbSchema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "database_id", nullable = false)
    private DbContext dbContext;

    @Column(name = "racm_id", nullable = false, unique = true)
    private Long racmId;

    @Column(name = "racm_uuid", nullable = false, unique = true)
    private String racmUuid;

    @NotNull
    @Column(name = "schema_name")
    private String schemaName;

    private String description;

    protected DbSchema() { }

    /**
     * Constructor for creating a new DbSchema.
     *
     * @param dbContext Database context.
     * @param racmId RACM id.
     * @param racmUuid RACM UUID.
     * @param schemaName Name of schema.
     */
    public DbSchema(DbContext dbContext, Long racmId, String racmUuid, String schemaName) {
        this.dbContext = dbContext;
        this.racmId = racmId;
        this.racmUuid = racmUuid;
        this.schemaName = schemaName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DbContext getDbContext() {
        return dbContext;
    }

    public void setDbContext(DbContext dbContext) {
        this.dbContext = dbContext;
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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
