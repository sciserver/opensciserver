package org.sciserver.racm.ugm.model;

/**
 * GL: until alternative implemetation nIt is imperative that this class is
 * exactly inn synch with its counterpart  in the VO-URP classes.
 */
public enum MemberStatus {

	INVITED("INVITED"), ACCEPTED("ACCEPTED"), DECLINED("DECLINED"), WITHDRAWN("WITHDRAWN"), OWNER("OWNER");
	
	/** string representation */
	private final String value;

	/**
	 * Creates a new MemberStatus Enumeration Literal
	 *
	 * @param v
	 *            string representation
	 */
	MemberStatus(final String v) {
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
	 * Return the MemberStatus enum constant corresponding to the given string
	 * representation (value)
	 *
	 * @param v
	 *            string representation (value)
	 *
	 * @return MemberStatus enum constant
	 *
	 * @throws IllegalArgumentException
	 *             if there is no matching enum constant
	 */
	public final static MemberStatus fromValue(final String v) {
		for (MemberStatus c : MemberStatus.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException("MemberStatus.fromValue : No enum const for the value : " + v);
	}

}
