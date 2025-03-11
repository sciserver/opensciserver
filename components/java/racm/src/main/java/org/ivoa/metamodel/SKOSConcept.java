
package org.ivoa.metamodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SKOSConcept complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SKOSConcept">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="broadestSKOSConcept" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *         &lt;element name="vocabularyURI" type="{http://www.w3.org/2001/XMLSchema}anyURI" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SKOSConcept", propOrder = {
    "broadestSKOSConcept",
    "vocabularyURI"
})
public class SKOSConcept {

    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String broadestSKOSConcept;
    @XmlSchemaType(name = "anyURI")
    protected List<String> vocabularyURI;

    /**
     * Gets the value of the broadestSKOSConcept property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBroadestSKOSConcept() {
        return broadestSKOSConcept;
    }

    /**
     * Sets the value of the broadestSKOSConcept property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBroadestSKOSConcept(String value) {
        this.broadestSKOSConcept = value;
    }

    /**
     * Gets the value of the vocabularyURI property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the vocabularyURI property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVocabularyURI().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getVocabularyURI() {
        if (vocabularyURI == null) {
            vocabularyURI = new ArrayList<String>();
        }
        return this.vocabularyURI;
    }

}
