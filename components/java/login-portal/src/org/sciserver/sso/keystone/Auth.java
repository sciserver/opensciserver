/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.keystone;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Auth {
    @JsonProperty("username")
    String userName;
    
    String password;
    
    @JsonProperty("trust_id")
    String trustId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTrustId() {
        return trustId;
    }

    public void setTrustId(String trustId) {
        this.trustId = trustId;
    }
    
}
