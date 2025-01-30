/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.model.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public class SlotsInfo {
    @JsonProperty("node_id")
    private long nodeId;

    @JsonProperty("port_numbers")
    private List<Integer> portNumbers;

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public List<Integer> getPortNumbers() {
        return portNumbers;
    }

    public void setPortNumbers(List<Integer> portNumbers) {
        this.portNumbers = portNumbers;
    }
}
