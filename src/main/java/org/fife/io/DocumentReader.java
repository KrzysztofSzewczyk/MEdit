/*
 * 02/24/2004
 *
 * DocumentReader.java - A reader for javax.swing.text.Document
 * objects.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.io;

import java.io.Reader;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * A <code>Reader</code> for <code>javax.swing.text.Document</code> objects.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DocumentReader extends Reader {

	/**
	 * The document we're working on.
	 */
	private final Document document;

	/**
	 * A remembered position in the document.
	 */
	private long mark;

	/**
	 * The stream's position in the document.
	 */
	private long position;

	/**
	 * Used for fast character access.
	 */
	private final Segment segment;

	/**
	 * Constructor.
	 *
	 * @param document
	 *            The document we're 'reading'.
	 */
	public DocumentReader(final Document document) {
		this.position = 0;
		this.mark = -1;
		this.document = document;
		this.segment = new Segment();
	}

	/**
	 * This currently does nothing...
	 */
	@Override
	public void close() {
	}

	/**
	 * Marks the present position in the stream. Subsequent calls to
	 * <code>reset()</code> will reposition the stream to this point.
	 *
	 * @param readAheadLimit
	 *            Ignored.
	 */
	@Override
	public void mark(final int readAheadLimit) {
		this.mark = this.position;
	}

	/**
	 * Tells whether this reader supports the <code>mark</code> operation. This
	 * always returns <code>true</code> for <code>DocumentReader</code>.
	 */
	@Override
	public boolean markSupported() {
		return true;
	}

	/**
	 * Reads the single character at the current position in the document.
	 */
	@Override
	public int read() {
		if (this.position >= this.document.getLength())
			return -1; // Read past end of document.
		try {
			this.document.getText((int) this.position, 1, this.segment);
			this.position++;
			return this.segment.array[this.segment.offset];
		} catch (final BadLocationException ble) {
			/* Should never happen?? */
			ble.printStackTrace();
			return -1;
		}
	}

	/**
	 * Read <code>array.length</code> characters from the beginning of the document
	 * into <code>array</code>.
	 *
	 * @param array
	 *            The array to read characters into.
	 * @return The number of characters read.
	 */
	@Override
	public int read(final char[] array) {
		return this.read(array, 0, array.length);
	}

	/**
	 * Reads characters into a portion of an array.
	 *
	 * @param cbuf
	 *            The destination buffer.
	 * @param off
	 *            Offset at which to start storing characters.
	 * @param len
	 *            Maximum number of characters to read.
	 * @return The number of characters read, or <code>-1</code> if the end of the
	 *         stream (document) has been reached.
	 */
	@Override
	public int read(final char[] cbuf, final int off, final int len) {
		int k;
		if (this.position >= this.document.getLength())
			return -1; // Read past end of document.
		k = len;
		if (this.position + k >= this.document.getLength())
			k = this.document.getLength() - (int) this.position;
		if (off + k >= cbuf.length)
			k = cbuf.length - off;
		try {
			this.document.getText((int) this.position, k, this.segment);
			this.position += k;
			System.arraycopy(this.segment.array, this.segment.offset, cbuf, off, k);
			return k;
		} catch (final BadLocationException ble) {
			/* Should never happen ? */
			return -1;
		}
	}

	/**
	 * Tells whether this reader is ready to be read without blocking for input.
	 * <code>DocumentReader</code> will always return true.
	 *
	 * @return <code>true</code> if the next read operation will return without
	 *         blocking.
	 */
	@Override
	public boolean ready() {
		return true;
	}

	/**
	 * Resets the stream. If the stream has been marked, then attempt to reposition
	 * it at the mark. If the stream has not been marked, then move it to the
	 * beginning of the document.
	 */
	@Override
	public void reset() {
		if (this.mark == -1)
			this.position = 0;
		else {
			this.position = this.mark;
			this.mark = -1;
		}
	}

	/**
	 * Move to the specified position in the document. If <code>pos</code> is
	 * greater than the document's length, the stream's position is moved to the end
	 * of the document.
	 *
	 * @param pos
	 *            The position in the document to move to.
	 */
	public void seek(final long pos) {
		this.position = Math.min(pos, this.document.getLength());
	}

	/**
	 * Skips characters. This will not 'skip' past the end of the document.
	 *
	 * @param n
	 *            The number of characters to skip.
	 * @return The number of characters actually skipped.
	 */
	@Override
	public long skip(final long n) {
		if (this.position + n <= this.document.getLength()) {
			this.position += n;
			return n;
		}
		final long temp = this.position;
		this.position = this.document.getLength();
		return this.document.getLength() - temp;
	}

}