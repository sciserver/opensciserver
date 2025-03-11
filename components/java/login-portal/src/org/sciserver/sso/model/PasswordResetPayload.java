package org.sciserver.sso.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordResetPayload {
	private final String userId;
	private final Instant creationTime;

	@JsonCreator
	public PasswordResetPayload(
			@JsonProperty("userId") String userId,
			@JsonProperty("creationTime") Instant creationTime) {
		this.userId = userId;
		this.creationTime = creationTime;
	}
	public String getUserId() {
		return userId;
	}
	public Instant getCreationTime() {
		return creationTime;
	}
}
