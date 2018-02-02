/*
 * 03/09/2013
 *
 * DefaultOccurrenceMarker - Marks occurrences of the current token for most
 * languages.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;

import org.fife.ui.rtextarea.SmartHighlightPainter;

/**
 * The default implementation of {@link OccurrenceMarker}. It goes through the
 * document and marks all instances of the specified token.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class DefaultOccurrenceMarker implements OccurrenceMarker {

	/**
	 * Highlights all instances of tokens identical to <code>t</code> in the
	 * specified document.
	 *
	 * @param doc
	 *            The document.
	 * @param t
	 *            The document whose relevant occurrences should be marked.
	 * @param h
	 *            The highlighter to add the highlights to.
	 * @param p
	 *            The painter for the highlights.
	 */
	public static final void markOccurrencesOfToken(final RSyntaxDocument doc, final Token t,
			final RSyntaxTextAreaHighlighter h, final SmartHighlightPainter p) {

		final char[] lexeme = t.getLexeme().toCharArray();
		final int type = t.getType();
		final int lineCount = doc.getDefaultRootElement().getElementCount();

		for (int i = 0; i < lineCount; i++) {
			Token temp = doc.getTokenListForLine(i);
			while (temp != null && temp.isPaintable()) {
				if (temp.is(type, lexeme))
					try {
						final int end = temp.getEndOffset();
						h.addMarkedOccurrenceHighlight(temp.getOffset(), end, p);
					} catch (final BadLocationException ble) {
						ble.printStackTrace(); // Never happens
					}
				temp = temp.getNextToken();
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Token getTokenToMark(final RSyntaxTextArea textArea) {

		// Get the token at the caret position.
		final int line = textArea.getCaretLineNumber();
		final Token tokenList = textArea.getTokenListForLine(line);
		final Caret c = textArea.getCaret();
		int dot = c.getDot();

		Token t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
		if (t == null /* EOL */ || !this.isValidType(textArea, t) || RSyntaxUtilities.isNonWordChar(t)) {
			// Try to the "left" of the caret.
			dot--;
			try {
				if (dot >= textArea.getLineStartOffset(line))
					t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}

		return t;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidType(final RSyntaxTextArea textArea, final Token t) {
		return textArea.getMarkOccurrencesOfTokenType(t.getType());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void markOccurrences(final RSyntaxDocument doc, final Token t, final RSyntaxTextAreaHighlighter h,
			final SmartHighlightPainter p) {
		DefaultOccurrenceMarker.markOccurrencesOfToken(doc, t, h, p);
	}

}