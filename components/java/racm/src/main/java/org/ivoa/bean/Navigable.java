package org.ivoa.bean;

/**
 * This interface defines an accept method with a Visitor instance

 * @see Visitor
 * 
 * @param <T> type of the visited class
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public interface Navigable<T> {

  /**
   * Navigate through this instance and possibly its children using the given visitor instance
   *
   * @param visitor visitor instance
   */
  public void accept(Visitor<T> visitor);

  /**
   * Navigate through this instance and possibly its children using the given visitor instance
   * 
   * @param visitor visitor instance
   * @param argument optional argument
   */
  public void accept(Visitor<T> visitor, Object argument);
}
