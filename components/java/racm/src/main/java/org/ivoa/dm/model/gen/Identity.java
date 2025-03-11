
package org.ivoa.dm.model.gen;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *         This class contains all flavor for objectType identifiers : <br/>
 *           - primary key value : numeric <br/>
 *           - string id : string for XML ID / IDREF standard mechanism in xml schemas <br/>
 *           - string URI : URI format for external references
 *       
 * 
 * <p>Java class for Identity complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Identity">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}long" />
 *       &lt;attribute name="ivoId" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="xmlId" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="publisherDID" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Identity")
public class Identity {

    @XmlAttribute(name = "id")
    protected Long id;
    @XmlAttribute(name = "ivoId")
    @XmlSchemaType(name = "anyURI")
    protected String ivoId;
    @XmlAttribute(name = "xmlId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String xmlId;
    @XmlAttribute(name = "publisherDID")
    protected String publisherDID;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setId(Long value) {
        this.id = value;
    }

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
     * Gets the value of the xmlId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXmlId() {
        return xmlId;
    }

    /**
     * Sets the value of the xmlId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXmlId(String value) {
        this.xmlId = value;
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

}
