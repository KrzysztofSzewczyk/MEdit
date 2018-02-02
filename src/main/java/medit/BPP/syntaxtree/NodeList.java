/* Generated by JTB 1.4.11 */
package medit.BPP.syntaxtree;

import java.util.ArrayList;
import java.util.Iterator;

import medit.BPP.visitor.IRetArguVisitor;
import medit.BPP.visitor.IRetVisitor;
import medit.BPP.visitor.IVoidArguVisitor;
import medit.BPP.visitor.IVoidVisitor;

public class NodeList implements INodeList {

	private static final int allocTb[] = { 1, 2, 3, 4, 5, 10, 20, 50 };

	private static final long serialVersionUID = 1411L;

	private int allocNb = 0;

	public ArrayList<INode> nodes;

	public NodeList() {
		this.nodes = new ArrayList<>(NodeList.allocTb[this.allocNb]);
	}

	public NodeList(final INode firstNode) {
		this.nodes = new ArrayList<>(NodeList.allocTb[this.allocNb]);
		this.addNode(firstNode);
	}

	public NodeList(final int sz) {
		this.nodes = new ArrayList<>(sz);
	}

	public NodeList(final int sz, final INode firstNode) {
		this.nodes = new ArrayList<>(sz);
		this.addNode(firstNode);
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

	@Override
	public void addNode(final INode n) {
		if (++this.allocNb < NodeList.allocTb.length)
			this.nodes.ensureCapacity(NodeList.allocTb[this.allocNb]);
		else
			this.nodes.ensureCapacity(
					(this.allocNb - NodeList.allocTb.length + 2) * NodeList.allocTb[NodeList.allocTb.length - 1]);
		this.nodes.add(n);
	}

	@Override
	public INode elementAt(final int i) {
		return this.nodes.get(i);
	}

	@Override
	public Iterator<INode> elements() {
		return this.nodes.iterator();
	}

	@Override
	public int size() {
		return this.nodes.size();
	}

}
