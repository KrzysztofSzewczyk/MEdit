/*
 * 08/13/2004
 *
 * RTextAreaEditorKit.java - The editor kit used by RTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Reader;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;

/**
 * An extension of <code>DefaultEditorKit</code> that adds functionality found
 * in <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 0.1
 */
// FIXME: Replace Utilities calls with custom versions (in RSyntaxUtilities) to
// cut down on all of the modelToViews, as each call causes
// a getTokenList => expensive!
@SuppressWarnings({ "checkstyle:constantname" })
public class RTextAreaEditorKit extends DefaultEditorKit {

	/**
	 * Creates a beep.
	 */
	public static class BeepAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public BeepAction() {
			super(DefaultEditorKit.beepAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			UIManager.getLookAndFeel().provideErrorFeedback(textArea);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.beepAction;
		}

	}

	/**
	 * Moves the caret to the beginning of the document.
	 */
	public static class BeginAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		public BeginAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (this.select)
				textArea.moveCaretPosition(0);
			else
				textArea.setCaretPosition(0);
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Toggles the position of the caret between the beginning of the line, and the
	 * first non-whitespace character on the line.
	 */
	public static class BeginLineAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Segment currentLine = new Segment(); // For speed.
		private final boolean select;

		public BeginLineAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			int newPos = 0;

