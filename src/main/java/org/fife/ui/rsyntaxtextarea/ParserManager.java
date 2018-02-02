/*
 * 09/26/2005
 *
 * ParserManager.java - Manages the parsing of an RSyntaxTextArea's document,
 * if necessary.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ToolTipInfo;
import org.fife.ui.rtextarea.RDocument;
import org.fife.ui.rtextarea.RTextAreaHighlighter.HighlightInfo;

/**
 * Manages running a parser object for an <code>RSyntaxTextArea</code>.
 *
 * @author Robert Futrell
 * @version 0.9
 */
class ParserManager implements DocumentListener, ActionListener, HyperlinkListener, PropertyChangeListener {

	/**
	 * Mapping of a parser notice to its highlight in the editor.
	 */
	private static class NoticeHighlightPair {

		private final HighlightInfo highlight;
		private final ParserNotice notice;

		NoticeHighlightPair(final ParserNotice notice, final HighlightInfo highlight) {
			this.notice = notice;
			this.highlight = highlight;
		}

	}

	/**
	 * Whether to print debug messages while running parsers.
	 */
	private static final boolean DEBUG_PARSING;
	/**
	 * The default delay between the last key press and when the document is parsed,
	 * in milliseconds.
	 */
	private static final int DEFAULT_DELAY_MS = 1250;
	/**
	 * If this system property is set to <code>true</code>, debug messages will be
	 * printed to stdout to help diagnose parsing issues.
	 */
	private static final String PROPERTY_DEBUG_PARSING = "rsta.debugParsing";
	static {
		boolean debugParsing = false;
		try {
			debugParsing = Boolean.getBoolean(ParserManager.PROPERTY_DEBUG_PARSING);
		} catch (final AccessControlException ace) {
			// Likely an applet's security manager.
			debugParsing = false; // FindBugs
		}
		DEBUG_PARSING = debugParsing;
	}
	private Position firstOffsetModded;
	private Position lastOffsetModded;

	/**
	 * Mapping of notices to their highlights in the editor. Can't use a Map since
	 * parsers could return two <code>ParserNotice</code>s that compare equally via
	 * <code>equals()</code>. Real-world example: The Perl compiler will return 2+
	 * identical error messages if the same error is committed in a single line more
	 * than once.
	 */
	private List<NoticeHighlightPair> noticeHighlightPairs;

	/**
	 * Painter used to underline errors.
	 */
	private final SquiggleUnderlineHighlightPainter parserErrorHighlightPainter = new SquiggleUnderlineHighlightPainter(
			Color.RED);

	private Parser parserForTip;

	private final List<Parser> parsers;

	private boolean running;

	private final RSyntaxTextArea textArea;

	private final Timer timer;

	/**
	 * Constructor.
	 *
	 * @param delay
	 *            The delay between the last key press and when the document is
	 *            parsed.
	 * @param textArea
	 *            The text area whose document the parser will be parsing.
	 */
	ParserManager(final int delay, final RSyntaxTextArea textArea) {
		this.textArea = textArea;
		textArea.getDocument().addDocumentListener(this);
		textArea.addPropertyChangeListener("document", this);
		this.parsers = new ArrayList<>(1); // Usually small
		this.timer = new Timer(delay, this);
		this.timer.setRepeats(false);
		this.running = true;
	}

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The text area whose document the parser will be parsing.
	 */
	ParserManager(final RSyntaxTextArea textArea) {
		this(ParserManager.DEFAULT_DELAY_MS, textArea);
	}

