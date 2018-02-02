/*
 * 06/30/2012
 *
 * RDocumentCharSequence.java - Iterator over a portion of an RTextArea's
 * document.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import javax.swing.text.BadLocationException;

/**
 * Allows iterating over a portion of an <code>RDocument</code>. This is of
 * course not thread-safe, so should only be used on the EDT or with external
 * synchronization.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RDocumentCharSequence implements CharSequence {

	private final RDocument doc;
	private final int end;
	private final int start;

	/**
	 * Creates a <code>CharSequence</code> representing the text in a document from
	 * the specified offset to the end of that document.
	 *
	 * @param doc
	 *            The document.
	 * @param start
	 *            The starting offset in the document, inclusive.
	 */
	RDocumentCharSequence(final RDocument doc, final int start) {
		this(doc, start, doc.getLength());
	}

	/**
	 * Constructor.
	 *
	 * @param doc
	 *            The document.
	 * @param start
	 *            The starting offset in the document, inclusive.
	 * @param end
	 *            the ending offset in the document, exclusive.
	 */
	RDocumentCharSequence(final RDocument doc, final int start, final int end) {
		this.doc = doc;
		this.start = start;
		this.end = end;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char charAt(final int index) {
		if (index < 0 || index >= this.length())
			throw new IndexOutOfBoundsException("Index " + index + " is not in range [0-" + this.length() + ")");
		try {
			return this.doc.charAt(this.start + index);
		} catch (final BadLocationException ble) {
			throw new IndexOutOfBoundsException(ble.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int length() {
		return this.end - this.start;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence subSequence(final int start, final int end) {
		if (start < 0)
			throw new IndexOutOfBoundsException("start must be >= 0 (" + start + ")");
		else if (end < 0)
			throw new IndexOutOfBoundsException("end must be >= 0 (" + end + ")");
		else if (end > this.length())
			throw new IndexOutOfBoundsException("end must be <= " + this.length() + " (" + end + ")");
		else if (start > end)
			throw new IndexOutOfBoundsException("start (" + start + ") cannot be > end (" + end + ")");
		final int newStart = this.start + start;
		final int newEnd = this.start + end;
		return new RDocumentCharSequence(this.doc, newStart, newEnd);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		try {
			return this.doc.getText(this.start, this.length());
		} catch (final BadLocationException ble) { // Never happens
			ble.printStackTrace();
			return "";
		}
	}

}