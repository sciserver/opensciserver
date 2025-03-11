package org.ivoa.bean;

/**
 * Visitor class to implement the visitor pattern T is the type of the visited class.
 * 
 * @see Navigable
 * @param <T> type of the visited class
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class Visitor<T> extends LogSupport {

  /**
   * Empty implementation of the Visitor Design pattern
   * 
   * @param element instance to visit
   * @param argument optional argument
   * @return true if the traversal is OK
   */
  public boolean visit(final T element, final Object argument) {
    if (logB.isDebugEnabled()) {
      logB.debug(this.getClass().getSimpleName() + ".visit : element : " + element + " - " + argument);
    }
    return true;
  }

}
