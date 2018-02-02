/*
 * 10/23/2011
 *
 * FoldCollapser.java - Goes through an RSTA instance and collapses folds of
 * specific types, such as comments.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.List;

/**
 * Collapses folds based on their type. You can create an instance of this class
 * to collapse all comment blocks when opening a new file, for example.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FoldCollapser {

	private final List<Integer> typesToCollapse;

	/**
	 * Creates an instance that collapses all comment blocks.
	 */
	public FoldCollapser() {
		this(FoldType.COMMENT);
	}

	/**
	 * Creates an instance that collapses all blocks of the specified type.
	 *
	 * @param typeToCollapse
	 *            The type to collapse.
	 * @see FoldType
	 */
	public FoldCollapser(final int typeToCollapse) {
		this.typesToCollapse = new ArrayList<>(3);
		this.addTypeToCollapse(typeToCollapse);
	}

	/**
	 * Adds a type of fold to collapse.
	 *
	 * @param typeToCollapse
	 *            The type of fold to collapse.
	 */
	public void addTypeToCollapse(final int typeToCollapse) {
		this.typesToCollapse.add(Integer.valueOf(typeToCollapse));
	}

	/**
	 * Collapses any relevant folds known by the fold manager.
	 *
	 * @param fm
	 *            The fold manager.
	 */
	public void collapseFolds(final FoldManager fm) {
		for (int i = 0; i < fm.getFoldCount(); i++) {
			final Fold fold = fm.getFold(i);
			this.collapseImpl(fold);
		}
	}

	/**
	 * Collapses the specified fold, and any of its child folds, as appropriate.
	 *
	 * @param fold
	 *            The fold to examine.
	 * @see #getShouldCollapse(Fold)
	 */
	protected void collapseImpl(final Fold fold) {
		if (this.getShouldCollapse(fold))
			fold.setCollapsed(true);
		for (int i = 0; i < fold.getChildCount(); i++)
			this.collapseImpl(fold.getChild(i));
	}

	/**
	 * Returns whether a specific fold should be collapsed.
	 *
	 * @param fold
	 *            The fold to examine.
	 * @return Whether the fold should be collapsed.
	 */
	public boolean getShouldCollapse(final Fold fold) {
		final int type = fold.getFoldType();
		for (final Integer typeToCollapse : this.typesToCollapse)
			if (type == typeToCollapse.intValue())
				return true;
		return false;
	}

}