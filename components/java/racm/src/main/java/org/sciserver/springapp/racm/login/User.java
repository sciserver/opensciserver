package org.sciserver.springapp.racm.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class User {
	@JsonProperty("user_id")
	String userId;
	
	@JsonProperty("username")
	String userName;
	
	String email;
	
	String password;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public static User fromJson(JsonNode json){
		User u = new User();
		u.setUserId(json.get("user_id").textValue());
		u.setUserName(json.get("username").textValue());
		if(json.get("password") != null)
			u.setPassword(json.get("password").textValue());
		u.setEmail(json.get("email").textValue());
		return u;
	}
}
