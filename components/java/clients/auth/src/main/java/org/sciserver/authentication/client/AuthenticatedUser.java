package org.sciserver.authentication.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown=true)
public class AuthenticatedUser extends User {
    private String email;
    private String token;

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public void setEmail(String email_in) {
        email = email_in;
    }

    public void setToken(String token_in) {
        token = token_in;
    }

}
