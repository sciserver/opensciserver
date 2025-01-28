package org.sciserver.compute.model;

public class NodeInfo {
	private long id;
	private String name;
	private long totalSlots;
	private long usedSlots;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getTotalSlots() {
		return totalSlots;
	}
	public void setTotalSlots(long slotsTotal) {
		this.totalSlots = slotsTotal;
	}
	public long getUsedSlots() {
		return usedSlots;
	}
	public void setUsedSlots(long slotsAvailable) {
		this.usedSlots = slotsAvailable;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

}
