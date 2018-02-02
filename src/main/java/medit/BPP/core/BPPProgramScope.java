
package medit.BPP.core;

import java.util.Hashtable;

public class BPPProgramScope extends BPPObject {

	private final Hashtable<String, BPPVariable> childs = new Hashtable<>();

	public BPPProgramScope(final BPPObject parent) {
		super(parent);
	}

	public BPPVariable child(final String name) {
		return this.child(name, this);
	}

	private BPPVariable child(final String name, final BPPProgramScope scope) {
		if (scope.getChilds().containsKey(name))
			return scope.getChilds().get(name);
		else if (scope.getParent() != null && scope.getParent() instanceof BPPProgramScope)
			return this.child(name, (BPPProgramScope) scope.getParent());
		return null;
	}

	public boolean existsChild(final String name) {
		return this.existsChild(name, this);
	}

	private boolean existsChild(final String name, final BPPProgramScope scope) {
		if (scope.getChilds().containsKey(name))
			return true;
		else if (scope.getParent() != null && scope.getParent() instanceof BPPProgramScope)
			return this.existsChild(name, (BPPProgramScope) scope.getParent());
		return false;
	}

	public Hashtable<String, BPPVariable> getChilds() {
		return this.childs;
	}

	public boolean pushChild(final String name, final BPPVariable child) {
		return this.childs.put(name, child) != null;
	}
}
