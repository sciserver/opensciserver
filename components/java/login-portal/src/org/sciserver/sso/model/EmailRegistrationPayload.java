package org.sciserver.sso.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailRegistrationPayload {
	private final UserCredentials attemptedUserCredentials;
	private final Instant creationTime;

	@JsonCreator
	public EmailRegistrationPayload(
			@JsonProperty("attemptedUserCredentials") UserCredentials attemptedUserCredentials,
			@JsonProperty("creationTime") Instant creationTime) {
		this.attemptedUserCredentials = attemptedUserCredentials;
		this.creationTime = creationTime;
	}
	public UserCredentials getAttemptedUserCredentials() {
		return attemptedUserCredentials;
	}
	public Instant getCreationTime() {
		return creationTime;
	}
}
