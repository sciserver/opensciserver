
package org.ivoa.votable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *     Added in Version 1.2: INFO for diagnostics
 *   
 * 
 * <p>Java class for Table complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Table">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DESCRIPTION" type="{http://www.ivoa.net/xml/VOTable/v1.2}anyTEXT" minOccurs="0"/>
 *         &lt;element name="INFO" type="{http://www.ivoa.net/xml/VOTable/v1.2}Info" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;choice maxOccurs="unbounded">
 *           &lt;element name="FIELD" type="{http://www.ivoa.net/xml/VOTable/v1.2}Field"/>
 *           &lt;element name="PARAM" type="{http://www.ivoa.net/xml/VOTable/v1.2}Param"/>
 *           &lt;element name="GROUP" type="{http://www.ivoa.net/xml/VOTable/v1.2}Group"/>
 *         &lt;/choice>
 *         &lt;element name="LINK" type="{http://www.ivoa.net/xml/VOTable/v1.2}Link" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="DATA" type="{http://www.ivoa.net/xml/VOTable/v1.2}Data" minOccurs="0"/>
 *         &lt;element name="INFO" type="{http://www.ivoa.net/xml/VOTable/v1.2}Info" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ID" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="ucd" type="{http://www.ivoa.net/xml/VOTable/v1.2}ucdType" />
 *       &lt;attribute name="utype" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="nrows" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Table", propOrder = {
    "description",
    "infosAndFIELDSAndPARAMS"
})
public class Table {

    @XmlElement(name = "DESCRIPTION")
    protected AnyTEXT description;
    @XmlElements({
        @XmlElement(name = "LINK", type = Link.class),
        @XmlElement(name = "DATA", type = Data.class),
        @XmlElement(name = "GROUP", type = Group.class),
        @XmlElement(name = "FIELD", type = Field.class),
        @XmlElement(name = "PARAM", type = Param.class),
        @XmlElement(name = "INFO", type = Info.class)
    })
    protected List<Object> infosAndFIELDSAndPARAMS;
    @XmlAttribute(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "name")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String name;
    @XmlAttribute(name = "ref")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object ref;
    @XmlAttribute(name = "ucd")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String ucd;
    @XmlAttribute(name = "utype")
    protected String utype;
    @XmlAttribute(name = "nrows")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nrows;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link AnyTEXT }
     *     
     */
    public AnyTEXT getDESCRIPTION() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link AnyTEXT }
     *     
     */
    public void setDESCRIPTION(AnyTEXT value) {
        this.description = value;
    }

    /**
     * Gets the value of the infosAndFIELDSAndPARAMS property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the infosAndFIELDSAndPARAMS property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getINFOSAndFIELDSAndPARAMS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Link }
     * {@link Data }
     * {@link Group }
     * {@link Field }
     * {@link Param }
     * {@link Info }
     * 
     * 
     */
    public List<Object> getINFOSAndFIELDSAndPARAMS() {
        if (infosAndFIELDSAndPARAMS == null) {
            infosAndFIELDSAndPARAMS = new ArrayList<Object>();
        }
        return this.infosAndFIELDSAndPARAMS;
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
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the ref property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getRef() {
        return ref;
    }

    /**
     * Sets the value of the ref property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setRef(Object value) {
        this.ref = value;
    }

    /**
     * Gets the value of the ucd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUcd() {
        return ucd;
    }

    /**
     * Sets the value of the ucd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUcd(String value) {
        this.ucd = value;
    }

    /**
     * Gets the value of the utype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtype() {
        return utype;
    }

    /**
     * Sets the value of the utype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtype(String value) {
        this.utype = value;
    }

    /**
     * Gets the value of the nrows property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNrows() {
        return nrows;
    }

    /**
     * Sets the value of the nrows property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNrows(BigInteger value) {
        this.nrows = value;
    }

}
