package org.sciserver.sciquery.metadata.model;

import java.util.HashMap;

public class DBDomain {

    private long domain_id;
    private String racm_domain_uuid;
    private String displayName;
    private String description;
    private HashMap<String, DBContext> contexts;
    
    public DBDomain(long domain_id, String racm_domain_uuid) {
        this.domain_id = domain_id;
        this.racm_domain_uuid = racm_domain_uuid;
        this.contexts = new HashMap<String, DBContext>();
    }
    public void addContext(DBContext context) {
        this.contexts.put(context.getName(), context);
    }
    public DBContext getContext(String context_name) {
        return this.contexts.get(context_name);
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
    public long getDomain_id() {
        return domain_id;
    }
    public String getRacm_domain_uuid() {
        return racm_domain_uuid;
    }
}
