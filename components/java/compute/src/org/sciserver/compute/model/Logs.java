/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class Logs {
    @JsonProperty("StdOut")
    private String stdOut;

    @JsonProperty("StdErr")
    private String stdErr;

    public String getStdOut() {
        return stdOut;
    }

    public void setStdOut(String stdOut) {
        this.stdOut = stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    public void setStdErr(String stdErr) {
        this.stdErr = stdErr;
    }
}