	/**
	 * Called when the timer fires (e.g. it's time to parse the document).
	 *
	 * @param e
	 *            The event.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		// Sanity check - should have >1 parser if event is fired.
		final int parserCount = this.getParserCount();
		if (parserCount == 0)
			return;

		long begin = 0;
		if (ParserManager.DEBUG_PARSING)
			begin = System.currentTimeMillis();

		final RSyntaxDocument doc = (RSyntaxDocument) this.textArea.getDocument();

		final Element root = doc.getDefaultRootElement();
		final int firstLine = this.firstOffsetModded == null ? 0
				: root.getElementIndex(this.firstOffsetModded.getOffset());
		final int lastLine = this.lastOffsetModded == null ? root.getElementCount() - 1
				: root.getElementIndex(this.lastOffsetModded.getOffset());
		this.firstOffsetModded = this.lastOffsetModded = null;
		if (ParserManager.DEBUG_PARSING)
			System.out.println("[DEBUG]: Minimum lines to parse: " + firstLine + "-" + lastLine);

		final String style = this.textArea.getSyntaxEditingStyle();
		doc.readLock();
		try {
			for (int i = 0; i < parserCount; i++) {
				final Parser parser = this.getParser(i);
				if (parser.isEnabled()) {
					final ParseResult res = parser.parse(doc, style);
					this.addParserNoticeHighlights(res);
				} else
					this.clearParserNoticeHighlights(parser);
			}
			this.textArea.fireParserNoticesChange();
		} finally {
			doc.readUnlock();
		}

		if (ParserManager.DEBUG_PARSING) {
			final float time = (System.currentTimeMillis() - begin) / 1000f;
			System.out.println("Total parsing time: " + time + " seconds");
		}

	}

	/**
	 * Adds a parser for the text area.
	 *
	 * @param parser
	 *            The new parser. If this is <code>null</code>, nothing happens.
	 * @see #getParser(int)
	 * @see #removeParser(Parser)
	 */
	public void addParser(final Parser parser) {
		if (parser != null && !this.parsers.contains(parser)) {
			if (this.running)
				this.timer.stop();
			this.parsers.add(parser);
			if (this.parsers.size() == 1)
				// Okay to call more than once.
				ToolTipManager.sharedInstance().registerComponent(this.textArea);
			if (this.running)
				this.timer.restart();
		}
	}

	/**
	 * Adds highlights for a list of parser notices. Any current notices from the
	 * same Parser, in the same parsed range, are removed.
	 *
	 * @param res
	 *            The result of a parsing.
	 * @see #clearParserNoticeHighlights()
	 */
	private void addParserNoticeHighlights(final ParseResult res) {

		// Parsers are supposed to return at least empty ParseResults, but
		// we'll be defensive here.
		if (res == null)
			return;

		if (ParserManager.DEBUG_PARSING)
			System.out.println("[DEBUG]: Adding parser notices from " + res.getParser());

		if (this.noticeHighlightPairs == null)
			this.noticeHighlightPairs = new ArrayList<>();

		this.removeParserNotices(res);

		final List<ParserNotice> notices = res.getNotices();
		if (notices.size() > 0) { // Guaranteed non-null

			final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();

			for (final ParserNotice notice : notices) {
				if (ParserManager.DEBUG_PARSING)
					System.out.println("[DEBUG]: ... adding: " + notice);
				try {
					HighlightInfo highlight = null;
					if (notice.getShowInEditor())
						highlight = h.addParserHighlight(notice, this.parserErrorHighlightPainter);
					this.noticeHighlightPairs.add(new NoticeHighlightPair(notice, highlight));
				} catch (final BadLocationException ble) { // Never happens
					ble.printStackTrace();
				}
			}

		}

		if (ParserManager.DEBUG_PARSING)
			System.out.println("[DEBUG]: Done adding parser notices from " + res.getParser());

	}

	/**
	 * Called when the document is modified.
	 *
	 * @param e
	 *            The document event.
	 */
	@Override
	public void changedUpdate(final DocumentEvent e) {
	}

	private void clearParserNoticeHighlights() {
		final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
		if (h != null)
			h.clearParserHighlights();
		if (this.noticeHighlightPairs != null)
			this.noticeHighlightPairs.clear();
	}

	/**
	 * Removes all parser notice highlights for a specific parser.
	 *
	 * @param parser
	 *            The parser whose highlights to remove.
	 */
	private void clearParserNoticeHighlights(final Parser parser) {
		final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
		if (h != null)
			h.clearParserHighlights(parser);
		if (this.noticeHighlightPairs != null) {
			final Iterator<NoticeHighlightPair> i = this.noticeHighlightPairs.iterator();
			while (i.hasNext()) {
				final NoticeHighlightPair pair = i.next();
				if (pair.notice.getParser() == parser)
					i.remove();
			}
		}
	}

	/**
	 * Removes all parsers and any highlights they have created.
	 *
	 * @see #addParser(Parser)
	 */
	public void clearParsers() {
		this.timer.stop();
		this.clearParserNoticeHighlights();
		this.parsers.clear();
		this.textArea.fireParserNoticesChange();
	}

