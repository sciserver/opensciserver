
package org.ivoa.votable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *     Deprecated in Version 1.2
 *   
 * 
 * <p>Java class for CoordinateSystem complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CoordinateSystem">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="ID" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="equinox" type="{http://www.ivoa.net/xml/VOTable/v1.2}astroYear" />
 *       &lt;attribute name="epoch" type="{http://www.ivoa.net/xml/VOTable/v1.2}astroYear" />
 *       &lt;attribute name="system" default="eq_FK5">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *             &lt;enumeration value="eq_FK4"/>
 *             &lt;enumeration value="eq_FK5"/>
 *             &lt;enumeration value="ICRS"/>
 *             &lt;enumeration value="ecl_FK4"/>
 *             &lt;enumeration value="ecl_FK5"/>
 *             &lt;enumeration value="galactic"/>
 *             &lt;enumeration value="supergalactic"/>
 *             &lt;enumeration value="xy"/>
 *             &lt;enumeration value="barycentric"/>
 *             &lt;enumeration value="geo_app"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CoordinateSystem", propOrder = {
    "value"
})
public class CoordinateSystem {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "ID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "equinox")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String equinox;
    @XmlAttribute(name = "epoch")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String epoch;
    @XmlAttribute(name = "system")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String system;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the equinox property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEquinox() {
        return equinox;
    }

    /**
     * Sets the value of the equinox property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEquinox(String value) {
        this.equinox = value;
    }

    /**
     * Gets the value of the epoch property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEpoch() {
        return epoch;
    }

    /**
     * Sets the value of the epoch property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEpoch(String value) {
        this.epoch = value;
    }

    /**
     * Gets the value of the system property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSystem() {
        if (system == null) {
            return "eq_FK5";
        } else {
            return system;
        }
    }

    /**
     * Sets the value of the system property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSystem(String value) {
        this.system = value;
    }

}
