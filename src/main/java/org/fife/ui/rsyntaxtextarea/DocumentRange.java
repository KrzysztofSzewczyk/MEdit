/*
 * 08/11/2009
 *
 * DocumentRange.java - A range of text in a document.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

/**
 * A range of text in a document.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DocumentRange implements Comparable<DocumentRange> {

	private int endOffs;
	private int startOffs;

	/**
	 * Constructor.
	 *
	 * @param startOffs
	 *            The starting offset in the document, inclusive.
	 * @param endOffs
	 *            The ending offset in the document, exclusive.
	 * @throws IllegalArgumentException
	 *             If <code>endOffs</code> is less than <code>startOffs</code>, or
	 *             either argument is less than zero.
	 */
	public DocumentRange(final int startOffs, final int endOffs) {
		this.set(startOffs, endOffs);
	}

	/**
	 * Compares this document range to another.
	 *
	 * @param other
	 *            Another document range.
	 * @return How the two should be sorted relative to each other.
	 */
	@Override
	public int compareTo(final DocumentRange other) {
		if (other == null)
			return 1;
		final int diff = this.startOffs - other.startOffs;
		if (diff != 0)
			return diff;
		return this.endOffs - other.endOffs;
	}

	/**
	 * Returns whether this document range is equal to another one.
	 *
	 * @param other
	 *            Another object, presumably a document range.
	 * @return Whether <code>other</code> is also a document range, and equal to
	 *         this one.
	 */
	@Override
	public boolean equals(final Object other) {
		if (other == this)
			return true;
		if (other instanceof DocumentRange)
			return this.compareTo((DocumentRange) other) == 0;
		return false;
	}

	/**
	 * Gets the end offset of the range.
	 *
	 * @return The end offset.
	 * @see #getStartOffset()
	 */
	public int getEndOffset() {
		return this.endOffs;
	}

	/**
	 * Gets the starting offset of the range.
	 *
	 * @return The starting offset.
	 * @see #getEndOffset()
	 */
	public int getStartOffset() {
		return this.startOffs;
	}

	/**
	 * Overridden simply as a best practice, since {@link #equals(Object)} is
	 * overridden.
	 *
	 * @return The hash code for this object.
	 */
	@Override
	public int hashCode() {
		return this.startOffs + this.endOffs;
	}

	/**
	 * Returns whether this document range has zero length. This can happen, for
	 * example, with regex searches of forms like <code>"foo|"</code>, where the
	 * right-hand sub-expression matches empty strings.
	 *
	 * @return Whether this document range has zero length.
	 */
	public boolean isZeroLength() {
		return this.startOffs == this.endOffs;
	}

	/**
	 * Sets the document range.
	 *
	 * @param start
	 *            The new start value, inclusive.
	 * @param end
	 *            The new end value, exclusive.
	 * @throws IllegalArgumentException
	 *             If <code>end</code> is less than <code>start</code>, or either
	 *             argument is less than zero.
	 */
	public void set(final int start, final int end) {
		if (start < 0 || end < 0)
			throw new IllegalArgumentException("start and end must be >= 0 (" + start + "-" + end + ")");
		if (end < start)
			throw new IllegalArgumentException("'end' cannot be less than 'start' (" + start + "-" + end + ")");
		this.startOffs = start;
		this.endOffs = end;
	}

	/**
	 * Returns a string representation of this object.
	 *
	 * @return A string representation of this object.
	 */
	@Override
	public String toString() {
		return "[DocumentRange: " + this.startOffs + "-" + this.endOffs + "]";
	}

	/**
	 * Translates this document range by a given amount.
	 *
	 * @param amount
	 *            The amount to translate this range by.
	 * @return This (modified) range.
	 */
	public DocumentRange translate(final int amount) {
		this.startOffs += amount;
		this.endOffs += amount;
		return this;
	}

}