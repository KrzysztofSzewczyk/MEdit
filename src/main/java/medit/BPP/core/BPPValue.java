
package medit.BPP.core;

public class BPPValue extends BPPObject {

	@SuppressWarnings("rawtypes")
	private Class type = null;
	private long value = 0;

	@SuppressWarnings("rawtypes")
	public Class getType() {
		if (this.type != null)
			return this.type;
		return long.class;
	}

	public long getValue() {
		return this.value;
	}

	public void setType(@SuppressWarnings("rawtypes") final Class type) {
		this.type = type;
	}

	public void setValue(final long value) {
		this.value = value;
	}
}
