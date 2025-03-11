package org.ivoa.dm.model;


import java.io.Serializable;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.ivoa.bean.LogSupport;


/**
 * Super class for all generated java classes (JPA 1.0 & soon JAXB 2.1 compliant). Implements Serializable to be
 * compatible with JPA API contract. Supports simple API (like java.lang.reflection) via MetaModelFactory class.
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public abstract class MetadataElement extends LogSupport implements Serializable, Cloneable {
  //~ Constants --------------------------------------------------------------------------------------------------------

  /** serial UID for Serializable interface */
  private static final long serialVersionUID = 1L;

  /** present flag for identity Map */
  private static final Object PRESENT = new Object();

  //~ Constructors -----------------------------------------------------------------------------------------------------

/**
   * Public No-arg Constructor for JAXB / JPA Compliance
   */
  public MetadataElement() {
    // nothing to do
  }

  //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * Gives the simple name for this UML Element
   *
   * @return getClass().getSimpleName() so no package prefix
   */
  public final String getClassName() {
    return getClass().getSimpleName();
  }

  /**
   * Clones this instance via standard java Cloneable support
   *
   * @return cloned instance
   *
   * @throws CloneNotSupportedException
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * Returns standard identity hashcode. Child classes can override this method to compute this value differently
   *
   * @return System.identityHashCode(this)
   *
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  /**
   * Returns equals(java.lang.Object, boolean) returned value with isDeep = false. Child classes can override
   * this method to force isDeep flag to true
   *
   * @param object the reference object with which to compare
   *
   * @return <code>true</code> if this object is the same as the object argument; <code>false</code> otherwise
   *
   * @see java.lang.Object#equals(java.lang.Object) method contract
   * @see #equals(java.lang.Object, boolean) method
   * @see #hashCode()
   * @see java.util.Hashtable
   */
  @Override
  public boolean equals(final Object object) {
    return equals(object, false);
  }

  /**
   * Returns true if the given object is a MetadataElement instance in this implementation. Child classes can
   * override this method to provide different behaviours like deep equals with attributes / references / collections
   *
   * @param object the reference object with which to compare
   * @param isDeep true means to call equals(Object, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   *
   * @return <code>true</code> if this object is the same as the object argument; <code>false</code> otherwise
   *
   * @see #equals(java.lang.Object) method
   */
  public boolean equals(final Object object, final boolean isDeep) {
    // first : identity comparison :
    if (this == object) {
      return true;
    }

    if (! (object instanceof MetadataElement)) {
      // if null : then returns false
      return false;
    }

    // implies that object is not null :
    if (! this.getClass().equals(object.getClass())) {
      return false;
    }

    return true;
  }

  /**
   * Returns the property value given the property name. <br>
   * Can be any property (internal, attribute, reference, collection) and all type must be supported (dataType,
   * objectType, enumeration)
   *
   * @param propertyName name of the property (like in UML model)
   *
   * @return property value or null if unknown or not defined
   */
  public Object getProperty(final String propertyName) {
    // needed by super.getProperty(propertyName) call
    return null;
  }

  /**
   * Sets the property value to the given property name. <br>
   * Can be any property (internal, attribute, reference, collection) and all type must be supported (dataType,
   * objectType, enumeration)
   *
   * @param propertyName name of the property (like in UML model)
   * @param value to be set
   *
   * @return true if property has been set
   */
  public boolean setProperty(final String propertyName, final Object value) {
    return false;
  }

  /**
   * Returns a string representation : creates a temporary StringBuilder(STRING_BUFFER_CAPACITY) and calls
   * #toString(java.lang.StringBuilder) method
   *
   * @return string representation
   *
   * @see #toString(java.lang.StringBuilder) method
   * @see #toString(java.lang.StringBuilder, boolean) method
   */
  @Override
  public final String toString() {
    return toString(new StringBuilder()).toString();
  }

  /**
   * Puts the string representation in the given string buffer : NO DEEP toString(java.lang.StringBuilder,
   * boolean) recursion
   *
   * @param sb given string buffer to fill
   *
   * @return the given string buffer filled with the string representation
   *
   * @see #toString(java.lang.StringBuilder, boolean) method
   */
  public final StringBuilder toString(final StringBuilder sb) {
    return toString(sb, false);
  }

  /**
   * Puts the string representation in the given string buffer WITH DEEP toString(java.lang.StringBuilder,
   * boolean) recursion
   *
   * @return the given string buffer filled with the string representation
   *
   * @see #toString(java.lang.StringBuilder, boolean) method
   */
  public final String deepToString() {
    return toString(new StringBuilder(), true).toString();
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
  public StringBuilder toString(final StringBuilder sb, final boolean isDeep) {
    final Map<MetadataElement, Object> ids = (isDeep) ? new IdentityHashMap<MetadataElement, Object>() : null;

    MetadataElement.deepToString(sb, isDeep, ids, this);

    if (ids != null) {
      // clears identity map to force gc asap :
      ids.clear();
    }

    return sb;
  }

  /**
   * Puts the string representation in the given string buffer : &lt;br&gt; "Type =[class name @ hashcode] : {
   * field name = field value , ...}"
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   * @param ids identity map to avoid toString() duplicates
   *
   * @return the given string buffer filled with the string representation
   */
  protected abstract StringBuilder deepToString(final StringBuilder sb, final boolean isDeep,
                                                final Map<MetadataElement, Object> ids);

  /**
   * Puts the base string representation in the given buffer : "$class_name$ @ $hashcode$" pattern
   *
   * @param sb given string buffer to fill
   *
   * @return the given string buffer filled with the string representation
   */
  protected StringBuilder baseToString(final StringBuilder sb) {
    return sb.append(getClass().getName()).append("@").append(Integer.toHexString(System.identityHashCode(this)));
  }

  /**
   * Puts the string representation of the given object in the given string buffer : &lt;br&gt;  "Type=[[$id :
   * $xmlId : $ivoId] $class_name$ @ $hashcode$]] : { field name = field value , ...}" pattern
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   * @param ids identity map to avoid toString() duplicates
   * @param o reference to represent
   *
   * @return the given string buffer filled with the string representation
   */
  public static final StringBuilder deepToString(final StringBuilder sb, final boolean isDeep,
                                                 final Map<MetadataElement, Object> ids, final Object o) {
    if (o != null) {
      if (o instanceof MetadataElement) {
        final MetadataElement e = (MetadataElement) o;

        sb.append("\n").append(e.getClassName()).append("=[");
        e.baseToString(sb).append("]");

        // avoid toString() duplicates :
        if(!(o instanceof MetadataObject) ||( isDeep && ! exists(e, ids))) {
          e.deepToString(sb, isDeep, ids);
        }
      } else if (o instanceof Collection<?>) {
        final Collection<?> c = (Collection<?>) o;

        if (c.size() > 0) {
          toString(sb.append("\n"), isDeep, ids, c);
        } else {
          sb.append("{}");
        }
      } else {
        sb.append(o.toString());
      }
    }

    return sb;
  }

  /**
   * Puts the string representation of the given object in the given string buffer :
   * key=value
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   * @param o reference to represent
   *
   * @return the given string buffer filled with the string representation
   */
  public static final StringBuilder deepDataTypeToString(final StringBuilder sb, final boolean isDeep, final Object o) {
    if (o != null) {
      if (o instanceof MetadataDataType) {
        final MetadataDataType d = (MetadataDataType) o;

        d.deepToString(sb, isDeep, null);
      } else {
        sb.append(o.toString());
      }
    }

    return sb;
  }

  /**
   * toString method for the given Collection
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   * @param ids identity map to avoid toString() duplicates
   * @param c collection
   *
   * @return the given string buffer filled with the string representation
   */
  private static String toString(final StringBuilder sb, final boolean isDeep, final Map<MetadataElement, Object> ids,
                                 final Collection<?> c) {
    return toString(sb, isDeep, ids, c, System.getProperty("line.separator"), "{", "}");
  }

  /**
   * toString method for the given Collection and string separators
   *
   * @param sb given string buffer to fill
   * @param isDeep true means to call toString(sb, true) recursively for all attributes / references / collections
   *        which are MetadataElement implementations
   * @param ids identity map to avoid toString() duplicates
   * @param c collection
   * @param sep seperator like '\n'
   * @param startSep starting seperator like '{'
   * @param endSep ending seperator like ']'
   *
   * @return the given string buffer filled with the string representation
   */
  private static String toString(final StringBuilder sb, final boolean isDeep, final Map<MetadataElement, Object> ids,
                                 final Collection<?> c, final String sep, final String startSep, final String endSep) {
    final Iterator<?> it = c.iterator();

    sb.append(startSep);

    for (int i = 0, max = c.size() - 1; i <= max; i++) {
      deepToString(sb, isDeep, ids, it.next());

      if (i < max) {
        sb.append(sep);
      }
    }

    sb.append(endSep);

    return sb.toString();
  }

  /**
   * Check if the identity map contains the given reference. If not found, adds the given reference in the
   * identity map
   *
   * @param e reference to find
   * @param ids identity map
   *
   * @return true if found
   */
  public static final boolean exists(final MetadataElement e, final Map<MetadataElement, Object> ids) {
    final boolean exist = ids.containsKey(e);

    if (! exist) {
      ids.put(e, PRESENT);
    }

    return exist;
  }

  /**
   * Utility method for <code>equals()</code> methods.
   *
   * @param o1 one object
   * @param o2 another object
   *
   * @return <code>true</code> if they're both <code>null</code> or both equal
   */
  protected static final boolean areEquals(final Object o1, final Object o2) {
    if ((o1 != o2) && ((o1 == null) || ! o1.equals(o2))) {
      return false;
    }

    return true;
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
