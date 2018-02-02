/*
 * 08/11/2009
 *
 * DefaultParserNotice.java - Base implementation of a parser notice.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.awt.Color;

/**
 * Base implementation of a parser notice. Most <code>Parser</code>
 * implementations can return instances of this in their parse result.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Parser
 * @see ParseResult
 */
public class DefaultParserNotice implements ParserNotice {

	private static final Color[] DEFAULT_COLORS = { new Color(255, 0, 128), // Error
			new Color(244, 200, 45), // Warning
			Color.gray, // Info
	};
	private Color color;
	private final int length;
	private Level level;
	private final int line;
	private final String message;
	private final int offset;
	private final Parser parser;
	private boolean showInEditor;

	private String toolTipText;

	/**
	 * Constructor.
	 *
	 * @param parser
	 *            The parser that created this notice.
	 * @param msg
	 *            The text of the message.
	 * @param line
	 *            The line number for the message.
	 */
	public DefaultParserNotice(final Parser parser, final String msg, final int line) {
		this(parser, msg, line, -1, -1);
	}

	/**
	 * Constructor.
	 *
	 * @param parser
	 *            The parser that created this notice.
	 * @param message
	 *            The message.
	 * @param line
	 *            The line number corresponding to the message.
	 * @param offset
	 *            The offset in the input stream of the code the message is
	 *            concerned with, or <code>-1</code> if unknown.
	 * @param length
	 *            The length of the code the message is concerned with, or
	 *            <code>-1</code> if unknown.
	 */
	public DefaultParserNotice(final Parser parser, final String message, final int line, final int offset,
			final int length) {
		this.parser = parser;
		this.message = message;
		this.line = line;
		this.offset = offset;
		this.length = length;
		this.setLevel(Level.ERROR);
		this.setShowInEditor(true);
	}

	/**
	 * Compares this parser notice to another.
	 *
	 * @param other
	 *            Another parser notice.
	 * @return How the two parser notices should be sorted relative to one another.
	 */
	@Override
	public int compareTo(final ParserNotice other) {
		int diff = -1;
		if (other != null) {
			diff = this.level.getNumericValue() - other.getLevel().getNumericValue();
			if (diff == 0) {
				diff = this.line - other.getLine();
				if (diff == 0)
					diff = this.message.compareTo(other.getMessage());
			}
		}
		return diff;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsPosition(final int pos) {
		return this.offset <= pos && pos < this.offset + this.length;
	}

	/**
	 * Returns whether this parser notice is equal to another one.
	 *
	 * @param obj
	 *            Another parser notice.
	 * @return Whether the two notices are equal.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof ParserNotice))
			return false;
		return this.compareTo((ParserNotice) obj) == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Color getColor() {
		Color c = this.color; // User-defined
		if (c == null)
			c = DefaultParserNotice.DEFAULT_COLORS[this.getLevel().getNumericValue()];
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getKnowsOffsetAndLength() {
		return this.offset >= 0 && this.length >= 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLength() {
		return this.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Level getLevel() {
		return this.level;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLine() {
		return this.line;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMessage() {
		return this.message;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getOffset() {
		return this.offset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Parser getParser() {
		return this.parser;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getShowInEditor() {
		return this.showInEditor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getToolTipText() {
		return this.toolTipText != null ? this.toolTipText : this.getMessage();
	}

	/**
	 * Returns the hash code for this notice.
	 *
	 * @return The hash code.
	 */
	@Override
	public int hashCode() {
		return this.line << 16 | this.offset;
	}

	/**
	 * Sets the color to use when painting this notice.
	 *
	 * @param color
	 *            The color to use.
	 * @see #getColor()
	 */
	public void setColor(final Color color) {
		this.color = color;
	}

	/**
	 * Sets the level of this notice.
	 *
	 * @param level
	 *            The new level.
	 * @see #getLevel()
	 */
	public void setLevel(Level level) {
		if (level == null)
			level = Level.ERROR;
		this.level = level;
	}

	/**
	 * Sets whether a squiggle underline should be drawn in the editor for this
	 * notice.
	 *
	 * @param show
	 *            Whether to draw a squiggle underline.
	 * @see #getShowInEditor()
	 */
	public void setShowInEditor(final boolean show) {
		this.showInEditor = show;
	}

	/**
	 * Sets the tool tip text to display for this notice.
	 *
	 * @param text
	 *            The new tool tip text. This can be HTML. If this is
	 *            <code>null</code>, then tool tips will return the same text as
	 *            {@link #getMessage()}.
	 * @see #getToolTipText()
	 */
	public void setToolTipText(final String text) {
		this.toolTipText = text;
	}

	/**
	 * Returns a string representation of this parser notice.
	 *
	 * @return This parser notice as a string.
	 */
	@Override
	public String toString() {
		return "Line " + this.getLine() + ": " + this.getMessage();
	}

}