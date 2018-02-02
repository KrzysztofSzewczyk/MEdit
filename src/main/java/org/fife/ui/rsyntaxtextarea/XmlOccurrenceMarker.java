/*
 * 03/09/2013
 *
 * XmlOccurrenceMarker - Marks occurrences of the current token for XML.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.fife.ui.rtextarea.SmartHighlightPainter;

/**
 * Marks occurrences of the current token for XML.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class XmlOccurrenceMarker implements OccurrenceMarker {

	/**
	 * Used internally when searching backward for a matching "open" tag.
	 */
	private static class Entry {

		private final boolean open;
		private final Token t;

		Entry(final boolean open, final Token t) {
			this.open = open;
			this.t = t;
		}

	}

	private static final char[] CLOSE_TAG_START = { '<', '/' };

	private static final char[] TAG_SELF_CLOSE = { '/', '>' };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Token getTokenToMark(final RSyntaxTextArea textArea) {
		return HtmlOccurrenceMarker.getTagNameTokenForCaretOffset(textArea, this);
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
	public void markOccurrences(final RSyntaxDocument doc, Token t, final RSyntaxTextAreaHighlighter h,
			final SmartHighlightPainter p) {

		final char[] lexeme = t.getLexeme().toCharArray();
		final int tokenOffs = t.getOffset();
		final Element root = doc.getDefaultRootElement();
		final int lineCount = root.getElementCount();
		int curLine = root.getElementIndex(t.getOffset());
		int depth = 0;

		// For now, we only check for tags on the current line, for
		// simplicity. Tags spanning multiple lines aren't common anyway.
		boolean found = false;
		boolean forward = true;
		t = doc.getTokenListForLine(curLine);
		while (t != null && t.isPaintable()) {
			if (t.getType() == TokenTypes.MARKUP_TAG_DELIMITER)
				if (t.isSingleChar('<') && t.getOffset() + 1 == tokenOffs) {
					found = true;
					break;
				} else if (t.is(XmlOccurrenceMarker.CLOSE_TAG_START) && t.getOffset() + 2 == tokenOffs) {
					found = true;
					forward = false;
					break;
				}
			t = t.getNextToken();
		}

		if (!found)
			return;

		if (forward) {

			t = t.getNextToken().getNextToken();

			do {

				while (t != null && t.isPaintable()) {
					if (t.getType() == TokenTypes.MARKUP_TAG_DELIMITER)
						if (t.is(XmlOccurrenceMarker.CLOSE_TAG_START)) {
							final Token match = t.getNextToken();
							if (match != null && match.is(lexeme))
								if (depth > 0)
									depth--;
								else {
									try {
										int end = match.getOffset() + match.length();
										h.addMarkedOccurrenceHighlight(match.getOffset(), end, p);
										end = tokenOffs + match.length();
										h.addMarkedOccurrenceHighlight(tokenOffs, end, p);
									} catch (final BadLocationException ble) {
										ble.printStackTrace(); // Never happens
									}
									return; // We're done!
								}
						} else if (t.isSingleChar('<')) {
							t = t.getNextToken();
							if (t != null && t.is(lexeme))
								depth++;
						}
					t = t == null ? null : t.getNextToken();
				}

				if (++curLine < lineCount)
					t = doc.getTokenListForLine(curLine);

			} while (curLine < lineCount);

		}

		else { // !forward

			// Idea: Get all opening and closing tags of the relevant type on
			// the current line. Find the opening tag paired to the closing
			// tag we found originally; if it's not on this line, keep going
			// to the previous line.

			final List<Entry> openCloses = new ArrayList<>();
			boolean inPossibleMatch = false;
			t = doc.getTokenListForLine(curLine);
			final int endBefore = tokenOffs - 2; // Stop before "</".

			do {

				while (t != null && t.getOffset() < endBefore && t.isPaintable()) {
					if (t.getType() == TokenTypes.MARKUP_TAG_DELIMITER)
						if (t.isSingleChar('<')) {
							final Token next = t.getNextToken();
							if (next != null) {
								if (next.is(lexeme)) {
									openCloses.add(new Entry(true, next));
									inPossibleMatch = true;
								} else
									inPossibleMatch = false;
								t = next;
							}
						} else if (t.isSingleChar('>'))
							inPossibleMatch = false;
						else if (inPossibleMatch && t.is(XmlOccurrenceMarker.TAG_SELF_CLOSE)) {
							openCloses.remove(openCloses.size() - 1);
							inPossibleMatch = false;
						} else if (t.is(XmlOccurrenceMarker.CLOSE_TAG_START)) {
							final Token next = t.getNextToken();
							if (next != null) {
								// Invalid XML might not have a match
								if (next.is(lexeme))
									openCloses.add(new Entry(false, next));
								t = next;
							}
						}
					t = t.getNextToken();
				}

				for (int i = openCloses.size() - 1; i >= 0; i--) {
					final Entry entry = openCloses.get(i);
					depth += entry.open ? -1 : 1;
					if (depth == -1) {
						try {
							final Token match = entry.t;
							int end = match.getOffset() + match.length();
							h.addMarkedOccurrenceHighlight(match.getOffset(), end, p);
							end = tokenOffs + match.length();
							h.addMarkedOccurrenceHighlight(tokenOffs, end, p);
						} catch (final BadLocationException ble) {
							ble.printStackTrace(); // Never happens
						}
						openCloses.clear();
						return;
					}
				}

				openCloses.clear();
				if (--curLine >= 0)
					t = doc.getTokenListForLine(curLine);

			} while (curLine >= 0);

		}

	}

}