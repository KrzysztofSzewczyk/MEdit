/*
 * 07/28/2008
 *
 * RtfToText.java - Returns the plain text version of RTF documents.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Gets the plain text version of RTF documents.
 * <p>
 *
 * This is used by <code>RtfTransferable</code> to return the plain text version
 * of the transferable when the receiver does not support RTF.
 *
 * @author Robert Futrell
 * @version 1.0
 */
final class RtfToText {

	/**
	 * Converts the contents of the specified byte array representing an RTF
	 * document into plain text.
	 *
	 * @param rtf
	 *            The byte array representing an RTF document.
	 * @return The contents of the RTF document, in plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static String getPlainText(final byte[] rtf) throws IOException {
		return RtfToText.getPlainText(new ByteArrayInputStream(rtf));
	}

	/**
	 * Converts the contents of the specified RTF file to plain text.
	 *
	 * @param file
	 *            The RTF file to convert.
	 * @return The contents of the file, in plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static String getPlainText(final File file) throws IOException {
		return RtfToText.getPlainText(new BufferedReader(new FileReader(file)));
	}

	/**
	 * Converts the contents of the specified input stream to plain text. The input
	 * stream will be closed when this method returns.
	 *
	 * @param in
	 *            The input stream to convert. This will be closed when this method
	 *            returns.
	 * @return The contents of the stream, in plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static String getPlainText(final InputStream in) throws IOException {
		return RtfToText.getPlainText(new InputStreamReader(in, "US-ASCII"));
	}

	/**
	 * Converts the contents of the specified <code>Reader</code> to plain text.
	 *
	 * @param r
	 *            The <code>Reader</code>. This will be closed when this method
	 *            returns.
	 * @return The contents of the <code>Reader</code>, in plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private static String getPlainText(final Reader r) throws IOException {
		try {
			final RtfToText converter = new RtfToText(r);
			return converter.convert();
		} finally {
			r.close();
		}
	}

	/**
	 * Converts the contents of the specified String to plain text.
	 *
	 * @param rtf
	 *            A string whose contents represent an RTF document.
	 * @return The contents of the String, in plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static String getPlainText(final String rtf) throws IOException {
		return RtfToText.getPlainText(new StringReader(rtf));
	}

	private int blockCount;

	private final StringBuilder controlWord;

	private boolean inControlWord;

	private final Reader r;

	private final StringBuilder sb;

	/**
	 * Private constructor.
	 *
	 * @param r
	 *            The reader to read RTF text from.
	 */
	private RtfToText(final Reader r) {
		this.r = r;
		this.sb = new StringBuilder();
		this.controlWord = new StringBuilder();
		this.blockCount = 0;
		this.inControlWord = false;
	}

	/**
	 * Converts the RTF text read from this converter's <code>Reader</code> into
	 * plain text. It is the caller's responsibility to close the reader after this
	 * method is called.
	 *
	 * @return The plain text.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private String convert() throws IOException {

		// Skip over first curly brace as the whole file is in '{' and '}'
		int i = this.r.read();
		if (i != '{')
			throw new IOException("Invalid RTF file");

		while ((i = this.r.read()) != -1) {

			final char ch = (char) i;
			switch (ch) {
			case '{':
				if (this.inControlWord && this.controlWord.length() == 0) { // "\{"
					this.sb.append('{');
					this.controlWord.setLength(0);
					this.inControlWord = false;
				} else
					this.blockCount++;
				break;
			case '}':
				if (this.inControlWord && this.controlWord.length() == 0) { // "\}"
					this.sb.append('}');
					this.controlWord.setLength(0);
					this.inControlWord = false;
				} else
					this.blockCount--;
				break;
			case '\\':
				if (this.blockCount == 0)
					if (this.inControlWord) {
						if (this.controlWord.length() == 0) { // "\\"
							this.sb.append('\\');
							this.controlWord.setLength(0);
							this.inControlWord = false;
						} else {
							this.endControlWord();
							this.inControlWord = true;
						}
					} else
						this.inControlWord = true;
				break;
			case ' ':
				if (this.blockCount == 0)
					if (this.inControlWord)
						this.endControlWord();
					else
						this.sb.append(' ');
				break;
			case '\r':
			case '\n':
				if (this.blockCount == 0)
					if (this.inControlWord)
						this.endControlWord();
				break;
			default:
				if (this.blockCount == 0)
					if (this.inControlWord)
						this.controlWord.append(ch);
					else
						this.sb.append(ch);
				break;
			}

		}

		return this.sb.toString();

	}

	/**
	 * Ends a control word. Checks whether it is a common one that affects the plain
	 * text output (such as "<code>par</code>" or "<code>tab</code>") and updates
	 * the text buffer accordingly.
	 */
	private void endControlWord() {
		final String word = this.controlWord.toString();
		if ("par".equals(word))
			this.sb.append('\n');
		else if ("tab".equals(word))
			this.sb.append('\t');
		this.controlWord.setLength(0);
		this.inControlWord = false;
	}

}