	/**
	 * Forces the given {@link Parser} to re-parse the content of this text area.
	 * <p>
	 *
	 * This method can be useful when a <code>Parser</code> can be configured as to
	 * what notices it returns. For example, if a Java language parser can be
	 * configured to set whether no serialVersionUID is a warning, error, or
	 * ignored, this method can be called after changing the expected notice type to
	 * have the document re-parsed.
	 *
	 * @param parser
	 *            The index of the <code>Parser</code> to re-run.
	 * @see #getParser(int)
	 */
	public void forceReparsing(final int parser) {
		final Parser p = this.getParser(parser);
		final RSyntaxDocument doc = (RSyntaxDocument) this.textArea.getDocument();
		final String style = this.textArea.getSyntaxEditingStyle();
		doc.readLock();
		try {
			if (p.isEnabled()) {
				final ParseResult res = p.parse(doc, style);
				this.addParserNoticeHighlights(res);
			} else
				this.clearParserNoticeHighlights(p);
			this.textArea.fireParserNoticesChange();
		} finally {
			doc.readUnlock();
		}
	}

	/**
	 * Returns the delay between the last "concurrent" edit and when the document is
	 * re-parsed.
	 *
	 * @return The delay, in milliseconds.
	 * @see #setDelay(int)
	 */
	public int getDelay() {
		return this.timer.getDelay();
	}

	/**
	 * Returns the specified parser.
	 *
	 * @param index
	 *            The index of the parser.
	 * @return The parser.
	 * @see #getParserCount()
	 * @see #addParser(Parser)
	 * @see #removeParser(Parser)
	 */
	public Parser getParser(final int index) {
		return this.parsers.get(index);
	}

	/**
	 * Returns the number of registered parsers.
	 *
	 * @return The number of registered parsers.
	 */
	public int getParserCount() {
		return this.parsers.size();
	}

	/**
	 * Returns a list of the current parser notices for this text area. This method
	 * (like most Swing methods) should only be called on the EDT.
	 *
	 * @return The list of notices. This will be an empty list if there are none.
	 */
	public List<ParserNotice> getParserNotices() {
		final List<ParserNotice> notices = new ArrayList<>();
		if (this.noticeHighlightPairs != null)
			for (final NoticeHighlightPair pair : this.noticeHighlightPairs)
				notices.add(pair.notice);
		return notices;
	}

	/**
	 * Returns the tool tip to display for a mouse event at the given location. This
	 * method is overridden to give a registered parser a chance to display a tool
	 * tip (such as an error description when the mouse is over an error highlight).
	 *
	 * @param e
	 *            The mouse event.
	 * @return The tool tip to display, and possibly a hyperlink event handler.
	 */
	public ToolTipInfo getToolTipText(final MouseEvent e) {

		String tip = null;
		HyperlinkListener listener = null;
		this.parserForTip = null;
		final Point p = e.getPoint();

		// try {
		final int pos = this.textArea.viewToModel(p);
		/*
		 * Highlighter.Highlight[] highlights = textArea.getHighlighter().
		 * getHighlights(); for (int i=0; i<highlights.length; i++) {
		 * Highlighter.Highlight h = highlights[i]; //if (h instanceof
		 * ParserNoticeHighlight) { // ParserNoticeHighlight pnh =
		 * (ParserNoticeHighlight)h; int start = h.getStartOffset(); int end =
		 * h.getEndOffset(); if (start<=pos && end>=pos) { //return pnh.getMessage();
		 * return textArea.getText(start, end-start); } //} }
		 */
		if (this.noticeHighlightPairs != null)
			for (final NoticeHighlightPair pair : this.noticeHighlightPairs) {
				final ParserNotice notice = pair.notice;
				if (this.noticeContainsPosition(notice, pos) && this.noticeContainsPointInView(notice, p)) {
					tip = notice.getToolTipText();
					this.parserForTip = notice.getParser();
					if (this.parserForTip instanceof HyperlinkListener)
						listener = (HyperlinkListener) this.parserForTip;
					break;
				}
			}

		final URL imageBase = this.parserForTip == null ? null : this.parserForTip.getImageBase();
		return new ToolTipInfo(tip, listener, imageBase);

	}

	/**
	 * Called when the document is modified.
	 *
	 * @param e
	 *            The document event.
	 */
	public void handleDocumentEvent(final DocumentEvent e) {
		if (this.running && this.parsers.size() > 0)
			this.timer.restart();
	}

