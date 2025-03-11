package org.sciserver.racm.ugm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceAccountModel {
    private Long id;
    private String name;
    
    public ServiceAccountModel(@JsonProperty("id") Long id, @JsonProperty("name") String name){
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
