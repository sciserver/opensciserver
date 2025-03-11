package org.sciserver.sso.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Rule {
	private RuleType type;
	private String regEx;
	
	@JsonCreator	
	public Rule(@JsonProperty("type") RuleType type, @JsonProperty("regEx") String regEx) {
		this.type = type;
		this.regEx = regEx;
	}
	
	public RuleType getType() {
		return type;
	}
	public void setType(RuleType type) {
		this.type = type;
	}
	public String getRegEx() {
		return regEx;
	}
	public void setRegEx(String regEx) {
		this.regEx = regEx;
	}
}
