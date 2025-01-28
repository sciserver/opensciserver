package org.ivoa.dm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.ivoa.metamodel.Attribute;
import org.ivoa.metamodel.Collection;
import org.ivoa.metamodel.DataType;
import org.ivoa.metamodel.ObjectType;
import org.ivoa.metamodel.Reference;
import org.ivoa.metamodel.TypeRef;

/**
 * ObjectClassType represents a java class (in memory) corresponding to an ObjectType. This class flattens the
 * inheritance hierarchy to know which attributes, references and collections are present in this element. Wrapper
 * pattern
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class ObjectClassType extends ClassType {
    // ~ Constants
    // --------------------------------------------------------------------------------------------------------

    /** ID Attribute */
    private static Attribute ID_ATTRIBUTE = null;
    /** Identity Attribute */
    private static Attribute IDENTITY_ATTRIBUTE = null;
    /** Identity Identifier */
    private static final String IDENTITY_NAME = "identity";

    // ~ Members
    // ----------------------------------------------------------------------------------------------------------

    /** flag to indicate if that element is a root element */
    private final boolean root;
    /** Name of base class */
    private final String baseclass;
    /** flag to indicate if that element is contained == belongs to a collection only */
    private boolean contained = false;
    /** all objectType references ordered by class hierarchy, keyed by name */
    private final Map<String, Reference> references = new LinkedHashMap<>();
    /** all collections ordered by class hierarchy, keyed by name */
    private final Map<String, Collection> collections = new LinkedHashMap<>();
    /** all subclasses of this class, keyed by name */
    private final LinkedHashMap<String, ObjectClassType> subclasses = new LinkedHashMap<>();
    /** all referrer (entity name), keyed by referrer's property name */
    private final LinkedHashMap<String, String> referrers = new LinkedHashMap<>();

    // ~ Constructors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param pType wrapped ObjectType
     */
    public ObjectClassType(final ObjectType pType) {
        super(pType);
        this.root = pType.isEntity();
        this.baseclass = (pType.getExtends() == null) ? null : pType.getExtends().getName();
    }

    // ~ Methods
    // ----------------------------------------------------------------------------------------------------------

    /**
     * MetaModelFactory calls this method to define IdentityType
     *
     * @param identityType identity UML DataType
     */
    protected static void doInitIdentity(final DataType identityType) {
        if (identityType == null) {
            log.error("ObjectClassType : identity Type is not defined !");

            return;
        }

        ID_ATTRIBUTE = identityType.getAttribute().get(0);

        final TypeRef identityRef = new TypeRef();

        identityRef.setName(identityType.getName());
        identityRef.setXmiidref(identityType.getXmiid());

        IDENTITY_ATTRIBUTE = new Attribute();
        IDENTITY_ATTRIBUTE.setDatatype(identityRef);
        IDENTITY_ATTRIBUTE.setName(IDENTITY_NAME);
        IDENTITY_ATTRIBUTE.setDescription("Identity : all flavor for identifiers");
    }

    /**
     * TODO : Method Description
     */
    public void initObjectClassType(Map<String, ObjectType> objectTypes) {
        if (log.isTraceEnabled()) {
            log.trace("ObjectClassType.init : enter : " + type.getName());
        }

        lazyAttributes();

        // Adds ID attribute :
        getAttributes().put(ID_ATTRIBUTE.getName(), ID_ATTRIBUTE);

        // Adds Identity attribute :
        getAttributes().put(IDENTITY_ATTRIBUTE.getName(), IDENTITY_ATTRIBUTE);

        this.process(getObjectType(), objectTypes);

        if (log.isTraceEnabled()) {
            log.trace("ObjectClassType.init : exit : " + type.getName() + " :\n" + toString());
        }
    }

    /**
     * TODO : Method Description
     *
     * @param ot
     */
    private void process(final ObjectType ot, Map<String, ObjectType> objectTypes) {
        if (log.isTraceEnabled()) {
            log.trace("ObjectClassType.process : enter : " + ot.getName());
        }

        // parent identityType definition :
        final TypeRef parentTypeRef = ot.getExtends();

        if (parentTypeRef != null) {
            if (log.isTraceEnabled()) {
                log.trace("ObjectClassType.process : find definition for : " + parentTypeRef.getName());
            }

            final ObjectType parentType = objectTypes.get(parentTypeRef.getName());

            if (parentType != null) {
                // go up in inheritance hierarchy and later down :
                this.process(parentType, objectTypes);
            }
        }

        // set contained flag according to the container TypeRef :
        if (!isContained() && (ot.getContainer() != null)) {
            setContained();
        }

        // set referrers (property name & class name) :
        for (final TypeRef ref : ot.getReferrer()) {
            addReferrer(ref.getName(), ref.getRelation());
        }

        String name;

        if (ot.getAttribute().size() > 0) {
            lazyAttributes();

            // navigate through attributes :
            for (final Attribute a : ot.getAttribute()) {
                name = a.getName();

                // attribute can be overridden for a given name :
                getAttributes().put(name, a);
            }
        }

        // navigate through references :
        for (final Reference r : ot.getReference()) {
            name = r.getName();

            // reference can be overridden for a given name :
            getReferences().put(name, r);
        }

        // navigate through collections :
        for (final Collection c : ot.getCollection()) {
            name = c.getName();

            // reference can be overridden for a given name :
            getCollections().put(name, c);
        }

        if (log.isTraceEnabled()) {
            log.trace("ObjectClassType.process : exit : " + ot.getName());
        }
    }

    /**
     * Sets
     */
    private void setContained() {
        if (log.isTraceEnabled()) {
            log.trace("ObjectClassType.setContained : " + this.getType().getName());
        }

        this.contained = true;
    }

    /**
     * Puts the string representation in the given string buffer : NO DEEP toString(java.lang.StringBuilder, boolean)
     * recursion
     *
     * @param sb given string buffer to fill
     *
     * @return the given string buffer filled with the string representation
     */
    @Override
    public final StringBuilder toString(final StringBuilder sb) {
        sb.append("ObjectClassType[");
        sb.append(getObjectType().getName());
        sb.append("]={");

        sb.append("isRoot=");
        sb.append(isRoot());

        sb.append(" | ");
        sb.append("hasContainer=");
        sb.append(isContained());

        if (isHasAttributes()) {
            sb.append(" | attributes={");

            for (final String name : getAttributes().keySet()) {
                sb.append(name).append(" ");
            }

            sb.append("}");
        }

        if (isHasReferences()) {
            sb.append(" | references={");

            for (final String name : getReferences().keySet()) {
                sb.append(name).append(" ");
            }

            sb.append("}");
        }

        if (isHasCollections()) {
            sb.append(" | collections={");

            for (final String name : getCollections().keySet()) {
                sb.append(name).append(" ");
            }

            sb.append("}");
        }

        return sb;
    }

    // --- getters -----
    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public ObjectType getObjectType() {
        return (ObjectType) type;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public boolean isContained() {
        return contained;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public boolean isRoot() {
        return root;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public boolean isHasReferences() {
        return references.size() > 0;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public Map<String, Reference> getReferences() {
        return references;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public final java.util.Collection<Reference> getReferenceList() {
        return references.values();
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public boolean isHasCollections() {
        return collections.size() > 0;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public Map<String, Collection> getCollections() {
        return collections;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public final java.util.Collection<Collection> getCollectionList() {
        return collections.values();
    }

    /**
     * Return the attribute with the utype corresponding to the specified value.<br/>
     * If this class does not contains such an attribute, return null.
     * 
     * @param utype
     * @return
     */
    public final Attribute getAttributeForUtype(String utype) {
        for (Attribute atr : this.getAttributeList())
            if (atr.getUtype().equals(utype))
                return atr;
        return null;
    }

    /**
     * Add the specified subclass to this' subclasses collection.<br>
     * Do check though that the subclass's baseclass is this!
     *
     * @param subclass
     */
    protected void addSubclass(final ObjectClassType subclass) {
        if ((subclass.getObjectType().getExtends() != null)
                && getObjectType().getName().equals(subclass.getBaseclass())) {
            subclasses.put(subclass.getObjectType().getName(), subclass);
        }
    }

    /**
     * Add the specified referrer to this' referrerss collection.<br>
     *
     * @param referrer the referring object type
     * @param reference the name of the reference to this
     */
    private void addReferrer(final String referrer, final String reference) {
        referrers.put(reference, referrer);
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public boolean isHasReferrers() {
        return referrers.size() > 0;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public Map<String, String> getReferrers() {
        return referrers;
    }

    /**
     * Return the list of subclasses
     *
     * @return list of subclasses
     */
    public java.util.Collection<ObjectClassType> getSubclassesList() {
        return subclasses.values();
    }

    /**
     * Return name of base class, null if has no base class
     *
     * @return base class, null if has no base class
     */
    public String getBaseclass() {
        return baseclass;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public java.util.Collection<ObjectClassType> getSubclasses() {
        return subclasses.values();
    }
}
//~ End of file --------------------------------------------------------------------------------------------------------
