package org.sciserver.racm.ugm.model;

import static java.util.stream.Collectors.collectingAndThen;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class PersonalUserInfo {
	private Long id;
	private String username;
	private String contactEmail;
	private String visibility;
	private String preferences;
	private String affiliation;
	private String fullname;

	@JsonCreator
	public PersonalUserInfo(@JsonProperty("id") Long id, @JsonProperty("username") String username,
			@JsonProperty("contactEmail") String contactEmail, @JsonProperty("preferences") String preferences,
			@JsonProperty("fullname") String fullname, @JsonProperty("affiliation") String affiliation,
			@JsonProperty("visibility") String visibility) {
		this.id = id;
		this.username = username;
		this.contactEmail = contactEmail;
		this.visibility = visibility;
		this.preferences = preferences;
		this.affiliation = affiliation;
		this.fullname = fullname;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String partyName) {
		this.fullname = partyName;
	}

	public String getVisibility() {
		return visibility;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public String getPreferences() {
		return preferences;
	}
}
