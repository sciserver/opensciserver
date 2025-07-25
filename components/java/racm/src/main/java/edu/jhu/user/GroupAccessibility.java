/**
This code was originally automatically generated from the UML model in
https://github.com/sciserver/resource-management/blob/master/vo-urp/RACM_v1.xml
using the VO-URP tool, https://github.com/sciserver/vo-urp. 
It is now included in the code-base and will no longer be 
generated automatically. You can edit this file, but be aware
of its origins when interpreting the code
**/
package edu.jhu.user;

import javax.xml.bind.annotation.*;

/**
 * UML Enumeration GroupAccessibility.
 *
 * 
 * TODO : Missing description : please, update your UML model asap.
 *
 * 
 * @author generated by VO-URP tools VO-URP Home
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public enum GroupAccessibility {

    /**
     * Value PUBLIC :
     * 
     * Groups with PUBLIC visibility can be entered by users.
     */
    PUBLIC("PUBLIC"),

    /**
     * Value PRIVATE :
     * 
     * Groups with PRIVATE vicsibility cannot be entered freely. Users must be invited.
     */
    PRIVATE("PRIVATE"),

    /**
     * Value SYSTEM :
     * 
     * 
     * TODO : Missing description : please, update your UML model asap.
     * 
     */
    SYSTEM("SYSTEM");

    /** string representation */
    private final String value;

    /**
     * Creates a new GroupAccessibility Enumeration Literal
     *
     * @param v string representation
     */
    GroupAccessibility(final String v) {
        value = v;
    }

    /**
     * Return the string representation of this enum constant (value)
     * 
     * @return string representation of this enum constant (value)
     */
    public final String value() {
        return this.value;
    }

    /**
     * Return the string representation of this enum constant (value)
     * 
     * @see #value()
     * @return string representation of this enum constant (value)
     */
    @Override
    public final String toString() {
        return value();
    }

    /**
     * Return the GroupAccessibility enum constant corresponding to the given string representation (value)
     *
     * @param v string representation (value)
     *
     * @return GroupAccessibility enum constant
     *
     * @throws IllegalArgumentException if there is no matching enum constant
     */
    public final static GroupAccessibility fromValue(final String v) {
        for (GroupAccessibility c : GroupAccessibility.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException("GroupAccessibility.fromValue : No enum const for the value : " + v);
    }

}
