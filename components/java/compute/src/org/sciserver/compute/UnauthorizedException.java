/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/

package org.sciserver.compute;


public class UnauthorizedException extends Exception {

    public UnauthorizedException(String string) {
        super(string);
    }

    public UnauthorizedException(String string, Throwable ex) {
        super(string, ex);
    }
}
