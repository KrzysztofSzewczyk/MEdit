/*
 * 10/08/2011
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rtextarea.RDocument;

/**
 * The default implementation of a fold manager. Besides keeping track of folds,
 * this class behaves as follows:
 *
 * <ul>
 * <li>If text containing a newline is inserted in a collapsed fold, that fold,
 * and any ancestor folds, are expanded. This ensures that modified text is
 * always visible to the user.
 * <li>If the text area's {@link RSyntaxTextArea#SYNTAX_STYLE_PROPERTY} changes,
 * the current fold parser is uninstalled, and one appropriate for the new
 * language, if any, is installed.
 * </ul>
 *
 * The folding strategy to use is retrieved from {@link FoldParserManager}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DefaultFoldManager implements FoldManager {

	/**
	 * Listens for events in the text editor.
	 */
	private class Listener implements DocumentListener, PropertyChangeListener {

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			// Adding text containing a newline to the visible line of a folded
			// Fold causes that Fold to unfold. Check only start offset of
			// insertion since that's the line that was "modified".
			final int startOffs = e.getOffset();
			final int endOffs = startOffs + e.getLength();
			final Document doc = e.getDocument();
			final Element root = doc.getDefaultRootElement();
			final int startLine = root.getElementIndex(startOffs);
			final int endLine = root.getElementIndex(endOffs);
			if (startLine != endLine) { // Inserted text covering > 1 line...
				final Fold fold = DefaultFoldManager.this.getFoldForLine(startLine);
				if (fold != null && fold.isCollapsed())
					fold.toggleCollapsedState();
			}
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			final String name = e.getPropertyName();

			if (RSyntaxTextArea.SYNTAX_STYLE_PROPERTY.equals(name)) {
				// Syntax style changed in editor.
				DefaultFoldManager.this.updateFoldParser();
				DefaultFoldManager.this.reparse(); // Even if no fold parser change, highlighting did
			}

			else if ("document".equals(name)) {
				// The document switched out from under us
				final RDocument old = (RDocument) e.getOldValue();
				if (old != null)
					old.removeDocumentListener(this);
				final RDocument newDoc = (RDocument) e.getNewValue();
				if (newDoc != null)
					newDoc.addDocumentListener(this);
				DefaultFoldManager.this.reparse();
			}

		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			// Removing text from the visible line of a folded Fold causes that
			// Fold to unfold. We only need to check the removal offset since
			// that's the new caret position.
			final int offs = e.getOffset();
			try {
				final int lastLineModified = DefaultFoldManager.this.textArea.getLineOfOffset(offs);
				// System.out.println(">>> " + lastLineModified);
				final Fold fold = DefaultFoldManager.this.getFoldForLine(lastLineModified);
				// System.out.println("&&& " + fold);
				if (fold != null && fold.isCollapsed())
					fold.toggleCollapsedState();
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}

	}

	private boolean codeFoldingEnabled;
	private FoldParser foldParser;
	private List<Fold> folds;
	private final Listener l;
	private Parser rstaParser;
	private final PropertyChangeSupport support;

	private final RSyntaxTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The text area whose folds we are managing.
	 */
	public DefaultFoldManager(final RSyntaxTextArea textArea) {
		this.textArea = textArea;
		this.support = new PropertyChangeSupport(this);
		this.l = new Listener();
		textArea.getDocument().addDocumentListener(this.l);
		textArea.addPropertyChangeListener(RSyntaxTextArea.SYNTAX_STYLE_PROPERTY, this.l);
		textArea.addPropertyChangeListener("document", this.l);
		this.folds = new ArrayList<>();
		this.updateFoldParser();
	}

	@Override
	public void addPropertyChangeListener(final PropertyChangeListener l) {
		this.support.addPropertyChangeListener(l);
	}

	@Override
	public void clear() {
		this.folds.clear();
	}

	@Override
	public boolean ensureOffsetNotInClosedFold(final int offs) {

		boolean foldsOpened = false;
		Fold fold = this.getDeepestFoldContaining(offs);

		while (fold != null) {
			if (fold.isCollapsed()) {
				fold.setCollapsed(false);
				foldsOpened = true;
			}
			fold = fold.getParent();
		}

		if (foldsOpened)
			RSyntaxUtilities.possiblyRepaintGutter(this.textArea);

		return foldsOpened;

	}

	@Override
	public Fold getDeepestFoldContaining(final int offs) {
		Fold deepestFold = null;
		if (offs > -1)
			for (int i = 0; i < this.folds.size(); i++) {
				final Fold fold = this.getFold(i);
				if (fold.containsOffset(offs)) {
					deepestFold = fold.getDeepestFoldContaining(offs);
					break;
				}
			}
		return deepestFold;
	}

	@Override
	public Fold getDeepestOpenFoldContaining(final int offs) {

		Fold deepestFold = null;

		if (offs > -1)
			for (int i = 0; i < this.folds.size(); i++) {
				final Fold fold = this.getFold(i);
				if (fold.containsOffset(offs)) {
					if (fold.isCollapsed())
						return null;
					deepestFold = fold.getDeepestOpenFoldContaining(offs);
					break;
				}
			}

		return deepestFold;

	}

	@Override
	public Fold getFold(final int index) {
		return this.folds.get(index);
	}

	@Override
	public int getFoldCount() {
		return this.folds.size();
	}

	@Override
	public Fold getFoldForLine(final int line) {
		return this.getFoldForLineImpl(null, this.folds, line);
	}

	private Fold getFoldForLineImpl(final Fold parent, final List<Fold> folds, final int line) {

		int low = 0;
		int high = folds.size() - 1;

		while (low <= high) {
			final int mid = low + high >> 1;
			final Fold midFold = folds.get(mid);
			final int startLine = midFold.getStartLine();
			if (line == startLine)
				return midFold;
			else if (line < startLine)
				high = mid - 1;
			else {
				final int endLine = midFold.getEndLine();
				if (line >= endLine)
					low = mid + 1;
				else { // line>startLine && line<=endLine
					final List<Fold> children = midFold.getChildren();
					return children != null ? this.getFoldForLineImpl(midFold, children, line) : null;
				}
			}
		}

		return null; // No fold for this line
	}

	@Override
	public int getHiddenLineCount() {
		int count = 0;
		for (final Fold fold : this.folds)
			count += fold.getCollapsedLineCount();
		return count;
	}

	@Override
	public int getHiddenLineCountAbove(final int line) {
		return this.getHiddenLineCountAbove(line, false);
	}

	@Override
	public int getHiddenLineCountAbove(final int line, final boolean physical) {

		int count = 0;

		for (final Fold fold : this.folds) {
			final int comp = physical ? line + count : line;
			if (fold.getStartLine() >= comp)
				break;
			count += this.getHiddenLineCountAboveImpl(fold, comp, physical);
		}

		return count;

	}

	/**
	 * Returns the number of lines "hidden" by collapsed folds above the specified
	 * line.
	 *
	 * @param fold
	 *            The current fold in the recursive algorithm. It and its children
	 *            are examined.
	 * @param line
	 *            The line.
	 * @param physical
	 *            Whether <code>line</code> is the number of a physical line (i.e.
	 *            visible, not code-folded), or a logical one (i.e. any line from
	 *            the model). If <code>line</code> was determined by a raw line
	 *            calculation (i.e. <code>(visibleTopY / lineHeight)</code>), this
	 *            value should be <code>true</code>. It should be <code>false</code>
	 *            when it was calculated from an offset in the document (for
	 *            example).
	 * @return The number of lines hidden in folds that are descendants of
	 *         <code>fold</code>, or <code>fold</code> itself, above
	 *         <code>line</code>.
	 */
	private int getHiddenLineCountAboveImpl(final Fold fold, final int line, final boolean physical) {

		int count = 0;

		if (fold.getEndLine() < line || fold.isCollapsed() && fold.getStartLine() < line)
			count = fold.getCollapsedLineCount();
		else {
			final int childCount = fold.getChildCount();
			for (int i = 0; i < childCount; i++) {
				final Fold child = fold.getChild(i);
				final int comp = physical ? line + count : line;
				if (child.getStartLine() >= comp)
					break;
				count += this.getHiddenLineCountAboveImpl(child, comp, physical);
			}
		}

		return count;

	}

	@Override
	public int getLastVisibleLine() {

		int lastLine = this.textArea.getLineCount() - 1;

		if (this.isCodeFoldingSupportedAndEnabled()) {
			final int foldCount = this.getFoldCount();
			if (foldCount > 0) {
				Fold lastFold = this.getFold(foldCount - 1);
				if (lastFold.containsLine(lastLine))
					if (lastFold.isCollapsed())
						lastLine = lastFold.getStartLine();
					else
						while (lastFold.getHasChildFolds()) {
							lastFold = lastFold.getLastChild();
							if (lastFold.containsLine(lastLine)) {
								if (lastFold.isCollapsed()) {
									lastLine = lastFold.getStartLine();
									break;
								}
							} else
								break;
						}
			}
		}

		return lastLine;

	}

	@Override
	public int getVisibleLineAbove(int line) {

		if (line <= 0 || line >= this.textArea.getLineCount())
			return -1;

		do
			line--;
		while (line >= 0 && this.isLineHidden(line));

		return line;

	}

	// private static int binaryFindFoldContainingLine(int line) {
	//
	// List allFolds;
	//
	// int low = 0;
	// int high = allFolds.size() - 1;
	//
	// while (low <= high) {
	// int mid = (low + high) >> 1;
	// Fold midVal = (Fold)allFolds.get(mid);
	// if (midVal.containsLine(line)) {
	// return mid;
	// }
	// if (line<=midVal.getStartLine()) {
	// high = mid - 1;
	// }
	// else { // line > midVal.getEndLine()
	// low = mid + 1;
	// }
	// }
	//
	// return -(low + 1); // key not found
	//
	// }

	@Override
	public int getVisibleLineBelow(int line) {

		final int lineCount = this.textArea.getLineCount();
		if (line < 0 || line >= lineCount - 1)
			return -1;

		do
			line++;
		while (line < lineCount && this.isLineHidden(line));

		return line == lineCount ? -1 : line;

	}

	@Override
	public boolean isCodeFoldingEnabled() {
		return this.codeFoldingEnabled;
	}

	@Override
	public boolean isCodeFoldingSupportedAndEnabled() {
		return this.codeFoldingEnabled && this.foldParser != null;
	}

	@Override
	public boolean isFoldStartLine(final int line) {
		return this.getFoldForLine(line) != null;
	}

	@Override
	public boolean isLineHidden(final int line) {
		for (final Fold fold : this.folds)
			if (fold.containsLine(line))
				if (fold.isCollapsed())
					return true;
				else
					return this.isLineHiddenImpl(fold, line);
		return false;
	}

	private boolean isLineHiddenImpl(final Fold parent, final int line) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			final Fold child = parent.getChild(i);
			if (child.containsLine(line))
				if (child.isCollapsed())
					return true;
				else
					return this.isLineHiddenImpl(child, line);
		}
		return false;
	}

	private void keepFoldState(final Fold newFold, final List<Fold> oldFolds) {
		final int previousLoc = Collections.binarySearch(oldFolds, newFold);
		// System.out.println(newFold + " => " + previousLoc);
		if (previousLoc >= 0) {
			final Fold prevFold = oldFolds.get(previousLoc);
			newFold.setCollapsed(prevFold.isCollapsed());
		} else {
			// previousLoc = -(insertion point) - 1;
			final int insertionPoint = -(previousLoc + 1);
			if (insertionPoint > 0) {
				final Fold possibleParentFold = oldFolds.get(insertionPoint - 1);
				if (possibleParentFold.containsOffset(newFold.getStartOffset())) {
					final List<Fold> children = possibleParentFold.getChildren();
					if (children != null)
						this.keepFoldState(newFold, children);
				}
			}
		}
	}

	private void keepFoldStates(final List<Fold> newFolds, final List<Fold> oldFolds) {
		for (final Fold newFold : newFolds) {
			this.keepFoldState(newFold, this.folds);
			final List<Fold> newChildFolds = newFold.getChildren();
			if (newChildFolds != null)
				this.keepFoldStates(newChildFolds, oldFolds);
		}
	}

	@Override
	public void removePropertyChangeListener(final PropertyChangeListener l) {
		this.support.removePropertyChangeListener(l);
	}

	@Override
	public void reparse() {

		if (this.codeFoldingEnabled && this.foldParser != null) {

			// Re-calculate folds. Keep the fold state of folds that are
			// still around.
			List<Fold> newFolds = this.foldParser.getFolds(this.textArea);
			if (newFolds == null)
				newFolds = Collections.emptyList();
			else
				this.keepFoldStates(newFolds, this.folds);
			this.folds = newFolds;

			// Let folks (gutter, etc.) know that folds have been updated.
			this.support.firePropertyChange(FoldManager.PROPERTY_FOLDS_UPDATED, null, this.folds);
			this.textArea.repaint();

		} else
			this.folds.clear();

	}

	@Override
	public void setCodeFoldingEnabled(final boolean enabled) {
		if (enabled != this.codeFoldingEnabled) {
			this.codeFoldingEnabled = enabled;
			if (this.rstaParser != null)
				this.textArea.removeParser(this.rstaParser);
			if (enabled) {
				this.rstaParser = new AbstractParser() {
					@Override
					public ParseResult parse(final RSyntaxDocument doc, final String style) {
						DefaultFoldManager.this.reparse();
						return new DefaultParseResult(this);
					}
				};
				this.textArea.addParser(this.rstaParser);
				this.support.firePropertyChange(FoldManager.PROPERTY_FOLDS_UPDATED, null, null);
				// reparse();
			} else {
				this.folds = Collections.emptyList();
				this.textArea.repaint();
				this.support.firePropertyChange(FoldManager.PROPERTY_FOLDS_UPDATED, null, null);
			}
		}
	}

	@Override
	public void setFolds(final List<Fold> folds) {
		this.folds = folds;
	}

	/**
	 * Updates the fold parser to be the one appropriate for the language currently
	 * being highlighted.
	 */
	private void updateFoldParser() {
		this.foldParser = FoldParserManager.get().getFoldParser(this.textArea.getSyntaxEditingStyle());
	}

}