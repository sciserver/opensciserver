package org.ivoa.dm.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.EntityManager;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;
import org.eclipse.persistence.sessions.changesets.UnitOfWorkChangeSet;

/**
 * MetadataObjectVisitor implementation :
 * This visitor sets username and update time on MetadattaObjects before peersistence.<br/>
 * Currently only sets this on MetadataRootEntityObjects, as we have these variables only there.
 * This may change in the future. Used by :
 * 
 * @see org.ivoa.dm.DataModelManager#persist(java.util.List, String)
 * @author Gerard Lemson (mpe)
 */
public final class PersistObjectPreProcessor extends MetaDataObjectVisitor {

  /* members : statefull visitor */
  /** timestamp = now */
  private final Date now;
  private UnitOfWorkChangeSet changeSet;

  /**
   * Public constructor.<br/>
   * Not protected and not a singleton for we need to preserve state within a persistence
   * transaction.
   * 
   * @param user user name
   * @param currentTimestamp now
   */
  public PersistObjectPreProcessor(final Timestamp currentTimestamp, TransientObjectManager tom) {
    super(true);
    now = currentTimestamp;
    this.changeSet = tom.getUnitOfWorkChangeSet();
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
  	{}
    
    if (object instanceof MetadataRootEntityObject) {
      final MetadataRootEntityObject root = (MetadataRootEntityObject) object;
      // TODO only set modifiedDate if object has been modified.
      final ObjectChangeSet objectChangeSet = changeSet.getObjectChangeSetForClone(root);
      if(objectChangeSet != null && objectChangeSet.hasChanges())
      	root.setModificationDate(now); // 
      if (root.isPurelyTransient()) {
        root.setCreationDate(now);
      }
    }
  }
}
// ~ End of file
// --------------------------------------------------------------------------------------------------------
