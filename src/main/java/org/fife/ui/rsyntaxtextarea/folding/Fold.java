/*
 * 10/08/2011
 *
 * Fold.java - A foldable region of text in an RSyntaxTextArea instance.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * Information about a foldable region.
 * <p>
 *
 * A <code>Fold</code> has zero or more children, and <code>Folds</code> thus
 * form a hierarchical structure, with "parent" folds containing the info about
 * any "child" folds they contain.
 * <p>
 *
 * Fold regions are denoted by a starting and ending offset, but the actual
 * folding is done on a per-line basis, so <code>Fold</code> instances provide
 * methods for retrieving both starting and ending offsets and lines. The
 * starting and ending offsets/lines are "sticky" and correctly track their
 * positions even as the document is modified.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class Fold implements Comparable<Fold> {

	private int cachedEndLine;
	private int cachedStartLine;
	private int childCollapsedLineCount;
	private List<Fold> children;
	private boolean collapsed;
	private Position endOffs;
	private int lastEndOffs = -1;
	private int lastStartOffs = -1;

	private Fold parent;
	private final Position startOffs;

	private final RSyntaxTextArea textArea;
	private final int type;

	public Fold(final int type, final RSyntaxTextArea textArea, final int startOffs) throws BadLocationException {
		this.type = type;
		this.textArea = textArea;
		this.startOffs = textArea.getDocument().createPosition(startOffs);
	}

	/**
	 * Two folds are considered equal if they start at the same offset.
	 *
	 * @param otherFold
	 *            Another fold to compare this one to.
	 * @return How this fold compares to the other.
	 */
	@Override
	public int compareTo(final Fold otherFold) {
		int result = -1;
		if (otherFold != null)
			result = this.startOffs.getOffset() - otherFold.startOffs.getOffset();
		// result = getStartLine() - otherFold.getStartLine();
		return result;
	}

	/**
	 * Returns whether the specified line would be hidden in this fold. Since RSTA
	 * displays the "first" line in a fold, this means that the line must must be
	 * between <code>(getStartLine()+1)</code> and <code>getEndLine()</code>,
	 * inclusive.
	 *
	 * @param line
	 *            The line to check.
	 * @return Whether the line would be hidden if this fold is collapsed.
	 * @see #containsOffset(int)
	 * @see #containsOrStartsOnLine(int)
	 */
	public boolean containsLine(final int line) {
		return line > this.getStartLine() && line <= this.getEndLine();
	}

	/**
	 * Returns whether the specified offset is "inside" the fold. This method
	 * returns <code>true</code> if the offset is greater than the fold start
	 * offset, and no further than the last offset of the last folded line.
	 *
	 * @param offs
	 *            The offset to check.
	 * @return Whether the offset is "inside" the fold.
	 * @see #containsLine(int)
	 */
	public boolean containsOffset(final int offs) {
		boolean contained = false;
		if (offs > this.getStartOffset()) {
			// Use Elements to avoid BadLocationExceptions
			final Element root = this.textArea.getDocument().getDefaultRootElement();
			final int line = root.getElementIndex(offs);
			contained = line <= this.getEndLine();
		}
		return contained;
	}

	/**
	 * Returns whether the given line is in the range
	 * <code>[getStartLine(), getEndLine()]</code>, inclusive.
	 *
	 * @param line
	 *            The line to check.
	 * @return Whether this fold contains, or starts on, the line.
	 * @see #containsLine(int)
	 */
	public boolean containsOrStartsOnLine(final int line) {
		return line >= this.getStartLine() && line <= this.getEndLine();
	}

	/**
	 * Creates a fold that is a child of this one.
	 *
	 * @param type
	 *            The type of fold.
	 * @param startOffs
	 *            The starting offset of the fold.
	 * @return The child fold.
	 * @throws BadLocationException
	 *             If <code>startOffs</code> is invalid.
	 * @see FoldType
	 */
	public Fold createChild(final int type, final int startOffs) throws BadLocationException {
		final Fold child = new Fold(type, this.textArea, startOffs);
		child.parent = this;
		if (this.children == null)
			this.children = new ArrayList<>();
		this.children.add(child);
		return child;
	}

	/**
	 * Two folds are considered equal if they have the same starting offset.
	 *
	 * @param otherFold
	 *            Another fold to compare this one to.
	 * @return Whether the two folds are equal.
	 * @see #compareTo(Fold)
	 */
	@Override
	public boolean equals(final Object otherFold) {
		return otherFold instanceof Fold && this.compareTo((Fold) otherFold) == 0;
	}

	/**
	 * Returns a specific child fold.
	 *
	 * @param index
	 *            The index of the child fold.
	 * @return The child fold.
	 * @see #getChildCount()
	 */
	public Fold getChild(final int index) {
		return this.children.get(index);
	}

	/**
	 * Returns the number of child folds.
	 *
	 * @return The number of child folds.
	 * @see #getChild(int)
	 */
	public int getChildCount() {
		return this.children == null ? 0 : this.children.size();
	}

	/**
	 * Returns the array of child folds. This is a shallow copy.
	 *
	 * @return The array of child folds, or <code>null</code> if there are none.
	 */
	List<Fold> getChildren() {
		return this.children;
	}

	/**
	 * Returns the number of collapsed lines under this fold. If this fold is
	 * collapsed, this method returns {@link #getLineCount()}, otherwise it returns
	 * the sum of all collapsed lines of all child folds of this one.
	 * <p>
	 *
	 * The value returned is cached, so this method returns quickly and shouldn't
	 * affect performance.
	 *
	 * @return The number of collapsed lines under this fold.
	 */
	public int getCollapsedLineCount() {
		return this.collapsed ? this.getLineCount() : this.childCollapsedLineCount;
	}

	/**
	 * Returns the "deepest" fold containing the specified offset. It is assumed
	 * that it's already been verified that <code>offs</code> is indeed contained in
	 * this fold.
	 *
	 * @param offs
	 *            The offset.
	 * @return The fold, or <code>null</code> if no child fold also contains the
	 *         offset.
	 * @see FoldManager#getDeepestFoldContaining(int)
	 */
	Fold getDeepestFoldContaining(final int offs) {
		Fold deepestFold = this;
		for (int i = 0; i < this.getChildCount(); i++) {
			final Fold fold = this.getChild(i);
			if (fold.containsOffset(offs)) {
				deepestFold = fold.getDeepestFoldContaining(offs);
				break;
			}
		}
		return deepestFold;
	}

	/**
	 * Returns the "deepest" open fold containing the specified offset. It is
	 * assumed that it's already been verified that <code>offs</code> is indeed
	 * contained in this fold.
	 *
	 * @param offs
	 *            The offset.
	 * @return The fold, or <code>null</code> if no open fold contains the offset.
	 * @see FoldManager#getDeepestOpenFoldContaining(int)
	 */
	Fold getDeepestOpenFoldContaining(final int offs) {

		Fold deepestFold = this;

		for (int i = 0; i < this.getChildCount(); i++) {
			final Fold fold = this.getChild(i);
			if (fold.containsOffset(offs)) {
				if (fold.isCollapsed())
					break;
				deepestFold = fold.getDeepestOpenFoldContaining(offs);
				break;
			}
		}

		return deepestFold;

	}

	/**
	 * Returns the end line of this fold. For example, in languages such as C and
	 * Java, this might be the line containing the closing curly brace of a code
	 * block.
	 * <p>
	 *
	 * The value returned by this method will automatically update as the text
	 * area's contents are modified, to track the ending line of the code block.
	 *
	 * @return The end line of this code block.
	 * @see #getEndOffset()
	 * @see #getStartLine()
	 */
	public int getEndLine() {
		final int endOffs = this.getEndOffset();
		if (this.lastEndOffs == endOffs)
			return this.cachedEndLine;
		this.lastEndOffs = endOffs;
		final Element root = this.textArea.getDocument().getDefaultRootElement();
		return this.cachedEndLine = root.getElementIndex(endOffs);
	}

	/**
	 * Returns the end offset of this fold. For example, in languages such as C and
	 * Java, this might be the offset of the closing curly brace of a code block.
	 * <p>
	 *
	 * The value returned by this method will automatically update as the text
	 * area's contents are modified, to track the ending offset of the code block.
	 *
	 * @return The end offset of this code block, or {@link Integer#MAX_VALUE} if
	 *         this fold region isn't closed properly. The latter causes this fold
	 *         to collapsed all lines through the end of the file.
	 * @see #getEndLine()
	 * @see #getStartOffset()
	 */
	public int getEndOffset() {
		return this.endOffs != null ? this.endOffs.getOffset() : Integer.MAX_VALUE;
	}

	/**
	 * Returns the type of fold this is. This will be one of the values in
	 * {@link FoldType}, or a user-defined value.
	 *
	 * @return The type of fold this is.
	 */
	public int getFoldType() {
		return this.type;
	}

	/**
	 * Returns whether this fold has any child folds.
	 *
	 * @return Whether this fold has any children.
	 * @see #getChildCount()
	 */
	public boolean getHasChildFolds() {
		return this.getChildCount() > 0;
	}

	/**
	 * Returns the last child fold.
	 *
	 * @return The last child fold, or <code>null</code> if this fold does not have
	 *         any children.
	 * @see #getChild(int)
	 * @see #getHasChildFolds()
	 */
	public Fold getLastChild() {
		final int childCount = this.getChildCount();
		return childCount == 0 ? null : this.getChild(childCount - 1);
	}

	/**
	 * Returns the number of lines that are hidden when this fold is collapsed.
	 *
	 * @return The number of lines hidden.
	 * @see #getStartLine()
	 * @see #getEndLine()
	 */
	public int getLineCount() {
		return this.getEndLine() - this.getStartLine();
	}

	/**
	 * Returns the parent fold of this one.
	 *
	 * @return The parent fold, or <code>null</code> if this is a top-level fold.
	 */
	public Fold getParent() {
		return this.parent;
	}

	/**
	 * Returns the starting line of this fold region. This is the only line in the
	 * fold region that is not hidden when a fold is collapsed.
	 * <p>
	 *
	 * The value returned by this method will automatically update as the text
	 * area's contents are modified, to track the starting line of the code block.
	 *
	 * @return The starting line of the code block.
	 * @see #getEndLine()
	 * @see #getStartOffset()
	 */
	public int getStartLine() {
		final int startOffs = this.getStartOffset();
		if (this.lastStartOffs == startOffs)
			return this.cachedStartLine;
		this.lastStartOffs = startOffs;
		final Element root = this.textArea.getDocument().getDefaultRootElement();
		return this.cachedStartLine = root.getElementIndex(startOffs);
	}

	/**
	 * Returns the starting offset of this fold region. For example, for languages
	 * such as C and Java, this would be the offset of the opening curly brace of a
	 * code block.
	 * <p>
	 *
	 * The value returned by this method will automatically update as the text
	 * area's contents are modified, to track the starting offset of the code block.
	 *
	 * @return The start offset of this fold.
	 * @see #getStartLine()
	 * @see #getEndOffset()
	 */
	public int getStartOffset() {
		return this.startOffs.getOffset();
	}

	@Override
	public int hashCode() {
		return this.getStartLine();
	}

	/**
	 * Returns whether this fold is collapsed.
	 *
	 * @return Whether this fold is collapsed.
	 * @see #setCollapsed(boolean)
	 * @see #toggleCollapsedState()
	 */
	public boolean isCollapsed() {
		return this.collapsed;
	}

	/**
	 * Returns whether this fold is entirely on a single line. In general, a
	 * {@link FoldParser} should not remember fold regions all on a single line,
	 * since there's really nothing to fold.
	 *
	 * @return Whether this fold is on a single line.
	 * @see #removeFromParent()
	 */
	public boolean isOnSingleLine() {
		return this.getStartLine() == this.getEndLine();
	}

	/**
	 * Removes this fold from its parent. This should only be called by
	 * {@link FoldParser} implementations if they determine that a fold is all on a
	 * single line (and thus shouldn't be remembered) after creating it.
	 *
	 * @return Whether this fold had a parent to be removed from.
	 * @see #isOnSingleLine()
	 */
	public boolean removeFromParent() {
		if (this.parent != null) {
			this.parent.removeMostRecentChild();
			this.parent = null;
			return true;
		}
		return false;
	}

	private void removeMostRecentChild() {
		this.children.remove(this.children.size() - 1);
	}

	/**
	 * Sets whether this <code>Fold</code> is collapsed. Calling this method will
	 * update both the text area and all <code>Gutter</code> components.
	 *
	 * @param collapsed
	 *            Whether this fold should be collapsed.
	 * @see #isCollapsed()
	 * @see #toggleCollapsedState()
	 */
	public void setCollapsed(final boolean collapsed) {

		if (collapsed != this.collapsed) {

			// Change our fold state and cached info about folded line count.
			final int lineCount = this.getLineCount();
			int linesToCollapse = lineCount - this.childCollapsedLineCount;
			if (!collapsed)
				linesToCollapse = -linesToCollapse;
			// System.out.println("Hiding lines: " + linesToCollapse +
			// " (" + lineCount + ", " + linesToCollapse + ")");
			this.collapsed = collapsed;
			if (this.parent != null)
				this.parent.updateChildCollapsedLineCount(linesToCollapse);

			// If an end point of the selection is being hidden, move the caret
			// "out" of the fold.
			if (collapsed) {
				int dot = this.textArea.getSelectionStart(); // Forgive variable name
				final Element root = this.textArea.getDocument().getDefaultRootElement();
				final int dotLine = root.getElementIndex(dot);
				boolean updateCaret = this.containsLine(dotLine);
				if (!updateCaret) {
					final int mark = this.textArea.getSelectionEnd();
					if (mark != dot) {
						final int markLine = root.getElementIndex(mark);
						updateCaret = this.containsLine(markLine);
					}
				}
				if (updateCaret) {
					dot = root.getElement(this.getStartLine()).getEndOffset() - 1;
					this.textArea.setCaretPosition(dot);
				}
			}

			this.textArea.foldToggled(this);

		}

	}

	/**
	 * Sets the ending offset of this fold, such as the closing curly brace of a
	 * code block in C or Java. {@link FoldParser} implementations should call this
	 * on an existing <code>Fold</code> upon finding its end. If this method isn't
	 * called, then this <code>Fold</code> is considered to have no end, i.e., it
	 * will collapse everything to the end of the file.
	 *
	 * @param endOffs
	 *            The end offset of this fold.
	 * @throws BadLocationException
	 *             If <code>endOffs</code> is not a valid location in the text area.
	 */
	public void setEndOffset(final int endOffs) throws BadLocationException {
		this.endOffs = this.textArea.getDocument().createPosition(endOffs);
	}

	/**
	 * Toggles the collapsed state of this fold.
	 *
	 * @see #setCollapsed(boolean)
	 */
	public void toggleCollapsedState() {
		this.setCollapsed(!this.collapsed);
	}

	/**
	 * Overridden for debugging purposes.
	 *
	 * @return A string representation of this <code>Fold</code>.
	 */
	@Override
	public String toString() {
		return "[Fold: " + "startOffs=" + this.getStartOffset() + ", endOffs=" + this.getEndOffset() + ", collapsed="
				+ this.collapsed + "]";
	}

	private void updateChildCollapsedLineCount(final int count) {
		this.childCollapsedLineCount += count;
		// if (childCollapsedLineCount>getLineCount()) {
		// Thread.dumpStack();
		// }
		if (!this.collapsed && this.parent != null)
			this.parent.updateChildCollapsedLineCount(count);
	}

}