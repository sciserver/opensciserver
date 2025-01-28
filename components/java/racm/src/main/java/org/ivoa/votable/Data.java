
package org.ivoa.votable;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *     Added in Version 1.2: INFO for diagnostics
 *   
 * 
 * <p>Java class for Data complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Data">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="TABLEDATA" type="{http://www.ivoa.net/xml/VOTable/v1.2}TableData"/>
 *           &lt;element name="BINARY" type="{http://www.ivoa.net/xml/VOTable/v1.2}Binary"/>
 *           &lt;element name="FITS" type="{http://www.ivoa.net/xml/VOTable/v1.2}FITS"/>
 *         &lt;/choice>
 *         &lt;element name="INFO" type="{http://www.ivoa.net/xml/VOTable/v1.2}Info" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Data", propOrder = {
    "fits",
    "binary",
    "tabledata",
    "infos"
})
public class Data {

    @XmlElement(name = "FITS")
    protected FITS fits;
    @XmlElement(name = "BINARY")
    protected Binary binary;
    @XmlElement(name = "TABLEDATA")
    protected TableData tabledata;
    @XmlElement(name = "INFO")
    protected List<Info> infos;

    /**
     * Gets the value of the fits property.
     * 
     * @return
     *     possible object is
     *     {@link FITS }
     *     
     */
    public FITS getFITS() {
        return fits;
    }

    /**
     * Sets the value of the fits property.
     * 
     * @param value
     *     allowed object is
     *     {@link FITS }
     *     
     */
    public void setFITS(FITS value) {
        this.fits = value;
    }

    /**
     * Gets the value of the binary property.
     * 
     * @return
     *     possible object is
     *     {@link Binary }
     *     
     */
    public Binary getBINARY() {
        return binary;
    }

    /**
     * Sets the value of the binary property.
     * 
     * @param value
     *     allowed object is
     *     {@link Binary }
     *     
     */
    public void setBINARY(Binary value) {
        this.binary = value;
    }

    /**
     * Gets the value of the tabledata property.
     * 
     * @return
     *     possible object is
     *     {@link TableData }
     *     
     */
    public TableData getTABLEDATA() {
        return tabledata;
    }

    /**
     * Sets the value of the tabledata property.
     * 
     * @param value
     *     allowed object is
     *     {@link TableData }
     *     
     */
    public void setTABLEDATA(TableData value) {
        this.tabledata = value;
    }

    /**
     * Gets the value of the infos property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the infos property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getINFOS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Info }
     * 
     * 
     */
    public List<Info> getINFOS() {
        if (infos == null) {
            infos = new ArrayList<Info>();
        }
        return this.infos;
    }

}
