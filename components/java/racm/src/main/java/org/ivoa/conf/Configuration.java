package org.ivoa.conf;

/**
 * This class acts as a singleton to store the global Configuration for the
 * application :
 * <ul>
 * <li>Values are loaded from the file global.properties located in the class
 * path (first occurrence in the class path is loaded).</li>
 * </ul>
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class Configuration extends PropertyHolder {
	// ~ Constants
	/** file name for property file */
	private static final String CONFIGURATION_FILE = "/runtime.properties";
	// --------------------------------------------------------------------------------------------------------

	/** serial UID for Serializable interface */
	private static final long serialVersionUID = 1L;
	/** singleton instance */
	private static final Configuration instance = new Configuration(CONFIGURATION_FILE);

	/* property keys */
	/**
	 * keyword for service ivo id. The IVO Identifier by which the data access
	 * service is registered. Used as prefix for ivo-id-s of all objects.
	 */
	private static final String SERVICE_IVOID = "service.ivoid";
	/** keyword for test mode */
	private static final String MODE_TEST = "mode.test";
	// ~ Members
	// ----------------------------------------------------------------------------------------------------------
	/** As the prefix is needed often, store it explicitly. */
	private String ivoIdPrefix = "";

	// ~ Constructors
	// -----------------------------------------------------------------------------------------------------
	private Configuration(String propertyFile) {
		super(propertyFile);

		this.ivoIdPrefix = getProperty(SERVICE_IVOID, "").trim() + "#";
	}

	// ~ Methods
	// ----------------------------------------------------------------------------------------------------------
	/**
	 * Return the Configuration singleton instance
	 *
	 * @return Configuration singleton instance
	 *
	 * @throws IllegalStateException
	 *             if a problem occured
	 */
	public static final Configuration getInstance() {
		return instance;
	}

	/**
	 * Returns test mode
	 *
	 * @return test mode
	 */
	public final boolean isTest() {
		return this.getBoolean(MODE_TEST);
	}

	/**
	 * Gives the service IVO Id + "#" as prefix for all
	 *
	 * @return service IVO Id + "#"
	 */
	public final String getIVOIdPrefix() {
		return this.ivoIdPrefix;
	}

	/**
	 * Gives the property value for the key [intermediate.model.file]
	 *
	 * @return property value for the key [intermediate.model.file] or null if not
	 *         found or contains only white spaces
	 */
	public String getIntermediateModelFile() {
		return getProperty("intermediate.model.file");
	}

	/**
	 * Gives the property value for the key [project.name]
	 *
	 * @return property value for the key [project.name] or null if not found or
	 *         contains only white spaces
	 */
	public String getProjectName() {
		return getProperty("project.name");
	}

	/**
	 * Gives the property value for the key [project.contact]
	 *
	 * @return property value for the key [project.contact] or null if not found or
	 *         contains only white spaces
	 */
	public String getProjectContact() {
		return getProperty("project.contact");
	}

	/**
	 * Gives the property value for the key [project.version]
	 *
	 * @return property value for the key [project.version] or null if not found or
	 *         contains only white spaces
	 */
	public String getProjectVersion() {
		return getProperty("project.version");
	}

	/**
	 * Gives the property value for the key [project.title]
	 *
	 * @return property value for the key [project.title] or null if not found or
	 *         contains only white spaces
	 */
	public String getProjectTitle() {
		return getProperty("project.title");
	}

	/**
	 * Gives the property value for the key [base.package]
	 *
	 * @return property value for the key [base.package] or null if not found or
	 *         contains only white spaces
	 */
	public String getBasePackage() {
		return getProperty("base.package");
	}

	/**
	 * Gives the property value for the key [jaxb.package]
	 *
	 * @return property value for the key [jaxb.package] or null if not found or
	 *         contains only white spaces
	 */
	public String getJAXBPackage() {
		return getProperty("jaxb.package");
	}

	/**
	 * Gives the property value for the key [intermediate.model.xmlns]
	 *
	 * @return property value for the key [intermediate.model.xmlns] or null if not
	 *         found or contains only white spaces
	 */
	public String getIntermediateModelXmlns() {
		return getProperty("intermediate.model.xmlns");
	}

	/**
	 * Gives the property value for the key [jpa.persistence.unit]
	 *
	 * @return property value for the key [jpa.persistence.unit] or null if not
	 *         found or contains only white spaces
	 */
	public String getJPAPU() {
		return getProperty("jpa.persistence.unit");
	}

	/**
	 * Gives the property value for the key [root.schema.url]
	 *
	 * @return property value for the key [root.schema.url] or null if not found or
	 *         contains only white spaces
	 */
	public String getRootSchemaURL() {
		return getProperty("root.schema.url");
	}

	/**
	 * Gives the property value for the key [root.schema.url]
	 *
	 * @return property value for the key [root.schema.url] or null if not found or
	 *         contains only white spaces
	 */
	public String getRootSchemaLocation() {
		return getProperty("root.schema.location");
	}

	/**
	 * Gives the property value for the key [jaxb.context.classpath]
	 *
	 * @return property value for the key [jaxb.context.classpath] or null if not
	 *         found or contains only white spaces
	 */
	public String getJAXBContextClasspath() {
		return getProperty("jaxb.context.classpath");
	}

	/**
	 * Gives the property value for the key [tap.metadata.xml.file]
	 *
	 * @return property value for the key [tap.metadata.xml.file] or null if not
	 *         found or contains only white spaces
	 */
	public String getTAPMetadataXMLFile() {
		return getProperty("tap.metadata.xml.file");
	}

	/**
	 * Gives the property value for the key [tap.metadata.votable.file]
	 *
	 * @return property value for the key [tap.metadata.votable.file] or null if not
	 *         found or contains only white spaces
	 */
	public String getTAPMetadataVOTableFile() {
		return getProperty("tap.metadata.votable.file");
	}
}
// ~ End of file
// --------------------------------------------------------------------------------------------------------
