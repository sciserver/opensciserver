package org.sciserver.racm.ugm.model;

/**
 * GL: until alternative implemetation nIt is imperative that this class is
 * exactly inn synch with its counterpart in the VO-URP classes.
 */
public enum GroupRole {

	MEMBER("MEMBER"), ADMIN("ADMIN"), OWNER("OWNER");

	/** string representation */
	private final String value;

	/**
	 * Creates a new GroupRole Enumeration Literal
	 *
	 * @param v
	 *            string representation
	 */
	GroupRole(final String v) {
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
	 * Return the GroupRole enum constant corresponding to the given string
	 * representation (value)
	 *
	 * @param v
	 *            string representation (value)
	 *
	 * @return GroupRole enum constant
	 *
	 * @throws IllegalArgumentException
	 *             if there is no matching enum constant
	 */
	public final static GroupRole fromValue(final String v) {
		for (GroupRole c : GroupRole.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException("GroupRole.fromValue : No enum const for the value : " + v);
	}

}
