/*
 * 04/23/2009
 *
 * RSyntaxTextAreaHighlighter.java - Highlighter for RSyntaxTextAreas.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.RTextAreaHighlighter;
import org.fife.ui.rtextarea.SmartHighlightPainter;

/**
 * The highlighter implementation used by {@link RSyntaxTextArea}s. It knows to
 * always paint "marked occurrences" highlights below selection highlights, and
 * squiggle underline highlights above all other highlights.
 * <p>
 *
 * Most of this code is copied from javax.swing.text.DefaultHighlighter;
 * unfortunately, we cannot re-use much of it since it is package private.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RSyntaxTextAreaHighlighter extends RTextAreaHighlighter {

	/**
	 * Highlight info implementation used for parser notices and marked occurrences.
	 */
	private static class SyntaxLayeredHighlightInfoImpl extends LayeredHighlightInfoImpl {

		private ParserNotice notice;// Color color; // Used only by Parser highlights.

		@Override
		public Color getColor() {
			// return color;
			Color color = null;
			if (this.notice != null) {
				color = this.notice.getColor();
				if (color == null)
					color = RSyntaxTextAreaHighlighter.DEFAULT_PARSER_NOTICE_COLOR;
			}
			return color;
		}

		@Override
		public String toString() {
			return "[SyntaxLayeredHighlightInfoImpl: " + "startOffs=" + this.getStartOffset() + ", endOffs="
					+ this.getEndOffset() + ", color=" + this.getColor() + "]";
		}

	}

	/**
	 * The default color used for parser notices when none is specified.
	 */
	private static final Color DEFAULT_PARSER_NOTICE_COLOR = Color.RED;

	/**
	 * Marked occurrences in the document (to be painted separately from other
	 * highlights).
	 */
	private final List<SyntaxLayeredHighlightInfoImpl> markedOccurrences;

	/**
	 * Highlights from document parsers. These should be painted "on top of" all
	 * other highlights to ensure they are always above the selection.
	 */
	private final List<SyntaxLayeredHighlightInfoImpl> parserHighlights;

	/**
	 * Constructor.
	 */
	public RSyntaxTextAreaHighlighter() {
		this.markedOccurrences = new ArrayList<>();
		this.parserHighlights = new ArrayList<>(0); // Often unused
	}

	/**
	 * Adds a special "marked occurrence" highlight.
	 *
	 * @param start
	 * @param end
	 * @param p
	 * @return A tag to reference the highlight later.
	 * @throws BadLocationException
	 * @see #clearMarkOccurrencesHighlights()
	 */
	Object addMarkedOccurrenceHighlight(final int start, final int end, final SmartHighlightPainter p)
			throws BadLocationException {
		final Document doc = this.textArea.getDocument();
		final TextUI mapper = this.textArea.getUI();
		// Always layered highlights for marked occurrences.
		final SyntaxLayeredHighlightInfoImpl i = new SyntaxLayeredHighlightInfoImpl();
		i.setPainter(p);
		i.setStartOffset(doc.createPosition(start));
		// HACK: Use "end-1" to prevent chars the user types at the "end" of
		// the highlight to be absorbed into the highlight (default Highlight
		// behavior).
		i.setEndOffset(doc.createPosition(end - 1));
		this.markedOccurrences.add(i);
		mapper.damageRange(this.textArea, start, end);
		return i;
	}

	/**
	 * Adds a highlight from a parser.
	 *
	 * @param notice
	 *            The notice from a {@link Parser}.
	 * @return A tag with which to reference the highlight.
	 * @throws BadLocationException
	 * @see #clearParserHighlights()
	 * @see #clearParserHighlights(Parser)
	 */
	HighlightInfo addParserHighlight(final ParserNotice notice, final HighlightPainter p) throws BadLocationException {

		final Document doc = this.textArea.getDocument();
		final TextUI mapper = this.textArea.getUI();

		int start = notice.getOffset();
		int end = 0;
		if (start == -1) { // Could just define an invalid line number
			final int line = notice.getLine();
			final Element root = doc.getDefaultRootElement();
			if (line >= 0 && line < root.getElementCount()) {
				final Element elem = root.getElement(line);
				start = elem.getStartOffset();
				end = elem.getEndOffset();
			}
		} else
			end = start + notice.getLength();

		// Always layered highlights for parser highlights.
		final SyntaxLayeredHighlightInfoImpl i = new SyntaxLayeredHighlightInfoImpl();
		i.setPainter(p);
		i.setStartOffset(doc.createPosition(start));
		// HACK: Use "end-1" to prevent chars the user types at the "end" of
		// the highlight to be absorbed into the highlight (default Highlight
		// behavior).
		i.setEndOffset(doc.createPosition(end - 1));
		i.notice = notice;// i.color = notice.getColor();

		this.parserHighlights.add(i);
		mapper.damageRange(this.textArea, start, end);
		return i;

	}

	/**
	 * Removes all "marked occurrences" highlights from the view.
	 *
	 * @see #addMarkedOccurrenceHighlight(int, int, SmartHighlightPainter)
	 */
	void clearMarkOccurrencesHighlights() {
		// Don't remove via an iterator; since our List is an ArrayList, this
		// implies tons of System.arrayCopy()s
		for (final HighlightInfo info : this.markedOccurrences)
			this.repaintListHighlight(info);
		this.markedOccurrences.clear();
	}

	/**
	 * Removes all parser highlights.
	 *
	 * @see #addParserHighlight(ParserNotice,
	 *      javax.swing.text.Highlighter.HighlightPainter)
	 */
	void clearParserHighlights() {
		// Don't remove via an iterator; since our List is an ArrayList, this
		// implies tons of System.arrayCopy()s
		for (int i = 0; i < this.parserHighlights.size(); i++)
			this.repaintListHighlight(this.parserHighlights.get(i));
		this.parserHighlights.clear();
	}

	/**
	 * Removes all of the highlights for a specific parser.
	 *
	 * @param parser
	 *            The parser.
	 */
	public void clearParserHighlights(final Parser parser) {

		final Iterator<SyntaxLayeredHighlightInfoImpl> i = this.parserHighlights.iterator();
		while (i.hasNext()) {

			final SyntaxLayeredHighlightInfoImpl info = i.next();

			if (info.notice.getParser() == parser) {
				if (info.width > 0 && info.height > 0)
					this.textArea.repaint(info.x, info.y, info.width, info.height);
				i.remove();
			}

		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deinstall(final JTextComponent c) {
		super.deinstall(c);
		this.markedOccurrences.clear();
		this.parserHighlights.clear();
	}

	/**
	 * Returns a list of "marked occurrences" in the text area. If there are no
	 * marked occurrences, this will be an empty list.
	 *
	 * @return The list of marked occurrences, or an empty list if none. The
	 *         contents of this list will be of type {@link DocumentRange}.
	 */
	public List<DocumentRange> getMarkedOccurrences() {
		final List<DocumentRange> list = new ArrayList<>(this.markedOccurrences.size());
		for (final HighlightInfo info : this.markedOccurrences) {
			final int start = info.getStartOffset();
			final int end = info.getEndOffset() + 1; // HACK
			if (start <= end) {
				// Occasionally a Marked Occurrence can have a lost end offset
				// but not start offset (replacing entire text content with
				// new content, and a marked occurrence is on the last token
				// in the document).
				final DocumentRange range = new DocumentRange(start, end);
				list.add(range);
			}
		}
		return list;
	}

	@Override
	public void paintLayeredHighlights(final Graphics g, final int lineStart, final int lineEnd, final Shape viewBounds,
			final JTextComponent editor, final View view) {
		this.paintListLayered(g, lineStart, lineEnd, viewBounds, editor, view, this.markedOccurrences);
		super.paintLayeredHighlights(g, lineStart, lineEnd, viewBounds, editor, view);
		this.paintListLayered(g, lineStart, lineEnd, viewBounds, editor, view, this.parserHighlights);
	}

	/**
	 * Removes a parser highlight from this view.
	 *
	 * @param tag
	 *            The reference to the highlight.
	 * @see #addParserHighlight(ParserNotice,
	 *      javax.swing.text.Highlighter.HighlightPainter)
	 */
	void removeParserHighlight(final HighlightInfo tag) {
		this.repaintListHighlight(tag);
		this.parserHighlights.remove(tag);
	}

}