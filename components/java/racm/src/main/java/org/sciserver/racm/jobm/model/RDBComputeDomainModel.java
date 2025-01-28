package org.sciserver.racm.jobm.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sciserver.racm.utils.model.RACMBaseModel;

public class RDBComputeDomainModel extends RACMBaseModel{
	private String vendor;
	private String name;
	private String description;
	private String apiEndpoint;
	public static final String SQLSERVER = "SQLSERVER";
	public static final String ORACLE = "ORACLE";
	public static final String MYSQL = "MYSQL";
	public static final String POSTGRES = "POSTGRES";
	public static final String DB2 = "DB2";
	public static final String SYBASE = "SYBASE";

	private static final Map<String,String> validVendors = new HashMap<>();
	static {
		validVendors.put(MYSQL, MYSQL);
		validVendors.put(ORACLE, ORACLE);
		validVendors.put(SQLSERVER, SQLSERVER);
		validVendors.put(POSTGRES, POSTGRES);
		validVendors.put(DB2, DB2);
		validVendors.put(SYBASE, SYBASE);

	}

	private List<DatabaseContextModel> databaseContexts;
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = validVendors.get(vendor);
	}
	public List<DatabaseContextModel> getDatabaseContexts() {
		return databaseContexts;
	}
	public void setDatabaseContexts(List<DatabaseContextModel> databaseContexts) {
		this.databaseContexts = databaseContexts;
	}
	public RDBComputeDomainModel(){}
	public RDBComputeDomainModel(long id){
		super(id);
	}
	public RDBComputeDomainModel(String id){
		super(id);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getApiEndpoint() {
		return apiEndpoint;
	}
	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

}
