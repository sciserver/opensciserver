
package org.ivoa.metamodel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Package complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Package">
 *   &lt;complexContent>
 *     &lt;extension base="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Element">
 *       &lt;sequence>
 *         &lt;element name="depends" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}PackageReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="objectType" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}ObjectType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="dataType" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}DataType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="enumeration" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Enumeration" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="primitiveType" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}PrimitiveType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="package" type="{https://github.com/glemson/vo-urp/xsd/vo-urp/v0.1}Package" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Package", propOrder = {
    "depends",
    "objectType",
    "dataType",
    "enumeration",
    "primitiveType",
    "_package"
})
public class Package
    extends Element
{

    protected List<PackageReference> depends;
    protected List<ObjectType> objectType;
    protected List<DataType> dataType;
    protected List<Enumeration> enumeration;
    protected List<PrimitiveType> primitiveType;
    @XmlElement(name = "package")
    protected List<Package> _package;

    /**
     * Gets the value of the depends property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the depends property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDepends().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PackageReference }
     * 
     * 
     */
    public List<PackageReference> getDepends() {
        if (depends == null) {
            depends = new ArrayList<PackageReference>();
        }
        return this.depends;
    }

    /**
     * Gets the value of the objectType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the objectType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getObjectType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ObjectType }
     * 
     * 
     */
    public List<ObjectType> getObjectType() {
        if (objectType == null) {
            objectType = new ArrayList<ObjectType>();
        }
        return this.objectType;
    }

    /**
     * Gets the value of the dataType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataType }
     * 
     * 
     */
    public List<DataType> getDataType() {
        if (dataType == null) {
            dataType = new ArrayList<DataType>();
        }
        return this.dataType;
    }

    /**
     * Gets the value of the enumeration property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the enumeration property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEnumeration().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Enumeration }
     * 
     * 
     */
    public List<Enumeration> getEnumeration() {
        if (enumeration == null) {
            enumeration = new ArrayList<Enumeration>();
        }
        return this.enumeration;
    }

    /**
     * Gets the value of the primitiveType property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the primitiveType property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrimitiveType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PrimitiveType }
     * 
     * 
     */
    public List<PrimitiveType> getPrimitiveType() {
        if (primitiveType == null) {
            primitiveType = new ArrayList<PrimitiveType>();
        }
        return this.primitiveType;
    }

    /**
     * Gets the value of the package property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the package property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPackage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Package }
     * 
     * 
     */
    public List<Package> getPackage() {
        if (_package == null) {
            _package = new ArrayList<Package>();
        }
        return this._package;
    }

}
