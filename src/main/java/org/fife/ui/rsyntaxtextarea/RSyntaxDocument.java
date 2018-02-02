/*
 * 10/16/2004
 *
 * RSyntaxDocument.java - A document capable of syntax highlighting, used by
 * RSyntaxTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.modes.AbstractMarkupTokenMaker;
import org.fife.ui.rtextarea.RDocument;
import org.fife.util.DynamicIntArray;

/**
 * The document used by {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}.
 * This document is like <code>javax.swing.text.PlainDocument</code> except that
 * it also keeps track of syntax highlighting in the document. It has a "style"
 * attribute associated with it that determines how syntax highlighting is done
 * (i.e., what language is being highlighted).
 * <p>
 *
 * Instances of <code>RSyntaxTextArea</code> will only accept instances of
 * <code>RSyntaxDocument</code>, since it is this document that keeps track of
 * syntax highlighting. All others will cause an exception to be thrown.
 * <p>
 *
 * To change the language being syntax highlighted at any time, you merely have
 * to call {@link #setSyntaxStyle}. Other than that, this document can be
 * treated like any other save one caveat: all <code>DocumentEvent</code>s of
 * type <code>CHANGE</code> use their offset and length values to represent the
 * first and last lines, respectively, that have had their syntax coloring
 * change. This is really a hack to increase the speed of the painting code and
 * should really be corrected, but oh well.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RSyntaxDocument extends RDocument implements Iterable<Token>, SyntaxConstants {

	/**
	 * If this is set to <code>true</code>, debug information about how much token
	 * caching is helping is printed to stdout.
	 */
	private static final boolean DEBUG_TOKEN_CACHING = false;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private transient Token cachedTokenList;

	private transient int lastLine = -1;

	/**
	 * Array of values representing the "last token type" on each line. This is used
	 * in cases such as multi-line comments: if the previous line ended with an
	 * (unclosed) multi-line comment, we can use this knowledge and start the
	 * current line's syntax highlighting in multi-line comment state.
	 */
	protected transient DynamicIntArray lastTokensOnLines;

	private transient Segment s;
	/**
	 * The current syntax style. Only cached to keep this class serializable.
	 */
	private String syntaxStyle;
	/**
	 * Splits text into tokens for the current programming language.
	 */
	private transient TokenMaker tokenMaker;
	/**
	 * Creates a {@link TokenMaker} appropriate for a given programming language.
	 */
	private transient TokenMakerFactory tokenMakerFactory;

	private transient int tokenRetrievalCount = 0;

	private transient int useCacheCount = 0;

	/**
	 * Constructs a plain text document. A default root element is created, and the
	 * tab size set to 5.
	 *
	 * @param syntaxStyle
	 *            The syntax highlighting scheme to use.
	 */
	public RSyntaxDocument(final String syntaxStyle) {
		this(null, syntaxStyle);
	}

	/**
	 * Constructs a plain text document. A default root element is created, and the
	 * tab size set to 5.
	 *
	 * @param tmf
	 *            The <code>TokenMakerFactory</code> for this document. If this is
	 *            <code>null</code>, a default factory is used.
	 * @param syntaxStyle
	 *            The syntax highlighting scheme to use.
	 */
	public RSyntaxDocument(final TokenMakerFactory tmf, final String syntaxStyle) {
		this.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(5));
		this.lastTokensOnLines = new DynamicIntArray(400);
		this.lastTokensOnLines.add(TokenTypes.NULL); // Initial (empty) line.
		this.s = new Segment();
		this.setTokenMakerFactory(tmf);
		this.setSyntaxStyle(syntaxStyle);
	}

	/**
	 * Alerts all listeners to this document of an insertion. This is overridden so
	 * we can update our syntax highlighting stuff.
	 * <p>
	 * The syntax highlighting stuff has to be here instead of in
	 * <code>insertUpdate</code> because <code>insertUpdate</code> is not called by
	 * the undo/redo actions, but this method is.
	 *
	 * @param e
	 *            The change.
	 */
	@Override
	protected void fireInsertUpdate(final DocumentEvent e) {

		this.cachedTokenList = null;

		/*
		 * Now that the text is actually inserted into the content and element
		 * structure, we can update our token elements and "last tokens on lines"
		 * structure.
		 */

		final Element lineMap = this.getDefaultRootElement();
		final DocumentEvent.ElementChange change = e.getChange(lineMap);
		final Element[] added = change == null ? null : change.getChildrenAdded();

		final int numLines = lineMap.getElementCount();
		final int line = lineMap.getElementIndex(e.getOffset());
		final int previousLine = line - 1;
		int previousTokenType = previousLine > -1 ? this.lastTokensOnLines.get(previousLine) : TokenTypes.NULL;

		// If entire lines were added...
		if (added != null && added.length > 0) {

			final Element[] removed = change.getChildrenRemoved();
			final int numRemoved = removed != null ? removed.length : 0;

			final int endBefore = line + added.length - numRemoved;
			// System.err.println("... adding lines: " + line + " - " + (endBefore-1));
			// System.err.println("... ... added: " + added.length + ", removed:" +
			// numRemoved);
			for (int i = line; i < endBefore; i++) {

				this.setSharedSegment(i); // Loads line i's text into s.

				final int tokenType = this.tokenMaker.getLastTokenTypeOnLine(this.s, previousTokenType);
				this.lastTokensOnLines.add(i, tokenType);
				// System.err.println("--------- lastTokensOnLines.size() == " +
				// lastTokensOnLines.getSize());

				previousTokenType = tokenType;

			} // End of for (int i=line; i<endBefore; i++).

			// Update last tokens for lines below until they stop changing.
			this.updateLastTokensBelow(endBefore, numLines, previousTokenType);

		} // End of if (added!=null && added.length>0).
		else // Update last tokens for lines below until they stop changing.
			this.updateLastTokensBelow(line, numLines, previousTokenType);

		// Let all listeners know about the insertion.
		super.fireInsertUpdate(e);

	}

	/**
	 * This method is called AFTER the content has been inserted into the document
	 * and the element structure has been updated.
	 * <p>
	 * The syntax-highlighting updates need to be done here (as opposed to an
	 * override of <code>postRemoveUpdate</code>) as this method is called in
	 * response to undo/redo events, whereas <code>postRemoveUpdate</code> is not.
	 * <p>
	 * Now that the text is actually inserted into the content and element
	 * structure, we can update our token elements and "last tokens on lines"
	 * structure.
	 *
	 * @param chng
	 *            The change that occurred.
	 * @see #removeUpdate
	 */
	@Override
	protected void fireRemoveUpdate(final DocumentEvent chng) {

		this.cachedTokenList = null;
		final Element lineMap = this.getDefaultRootElement();
		final int numLines = lineMap.getElementCount();

		final DocumentEvent.ElementChange change = chng.getChange(lineMap);
		final Element[] removed = change == null ? null : change.getChildrenRemoved();

		// If entire lines were removed...
		if (removed != null && removed.length > 0) {

			final int line = change.getIndex(); // First line entirely removed.
			final int previousLine = line - 1; // Line before that.
			final int previousTokenType = previousLine > -1 ? this.lastTokensOnLines.get(previousLine)
					: TokenTypes.NULL;

			final Element[] added = change.getChildrenAdded();
			final int numAdded = added == null ? 0 : added.length;

			// Remove the cached last-token values for the removed lines.
			final int endBefore = line + removed.length - numAdded;
			// System.err.println("... removing lines: " + line + " - " + (endBefore-1));
			// System.err.println("... added: " + numAdded + ", removed: " +
			// removed.length);

			this.lastTokensOnLines.removeRange(line, endBefore); // Removing values for lines [line-(endBefore-1)].
			// System.err.println("--------- lastTokensOnLines.size() == " +
			// lastTokensOnLines.getSize());

			// Update last tokens for lines below until they've stopped changing.
			this.updateLastTokensBelow(line, numLines, previousTokenType);

		} // End of if (removed!=null && removed.size()>0).

		// Otherwise, text was removed from just one line...
		else {

			final int line = lineMap.getElementIndex(chng.getOffset());
			if (line >= this.lastTokensOnLines.getSize())
				return; // If we're editing the last line in a document...

			final int previousLine = line - 1;
			final int previousTokenType = previousLine > -1 ? this.lastTokensOnLines.get(previousLine)
					: TokenTypes.NULL;
			// System.err.println("previousTokenType for line : " + previousLine + " is " +
			// previousTokenType);
			// Update last tokens for lines below until they've stopped changing.
			this.updateLastTokensBelow(line, numLines, previousTokenType);

		}

		// Let all of our listeners know about the removal.
		super.fireRemoveUpdate(chng);

	}

	/**
	 * Returns the closest {@link TokenTypes "standard" token type} for a given
	 * "internal" token type (e.g. one whose value is <code>&lt; 0</code>).
	 *
	 * @param type
	 *            The token type.
	 * @return The closest "standard" token type. If a mapping is not defined for
	 *         this language, then <code>type</code> is returned.
	 */
	public int getClosestStandardTokenTypeForInternalType(final int type) {
		return this.tokenMaker.getClosestStandardTokenTypeForInternalType(type);
	}

	/**
	 * Returns whether closing markup tags should be automatically completed. This
	 * method only returns <code>true</code> if {@link #getLanguageIsMarkup()} also
	 * returns <code>true</code>.
	 *
	 * @return Whether markup closing tags should be automatically completed.
	 * @see #getLanguageIsMarkup()
	 */
	public boolean getCompleteMarkupCloseTags() {
		// TODO: Remove terrible dependency on AbstractMarkupTokenMaker
		return this.getLanguageIsMarkup() && ((AbstractMarkupTokenMaker) this.tokenMaker).getCompleteCloseTags();
	}

	/**
	 * Returns whether the current programming language uses curly braces
	 * ('<code>{</code>' and '<code>}</code>') to denote code blocks.
	 *
	 * @param languageIndex
	 *            The language index at the offset in question. Since some
	 *            <code>TokenMaker</code>s effectively have nested languages (such
	 *            as JavaScript in HTML), this parameter tells the
	 *            <code>TokenMaker</code> what sub-language to look at.
	 * @return Whether curly braces denote code blocks.
	 */
	public boolean getCurlyBracesDenoteCodeBlocks(final int languageIndex) {
		return this.tokenMaker.getCurlyBracesDenoteCodeBlocks(languageIndex);
	}

	/**
	 * Returns whether the current language is a markup language, such as HTML, XML
	 * or PHP.
	 *
	 * @return Whether the current language is a markup language.
	 */
	public boolean getLanguageIsMarkup() {
		return this.tokenMaker.isMarkupLanguage();
	}

	/**
	 * Returns the token type of the last token on the given line.
	 *
	 * @param line
	 *            The line to inspect.
	 * @return The token type of the last token on the specified line. If the line
	 *         is invalid, an exception is thrown.
	 */
	public int getLastTokenTypeOnLine(final int line) {
		return this.lastTokensOnLines.get(line);
	}

	/**
	 * Returns the text to place at the beginning and end of a line to "comment" it
	 * in this programming language.
	 *
	 * @return The start and end strings to add to a line to "comment" it out. A
	 *         <code>null</code> value for either means there is no string to add
	 *         for that part. A value of <code>null</code> for the array means this
	 *         language does not support commenting/uncommenting lines.
	 */
	public String[] getLineCommentStartAndEnd(final int languageIndex) {
		return this.tokenMaker.getLineCommentStartAndEnd(languageIndex);
	}

	/**
	 * Returns whether tokens of the specified type should have "mark occurrences"
	 * enabled for the current programming language.
	 *
	 * @param type
	 *            The token type.
	 * @return Whether tokens of this type should have "mark occurrences" enabled.
	 */
	boolean getMarkOccurrencesOfTokenType(final int type) {
		return this.tokenMaker.getMarkOccurrencesOfTokenType(type);
	}

	/**
	 * Returns the occurrence marker for the current language.
	 *
	 * @return The occurrence marker.
	 */
	OccurrenceMarker getOccurrenceMarker() {
		return this.tokenMaker.getOccurrenceMarker();
	}

	/**
	 * This method returns whether auto indentation should be done if Enter is
	 * pressed at the end of the specified line.
	 *
	 * @param line
	 *            The line to check.
	 * @return Whether an extra indentation should be done.
	 */
	public boolean getShouldIndentNextLine(final int line) {
		Token t = this.getTokenListForLine(line);
		t = t.getLastNonCommentNonWhitespaceToken();
		return this.tokenMaker.getShouldIndentNextLineAfter(t);
	}

	/**
	 * Returns the syntax style being used.
	 *
	 * @return The syntax style.
	 * @see #setSyntaxStyle(String)
	 */
	public String getSyntaxStyle() {
		return this.syntaxStyle;
	}

	/**
	 * Returns a token list for the specified segment of text representing the
	 * specified line number. This method is basically a wrapper for
	 * <code>tokenMaker.getTokenList</code> that takes into account the last token
	 * on the previous line to assure token accuracy.
	 *
	 * @param line
	 *            The line number of <code>text</code> in the document, &gt;= 0.
	 * @return A token list representing the specified line.
	 */
	public final Token getTokenListForLine(final int line) {

		this.tokenRetrievalCount++;
		if (line == this.lastLine && this.cachedTokenList != null) {
			if (RSyntaxDocument.DEBUG_TOKEN_CACHING) {
				this.useCacheCount++;
				System.err.println(
						"--- Using cached line; ratio now: " + this.useCacheCount + "/" + this.tokenRetrievalCount);
			}
			return this.cachedTokenList;
		}
		this.lastLine = line;

		final Element map = this.getDefaultRootElement();
		final Element elem = map.getElement(line);
		final int startOffset = elem.getStartOffset();
		// int endOffset = (line==map.getElementCount()-1 ? elem.getEndOffset() - 1:
		// elem.getEndOffset() - 1);
		final int endOffset = elem.getEndOffset() - 1; // Why always "-1"?
		try {
			this.getText(startOffset, endOffset - startOffset, this.s);
		} catch (final BadLocationException ble) {
			ble.printStackTrace();
			return null;
		}
		final int initialTokenType = line == 0 ? TokenTypes.NULL : this.getLastTokenTypeOnLine(line - 1);

		// return tokenMaker.getTokenList(s, initialTokenType, startOffset);
		this.cachedTokenList = this.tokenMaker.getTokenList(this.s, initialTokenType, startOffset);
		return this.cachedTokenList;

	}

	boolean insertBreakSpecialHandling(final ActionEvent e) {
		final Action a = this.tokenMaker.getInsertBreakAction();
		if (a != null) {
			a.actionPerformed(e);
			return true;
		}
		return false;
	}

	/**
	 * Returns whether a character could be part of an "identifier" token in a
	 * specific language. This is used to identify such things as the bounds of the
	 * "word" to select on double-clicking.
	 *
	 * @param languageIndex
	 *            The language index the character was found in.
	 * @param ch
	 *            The character.
	 * @return Whether the character could be part of an "identifier" token.
	 */
	public boolean isIdentifierChar(final int languageIndex, final char ch) {
		return this.tokenMaker.isIdentifierChar(languageIndex, ch);
	}

	/**
	 * Returns an iterator over the paintable tokens in this document. Results are
	 * undefined if this document is modified while the iterator is being iterated
	 * through, so this should only be used on the EDT.
	 * <p>
	 *
	 * The <code>remove()</code> method of the returned iterator will throw an
	 * <code>UnsupportedOperationException</code>.
	 *
	 * @return An iterator.
	 */
	@Override
	public Iterator<Token> iterator() {
		return new TokenIterator(this);
	}

	/**
	 * Deserializes a document.
	 *
	 * @param in
	 *            The stream to read from.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(final ObjectInputStream in) throws ClassNotFoundException, IOException {

		in.defaultReadObject();

		// Install default TokenMakerFactory. To support custom TokenMakers,
		// both JVM's should install default TokenMakerFactories that support
		// the language they want to use beforehand.
		this.setTokenMakerFactory(null);

		// Handle other transient stuff
		this.s = new Segment();
		final int lineCount = this.getDefaultRootElement().getElementCount();
		this.lastTokensOnLines = new DynamicIntArray(lineCount);
		this.setSyntaxStyle(this.syntaxStyle); // Actually install (transient) TokenMaker

	}

	/**
	 * Makes our private <code>Segment s</code> point to the text in our document
	 * referenced by the specified element. Note that <code>line</code> MUST be a
	 * valid line number in the document.
	 *
	 * @param line
	 *            The line number you want to get.
	 */
	private void setSharedSegment(final int line) {

		final Element map = this.getDefaultRootElement();
		// int numLines = map.getElementCount();

		final Element element = map.getElement(line);
		if (element == null)
			throw new InternalError("Invalid line number: " + line);
		final int startOffset = element.getStartOffset();
		// int endOffset = (line==numLines-1 ?
		// element.getEndOffset()-1 : element.getEndOffset() - 1);
		final int endOffset = element.getEndOffset() - 1; // Why always "-1"?
		try {
			this.getText(startOffset, endOffset - startOffset, this.s);
		} catch (final BadLocationException ble) {
			throw new InternalError("Text range not in document: " + startOffset + "-" + endOffset);
		}

	}

	/**
	 * Sets the syntax style being used for syntax highlighting in this document.
	 * What styles are supported by a document is determined by its
	 * {@link TokenMakerFactory}. By default, all <code>RSyntaxDocument</code>s
	 * support all languages built into <code>RSyntaxTextArea</code>.
	 *
	 * @param styleKey
	 *            The new style to use, such as
	 *            {@link SyntaxConstants#SYNTAX_STYLE_JAVA}. If this style is not
	 *            known or supported by this document, then
	 *            {@link SyntaxConstants#SYNTAX_STYLE_NONE} is used.
	 * @see #setSyntaxStyle(TokenMaker)
	 * @see #getSyntaxStyle()
	 */
	public void setSyntaxStyle(final String styleKey) {
		this.tokenMaker = this.tokenMakerFactory.getTokenMaker(styleKey);
		this.updateSyntaxHighlightingInformation();
		this.syntaxStyle = styleKey;
	}

	/**
	 * Sets the syntax style being used for syntax highlighting in this document.
	 * You should call this method if you've created a custom token maker for a
	 * language not normally supported by <code>RSyntaxTextArea</code>.
	 *
	 * @param tokenMaker
	 *            The new token maker to use.
	 * @see #setSyntaxStyle(String)
	 */
	public void setSyntaxStyle(final TokenMaker tokenMaker) {
		this.tokenMaker = tokenMaker;
		this.updateSyntaxHighlightingInformation();
		this.syntaxStyle = "text/unknown"; // TODO: Make me public?
	}

	/**
	 * Sets the token maker factory used by this document.
	 *
	 * @param tmf
	 *            The <code>TokenMakerFactory</code> for this document. If this is
	 *            <code>null</code>, a default factory is used.
	 */
	public void setTokenMakerFactory(final TokenMakerFactory tmf) {
		this.tokenMakerFactory = tmf != null ? tmf : TokenMakerFactory.getDefaultInstance();
	}

	/**
	 * Loops through the last-tokens-on-lines array from a specified point onward,
	 * updating last-token values until they stop changing. This should be called
	 * when lines are updated/inserted/removed, as doing so may cause lines below to
	 * change color.
	 *
	 * @param line
	 *            The first line to check for a change in last-token value.
	 * @param numLines
	 *            The number of lines in the document.
	 * @param previousTokenType
	 *            The last-token value of the line just before <code>line</code>.
	 * @return The last line that needs repainting.
	 */
	private int updateLastTokensBelow(int line, final int numLines, int previousTokenType) {

		final int firstLine = line;

		// Loop through all lines past our starting point. Update even the last
		// line's info, even though there aren't any lines after it that depend
		// on it changing for them to be changed, as its state may be used
		// elsewhere in the library.
		final int end = numLines;
		// System.err.println("--- end==" + end + " (numLines==" + numLines + ")");
		while (line < end) {

			this.setSharedSegment(line); // Sets s's text to that of line 'line' in the document.

			final int oldTokenType = this.lastTokensOnLines.get(line);
			final int newTokenType = this.tokenMaker.getLastTokenTypeOnLine(this.s, previousTokenType);
			// System.err.println("---------------- line " + line + "; oldTokenType==" +
			// oldTokenType + ", newTokenType==" + newTokenType + ", s=='" + s + "'");

			// If this line's end-token value didn't change, stop here. Note
			// that we're saying this line needs repainting; this is because
			// the beginning of this line did indeed change color, but the
			// end didn't.
			if (oldTokenType == newTokenType) {
				// System.err.println("... ... ... repainting lines " + firstLine + "-" + line);
				this.fireChangedUpdate(new DefaultDocumentEvent(firstLine, line, DocumentEvent.EventType.CHANGE));
				return line;
			}

			// If the line's end-token value did change, update it and
			// keep going.
			// NOTE: "setUnsafe" is okay here as the bounds checking was
			// already done in lastTokensOnLines.get(line) above.
			this.lastTokensOnLines.setUnsafe(line, newTokenType);
			previousTokenType = newTokenType;
			line++;

		} // End of while (line<numLines).

		// If any lines had their token types changed, fire a changed update
		// for them. The view will repaint the area covered by the lines.
		// FIXME: We currently cheat and send the line range that needs to be
		// repainted as the "offset and length" of the change, since this is
		// what the view needs. We really should send the actual offset and
		// length.
		if (line > firstLine)
			// System.err.println("... ... ... repainting lines " + firstLine + "-" + line);
			this.fireChangedUpdate(new DefaultDocumentEvent(firstLine, line, DocumentEvent.EventType.CHANGE));

		return line;

	}

	/**
	 * Updates internal state information; e.g. the "last tokens on lines" data.
	 * After this, a changed update is fired to let listeners know that the
	 * document's structure has changed.
	 * <p>
	 *
	 * This is called internally whenever the syntax style changes.
	 */
	private void updateSyntaxHighlightingInformation() {

		// Reinitialize the "last token on each line" array. Note that since
		// the actual text in the document isn't changing, the number of lines
		// is the same.
		final Element map = this.getDefaultRootElement();
		final int numLines = map.getElementCount();
		int lastTokenType = TokenTypes.NULL;
		for (int i = 0; i < numLines; i++) {
			this.setSharedSegment(i);
			lastTokenType = this.tokenMaker.getLastTokenTypeOnLine(this.s, lastTokenType);
			this.lastTokensOnLines.set(i, lastTokenType);
		}

		// Clear our token cache to force re-painting
		this.lastLine = -1;
		this.cachedTokenList = null;

		// Let everybody know that syntax styles have (probably) changed.
		this.fireChangedUpdate(new DefaultDocumentEvent(0, numLines - 1, DocumentEvent.EventType.CHANGE));

	}

}