/*
 * 08/29/2004
 *
 * RSyntaxTextAreaEditorKit.java - The editor kit used by RSyntaxTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.text.CharacterIterator;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.TextAction;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldCollapser;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;
import org.fife.ui.rtextarea.IconRowHeader;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaEditorKit;
import org.fife.ui.rtextarea.RecordableTextAction;

/**
 * An extension of <code>RTextAreaEditorKit</code> that adds functionality for
 * programming-specific stuff. There are currently subclasses to handle:
 *
 * <ul>
 * <li>Toggling code folds.</li>
 * <li>Aligning "closing" curly braces with their matches, if the current
 * programming language uses curly braces to identify code blocks.</li>
 * <li>Copying the current selection as RTF.</li>
 * <li>Block indentation (increasing the indent of one or multiple lines)</li>
 * <li>Block un-indentation (decreasing the indent of one or multiple lines)
 * </li>
 * <li>Inserting a "code template" when a configurable key (e.g. a space) is
 * pressed</li>
 * <li>Decreasing the point size of all fonts in the text area</li>
 * <li>Increasing the point size of all fonts in the text area</li>
 * <li>Moving the caret to the "matching bracket" of the one at the current
 * caret position</li>
 * <li>Toggling whether the currently selected lines are commented out.</li>
 * <li>Better selection of "words" on mouse double-clicks for programming
 * languages.</li>
 * <li>Better keyboard navigation via Ctrl+arrow keys for programming
 * languages.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 0.5
 */
public class RSyntaxTextAreaEditorKit extends RTextAreaEditorKit {

	/**
	 * Positions the caret at the beginning of the word. This class is here to
	 * better handle finding the "beginning of the word" for programming languages.
	 */
	protected static class BeginWordAction extends RTextAreaEditorKit.BeginWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment seg;

		protected BeginWordAction(final String name, final boolean select) {
			super(name, select);
			this.seg = new Segment();
		}

		@Override
		protected int getWordStart(final RTextArea textArea, int offs) throws BadLocationException {

			if (offs == 0)
				return offs;

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final int line = textArea.getLineOfOffset(offs);
			final int start = textArea.getLineStartOffset(line);
			if (offs == start)
				return start;
			int end = textArea.getLineEndOffset(line);
			if (line != textArea.getLineCount() - 1)
				end--;
			doc.getText(start, end - start, this.seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other. We take special care here as we're starting
			// in the middle of the Segment to check whether we're already at
			// the "beginning" of a word.
			final int firstIndex = this.seg.getBeginIndex() + offs - start - 1;
			this.seg.setIndex(firstIndex);
			char ch = this.seg.current();
			final char nextCh = offs == end ? 0 : this.seg.array[this.seg.getIndex() + 1];

			// The "word" is a group of letters and/or digits
			final int languageIndex = 0; // TODO
			if (doc.isIdentifierChar(languageIndex, ch)) {
				if (offs != end && !doc.isIdentifierChar(languageIndex, nextCh))
					return offs;
				do
					ch = this.seg.previous();
				while (doc.isIdentifierChar(languageIndex, ch) && ch != CharacterIterator.DONE);
			}

			// The "word" is whitespace
			else if (Character.isWhitespace(ch)) {
				if (offs != end && !Character.isWhitespace(nextCh))
					return offs;
				do
					ch = this.seg.previous();
				while (Character.isWhitespace(ch));
			}

			// Otherwise, the "word" a single "something else" char (operator,
			// etc.).

			offs -= firstIndex - this.seg.getIndex() + 1;// seg.getEndIndex() - seg.getIndex();
			if (ch != CharacterIterator.DONE && nextCh != '\n')
				offs++;

			return offs;

		}

	}

	/**
	 * Expands or collapses the nearest fold.
	 */
	public static class ChangeFoldStateAction extends FoldRelatedAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private boolean collapse;

		public ChangeFoldStateAction(final String name, final boolean collapse) {
			super(name);
			this.collapse = collapse;
		}

		public ChangeFoldStateAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			if (rsta.isCodeFoldingEnabled()) {
				final Fold fold = this.getClosestFold(rsta);
				if (fold != null)
					fold.setCollapsed(this.collapse);
				RSyntaxUtilities.possiblyRepaintGutter(textArea);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action that (optionally) aligns a closing curly brace with the line
	 * containing its matching opening curly brace.
	 */
	public static class CloseCurlyBraceAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		private Point bracketInfo;
		private final Segment seg;

		public CloseCurlyBraceAction() {
			super(RSyntaxTextAreaEditorKit.rstaCloseCurlyBraceAction);
			this.seg = new Segment();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			final RSyntaxDocument doc = (RSyntaxDocument) rsta.getDocument();

			int languageIndex = 0;
			int dot = textArea.getCaretPosition();
			if (dot > 0) {
				final Token t = RSyntaxUtilities.getTokenAtOffset(rsta, dot - 1);
				languageIndex = t == null ? 0 : t.getLanguageIndex();
			}
			final boolean alignCurlyBraces = rsta.isAutoIndentEnabled()
					&& doc.getCurlyBracesDenoteCodeBlocks(languageIndex);

			if (alignCurlyBraces)
				textArea.beginAtomicEdit();

			try {

				textArea.replaceSelection("}");

				// If the user wants to align curly braces...
				if (alignCurlyBraces) {

					final Element root = doc.getDefaultRootElement();
					dot = rsta.getCaretPosition() - 1; // Start before '}'
					final int line = root.getElementIndex(dot);
					final Element elem = root.getElement(line);
					final int start = elem.getStartOffset();

					// Get the current line's text up to the '}' entered.
					try {
						doc.getText(start, dot - start, this.seg);
					} catch (final BadLocationException ble) { // Never happens
						ble.printStackTrace();
						return;
					}

					// Only attempt to align if there's only whitespace up to
					// the '}' entered.
					for (int i = 0; i < this.seg.count; i++) {
						final char ch = this.seg.array[this.seg.offset + i];
						if (!Character.isWhitespace(ch))
							return;
					}

					// Locate the matching '{' bracket, and replace the leading
					// whitespace for the '}' to match that of the '{' char's line.
					this.bracketInfo = RSyntaxUtilities.getMatchingBracketPosition(rsta, this.bracketInfo);
					if (this.bracketInfo.y > -1)
						try {
							final String ws = RSyntaxUtilities.getLeadingWhitespace(doc, this.bracketInfo.y);
							rsta.replaceRange(ws, start, dot);
						} catch (final BadLocationException ble) {
							ble.printStackTrace();
							return;
						}

				}

			} finally {
				if (alignCurlyBraces)
					textArea.endAtomicEdit();
			}

		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaCloseCurlyBraceAction;
		}

	}

