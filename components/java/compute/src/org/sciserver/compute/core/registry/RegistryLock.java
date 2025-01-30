/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute.core.registry;

public class RegistryLock implements AutoCloseable {
    private Registry registry;

    @Override
    public void close() throws Exception {
        registry.releaseLock();
    }

    public RegistryLock(Registry registry) throws Exception {
        this.registry = registry;
        this.registry.acquireLock();
    }
}
