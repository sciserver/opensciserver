
package org.ivoa.votable;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *     Deprecated in Version 1.1
 *   
 * 
 * <p>Java class for Definitions complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Definitions">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="COOSYS" type="{http://www.ivoa.net/xml/VOTable/v1.2}CoordinateSystem"/>
 *         &lt;element name="PARAM" type="{http://www.ivoa.net/xml/VOTable/v1.2}Param"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Definitions", propOrder = {
    "coosysAndPARAMS"
})
public class Definitions {

    @XmlElements({
        @XmlElement(name = "COOSYS", type = CoordinateSystem.class),
        @XmlElement(name = "PARAM", type = Param.class)
    })
    protected List<Object> coosysAndPARAMS;

    /**
     * Gets the value of the coosysAndPARAMS property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the coosysAndPARAMS property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCOOSYSAndPARAMS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CoordinateSystem }
     * {@link Param }
     * 
     * 
     */
    public List<Object> getCOOSYSAndPARAMS() {
        if (coosysAndPARAMS == null) {
            coosysAndPARAMS = new ArrayList<Object>();
        }
        return this.coosysAndPARAMS;
    }

}