	/**
	 * (Optionally) completes a closing markup tag.
	 */
	public static class CloseMarkupTagAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public CloseMarkupTagAction() {
			super(RSyntaxTextAreaEditorKit.rstaCloseMarkupTagAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			final RSyntaxDocument doc = (RSyntaxDocument) rsta.getDocument();

			final Caret c = rsta.getCaret();
			final boolean selection = c.getDot() != c.getMark();
			rsta.replaceSelection("/");

			// Don't automatically complete a tag if there was a selection
			final int dot = c.getDot();

			if (doc.getLanguageIsMarkup() && doc.getCompleteMarkupCloseTags() && !selection && rsta.getCloseMarkupTags()
					&& dot > 1)
				try {

					// Check actual char before token type, since it's quicker
					final char ch = doc.charAt(dot - 2);
					if (ch == '<' || ch == '[') {

						Token t = doc.getTokenListForLine(rsta.getCaretLineNumber());
						t = RSyntaxUtilities.getTokenAtOffset(t, dot - 1);
						if (t != null && t.getType() == TokenTypes.MARKUP_TAG_DELIMITER) {
							// System.out.println("Huzzah - closing tag!");
							final String tagName = this.discoverTagName(doc, dot);
							if (tagName != null)
								rsta.replaceSelection(tagName + (char) (ch + 2));
						}

					}

				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(rsta);
					ble.printStackTrace();
				}

		}

		/**
		 * Discovers the name of the tag being closed. Assumes standard SGML-style
		 * markup tags.
		 *
		 * @param doc
		 *            The document to parse.
		 * @param dot
		 *            The location of the caret. This should be right after the start of
		 *            a closing tag token (e.g. "<code>&lt;/</code>" or "<code>[</code>"
		 *            in the case of BBCode).
		 * @return The name of the tag to close, or <code>null</code> if it could not be
		 *         determined.
		 */
		private String discoverTagName(final RSyntaxDocument doc, final int dot) {

			final Stack<String> stack = new Stack<>();

			final Element root = doc.getDefaultRootElement();
			final int curLine = root.getElementIndex(dot);

			for (int i = 0; i <= curLine; i++) {

				Token t = doc.getTokenListForLine(i);
				while (t != null && t.isPaintable()) {

					if (t.getType() == TokenTypes.MARKUP_TAG_DELIMITER)
						if (t.isSingleChar('<') || t.isSingleChar('[')) {
							t = t.getNextToken();
							while (t != null && t.isPaintable()) {
								if (t.getType() == TokenTypes.MARKUP_TAG_NAME ||
								// Being lenient here and also checking
								// for attributes, in case they
								// (incorrectly) have whitespace between
								// the '<' char and the element name.
										t.getType() == TokenTypes.MARKUP_TAG_ATTRIBUTE) {
									stack.push(t.getLexeme());
									break;
								}
								t = t.getNextToken();
							}
						} else if (t.length() == 2 && t.charAt(0) == '/'
								&& (t.charAt(1) == '>' || t.charAt(1) == ']')) {
							if (!stack.isEmpty())
								stack.pop();
						} else if (t.length() == 2 && (t.charAt(0) == '<' || t.charAt(0) == '[')
								&& t.charAt(1) == '/') {
							String tagName = null;
							if (!stack.isEmpty())
								tagName = stack.pop();
							if (t.getEndOffset() >= dot)
								return tagName;
						}

					t = t == null ? null : t.getNextToken();

				}

			}

			return null; // Should never happen

		}

		@Override
		public String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Collapses all comment folds.
	 */
	public static class CollapseAllCommentFoldsAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public CollapseAllCommentFoldsAction() {
			super(RSyntaxTextAreaEditorKit.rstaCollapseAllCommentFoldsAction);
			this.setProperties(RSyntaxTextAreaEditorKit.msg, "Action.CollapseCommentFolds");
		}

		public CollapseAllCommentFoldsAction(final String name, final Icon icon, final String desc,
				final Integer mnemonic, final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			if (rsta.isCodeFoldingEnabled()) {
				final FoldCollapser collapser = new FoldCollapser();
				collapser.collapseFolds(rsta.getFoldManager());
				RSyntaxUtilities.possiblyRepaintGutter(textArea);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaCollapseAllCommentFoldsAction;
		}

	}

	/**
	 * Collapses all folds.
	 */
	public static class CollapseAllFoldsAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public CollapseAllFoldsAction() {
			this(false);
		}

		public CollapseAllFoldsAction(final boolean localizedName) {
			super(RSyntaxTextAreaEditorKit.rstaCollapseAllFoldsAction);
			if (localizedName)
				this.setProperties(RSyntaxTextAreaEditorKit.msg, "Action.CollapseAllFolds");
		}

		public CollapseAllFoldsAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			if (rsta.isCodeFoldingEnabled()) {
				final FoldCollapser collapser = new FoldCollapser() {
					@Override
					public boolean getShouldCollapse(final Fold fold) {
						return true;
					}
				};
				collapser.collapseFolds(rsta.getFoldManager());
				RSyntaxUtilities.possiblyRepaintGutter(textArea);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaCollapseAllFoldsAction;
		}

	}

	/**
	 * Action for copying text as RTF.
	 */
	public static class CopyAsRtfAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public CopyAsRtfAction() {
			super(RSyntaxTextAreaEditorKit.rstaCopyAsRtfAction);
		}

		public CopyAsRtfAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			((RSyntaxTextArea) textArea).copyAsRtf();
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action for decreasing the font size of all fonts in the text area.
	 */
	public static class DecreaseFontSizeAction extends RTextAreaEditorKit.DecreaseFontSizeAction {

		private static final long serialVersionUID = 1L;

		public DecreaseFontSizeAction() {
			super();
		}

		public DecreaseFontSizeAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			final SyntaxScheme scheme = rsta.getSyntaxScheme();

			// All we need to do is update all of the fonts in syntax
			// schemes, then call setSyntaxHighlightingColorScheme with the
			// same scheme already being used. This relies on the fact that
			// that method does not check whether the new scheme is different
			// from the old scheme before updating.

			boolean changed = false;
			final int count = scheme.getStyleCount();
			for (int i = 0; i < count; i++) {
				final Style ss = scheme.getStyle(i);
				if (ss != null) {
					final Font font = ss.font;
					if (font != null) {
						final float oldSize = font.getSize2D();
						final float newSize = oldSize - this.decreaseAmount;
						if (newSize >= DecreaseFontSizeAction.MINIMUM_SIZE) {
							// Shrink by decreaseAmount.
							ss.font = font.deriveFont(newSize);
							changed = true;
						} else if (oldSize > DecreaseFontSizeAction.MINIMUM_SIZE) {
							// Can't shrink by full decreaseAmount, but
							// can shrink a little bit.
							ss.font = font.deriveFont(DecreaseFontSizeAction.MINIMUM_SIZE);
							changed = true;
						}
					}
				}
			}