	/**
	 * Called when the user clicks a hyperlink in a
	 * {@link org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip}.
	 *
	 * @param e
	 *            The event.
	 */
	@Override
	public void hyperlinkUpdate(final HyperlinkEvent e) {
		if (this.parserForTip != null && this.parserForTip.getHyperlinkListener() != null)
			this.parserForTip.getHyperlinkListener().linkClicked(this.textArea, e);
	}

	/**
	 * Called when the document is modified.
	 *
	 * @param e
	 *            The document event.
	 */
	@Override
	public void insertUpdate(final DocumentEvent e) {

		// Keep track of the first and last offset modified. Some parsers are
		// smart and will only re-parse this section of the file.
		try {
			int offs = e.getOffset();
			if (this.firstOffsetModded == null || offs < this.firstOffsetModded.getOffset())
				this.firstOffsetModded = e.getDocument().createPosition(offs);
			offs = e.getOffset() + e.getLength();
			if (this.lastOffsetModded == null || offs > this.lastOffsetModded.getOffset())
				this.lastOffsetModded = e.getDocument().createPosition(offs);
		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Shouldn't happen
		}

		this.handleDocumentEvent(e);

	}

	/**
	 * Since <code>viewToModel()</code> returns the <em>closest</em> model position,
	 * and the position doesn't <em>necessarily</em> contain the point passed in as
	 * an argument, this method checks whether the point is indeed contained in the
	 * view rectangle for the specified offset.
	 *
	 * @param notice
	 *            The parser notice.
	 * @param p
	 *            The point possibly contained in the view range of the parser
	 *            notice.
	 * @return Whether the parser notice actually contains the specified point in
	 *         the view.
	 */
	private boolean noticeContainsPointInView(final ParserNotice notice, final Point p) {

		try {

			int start, end;
			if (notice.getKnowsOffsetAndLength()) {
				start = notice.getOffset();
				end = start + notice.getLength() - 1;
			} else {
				final Document doc = this.textArea.getDocument();
				final Element root = doc.getDefaultRootElement();
				final int line = notice.getLine();
				// Defend against possible bad user-defined notices.
				if (line < 0)
					return false;
				final Element elem = root.getElement(line);
				start = elem.getStartOffset();
				end = elem.getEndOffset() - 1;
			}

			final Rectangle r1 = this.textArea.modelToView(start);
			final Rectangle r2 = this.textArea.modelToView(end);
			if (r1.y != r2.y)
				// If the notice spans multiple lines, give them the benefit
				// of the doubt. This is only "wrong" if the user is in empty
				// space "to the right" of the error marker when it ends at the
				// end of a line anyway.
				return true;

			r1.y--; // Be a tiny bit lenient.
			r1.height += 2; // Ditto
			return p.x >= r1.x && p.x < r2.x + r2.width && p.y >= r1.y && p.y < r1.y + r1.height;

		} catch (final BadLocationException ble) { // Never occurs
			// Give them the benefit of the doubt, should 99% of the time be
			// true anyway
			return true;
		}

	}

	/**
	 * Returns whether a parser notice contains the specified offset.
	 *
	 * @param notice
	 *            The notice.
	 * @param offs
	 *            The offset.
	 * @return Whether the notice contains the offset.
	 */
	private boolean noticeContainsPosition(final ParserNotice notice, final int offs) {
		if (notice.getKnowsOffsetAndLength())
			return notice.containsPosition(offs);
		final Document doc = this.textArea.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int line = notice.getLine();
		if (line < 0)
			return false;
		final Element elem = root.getElement(line);
		return elem != null && offs >= elem.getStartOffset() && offs < elem.getEndOffset();
	}

	/**
	 * Called when a property we're interested in changes.
	 *
	 * @param e
	 *            The property change event.
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent e) {

		final String name = e.getPropertyName();

		if ("document".equals(name)) {
			// The document switched out from under us
			final RDocument old = (RDocument) e.getOldValue();
			if (old != null)
				old.removeDocumentListener(this);
			final RDocument newDoc = (RDocument) e.getNewValue();
			if (newDoc != null)
				newDoc.addDocumentListener(this);
		}

	}

	/**
	 * Removes a parser.
	 *
	 * @param parser
	 *            The parser to remove.
	 * @return Whether the parser was found.
	 * @see #addParser(Parser)
	 * @see #getParser(int)
	 */
	public boolean removeParser(final Parser parser) {
		this.removeParserNotices(parser);
		final boolean removed = this.parsers.remove(parser);
		if (removed)
			this.textArea.fireParserNoticesChange();
		return removed;
	}

