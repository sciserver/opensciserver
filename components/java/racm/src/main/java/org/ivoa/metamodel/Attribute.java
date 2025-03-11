
package org.ivoa.metamodel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Attribute complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Attribute">
 *   &lt;complexContent>
 *     &lt;extension base="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Element">
 *       &lt;sequence>
 *         &lt;element name="datatype" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}TypeRef"/>
 *         &lt;element name="multiplicity" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Multiplicity"/>
 *         &lt;element name="constraints" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Constraints" minOccurs="0"/>
 *         &lt;element name="skosconcept" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}SKOSConcept" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Attribute", propOrder = {
    "datatype",
    "multiplicity",
    "constraints",
    "skosconcept"
})
public class Attribute
    extends Element
{

    @XmlElement(required = true)
    protected TypeRef datatype;
    @XmlElement(required = true)
    protected String multiplicity;
    protected Constraints constraints;
    protected SKOSConcept skosconcept;

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
     * Gets the value of the constraints property.
     * 
     * @return
     *     possible object is
     *     {@link Constraints }
     *     
     */
    public Constraints getConstraints() {
        return constraints;
    }

    /**
     * Sets the value of the constraints property.
     * 
     * @param value
     *     allowed object is
     *     {@link Constraints }
     *     
     */
    public void setConstraints(Constraints value) {
        this.constraints = value;
    }

    /**
     * Gets the value of the skosconcept property.
     * 
     * @return
     *     possible object is
     *     {@link SKOSConcept }
     *     
     */
    public SKOSConcept getSkosconcept() {
        return skosconcept;
    }

    /**
     * Sets the value of the skosconcept property.
     * 
     * @param value
     *     allowed object is
     *     {@link SKOSConcept }
     *     
     */
    public void setSkosconcept(SKOSConcept value) {
        this.skosconcept = value;
    }

}
