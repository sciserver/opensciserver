package org.ivoa.dm.model;

import org.ivoa.bean.Visitor;
import org.ivoa.dm.MetaModelFactory;
import org.ivoa.dm.ObjectClassType;
import org.ivoa.util.JavaUtils;

/**
 * Represents a visitor in the visitor pattern.<br>
 * Can traverse a MetadataObjbect containment tree.
 *
 * By default traverses only collections.
 * Has a process method called before the contained collections are traversed, or afterwards depending on the processBeforeChildren flag
 *
 * TODO decide whether we want to traverse a complete graph, in which case
 * we need to keep track of whether objects have already been visited, as
 * objects may now be reached in multiple ways.
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public abstract class MetaDataObjectVisitor extends Visitor<MetadataObject> {

  //~ Members ----------------------------------------------------------------------------------------------------------
  /** flag to indicate that the process method() must be applied to the current element before its collections are being processed */
  private final boolean processBeforeChildren;

  //~ Constructors -----------------------------------------------------------------------------------------------------
  /**
   * Protected constructor to avoid to create instance except for singletons (stateless classes)
   *
   * @param pProcessBeforeChildren flag to indicate that the process method() must be applied to the current element before its collections are being processed
   */
  protected MetaDataObjectVisitor(final boolean pProcessBeforeChildren) {
    super();
    this.processBeforeChildren = pProcessBeforeChildren;
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------
  /**
   * Concrete implementation of the Visitor Design pattern to traverse the MetaDataObject and its collections : <br/>
   * First the preProcess method of the specified visitor is called with the given element. Then it is sent on to
   * the contained collections. At the end the postProcess method is called with the given element.
   *
   * @param element MetadataObject instance to visit
   * @param argument optional argument
   * @return true if the traversal is OK
   */
  @Override
  public final boolean visit(final MetadataObject element, final Object argument) {
    if (logB.isDebugEnabled()) {
      logB.debug(this.getClass().getSimpleName() + ".visit : enter : element : " + element);
    }

    // send the preProcess events :
    if (this.processBeforeChildren) {
      this.process(element, argument);
    }

    final ObjectClassType md = element.getClassMetaData();
    final java.util.Collection<org.ivoa.metamodel.Collection> collections = md.getCollectionList();

    if (!JavaUtils.isEmpty(collections)) {
      final MetaModelFactory mmf = MetaModelFactory.getInstance();
      
      java.util.Collection<?> objects;
      for (org.ivoa.metamodel.Collection collection : collections) {
        objects = (java.util.Collection<?>) element.getProperty(collection.getName());
        
        if (!JavaUtils.isEmpty(objects)) {
          // Check that collection data type corresponds to an object Type (not primitive type):
          if (mmf.getObjectClassType(collection.getDatatype().getName()) != null) {
            for (final Object val : objects) {
              if (val instanceof MetadataObject) {
                final MetadataObject item = (MetadataObject) val;
                // traverse collection elements :
                item.accept(this, argument);
              }
            }
          }
        }
      }
    }

    // send the postProcess events :
    if (!this.processBeforeChildren) {
      this.process(element, argument);
    }

    if (logB.isDebugEnabled()) {
      logB.debug(this.getClass().getSimpleName() + ".visit : exit : element : " + element);
    }
    return true;
  }

  /**
   * Process the specified object
   * @param object MetadataObject instance
   * @param argument optional argument
   */
  public abstract void process(final MetadataObject object, final Object argument);

  /*
   * Utility methods for accessing protected methods of MetadataObject
   *
   * Facade like pattern
   */
  /**
   * Return the status object of the MetadataObject instance
   *
   * @param object MetadataObject instance
   * @return state instance
   */
  protected final State getInternalState(final MetadataObject object) {
    return object.getInternalState();
  }

  /**
   * Set the external identifier (URI) on the MetadataObject instance
   *
   * @param object MetadataObject instance
   * @param pIvoId external identifier (URI)
   */
  protected final void setIvoId(final MetadataObject object, final String pIvoId) {
    object.getIdentity().setIvoId(pIvoId);
  }

  /**
   * Set the local xsd:ID for this object on the MetadataObject instance
   *
   * @param object MetadataObject instance
   * @param pXmlId local xsd:ID for this object
   */
  protected final void setXmlId(final MetadataObject object, final String pXmlId) {
    object.getIdentity().setXmlId(pXmlId);
  }

  /**
   * Set all Reference objects to null on the MetadataObject instance
   *
   * @param object MetadataObject instance
   */
  protected final void resetReferencesAfterMarshalling(final MetadataObject object) {
    object.resetReferencesAfterMarshalling();
  }

  /**
   * Set all Reference objects to their appropriate value on the MetadataObject instance
   *
   * @param object MetadataObject instance
   */
  protected final void prepareReferencesForMarshalling(final MetadataObject object) {
    object.prepareReferencesForMarshalling();
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