			// Do the text area's font also.
			final Font font = rsta.getFont();
			final float oldSize = font.getSize2D();
			final float newSize = oldSize - this.decreaseAmount;
			if (newSize >= DecreaseFontSizeAction.MINIMUM_SIZE) {
				// Shrink by decreaseAmount.
				rsta.setFont(font.deriveFont(newSize));
				changed = true;
			} else if (oldSize > DecreaseFontSizeAction.MINIMUM_SIZE) {
				// Can't shrink by full decreaseAmount, but
				// can shrink a little bit.
				rsta.setFont(font.deriveFont(DecreaseFontSizeAction.MINIMUM_SIZE));
				changed = true;
			}

			// If we updated at least one font, update the screen. If
			// all of the fonts were already the minimum size, beep.
			if (changed) {
				rsta.setSyntaxScheme(scheme);
				// NOTE: This is a hack to get an encompassing
				// RTextScrollPane to repaint its line numbers to account
				// for a change in line height due to a font change. I'm
				// not sure why we need to do this here but not when we
				// change the syntax highlighting color scheme via the
				// Options dialog... setSyntaxHighlightingColorScheme()
				// calls revalidate() which won't repaint the scroll pane
				// if scrollbars don't change, which is why we need this.
				Component parent = rsta.getParent();
				if (parent instanceof javax.swing.JViewport) {
					parent = parent.getParent();
					if (parent instanceof JScrollPane)
						parent.repaint();
				}
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);

		}

	}

	/**
	 * Action for when un-indenting lines (either the current line if there is
	 * selection, or all selected lines if there is one).
	 */
	public static class DecreaseIndentAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		private final Segment s;

		public DecreaseIndentAction() {
			this(RSyntaxTextAreaEditorKit.rstaDecreaseIndentAction);
		}

		public DecreaseIndentAction(final String name) {
			super(name);
			this.s = new Segment();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final Document document = textArea.getDocument();
			final Element map = document.getDefaultRootElement();
			final Caret c = textArea.getCaret();
			int dot = c.getDot();
			int mark = c.getMark();
			int line1 = map.getElementIndex(dot);
			final int tabSize = textArea.getTabSize();

			// If there is a selection, indent all lines in the selection.
			// Otherwise, indent the line the caret is on.
			if (dot != mark) {
				// Note that we cheaply reuse variables here, so don't
				// take their names to mean what they are.
				final int line2 = map.getElementIndex(mark);
				dot = Math.min(line1, line2);
				mark = Math.max(line1, line2);
				Element elem;
				textArea.beginAtomicEdit();
				try {
					for (line1 = dot; line1 < mark; line1++) {
						elem = map.getElement(line1);
						this.handleDecreaseIndent(elem, document, tabSize);
					}
					// Don't do the last line if the caret is at its
					// beginning. We must call getDot() again and not just
					// use 'dot' as the caret's position may have changed
					// due to the insertion of the tabs above.
					elem = map.getElement(mark);
					final int start = elem.getStartOffset();
					if (Math.max(c.getDot(), c.getMark()) != start)
						this.handleDecreaseIndent(elem, document, tabSize);
				} catch (final BadLocationException ble) {
					ble.printStackTrace();
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				} finally {
					textArea.endAtomicEdit();
				}
			} else {
				final Element elem = map.getElement(line1);
				try {
					this.handleDecreaseIndent(elem, document, tabSize);
				} catch (final BadLocationException ble) {
					ble.printStackTrace();
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				}
			}

		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaDecreaseIndentAction;
		}

		/**
		 * Actually does the "de-indentation." This method finds where the given
		 * element's leading whitespace ends, then, if there is indeed leading
		 * whitespace, removes either the last char in it (if it is a tab), or removes
		 * up to the number of spaces equal to a tab in the specified document (i.e., if
		 * the tab size was 5 and there were 3 spaces at the end of the leading
		 * whitespace, the three will be removed; if there were 8 spaces, only the first
		 * 5 would be removed).
		 *
		 * @param elem
		 *            The element to "de-indent."
		 * @param doc
		 *            The document containing the specified element.
		 * @param tabSize
		 *            The size of a tab, in spaces.
		 */
		private void handleDecreaseIndent(final Element elem, final Document doc, final int tabSize)
				throws BadLocationException {
			final int start = elem.getStartOffset();
			int end = elem.getEndOffset() - 1; // Why always true??
			doc.getText(start, end - start, this.s);
			int i = this.s.offset;
			end = i + this.s.count;
			if (end > i)
				// If the first character is a tab, remove it.
				if (this.s.array[i] == '\t')
					doc.remove(start, 1);
				else if (this.s.array[i] == ' ') {
					i++;
					int toRemove = 1;
					while (i < end && this.s.array[i] == ' ' && toRemove < tabSize) {
						i++;
						toRemove++;
					}
					doc.remove(start, toRemove);
				}
		}

	}

	/**
	 * Deletes the previous word, but differentiates symbols from "words" to match
	 * the behavior of code editors.
	 */
	public static class DeletePrevWordAction extends RTextAreaEditorKit.DeletePrevWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment seg = new Segment();

		@Override
		protected int getPreviousWordStart(final RTextArea textArea, int offs) throws BadLocationException {

			if (offs == 0)
				return offs;

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final int line = textArea.getLineOfOffset(offs);
			final int start = textArea.getLineStartOffset(line);
			if (offs == start)
				return start - 1; // Just delete the newline
			int end = textArea.getLineEndOffset(line);
			if (line != textArea.getLineCount() - 1)
				end--;
			doc.getText(start, end - start, this.seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other. We take special care here as we're starting
			// in the middle of the Segment to check whether we're already at
			// the "beginning" of a word.
			final int firstIndex = this.seg.getBeginIndex() + offs - start - 1;
			this.seg.setIndex(firstIndex);
			char ch = this.seg.current();

			// Always strip off whitespace first
			if (Character.isWhitespace(ch))
				do
					ch = this.seg.previous();
				while (Character.isWhitespace(ch));

			// The "word" is a group of letters and/or digits
			final int languageIndex = 0; // TODO
			if (doc.isIdentifierChar(languageIndex, ch))
				do
					ch = this.seg.previous();
				while (doc.isIdentifierChar(languageIndex, ch));
			else
				while (!Character.isWhitespace(ch) && !doc.isIdentifierChar(languageIndex, ch)
						&& ch != CharacterIterator.DONE)
					ch = this.seg.previous();

			if (ch == CharacterIterator.DONE)
				return start; // Removed last "token" of the line
			offs -= firstIndex - this.seg.getIndex();
			return offs;

		}

	}

	/**
	 * Overridden to use the programming language RSTA is displaying when computing
	 * words to complete.
	 */
	public static class DumbCompleteWordAction extends RTextAreaEditorKit.DumbCompleteWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private static int getWordStartImpl(final RSyntaxDocument doc, final Element elem, final int offs)
				throws BadLocationException {

			final int start = elem.getStartOffset();

			int wordStart = offs;
			while (wordStart >= start) {
				final char ch = doc.charAt(wordStart);
				// Ignore newlines so we work when caret is at end of line
				if (!DumbCompleteWordAction.isIdentifierChar(ch) && ch != '\n')
					break;
				wordStart--;
			}

			return wordStart == offs ? offs : wordStart + 1;

		}

		/**
		 * Returns whether the specified character should be considered part of an
		 * identifier.
		 *
		 * @param ch
		 *            The character.
		 * @return Whether the character is part of an identifier.
		 */
		private static boolean isIdentifierChar(final char ch) {
			// return doc.isIdentifierChar(languageIndex, ch);
			return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
		}

		@Override
		protected int getPreviousWord(final RTextArea textArea, int offs) throws BadLocationException {

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			int line = root.getElementIndex(offs);
			Element elem = root.getElement(line);

			// If caret is at the beginning of a word, we should return the
			// previous word
			final int start = elem.getStartOffset();
			if (offs > start) {
				final char ch = doc.charAt(offs);
				if (DumbCompleteWordAction.isIdentifierChar(ch))
					offs--;
			} else { // offs == start => previous word is on previous line
				if (line == 0)
					return -1;
				elem = root.getElement(--line);
				offs = elem.getEndOffset() - 1;
			}

			int prevWordStart = this.getPreviousWordStartInLine(doc, elem, offs);
			while (prevWordStart == -1 && line > 0) {
				line--;
				elem = root.getElement(line);
				prevWordStart = this.getPreviousWordStartInLine(doc, elem, elem.getEndOffset());
			}

			return prevWordStart;

		}

		private int getPreviousWordStartInLine(final RSyntaxDocument doc, final Element elem, final int offs)
				throws BadLocationException {

			final int start = elem.getStartOffset();
			int cur = offs;

			// Skip any whitespace or non-word chars
			while (cur >= start) {
				final char ch = doc.charAt(cur);
				if (DumbCompleteWordAction.isIdentifierChar(ch))
					break;
				cur--;
			}
			if (cur < start)
				// Empty line or nothing but whitespace/non-word chars
				return -1;

			return DumbCompleteWordAction.getWordStartImpl(doc, elem, cur);

		}

		@Override
		protected int getWordEnd(final RTextArea textArea, final int offs) throws BadLocationException {

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			final int line = root.getElementIndex(offs);
			final Element elem = root.getElement(line);
			final int end = elem.getEndOffset() - 1;

			int wordEnd = offs;
			while (wordEnd <= end) {
				if (!DumbCompleteWordAction.isIdentifierChar(doc.charAt(wordEnd)))
					break;
				wordEnd++;
			}

			return wordEnd;

		}

		@Override
		protected int getWordStart(final RTextArea textArea, final int offs) throws BadLocationException {
			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			final int line = root.getElementIndex(offs);
			final Element elem = root.getElement(line);
			return DumbCompleteWordAction.getWordStartImpl(doc, elem, offs);
		}

		/**
		 * Overridden to not suggest word completions if the text right before the caret
		 * contains non-word characters, such as '/' or '%'.
		 *
		 * @param prefix
		 *            The prefix characters before the caret.
		 * @return Whether the prefix could be part of a "word" in the context of the
		 *         text area's current content.
		 */
		@Override
		protected boolean isAcceptablePrefix(final String prefix) {
			return prefix.length() > 0 && DumbCompleteWordAction.isIdentifierChar(prefix.charAt(prefix.length() - 1));
		}

	}

	/**
	 * Positions the caret at the end of the word. This class is here to better
	 * handle finding the "end of the word" in programming languages.
	 */
	protected static class EndWordAction extends RTextAreaEditorKit.EndWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment seg;

		protected EndWordAction(final String name, final boolean select) {
			super(name, select);
			this.seg = new Segment();
		}

		@Override
		protected int getWordEnd(final RTextArea textArea, int offs) throws BadLocationException {

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			if (offs == doc.getLength())
				return offs;

			final int line = textArea.getLineOfOffset(offs);
			int end = textArea.getLineEndOffset(line);
			if (line != textArea.getLineCount() - 1)
				end--; // Hide newline
			if (offs == end)
				return end;
			doc.getText(offs, end - offs, this.seg);

			// Determine the "type" of char at offs - letter/digit,
			// whitespace or other
			char ch = this.seg.first();

			// The "word" is a group of letters and/or digits
			final int languageIndex = 0; // TODO
			if (doc.isIdentifierChar(languageIndex, ch))
				do
					ch = this.seg.next();
				while (doc.isIdentifierChar(languageIndex, ch) && ch != CharacterIterator.DONE);
			else if (Character.isWhitespace(ch))
				do
					ch = this.seg.next();
				while (Character.isWhitespace(ch));

			// Otherwise, the "word" is a single character of some other type
			// (operator, etc.).

			offs += this.seg.getIndex() - this.seg.getBeginIndex();
			return offs;

		}

	}

	/**
	 * Expands all folds.
	 */
	public static class ExpandAllFoldsAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public ExpandAllFoldsAction() {
			this(false);
		}

		public ExpandAllFoldsAction(final boolean localizedName) {
			super(RSyntaxTextAreaEditorKit.rstaExpandAllFoldsAction);
			if (localizedName)
				this.setProperties(RSyntaxTextAreaEditorKit.msg, "Action.ExpandAllFolds");
		}

		public ExpandAllFoldsAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			if (rsta.isCodeFoldingEnabled()) {
				final FoldManager fm = rsta.getFoldManager();
				for (int i = 0; i < fm.getFoldCount(); i++)
					this.expand(fm.getFold(i));
				RSyntaxUtilities.possiblyRepaintGutter(rsta);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		private void expand(final Fold fold) {
			fold.setCollapsed(false);
			for (int i = 0; i < fold.getChildCount(); i++)
				this.expand(fold.getChild(i));
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaExpandAllFoldsAction;
		}

	}

	/**
	 * Base class for folding-related actions.
	 */
	abstract static class FoldRelatedAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		FoldRelatedAction(final String name) {
			super(name);
		}

		FoldRelatedAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		protected Fold getClosestFold(final RSyntaxTextArea textArea) {
			final int offs = textArea.getCaretPosition();
			final int line = textArea.getCaretLineNumber();
			final FoldManager fm = textArea.getFoldManager();
			Fold fold = fm.getFoldForLine(line);
			if (fold == null)
				fold = fm.getDeepestOpenFoldContaining(offs);
			return fold;
		}

	}

	/**
	 * Action for moving the caret to the "matching bracket" of the bracket at the
	 * caret position (either before or after).
	 */
	public static class GoToMatchingBracketAction extends RecordableTextAction {

		/**
		 * Moves the caret to the end of the document, taking into account code folding.
		 */
		public static class EndAction extends RTextAreaEditorKit.EndAction {

			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			public EndAction(final String name, final boolean select) {
				super(name, select);
			}

			@Override
			protected int getVisibleEnd(final RTextArea textArea) {
				final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
				return rsta.getLastVisibleOffset();
			}

		}

		private static final long serialVersionUID = 1L;

		private Point bracketInfo;

		public GoToMatchingBracketAction() {
			super(RSyntaxTextAreaEditorKit.rstaGoToMatchingBracketAction);
		}

		public GoToMatchingBracketAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			this.bracketInfo = RSyntaxUtilities.getMatchingBracketPosition(rsta, this.bracketInfo);
			if (this.bracketInfo.y > -1)
				// Go to the position AFTER the bracket so the previous
				// bracket (which we were just on) is highlighted.
				rsta.setCaretPosition(this.bracketInfo.y + 1);
			else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaGoToMatchingBracketAction;
		}

	}

	/**
	 * Action for increasing the font size of all fonts in the text area.
	 */
	public static class IncreaseFontSizeAction extends RTextAreaEditorKit.IncreaseFontSizeAction {

		private static final long serialVersionUID = 1L;

		public IncreaseFontSizeAction() {
			super();
		}

		public IncreaseFontSizeAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			final SyntaxScheme scheme = rsta.getSyntaxScheme();

			// All we need to do is update all of the fonts in syntax
			// schemes, then call setSyntaxHighlightingColorScheme with the
			// same scheme already being used. This relies on the fact that
			// that method does not check whether the new scheme is different
			// from the old scheme before updating.

			boolean changed = false;
			final int count = scheme.getStyleCount();
			for (int i = 0; i < count; i++) {
				final Style ss = scheme.getStyle(i);
				if (ss != null) {
					final Font font = ss.font;
					if (font != null) {
						final float oldSize = font.getSize2D();
						final float newSize = oldSize + this.increaseAmount;
						if (newSize <= IncreaseFontSizeAction.MAXIMUM_SIZE) {
							// Grow by increaseAmount.
							ss.font = font.deriveFont(newSize);
							changed = true;
						} else if (oldSize < IncreaseFontSizeAction.MAXIMUM_SIZE) {
							// Can't grow by full increaseAmount, but
							// can grow a little bit.
							ss.font = font.deriveFont(IncreaseFontSizeAction.MAXIMUM_SIZE);
							changed = true;
						}
					}
				}
			}

			// Do the text area's font also.
			final Font font = rsta.getFont();
			final float oldSize = font.getSize2D();
			final float newSize = oldSize + this.increaseAmount;
			if (newSize <= IncreaseFontSizeAction.MAXIMUM_SIZE) {
				// Grow by increaseAmount.
				rsta.setFont(font.deriveFont(newSize));
				changed = true;
			} else if (oldSize < IncreaseFontSizeAction.MAXIMUM_SIZE) {
				// Can't grow by full increaseAmount, but
				// can grow a little bit.
				rsta.setFont(font.deriveFont(IncreaseFontSizeAction.MAXIMUM_SIZE));
				changed = true;
			}

			// If we updated at least one font, update the screen. If
			// all of the fonts were already the minimum size, beep.
			if (changed) {
				rsta.setSyntaxScheme(scheme);
				// NOTE: This is a hack to get an encompassing
				// RTextScrollPane to repaint its line numbers to account
				// for a change in line height due to a font change. I'm
				// not sure why we need to do this here but not when we
				// change the syntax highlighting color scheme via the
				// Options dialog... setSyntaxHighlightingColorScheme()
				// calls revalidate() which won't repaint the scroll pane
				// if scrollbars don't change, which is why we need this.
				Component parent = rsta.getParent();
				if (parent instanceof javax.swing.JViewport) {
					parent = parent.getParent();
					if (parent instanceof JScrollPane)
						parent.repaint();
				}
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);

		}

	}

	/**
	 * Action for when the user presses the Enter key. This is here so we can be
	 * smart and "auto-indent" for programming languages.
	 */
	public static class InsertBreakAction extends RTextAreaEditorKit.InsertBreakAction {

		private static final long serialVersionUID = 1L;

		/**
		 * @return The first location in the string past <code>pos</code> that is NOT a
		 *         whitespace char, or <code>-1</code> if only whitespace chars follow
		 *         <code>pos</code> (or it is the end position in the string).
		 */
		private static int atEndOfLine(final int pos, final String s, final int sLen) {
			for (int i = pos; i < sLen; i++)
				if (!RSyntaxUtilities.isWhitespace(s.charAt(i)))
					return i;
			return -1;
		}

		private static int getOpenBraceCount(final RSyntaxDocument doc, final int languageIndex) {
			int openCount = 0;
			for (final Token t : doc)
				if (t.getType() == TokenTypes.SEPARATOR && t.length() == 1 && t.getLanguageIndex() == languageIndex) {
					final char ch = t.charAt(0);
					if (ch == '{')
						openCount++;
					else if (ch == '}')
						openCount--;
				}
			return openCount;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final RSyntaxTextArea sta = (RSyntaxTextArea) textArea;
			final boolean noSelection = sta.getSelectionStart() == sta.getSelectionEnd();

			// First, see if this language wants to handle inserting newlines
			// itself.
			boolean handled = false;
			if (noSelection) {
				final RSyntaxDocument doc = (RSyntaxDocument) sta.getDocument();
				handled = doc.insertBreakSpecialHandling(e);
			}

			// If not...
			if (!handled)
				this.handleInsertBreak(sta, noSelection);

		}

		/**
		 * Actually inserts the newline into the document, and auto-indents if
		 * appropriate. This method can be called by token makers who implement a custom
		 * action for inserting newlines.
		 *
		 * @param textArea
		 * @param noSelection
		 *            Whether there is no selection.
		 */
		protected void handleInsertBreak(final RSyntaxTextArea textArea, final boolean noSelection) {
			// If we're auto-indenting...
			if (noSelection && textArea.isAutoIndentEnabled())
				this.insertNewlineWithAutoIndent(textArea);
			else {
				textArea.replaceSelection("\n");
				if (noSelection)
					this.possiblyCloseCurlyBrace(textArea, null);
			}
		}

		private void insertNewlineWithAutoIndent(final RSyntaxTextArea sta) {

			try {

				final int caretPos = sta.getCaretPosition();
				final Document doc = sta.getDocument();
				final Element map = doc.getDefaultRootElement();
				final int lineNum = map.getElementIndex(caretPos);
				final Element line = map.getElement(lineNum);
				final int start = line.getStartOffset();
				final int end = line.getEndOffset() - 1; // Why always "-1"?
				final int len = end - start;
				final String s = doc.getText(start, len);

				// endWS is the end of the leading whitespace of the
				// current line.
				final String leadingWS = RSyntaxUtilities.getLeadingWhitespace(s);
				final StringBuilder sb = new StringBuilder("\n");
				sb.append(leadingWS);

				// If there is only whitespace between the caret and
				// the EOL, pressing Enter auto-indents the new line to
				// the same place as the previous line.
				final int nonWhitespacePos = InsertBreakAction.atEndOfLine(caretPos - start, s, len);
				if (nonWhitespacePos == -1) {
					if (leadingWS.length() == len && sta.isClearWhitespaceLinesEnabled()) {
						// If the line was nothing but whitespace, select it
						// so its contents get removed.
						sta.setSelectionStart(start);
						sta.setSelectionEnd(end);
					}
					sta.replaceSelection(sb.toString());
				}

				// If there is non-whitespace between the caret and the
				// EOL, pressing Enter takes that text to the next line
				// and auto-indents it to the same place as the last
				// line.
				else {
					sb.append(s.substring(nonWhitespacePos));
					sta.replaceRange(sb.toString(), caretPos, end);
					sta.setCaretPosition(caretPos + leadingWS.length() + 1);
				}

				// Must do it after everything else, as the "smart indent"
				// calculation depends on the previous line's state
				// AFTER the Enter press (stuff may have been moved down).
				if (sta.getShouldIndentNextLine(lineNum))
					sta.replaceSelection("\t");

				this.possiblyCloseCurlyBrace(sta, leadingWS);

			} catch (final BadLocationException ble) { // Never happens
				sta.replaceSelection("\n");
				ble.printStackTrace();
			}

		}

		private void possiblyCloseCurlyBrace(final RSyntaxTextArea textArea, final String leadingWS) {

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();

			if (textArea.getCloseCurlyBraces()) {

				final int line = textArea.getCaretLineNumber();
				Token t = doc.getTokenListForLine(line - 1);
				t = t.getLastNonCommentNonWhitespaceToken();

				if (t != null && t.isLeftCurly()) {

					final int languageIndex = t.getLanguageIndex();
					if (doc.getCurlyBracesDenoteCodeBlocks(languageIndex)
							&& InsertBreakAction.getOpenBraceCount(doc, languageIndex) > 0) {
						final StringBuilder sb = new StringBuilder();
						if (line == textArea.getLineCount() - 1)
							sb.append('\n');
						if (leadingWS != null)
							sb.append(leadingWS);
						sb.append("}\n");
						final int dot = textArea.getCaretPosition();
						final int end = textArea.getLineEndOffsetOfCurrentLine();
						// Insert at end of line, not at dot: they may have
						// pressed Enter in the middle of the line and brought
						// some text (though it must be whitespace and/or
						// comments) down onto the new line.
						textArea.insert(sb.toString(), end);
						textArea.setCaretPosition(dot); // Caret may have moved
					}

				}

			}

		}

	}

	/**
	 * Action for inserting tabs. This is extended to "block indent" a group of
	 * contiguous lines if they are selected.
	 */
	public static class InsertTabAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public InsertTabAction() {
			super(DefaultEditorKit.insertTabAction);
		}

		public InsertTabAction(final String name) {
			super(name);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final Document document = textArea.getDocument();
			final Element map = document.getDefaultRootElement();
			final Caret c = textArea.getCaret();
			final int dot = c.getDot();
			final int mark = c.getMark();
			final int dotLine = map.getElementIndex(dot);
			final int markLine = map.getElementIndex(mark);

			// If there is a multi-line selection, indent all lines in
			// the selection.
			if (dotLine != markLine) {
				final int first = Math.min(dotLine, markLine);
				final int last = Math.max(dotLine, markLine);
				Element elem;
				int start;

				// Since we're using Document.insertString(), we must mimic the
				// soft tab behavior provided by RTextArea.replaceSelection().
				String replacement = "\t";
				if (textArea.getTabsEmulated()) {
					final StringBuilder sb = new StringBuilder();
					final int temp = textArea.getTabSize();
					for (int i = 0; i < temp; i++)
						sb.append(' ');
					replacement = sb.toString();
				}

				textArea.beginAtomicEdit();
				try {
					for (int i = first; i < last; i++) {
						elem = map.getElement(i);
						start = elem.getStartOffset();
						document.insertString(start, replacement, null);
					}
					// Don't do the last line if the caret is at its
					// beginning. We must call getDot() again and not just
					// use 'dot' as the caret's position may have changed
					// due to the insertion of the tabs above.
					elem = map.getElement(last);
					start = elem.getStartOffset();
					if (Math.max(c.getDot(), c.getMark()) != start)
						document.insertString(start, replacement, null);
				} catch (final BadLocationException ble) { // Never happens.
					ble.printStackTrace();
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				} finally {
					textArea.endAtomicEdit();
				}
			} else
				textArea.replaceSelection("\t");

		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.insertTabAction;
		}

	}

	/**
	 * Action to move the selection and/or caret. Constructor indicates direction to
	 * use. This class overrides the behavior defined in {@link RTextAreaEditorKit}
	 * to better skip "words" in source code.
	 */
	public static class NextWordAction extends RTextAreaEditorKit.NextWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment seg;

		public NextWordAction(final String nm, final boolean select) {
			super(nm, select);
			this.seg = new Segment();
		}

		/**
		 * Overridden to do better with skipping "words" in code.
		 */
		@Override
		protected int getNextWord(final RTextArea textArea, int offs) throws BadLocationException {

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			if (offs == doc.getLength())
				return offs;

			final Element root = doc.getDefaultRootElement();
			int line = root.getElementIndex(offs);
			final int end = root.getElement(line).getEndOffset() - 1;
			if (offs == end) {// If we're already at the end of the line...
				final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
				if (rsta.isCodeFoldingEnabled()) { // Start of next visible line
					final FoldManager fm = rsta.getFoldManager();
					final int lineCount = root.getElementCount();
					while (++line < lineCount && fm.isLineHidden(line))
						;
					if (line < lineCount)
						offs = root.getElement(line).getStartOffset();
					// No lower visible line - we're already at last visible offset
					return offs;
				} else
					return offs + 1; // Start of next line.
			}
			doc.getText(offs, end - offs, this.seg);

			// Determine the "type" of char at offs - letter/digit,
			// whitespace or other
			char ch = this.seg.first();

			// Skip the group of letters and/or digits
			final int languageIndex = 0;
			if (doc.isIdentifierChar(languageIndex, ch))
				do
					ch = this.seg.next();
				while (doc.isIdentifierChar(languageIndex, ch) && ch != CharacterIterator.DONE);
			else if (!Character.isWhitespace(ch))
				do
					ch = this.seg.next();
				while (ch != CharacterIterator.DONE
						&& !(doc.isIdentifierChar(languageIndex, ch) || Character.isWhitespace(ch)));

			// Skip any trailing whitespace
			while (Character.isWhitespace(ch))
				ch = this.seg.next();

			offs += this.seg.getIndex() - this.seg.getBeginIndex();
			return offs;

		}

	}

	/**
	 * Action for when the user tries to insert a template (that is, they've typed a
	 * template ID and pressed the trigger character (a space) in an attempt to do
	 * the substitution).
	 */
	public static class PossiblyInsertTemplateAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public PossiblyInsertTemplateAction() {
			super(RSyntaxTextAreaEditorKit.rstaPossiblyInsertTemplateAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled())
				return;

			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;

			if (RSyntaxTextArea.getTemplatesEnabled()) {

				final Document doc = textArea.getDocument();
				if (doc != null)
					try {

						final CodeTemplateManager manager = RSyntaxTextArea.getCodeTemplateManager();
						final CodeTemplate template = manager == null ? null : manager.getTemplate(rsta);

						// A non-null template means modify the text to insert!
						if (template != null)
							template.invoke(rsta);
						else
							this.doDefaultInsert(rsta);

					} catch (final BadLocationException ble) {
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					}

			} // End of if (textArea.getTemplatesEnabled()).
			else
				this.doDefaultInsert(rsta);

		}

		private void doDefaultInsert(final RTextArea textArea) {
			// FIXME: We need a way to get the "trigger string" (i.e.,
			// the text that was just typed); however, the text area's
			// template manager might be null (if templates are disabled).
			// Also, the manager's trigger string doesn't yet match up with
			// that defined in RSyntaxTextAreaEditorKit.java (which is
			// hardcoded as a space)...
			// String str = manager.getInsertTriggerString();
			// int mod = manager.getInsertTrigger().getModifiers();
			// if (str!=null && str.length()>0 &&
			// ((mod&ActionEvent.ALT_MASK)==(mod&ActionEvent.CTRL_MASK))) {
			// char ch = str.charAt(0);
			// if (ch>=0x20 && ch!=0x7F)
			// textArea.replaceSelection(str);
			// }
			textArea.replaceSelection(" ");
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaPossiblyInsertTemplateAction;
		}

	}

	/**
	 * Action to move the selection and/or caret. Constructor indicates direction to
	 * use. This class overrides the behavior defined in {@link RTextAreaEditorKit}
	 * to better skip "words" in source code.
	 */
	public static class PreviousWordAction extends RTextAreaEditorKit.PreviousWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment seg;

		public PreviousWordAction(final String nm, final boolean select) {
			super(nm, select);
			this.seg = new Segment();
		}

		/**
		 * Overridden to do better with skipping "words" in code.
		 */
		@Override
		protected int getPreviousWord(final RTextArea textArea, int offs) throws BadLocationException {

			if (offs == 0)
				return offs;

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			int line = root.getElementIndex(offs);
			final int start = root.getElement(line).getStartOffset();
			if (offs == start) {// If we're already at the start of the line...
				final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
				if (rsta.isCodeFoldingEnabled()) { // End of next visible line
					final FoldManager fm = rsta.getFoldManager();
					while (--line >= 0 && fm.isLineHidden(line))
						;
					if (line >= 0)
						offs = root.getElement(line).getEndOffset() - 1;
					// No earlier visible line - we must be at offs==0...
					return offs;
				} else
					return start - 1; // End of previous line.
			}
			doc.getText(start, offs - start, this.seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other
			char ch = this.seg.last();

			// Skip any "leading" whitespace
			while (Character.isWhitespace(ch))
				ch = this.seg.previous();

			// Skip the group of letters and/or digits
			final int languageIndex = 0;
			if (doc.isIdentifierChar(languageIndex, ch))
				do
					ch = this.seg.previous();
				while (doc.isIdentifierChar(languageIndex, ch) && ch != CharacterIterator.DONE);
			else if (!Character.isWhitespace(ch))
				do
					ch = this.seg.previous();
				while (ch != CharacterIterator.DONE
						&& !(doc.isIdentifierChar(languageIndex, ch) || Character.isWhitespace(ch)));

			offs -= this.seg.getEndIndex() - this.seg.getIndex();
			if (ch != CharacterIterator.DONE)
				offs++;

			return offs;

		}

	}

	/**
	 * Selects the word around the caret. This class is here to better handle
	 * selecting "words" in programming languages.
	 */
	public static class SelectWordAction extends RTextAreaEditorKit.SelectWordAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		protected void createActions() {
			this.start = new BeginWordAction("pigdog", false);
			this.end = new EndWordAction("pigdog", true);
		}

	}

	/**
	 * Action that toggles whether the currently selected lines are commented.
	 */
	public static class ToggleCommentAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ToggleCommentAction() {
			super(RSyntaxTextAreaEditorKit.rstaToggleCommentAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();
			final Element map = doc.getDefaultRootElement();
			final Caret c = textArea.getCaret();
			final int dot = c.getDot();
			final int mark = c.getMark();
			int line1 = map.getElementIndex(dot);
			final int line2 = map.getElementIndex(mark);
			final int start = Math.min(line1, line2);
			int end = Math.max(line1, line2);

			final Token t = doc.getTokenListForLine(start);
			final int languageIndex = t != null ? t.getLanguageIndex() : 0;
			final String[] startEnd = doc.getLineCommentStartAndEnd(languageIndex);

			if (startEnd == null) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			// Don't toggle comment on last line if there is no
			// text selected on it.
			if (start != end) {
				final Element elem = map.getElement(end);
				if (Math.max(dot, mark) == elem.getStartOffset())
					end--;
			}

			textArea.beginAtomicEdit();
			try {
				final boolean add = this.getDoAdd(doc, map, start, end, startEnd);
				for (line1 = start; line1 <= end; line1++) {
					final Element elem = map.getElement(line1);
					this.handleToggleComment(elem, doc, startEnd, add);
				}
			} catch (final BadLocationException ble) {
				ble.printStackTrace();
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			} finally {
				textArea.endAtomicEdit();
			}

		}

		private boolean getDoAdd(final Document doc, final Element map, final int startLine, final int endLine,
				final String[] startEnd) throws BadLocationException {
			boolean doAdd = false;
			for (int i = startLine; i <= endLine; i++) {
				final Element elem = map.getElement(i);
				final int start = elem.getStartOffset();
				final String t = doc.getText(start, elem.getEndOffset() - start - 1);
				if (!t.startsWith(startEnd[0]) || startEnd[1] != null && !t.endsWith(startEnd[1])) {
					doAdd = true;
					break;
				}
			}
			return doAdd;
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaToggleCommentAction;
		}

		private void handleToggleComment(final Element elem, final Document doc, final String[] startEnd,
				final boolean add) throws BadLocationException {
			final int start = elem.getStartOffset();
			final int end = elem.getEndOffset() - 1;
			if (add) {
				doc.insertString(start, startEnd[0], null);
				if (startEnd[1] != null)
					doc.insertString(end + startEnd[0].length(), startEnd[1], null);
			} else {
				doc.remove(start, startEnd[0].length());
				if (startEnd[1] != null) {
					final int temp = startEnd[1].length();
					doc.remove(end - startEnd[0].length() - temp, temp);
				}
			}
		}

	}

	/**
	 * Toggles the fold at the current caret position or line.
	 */
	public static class ToggleCurrentFoldAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public ToggleCurrentFoldAction() {
			super(RSyntaxTextAreaEditorKit.rstaToggleCurrentFoldAction);
			this.setProperties(RSyntaxTextAreaEditorKit.msg, "Action.ToggleCurrentFold");
		}

		public ToggleCurrentFoldAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
			if (rsta.isCodeFoldingEnabled()) {
				final Fold fold = this.getClosestFold(rsta);
				if (fold != null)
					fold.toggleCollapsedState();
				RSyntaxUtilities.possiblyRepaintGutter(textArea);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
		}

		@Override
		public final String getMacroID() {
			return RSyntaxTextAreaEditorKit.rstaToggleCurrentFoldAction;
		}

	}

	/**
	 * The actions that <code>RSyntaxTextAreaEditorKit</code> adds to those of
	 * <code>RTextAreaEditorKit</code>.
	 */
	private static final Action[] defaultActions = { new CloseCurlyBraceAction(), new CloseMarkupTagAction(),
			new BeginWordAction(DefaultEditorKit.beginWordAction, false),
			new BeginWordAction(DefaultEditorKit.selectionBeginWordAction, true),
			new ChangeFoldStateAction(RSyntaxTextAreaEditorKit.rstaCollapseFoldAction, true),
			new ChangeFoldStateAction(RSyntaxTextAreaEditorKit.rstaExpandFoldAction, false),
			new CollapseAllFoldsAction(), new CopyAsRtfAction(),
			// new DecreaseFontSizeAction(),
			new DecreaseIndentAction(), new DeletePrevWordAction(), new DumbCompleteWordAction(),
			new EndAction(DefaultEditorKit.endAction, false), new EndAction(DefaultEditorKit.selectionEndAction, true),
			new EndWordAction(DefaultEditorKit.endWordAction, false),
			new EndWordAction(DefaultEditorKit.endWordAction, true), new ExpandAllFoldsAction(),
			new GoToMatchingBracketAction(), new InsertBreakAction(),
			// new IncreaseFontSizeAction(),
			new InsertTabAction(), new NextWordAction(DefaultEditorKit.nextWordAction, false),
			new NextWordAction(DefaultEditorKit.selectionNextWordAction, true), new PossiblyInsertTemplateAction(),
			new PreviousWordAction(DefaultEditorKit.previousWordAction, false),
			new PreviousWordAction(DefaultEditorKit.selectionPreviousWordAction, true), new SelectWordAction(),
			new ToggleCommentAction(), };

	private static final ResourceBundle msg = ResourceBundle.getBundle(RSyntaxTextAreaEditorKit.MSG);

	private static final String MSG = "org.fife.ui.rsyntaxtextarea.RSyntaxTextArea";

	public static final String rstaCloseCurlyBraceAction = "RSTA.CloseCurlyBraceAction";

	public static final String rstaCloseMarkupTagAction = "RSTA.CloseMarkupTagAction";

	public static final String rstaCollapseAllCommentFoldsAction = "RSTA.CollapseAllCommentFoldsAction";

	public static final String rstaCollapseAllFoldsAction = "RSTA.CollapseAllFoldsAction";

	public static final String rstaCollapseFoldAction = "RSTA.CollapseFoldAction";

	public static final String rstaCopyAsRtfAction = "RSTA.CopyAsRtfAction";

	public static final String rstaDecreaseIndentAction = "RSTA.DecreaseIndentAction";

	public static final String rstaExpandAllFoldsAction = "RSTA.ExpandAllFoldsAction";

	public static final String rstaExpandFoldAction = "RSTA.ExpandFoldAction";

	public static final String rstaGoToMatchingBracketAction = "RSTA.GoToMatchingBracketAction";

	public static final String rstaPossiblyInsertTemplateAction = "RSTA.TemplateAction";

	public static final String rstaToggleCommentAction = "RSTA.ToggleCommentAction";

	public static final String rstaToggleCurrentFoldAction = "RSTA.ToggleCurrentFoldAction";

	private static final long serialVersionUID = 1L;

	/**
	 * Returns localized text for an action. There's definitely a better place for
	 * this functionality.
	 *
	 * @param key
	 *            The key into the action resource bundle.
	 * @return The localized text.
	 */
	public static String getString(final String key) {
		return RSyntaxTextAreaEditorKit.msg.getString(key);
	}

	/**
	 * Constructor.
	 */
	public RSyntaxTextAreaEditorKit() {
	}

	/**
	 * Returns the default document used by <code>RSyntaxTextArea</code>s.
	 *
	 * @return The document.
	 */
	@Override
	public Document createDefaultDocument() {
		return new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE);
	}

	/**
	 * Overridden to return a row header that is aware of folding.
	 *
	 * @param textArea
	 *            The text area.
	 * @return The icon row header.
	 */
	@Override
	public IconRowHeader createIconRowHeader(final RTextArea textArea) {
		return new FoldingAwareIconRowHeader((RSyntaxTextArea) textArea);
	}

	/**
	 * Fetches the set of commands that can be used on a text component that is
	 * using a model and view produced by this kit.
	 *
	 * @return the command list
	 */
	@Override
	public Action[] getActions() {
		return TextAction.augmentList(super.getActions(), RSyntaxTextAreaEditorKit.defaultActions);
	}

}