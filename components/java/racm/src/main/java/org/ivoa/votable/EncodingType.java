
package org.ivoa.votable;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for encodingType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="encodingType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="gzip"/>
 *     &lt;enumeration value="base64"/>
 *     &lt;enumeration value="dynamic"/>
 *     &lt;enumeration value="none"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "encodingType")
@XmlEnum
public enum EncodingType {

    @XmlEnumValue("gzip")
    GZIP("gzip"),
    @XmlEnumValue("base64")
    BASE_64("base64"),
    @XmlEnumValue("dynamic")
    DYNAMIC("dynamic"),
    @XmlEnumValue("none")
    NONE("none");
    private final String value;

    EncodingType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EncodingType fromValue(String v) {
        for (EncodingType c: EncodingType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
