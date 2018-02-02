
package medit.BPP.core;

public class BPPObject {

	private BPPObject parent = null;

	public BPPObject() {
	}

	public BPPObject(final BPPObject parent) {
		this.parent = parent;
	}

	public BPPObject getParent() {
		return this.parent;
	}
}
