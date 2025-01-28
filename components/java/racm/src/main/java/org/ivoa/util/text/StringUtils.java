package org.ivoa.util.text;

/**
 * Useful string methods
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class StringUtils {
  //~ Constructors -----------------------------------------------------------------------------------------------------

/**
   * Forbidden Constructor
   */
  private StringUtils() {
    /* no-op */
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * Return an encoded string : < > characters replaced by html entities
   *
   * @param source input string
   * @return encoded string
   */
  public static final String escapeXml(final String source) {
    return source.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
