package org.sciserver.racm.admin.model;

import java.util.List;

public class JPQLQueryModel {

	private String jpql;
	private List<String> rows;
	private String xmlString;
	private String error;
	public JPQLQueryModel(){
		this.error=null;
	}
	public JPQLQueryModel(String jpql){
		this();
		this.jpql = jpql;
	}
	public String getJpql() {
		return jpql;
	}
	public void setJpql(String jpql) {
		this.jpql = jpql;
	}
	public List<String> getRows() {
		return rows;
	}
	public void setRows(List<String> rows) {
		this.rows = rows;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getXmlString() {
		return xmlString;
	}
	public void setXmlString(String xmlString) {
		this.xmlString = xmlString;
	}
	
}
