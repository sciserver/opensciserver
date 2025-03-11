package org.sciserver.sso.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class RuleType {
	public static RuleType ALLOW = new RuleType("ALLOW");
	public static RuleType DENY = new RuleType("DENY");
	public static RuleType NEEDS_APPROVAL = new RuleType("NEEDS_APPROVAL");
	
	private int intType;
	
	public int getIntType() {
		return intType;
	}
	
	public String getType() {
		switch(intType) {
			case 0: return "DENY";
			case 1: return "ALLOW";
			case 2: return "NEEDS_APPROVAL";
			default: return "_";
		}
	}
	
	public void setType(String value) {
		switch (value) {
			case "DENY": this.intType = 0; break;
			case "ALLOW": this.intType = 1; break;
			case "NEEDS_APPROVAL": this.intType = 2; break;
			default: this.intType = -1;
		}
	}
	
	public RuleType(int value) {
		this.intType = value;
	}
	
	@JsonCreator
	public RuleType(String value) {
		setType(value);
	}
	
	@Override
	@JsonValue
	public String toString() {
		return getType();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		return ((RuleType)o).getIntType() == this.getIntType();
	}
}
