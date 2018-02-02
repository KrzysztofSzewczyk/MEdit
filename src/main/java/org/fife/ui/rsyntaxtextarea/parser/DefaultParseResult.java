/*
 * 07/27/2009
 *
 * DefaultParseResult.java - A basic implementation of a ParseResult.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic implementation of {@link ParseResult}. Most, if not all,
 * <code>Parser</code>s can return instances of this class.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Parser
 */
public class DefaultParseResult implements ParseResult {

	private Exception error;
	private int firstLineParsed;
	private int lastLineParsed;
	private final List<ParserNotice> notices;
	private final Parser parser;
	private long parseTime;

	public DefaultParseResult(final Parser parser) {
		this.parser = parser;
		this.notices = new ArrayList<>();
	}

	/**
	 * Adds a parser notice.
	 *
	 * @param notice
	 *            The new notice.
	 * @see #clearNotices()
	 */
	public void addNotice(final ParserNotice notice) {
		this.notices.add(notice);
	}

	/**
	 * Clears any parser notices in this result.
	 *
	 * @see #addNotice(ParserNotice)
	 */
	public void clearNotices() {
		this.notices.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Exception getError() {
		return this.error;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getFirstLineParsed() {
		return this.firstLineParsed;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getLastLineParsed() {
		return this.lastLineParsed;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParserNotice> getNotices() {
		return this.notices;
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
	public long getParseTime() {
		return this.parseTime;
	}

	/**
	 * Sets the error that occurred when last parsing the document, if any.
	 *
	 * @param e
	 *            The error that occurred, or <code>null</code> if no error
	 *            occurred.
	 */
	public void setError(final Exception e) {
		this.error = e;
	}

	/**
	 * Sets the line range parsed.
	 *
	 * @param first
	 *            The first line parsed, inclusive.
	 * @param last
	 *            The last line parsed, inclusive.
	 * @see #getFirstLineParsed()
	 * @see #getLastLineParsed()
	 */
	public void setParsedLines(final int first, final int last) {
		this.firstLineParsed = first;
		this.lastLineParsed = last;
	}

	/**
	 * Sets the amount of time it took for this parser to parse the document.
	 *
	 * @param time
	 *            The amount of time, in milliseconds.
	 * @see #getParseTime()
	 */
	public void setParseTime(final long time) {
		this.parseTime = time;
	}

}