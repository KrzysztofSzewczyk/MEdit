/* Generated by JTB 1.4.11 */
package medit.BPP.syntaxtree;

import medit.BPP.visitor.IRetArguVisitor;
import medit.BPP.visitor.IRetVisitor;
import medit.BPP.visitor.IVoidArguVisitor;
import medit.BPP.visitor.IVoidVisitor;

public interface INodeList extends INode {

  public void addNode(final INode n);

  public INode elementAt(int i);

  public java.util.Iterator<INode> elements();

  public int size();

  @Override
  public <R, A> R accept(final IRetArguVisitor<R, A> vis, final A argu);

  @Override
  public <R> R accept(final IRetVisitor<R> vis);

  @Override
  public <A> void accept(final IVoidArguVisitor<A> vis, final A argu);

  @Override
  public void accept(final IVoidVisitor vis);

}
