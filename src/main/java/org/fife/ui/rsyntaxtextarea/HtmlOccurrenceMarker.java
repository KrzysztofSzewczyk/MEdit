/*
 * 12/01/2014
 *
 * Copyright (C) 2013 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.fife.ui.rtextarea.SmartHighlightPainter;

/**
 * Marks occurrences of the current token for HTML. Tags that require a closing
 * tag have their "opposite" tag closed.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class HtmlOccurrenceMarker implements OccurrenceMarker {

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

	private static final Set<String> TAGS_REQUIRING_CLOSING = HtmlOccurrenceMarker.getRequiredClosingTags();

	public static final Set<String> getRequiredClosingTags() {
		final String[] tags = { "html", "head", "title", "style", "script", "noscript", "body", "section", "nav",
				"article", "aside", "h1", "h2", "h3", "h4", "h5", "h6", "header", "footer", "address", "pre", "dialog",
				"blockquote", "ol", "ul", "dl", "a", "q", "cite", "em", "strong", "small", "mark", "dfn", "abbr",
				"time", "progress", "meter", "code", "var", "samp", "kbd", "sub", "sup", "span", "i", "b", "bdo",
				"ruby", "rt", "rp", "ins", "del", "figure", "iframe", "object", "video", "audio", "canvas", "map",
				"table", "caption", "form", "fieldset", "label", "button", "select", "datalist", "textarea", "output",
				"details", "bb", "menu", "legend", "div",
				// Obsolete elements
				"acronym", "applet", "big", "blink", "center", "dir", "font", "frame", "frameset", "isindex", "listing",
				"marquee", "nobr", "noembed", "noframes", "plaintext", "s", "spacer", "strike", "tt", "u", "xmp", };
		return new HashSet<>(Arrays.asList(tags));
	}

	/**
	 * If the caret is inside of a tag, this method returns the token representing
	 * the tag name; otherwise, <code>null</code> is returned.
	 * <p>
	 *
	 * Currently, this method only checks for tag names on the same line as the
	 * caret, for simplicity. In the future it could check prior lines until the tag
	 * name is found.
	 *
	 * @param textArea
	 *            The text area.
	 * @param occurrenceMarker
	 *            The occurrence marker.
	 * @return The token to mark occurrences of. Note that, if the specified
	 *         occurrence marker identifies tokens other than tag names, these other
	 *         element types may be returned.
	 */
	public static final Token getTagNameTokenForCaretOffset(final RSyntaxTextArea textArea,
			final OccurrenceMarker occurrenceMarker) {

		// Get the tag name token.
		// For now, we only check for tags on the current line, for simplicity.

		final int dot = textArea.getCaretPosition();
		Token t = textArea.getTokenListForLine(textArea.getCaretLineNumber());
		Token toMark = null;

		while (t != null && t.isPaintable()) {
			if (t.getType() == TokenTypes.MARKUP_TAG_NAME)
				toMark = t;
			// Check for the token containing the caret before checking
			// if it's the close token.
			if (t.getEndOffset() == dot || t.containsPosition(dot)) {
				// Some languages, like PHP, mark functions/variables (PHP,
				// JavaScirpt) as well as HTML tags.
				if (occurrenceMarker.isValidType(textArea, t) && t.getType() != TokenTypes.MARKUP_TAG_NAME)
					return t;
				if (t.containsPosition(dot))
					break;
			}
			if (t.getType() == TokenTypes.MARKUP_TAG_DELIMITER)
				if (t.isSingleChar('>') || t.is(HtmlOccurrenceMarker.TAG_SELF_CLOSE))
					toMark = null;
			t = t.getNextToken();
		}

		return toMark;

	}

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

		if (t.getType() != TokenTypes.MARKUP_TAG_NAME) {
			DefaultOccurrenceMarker.markOccurrencesOfToken(doc, t, h, p);
			return;
		}

		String lexemeStr = t.getLexeme();
		final char[] lexeme = lexemeStr.toCharArray();
		lexemeStr = lexemeStr.toLowerCase();
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
					// Don't try to match a tag that is optionally closed (or
					// closing is forbidden entirely).
					if (HtmlOccurrenceMarker.TAGS_REQUIRING_CLOSING.contains(lexemeStr))
						found = true;
					break;
				} else if (t.is(HtmlOccurrenceMarker.CLOSE_TAG_START) && t.getOffset() + 2 == tokenOffs) {
					// Searching backward, we assume we can find the opening
					// tag. Don't really care if it's valid or not.
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
						if (t.is(HtmlOccurrenceMarker.CLOSE_TAG_START)) {
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
						else if (inPossibleMatch && t.is(HtmlOccurrenceMarker.TAG_SELF_CLOSE)) {
							openCloses.remove(openCloses.size() - 1);
							inPossibleMatch = false;
						} else if (t.is(HtmlOccurrenceMarker.CLOSE_TAG_START)) {
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