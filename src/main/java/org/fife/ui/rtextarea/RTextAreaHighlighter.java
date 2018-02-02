/*
 * 10/13/2013
 *
 * RTextAreaHighlighter.java - Highlighter for RTextAreas.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import javax.swing.plaf.TextUI;
import javax.swing.plaf.basic.BasicTextUI.BasicHighlighter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.fife.ui.rsyntaxtextarea.DocumentRange;

/**
 * The highlighter implementation used by {@link RTextArea}s. It knows to always
 * paint "mark all" highlights below selection highlights.
 * <p>
 *
 * Most of this code is copied from javax.swing.text.DefaultHighlighter;
 * unfortunately, we cannot re-use much of it since it is package private.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RTextAreaHighlighter extends BasicHighlighter {

	/**
	 * Information about a highlight being painted by this highlighter.
	 */
	public interface HighlightInfo extends Highlighter.Highlight {
	}

	/**
	 * A straightforward implementation of <code>HighlightInfo</code>.
	 */
	protected static class HighlightInfoImpl implements HighlightInfo {

		private Position p0;
		private Position p1;
		private Highlighter.HighlightPainter painter;

		/** To be extended by subclasses. */
		public Color getColor() {
			return null;
		}

		@Override
		public int getEndOffset() {
			return this.p1.getOffset();
		}

		@Override
		public Highlighter.HighlightPainter getPainter() {
			return this.painter;
		}

		@Override
		public int getStartOffset() {
			return this.p0.getOffset();
		}

		public void setEndOffset(final Position endOffset) {
			this.p1 = endOffset;
		}

		public void setPainter(final Highlighter.HighlightPainter painter) {
			this.painter = painter;
		}

		public void setStartOffset(final Position startOffset) {
			this.p0 = startOffset;
		}

	}

	/**
	 * Information about a layered highlight being painted by this highlighter.
	 */
	public interface LayeredHighlightInfo extends HighlightInfo {

		/**
		 * Restricts the region based on the receivers offsets and messages the painter
		 * to paint the region.
		 */
		void paintLayeredHighlights(Graphics g, int p0, int p1, Shape viewBounds, JTextComponent editor, View view);

	}

	/**
	 * A straightforward implementation of <code>HighlightInfo</code> for painting
	 * layered highlights.
	 */
	protected static class LayeredHighlightInfoImpl extends HighlightInfoImpl implements LayeredHighlightInfo {

		/*
		 * NOTE: This implementation is a "hack" so typing at the "end" of the highlight
		 * does not extend it to include the newly-typed chars, which is the standard
		 * behavior of Swing Highlights. It assumes that the "p1" Position set is
		 * actually 1 char too short, and will render the selection as if that "extra"
		 * char should be highlighted.
		 */

		public int height;
		public int width;
		public int x;
		public int y;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void paintLayeredHighlights(final Graphics g, int p0, int p1, final Shape viewBounds,
				final JTextComponent editor, final View view) {
			final int start = this.getStartOffset();
			int end = this.getEndOffset();
			end++; // Workaround for Java highlight issues
			// Restrict the region to what we represent
			p0 = Math.max(start, p0);
			p1 = Math.min(end, p1);
			if (this.getColor() != null && this.getPainter() instanceof ChangeableHighlightPainter)
				((ChangeableHighlightPainter) this.getPainter()).setPaint(this.getColor());
			// Paint the appropriate region using the painter and union
			// the effected region with our bounds.
			this.union(((LayeredHighlighter.LayerPainter) this.getPainter()).paintLayer(g, p0, p1, viewBounds, editor,
					view));
		}

		void union(final Shape bounds) {
			if (bounds == null)
				return;
			final Rectangle alloc = bounds instanceof Rectangle ? (Rectangle) bounds : bounds.getBounds();
			if (this.width == 0 || this.height == 0) {
				this.x = alloc.x;
				this.y = alloc.y;
				this.width = alloc.width;
				this.height = alloc.height;
			} else {
				this.width = Math.max(this.x + this.width, alloc.x + alloc.width);
				this.height = Math.max(this.y + this.height, alloc.y + alloc.height);
				this.x = Math.min(this.x, alloc.x);
				this.width -= this.x;
				this.y = Math.min(this.y, alloc.y);
				this.height -= this.y;
			}
		}

	}

	/**
	 * The "mark all" highlights (to be painted separately from other highlights).
	 */
	private final List<HighlightInfo> markAllHighlights;

	/**
	 * The text component we are the highlighter for.
	 */
	protected RTextArea textArea;

	/**
	 * Constructor.
	 */
	public RTextAreaHighlighter() {
		this.markAllHighlights = new ArrayList<>();
	}

	/**
	 * Adds a special "marked occurrence" highlight.
	 *
	 * @param start
	 * @param end
	 * @param p
	 * @return A tag to reference the highlight later.
	 * @throws BadLocationException
	 * @see #clearMarkAllHighlights()
	 */
	Object addMarkAllHighlight(final int start, final int end, final HighlightPainter p) throws BadLocationException {
		final Document doc = this.textArea.getDocument();
		final TextUI mapper = this.textArea.getUI();
		// Always layered highlights for marked occurrences.
		final HighlightInfoImpl i = new LayeredHighlightInfoImpl();
		i.setPainter(p);
		i.p0 = doc.createPosition(start);
		// HACK: Use "end-1" to prevent chars the user types at the "end" of
		// the highlight to be absorbed into the highlight (default Highlight
		// behavior).
		i.p1 = doc.createPosition(end - 1);
		this.markAllHighlights.add(i);
		mapper.damageRange(this.textArea, start, end);
		return i;
	}

	/**
	 * Removes all "mark all" highlights from the view.
	 *
	 * @see #addMarkAllHighlight(int, int,
	 *      javax.swing.text.Highlighter.HighlightPainter)
	 */
	void clearMarkAllHighlights() {
		// Don't remove via an iterator; since our List is an ArrayList, this
		// implies tons of System.arrayCopy()s
		for (final HighlightInfo info : this.markAllHighlights)
			this.repaintListHighlight(info);
		this.markAllHighlights.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deinstall(final JTextComponent c) {
		this.textArea = null;
		this.markAllHighlights.clear();
	}

	/**
	 * Returns the number of "mark all" highlights currently shown in the editor.
	 *
	 * @return The "mark all" highlight count.
	 */
	public int getMarkAllHighlightCount() {
		return this.markAllHighlights.size();
	}

	/**
	 * Returns a list of "mark all" highlights in the text area. If there are no
	 * such highlights, this will be an empty list.
	 *
	 * @return The list of "mark all" highlight ranges.
	 */
	public List<DocumentRange> getMarkAllHighlightRanges() {
		final List<DocumentRange> list = new ArrayList<>(this.markAllHighlights.size());
		for (final HighlightInfo info : this.markAllHighlights) {
			final int start = info.getStartOffset();
			final int end = info.getEndOffset() + 1; // HACK
			final DocumentRange range = new DocumentRange(start, end);
			list.add(range);
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void install(final JTextComponent c) {
		super.install(c);
		this.textArea = (RTextArea) c;
	}

	/**
	 * When leaf Views (such as LabelView) are rendering they should call into this
	 * method. If a highlight is in the given region it will be drawn immediately.
	 *
	 * @param g
	 *            Graphics used to draw
	 * @param lineStart
	 *            starting offset of view
	 * @param lineEnd
	 *            ending offset of view
	 * @param viewBounds
	 *            Bounds of View
	 * @param editor
	 *            JTextComponent
	 * @param view
	 *            View instance being rendered
	 */
	@Override
	public void paintLayeredHighlights(final Graphics g, final int lineStart, final int lineEnd, final Shape viewBounds,
			final JTextComponent editor, final View view) {
		this.paintListLayered(g, lineStart, lineEnd, viewBounds, editor, view, this.markAllHighlights);
		super.paintLayeredHighlights(g, lineStart, lineEnd, viewBounds, editor, view);
	}

	protected void paintListLayered(final Graphics g, final int lineStart, final int lineEnd, final Shape viewBounds,
			final JTextComponent editor, final View view, final List<? extends HighlightInfo> highlights) {
		for (int i = highlights.size() - 1; i >= 0; i--) {
			final HighlightInfo tag = highlights.get(i);
			if (tag instanceof LayeredHighlightInfo) {
				final LayeredHighlightInfo lhi = (LayeredHighlightInfo) tag;
				final int highlightStart = lhi.getStartOffset();
				final int highlightEnd = lhi.getEndOffset() + 1; // "+1" workaround for Java highlight issues
				if (lineStart < highlightStart && lineEnd > highlightStart
						|| lineStart >= highlightStart && lineStart < highlightEnd)
					lhi.paintLayeredHighlights(g, lineStart, lineEnd, viewBounds, editor, view);
			}
		}
	}

	protected void repaintListHighlight(final HighlightInfo info) {
		// Note: We're relying on implementation here, not interface. Yuck...
		if (info instanceof LayeredHighlightInfoImpl) {
			final LayeredHighlightInfoImpl lhi = (LayeredHighlightInfoImpl) info;
			if (lhi.width > 0 && lhi.height > 0)
				this.textArea.repaint(lhi.x, lhi.y, lhi.width, lhi.height);
		} else {
			final TextUI ui = this.textArea.getUI();
			ui.damageRange(this.textArea, info.getStartOffset(), info.getEndOffset());
			// safeDamageRange(info.p0, info.p1);
		}
	}

}