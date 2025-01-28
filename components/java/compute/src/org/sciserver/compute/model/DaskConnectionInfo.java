package org.sciserver.compute.model;

public class DaskConnectionInfo {
	private String schedulerUrl;
	private String ca;
	private String clientCert;
	private String clientKey;
	
	public String getCa() {
		return ca;
	}
	public void setCa(String ca) {
		this.ca = ca;
	}
	public String getClientCert() {
		return clientCert;
	}
	public void setClientCert(String clientCert) {
		this.clientCert = clientCert;
	}
	public String getClientKey() {
		return clientKey;
	}
	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}
	public String getSchedulerUrl() {
		return schedulerUrl;
	}
	public void setSchedulerUrl(String schedulerUrl) {
		this.schedulerUrl = schedulerUrl;
	}
}
