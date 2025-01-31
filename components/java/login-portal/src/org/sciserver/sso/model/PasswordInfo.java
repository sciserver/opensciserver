/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso.model;

public class PasswordInfo {

    private String password;
    private String confirmPassword;
    private String validationCode;
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getConfirmPassword() {
        return confirmPassword;
    }
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public String getValidationCode() {
        return validationCode;
    }
    public void setValidationCode(String verificationCode) {
        this.validationCode = verificationCode;
    }
}
