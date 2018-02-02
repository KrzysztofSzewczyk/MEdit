/*
 * 09/24/2004
 *
 * UnicodeWriter.java - Writes Unicode output with the proper BOM.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Writes Unicode text to an output stream. If the specified encoding is a
 * Unicode, then the text is preceded by the proper Unicode BOM. If it is any
 * other encoding, this class behaves just like <code>OutputStreamWriter</code>.
 * This class is here because Java's <code>OutputStreamWriter</code> apparently
 * doesn't believe in writing BOMs.
 * <p>
 *
 * For optimum performance, it is recommended that you wrap all instances of
 * <code>UnicodeWriter</code> with a <code>java.io.BufferedWriter</code>.
 *
 * @author Robert Futrell
 * @version 0.7
 */
public class UnicodeWriter extends Writer {

	/**
	 * If this system property evaluates to "<code>false</code>", ignoring case,
	 * files written out as UTF-8 will not have a BOM written for them. Otherwise
	 * (even if the property is not set), UTF-8 files will have a BOM written.
	 */
	public static final String PROPERTY_WRITE_UTF8_BOM = "UnicodeWriter.writeUtf8BOM";

	private static final byte[] UTF16BE_BOM = new byte[] { (byte) 0xFE, (byte) 0xFF };

	private static final byte[] UTF16LE_BOM = new byte[] { (byte) 0xFF, (byte) 0xFE };

	private static final byte[] UTF32BE_BOM = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF };

	private static final byte[] UTF32LE_BOM = new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 };

	private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	/**
	 * Returns whether UTF-8 files should have a BOM in them when written.
	 *
	 * @return Whether to write a BOM for UTF-8 files.
	 * @see #setWriteUtf8BOM(boolean)
	 * @see UnicodeWriter
	 */
	public static boolean getWriteUtf8BOM() {
		final String prop = System.getProperty(UnicodeWriter.PROPERTY_WRITE_UTF8_BOM);
		// We default to writing the BOM, for some reason.
		if (prop != null && Boolean.valueOf(prop).equals(Boolean.FALSE))
			return false;
		return true;
	}

	/**
	 * Sets whether UTF-8 files should have a BOM written in them.
	 *
	 * @param write
	 *            Whether to write a BOM.
	 * @see #getWriteUtf8BOM()
	 * @see UnicodeWriter
	 */
	public static void setWriteUtf8BOM(final boolean write) {
		System.setProperty(UnicodeWriter.PROPERTY_WRITE_UTF8_BOM, Boolean.toString(write));
	}

	/**
	 * The writer actually doing the writing.
	 */
	private OutputStreamWriter internalOut;

	/**
	 * This is a utility constructor since the vast majority of the time, this class
	 * will be used to write Unicode files.
	 *
	 * @param file
	 *            The file to which to write the Unicode output.
	 * @param encoding
	 *            The encoding to use.
	 * @throws IOException
	 *             If an IO exception occurs.
	 */
	public UnicodeWriter(final File file, final String encoding) throws IOException {
		this(new FileOutputStream(file), encoding);
	}

	/**
	 * Creates a new writer.
	 *
	 * @param out
	 *            The output stream to write.
	 * @param encoding
	 *            The encoding to use.
	 * @throws IOException
	 *             If an IO exception occurs.
	 */
	public UnicodeWriter(final OutputStream out, final String encoding) throws IOException {
		this.init(out, encoding);
	}

	/**
	 * This is a utility constructor since the vast majority of the time, this class
	 * will be used to write Unicode files.
	 *
	 * @param fileName
	 *            The file to which to write the Unicode output.
	 * @param encoding
	 *            The encoding to use.
	 * @throws IOException
	 *             If an IO exception occurs.
	 */
	public UnicodeWriter(final String fileName, final String encoding) throws IOException {
		this(new FileOutputStream(fileName), encoding);
	}

	/**
	 * Closes this writer.
	 *
	 * @throws IOException
	 *             If an IO exception occurs.
	 */
	@Override
	public void close() throws IOException {
		this.internalOut.close();
	}

	/**
	 * Flushes the stream.
	 *
	 * @throws IOException
	 *             If an IO exception occurs.
	 */
	@Override
	public void flush() throws IOException {
		this.internalOut.flush();
	}

	/**
	 * Returns the encoding being used to write this output stream (i.e., the
	 * encoding of the file).
	 *
	 * @return The encoding of the stream.
	 */
	public String getEncoding() {
		return this.internalOut.getEncoding();
	}

	/**
	 * Initializes the internal output stream and writes the BOM if the specified
	 * encoding is a Unicode encoding.
	 *
	 * @param out
	 *            The output stream we are writing.
	 * @param encoding
	 *            The encoding in which to write.
	 * @throws IOException
	 *             If an I/O error occurs while writing a BOM.
	 */
	private void init(final OutputStream out, final String encoding) throws IOException {

		this.internalOut = new OutputStreamWriter(out, encoding);

		// Write the proper BOM if they specified a Unicode encoding.
		// NOTE: Creating an OutputStreamWriter with encoding "UTF-16" DOES
		// DOES write out the BOM; "UTF-16LE", "UTF-16BE", "UTF-32", "UTF-32LE"
		// and "UTF-32BE" don't.
		if ("UTF-8".equals(encoding)) {
			if (UnicodeWriter.getWriteUtf8BOM())
				out.write(UnicodeWriter.UTF8_BOM, 0, UnicodeWriter.UTF8_BOM.length);
		} else if ("UTF-16LE".equals(encoding))
			out.write(UnicodeWriter.UTF16LE_BOM, 0, UnicodeWriter.UTF16LE_BOM.length);
		else if (/* "UTF-16".equals(encoding) || */"UTF-16BE".equals(encoding))
			out.write(UnicodeWriter.UTF16BE_BOM, 0, UnicodeWriter.UTF16BE_BOM.length);
		else if ("UTF-32LE".equals(encoding))
			out.write(UnicodeWriter.UTF32LE_BOM, 0, UnicodeWriter.UTF32LE_BOM.length);
		else if ("UTF-32".equals(encoding) || "UTF-32BE".equals(encoding))
			out.write(UnicodeWriter.UTF32BE_BOM, 0, UnicodeWriter.UTF32BE_BOM.length);

	}

	/**
	 * Writes a portion of an array of characters.
	 *
	 * @param cbuf
	 *            The buffer of characters.
	 * @param off
	 *            The offset from which to start writing characters.
	 * @param len
	 *            The number of characters to write.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		this.internalOut.write(cbuf, off, len);
	}

	/**
	 * Writes a single character.
	 *
	 * @param c
	 *            An integer specifying the character to write.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	@Override
	public void write(final int c) throws IOException {
		this.internalOut.write(c);
	}

	/**
	 * Writes a portion of a string.
	 *
	 * @param str
	 *            The string from which to write.
	 * @param off
	 *            The offset from which to start writing characters.
	 * @param len
	 *            The number of characters to write.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	@Override
	public void write(final String str, final int off, final int len) throws IOException {
		this.internalOut.write(str, off, len);
	}

}