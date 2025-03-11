package org.ivoa.util;

import java.util.Collection;

/**
 * Main Java utility methods : isEmpty / asList
 * 
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class JavaUtils {

  /**
   * Test if value is set ie not empty
   *
   * @param value string value
   * @return true if value is NOT empty
   */
  public static boolean isSet(final String value) {
    return !isEmpty(value);
  }

  /**
   * Test if value is empty (null or no chars)
   * 
   * @param value string value
   * @return true if value is empty (null or no chars)
   */
  public static boolean isEmpty(final String value) {
    return value == null || value.length() == 0;
  }

  /**
   * Test if value is empty (null or no chars after trim)
   * 
   * @param value string value
   * @return true if value is empty (null or no chars after trim)
   */
  public static boolean isTrimmedEmpty(final String value) {
    return value == null || value.trim().length() == 0;
  }

  /**
   * Is the given collection null or empty ?
   * 
   * @param col collection to test
   * @return true if the collection is null or empty
   */
  public static boolean isEmpty(final Collection<?> col) {
    return col == null || col.isEmpty();
  }
}
