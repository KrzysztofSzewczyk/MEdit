
package medit.BPP.interpreter;

import medit.BPP.core.BPPProgramScope;
import medit.BPP.syntaxtree.AdditiveExpression;
import medit.BPP.syntaxtree.IfExpression;
import medit.BPP.syntaxtree.JavaStaticMethods;
import medit.BPP.syntaxtree.MathExpression;
import medit.BPP.syntaxtree.MultiplicativeExpression;
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

/**
 * No one ever should touch this dirty shit
 *
 * @author Krzysztof Szewczyk
 *
 */

public interface Interpret {

	public Object visit(AdditiveExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(IfExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(JavaStaticMethods node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(MathExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(MultiplicativeExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(RelationalEqualityExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(RelationalExprssion node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(RelationalGreaterExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(RelationalLessExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(Require node, BPPProgramScope scope, Object... objects);

	public Object visit(Start node) throws Exception;

	public Object visit(StatementExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(UnaryExpression node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(UnaryRelational node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(VariableAssign node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(VariableDeclaration node, BPPProgramScope scope, Object... objects) throws Exception;

	public Object visit(VariableName node, BPPProgramScope scope, Object... objects);

	public Object visit(WhileExpression node, BPPProgramScope scope, Object... objects) throws Exception;
}
