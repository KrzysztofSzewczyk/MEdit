/*
 * 06/30/2012
 *
 * RDocument.java - Document class used by RTextAreas.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import javax.swing.text.BadLocationException;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;

/**
 * The document implementation used by instances of <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RDocument extends PlainDocument {

	/**
	 * Document content that provides fast access to individual characters.
	 */
	private static class RGapContent extends GapContent {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public char charAt(final int offset) throws BadLocationException {
			if (offset < 0 || offset >= this.length())
				throw new BadLocationException("Invalid offset", offset);
			final int g0 = this.getGapStart();
			final char[] array = (char[]) this.getArray();
			if (offset < g0)
				return array[offset];
			return array[this.getGapEnd() + offset - g0]; // above gap
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public RDocument() {
		super(new RGapContent());
	}

	/**
	 * Returns the character in the document at the specified offset.
	 *
	 * @param offset
	 *            The offset of the character.
	 * @return The character.
	 * @throws BadLocationException
	 *             If the offset is invalid.
	 */
	public char charAt(final int offset) throws BadLocationException {
		return ((RGapContent) this.getContent()).charAt(offset);
	}

}