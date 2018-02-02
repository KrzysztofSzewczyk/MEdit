
package medit.BPP.interpreter;

import java.util.Enumeration;
import java.util.LinkedList;

import medit.BPP.core.BPPProgramScope;
import medit.BPP.core.BPPValue;
import medit.BPP.core.BPPVariable;
import medit.BPP.reflect.BPPReflection;
import medit.BPP.syntaxtree.AdditiveExpression;
import medit.BPP.syntaxtree.IfExpression;
import medit.BPP.syntaxtree.JavaStaticMethods;
import medit.BPP.syntaxtree.MathExpression;
import medit.BPP.syntaxtree.MultiplicativeExpression;
import medit.BPP.syntaxtree.NodeChoice;
import medit.BPP.syntaxtree.NodeSequence;
import medit.BPP.syntaxtree.NodeToken;
import medit.BPP.syntaxtree.RelationalEqualityExpression;
import medit.BPP.syntaxtree.RelationalExprssion;
import medit.BPP.syntaxtree.RelationalGreaterExpression;
import medit.BPP.syntaxtree.RelationalLessExpression;
import medit.BPP.syntaxtree.Require;
import medit.BPP.syntaxtree.Start;
import medit.BPP.syntaxtree.StatementExpression;
import medit.BPP.syntaxtree.UnaryExpression;
import medit.BPP.syntaxtree.UnaryRelational;
import medit.BPP.syntaxtree.VariableAssign;
import medit.BPP.syntaxtree.VariableDeclaration;
import medit.BPP.syntaxtree.VariableName;
import medit.BPP.syntaxtree.WhileExpression;

public class Interpreter implements Interpret {

	@Override
	public Object visit(final AdditiveExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		final BPPValue value = (BPPValue) this.visit(node.f0, scope, objects);

		final Enumeration e = (Enumeration) node.f1.elements();
		while (e.hasMoreElements()) {
			final NodeSequence ns = (NodeSequence) e.nextElement();
			final NodeChoice nc = (NodeChoice) ns.elementAt(0);

			final BPPValue tmp = (BPPValue) this.visit((MultiplicativeExpression) ns.elementAt(1), scope, objects);
			if (nc.choice.toString().equals("+")) {
				tmp.setValue(value.getValue() + tmp.getValue());
				return tmp;
			} else if (nc.choice.toString().equals("-")) {
				tmp.setValue(value.getValue() - tmp.getValue());
				return tmp;
			}
		}
		return value;
	}

	@Override
	public Object visit(final IfExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		final BPPProgramScope ifScope = new BPPProgramScope(scope);
		if (new Boolean(this.visit(node.f1, scope, objects).toString())) {
			final Enumeration e = (Enumeration) node.f3.elements();
			while (e.hasMoreElements())
				this.visit((StatementExpression) e.nextElement(), ifScope, objects);
		}
		return null;
	}

	@Override
	public Object visit(final JavaStaticMethods node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		final String identifier = BPPReflection.fullIdentifier(node.f0.tokenImage);
		if (identifier != null) {
			Object currentObject = BPPReflection.makeObject(identifier);
			if (currentObject != null) {
				final Enumeration e = (Enumeration) node.f1.elements();
				while (e.hasMoreElements()) {
					final NodeSequence ns = (NodeSequence) e.nextElement();
					if (BPPReflection.existsField(currentObject, ns.elementAt(1).toString()))
						currentObject = BPPReflection.getFieldObject(currentObject, ns.elementAt(1).toString());
					else {
						final LinkedList<BPPValue> params = new LinkedList<>();
						params.add((BPPValue) this.visit(node.f3, scope, objects));
						final Enumeration eVal = (Enumeration) node.f4.elements();
						while (eVal.hasMoreElements()) {
							final NodeSequence nsVal = (NodeSequence) eVal.nextElement();
							params.add((BPPValue) this.visit((MathExpression) nsVal.elementAt(1), scope, objects));
						}

						if (BPPReflection.existsSubroutine(currentObject, ns.elementAt(1).toString(),
								params.toArray(new BPPValue[] {})))
							return BPPReflection.invokeStaticSubroutine(currentObject, ns.elementAt(1).toString(),
									params.toArray(new BPPValue[] {}));
						break;
					}
				}
			}
		}

		return null;
	}

