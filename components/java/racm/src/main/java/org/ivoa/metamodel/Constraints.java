
package org.ivoa.metamodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Constraints complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Constraints">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="minLength" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="maxLength" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="length" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="uniqueGlobally" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="uniqueInCollection" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Constraints", propOrder = {
    "minLengthOrMaxLengthOrLength"
})
public class Constraints {

    @XmlElementRefs({
        @XmlElementRef(name = "minLength", type = JAXBElement.class),
        @XmlElementRef(name = "length", type = JAXBElement.class),
        @XmlElementRef(name = "uniqueInCollection", type = JAXBElement.class),
        @XmlElementRef(name = "maxLength", type = JAXBElement.class),
        @XmlElementRef(name = "uniqueGlobally", type = JAXBElement.class)
    })
    protected List<JAXBElement<? extends Serializable>> minLengthOrMaxLengthOrLength;

    /**
     * Gets the value of the minLengthOrMaxLengthOrLength property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the minLengthOrMaxLengthOrLength property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMinLengthOrMaxLengthOrLength().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Integer }{@code >}
     * {@link JAXBElement }{@code <}{@link Integer }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link Integer }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends Serializable>> getMinLengthOrMaxLengthOrLength() {
        if (minLengthOrMaxLengthOrLength == null) {
            minLengthOrMaxLengthOrLength = new ArrayList<JAXBElement<? extends Serializable>>();
        }
        return this.minLengthOrMaxLengthOrLength;
    }

}
