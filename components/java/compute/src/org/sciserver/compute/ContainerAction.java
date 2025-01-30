/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;

import org.sciserver.compute.core.registry.ExecutableContainer;


@FunctionalInterface
public interface ContainerAction {
    public void execute(ExecutableContainer container) throws Exception;
}
