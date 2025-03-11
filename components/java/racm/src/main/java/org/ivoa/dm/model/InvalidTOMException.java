package org.ivoa.dm.model;

import java.util.Iterator;
import java.util.List;

import org.ivoa.dm.VOURPException;

public class InvalidTOMException extends VOURPException {
  private static final long serialVersionUID = 2154083950195812169L;

  private List<MetadataObject> invalidObjects;
	public InvalidTOMException(List<MetadataObject> _invalidObjects){
		super(VOURPException.ILLEGAL_STATE, "");
		this.invalidObjects = _invalidObjects;
	}
	public Iterator<MetadataObject> getInvalidObjects() {
		return invalidObjects.iterator();
	}
	public String getMessage(){
		StringBuffer sb = new StringBuffer("TOM validation errors\n+++++++++++++++++++++++++++++++++\n");
		for(MetadataObject o: invalidObjects){
			sb.append(o.toString()).append(":\n").append(o.validationErrors());
		}
		return sb.toString();
	}
}
