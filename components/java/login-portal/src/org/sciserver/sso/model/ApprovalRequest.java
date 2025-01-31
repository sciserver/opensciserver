package org.sciserver.sso.model;

import java.util.Date;

public class ApprovalRequest {
	private Date createdAt;
	private String keystoneUserId;
	private String name;
	private String email;
	private String ipAddress;
	private String extra;
	private ApprovalStatus status;
	
	public ApprovalRequest(String keystoneUserId, String name, String email, String ipAddress, String extra, ApprovalStatus status) {
		this(null, keystoneUserId, name, email, ipAddress, extra, status);
	}
	
	public ApprovalRequest(Date createdAt, String keystoneUserId, String name, String email, String ipAddress, String extra, ApprovalStatus status) {
		this.createdAt = createdAt;
		this.keystoneUserId = keystoneUserId;
		this.name = name;
		this.email = email;
		this.ipAddress = ipAddress;
		this.extra = extra;
		this.status = status;
	}
	
	public String getKeystoneUserId() {
		return keystoneUserId;
	}
	public void setKeystoneUserId(String keystoneUserId) {
		this.keystoneUserId = keystoneUserId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getExtra() {
		return extra;
	}
	public void setExtra(String extra) {
		this.extra = extra;
	}
	public ApprovalStatus getStatus() {
		return status;
	}
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
}
