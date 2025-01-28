package org.ivoa.conf;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Properties;

import org.ivoa.bean.LogSupport;
import org.ivoa.util.CollectionUtils;
import org.ivoa.util.JavaUtils;

/**
 * This class loads a property file & exposes a simple API
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class PropertyHolder extends LogSupport implements Serializable {
    //~ Constants --------------------------------------------------------------------------------------------------------

    /** serial UID for Serializable interface */
    private static final long serialVersionUID = 1L;

    //~ Members ----------------------------------------------------------------------------------------------------------
    /** property file */
    private final String propertyFile;
    /** properties */
    private Properties properties = null;

    //~ Constructors -----------------------------------------------------------------------------------------------------

    //~ Methods ----------------------------------------------------------------------------------------------------------
    /**
     * Concrete implementations of the SingletonSupport's initialize() method :<br/>
     * Callback to initialize this SingletonSupport instance<br/>
     *
     * Loads the configuration file found in the system classpath (removes any empty property
     * value)<br/>
     *
     * Use the Factory Pattern (introspection used to get a newInstance of the concrete PropertyHolder reference)
     *
     * @see SingletonSupport#initialize()
     *
   * @throws IllegalStateException if a problem occurred
     */
	public PropertyHolder(final String propertyFile) {
		this.propertyFile = propertyFile;

		if (JavaUtils.isEmpty(propertyFile)) {
			throw new IllegalStateException(
					"Unable to load the configuration : the property file to load is undefined !");
		}

		boolean res = false;

		try (InputStream in = getClass().getResource(propertyFile).openStream()) {

			this.properties = new Properties();
			this.properties.load(in);

			// filter empty strings :
			String k;

			// filter empty strings :
			String s;

			for (final Iterator<Object> it = this.properties.keySet().iterator(); it.hasNext();) {
				k = (String) it.next();
				s = this.properties.getProperty(k);

				if (JavaUtils.isTrimmedEmpty(s)) {
					it.remove();
				}
			}

			if (logB.isDebugEnabled()) {
				logB.debug("properties [" + propertyFile + "] : " + CollectionUtils.toString(getProperties()));
			}

			res = true;
		} catch (final IOException ioe) {
			logB.error("IO Failure : ", ioe);
		}

		if (!res) {
			throw new IllegalStateException("Unable to load the configuration : " + propertyFile + " !");
		}
	}

    /**
     * Return the property file name
     * @return property file name
     */
    public final String getPropertyFile() {
        return propertyFile;
    }

    /**
     * Returns the loaded properties. Warning : DO NOT change keys or values in the Properties (map)
     *
     * @return Properties (map)
     */
    public final Properties getProperties() {
        return this.properties;
    }

    /**
     * Get a String property
     *
     * @param name given key
     *
     * @return string value or null if not found or contains only white spaces
     */
    public final String getProperty(final String name) {
        return this.getProperties().getProperty(name);
    }

    /**
     * Get a required String property
     *
     * @param name given key
     * @return string value
     * @throws IllegalStateException if the value is empty
     */
    public final String getRequiredProperty(final String name) {
        final String value = this.getProperties().getProperty(name);

        if (JavaUtils.isTrimmedEmpty(value)) {
            throw new IllegalStateException("undefined property [" + name + "] in the configuration file = " + propertyFile + " !");
        }
        return value;
    }

    /**
     * Get a String property
     *
     * @param name given key
     * @param def default value
     *
     * @return string value or null if not found or contains only white spaces
     */
    public final String getProperty(final String name, final String def) {
        return this.getProperties().getProperty(name, def);
    }

    /**
     * Gets a Boolean from the Property value : valueOf(val)
     *
     * @param name property key
     *
     * @return boolean value or false if not found
     */
    public final boolean getBoolean(final String name) {
        final String val = this.getProperty(name);

        if (val == null) {
            return false;
        }

        return Boolean.valueOf(val).booleanValue();
    }
}
//~ End of file --------------------------------------------------------------------------------------------------------
