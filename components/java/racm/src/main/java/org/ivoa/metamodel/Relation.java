
package org.ivoa.metamodel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Relation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Relation">
 *   &lt;complexContent>
 *     &lt;extension base="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Element">
 *       &lt;sequence>
 *         &lt;element name="datatype" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}TypeRef"/>
 *         &lt;element name="multiplicity" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Multiplicity"/>
 *         &lt;element name="subsets" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="xmiidref" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Relation", propOrder = {
    "datatype",
    "multiplicity",
    "subsets"
})
@XmlSeeAlso({
    Reference.class,
    Collection.class
})
public abstract class Relation
    extends Element
{

    @XmlElement(required = true)
    protected TypeRef datatype;
    @XmlElement(required = true)
    protected String multiplicity;
    protected String subsets;
    @XmlAttribute(name = "xmiidref")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object xmiidref;

    /**
     * Gets the value of the datatype property.
     * 
     * @return
     *     possible object is
     *     {@link TypeRef }
     *     
     */
    public TypeRef getDatatype() {
        return datatype;
    }

    /**
     * Sets the value of the datatype property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeRef }
     *     
     */
    public void setDatatype(TypeRef value) {
        this.datatype = value;
    }

    /**
     * Gets the value of the multiplicity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMultiplicity() {
        return multiplicity;
    }

    /**
     * Sets the value of the multiplicity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMultiplicity(String value) {
        this.multiplicity = value;
    }

    /**
     * Gets the value of the subsets property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubsets() {
        return subsets;
    }

    /**
     * Sets the value of the subsets property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubsets(String value) {
        this.subsets = value;
    }

    /**
     * Gets the value of the xmiidref property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getXmiidref() {
        return xmiidref;
    }

    /**
     * Sets the value of the xmiidref property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setXmiidref(Object value) {
        this.xmiidref = value;
    }

}
