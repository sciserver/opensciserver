package org.ivoa.xml.validator;

import java.io.Serializable;


/**
 * Xml validation error message
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class ErrorMessage implements Serializable {
  //~ Constants --------------------------------------------------------------------------------------------------------

  /** serial UID for Serializable interface */
  private static final long serialVersionUID = 1L;

  //~ Members ----------------------------------------------------------------------------------------------------------

  /** TODO : Field Description */
  private SEVERITY severity;
  /** TODO : Field Description */
  private int lineNumber;
  /** TODO : Field Description */
  private int columnNumber;
  /** TODO : Field Description */
  private String message;

  //~ Constructors -----------------------------------------------------------------------------------------------------

/**
   * Constructor
   * @param pSeverity severity of the validation error
   * @param pLineNumber line number in the input document
   * @param pColumnNumber column number in the input document
   * @param pMessage error message
   */
  public ErrorMessage(final SEVERITY pSeverity, final int pLineNumber, final int pColumnNumber, final String pMessage) {
    this.severity = pSeverity;
    this.lineNumber = pLineNumber;
    this.columnNumber = pColumnNumber;
    this.message = pMessage;
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * TODO : Method Description
   *
   * @return value TODO : Value Description
   */
  @Override
  public String toString() {
    return this.severity + " [" + this.lineNumber + " : " + this.columnNumber + "] " + this.message;
  }

  /**
   * TODO : Method Description
   *
   * @return value TODO : Value Description
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * TODO : Method Description
   *
   * @return value TODO : Value Description
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * TODO : Method Description
   *
   * @return value TODO : Value Description
   */
  public String getMessage() {
    return message;
  }

  /**
   * TODO : Method Description
   *
   * @return value TODO : Value Description
   */
  public SEVERITY getSeverity() {
    return severity;
  }

  //~ Enumerations -----------------------------------------------------------------------------------------------------

  /**
   * Error severity enum
   */
  public enum SEVERITY {
      //~ Enumeration constant initializers ----------------------------------------------------------------------------
    /** WARNING */
    WARNING, 
    /** ERROR */
    ERROR, 
    /** FATAL */
    FATAL;
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
