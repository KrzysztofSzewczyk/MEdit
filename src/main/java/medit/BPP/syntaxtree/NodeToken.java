/* Generated by JTB 1.4.11 */
package medit.BPP.syntaxtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import medit.BPP.visitor.IRetArguVisitor;
import medit.BPP.visitor.IRetVisitor;
import medit.BPP.visitor.IVoidArguVisitor;
import medit.BPP.visitor.IVoidVisitor;

public class NodeToken implements INode {

	public static final String LS = System.getProperty("line.separator");

	private static final long serialVersionUID = 1411L;

	public int beginColumn;

	public int beginLine;

	public int endColumn;

	public int endLine;

	public int kind;

	public List<NodeToken> specialTokens;

	public String tokenImage;

	public NodeToken(final String s) {
		this(s, -1, -1, -1, -1, -1);
	}

	public NodeToken(final String s, final int kn, final int bl, final int bc, final int el, final int ec) {
		this.tokenImage = s;
		this.specialTokens = null;
		this.kind = kn;
		this.beginLine = bl;
		this.beginColumn = bc;
		this.endLine = el;
		this.endColumn = ec;
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

	public void addSpecial(final NodeToken s) {
		if (this.specialTokens == null)
			this.specialTokens = new ArrayList<>();
		this.specialTokens.add(s);
	}

	public NodeToken getSpecialAt(final int i) {
		if (this.specialTokens == null)
			throw new NoSuchElementException("No specialTokens in token");
		return this.specialTokens.get(i);
	}

	public String getSpecials(final String spc) {
		if (this.specialTokens == null)
			return "";
		int stLastLine = -1;
		final StringBuilder buf = new StringBuilder(64);
		boolean hasEol = false;
		for (final Iterator<NodeToken> e = this.specialTokens.iterator(); e.hasNext();) {
			final NodeToken st = e.next();
			final char c = st.tokenImage.charAt(st.tokenImage.length() - 1);
			hasEol = c == '\n' || c == '\r';
			if (stLastLine != -1)
				// not first line
				if (stLastLine != st.beginLine)
					// if not on the same line as the previous
					buf.append(spc);
				else
					// on the same line as the previous
					buf.append(' ');
			buf.append(st.tokenImage);
			if (!hasEol && e.hasNext())
				// not a single line comment and not the last one
				buf.append(NodeToken.LS);
			stLastLine = st.endLine;
		}
		// keep the same number of blank lines before the current non special
		for (int i = stLastLine + (hasEol ? 1 : 0); i < this.beginLine; i++) {
			buf.append(NodeToken.LS);
			if (i != this.beginLine - 1)
				buf.append(spc);
		}
		// indent if the current non special is not on the same line
		if (stLastLine != this.beginLine)
			buf.append(spc);
		return buf.toString();
	}

	public int numSpecials() {
		if (this.specialTokens == null)
			return 0;
		return this.specialTokens.size();
	}

	@Override
	public String toString() {
		return this.tokenImage;
	}

	public void trimSpecials() {
		if (this.specialTokens == null)
			return;
		((ArrayList<NodeToken>) this.specialTokens).trimToSize();
	}

	public String withSpecials(final String spc) {
		return this.withSpecials(spc, null);
	}

	public String withSpecials(final String spc, final String var) {
		final String specials = this.getSpecials(spc);
		int len = specials.length();
		if (len == 0)
			return var == null ? this.tokenImage : var + this.tokenImage;
		if (var != null)
			len += var.length();
		final StringBuilder buf = new StringBuilder(len + this.tokenImage.length());
		buf.append(specials);
		// see if needed to add a space
		int stLastLine = -1;
		for (final NodeToken nodeToken : this.specialTokens)
			stLastLine = nodeToken.endLine;
		if (stLastLine == this.beginLine)
			buf.append(' ');
		if (var != null)
			buf.append(var);
		buf.append(this.tokenImage);
		return buf.toString();
	}

}
