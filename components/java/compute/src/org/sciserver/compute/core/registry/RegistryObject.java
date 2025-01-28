/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.compute.core.registry;

public abstract class RegistryObject {

	protected Registry registry;

	private long id;

	public Registry getRegistry() {
		return registry;
	}

	public RegistryObject(Registry registry) {
		this.registry = registry;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
