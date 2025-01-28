package org.sciserver.racm.utils.model;

public class RACMMVCBaseModel extends RACMBaseModel {

	public RACMMVCBaseModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RACMMVCBaseModel(Long id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public RACMMVCBaseModel(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	private String errorMessage;
	private boolean isValid;

	public String getErrorMessage(){
		return this.errorMessage;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setException(Exception exception) {
		this.errorMessage = (exception != null?exception.getMessage():null);
		this.isValid = exception!= null;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

}