	@Override
	public Object visit(final MathExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		return this.visit(node.f0, scope, objects);
	}

	@Override
	public Object visit(final MultiplicativeExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		final BPPValue value = (BPPValue) this.visit(node.f0, scope, objects);

		final Enumeration e = (Enumeration) node.f1.elements();
		while (e.hasMoreElements()) {
			final NodeSequence ns = (NodeSequence) e.nextElement();
			final NodeChoice nc = (NodeChoice) ns.elementAt(0);
			final BPPValue tmp = (BPPValue) this.visit((UnaryExpression) ns.elementAt(1), scope, objects);

			if (nc.choice.toString().equals("*")) {
				tmp.setValue(value.getValue() * tmp.getValue());
				return tmp;
			} else if (nc.choice.toString().equals("/")) {
				tmp.setValue(value.getValue() / tmp.getValue());
				return tmp;
			} else if (nc.choice.toString().equals("%")) {
				tmp.setValue(value.getValue() % tmp.getValue());
				return tmp;
			}
		}
		return value;
	}

	@Override
	public Object visit(final RelationalEqualityExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		Object obj = this.visit(node.f0, scope, objects);
		if (node.f1.node != null && obj instanceof Long) {
			final NodeSequence ns = (NodeSequence) node.f1.node;
			final Object tmp = this.visit((RelationalGreaterExpression) ns.elementAt(1), scope, objects);
			if (tmp instanceof Long) {
				final NodeChoice nc = (NodeChoice) ns.elementAt(0);
				if (nc.choice.toString().equals("=="))
					obj = Long.parseLong(obj.toString()) == Long.parseLong(tmp.toString());
				else if (nc.choice.toString().equals("!="))
					obj = Long.parseLong(obj.toString()) != Long.parseLong(tmp.toString());
			}
		}
		return obj;
	}

	@Override
	public Object visit(final RelationalExprssion node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		return this.visit(node.f0, scope, objects);
	}

	@Override
	public Object visit(final RelationalGreaterExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		Object obj = this.visit(node.f0, scope, objects);
		if (node.f1.node != null && obj instanceof Long) {
			final NodeSequence ns = (NodeSequence) node.f1.node;
			final Object tmp = this.visit((RelationalLessExpression) ns.elementAt(1), scope, objects);
			if (tmp instanceof Long) {
				final NodeChoice nc = (NodeChoice) ns.elementAt(0);
				if (nc.choice.toString().equals(">"))
					obj = Long.parseLong(obj.toString()) > Long.parseLong(tmp.toString());
				else if (nc.choice.toString().equals(">="))
					obj = Long.parseLong(obj.toString()) >= Long.parseLong(tmp.toString());
			}
		}
		return obj;
	}

	@Override
	public Object visit(final RelationalLessExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		Object obj = this.visit(node.f0, scope, objects);
		if (node.f1.node != null && obj instanceof Long) {
			final NodeSequence ns = (NodeSequence) node.f1.node;
			final Object tmp = this.visit((UnaryRelational) ns.elementAt(1), scope, objects);
			if (tmp instanceof Long) {
				final NodeChoice nc = (NodeChoice) ns.elementAt(0);
				if (nc.choice.toString().equals("<"))
					obj = Long.parseLong(obj.toString()) < Long.parseLong(tmp.toString());
				else if (nc.choice.toString().equals("<="))
					obj = Long.parseLong(obj.toString()) <= Long.parseLong(tmp.toString());
			}
		}
		return obj;
	}

