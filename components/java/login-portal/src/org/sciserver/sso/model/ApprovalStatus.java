package org.sciserver.sso.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ApprovalStatus {
	public static ApprovalStatus REQUESTED = new ApprovalStatus("REQUESTED");
	public static ApprovalStatus ACCEPTED = new ApprovalStatus("ACCEPTED");
	public static ApprovalStatus REJECTED = new ApprovalStatus("REJECTED");
	
	private int intValue;
	
	public int getIntValue() {
		return intValue;
	}
	
	public String getStringValue() {
		switch(intValue) {
			case 0: return "REQUESTED";
			case 1: return "ACCEPTED";
			case 2: return "REJECTED";
			default: return "_";
		}
	}
	
	public void setStringValue(String value) {
		switch (value) {
			case "REQUESTED": this.intValue = 0; break;
			case "ACCEPTED": this.intValue = 1; break;
			case "REJECTED": this.intValue = 2; break;
			default: this.intValue = -1;
		}
	}
	
	public ApprovalStatus(int value) {
		this.intValue = value;
	}
	
	@JsonCreator
	public ApprovalStatus(String value) {
		setStringValue(value);
	}
	
	@Override
	@JsonValue
	public String toString() {
		return getStringValue();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		return ((ApprovalStatus)o).getIntValue() == this.getIntValue();
	}
}
