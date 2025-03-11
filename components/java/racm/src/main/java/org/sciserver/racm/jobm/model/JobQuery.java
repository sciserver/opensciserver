package org.sciserver.racm.jobm.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JobQuery {
	@NonNull
	private final JobType type;
	@NonNull
	private final List<Long> jobIds;
	@NonNull
	private final List<Integer> jobStatuses;
	private final int limit;
	@Nullable
	private final Instant submitTimeStart;
	@Nullable
	private final Instant submitTimeEnd;
	@Nullable
	private final ORDER_BY orderBy;
	private final int pageNumber;
	private final boolean includeDetails;

	@JsonCreator
	private JobQuery(
			@JsonProperty("type") JobType type,
			@JsonProperty(value="jobIds", required=false) List<Long> jobIds,
			@JsonProperty(value="jobStatuses", required=false) List<Integer> jobStatuses,
			@JsonProperty(value="limit", required=false, defaultValue="" + Integer.MAX_VALUE) int limit,
			@JsonProperty(value="submitTimeStart", required=false) Instant submitTimeStart,
			@JsonProperty(value="submitTimeEnd", required=false) Instant submitTimeEnd,
			@JsonProperty(value="orderBy", required=false) ORDER_BY orderBy,
			@JsonProperty(value="pageNumber", required=false, defaultValue="0") int pageNumber,
			@JsonProperty(value="includeDetails", required=false, defaultValue="false") boolean includeDetails) {
		this.type = type;
		this.jobIds = jobIds != null ? jobIds : Collections.emptyList();
		this.jobStatuses = jobStatuses != null ? jobStatuses : Collections.emptyList();
		this.limit = limit;
		this.submitTimeStart = submitTimeStart;
		this.submitTimeEnd = submitTimeEnd;
		this.orderBy = orderBy;
		this.pageNumber = pageNumber;
		this.includeDetails = includeDetails;
	}

	@NonNull
	public JobType getType() {
		return type;
	}

	@NonNull
	public List<Long> getJobIds() {
		return jobIds;
	}

	@NonNull
	public List<Integer> getJobStatuses() {
		return jobStatuses;
	}

	public int getLimit() {
		return limit;
	}

	public Optional<Instant> getSubmitTimeStart() {
		return Optional.ofNullable(submitTimeStart);
	}

	public Optional<Instant> getSubmitTimeEnd() {
		return Optional.ofNullable(submitTimeEnd);
	}

	public Optional<ORDER_BY> getOrderBy() {
		return Optional.ofNullable(orderBy);
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public boolean includeDetails() {
		return includeDetails;
	}

	public enum ORDER_BY {
		ASC_JOB_ID,
		DESC_JOB_ID,
		ASC_SUBMIT_TIME,
		DESC_SUBMIT_TIME
	}

	public enum JobType {
		DOCKER,
		RDB
	}
}
