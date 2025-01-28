package org.sciserver.racm.jobm.model;

import java.util.HashMap;
import java.util.Map;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class RDBTargetModel extends RACMBaseModel{

	private String location;
	private String type;
	private Short resultNumber;

	// valid target formats
	public static final String TABLE="TABLE";
	public static final String FILE_CSV="FILE_CSV";
	public static final String FILE_TSV="FILE_TSV";
	public static final String FILE_BSV="FILE_BSV";
	public static final String FILE_VOTABLE="FILE_VOTABLE";
	public static final String FILE_FITS="FILE_FITS";
	public static final String FILE_JSON="FILE_JSON";
	private static final Map<String,String> validTypes = new HashMap<>();
	static {
		validTypes.put(TABLE, TABLE);
		validTypes.put(FILE_CSV, FILE_CSV);
		validTypes.put(FILE_TSV, FILE_TSV);
		validTypes.put(FILE_BSV, FILE_BSV);
		validTypes.put(FILE_JSON, FILE_JSON);
		validTypes.put(FILE_FITS, FILE_FITS);
		validTypes.put(FILE_VOTABLE, FILE_VOTABLE);
	}

	public RDBTargetModel() {
		super();
	}

	public RDBTargetModel(Long id) {
		super(id);
	}

	public RDBTargetModel(String id) {
		super(id);
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
	  this.type = validTypes.get(type);
	}

	public Short getResultNumber() {
		return resultNumber;
	}

	public void setResultNumber(Short resultNumber) {
		this.resultNumber = resultNumber;
	}

}
