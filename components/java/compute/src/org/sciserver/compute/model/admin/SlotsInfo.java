package org.sciserver.compute.model.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
