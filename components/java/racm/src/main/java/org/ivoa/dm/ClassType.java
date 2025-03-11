package org.ivoa.dm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ivoa.bean.LogSupport;
import org.ivoa.metamodel.Attribute;
import org.ivoa.metamodel.DataType;
import org.ivoa.metamodel.Type;
import org.ivoa.metamodel.TypeRef;

/**
 * ClassType represents a java class (in memory) corresponding to the metamodel for an UML DataType. This class is used
 * to find directly all elements inside an inheritance hierarchy
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public class ClassType extends LogSupport {
    // ~ Members
    // ----------------------------------------------------------------------------------------------------------

    /** wrapped type */
    protected final Type type;
    /** all DataType attributes ordered by the class hierarchy */
    private Map<String, Attribute> attributes = null;

    // ~ Constructors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Constructor for a given type
     *
     * @param pType to wrap
     */
    ClassType(final Type pType) {
        this.type = pType;
    }

    // ~ Methods
    // ----------------------------------------------------------------------------------------------------------

    /**
     * TODO : Method Description
     */
    void init(Map<String, DataType> dataTypes) {
        if (log.isTraceEnabled()) {
            log.trace("ClassType.init : enter : " + type.getName());
        }

        process(getDataType(), dataTypes);

        if (log.isTraceEnabled()) {
            log.trace("ClassType.init : exit : " + type.getName() + " :\n" + toString());
        }
    }

    /**
     * TODO : Method Description
     *
     * @param t
     */
    private void process(final DataType t, Map<String, DataType> dataTypes) {
        if (log.isTraceEnabled()) {
            log.trace("ClassType.process : enter : " + t.getName());
        }

        // parent identityType definition :
        final TypeRef parentTypeRef = t.getExtends();

        if (parentTypeRef != null) {
            if (log.isTraceEnabled()) {
                log.trace("ClassType.process : find definition for : " + parentTypeRef.getName());
            }

            final DataType parentType = dataTypes.get(parentTypeRef.getName());

            if (parentType != null) {
                // go up in inheritance hierarchy and later down :
                process(parentType, dataTypes);
            }
        }

        String name;

        // check collection to prepare local collection :
        if (t.getAttribute().size() > 0) {
            lazyAttributes();

            // navigate through attributes :
            for (final Attribute a : t.getAttribute()) {
                name = a.getName();

                // attribute can be overridden for a given name :
                getAttributes().put(name, a);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("ClassType.process : exit : " + t.getName());
        }
    }

    /**
     * Returns a string representation : creates a temporary StringBuilder(STRING_BUFFER_CAPACITY) and calls
     * #toString(java.lang.StringBuilder) method
     *
     * @return string representation
     *
     * @see #toString(java.lang.StringBuilder) method
     */
    @Override
    public final String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Puts the string representation in the given string buffer : NO DEEP toString(java.lang.StringBuilder, boolean)
     * recursion
     *
     * @param sb given string buffer to fill
     *
     * @return the given string buffer filled with the string representation
     */
    public StringBuilder toString(final StringBuilder sb) {
        sb.append("ClassType[");
        sb.append(getDataType().getName());
        sb.append("]={");

        if (isHasAttributes()) {
            sb.append("attributes={");

            for (final String name : getAttributes().keySet()) {
                sb.append(name).append(" ");
            }

            sb.append("}");
        }

        return sb;
    }

    // --- getters -----
    /**
     * Returns the wrapped UML type
     *
     * @return wrapped UML type
     */
    public final Type getType() {
        return type;
    }

    /**
     * Returns the wrapped UML datatype
     *
     * @return wrapped UML datatype
     */
    public final DataType getDataType() {
        return (DataType) type;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public final boolean isHasAttributes() {
        return attributes != null;
    }

    /**
     * TODO : Method Description
     */
    protected void lazyAttributes() {
        if (getAttributes() == null) {
            this.attributes = new LinkedHashMap<>();
        }
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public final Map<String, Attribute> getAttributes() {
        return attributes;
    }

    /**
     * TODO : Method Description
     *
     * @return value TODO : Value Description
     */
    public final Collection<Attribute> getAttributeList() {
        return attributes.values();
    }
}
//~ End of file --------------------------------------------------------------------------------------------------------
