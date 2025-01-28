package org.sciserver.racm.jobm.model;

import java.util.ArrayList;
import java.util.List;

public class RDBJobModel extends COMPMJobModel {
	private String inputSql;
	private String sql;
	private List<RDBTargetModel> targets = new ArrayList<>();

	private String databaseContextName;
	private String rdbResourceContextUUID;
	private String rdbDomainName;
	private Long rdbDomainId;

	public RDBJobModel() {
		super();
	}

	public RDBJobModel(Long id) {
		super(id);
	}

	public String getInputSql() {
		return inputSql;
	}

	public void setInputSql(String inputSql) {
		this.inputSql = inputSql;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public List<RDBTargetModel> getTargets() {
		return targets;
	}

	public void setTargets(List<RDBTargetModel> targets) {
		if(targets != null)
			this.targets = targets;
		else
			this.targets = new ArrayList<>();
	}

	public String getDatabaseContextName() {
		return databaseContextName;
	}

	public void setDatabaseContextName(String databaseContextName) {
		this.databaseContextName = databaseContextName;
	}



	public String getRdbResourceContextUUID() {
		return rdbResourceContextUUID;
	}

	public void setRdbResourceContextUUID(String rdbResourceContextUUID) {
		this.rdbResourceContextUUID = rdbResourceContextUUID;
	}

	public Long getRdbDomainId() {
		return rdbDomainId;
	}

	public void setRdbDomainId(Long rdbDomainId) {
		this.rdbDomainId = rdbDomainId;
	}

	public String getRdbDomainName() {
		return rdbDomainName;
	}

	public void setRdbDomainName(String rdbDomainName) {
		this.rdbDomainName = rdbDomainName;
	}
}
