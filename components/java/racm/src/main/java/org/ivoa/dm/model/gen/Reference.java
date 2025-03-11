
package org.ivoa.dm.model.gen;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *         This class defines a reference (close to Identity class) as it contains both xmlId 
 *             and ivoId and publisherDID identifiers : <br/>
 *         It also allows one to indirectly reference fragments of objects using a nested referecne
 *         that follows the containment hierarchy upwards.
 *       
 * 
 * <p>Java class for Reference complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Reference">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="ivoId" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="publisherDID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="xmlId" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Reference")
public class Reference {

    @XmlAttribute(name = "ivoId")
    @XmlSchemaType(name = "anyURI")
    protected String ivoId;
    @XmlAttribute(name = "publisherDID")
    protected String publisherDID;
    @XmlAttribute(name = "xmlId")
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object xmlId;

    /**
     * Gets the value of the ivoId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIvoId() {
        return ivoId;
    }

    /**
     * Sets the value of the ivoId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIvoId(String value) {
        this.ivoId = value;
    }

    /**
     * Gets the value of the publisherDID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublisherDID() {
        return publisherDID;
    }

    /**
     * Sets the value of the publisherDID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublisherDID(String value) {
        this.publisherDID = value;
    }

    /**
     * Gets the value of the xmlId property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getXmlId() {
        return xmlId;
    }

    /**
     * Sets the value of the xmlId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setXmlId(Object value) {
        this.xmlId = value;
    }

}
