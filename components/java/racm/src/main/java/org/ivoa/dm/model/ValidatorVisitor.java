package org.ivoa.dm.model;

import java.util.ArrayList;

/**
 * MetadataObjectVisitor implementation :
 * This visitor sets username and update time on MetadattaObjects before peersistence.<br/>
 * Currently only sets this on MetadataRootEntityObjects, as we have these variables only there.
 * This may change in the future. Used by :
 *
 * @see org.ivoa.dm.DataModelManager#persist(java.util.List, String)
 * @author Gerard Lemson (mpe)
 */
public final class ValidatorVisitor extends MetaDataObjectVisitor {

	private ArrayList<MetadataObject> invalidObjects;
  /**
   * Public constructor.<br/>
   * Not protected and not a singleton for we need to preserve state within a persistence
   * transaction.
   *
   * @param user user name
   * @param currentTimestamp now
   */
  public ValidatorVisitor() {
  	super(true);
  	invalidObjects = new ArrayList<>();
  }

  // ~ Methods
  // ----------------------------------------------------------------------------------------------------------
  /**
   * Process the specified object
   *
   * @param object MetadataObject instance
   * @param argument optional argument
   */
  @Override
  public void process(final MetadataObject object, final Object argument) {
  	if(!object.isValid())
  		invalidObjects.add(object);
  }

  public boolean foundErrors(){
  	return invalidObjects.size() > 0;
  }
	public ArrayList<MetadataObject> getInvalidObjects() {
		return invalidObjects;
	}
}
// ~ End of file
// --------------------------------------------------------------------------------------------------------
