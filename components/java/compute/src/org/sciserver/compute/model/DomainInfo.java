/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.model;

public class DomainInfo {
	private long id;
	private String name;
	private String description;
	private Iterable<NodeInfo> nodes;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Iterable<NodeInfo> getNodes() {
		return nodes;
	}
	public void setNodes(Iterable<NodeInfo> nodes) {
		this.nodes = nodes;
	}
}
