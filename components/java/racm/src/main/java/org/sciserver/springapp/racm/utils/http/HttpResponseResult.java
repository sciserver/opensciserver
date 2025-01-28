package org.sciserver.springapp.racm.utils.http;

public class HttpResponseResult {
	private final int responseCode;
	private String message;

	public HttpResponseResult(int responseCode, String message) {
		this.responseCode = responseCode;
		this.message = message;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getMessage() {
		return message;
	}
}