			try {

				// Is line wrap enabled?
				if (textArea.getLineWrap()) {
					final int offs = textArea.getCaretPosition();
					// TODO: Replace Utilities call with custom version
					// to cut down on all of the modelToViews, as each call
					// causes TokenList => expensive!
					final int begOffs = Utilities.getRowStart(textArea, offs);
					// TODO: line wrap doesn't currently toggle between
					// the first non-whitespace char and the actual start
					// of the line line the no-line-wrap version does.
					newPos = begOffs;
				}

				// No line wrap - optimized for performance!
				else {

					// We use the elements instead of calling
					// getLineOfOffset(), etc. to speed things up just a
					// tad (i.e. micro-optimize).
					final int caretPosition = textArea.getCaretPosition();
					final Document document = textArea.getDocument();
					final Element map = document.getDefaultRootElement();
					final int currentLineNum = map.getElementIndex(caretPosition);
					final Element currentLineElement = map.getElement(currentLineNum);
					final int currentLineStart = currentLineElement.getStartOffset();
					final int currentLineEnd = currentLineElement.getEndOffset();
					final int count = currentLineEnd - currentLineStart;
					if (count > 0) { // If there are chars in the line...
						document.getText(currentLineStart, count, this.currentLine);
						int firstNonWhitespace = this.getFirstNonWhitespacePos();
						firstNonWhitespace = currentLineStart + firstNonWhitespace - this.currentLine.offset;
						if (caretPosition != firstNonWhitespace)
							newPos = firstNonWhitespace;
						else
							newPos = currentLineStart;
					} else
						newPos = currentLineStart;

				}

				if (this.select)
					textArea.moveCaretPosition(newPos);
				else
					textArea.setCaretPosition(newPos);

			} catch (final BadLocationException ble) {
				/* Shouldn't ever happen. */
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				ble.printStackTrace();
			}

		}

		private int getFirstNonWhitespacePos() {
			final int offset = this.currentLine.offset;
			final int end = offset + this.currentLine.count - 1;
			int pos = offset;
			final char[] array = this.currentLine.array;
			char currentChar = array[pos];
			while ((currentChar == '\t' || currentChar == ' ') && ++pos < end)
				currentChar = array[pos];
			return pos;
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action that begins recording a macro.
	 */
	public static class BeginRecordingMacroAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public BeginRecordingMacroAction() {
			super(RTextAreaEditorKit.rtaBeginRecordingMacroAction);
		}

		public BeginRecordingMacroAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			RTextArea.beginRecordingMacro();
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaBeginRecordingMacroAction;
		}

		@Override
		public boolean isRecordable() {
			return false; // Never record the recording of a macro!
		}

	}

	/**
	 * Positions the caret at the beginning of the word.
	 */
	protected static class BeginWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		protected BeginWordAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			try {
				final int offs = textArea.getCaretPosition();
				final int begOffs = this.getWordStart(textArea, offs);
				if (this.select)
					textArea.moveCaretPosition(begOffs);
				else
					textArea.setCaretPosition(begOffs);
			} catch (final BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getWordStart(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getWordStart(textArea, offs);
		}

	}

	/**
	 * Action for displaying a popup with a list of recently pasted text snippets.
	 */
	public static class ClipboardHistoryAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final ClipboardHistory clipboardHistory;

		public ClipboardHistoryAction() {
			super(RTextAreaEditorKit.clipboardHistoryAction);
			this.clipboardHistory = ClipboardHistory.get();
		}

		public ClipboardHistoryAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
			this.clipboardHistory = ClipboardHistory.get();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final Window owner = SwingUtilities.getWindowAncestor(textArea);
			final ClipboardHistoryPopup popup = new ClipboardHistoryPopup(owner, textArea);
			popup.setContents(this.clipboardHistory.getHistory());
			popup.setVisible(true);
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.clipboardHistoryAction;
		}

	}

	/**
	 * Action for copying text.
	 */
	public static class CopyAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public CopyAction() {
			super(DefaultEditorKit.copyAction);
		}

		public CopyAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.copy();
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.copyAction;
		}

	}

	/**
	 * Action for cutting text.
	 */
	public static class CutAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public CutAction() {
			super(DefaultEditorKit.cutAction);
		}

		public CutAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.cut();
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.cutAction;
		}

	}

	/**
	 * Action for decreasing the font size.
	 */
	public static class DecreaseFontSizeAction extends RecordableTextAction {

		protected static final float MINIMUM_SIZE = 2.0f;

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		protected float decreaseAmount;

		public DecreaseFontSizeAction() {
			super(RTextAreaEditorKit.rtaDecreaseFontSizeAction);
			this.initialize();
		}

		public DecreaseFontSizeAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
			this.initialize();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			Font font = textArea.getFont();
			final float oldSize = font.getSize2D();
			final float newSize = oldSize - this.decreaseAmount;
			if (newSize >= DecreaseFontSizeAction.MINIMUM_SIZE) {
				// Shrink by decreaseAmount.
				font = font.deriveFont(newSize);
				textArea.setFont(font);
			} else if (oldSize > DecreaseFontSizeAction.MINIMUM_SIZE) {
				// Can't shrink by full decreaseAmount, but can shrink a
				// little bit.
				font = font.deriveFont(DecreaseFontSizeAction.MINIMUM_SIZE);
				textArea.setFont(font);
			} else
				// Our font size must be at or below MINIMUM_SIZE.
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaDecreaseFontSizeAction;
		}

		protected void initialize() {
			this.decreaseAmount = 1.0f;
		}

	}

	/**
	 * The action to use when no actions in the input/action map meet the key
	 * pressed. This is actually called from the keymap I believe.
	 */
	public static class DefaultKeyTypedAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Action delegate;

		public DefaultKeyTypedAction() {
			super(DefaultEditorKit.defaultKeyTypedAction, null, null, null, null);
			this.delegate = new DefaultEditorKit.DefaultKeyTypedAction();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			// DefaultKeyTypedAction *is* different across different JVM's
			// (at least the OSX implementation must be different - Alt+Numbers
			// inputs symbols such as '[', '{', etc., which is a *required*
			// feature on MacBooks running with non-English input, such as
			// German or Swedish Pro). So we can't just copy the
			// implementation, we must delegate to it.
			this.delegate.actionPerformed(e);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.defaultKeyTypedAction;
		}

	}

	/**
	 * Deletes the current line(s).
	 */
	public static class DeleteLineAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public DeleteLineAction() {
			super(RTextAreaEditorKit.rtaDeleteLineAction, null, null, null, null);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			final int selStart = textArea.getSelectionStart();
			final int selEnd = textArea.getSelectionEnd();

			try {

				final int line1 = textArea.getLineOfOffset(selStart);
				final int startOffs = textArea.getLineStartOffset(line1);
				final int line2 = textArea.getLineOfOffset(selEnd);
				int endOffs = textArea.getLineEndOffset(line2);

				// Don't remove the last line if no actual chars are selected
				if (line2 > line1)
					if (selEnd == textArea.getLineStartOffset(line2))
						endOffs = selEnd;

				textArea.replaceRange(null, startOffs, endOffs);

			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaDeleteLineAction;
		}

	}

	/**
	 * Deletes the character of content that follows the current caret position.
	 */
	public static class DeleteNextCharAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public DeleteNextCharAction() {
			super(DefaultEditorKit.deleteNextCharAction, null, null, null, null);
		}

		public DeleteNextCharAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			boolean beep = true;
			if (textArea != null && textArea.isEditable())
				try {
					final Document doc = textArea.getDocument();
					final Caret caret = textArea.getCaret();
					final int dot = caret.getDot();
					final int mark = caret.getMark();
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						beep = false;
					} else if (dot < doc.getLength()) {
						int delChars = 1;
						if (dot < doc.getLength() - 1) {
							final String dotChars = doc.getText(dot, 2);
							final char c0 = dotChars.charAt(0);
							final char c1 = dotChars.charAt(1);
							if (c0 >= '\uD800' && c0 <= '\uDBFF' && c1 >= '\uDC00' && c1 <= '\uDFFF')
								delChars = 2;
						}
						doc.remove(dot, delChars);
						beep = false;
					}
				} catch (final BadLocationException bl) {
				}

			if (beep)
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			if (textArea != null)
				textArea.requestFocusInWindow();

		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.deleteNextCharAction;
		}

	}

	/**
	 * Deletes the character of content that precedes the current caret position.
	 */
	public static class DeletePrevCharAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public DeletePrevCharAction() {
			super(DefaultEditorKit.deletePrevCharAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			boolean beep = true;
			if (textArea != null && textArea.isEditable())
				try {
					final Document doc = textArea.getDocument();
					final Caret caret = textArea.getCaret();
					final int dot = caret.getDot();
					final int mark = caret.getMark();
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						beep = false;
					} else if (dot > 0) {
						int delChars = 1;
						if (dot > 1) {
							final String dotChars = doc.getText(dot - 2, 2);
							final char c0 = dotChars.charAt(0);
							final char c1 = dotChars.charAt(1);
							if (c0 >= '\uD800' && c0 <= '\uDBFF' && c1 >= '\uDC00' && c1 <= '\uDFFF')
								delChars = 2;
						}
						doc.remove(dot - delChars, delChars);
						beep = false;
					}
				} catch (final BadLocationException bl) {
				}

			if (beep)
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);

		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.deletePrevCharAction;
		}

	}

	/**
	 * Action that deletes the previous word in the text area.
	 */
	public static class DeletePrevWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public DeletePrevWordAction() {
			super(RTextAreaEditorKit.rtaDeletePrevWordAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			try {
				final int end = textArea.getSelectionStart();
				final int start = this.getPreviousWordStart(textArea, end);
				if (end > start)
					textArea.getDocument().remove(start, end - start);
			} catch (final BadLocationException ex) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public String getMacroID() {
			return RTextAreaEditorKit.rtaDeletePrevWordAction;
		}

		/**
		 * Returns the starting offset to delete. Exists so subclasses can override.
		 */
		protected int getPreviousWordStart(final RTextArea textArea, final int end) throws BadLocationException {
			return Utilities.getPreviousWord(textArea, end);
		}

	}

	/**
	 * Action that deletes all text from the caret position to the end of the
	 * caret's line.
	 */
	public static class DeleteRestOfLineAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public DeleteRestOfLineAction() {
			super(RTextAreaEditorKit.rtaDeleteRestOfLineAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			try {

				// We use the elements instead of calling getLineOfOffset(),
				// etc. to speed things up just a tad (i.e. micro-optimize).
				final Document document = textArea.getDocument();
				final int caretPosition = textArea.getCaretPosition();
				final Element map = document.getDefaultRootElement();
				final int currentLineNum = map.getElementIndex(caretPosition);
				final Element currentLineElement = map.getElement(currentLineNum);
				// Always take -1 as we don't want to remove the newline.
				final int currentLineEnd = currentLineElement.getEndOffset() - 1;
				if (caretPosition < currentLineEnd)
					document.remove(caretPosition, currentLineEnd - caretPosition);

			} catch (final BadLocationException ble) {
				ble.printStackTrace();
			}

		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaDeleteRestOfLineAction;
		}

	}

	/**
	 * Finds the most recent word in the document that matches the "word" up to the
	 * current caret position, and auto-completes the rest. Repeatedly calling this
	 * action at the same location in the document goes one match back each time it
	 * is called.
	 */
	public static class DumbCompleteWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private int lastDot;
		private String lastPrefix;
		private int lastWordStart;
		private int searchOffs;

		public DumbCompleteWordAction() {
			super(RTextAreaEditorKit.rtaDumbCompleteWordAction);
			this.lastWordStart = this.searchOffs = this.lastDot = -1;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled())
				return;

			try {

				final int dot = textArea.getCaretPosition();
				if (dot == 0)
					return;

				final int curWordStart = this.getWordStart(textArea, dot);

				if (this.lastWordStart != curWordStart || dot != this.lastDot) {
					this.lastPrefix = textArea.getText(curWordStart, dot - curWordStart);
					// Utilities.getWordStart() treats spans of whitespace and
					// single non-letter chars as "words."
					if (!this.isAcceptablePrefix(this.lastPrefix)) {
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
						return;
					}
					this.lastWordStart = dot - this.lastPrefix.length();
					// searchOffs = lastWordStart;
					// searchOffs = getWordStart(textArea, lastWordStart);
					this.searchOffs = Math.max(this.lastWordStart - 1, 0);
				}

				while (this.searchOffs > 0) {
					int wordStart = 0;
					try {
						wordStart = this.getPreviousWord(textArea, this.searchOffs);
					} catch (final BadLocationException ble) {
						// No more words. Sometimes happens for example if the
						// document starts off with whitespace - then searchOffs
						// is > 0 but there are no more words
						wordStart = BreakIterator.DONE;
					}
					if (wordStart == BreakIterator.DONE) {
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
						break;
					}
					final int end = this.getWordEnd(textArea, wordStart);
					final String word = textArea.getText(wordStart, end - wordStart);
					this.searchOffs = wordStart;
					if (word.startsWith(this.lastPrefix)) {
						textArea.replaceRange(word, this.lastWordStart, dot);
						this.lastDot = textArea.getCaretPosition(); // Maybe shifted
						break;
					}
				}

			} catch (final BadLocationException ble) { // Never happens
				ble.printStackTrace();
			}

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getPreviousWord(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getPreviousWord(textArea, offs);
		}

		protected int getWordEnd(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getWordEnd(textArea, offs);
		}

		protected int getWordStart(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getWordStart(textArea, offs);
		}

		/**
		 * <code>Utilities.getWordStart()</code> treats spans of whitespace and single
		 * non-letter chars as "words." This method is used to filter that kind of thing
		 * out - non-words should not be suggested by this action.
		 *
		 * @param prefix
		 *            The prefix characters before the caret.
		 * @return Whether the prefix could be part of a "word" in the context of the
		 *         text area's current content.
		 */
		protected boolean isAcceptablePrefix(final String prefix) {
			return prefix.length() > 0 && Character.isLetter(prefix.charAt(prefix.length() - 1));
		}

	}

	/**
	 * Moves the caret to the end of the document.
	 */
	public static class EndAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		public EndAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final int dot = this.getVisibleEnd(textArea);
			if (this.select)
				textArea.moveCaretPosition(dot);
			else
				textArea.setCaretPosition(dot);
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getVisibleEnd(final RTextArea textArea) {
			return textArea.getDocument().getLength();
		}

	}

	/**
	 * Positions the caret at the end of the line.
	 */
	public static class EndLineAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		public EndLineAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final int offs = textArea.getCaretPosition();
			int endOffs = 0;
			try {
				if (textArea.getLineWrap())
					// Must check per character, since one logical line may be
					// many physical lines.
					// FIXME: Replace Utilities call with custom version to
					// cut down on all of the modelToViews, as each call causes
					// a getTokenList => expensive!
					endOffs = Utilities.getRowEnd(textArea, offs);
				else {
					final Element root = textArea.getDocument().getDefaultRootElement();
					final int line = root.getElementIndex(offs);
					endOffs = root.getElement(line).getEndOffset() - 1;
				}
				if (this.select)
					textArea.moveCaretPosition(endOffs);
				else
					textArea.setCaretPosition(endOffs);
			} catch (final Exception ex) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action that ends recording a macro.
	 */
	public static class EndRecordingMacroAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public EndRecordingMacroAction() {
			super(RTextAreaEditorKit.rtaEndRecordingMacroAction);
		}

		public EndRecordingMacroAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			RTextArea.endRecordingMacro();
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaEndRecordingMacroAction;
		}

		@Override
		public boolean isRecordable() {
			return false; // Never record the recording of a macro!
		}

	}

	/**
	 * Positions the caret at the end of the word.
	 */
	protected static class EndWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		protected EndWordAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			try {
				final int offs = textArea.getCaretPosition();
				final int endOffs = this.getWordEnd(textArea, offs);
				if (this.select)
					textArea.moveCaretPosition(endOffs);
				else
					textArea.setCaretPosition(endOffs);
			} catch (final BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getWordEnd(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getWordEnd(textArea, offs);
		}

	}

	/**
	 * Action for increasing the font size.
	 */
	public static class IncreaseFontSizeAction extends RecordableTextAction {

		protected static final float MAXIMUM_SIZE = 40.0f;

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		protected float increaseAmount;

		public IncreaseFontSizeAction() {
			super(RTextAreaEditorKit.rtaIncreaseFontSizeAction);
			this.initialize();
		}

		public IncreaseFontSizeAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
			this.initialize();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			Font font = textArea.getFont();
			final float oldSize = font.getSize2D();
			final float newSize = oldSize + this.increaseAmount;
			if (newSize <= IncreaseFontSizeAction.MAXIMUM_SIZE) {
				// Grow by increaseAmount.
				font = font.deriveFont(newSize);
				textArea.setFont(font);
			} else if (oldSize < IncreaseFontSizeAction.MAXIMUM_SIZE) {
				// Can't grow by full increaseAmount, but can grow a
				// little bit.
				font = font.deriveFont(IncreaseFontSizeAction.MAXIMUM_SIZE);
				textArea.setFont(font);
			} else
				// Our font size must be at or bigger than MAXIMUM_SIZE.
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaIncreaseFontSizeAction;
		}

		protected void initialize() {
			this.increaseAmount = 1.0f;
		}

	}

	/**
	 * Action for when the user presses the Enter key.
	 */
	public static class InsertBreakAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public InsertBreakAction() {
			super(DefaultEditorKit.insertBreakAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			textArea.replaceSelection("\n");
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.insertBreakAction;
		}

		/*
		 * Overridden for Sun bug 4515750. Sun fixed this in a more complicated way, but
		 * I'm not sure why. See BasicTextUI#getActionMap() and
		 * BasicTextUI.TextActionWrapper.
		 */
		@Override
		public boolean isEnabled() {
			final JTextComponent tc = this.getTextComponent(null);
			return tc == null || tc.isEditable() ? super.isEnabled() : false;
		}

	}

	/**
	 * Action taken when content is to be inserted.
	 */
	public static class InsertContentAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public InsertContentAction() {
			super(DefaultEditorKit.insertContentAction, null, null, null, null);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			final String content = e.getActionCommand();
			if (content != null)
				textArea.replaceSelection(content);
			else
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.insertContentAction;
		}

	}

	/**
	 * Places a tab character into the document. If there is a selection, it is
	 * removed before the tab is added.
	 */
	public static class InsertTabAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public InsertTabAction() {
			super(DefaultEditorKit.insertTabAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			textArea.replaceSelection("\t");
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.insertTabAction;
		}

	}

	/**
	 * Action to invert the selection's case.
	 */
	public static class InvertSelectionCaseAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public InvertSelectionCaseAction() {
			super(RTextAreaEditorKit.rtaInvertSelectionCaseAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			final String selection = textArea.getSelectedText();
			if (selection != null) {
				final StringBuilder buffer = new StringBuilder(selection);
				final int length = buffer.length();
				for (int i = 0; i < length; i++) {
					final char c = buffer.charAt(i);
					if (Character.isUpperCase(c))
						buffer.setCharAt(i, Character.toLowerCase(c));
					else if (Character.isLowerCase(c))
						buffer.setCharAt(i, Character.toUpperCase(c));
				}
				textArea.replaceSelection(buffer.toString());
			}
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action to join the current line and the following line.
	 */
	public static class JoinLinesAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public JoinLinesAction() {
			super(RTextAreaEditorKit.rtaJoinLinesAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			try {
				final Caret c = textArea.getCaret();
				int caretPos = c.getDot();
				final Document doc = textArea.getDocument();
				final Element map = doc.getDefaultRootElement();
				final int lineCount = map.getElementCount();
				final int line = map.getElementIndex(caretPos);
				if (line == lineCount - 1) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
				final Element lineElem = map.getElement(line);
				caretPos = lineElem.getEndOffset() - 1;
				c.setDot(caretPos); // Gets rid of any selection.
				doc.remove(caretPos, 1); // Should be '\n'.
			} catch (final BadLocationException ble) {
				/* Shouldn't ever happen. */
				ble.printStackTrace();
			}
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action that moves a line up or down.
	 */
	public static class LineMoveAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final int moveAmt;

		public LineMoveAction(final String name, final int moveAmt) {
			super(name);
			this.moveAmt = moveAmt;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			try {

				final int dot = textArea.getCaretPosition();
				final int mark = textArea.getCaret().getMark();
				final Document doc = textArea.getDocument();
				final Element root = doc.getDefaultRootElement();
				final int startLine = root.getElementIndex(Math.min(dot, mark));
				final int endLine = root.getElementIndex(Math.max(dot, mark));

				// If we're moving more than one line, only move the last line
				// if they've selected more than one char in it.
				int moveCount = endLine - startLine + 1;
				if (moveCount > 1) {
					final Element elem = root.getElement(endLine);
					if (dot == elem.getStartOffset() || mark == elem.getStartOffset())
						moveCount--;
				}

				if (this.moveAmt == -1 && startLine > 0)
					this.moveLineUp(textArea, startLine, moveCount);
				else if (this.moveAmt == 1 && endLine < root.getElementCount() - 1)
					this.moveLineDown(textArea, startLine, moveCount);
				else {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			} catch (final BadLocationException ble) {
				// Never happens.
				ble.printStackTrace();
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		private void moveLineDown(final RTextArea textArea, final int line, final int lineCount)
				throws BadLocationException {

			// If we'd be moving lines past the end of the document, stop.
			// We could perhaps just decide to move the lines to the end of the
			// file, but this just keeps things simple.
			// if (textArea.getLineCount() - line < lineCount) {
			// UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			// return;
			// }

			final Document doc = textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			Element elem = root.getElement(line);
			final int start = elem.getStartOffset();

			final int endLine = line + lineCount - 1;
			elem = root.getElement(endLine);
			final int end = elem.getEndOffset();

			textArea.beginAtomicEdit();
			try {

				final String text = doc.getText(start, end - start);
				doc.remove(start, end - start);

				final int insertLine = Math.min(line + 1, textArea.getLineCount());
				boolean newlineInserted = false;
				if (insertLine == textArea.getLineCount()) {
					textArea.append("\n");
					newlineInserted = true;
				}

				final int insertOffs = textArea.getLineStartOffset(insertLine);
				doc.insertString(insertOffs, text, null);
				textArea.setSelectionStart(insertOffs);
				textArea.setSelectionEnd(insertOffs + text.length() - 1);

				if (newlineInserted)
					doc.remove(doc.getLength() - 1, 1);

			} finally {
				textArea.endAtomicEdit();
			}

		}

		private void moveLineUp(final RTextArea textArea, final int line, final int moveCount)
				throws BadLocationException {

			final Document doc = textArea.getDocument();
			final Element root = doc.getDefaultRootElement();
			Element elem = root.getElement(line);
			final int start = elem.getStartOffset();

			final int endLine = line + moveCount - 1;
			elem = root.getElement(endLine);
			int end = elem.getEndOffset();
			final int lineCount = textArea.getLineCount();
			boolean movingLastLine = false;
			if (endLine == lineCount - 1) {
				movingLastLine = true;
				end--;
			}

			final int insertLine = Math.max(line - 1, 0);

			textArea.beginAtomicEdit();
			try {

				String text = doc.getText(start, end - start);
				if (movingLastLine)
					text += '\n';
				doc.remove(start, end - start);

				final int insertOffs = textArea.getLineStartOffset(insertLine);
				doc.insertString(insertOffs, text, null);
				textArea.setSelectionStart(insertOffs);
				final int selEnd = insertOffs + text.length() - 1;
				textArea.setSelectionEnd(selEnd);
				if (movingLastLine)
					doc.remove(doc.getLength() - 1, 1);

			} finally {
				textArea.endAtomicEdit();
			}

		}

	}

	/**
	 * Action to make the selection lower-case.
	 */
	public static class LowerSelectionCaseAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public LowerSelectionCaseAction() {
			super(RTextAreaEditorKit.rtaLowerSelectionCaseAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			final String selection = textArea.getSelectedText();
			if (selection != null)
				textArea.replaceSelection(selection.toLowerCase());
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action that moves the caret to the next (or previous) bookmark.
	 */
	public static class NextBookmarkAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean forward;

		public NextBookmarkAction(final String name, final boolean forward) {
			super(name);
			this.forward = forward;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final Gutter gutter = RSyntaxUtilities.getGutter(textArea);
			if (gutter != null)
				try {

					final GutterIconInfo[] bookmarks = gutter.getBookmarks();
					if (bookmarks.length == 0) {
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
						return;
					}

					GutterIconInfo moveTo = null;
					final int curLine = textArea.getCaretLineNumber();

					if (this.forward) {
						for (final GutterIconInfo bookmark : bookmarks) {
							final int offs = bookmark.getMarkedOffset();
							final int line = textArea.getLineOfOffset(offs);
							if (line > curLine) {
								moveTo = bookmark;
								break;
							}
						}
						if (moveTo == null)
							moveTo = bookmarks[0];
					} else {
						for (int i = bookmarks.length - 1; i >= 0; i--) {
							final GutterIconInfo bookmark = bookmarks[i];
							final int offs = bookmark.getMarkedOffset();
							final int line = textArea.getLineOfOffset(offs);
							if (line < curLine) {
								moveTo = bookmark;
								break;
							}
						}
						if (moveTo == null)
							moveTo = bookmarks[bookmarks.length - 1];
					}

					int offs = moveTo.getMarkedOffset();
					if (textArea instanceof RSyntaxTextArea) {
						final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
						if (rsta.isCodeFoldingEnabled())
							rsta.getFoldManager().ensureOffsetNotInClosedFold(offs);
					}
					final int line = textArea.getLineOfOffset(offs);
					offs = textArea.getLineStartOffset(line);
					textArea.setCaretPosition(offs);

				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					ble.printStackTrace();
				}

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Selects the next occurrence of the text last selected.
	 */
	public static class NextOccurrenceAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public NextOccurrenceAction(final String name) {
			super(name);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			String selectedText = textArea.getSelectedText();
			if (selectedText == null || selectedText.length() == 0) {
				selectedText = RTextArea.getSelectedOccurrenceText();
				if (selectedText == null || selectedText.length() == 0) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			}
			final SearchContext context = new SearchContext(selectedText);
			if (!textArea.getMarkAllOnOccurrenceSearches())
				context.setMarkAll(false);
			if (!SearchEngine.find(textArea, context).wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			RTextArea.setSelectedOccurrenceText(selectedText);
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action to move the selection and/or caret. Constructor indicates direction to
	 * use.
	 */
	public static class NextVisualPositionAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final int direction;
		private final boolean select;

		public NextVisualPositionAction(final String nm, final boolean select, final int dir) {
			super(nm);
			this.select = select;
			this.direction = dir;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final Caret caret = textArea.getCaret();
			int dot = caret.getDot();

			/*
			 * Move to the beginning/end of selection on a "non-shifted" left- or
			 * right-keypress. We shouldn't have to worry about navigation filters as, if
			 * one is being used, it let us get to that position before.
			 */
			if (!this.select)
				switch (this.direction) {
				case SwingConstants.EAST:
					int mark = caret.getMark();
					if (dot != mark) {
						caret.setDot(Math.max(dot, mark));
						return;
					}
					break;
				case SwingConstants.WEST:
					mark = caret.getMark();
					if (dot != mark) {
						caret.setDot(Math.min(dot, mark));
						return;
					}
					break;
				default:
				}

			final Position.Bias[] bias = new Position.Bias[1];
			Point magicPosition = caret.getMagicCaretPosition();

			try {

				if (magicPosition == null
						&& (this.direction == SwingConstants.NORTH || this.direction == SwingConstants.SOUTH)) {
					final Rectangle r = textArea.modelToView(dot);
					magicPosition = new Point(r.x, r.y);
				}

				final NavigationFilter filter = textArea.getNavigationFilter();

				if (filter != null)
					dot = filter.getNextVisualPositionFrom(textArea, dot, Position.Bias.Forward, this.direction, bias);
				else
					dot = textArea.getUI().getNextVisualPositionFrom(textArea, dot, Position.Bias.Forward,
							this.direction, bias);
				if (this.select)
					caret.moveDot(dot);
				else
					caret.setDot(dot);

				if (magicPosition != null
						&& (this.direction == SwingConstants.NORTH || this.direction == SwingConstants.SOUTH))
					caret.setMagicCaretPosition(magicPosition);

			} catch (final BadLocationException ble) {
				ble.printStackTrace();
			}

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Positions the caret at the next word.
	 */
	public static class NextWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		public NextWordAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			int offs = textArea.getCaretPosition();
			final int oldOffs = offs;
			final Element curPara = Utilities.getParagraphElement(textArea, offs);

			try {
				offs = this.getNextWord(textArea, offs);
				if (offs >= curPara.getEndOffset() && oldOffs != curPara.getEndOffset() - 1)
					// we should first move to the end of current paragraph
					// http://bugs.sun.com/view_bug.do?bug_id=4278839
					offs = curPara.getEndOffset() - 1;
			} catch (final BadLocationException ble) {
				final int end = textArea.getDocument().getLength();
				if (offs != end)
					if (oldOffs != curPara.getEndOffset() - 1)
						offs = curPara.getEndOffset() - 1;
					else
						offs = end;
			}

			if (this.select)
				textArea.moveCaretPosition(offs);
			else
				textArea.setCaretPosition(offs);

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getNextWord(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getNextWord(textArea, offs);
		}

	}

	/**
	 * Pages one view to the left or right.
	 */
	static class PageAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean left;
		private final boolean select;

		PageAction(final String name, final boolean left, final boolean select) {
			super(name);
			this.select = select;
			this.left = left;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			int selectedIndex;
			final Rectangle visible = new Rectangle();
			textArea.computeVisibleRect(visible);
			if (this.left)
				visible.x = Math.max(0, visible.x - visible.width);
			else
				visible.x += visible.width;

			selectedIndex = textArea.getCaretPosition();
			if (selectedIndex != -1) {
				if (this.left)
					selectedIndex = textArea.viewToModel(new Point(visible.x, visible.y));
				else
					selectedIndex = textArea
							.viewToModel(new Point(visible.x + visible.width - 1, visible.y + visible.height - 1));
				final Document doc = textArea.getDocument();
				if (selectedIndex != 0 && selectedIndex > doc.getLength() - 1)
					selectedIndex = doc.getLength() - 1;
				else if (selectedIndex < 0)
					selectedIndex = 0;
				if (this.select)
					textArea.moveCaretPosition(selectedIndex);
				else
					textArea.setCaretPosition(selectedIndex);
			}

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action for pasting text.
	 */
	public static class PasteAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public PasteAction() {
			super(DefaultEditorKit.pasteAction);
		}

		public PasteAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.paste();
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.pasteAction;
		}

	}

	/**
	 * "Plays back" the last macro recorded.
	 */
	public static class PlaybackLastMacroAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public PlaybackLastMacroAction() {
			super(RTextAreaEditorKit.rtaPlaybackLastMacroAction);
		}

		public PlaybackLastMacroAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.playbackLastMacro();
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaPlaybackLastMacroAction;
		}

		@Override
		public boolean isRecordable() {
			return false; // Don't record macro playbacks.
		}

	}

	/**
	 * Select the previous occurrence of the text last selected.
	 */
	public static class PreviousOccurrenceAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public PreviousOccurrenceAction(final String name) {
			super(name);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			String selectedText = textArea.getSelectedText();
			if (selectedText == null || selectedText.length() == 0) {
				selectedText = RTextArea.getSelectedOccurrenceText();
				if (selectedText == null || selectedText.length() == 0) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			}
			final SearchContext context = new SearchContext(selectedText);
			if (!textArea.getMarkAllOnOccurrenceSearches())
				context.setMarkAll(false);
			context.setSearchForward(false);
			if (!SearchEngine.find(textArea, context).wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			RTextArea.setSelectedOccurrenceText(selectedText);
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Positions the caret at the beginning of the previous word.
	 */
	public static class PreviousWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final boolean select;

		public PreviousWordAction(final String name, final boolean select) {
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			int offs = textArea.getCaretPosition();
			boolean failed = false;
			try {

				final Element curPara = Utilities.getParagraphElement(textArea, offs);
				offs = this.getPreviousWord(textArea, offs);
				if (offs < curPara.getStartOffset())
					offs = Utilities.getParagraphElement(textArea, offs).getEndOffset() - 1;

			} catch (final BadLocationException bl) {
				if (offs != 0)
					offs = 0;
				else
					failed = true;
			}

			if (!failed) {
				if (this.select)
					textArea.moveCaretPosition(offs);
				else
					textArea.setCaretPosition(offs);
			} else
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);

		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

		protected int getPreviousWord(final RTextArea textArea, final int offs) throws BadLocationException {
			return Utilities.getPreviousWord(textArea, offs);
		}

	}

	/**
	 * Re-does the last action undone.
	 */
	public static class RedoAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public RedoAction() {
			super(RTextAreaEditorKit.rtaRedoAction);
		}

		public RedoAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (textArea.isEnabled() && textArea.isEditable()) {
				textArea.redoLastAction();
				textArea.requestFocusInWindow();
			}
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaRedoAction;
		}

	}

	/**
	 * Scrolls the text area one line up or down, without changing the caret
	 * position.
	 */
	public static class ScrollAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final int delta;

		public ScrollAction(final String name, final int delta) {
			super(name);
			this.delta = delta;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final Container parent = textArea.getParent();
			if (parent instanceof JViewport) {
				final JViewport viewport = (JViewport) parent;
				final Point p = viewport.getViewPosition();
				p.y += this.delta * textArea.getLineHeight();
				if (p.y < 0)
					p.y = 0;
				else {
					final Rectangle viewRect = viewport.getViewRect();
					final int visibleEnd = p.y + viewRect.height;
					if (visibleEnd >= textArea.getHeight())
						p.y = textArea.getHeight() - viewRect.height;
				}
				viewport.setViewPosition(p);
			}
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Selects the entire document.
	 */
	public static class SelectAllAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public SelectAllAction() {
			super(DefaultEditorKit.selectAllAction);
		}

		public SelectAllAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final Document doc = textArea.getDocument();
			textArea.setCaretPosition(0);
			textArea.moveCaretPosition(doc.getLength());
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.selectAllAction;
		}

	}

	/**
	 * Selects the line around the caret.
	 */
	public static class SelectLineAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Action end;
		private final Action start;

		public SelectLineAction() {
			super(DefaultEditorKit.selectLineAction);
			this.start = new BeginLineAction("pigdog", false);
			this.end = new EndLineAction("pigdog", true);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			this.start.actionPerformed(e);
			this.end.actionPerformed(e);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.selectLineAction;
		}

	}

	/**
	 * Selects the word around the caret.
	 */
	public static class SelectWordAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		protected Action end;
		protected Action start;

		public SelectWordAction() {
			super(DefaultEditorKit.selectWordAction);
			this.createActions();
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			this.start.actionPerformed(e);
			this.end.actionPerformed(e);
		}

		protected void createActions() {
			this.start = new BeginWordAction("pigdog", false);
			this.end = new EndWordAction("pigdog", true);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.selectWordAction;
		}

	}

	/**
	 * Puts the text area into read-only mode.
	 */
	public static class SetReadOnlyAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public SetReadOnlyAction() {
			super(DefaultEditorKit.readOnlyAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.setEditable(false);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.readOnlyAction;
		}

		@Override
		public boolean isRecordable() {
			return false; // Why would you want to record this?
		}

	}

	/**
	 * Puts the text area into writable (from read-only) mode.
	 */
	public static class SetWritableAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public SetWritableAction() {
			super(DefaultEditorKit.writableAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.setEditable(true);
		}

		@Override
		public final String getMacroID() {
			return DefaultEditorKit.writableAction;
		}

		@Override
		public boolean isRecordable() {
			return false; // Why would you want to record this?
		}

	}

	/**
	 * The action for inserting a time/date stamp.
	 */
	public static class TimeDateAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public TimeDateAction() {
			super(RTextAreaEditorKit.rtaTimeDateAction);
		}

		public TimeDateAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			final Date today = new Date();
			final DateFormat timeDateStamp = DateFormat.getDateTimeInstance();
			final String dateString = timeDateStamp.format(today);
			textArea.replaceSelection(dateString);
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaTimeDateAction;
		}

	}

	/**
	 * Toggles whether the current line has a bookmark.
	 */
	public static class ToggleBookmarkAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ToggleBookmarkAction() {
			super(RTextAreaEditorKit.rtaToggleBookmarkAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final Gutter gutter = RSyntaxUtilities.getGutter(textArea);
			if (gutter != null) {
				final int line = textArea.getCaretLineNumber();
				try {
					gutter.toggleBookmark(line);
				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					ble.printStackTrace();
				}
			}
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaToggleBookmarkAction;
		}

	}

	/**
	 * The action for the insert key toggling insert/overwrite modes.
	 */
	public static class ToggleTextModeAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ToggleTextModeAction() {
			super(RTextAreaEditorKit.rtaToggleTextModeAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			final int textMode = textArea.getTextMode();
			if (textMode == RTextArea.INSERT_MODE)
				textArea.setTextMode(RTextArea.OVERWRITE_MODE);
			else
				textArea.setTextMode(RTextArea.INSERT_MODE);
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaToggleTextModeAction;
		}

	}

	/**
	 * Undoes the last action done.
	 */
	public static class UndoAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public UndoAction() {
			super(RTextAreaEditorKit.rtaUndoAction);
		}

		public UndoAction(final String name, final Icon icon, final String desc, final Integer mnemonic,
				final KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (textArea.isEnabled() && textArea.isEditable()) {
				textArea.undoLastAction();
				textArea.requestFocusInWindow();
			}
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaUndoAction;
		}

	}

	/**
	 * Removes the selection, if any.
	 */
	public static class UnselectAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public UnselectAction() {
			super(RTextAreaEditorKit.rtaUnselectAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			textArea.setCaretPosition(textArea.getCaretPosition());
		}

		@Override
		public final String getMacroID() {
			return RTextAreaEditorKit.rtaUnselectAction;
		}

	}

	/**
	 * Action to make the selection upper-case.
	 */
	public static class UpperSelectionCaseAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public UpperSelectionCaseAction() {
			super(RTextAreaEditorKit.rtaUpperSelectionCaseAction);
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			final String selection = textArea.getSelectedText();
			if (selection != null)
				textArea.replaceSelection(selection.toUpperCase());
			textArea.requestFocusInWindow();
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Scrolls up/down vertically. The select version of this action extends the
	 * selection, instead of simply moving the caret.
	 */
	public static class VerticalPageAction extends RecordableTextAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final int direction;
		private final boolean select;

		public VerticalPageAction(final String name, final int direction, final boolean select) {
			super(name);
			this.select = select;
			this.direction = direction;
		}

		@Override
		public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea) {

			final Rectangle visible = textArea.getVisibleRect();
			final Rectangle newVis = new Rectangle(visible);
			final int selectedIndex = textArea.getCaretPosition();
			final int scrollAmount = textArea.getScrollableBlockIncrement(visible, SwingConstants.VERTICAL,
					this.direction);
			final int initialY = visible.y;
			final Caret caret = textArea.getCaret();
			final Point magicPosition = caret.getMagicCaretPosition();
			int yOffset;

			if (selectedIndex != -1)
				try {

					final Rectangle dotBounds = textArea.modelToView(selectedIndex);
					final int x = magicPosition != null ? magicPosition.x : dotBounds.x;
					final int h = dotBounds.height;
					yOffset = this.direction * ((int) Math.ceil(scrollAmount / (double) h) - 1) * h;
					newVis.y = this.constrainY(textArea, initialY + yOffset, yOffset, visible.height);
					int newIndex;

					if (visible.contains(dotBounds.x, dotBounds.y))
						// Dot is currently visible, base the new
						// location off the old, or
						newIndex = textArea
								.viewToModel(new Point(x, this.constrainY(textArea, dotBounds.y + yOffset, 0, 0)));
					else // Dot isn't visible, choose the top or the bottom
					// for the new location.
					if (this.direction == -1)
						newIndex = textArea.viewToModel(new Point(x, newVis.y));
					else
						newIndex = textArea.viewToModel(new Point(x, newVis.y + visible.height));
					newIndex = this.constrainOffset(textArea, newIndex);
					if (newIndex != selectedIndex) {
						// Make sure the new visible location contains
						// the location of dot, otherwise Caret will
						// cause an additional scroll.
						this.adjustScrollIfNecessary(textArea, newVis, initialY, newIndex);
						if (this.select)
							textArea.moveCaretPosition(newIndex);
						else
							textArea.setCaretPosition(newIndex);
					}

				} catch (final BadLocationException ble) {
				}
			else {
				yOffset = this.direction * scrollAmount;
				newVis.y = this.constrainY(textArea, initialY + yOffset, yOffset, visible.height);
			}

			if (magicPosition != null)
				caret.setMagicCaretPosition(magicPosition);

			textArea.scrollRectToVisible(newVis);
		}

		private void adjustScrollIfNecessary(final JTextComponent text, final Rectangle visible, final int initialY,
				final int index) {
			try {
				final Rectangle dotBounds = text.modelToView(index);
				if (dotBounds.y < visible.y || dotBounds.y > visible.y + visible.height
						|| dotBounds.y + dotBounds.height > visible.y + visible.height) {
					int y;
					if (dotBounds.y < visible.y)
						y = dotBounds.y;
					else
						y = dotBounds.y + dotBounds.height - visible.height;
					if (this.direction == -1 && y < initialY || this.direction == 1 && y > initialY)
						// Only adjust if won't cause scrolling upward.
						visible.y = y;
				}
			} catch (final BadLocationException ble) {
			}
		}

		private int constrainOffset(final JTextComponent text, int offset) {
			final Document doc = text.getDocument();
			if (offset != 0 && offset > doc.getLength())
				offset = doc.getLength();
			if (offset < 0)
				offset = 0;
			return offset;
		}

		private int constrainY(final JTextComponent textArea, int y, final int vis, final int screenHeight) {
			if (y < 0)
				y = 0;
			else if (y + vis > textArea.getHeight())
				// y = Math.max(0, textArea.getHeight() - vis);
				y = Math.max(0, textArea.getHeight() - screenHeight);
			return y;
		}

		@Override
		public final String getMacroID() {
			return this.getName();
		}

	}

	/**
	 * Action to display the paste history popup.
	 */
	public static final String clipboardHistoryAction = "RTA.PasteHistoryAction";

	/**
	 * The actions that <code>RTextAreaEditorKit</code> adds to those of the default
	 * editor kit.
	 */
	private static final RecordableTextAction[] defaultActions = { new BeginAction(DefaultEditorKit.beginAction, false),
			new BeginAction(DefaultEditorKit.selectionBeginAction, true),
			new BeginLineAction(DefaultEditorKit.beginLineAction, false),
			new BeginLineAction(DefaultEditorKit.selectionBeginLineAction, true), new BeginRecordingMacroAction(),
			new BeginWordAction(DefaultEditorKit.beginWordAction, false),
			new BeginWordAction(DefaultEditorKit.selectionBeginWordAction, true), new ClipboardHistoryAction(),
			new CopyAction(), new CutAction(), new DefaultKeyTypedAction(), new DeleteLineAction(),
			new DeleteNextCharAction(), new DeletePrevCharAction(), new DeletePrevWordAction(),
			new DeleteRestOfLineAction(), new DumbCompleteWordAction(),
			new EndAction(DefaultEditorKit.endAction, false), new EndAction(DefaultEditorKit.selectionEndAction, true),
			new EndLineAction(DefaultEditorKit.endLineAction, false),
			new EndLineAction(DefaultEditorKit.selectionEndLineAction, true), new EndRecordingMacroAction(),
			new EndWordAction(DefaultEditorKit.endWordAction, false),
			new EndWordAction(DefaultEditorKit.endWordAction, true), new InsertBreakAction(), new InsertContentAction(),
			new InsertTabAction(), new InvertSelectionCaseAction(), new JoinLinesAction(),
			new LowerSelectionCaseAction(), new LineMoveAction(RTextAreaEditorKit.rtaLineUpAction, -1),
			new LineMoveAction(RTextAreaEditorKit.rtaLineDownAction, 1),
			new NextBookmarkAction(RTextAreaEditorKit.rtaNextBookmarkAction, true),
			new NextBookmarkAction(RTextAreaEditorKit.rtaPrevBookmarkAction, false),
			new NextVisualPositionAction(DefaultEditorKit.forwardAction, false, SwingConstants.EAST),
			new NextVisualPositionAction(DefaultEditorKit.backwardAction, false, SwingConstants.WEST),
			new NextVisualPositionAction(DefaultEditorKit.selectionForwardAction, true, SwingConstants.EAST),
			new NextVisualPositionAction(DefaultEditorKit.selectionBackwardAction, true, SwingConstants.WEST),
			new NextVisualPositionAction(DefaultEditorKit.upAction, false, SwingConstants.NORTH),
			new NextVisualPositionAction(DefaultEditorKit.downAction, false, SwingConstants.SOUTH),
			new NextVisualPositionAction(DefaultEditorKit.selectionUpAction, true, SwingConstants.NORTH),
			new NextVisualPositionAction(DefaultEditorKit.selectionDownAction, true, SwingConstants.SOUTH),
			new NextOccurrenceAction(RTextAreaEditorKit.rtaNextOccurrenceAction),
			new PreviousOccurrenceAction(RTextAreaEditorKit.rtaPrevOccurrenceAction),
			new NextWordAction(DefaultEditorKit.nextWordAction, false),
			new NextWordAction(DefaultEditorKit.selectionNextWordAction, true),
			new PageAction(RTextAreaEditorKit.rtaSelectionPageLeftAction, true, true),
			new PageAction(RTextAreaEditorKit.rtaSelectionPageRightAction, false, true), new PasteAction(),
			new PlaybackLastMacroAction(), new PreviousWordAction(DefaultEditorKit.previousWordAction, false),
			new PreviousWordAction(DefaultEditorKit.selectionPreviousWordAction, true), new RedoAction(),
			new ScrollAction(RTextAreaEditorKit.rtaScrollUpAction, -1),
			new ScrollAction(RTextAreaEditorKit.rtaScrollDownAction, 1), new SelectAllAction(), new SelectLineAction(),
			new SelectWordAction(), new SetReadOnlyAction(), new SetWritableAction(), new ToggleBookmarkAction(),
			new ToggleTextModeAction(), new UndoAction(), new UnselectAction(), new UpperSelectionCaseAction(),
			new VerticalPageAction(DefaultEditorKit.pageUpAction, -1, false),
			new VerticalPageAction(DefaultEditorKit.pageDownAction, 1, false),
			new VerticalPageAction(RTextAreaEditorKit.rtaSelectionPageUpAction, -1, true),
			new VerticalPageAction(RTextAreaEditorKit.rtaSelectionPageDownAction, 1, true) };

	/**
	 * The amount of characters read at a time when reading a file.
	 */
	private static final int READBUFFER_SIZE = 32768;

	/**
	 * The name of the action that begins recording a macro.
	 */
	public static final String rtaBeginRecordingMacroAction = "RTA.BeginRecordingMacroAction";

	/**
	 * The name of the action to decrease the font size.
	 */
	public static final String rtaDecreaseFontSizeAction = "RTA.DecreaseFontSizeAction";

	/**
	 * The name of the action that deletes the current line.
	 */
	public static final String rtaDeleteLineAction = "RTA.DeleteLineAction";

	/**
	 * The name of the action to delete the word before the caret.
	 */
	public static final String rtaDeletePrevWordAction = "RTA.DeletePrevWordAction";

	/**
	 * The name of the action taken to delete the remainder of the line (from the
	 * caret position to the end of the line).
	 */
	public static final String rtaDeleteRestOfLineAction = "RTA.DeleteRestOfLineAction";

	/**
	 * The name of the action that completes the word at the caret position with the
	 * last word in the document that starts with the text up to the caret.
	 */
	public static final String rtaDumbCompleteWordAction = "RTA.DumbCompleteWordAction";

	/**
	 * The name of the action that ends recording a macro.
	 */
	public static final String rtaEndRecordingMacroAction = "RTA.EndRecordingMacroAction";

	/**
	 * The name of the action to increase the font size.
	 */
	public static final String rtaIncreaseFontSizeAction = "RTA.IncreaseFontSizeAction";

	/**
	 * The name of the action that inverts the case of the current selection.
	 */
	public static final String rtaInvertSelectionCaseAction = "RTA.InvertCaseAction";

	/**
	 * The name of the action to join two lines.
	 */
	public static final String rtaJoinLinesAction = "RTA.JoinLinesAction";

	/**
	 * Action to move a line down.
	 */
	public static final String rtaLineDownAction = "RTA.LineDownAction";

	/**
	 * Action to move a line up.
	 */
	public static final String rtaLineUpAction = "RTA.LineUpAction";

	/**
	 * The name of the action to make the current selection lower-case.
	 */
	public static final String rtaLowerSelectionCaseAction = "RTA.LowerCaseAction";

	/**
	 * Action to jump to the next bookmark.
	 */
	public static final String rtaNextBookmarkAction = "RTA.NextBookmarkAction";

	/**
	 * Action to select the next occurrence of the selected text.
	 */
	public static final String rtaNextOccurrenceAction = "RTA.NextOccurrenceAction";

	/**
	 * The name of the action that "plays back" the last macro.
	 */
	public static final String rtaPlaybackLastMacroAction = "RTA.PlaybackLastMacroAction";

	/**
	 * Action to jump to the previous bookmark.
	 */
	public static final String rtaPrevBookmarkAction = "RTA.PrevBookmarkAction";

	/**
	 * Action to select the previous occurrence of the selected text.
	 */
	public static final String rtaPrevOccurrenceAction = "RTA.PrevOccurrenceAction";

	/**
	 * The name of the action for "redoing" the last action undone.
	 */
	public static final String rtaRedoAction = "RTA.RedoAction";

	/**
	 * The name of the action to scroll the text area down one line without changing
	 * the caret's position.
	 */
	public static final String rtaScrollDownAction = "RTA.ScrollDownAction";

	/**
	 * The name of the action to scroll the text area up one line without changing
	 * the caret's position.
	 */
	public static final String rtaScrollUpAction = "RTA.ScrollUpAction";

	/**
	 * The name of the action for "paging down" with the selection.
	 */
	public static final String rtaSelectionPageDownAction = "RTA.SelectionPageDownAction";

	/**
	 * The name of the action for "paging left" with the selection.
	 */
	public static final String rtaSelectionPageLeftAction = "RTA.SelectionPageLeftAction";

	/**
	 * The name of the action for "paging right" with the selection.
	 */
	public static final String rtaSelectionPageRightAction = "RTA.SelectionPageRightAction";

	/**
	 * The name of the action for "paging up" with the selection.
	 */
	public static final String rtaSelectionPageUpAction = "RTA.SelectionPageUpAction";

	/**
	 * The name of the action for inserting a time/date stamp.
	 */
	public static final String rtaTimeDateAction = "RTA.TimeDateAction";

	/**
	 * Toggles whether the current line has a bookmark, if this text area is in an
	 * {@link RTextScrollPane}.
	 */
	public static final String rtaToggleBookmarkAction = "RTA.ToggleBookmarkAction";

	/**
	 * The name of the action taken when the user hits the Insert key (thus toggling
	 * between insert and overwrite modes).
	 */
	public static final String rtaToggleTextModeAction = "RTA.ToggleTextModeAction";

	/**
	 * The name of the action for "undoing" the last action done.
	 */
	public static final String rtaUndoAction = "RTA.UndoAction";

	/**
	 * The name of the action for unselecting any selected text in the text area.
	 */
	public static final String rtaUnselectAction = "RTA.UnselectAction";

	/**
	 * The name of the action for making the current selection upper-case.
	 */
	public static final String rtaUpperSelectionCaseAction = "RTA.UpperCaseAction";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public RTextAreaEditorKit() {
		super();
	}

	/**
	 * Creates an icon row header to use in the gutter for a text area.
	 *
	 * @param textArea
	 *            The text area.
	 * @return The icon row header.
	 */
	public IconRowHeader createIconRowHeader(final RTextArea textArea) {
		return new IconRowHeader(textArea);
	}

	/**
	 * Creates a line number list to use in the gutter for a text area.
	 *
	 * @param textArea
	 *            The text area.
	 * @return The line number list.
	 */
	public LineNumberList createLineNumberList(final RTextArea textArea) {
		return new LineNumberList(textArea);
	}

	/**
	 * Fetches the set of commands that can be used on a text component that is
	 * using a model and view produced by this kit.
	 *
	 * @return the command list
	 */
	@Override
	public Action[] getActions() {
		return RTextAreaEditorKit.defaultActions;
	}

	/**
	 * Inserts content from the given stream, which will be treated as plain text.
	 * This method is overridden merely so we can increase the number of characters
	 * read at a time.
	 *
	 * @param in
	 *            The stream to read from
	 * @param doc
	 *            The destination for the insertion.
	 * @param pos
	 *            The location in the document to place the content &gt;= 0.
	 * @exception IOException
	 *                on any I/O error
	 * @exception BadLocationException
	 *                if pos represents an invalid location within the document.
	 */
	@Override
	public void read(final Reader in, final Document doc, int pos) throws IOException, BadLocationException {

		final char[] buff = new char[RTextAreaEditorKit.READBUFFER_SIZE];
		int nch;
		boolean lastWasCR = false;
		boolean isCRLF = false;
		boolean isCR = false;
		int last;
		final boolean wasEmpty = doc.getLength() == 0;

		// Read in a block at a time, mapping \r\n to \n, as well as single
		// \r's to \n's. If a \r\n is encountered, \r\n will be set as the
		// newline string for the document, if \r is encountered it will
		// be set as the newline character, otherwise the newline property
		// for the document will be removed.
		while ((nch = in.read(buff, 0, buff.length)) != -1) {
			last = 0;
			for (int counter = 0; counter < nch; counter++)
				switch (buff[counter]) {
				case '\r':
					if (lastWasCR) {
						isCR = true;
						if (counter == 0) {
							doc.insertString(pos, "\n", null);
							pos++;
						} else
							buff[counter - 1] = '\n';
					} else
						lastWasCR = true;
					break;
				case '\n':
					if (lastWasCR) {
						if (counter > last + 1) {
							doc.insertString(pos, new String(buff, last, counter - last - 1), null);
							pos += counter - last - 1;
						}
						// else nothing to do, can skip \r, next write will
						// write \n
						lastWasCR = false;
						last = counter;
						isCRLF = true;
					}
					break;
				default:
					if (lastWasCR) {
						isCR = true;
						if (counter == 0) {
							doc.insertString(pos, "\n", null);
							pos++;
						} else
							buff[counter - 1] = '\n';
						lastWasCR = false;
					}
					break;
				} // End of switch (buff[counter]).

			if (last < nch)
				if (lastWasCR) {
					if (last < nch - 1) {
						doc.insertString(pos, new String(buff, last, nch - last - 1), null);
						pos += nch - last - 1;
					}
				} else {
					doc.insertString(pos, new String(buff, last, nch - last), null);
					pos += nch - last;
				}

		} // End of while ((nch = in.read(buff, 0, buff.length)) != -1).

		if (lastWasCR) {
			doc.insertString(pos, "\n", null);
			isCR = true;
		}

		if (wasEmpty)
			if (isCRLF)
				doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\r\n");
			else if (isCR)
				doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\r");
			else
				doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");

	}

}