package org.ivoa.dm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.ivoa.bean.LogSupport;
import org.ivoa.conf.Configuration;
import org.ivoa.dm.model.MetadataElement;
import org.ivoa.metamodel.Attribute;
import org.ivoa.metamodel.DataType;
import org.ivoa.metamodel.Element;
import org.ivoa.metamodel.Enumeration;
import org.ivoa.metamodel.Model;
import org.ivoa.metamodel.ObjectType;
import org.ivoa.metamodel.PrimitiveType;
import org.ivoa.util.CollectionUtils;
import org.ivoa.util.text.StringUtils;

/**
 * This Class exposes a MetaModel API to get informations on every UML elements to allow easy inspection of any
 * instances of the model It loads the metaModel from an XML document with JAXB and prepares the collections
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class MetaModelFactory extends LogSupport {
    // ~ Constants
    // --------------------------------------------------------------------------------------------------------

    /** configuration test flag */
    public static final boolean isTest = Configuration.getInstance().isTest();
    /** meta model path TODO use RuntimeConfiguration */
    public static final String MODEL_NAMESPACE = "https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1";
    /**
     * model path TODO do we need this? Elsewhere we already use the path explicitly
     */
    public static final String BASE_PACKAGE;

    static {
        String bp = Configuration.getInstance().getBasePackage();

        BASE_PACKAGE = (bp.endsWith(".") ? bp : (bp + "."));
    }
    /** Identity Type */
    private static final String IDENTITY_TYPE = "Identity";
    /** model path */
    private static final String JAXB_PACKAGE = Configuration.getInstance().getJAXBPackage();
    /** model file to load */
    private static final String MODEL_FILE = Configuration.getInstance().getIntermediateModelFile();
    /** singleton instance */
    private static final MetaModelFactory instance = new MetaModelFactory();

    // ~ Members
    // ----------------------------------------------------------------------------------------------------------
    /** meta model loaded */
    private final Model model;

    // Maybe we should reuse the xmiId property instead of name ?
    /** primitiveTypes in the model */
    private final Map<String, PrimitiveType> primitiveTypes = new HashMap<>();
    /** dataTypes in the model */
    private final Map<String, DataType> dataTypes = new HashMap<>();
    /** enumarations in the model */
    private final Map<String, Enumeration> enumerations = new HashMap<>();
    /** objectTypes in the model */
    private final Map<String, ObjectType> objectTypes = new LinkedHashMap<>();
    /**
     * skosconcepts used in the model. This Map is keyed by the utype of the skosconcept and the ObjectTYpe where it
     * appears
     */
    private final Map<Attribute, ObjectType> skosConcepts = new LinkedHashMap<>();
    /** classTypes in the model */
    private final Map<String, ClassType> classTypes = new HashMap<>();
    /** objectClassTypes in the model */
    private final Map<String, ObjectClassType> objectClassTypes = new LinkedHashMap<>();
    /** classes in the model */
    private final Map<String, Class<? extends MetadataElement>> classes = new HashMap<>();

    // ~ Constructors
    // -----------------------------------------------------------------------------------------------------

    // ~ Methods
    // ----------------------------------------------------------------------------------------------------------
    /**
     * Return the EntityConfigFactory singleton instance
     *
     * @return EntityConfigFactory singleton instance
     *
     * @throws IllegalStateException if a problem occurred
     */
    public static MetaModelFactory getInstance() {
        return instance;
    }

    private MetaModelFactory() {
        if (logB.isTraceEnabled()) {
            logB.trace("MetaModelFactory constructor ...");
        }

        try {
            this.model = loadModel(MODEL_FILE);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // TODO check whether next is sufficient to add Identity to datatypes
        for (final org.ivoa.metamodel.Profile prof : this.model.getProfile()) {
            try {
                processProfile(prof);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        for (final org.ivoa.metamodel.Package p : this.model.getPackage()) {
            try {
                processPackage(p, BASE_PACKAGE);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("primitiveTypes : \n" + CollectionUtils.toString(getPrimitiveTypes(), "\n", "", ""));
            log.debug("dataTypes : \n" + CollectionUtils.toString(getDataTypes(), "\n", "", ""));
            log.debug("enumerations : \n" + CollectionUtils.toString(getEnumerations(), "\n", "", ""));
            log.debug("objectTypes : \n" + CollectionUtils.toString(getObjectTypes(), "\n", "", ""));
            log.debug("classes : \n" + CollectionUtils.toString(getClasses(), "\n", "", ""));
        }

        for (final DataType d : getDataTypes().values()) {
            // creates an associated classType :
            ClassType ct = new ClassType(d);
            ct.init(getDataTypes());

            getClassTypes().put(d.getName(), ct);
        }

        // first : retrieve Identity DataType :
        ObjectClassType.doInitIdentity(getDataType(IDENTITY_TYPE));

        for (final ObjectType o : getObjectTypes().values()) {
            // creates an associated objectClassType :
            ObjectClassType ot = new ObjectClassType(o);
            ot.initObjectClassType(getObjectTypes());

            getObjectClassTypes().put(o.getName(), ot);

            for (Attribute attr : o.getAttribute()) {
                if (attr.getSkosconcept() != null) {
                    skosConcepts.put(attr, o);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("classTypes : \n" + CollectionUtils.toString(getClassTypes(), "\n", "", ""));
            log.debug("objectClassTypes : \n" + CollectionUtils.toString(getObjectClassTypes(), "\n", "", ""));
        }

        for (final ObjectClassType c : getObjectClassTypes().values()) {
            if (c.isRoot()) {
                log.debug("root : " + c.getType().getName());
            }
        }

        for (final ObjectClassType c : getObjectClassTypes().values()) {
            if (c.getBaseclass() != null) {
                ObjectClassType base = getObjectClassType(c.getBaseclass());

                base.addSubclass(c);
            }
        }
    }

    /**
     * Returns the JAXBContext
     *
     * @return JAXBContext
     */
    private static JAXBContext getJAXBContext() {
        try {
            return JAXBContext.newInstance(JAXB_PACKAGE);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the profile. Pay particular attention to the Identity type, which is to be mapped to the predefined class
     * org.ivoa.dm.model
     *
     * @param prof uml profile
     * @throws ClassNotFoundException
     */
    private void processProfile(final org.ivoa.metamodel.Profile prof) throws ClassNotFoundException {
        for (final org.ivoa.metamodel.Package p : prof.getPackage()) {
            processPackage(p, BASE_PACKAGE);
        }
    }

    /**
     * Recursive method to fill collections (primitive types, data types, enumeration & object types)
     *
     * @param p package to process
     * @param parentPath parent package path
     * @throws ClassNotFoundException
     */
    private void processPackage(final org.ivoa.metamodel.Package p, final String parentPath)
            throws ClassNotFoundException {
        if (log.isTraceEnabled()) {
            log.trace("processPackage : enter : " + p.getName());
        }

        final String packagePath = parentPath + p.getName() + ".";

        Object old;

        for (final PrimitiveType t : p.getPrimitiveType()) {
            processDescription(t);

            old = getPrimitiveTypes().put(t.getName(), t);

            if (old != null) {
                log.error("MetaModelFactory.processPackage : DUPLICATES detected with same name : { " + t + " } <> { "
                        + old + " }");
            }
        }

        String className;

        for (final DataType d : p.getDataType()) {
            processDescription(d);
            processCollection(d.getAttribute());

            old = getDataTypes().put(d.getName(), d);

            if (old != null) {
                log.error("MetaModelFactory.processPackage : DUPLICATES detected with same name : { " + d + " } <> { "
                        + old + " }");
            }

            // adds ObjectType classes :
            className = packagePath + d.getName();
            getClasses().put(d.getName(), Class.forName(className).asSubclass(MetadataElement.class));
        }

        for (final Enumeration e : p.getEnumeration()) {
            processDescription(e);

            old = getEnumerations().put(e.getName(), e);

            if (old != null) {
                log.error("MetaModelFactory.processPackage : DUPLICATES detected with same name : { " + e + " } <> { "
                        + old + " }");
            }
        }

        for (final ObjectType o : p.getObjectType()) {
            processDescription(o);

            processCollection(o.getAttribute());
            processCollection(o.getReference());
            processCollection(o.getCollection());

            old = getObjectTypes().put(o.getName(), o);

            if (old != null) {
                log.error("MetaModelFactory.processPackage : DUPLICATES detected with same name : { " + o + " } <> { "
                        + old + " }");
            }

            // adds ObjectType classes :
            className = packagePath + o.getName();
            getClasses().put(o.getName(), Class.forName(className).asSubclass(MetadataElement.class));
        }

        for (final org.ivoa.metamodel.Package cp : p.getPackage()) {
            processDescription(cp);

            processPackage(cp, packagePath);
        }

        if (log.isTraceEnabled()) {
            log.trace("processPackage : exit : " + p.getName());
        }
    }

    /**
     * For the given collection of UML Element, fix description content
     *
     * @param c collection to process
     */
    private void processCollection(final Collection<? extends Element> c) {
        for (final Element e : c) {
            processDescription(e);
        }
    }

    /**
     * Fix CR and Tab chars in description field
     *
     * @param e UML Element
     */
    private void processDescription(final Element e) {
        String desc = e.getDescription();

        // converts double quotes to simple quotes (HTML) :
        desc = desc.replaceAll("\"", "'");
        // remove all white spaces (CR)
        desc = desc.replaceAll("\\s+", " ");
        desc = StringUtils.escapeXml(desc);

        e.setDescription(desc);
    }

    /**
     * Uses Jaxb to unmarshall the given file
     *
     * @param fileName file to load
     *
     * @return Model or null
     * @throws IOException
     */
    private static Model unmarshallFile(final String fileName) throws IOException {
        try (InputStream in = MetaModelFactory.class.getClassLoader().getResource(fileName).openStream()) {

            // create an Unmarshaller
            final Unmarshaller u = getJAXBContext().createUnmarshaller();

            // unmarshall a Model instance document into a tree of Java content
            // objects composed of classes from the org.ivoa.metamodel package :
            return (Model) u.unmarshal(in);

        } catch (final JAXBException je) {
            log.error("MetaModelFactory.unmarshallFile : JAXB Failure : ", je);
        } catch (final RuntimeException re) {
            log.error("MetaModelFactory.unmarshallFile : Runtime Failure : ", re);
        }

        return null;
    }

    /**
     * Loads an Xml Model instance
     *
     * @param file model to load
     * @return Model
     * @throws IOException
     * @throws IllegalStateException if the model can not be loaded, unmarshalled or is empty
     */
    private static Model loadModel(final String file) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("loadModel : file : " + file);
        }
        Model model = unmarshallFile(file);

        if (model == null) {
            throw new IllegalStateException("Unable to load the model : " + file);
        }
        // check packages :
        if (model.getPackage().isEmpty()) {
            throw new IllegalStateException("Unable to get any package from the loaded model : " + file);
        }

        if (log.isTraceEnabled()) {
            log.trace("loadModel : exit");
        }
        return model;
    }

    /**
     * Returns dataTypes in the model
     *
     * @return dataTypes in the model
     */
    public Map<String, DataType> getDataTypes() {
        return dataTypes;
    }

    /**
     * Returns a dataType for the given name
     *
     * @param name lookup criteria
     *
     * @return dataType or null if not found
     */
    public DataType getDataType(final String name) {
        return getDataTypes().get(name);
    }

    /**
     * Returns enumerations in the model
     *
     * @return enumerations in the model
     */
    public Map<String, Enumeration> getEnumerations() {
        return enumerations;
    }

    /**
     * Returns an enumeration for the given name
     *
     * @param name lookup criteria
     *
     * @return enumeration or null if not found
     */
    public Enumeration getEnumeration(final String name) {
        return getEnumerations().get(name);
    }

    /**
     * Returns objectTypes in the model
     *
     * @return objectTypes in the model
     */
    public Map<String, ObjectType> getObjectTypes() {
        return objectTypes;
    }

    /**
     * Returns an objectType for the given name
     *
     * @param name lookup criteria
     *
     * @return objectType or null if not found
     */
    public ObjectType getObjectType(final String name) {
        return getObjectTypes().get(name);
    }

    /**
     * Returns classTypes in the model
     *
     * @return classTypes in the model
     */
    public Map<String, ClassType> getClassTypes() {
        return classTypes;
    }

    /**
     * Returns a classType for the given name
     *
     * @param name lookup criteria
     *
     * @return classType or null if not found
     */
    public ClassType getClassType(final String name) {
        return getClassTypes().get(name);
    }

    /**
     * Returns objectClassTypes in the model
     *
     * @return objectClassTypes in the model
     */
    public Map<String, ObjectClassType> getObjectClassTypes() {
        return objectClassTypes;
    }

    /**
     * Returns objectClassTypes as an ordered list
     *
     * @return objectClassTypes in the model
     */
    public Collection<ObjectClassType> getObjectClassTypeList() {
        return objectClassTypes.values();
    }

    /**
     * Returns a classType for the given name
     *
     * @param name lookup criteria
     *
     * @return classType or null if not found
     */
    public ObjectClassType getObjectClassType(final String name) {
        return getObjectClassTypes().get(name);
    }

    /**
     * Returns a classType for the given name
     *
     * @param type class lookup criteria
     *
     * @return classType or null if not found
     */
    public ObjectClassType getObjectClassType(final Class<?> type) {
        return getObjectClassTypes().get(type.getSimpleName());
    }

    /**
     * Returns primitiveTypes in the model
     *
     * @return primitiveTypes in the model
     */
    public Map<String, PrimitiveType> getPrimitiveTypes() {
        return primitiveTypes;
    }

    /**
     * Returns a primitiveType for the given name
     *
     * @param name lookup criteria
     *
     * @return primitiveType or null if not found
     */
    public PrimitiveType getPrimitiveType(final String name) {
        return getPrimitiveTypes().get(name);
    }

    /**
     * Returns classes in the model
     *
     * @return classes in the model
     */
    public Map<String, Class<? extends MetadataElement>> getClasses() {
        return classes;
    }

    /**
     * Returns a class for the given name
     *
     * @param name lookup criteria
     *
     * @return class or null if not found
     */
    public Class<? extends MetadataElement> getClass(final String name) {
        return getClasses().get(name);
    }
}
// ~ End of file
// --------------------------------------------------------------------------------------------------------
