/* Generated by JTB 1.4.11 */
package medit.BPP.syntaxtree;

import medit.BPP.visitor.*;

public class MathExpression implements INode {

  public AdditiveExpression f0;

  private static final long serialVersionUID = 1411L;

  public MathExpression(final AdditiveExpression n0) {
    f0 = n0;
  }

  @Override
  public <R, A> R accept(final IRetArguVisitor<R, A> vis, final A argu) {
    return vis.visit(this, argu);
  }

  @Override
  public <R> R accept(final IRetVisitor<R> vis) {
    return vis.visit(this);
  }

  @Override
  public <A> void accept(final IVoidArguVisitor<A> vis, final A argu) {
    vis.visit(this, argu);
  }

  @Override
  public void accept(final IVoidVisitor vis) {
    vis.visit(this);
  }

}
