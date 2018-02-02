/*
 * 02/10/2009
 *
 * LineHighlightManager - Manages line highlights.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

/**
 * Manages line highlights in an <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class LineHighlightManager {

	/**
	 * Information about a line highlight.
	 */
	private static class LineHighlightInfo {

		private final Color color;
		private final Position offs;

		LineHighlightInfo(final Position offs, final Color c) {
			this.offs = offs;
			this.color = c;
		}

		public Color getColor() {
			return this.color;
		}

		public int getOffset() {
			return this.offs.getOffset();
		}

		@Override
		public int hashCode() {
			return this.getOffset();
		}

	}

	/**
	 * Comparator used when adding new highlights. This is done here instead of
	 * making <code>LineHighlightInfo</code> implement <code>Comparable</code> as
	 * correctly implementing the latter prevents two LHI's pointing to the same
	 * line from correctly being distinguished from one another. See:
	 * https://github.com/bobbylight/RSyntaxTextArea/issues/161
	 */
	private static class LineHighlightInfoComparator implements Comparator<LineHighlightInfo> {

		@Override
		public int compare(final LineHighlightInfo lhi1, final LineHighlightInfo lhi2) {
			if (lhi1.getOffset() < lhi2.getOffset())
				return -1;
			return lhi1.getOffset() == lhi2.getOffset() ? 0 : 1;
		}

	}

	private final LineHighlightInfoComparator comparator;

	private List<LineHighlightInfo> lineHighlights;

	private final RTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The parent text area.
	 */
	LineHighlightManager(final RTextArea textArea) {
		this.textArea = textArea;
		this.comparator = new LineHighlightInfoComparator();
	}

	/**
	 * Highlights the specified line.
	 *
	 * @param line
	 *            The line to highlight.
	 * @param color
	 *            The color to highlight with.
	 * @return A tag for the highlight.
	 * @throws BadLocationException
	 *             If <code>line</code> is not a valid line number.
	 * @see #removeLineHighlight(Object)
	 */
	public Object addLineHighlight(final int line, final Color color) throws BadLocationException {
		final int offs = this.textArea.getLineStartOffset(line);
		final LineHighlightInfo lhi = new LineHighlightInfo(this.textArea.getDocument().createPosition(offs), color);
		if (this.lineHighlights == null)
			this.lineHighlights = new ArrayList<>(1);
		int index = Collections.binarySearch(this.lineHighlights, lhi, this.comparator);
		if (index < 0)
			index = -(index + 1);
		this.lineHighlights.add(index, lhi);
		this.repaintLine(lhi);
		return lhi;
	}

	/**
	 * Returns the current line highlights' tags.
	 *
	 * @return The current line highlights' tags, or an empty list if there are
	 *         none.
	 */
	protected List<Object> getCurrentLineHighlightTags() {
		return this.lineHighlights == null ? Collections.emptyList() : new ArrayList<>(this.lineHighlights);
	}

	/**
	 * Returns the current number of line highlights. Useful for testing.
	 *
	 * @return The current number of line highlights.
	 */
	protected int getLineHighlightCount() {
		return this.lineHighlights == null ? 0 : this.lineHighlights.size();
	}

	/**
	 * Paints any highlighted lines in the specified line range.
	 *
	 * @param g
	 *            The graphics context.
	 */
	public void paintLineHighlights(final Graphics g) {

		final int count = this.lineHighlights == null ? 0 : this.lineHighlights.size();
		if (count > 0) {

			final int docLen = this.textArea.getDocument().getLength();
			final Rectangle vr = this.textArea.getVisibleRect();
			final int lineHeight = this.textArea.getLineHeight();

			try {

				for (int i = 0; i < count; i++) {
					final LineHighlightInfo lhi = this.lineHighlights.get(i);
					final int offs = lhi.getOffset();
					if (offs >= 0 && offs <= docLen) {
						final int y = this.textArea.yForLineContaining(offs);
						if (y > vr.y - lineHeight)
							if (y < vr.y + vr.height) {
								g.setColor(lhi.getColor());
								g.fillRect(0, y, this.textArea.getWidth(), lineHeight);
							} else
								break; // Out of visible rect
					}
				}

			} catch (final BadLocationException ble) { // Never happens
				ble.printStackTrace();
			}
		}

	}

	/**
	 * Removes all line highlights.
	 *
	 * @see #removeLineHighlight(Object)
	 */
	public void removeAllLineHighlights() {
		if (this.lineHighlights != null) {
			this.lineHighlights.clear();
			this.textArea.repaint();
		}
	}

	/**
	 * Removes a line highlight.
	 *
	 * @param tag
	 *            The tag of the line highlight to remove.
	 * @see #addLineHighlight(int, Color)
	 */
	public void removeLineHighlight(final Object tag) {
		if (tag instanceof LineHighlightInfo) {
			this.lineHighlights.remove(tag);
			this.repaintLine((LineHighlightInfo) tag);
		}
	}

	/**
	 * Repaints the line pointed to by the specified highlight information.
	 *
	 * @param lhi
	 *            The highlight information.
	 */
	private void repaintLine(final LineHighlightInfo lhi) {
		final int offs = lhi.getOffset();
		// May be > length if they deleted text including the highlight
		if (offs >= 0 && offs <= this.textArea.getDocument().getLength())
			try {
				final int y = this.textArea.yForLineContaining(offs);
				if (y > -1)
					this.textArea.repaint(0, y, this.textArea.getWidth(), this.textArea.getLineHeight());
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
	}

}