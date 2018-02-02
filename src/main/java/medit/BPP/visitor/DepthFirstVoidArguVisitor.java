/* Generated by JTB 1.4.11 */
package medit.BPP.visitor;

import java.util.Iterator;

import medit.BPP.syntaxtree.AdditiveExpression;
import medit.BPP.syntaxtree.INode;
import medit.BPP.syntaxtree.IfExpression;
import medit.BPP.syntaxtree.JavaStaticMethods;
import medit.BPP.syntaxtree.MathExpression;
import medit.BPP.syntaxtree.MultiplicativeExpression;
import medit.BPP.syntaxtree.NodeChoice;
import medit.BPP.syntaxtree.NodeList;
import medit.BPP.syntaxtree.NodeListOptional;
import medit.BPP.syntaxtree.NodeOptional;
import medit.BPP.syntaxtree.NodeSequence;
import medit.BPP.syntaxtree.NodeTCF;
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

public class DepthFirstVoidArguVisitor<A> implements IVoidArguVisitor<A> {

	@Override
	public void visit(final AdditiveExpression n, final A argu) {
		// f0 -> MultiplicativeExpression()
		n.f0.accept(this, argu);
		// f1 -> ( #0 ( %0 "+"
		// .. .. . .. | %1 "-" )
		// .. .. . #1 MultiplicativeExpression() )*
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final IfExpression n, final A argu) {
		// f0 -> "if"
		n.f0.accept(this, argu);
		// f1 -> RelationalExprssion()
		n.f1.accept(this, argu);
		// f2 -> "begin"
		n.f2.accept(this, argu);
		// f3 -> ( StatementExpression() )*
		n.f3.accept(this, argu);
		// f4 -> "end"
		n.f4.accept(this, argu);
	}

	@Override
	public void visit(final JavaStaticMethods n, final A argu) {
		// f0 -> < IDENTIFIER >
		n.f0.accept(this, argu);
		// f1 -> ( #0 ":" #1 < IDENTIFIER > )+
		n.f1.accept(this, argu);
		// f2 -> "("
		n.f2.accept(this, argu);
		// f3 -> MathExpression()
		n.f3.accept(this, argu);
		// f4 -> ( #0 "," #1 MathExpression() )*
		n.f4.accept(this, argu);
		// f5 -> ")"
		n.f5.accept(this, argu);
		// f6 -> "."
		n.f6.accept(this, argu);
	}

	@Override
	public void visit(final MathExpression n, final A argu) {
		// f0 -> AdditiveExpression()
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final MultiplicativeExpression n, final A argu) {
		// f0 -> UnaryExpression()
		n.f0.accept(this, argu);
		// f1 -> ( #0 ( %0 "*"
		// .. .. . .. | %1 "/"
		// .. .. . .. | %2 "%" )
		// .. .. . #1 UnaryExpression() )*
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final NodeChoice n, final A argu) {
		n.choice.accept(this, argu);
		return;
	}

	@Override
	public void visit(final NodeList n, final A argu) {
		for (final Iterator<INode> e = n.elements(); e.hasNext();)
			e.next().accept(this, argu);
		return;
	}

	@Override
	public void visit(final NodeListOptional n, final A argu) {
		if (n.present()) {
			for (final Iterator<INode> e = n.elements(); e.hasNext();)
				e.next().accept(this, argu);
			return;
		} else
			return;
	}

	@Override
	public void visit(final NodeOptional n, final A argu) {
		if (n.present()) {
			n.node.accept(this, argu);
			return;
		} else
			return;
	}

	@Override
	public void visit(final NodeSequence n, final A argu) {
		for (final Iterator<INode> e = n.elements(); e.hasNext();)
			e.next().accept(this, argu);
		return;
	}

	@Override
	public void visit(final NodeTCF n, @SuppressWarnings("unused") final A argu) {
		return;
	}

	@Override
	public void visit(final NodeToken n, @SuppressWarnings("unused") final A argu) {
		return;
	}

	@Override
	public void visit(final RelationalEqualityExpression n, final A argu) {
		// f0 -> RelationalGreaterExpression()
		n.f0.accept(this, argu);
		// f1 -> [ #0 ( %0 "=="
		// .. .. . .. | %1 "!=" )
		// .. .. . #1 RelationalGreaterExpression() ]
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final RelationalExprssion n, final A argu) {
		// f0 -> RelationalEqualityExpression()
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final RelationalGreaterExpression n, final A argu) {
		// f0 -> RelationalLessExpression()
		n.f0.accept(this, argu);
		// f1 -> [ #0 ( %0 ">"
		// .. .. . .. | %1 ">=" )
		// .. .. . #1 RelationalLessExpression() ]
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final RelationalLessExpression n, final A argu) {
		// f0 -> UnaryRelational()
		n.f0.accept(this, argu);
		// f1 -> [ #0 ( %0 "<"
		// .. .. . .. | %1 "<=" )
		// .. .. . #1 UnaryRelational() ]
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final Require n, final A argu) {
		// f0 -> "with"
		n.f0.accept(this, argu);
		// f1 -> ( < IDENTIFIER > )+
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final Start n, final A argu) {
		// f0 -> ( #0 Require() #1 "." )+
		n.f0.accept(this, argu);
		// f1 -> ( StatementExpression() )*
		n.f1.accept(this, argu);
	}

	@Override
	public void visit(final StatementExpression n, final A argu) {
		// f0 -> . %0 VariableDeclaration()
		// .. .. | %1 VariableAssign()
		// .. .. | %2 JavaStaticMethods()
		// .. .. | %3 IfExpression()
		// .. .. | %4 WhileExpression()
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final UnaryExpression n, final A argu) {
		// f0 -> . %0 #0 "(" #1 MathExpression() #2 ")"
		// .. .. | %1 < INTEGER_LITERAL >
		// .. .. | %2 VariableName()
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final UnaryRelational n, final A argu) {
		// f0 -> MathExpression()
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final VariableAssign n, final A argu) {
		// f0 -> VariableName()
		n.f0.accept(this, argu);
		// f1 -> "="
		n.f1.accept(this, argu);
		// f2 -> MathExpression()
		n.f2.accept(this, argu);
		// f3 -> "."
		n.f3.accept(this, argu);
	}

	@Override
	public void visit(final VariableDeclaration n, final A argu) {
		// f0 -> "var"
		n.f0.accept(this, argu);
		// f1 -> VariableName()
		n.f1.accept(this, argu);
		// f2 -> "="
		n.f2.accept(this, argu);
		// f3 -> MathExpression()
		n.f3.accept(this, argu);
		// f4 -> "."
		n.f4.accept(this, argu);
	}

	@Override
	public void visit(final VariableName n, final A argu) {
		// f0 -> < IDENTIFIER >
		n.f0.accept(this, argu);
	}

	@Override
	public void visit(final WhileExpression n, final A argu) {
		// f0 -> "while"
		n.f0.accept(this, argu);
		// f1 -> RelationalExprssion()
		n.f1.accept(this, argu);
		// f2 -> "begin"
		n.f2.accept(this, argu);
		// f3 -> ( StatementExpression() )*
		n.f3.accept(this, argu);
		// f4 -> "end"
		n.f4.accept(this, argu);
	}

}
