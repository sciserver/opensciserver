package org.ivoa.dm.model;

/**
 * Keeps track of life-cycle states such as whether an object is supposed to be marshalled. Possibly whether an
 * object is purely transient (i.e. does not have a rperesentation in a DB yet),  though JPA is supposedly taking care
 * of this. But might be noce for us to know. Can also keep track of removed objects (again, JPA should know), or has
 * been modified, and therefore its state should e updated in the DB (though again JPA will likely take care of
 * this?).  For now only marshalling flags are to be set, so that we know whether an XML IDREF should be set on a
 * reference, or an ivoId.
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class State {
  //~ Constants --------------------------------------------------------------------------------------------------------

  /**
   * marshall flag value to indicate that the parent metadata object must be marshalled
   */
  public static final int TO_BE_MARSHALLED = 32;

  //~ Members ----------------------------------------------------------------------------------------------------------
  /**
   * status value (binary flags)
   */
  private int status = 0;

  //~ Methods ----------------------------------------------------------------------------------------------------------
  /**
   * Set the marshall flag
   */
  public void setToBeMarshalled() {
    this.status |= TO_BE_MARSHALLED;
  }

  /**
   * UnSet the marshall flag
   */
  public void unsetToBeMarshalled() {
    if (isToBeMarshalled()) {
      this.status -= TO_BE_MARSHALLED;
    }
  }

  /**
   * Return the marshall flag
   *
   * @return true if the marshall flag is set
   */
  public boolean isToBeMarshalled() {
    return (this.status & TO_BE_MARSHALLED) == TO_BE_MARSHALLED;
  }
}
//~ End of file --------------------------------------------------------------------------------------------------------
