package org.sciserver.authentication.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown=true)
public class User {
    private String userId; // from login-portal, NOT to be confused with RACM id
    private String userName;

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    @JsonSetter("user_id")
    public void setUserId(String userId_in) {
        userId = userId_in;
    }

    @JsonSetter("username")
    public void setUserName(String userName_in) {
        userName = userName_in;
    }

}
