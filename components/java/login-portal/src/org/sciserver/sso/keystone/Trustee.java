/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.keystone;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trustee {
    @JsonProperty("username")
    String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
