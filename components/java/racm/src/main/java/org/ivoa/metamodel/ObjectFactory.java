
package org.ivoa.metamodel;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.ivoa.metamodel package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ConstraintsMinLength_QNAME = new QName("", "minLength");
    private final static QName _ConstraintsLength_QNAME = new QName("", "length");
    private final static QName _ConstraintsUniqueGlobally_QNAME = new QName("", "uniqueGlobally");
    private final static QName _ConstraintsMaxLength_QNAME = new QName("", "maxLength");
    private final static QName _ConstraintsUniqueInCollection_QNAME = new QName("", "uniqueInCollection");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.ivoa.metamodel
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Enumeration }
     * 
     */
    public Enumeration createEnumeration() {
        return new Enumeration();
    }

    /**
     * Create an instance of {@link Model }
     * 
     */
    public Model createModel() {
        return new Model();
    }

    /**
     * Create an instance of {@link Profile }
     * 
     */
    public Profile createProfile() {
        return new Profile();
    }

    /**
     * Create an instance of {@link Package }
     * 
     */
    public Package createPackage() {
        return new Package();
    }

    /**
     * Create an instance of {@link Attribute }
     * 
     */
    public Attribute createAttribute() {
        return new Attribute();
    }

    /**
     * Create an instance of {@link TypeRef }
     * 
     */
    public TypeRef createTypeRef() {
        return new TypeRef();
    }

    /**
     * Create an instance of {@link Reference }
     * 
     */
    public Reference createReference() {
        return new Reference();
    }

    /**
     * Create an instance of {@link ObjectType }
     * 
     */
    public ObjectType createObjectType() {
        return new ObjectType();
    }

    /**
     * Create an instance of {@link Constraints }
     * 
     */
    public Constraints createConstraints() {
        return new Constraints();
    }

    /**
     * Create an instance of {@link DataType }
     * 
     */
    public DataType createDataType() {
        return new DataType();
    }

    /**
     * Create an instance of {@link Collection }
     * 
     */
    public Collection createCollection() {
        return new Collection();
    }

    /**
     * Create an instance of {@link PrimitiveType }
     * 
     */
    public PrimitiveType createPrimitiveType() {
        return new PrimitiveType();
    }

    /**
     * Create an instance of {@link SKOSConcept }
     * 
     */
    public SKOSConcept createSKOSConcept() {
        return new SKOSConcept();
    }

    /**
     * Create an instance of {@link PackageReference }
     * 
     */
    public PackageReference createPackageReference() {
        return new PackageReference();
    }

    /**
     * Create an instance of {@link Enumeration.Literal }
     * 
     */
    public Enumeration.Literal createEnumerationLiteral() {
        return new Enumeration.Literal();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "minLength", scope = Constraints.class)
    public JAXBElement<Integer> createConstraintsMinLength(Integer value) {
        return new JAXBElement<Integer>(_ConstraintsMinLength_QNAME, Integer.class, Constraints.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "length", scope = Constraints.class)
    public JAXBElement<Integer> createConstraintsLength(Integer value) {
        return new JAXBElement<Integer>(_ConstraintsLength_QNAME, Integer.class, Constraints.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "uniqueGlobally", scope = Constraints.class, defaultValue = "false")
    public JAXBElement<Boolean> createConstraintsUniqueGlobally(Boolean value) {
        return new JAXBElement<Boolean>(_ConstraintsUniqueGlobally_QNAME, Boolean.class, Constraints.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "maxLength", scope = Constraints.class)
    public JAXBElement<Integer> createConstraintsMaxLength(Integer value) {
        return new JAXBElement<Integer>(_ConstraintsMaxLength_QNAME, Integer.class, Constraints.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "uniqueInCollection", scope = Constraints.class, defaultValue = "false")
    public JAXBElement<Boolean> createConstraintsUniqueInCollection(Boolean value) {
        return new JAXBElement<Boolean>(_ConstraintsUniqueInCollection_QNAME, Boolean.class, Constraints.class, value);
    }

}
