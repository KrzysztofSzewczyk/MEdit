/*
 * 08/28/2013
 *
 * TokenIterator.java - An iterator over the Tokens in an RSyntaxDocument.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.Iterator;

/**
 * Allows you to iterate through all paintable tokens in an
 * <code>RSyntaxDocument</code>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class TokenIterator implements Iterator<Token> {

	private int curLine;
	private final RSyntaxDocument doc;
	private Token token;

	/**
	 * Constructor.
	 *
	 * @param doc
	 *            The document whose tokens we should iterate over.
	 */
	TokenIterator(final RSyntaxDocument doc) {
		this.doc = doc;
		this.loadTokenListForCurLine();
		final int lineCount = this.getLineCount();
		while ((this.token == null || !this.token.isPaintable()) && this.curLine < lineCount - 1) {
			this.curLine++;
			this.loadTokenListForCurLine();
		}
	}

	private int getLineCount() {
		return this.doc.getDefaultRootElement().getElementCount();
	}

	/**
	 * Returns whether any more paintable tokens are in the document.
	 *
	 * @return Whether there are any more paintable tokens.
	 * @see #next()
	 */
	@Override
	public boolean hasNext() {
		return this.token != null;
	}

	private void loadTokenListForCurLine() {
		this.token = this.doc.getTokenListForLine(this.curLine);
		if (this.token != null && !this.token.isPaintable())
			// Common end of document scenario
			this.token = null;
	}

	/**
	 * Returns the next paintable token in the document.
	 *
	 * @return The next paintable token in the document.
	 * @see #hasNext()
	 */
	@Override
	public Token next() {

		Token t = this.token;
		boolean tIsCloned = false;
		final int lineCount = this.getLineCount();

		// Get the next token, going to the next line if necessary.
		if (this.token != null && this.token.isPaintable())
			this.token = this.token.getNextToken();
		else if (this.curLine < lineCount - 1) {
			t = new TokenImpl(t); // Clone t since tokens are pooled
			tIsCloned = true;
			this.curLine++;
			this.loadTokenListForCurLine();
		} else if (this.token != null && !this.token.isPaintable())
			// Ends with a non-paintable token (not sure this ever happens)
			this.token = null;

		while ((this.token == null || !this.token.isPaintable()) && this.curLine < lineCount - 1) {
			if (!tIsCloned) {
				t = new TokenImpl(t); // Clone t since tokens are pooled
				tIsCloned = true;
			}
			this.curLine++;
			this.loadTokenListForCurLine();
		}
		if (this.token != null && !this.token.isPaintable() && this.curLine == lineCount - 1)
			this.token = null;

		return t;

	}

	/**
	 * Always throws {@link UnsupportedOperationException}, as <code>Token</code>
	 * removal is not supported.
	 *
	 * @throws UnsupportedOperationException
	 *             always.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}