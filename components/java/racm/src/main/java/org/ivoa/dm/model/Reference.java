package org.ivoa.dm.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.ivoa.bean.LogSupport;
import org.ivoa.util.JavaUtils;

/**
 * This class defines a reference (close to Identity class) as it contains both xmlId and ivoId identifiers : <br>
 * - string xmlId : string for XML IDREF (xsd:idref type) for xml instances <br>
 * - ivoId : URI format for external references
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Reference", namespace = "http://www.ivoa.net/xml/dm/base/v0.1")
public class Reference extends LogSupport implements Serializable {
  //~ Constants --------------------------------------------------------------------------------------------------------

  /** serial UID for Serializable interface */
  private static final long serialVersionUID = 1L;

  //~ Members ----------------------------------------------------------------------------------------------------------
  /**
   * An Identity value representing a local xsd:ID for this object, can be used when registering a DM/Resource
   * that has internal references.
   */
  @XmlIDREF
  @XmlSchemaType(name = "IDREF")
  @XmlAttribute(name = "xmlId", required = false)
  private Identity xmlIdentity;
  /**
   * The URI uniquely specifying the object as it is published in a DM service
   */
  @XmlSchemaType(name = "anyURI")
  @XmlAttribute(name = "ivoId", required = false)
  private String ivoId;
  /**
   * The identifier assigned to the referenced object by the publisher.  Must also be a URI (see SSA andother
   * similar usages) and unique.
   */
  @XmlSchemaType(name = "anyURI")
  @XmlAttribute(name = "publisherDID", required = false)
  private String publisherDID;

  //~ Constructors -----------------------------------------------------------------------------------------------------
  /**
   * Public No-arg Constructor for JAXB Compliance
   */
  public Reference() {
    // nothing to do
    }

  //~ Methods ----------------------------------------------------------------------------------------------------------
  /**
   * Returns the external identifier (URI)
   *
   * @return external identifier (URI)
   */
  public String getIvoId() {
    return ivoId;
  }

  /**
   * Sets the external identifier (URI)
   *
   * @param pIvoId external identifier (URI)
   */
  protected void setIvoId(final String pIvoId) {
    this.ivoId = pIvoId;
  }

  /**
   * Returns the local xsd:ID for this object
   *
   * @return local xsd:ID for this object
   */
  public String getXmlId() {
    return ((xmlIdentity == null) ? null : xmlIdentity.getXmlId());
  }

  /**
   * Sets the local xsd:ID for this object
   *
   * @param pXmlId local xsd:ID for this object
   */
  protected void setXmlId(final String pXmlId) {
    if (JavaUtils.isSet(pXmlId)) {
      /*
       * Note : if xmlIdentity != null then xmlIdentity.xmlId must be not null (used by XMLIDREF) :
       */
      if (xmlIdentity == null) {
        xmlIdentity = new Identity();
      }

      xmlIdentity.setXmlId(pXmlId);
    }
  }

  /**
   * Returns the URI uniquely specifying the object as it is published in a DMService
   *
   * @return URI uniquely specifying the object as it is published in a DMService
   */
  public String getPublisherDID() {
    return publisherDID;
  }

  /**
   * Sets the URI uniquely specifying the object as it is published in a DMService
   *
   * @param pPublisherDID URI uniquely specifying the object as it is published in a DMService
   */
  public void setPublisherDID(final String pPublisherDID) {
    this.publisherDID = pPublisherDID;
  }

	/**
	 * Returns a string representation : creates a temporary
	 * StringBuilder(STRING_BUFFER_CAPACITY) and calls
	 * #toString(java.lang.StringBuilder) method
	 *
	 * @return string representation
	 *
	 * @see #toString(java.lang.StringBuilder) method
	 */
	@Override
	public final String toString() {
		return toString(new StringBuilder()).toString();
	}

  /**
   * Puts the string representation in the given buffer : "[$id : $xmlId : $ivoId] " pattern
   *
   * @param sb given string buffer to fill
   *
   * @return the given string buffer filled with the string representation
   */
  protected final StringBuilder toString(final StringBuilder sb) {
    sb.append("[");

    if (JavaUtils.isSet(getXmlId())) {
      sb.append(" - xmlId: ").append(getXmlId());
    }

    if (JavaUtils.isSet(getIvoId())) {
      sb.append(" - ivoId: ").append(getIvoId());
    }

    if (JavaUtils.isSet(getPublisherDID())) {
      sb.append(" - publisherDID: ").append(getPublisherDID());
    }
   return sb.append("] ");
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
