
package medit.BPP.core;

public class BPPVariable extends BPPObject {

	private String variableName = null;
	private BPPValue variableValue = null;

	public String getVariableName() {
		return this.variableName;
	}

	public BPPValue getVariableValue() {
		return this.variableValue;
	}

	public void setVariableName(final String variableName) {
		this.variableName = variableName;
	}

	public void setVariableValue(final BPPValue variableValue) {
		this.variableValue = variableValue;
	}

}
