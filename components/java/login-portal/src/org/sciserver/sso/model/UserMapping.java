package org.sciserver.sso.model;

public class UserMapping {
	
	private String keystoneUserId;
	private String externalUserId;
	private String keystoneTrustId;
	private String externalUsername;
	
	public String getExternalUsername() {
		return externalUsername;
	}
	public void setExternalUsername(String externalUsername) {
		this.externalUsername = externalUsername;
	}
	public String getKeystoneUserId() {
		return keystoneUserId;
	}
	public void setKeystoneUserId(String keystoneUserId) {
		this.keystoneUserId = keystoneUserId;
	}
	public String getExternalUserId() {
		return externalUserId;
	}
	public void setExternalUserId(String externalUserId) {
		this.externalUserId = externalUserId;
	}
	public String getKeystoneTrustId() {
		return keystoneTrustId;
	}
	public void setKeystoneTrustId(String keystoneTrustId) {
		this.keystoneTrustId = keystoneTrustId;
	}
}
