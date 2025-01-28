package org.sciserver.springapp.racm.utils.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONObject;
import org.sciserver.springapp.racm.ugm.domain.UserProfile;

public class LogBuilder {
	private boolean doShowInUserHistory = false;
	private LogMessageType type = LogMessageType.RACM;
	private UserProfile userProfile = null;
	private String sentenceSubject;
	private String sentenceVerb;
	private String sentencePredicate;
	private String action = null;
	private boolean error = false;
	private String errorText = null;
	private Exception errorException = null;

	private Map<String, Object> extraFields = new HashMap<>();

	private enum LogMessageType {
		RACM, JOBM, FILESERVICE;
	}

	// Should only be constructed by LogUtils
	LogBuilder() {}

	public LogBuilder showInUserHistory() {
		doShowInUserHistory = true;
		return this;
	}

	public LogBuilder forJOBM() {
		return forJOBM(true);
	}

	public LogBuilder forJOBM(boolean condition) {
		if (condition)
			type = LogMessageType.JOBM;
		return this;
	}

	public LogBuilder forFileService() {
		type = LogMessageType.FILESERVICE;
		return this;
	}

	public LogBuilder user(UserProfile up) {
		userProfile = up;
		return this;
	}

	public LogBuilder user(Optional<UserProfile> up) {
		if (up.isPresent())
			userProfile = up.get();
		return this;
	}

	public LogBuilder subject(String sentenceSubject) {
		this.sentenceSubject = sentenceSubject;
		return this;
	}

	public LogBuilder verb(String sentenceVerb) {
		this.sentenceVerb = sentenceVerb;
		return this;
	}

	public LogBuilder predicate(String sentencePredicate, Object... formatArgs) {
		this.sentencePredicate = String.format(sentencePredicate, formatArgs);
		return this;
	}

	public LogBuilder action(String actionFormat, Object... formatArgs) {
		this.action = String.format(actionFormat, formatArgs);
		return this;
	}

	public LogBuilder sentence() {
		return this;
	}

	public LogBuilder extraField(String key, Object value) {
		extraFields.put(key, value);
		return this;
	}

	public LogBuilder logError() {
		error = true;
		return this;
	}

	public LogBuilder errorText(String text) {
		this.errorText = text;
		return this;
	}

	public LogBuilder exception(Exception e) {
		this.errorException = e;
		return this;
	}

	public void log() {
		if (error) {
			LogUtils.logError(this.errorText, Optional.ofNullable(userProfile),
					this.errorException, this.type == LogMessageType.JOBM);
			return;
		}

		if (action == null) {
			Objects.requireNonNull(sentenceSubject);
			Objects.requireNonNull(sentenceVerb);
			Objects.requireNonNull(sentencePredicate);
			action = String.join(" ", sentenceSubject, sentenceVerb, sentencePredicate);
		}
		JSONObject content = new JSONObject()
				.put("action", action)
				.put("sentence", new JSONObject()
						.put("subject", sentenceSubject)
						.put("verb", sentenceVerb)
						.put("predicate", sentencePredicate));
		for (Map.Entry<String, Object> field : extraFields.entrySet()) {
			content.put(field.getKey(), field.getValue());
		}

		switch (this.type) {
		case RACM:
			LogUtils.logRACM(content, doShowInUserHistory, Optional.ofNullable(userProfile));
			break;
		case JOBM:
			LogUtils.logJobm(content, doShowInUserHistory, Optional.ofNullable(userProfile));
			break;
		case FILESERVICE:
			LogUtils.logFileService(content, doShowInUserHistory, Optional.ofNullable(userProfile));
			break;
		}
	}
}