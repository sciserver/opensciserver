package org.ivoa.dm.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Root Entity Object is a base type corresponding to a full XML document
 * 
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
@MappedSuperclass
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "MetadataRootEntityObject", namespace = "http://www.ivoa.net/xml/dm/base/v0.1")
public abstract class MetadataRootEntityObject extends MetadataObject {
    //~ Constants --------------------------------------------------------------------------------------------------------

    /**
     * serial UID for Serializable interface
     */
    private static final long serialVersionUID = 1L;

    //~ Members ----------------------------------------------------------------------------------------------------------

  /**
   * Date defined when this root entity was added to the database
   */
  @Basic(optional = true)
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "creationDate", nullable = true)
  @XmlElement(name = "creationDate", required = false, type = Date.class)
  private Date creationDate;

  /**
   * Date defined when this root entity was last updated in the database
   */
  @Basic(optional = true)
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "modificationDate", nullable = true)
  @XmlElement(name = "modificationDate", required = true, type = Date.class)
  private Date modificationDate;

  /**
   * The transient object manager every entity ust be added to.<br/>
   */
  @Transient
  @XmlTransient
  private TransientObjectManager tom;
    //~ Constructors -----------------------------------------------------------------------------------------------------

  /**
   * Public constructor
   */
  public MetadataRootEntityObject(TransientObjectManager tom) {
    super();
    this.tom = tom;
    tom.addEntity(this);
  }
  protected MetadataRootEntityObject() {
     super();
  }

    //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * @return Date defined when this root entity was added to the database
   */
  public Date getCreationDate() {
    return creationDate;
  }

  /**
   * @param pCreationDate Date defined when this root entity was added to the database
   */
  public void setCreationDate(final Date pCreationDate) {
    this.creationDate = pCreationDate;
  }

  /**
   * @return Date defined when this root entity was last updated in the database
   */
  public Date getModificationDate() {
    return modificationDate;
  }

  /**
   * @param pModificationDate Date defined when this root entity was last updated in the database
   */
  public void setModificationDate(final Date pModificationDate) {
    this.modificationDate = pModificationDate;
  }

  /**
   * Puts the string representation in the given string buffer : &lt;br&gt; "Type =[class name @ hashcode] : {
   * field name = field value , ...}". If isDeep is true, it uses an IdentityHashMap to avoid duplicate toString() in
   * the recursion
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   *
   * @return the given string buffer filled with the string representation
   */
  @Override
  protected StringBuilder deepToString(final StringBuilder sb, final boolean isDeep,
      final Map<MetadataElement, Object> ids) {
      // TODO : implement
    return null;
  }
 @Override
public TransientObjectManager getTom() {
	return tom;
}
 @Override
public void setTom(TransientObjectManager tom) {
	if(this.tom == null){
		this.tom = tom;
		tom.addEntity(this);
	}
	// else throw exception??
}
@Override
public IMetadataObjectContainer getContainer(){
	return this.tom;
}

}