	/**
	 * Removes all parser notices (and clears highlights in the editor) from a
	 * particular parser.
	 *
	 * @param parser
	 *            The parser.
	 */
	private void removeParserNotices(final Parser parser) {
		if (this.noticeHighlightPairs != null) {
			final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
			for (final Iterator<NoticeHighlightPair> i = this.noticeHighlightPairs.iterator(); i.hasNext();) {
				final NoticeHighlightPair pair = i.next();
				if (pair.notice.getParser() == parser && pair.highlight != null) {
					h.removeParserHighlight(pair.highlight);
					i.remove();
				}
			}
		}
	}

	/**
	 * Removes any currently stored notices (and the corresponding highlights from
	 * the editor) from the same Parser, and in the given line range, as in the
	 * results.
	 *
	 * @param res
	 *            The results.
	 */
	private void removeParserNotices(final ParseResult res) {
		if (this.noticeHighlightPairs != null) {
			final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
			for (final Iterator<NoticeHighlightPair> i = this.noticeHighlightPairs.iterator(); i.hasNext();) {
				final NoticeHighlightPair pair = i.next();
				boolean removed = false;
				if (this.shouldRemoveNotice(pair.notice, res)) {
					if (pair.highlight != null)
						h.removeParserHighlight(pair.highlight);
					i.remove();
					removed = true;
				}
				if (ParserManager.DEBUG_PARSING) {
					final String text = removed ? "[DEBUG]: ... notice removed: " : "[DEBUG]: ... notice not removed: ";
					System.out.println(text + pair.notice);
				}
			}

		}

	}

	/**
	 * Called when the document is modified.
	 *
	 * @param e
	 *            The document event.
	 */
	@Override
	public void removeUpdate(final DocumentEvent e) {

		// Keep track of the first and last offset modified. Some parsers are
		// smart and will only re-parse this section of the file. Note that
		// for removals, only the line at the removal start needs to be
		// re-parsed.
		try {
			final int offs = e.getOffset();
			if (this.firstOffsetModded == null || offs < this.firstOffsetModded.getOffset())
				this.firstOffsetModded = e.getDocument().createPosition(offs);
			if (this.lastOffsetModded == null || offs > this.lastOffsetModded.getOffset())
				this.lastOffsetModded = e.getDocument().createPosition(offs);
		} catch (final BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}

		this.handleDocumentEvent(e);

	}

	/**
	 * Restarts parsing the document.
	 *
	 * @see #stopParsing()
	 */
	public void restartParsing() {
		this.timer.restart();
		this.running = true;
	}

	/**
	 * Sets the delay between the last "concurrent" edit and when the document is
	 * re-parsed.
	 *
	 * @param millis
	 *            The new delay, in milliseconds. This must be greater than
	 *            <code>0</code>.
	 * @see #getDelay()
	 */
	public void setDelay(final int millis) {
		if (this.running)
			this.timer.stop();
		this.timer.setInitialDelay(millis);
		this.timer.setDelay(millis);
		if (this.running)
			this.timer.start();
	}

	/**
	 * Returns whether a parser notice should be removed, based on a parse result.
	 *
	 * @param notice
	 *            The notice in question.
	 * @param res
	 *            The result.
	 * @return Whether the notice should be removed.
	 */
	private boolean shouldRemoveNotice(final ParserNotice notice, final ParseResult res) {

		if (ParserManager.DEBUG_PARSING)
			System.out.println(
					"[DEBUG]: ... ... shouldRemoveNotice " + notice + ": " + (notice.getParser() == res.getParser()));

		// NOTE: We must currently remove all notices for the parser. Parser
		// implementors are required to parse the entire document each parsing
		// request, as RSTA is not yet sophisticated enough to determine the
		// minimum range of text to parse (and ParserNotices' locations aren't
		// updated when the Document is mutated, which would be a requirement
		// for this as well).
		// return same_parser && (in_reparsed_range || in_deleted_end_of_doc)
		return notice.getParser() == res.getParser();

	}

	/**
	 * Stops parsing the document.
	 *
	 * @see #restartParsing()
	 */
	public void stopParsing() {
		this.timer.stop();
		this.running = false;
	}

}