	/**
	 * Convert B++ package name to Java package name
	 */
	@Override
	public Object visit(final Require node, final BPPProgramScope scope, final Object... objects) {
		final StringBuilder builder = new StringBuilder();
		final Enumeration element = (Enumeration) node.f1.elements();
		while (element.hasMoreElements()) {
			builder.append(element.nextElement());
			if (element.hasMoreElements())
				builder.append(".");
		}
		return builder;
	}

	@Override
	public Object visit(final Start node) throws Exception {
		final Enumeration importedPackagesEnum = (Enumeration) node.f0.elements();
		while (importedPackagesEnum.hasMoreElements()) {
			final NodeSequence ns = (NodeSequence) importedPackagesEnum.nextElement();
			BPPReflection.pushPackage(this.visit((Require) ns.elementAt(0), null).toString());
		}

		if (node.f1.size() > 0) {
			final BPPProgramScope parent = new BPPProgramScope(null);
			final Enumeration statement = (Enumeration) node.f1.elements();
			while (statement.hasMoreElements())
				this.visit((StatementExpression) statement.nextElement(), parent);
		}

		return null;
	}

	@Override
	public Object visit(final StatementExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		if (node.f0.choice instanceof VariableDeclaration)
			return this.visit((VariableDeclaration) node.f0.choice, scope, objects);
		else if (node.f0.choice instanceof VariableAssign)
			return this.visit((VariableAssign) node.f0.choice, scope, objects);
		else if (node.f0.choice instanceof JavaStaticMethods)
			return this.visit((JavaStaticMethods) node.f0.choice, scope, objects);
		else if (node.f0.choice instanceof IfExpression)
			return this.visit((IfExpression) node.f0.choice, scope, objects);
		else if (node.f0.choice instanceof WhileExpression)
			return this.visit((WhileExpression) node.f0.choice, scope, objects);
		return null;
	}

	@Override
	public Object visit(final UnaryExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		if (node.f0.choice instanceof NodeToken) {
			final BPPValue value = new BPPValue();
			value.setValue(Long.parseLong(node.f0.choice.toString()));
			value.setType(long.class); // here ;)
			return value;
		} else if (node.f0.choice instanceof VariableName) {
			final String var = this.visit((VariableName) node.f0.choice, scope, objects).toString();
			if (scope.existsChild(var))
				return scope.child(var).getVariableValue();
			else
				throw new Exception("Variable " + var + " does not exist.");
		} else if (node.f0.choice instanceof NodeSequence) {
			final NodeSequence ns = (NodeSequence) node.f0.choice;
			return this.visit((MathExpression) ns.elementAt(1), scope, objects);
		}
		return null;
	}

	@Override
	public Object visit(final UnaryRelational node, final BPPProgramScope scope, final Object... objects)
			throws Exception {

		return ((BPPValue) this.visit(node.f0, scope, objects)).getValue();
	}

	@Override
	public Object visit(final VariableAssign node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		final String name = this.visit(node.f0, scope, objects).toString();
		if (scope.existsChild(name)) {
			final BPPVariable var = scope.child(name);
			var.setVariableValue((BPPValue) this.visit(node.f2, scope, objects));
		}
		return null;
	}

	@Override
	public Object visit(final VariableDeclaration node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		final BPPVariable var = new BPPVariable();
		var.setVariableName(this.visit(node.f1, scope, objects).toString());
		var.setVariableValue((BPPValue) this.visit(node.f3, scope, objects));
		scope.pushChild(var.getVariableName(), var);
		return null;
	}

	@Override
	public Object visit(final VariableName node, final BPPProgramScope scope, final Object... objects) {
		return node.f0.tokenImage;
	}

	@Override
	public Object visit(final WhileExpression node, final BPPProgramScope scope, final Object... objects)
			throws Exception {
		final BPPProgramScope whileScope = new BPPProgramScope(scope);
		while (new Boolean(this.visit(node.f1, scope, objects).toString())) {
			final Enumeration e = (Enumeration) node.f3.elements();
			while (e.hasMoreElements())
				this.visit((StatementExpression) e.nextElement(), whileScope, objects);
		}
		return null;
	}

}
