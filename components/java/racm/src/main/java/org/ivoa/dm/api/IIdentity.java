package org.ivoa.dm.api;

import java.io.Serializable;

/**
 * This class interface defines the Identity API to specify all objectType identifiers :  <br/>
 * - primary key value : numeric (long)  <br/>
 * - string xmlId : string for XML ID (xsd:id type) for xml instances <br/>
 * - ivoId : URI format for external references <br/>
 * - publisherDID : URI uniquely specifying the object as it is published in a DMService
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public interface IIdentity extends Serializable {

  //~ Methods ----------------------------------------------------------------------------------------------------------
  /**
   * Returns the primary key
   *
   * @return primary key or null if the metadata object is transient
   */
  public Long getId();

  /**
   * Returns the external identifier (URI)
   *
   * @return external identifier (URI)
   */
  public String getIvoId();

  /**
   * Returns the local xsd:ID for this object
   *
   * @return local xsd:ID for this object
   */
  public String getXmlId();

  /**
   * Returns the URI uniquely specifying the object as it is published in a DMService
   *
   * @return URI uniquely specifying the object as it is published in a DMService
   */
  public String getPublisherDID();

  /**
   * Sets the URI uniquely specifying the object as it is published in a DMService
   *
   * @param pPublisherDID URI uniquely specifying the object as it is published in a DMService
   */
  public void setPublisherDID(final String pPublisherDID);
}
//~ End of file --------------------------------------------------------------------------------------------------------
