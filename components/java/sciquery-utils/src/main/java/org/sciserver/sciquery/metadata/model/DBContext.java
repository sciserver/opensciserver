package org.sciserver.sciquery.metadata.model;

public class DBContext {

    private DBDomain domain;
    private long id;
    private String name;
    private String description;
    private String host;
    private int port;
    private String vendor;
    private String db_name;

    private String jdbcURL;
    private String racm_resource_uuid;
    private String readerusername;
    private String readeruserpassword;
    private String writerusername;
    private String writeruserpassword;
    
    public DBContext(DBDomain domain, long id, String name) {
        this.domain = domain;
        this.id = id;
        this.name = name;
        domain.addContext(this);
    }
    
    public String getDomainUUID() {
        return this.domain.getRacm_domain_uuid();
    }
    public String getName() {
        return name;
    }
    public String getJdbcURL() {
        return jdbcURL;
    }
    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }
    public String getReaderusername() {
        return readerusername;
    }
    public void setReaderusername(String readerusername) {
        this.readerusername = readerusername;
    }
    public String getReaderuserpassword() {
        return readeruserpassword;
    }
    public void setReaderuserpassword(String readeruserpassword) {
        this.readeruserpassword = readeruserpassword;
    }
    public String getWriterusername() {
        return writerusername;
    }
    public void setWriterusername(String writerusername) {
        this.writerusername = writerusername;
    }
    public String getWriteruserpassword() {
        return writeruserpassword;
    }
    public void setWriteruserpassword(String writeruserpassword) {
        this.writeruserpassword = writeruserpassword;
    }

    public String getRacm_resource_uuid() {
        return racm_resource_uuid;
    }

    public void setRacm_resource_uuid(String racm_resource_uuid) {
        this.racm_resource_uuid = racm_resource_uuid;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public long getId() {
        return id;
    }
    public String getDb_name() {
        return db_name;
    }

    public void setDb_name(String db_name) {
        this.db_name = db_name;
    }
}
