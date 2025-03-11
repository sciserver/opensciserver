package org.sciserver.racm.jobm.model;

import java.util.HashMap;
import java.util.Map;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class JobMessageModel extends RACMBaseModel{

	private String jobId;
	public static final String CANCEL = "CANCEL";
	public static final String STATUS = "STATUS";
	public static final String RESPONSE = "RESPONSE";
	public static final String ERROR = "ERROR";
	public static final String TIMEOUT = "TIMEOUT";
	public static final String OUTPUT = "OUTPUT";
	private static final Map<String,String> validLabels;
	static {
		validLabels = new HashMap<>();
		validLabels.put(CANCEL,CANCEL);
		validLabels.put(STATUS,STATUS);
		validLabels.put(RESPONSE,RESPONSE);
		validLabels.put(ERROR,ERROR);
		validLabels.put(TIMEOUT, TIMEOUT);
		validLabels.put(OUTPUT, OUTPUT);
	}
	private String content;
	private String label;
	public JobMessageModel(){}
	public JobMessageModel(long id){
		super(id);
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getLabel() {
		return label;
	}
	/** if label is not a valid one, will be set to null. NO exception is thrown! */
	public void setLabel(String label) {
		this.label = validLabels.get(label);
	}
}
