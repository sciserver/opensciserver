package org.ivoa.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * Collection toString() methods
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class CollectionUtils {

  /** Line separator string */
  private final static String LINE_SEPARATOR = System.getProperty("line.separator");

  /** begin separator = \n{\n */
  private final static String BEGIN_SEPARATOR = LINE_SEPARATOR + "{" + LINE_SEPARATOR;

  /** end separator = \n} */
  private final static String END_SEPARATOR = LINE_SEPARATOR + "}";

  //~ Constructors -----------------------------------------------------------------------------------------------------

  /**
   * Creates a new CollectionUtils object
   */
  private CollectionUtils() {
    /* no-op */
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------
  /**
   * toString method for a Map instance Format : <code><br/>
   * {<br/>
   * key = value<br/>
   * ...<br/>
   * }
   * </code>
   * 
   * @param m map
   * @return string
   */
  public static String toString(final Map<?, ?> m) {
    return toString(m, LINE_SEPARATOR, BEGIN_SEPARATOR, END_SEPARATOR);
  }

  /**
   * toString method for a Map instance with the given start, line and end separators
   * 
   * @param m map
   * @param lineSep line separator
   * @param startSep start separator
   * @param endSep end separator
   * @return string
   */
  public static String toString(final Map<?, ?> m, final String lineSep, final String startSep,
      final String endSep) {
    return toString(new StringBuilder(), m, lineSep, startSep, endSep).toString();
  }

  /**
   * toString method for a Map instance with the given start, line and end separators
   * 
   * @param sb buffer
   * @param m map
   * @param lineSep line separator
   * @param startSep start separator
   * @param endSep end separator
   * @return buffer (sb)
   */
  private static <K, V> StringBuilder toString(final StringBuilder sb, final Map<K, V> m, final String lineSep,
      final String startSep, final String endSep) {
    final Iterator<Map.Entry<K, V>> it = m.entrySet().iterator();

    sb.append(startSep);

    Map.Entry<K, V> e;
    Object key;
    Object value;

    for (int i = 0, max = m.size() - 1; i <= max; i++) {
      e = it.next();
      key = e.getKey();
      value = e.getValue();
      sb.append(key).append(" = ").append(value);

      if (i < max) {
        sb.append(lineSep);
      }
    }

    return sb.append(endSep);
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
