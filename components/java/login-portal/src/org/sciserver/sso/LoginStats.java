/*******************************************************************************
 * Copyright (c) Johns Hopkins University. All rights reserved.
 * Licensed under a modified Apache 2.0 license. 
 * See LICENSE.txt in the project root for license information.
 *******************************************************************************/
package org.sciserver.sso;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginStats {
	private AtomicInteger attempts;
	private Instant firstAttemptTime = Instant.now();

	public LoginStats(int initialCount) {
		attempts = new AtomicInteger(initialCount);
	}

	public AtomicInteger getAttempts() {
		return attempts;
	}

	public Instant getFirstAttemptTime() {
		return firstAttemptTime;
	}
}
