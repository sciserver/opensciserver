
package org.ivoa.metamodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ObjectType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ObjectType">
 *   &lt;complexContent>
 *     &lt;extension base="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Type">
 *       &lt;sequence>
 *         &lt;element name="container" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}TypeRef" minOccurs="0"/>
 *         &lt;element name="referrer" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}TypeRef" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="attribute" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Attribute" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="collection" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Collection" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="reference" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="entity" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ObjectType", propOrder = {
    "container",
    "referrer",
    "attribute",
    "collection",
    "reference"
})
public class ObjectType
    extends Type
{

    protected TypeRef container;
    protected List<TypeRef> referrer;
    protected List<Attribute> attribute;
    protected List<Collection> collection;
    protected List<Reference> reference;
    @XmlAttribute(name = "entity")
    protected Boolean entity;

    /**
     * Gets the value of the container property.
     * 
     * @return
     *     possible object is
     *     {@link TypeRef }
     *     
     */
    public TypeRef getContainer() {
        return container;
    }

    /**
     * Sets the value of the container property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeRef }
     *     
     */
    public void setContainer(TypeRef value) {
        this.container = value;
    }

    /**
     * Gets the value of the referrer property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the referrer property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReferrer().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TypeRef }
     * 
     * 
     */
    public List<TypeRef> getReferrer() {
        if (referrer == null) {
            referrer = new ArrayList<TypeRef>();
        }
        return this.referrer;
    }

    /**
     * Gets the value of the attribute property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attribute property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttribute().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Attribute }
     * 
     * 
     */
    public List<Attribute> getAttribute() {
        if (attribute == null) {
            attribute = new ArrayList<Attribute>();
        }
        return this.attribute;
    }

    /**
     * Gets the value of the collection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the collection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCollection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Collection }
     * 
     * 
     */
    public List<Collection> getCollection() {
        if (collection == null) {
            collection = new ArrayList<Collection>();
        }
        return this.collection;
    }

    /**
     * Gets the value of the reference property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reference property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReference().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Reference }
     * 
     * 
     */
    public List<Reference> getReference() {
        if (reference == null) {
            reference = new ArrayList<Reference>();
        }
        return this.reference;
    }

    /**
     * Gets the value of the entity property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEntity() {
        if (entity == null) {
            return false;
        } else {
            return entity;
        }
    }

    /**
     * Sets the value of the entity property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEntity(Boolean value) {
        this.entity = value;
    }

}
