package org.sciserver.springapp.sciquery.model;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Represents the information needed for creating the database server table in the SciQuery database.
 */
@Entity
@Table(name = "DBServer")
@Schema(description = "This class represents the information needed for creating the database server table " 
                    + "in the SciQuery database.")
public class DbServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID of this database server in the SciQuery database. "
                        + "Automatically generated identity column.")
    private Long id;

    @NotNull
    @Column(name = "server_name")
    @Schema(description = "Name of this database server.")
    private String serverName;

    @NotNull
    @Schema(description = "Description of this database server.")
    private String description;

    @JsonView(JsonModelViews.Internal.class)
    @NotNull
    @Schema(description = "Host of this database server.")
    private String host;

    @JsonView(JsonModelViews.Internal.class)
    @Schema(description = "Listening port for the DB connection to this server.")
    private Integer port;

    @NotNull
    @Column(length = 16)
    @Schema(description = "Database server vendor. E.g., POSTGRES or SQLSERVER.")
    private String vendor;

    @Column(name = "vendor_version")
    @Schema(description = "Version of this database server's vendor.")
    private String vendorVersion;

    protected DbServer() { }

    /**
     * Constructor for creating a new DbServer. 
     *
     * @param serverName Server name.
     * @param description Description of this database server.
     * @param host Host of this database server.
     * @param vendor Database server vendor.
     */
    public DbServer(String serverName, String description, String host, String vendor) {
        this.serverName = serverName;
        this.description = description;
        this.host = host;
        this.vendor = vendor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getVendorVersion() {
        return vendorVersion;
    }

    public void setVendorVersion(String vendorVersion) {
        this.vendorVersion = vendorVersion;
    }
}
