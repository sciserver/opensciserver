package org.ivoa.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Log Support class to manage LogUtil references and classLoader issues
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class LogSupport {
	/** Main logger = org.ivoa */
	private static final String LOGGER_MAIN = "org.ivoa";
	/** Base framework logger = org.ivoa.base */
	private static final String LOGGER_BASE = "org.ivoa.base";
	/** Development logger = org.ivoa.dev */
	private static final String LOGGER_DEV = "org.ivoa.dev";
	// ~ Constants
	// --------------------------------------------------------------------------------------------------------

	/** 
   * Main Logger for the application
   * @see org.ivoa.bean.LogSupport
   */
  protected static Log log = LogFactory.getLog(LOGGER_MAIN);
	/**
	 * Logger for the base framework
	 * 
	 * @see org.ivoa.bean.LogSupport
	 */
	protected static Log logB = LogFactory.getLog(LOGGER_BASE);
	/**
	 * Logger for development purposes
	 * 
	 * @see org.ivoa.bean.LogSupport
	 */
	protected static Log logD = LogFactory.getLog(LOGGER_DEV);

	// ~ End of file
	// --------------------------------------------------------------------------------------------------------
}
