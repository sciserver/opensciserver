package org.ivoa.dm.model;

/**
 * MetadataDataType : Super class for all UML DataType only.
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public abstract class MetadataDataType extends MetadataElement {
  //~ Constants --------------------------------------------------------------------------------------------------------

  /** serial UID for Serializable interface */
  private static final long serialVersionUID = 1L;

  //~ Constructors -----------------------------------------------------------------------------------------------------

/**
   * Public No-arg Constructor for JAXB / JPA Compliance
   */
  public MetadataDataType() {
    super();
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * Puts the string representation in the given string buffer :  field name = field value , ...".
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes
   *
   * @return the given string buffer filled with the string representation
   */
  @Override
  public StringBuilder toString(final StringBuilder sb, final boolean isDeep) {
    // dump all attributes for Data Types :
    this.deepToString(sb, true, null);

    return sb;
  }
  /**
   * Default validation method for DataType-s<br/>
   * @return
   */
  public boolean isValid(){
  	return true;
  }
  public String validationErrors(){
  	return "";
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
