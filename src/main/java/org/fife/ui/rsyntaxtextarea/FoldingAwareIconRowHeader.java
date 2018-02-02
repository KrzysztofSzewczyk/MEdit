/*
 * 03/07/2012
 *
 * FoldingAwareIconRowHeader - Icon row header that paints itself correctly
 * even when code folding is enabled.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.IconRowHeader;

/**
 * A row header component that takes code folding into account when painting
 * itself.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FoldingAwareIconRowHeader extends IconRowHeader {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The parent text area.
	 */
	public FoldingAwareIconRowHeader(final RSyntaxTextArea textArea) {
		super(textArea);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		// When line wrap is not enabled, take the faster code path.
		if (this.textArea == null)
			return;
		final RSyntaxTextArea rsta = (RSyntaxTextArea) this.textArea;
		final FoldManager fm = rsta.getFoldManager();
		if (!fm.isCodeFoldingSupportedAndEnabled()) {
			super.paintComponent(g);
			return;
		}

		this.visibleRect = g.getClipBounds(this.visibleRect);
		if (this.visibleRect == null)
			this.visibleRect = this.getVisibleRect();
		// System.out.println("IconRowHeader repainting: " + visibleRect);
		if (this.visibleRect == null)
			return;
		this.paintBackgroundImpl(g, this.visibleRect);

		if (this.textArea.getLineWrap()) {
			this.paintComponentWrapped(g);
			return;
		}

		final Document doc = this.textArea.getDocument();
		final Element root = doc.getDefaultRootElement();
		this.textAreaInsets = this.textArea.getInsets(this.textAreaInsets);
		if (this.visibleRect.y < this.textAreaInsets.top) {
			this.visibleRect.height -= this.textAreaInsets.top - this.visibleRect.y;
			this.visibleRect.y = this.textAreaInsets.top;
		}

		// Get the first line to paint.
		final int cellHeight = this.textArea.getLineHeight();
		int topLine = (this.visibleRect.y - this.textAreaInsets.top) / cellHeight;

		// Get where to start painting (top of the row).
		// We need to be "scrolled up" up just enough for the missing part of
		// the first line.
		final int y = topLine * cellHeight + this.textAreaInsets.top;

		// AFTER calculating visual offset to paint at, account for folding.
		topLine += fm.getHiddenLineCountAbove(topLine, true);

		// Paint the active line range.
		if (this.activeLineRangeStart > -1 && this.activeLineRangeEnd > -1) {
			final Color activeLineRangeColor = this.getActiveLineRangeColor();
			g.setColor(activeLineRangeColor);
			try {

				final int realY1 = rsta.yForLine(this.activeLineRangeStart);
				if (realY1 > -1) { // Not in a collapsed fold...

					int y1 = realY1;// Math.max(y, realY1);

					int y2 = rsta.yForLine(this.activeLineRangeEnd);
					if (y2 == -1)
						y2 = y1;
					y2 += cellHeight - 1;

					if (y2 < this.visibleRect.y || y1 > this.visibleRect.y + this.visibleRect.height)
						// System.out.println("... nothing to paint, bailing...");
						return;
					y1 = Math.max(y, realY1);
					y2 = Math.min(y2, this.visibleRect.y + this.visibleRect.height);
					// System.out.println(y1 + "... " + y2 + "; " + realY1 + ", " + visibleRect);

					int j = y1;
					while (j <= y2) {
						final int yEnd = Math.min(y2, j + this.getWidth());
						final int xEnd = yEnd - j;
						g.drawLine(0, j, xEnd, yEnd);
						j += 2;
					}

					int i = 2;
					while (i < this.getWidth()) {
						final int yEnd = y1 + this.getWidth() - i;
						g.drawLine(i, y1, this.getWidth(), yEnd);
						i += 2;
					}

					if (realY1 >= y && realY1 < this.visibleRect.y + this.visibleRect.height)
						g.drawLine(0, realY1, this.getWidth(), realY1);
					if (y2 >= y && y2 < this.visibleRect.y + this.visibleRect.height)
						g.drawLine(0, y2, this.getWidth(), y2);

				}

			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}

		// Paint icons
		if (this.trackingIcons != null) {
			int lastLine = this.textArea.getLineCount() - 1;
			for (int i = this.trackingIcons.size() - 1; i >= 0; i--) { // Last to first
				final GutterIconInfo ti = this.getTrackingIcon(i);
				final int offs = ti.getMarkedOffset();
				if (offs >= 0 && offs <= doc.getLength()) {
					final int line = root.getElementIndex(offs);
					if (line <= lastLine && line >= topLine)
						try {
							final Icon icon = ti.getIcon();
							if (icon != null) {
								final int lineY = rsta.yForLine(line);
								if (lineY >= y && lineY <= this.visibleRect.y + this.visibleRect.height) {
									final int y2 = lineY + (cellHeight - icon.getIconHeight()) / 2;
									icon.paintIcon(this, g, 0, y2);
									lastLine = line - 1; // Paint only 1 icon per line
								}
							}
						} catch (final BadLocationException ble) {
							ble.printStackTrace(); // Never happens
						}
					else if (line < topLine)
						break; // All other lines are above us, so quit now
				}
			}
		}

	}

	/**
	 * Paints icons when line wrapping is enabled. Note that this does not override
	 * the parent class's implementation to avoid this version being called when
	 * line wrapping is disabled.
	 */
	private void paintComponentWrapped(final Graphics g) {

		// The variables we use are as follows:
		// - visibleRect is the "visible" area of the text area; e.g.
		// [0,100, 300,100+(lineCount*cellHeight)-1].
		// actualTop.y is the topmost-pixel in the first logical line we
		// paint. Note that we may well not paint this part of the logical
		// line, as it may be broken into many physical lines, with the first
		// few physical lines scrolled past. Note also that this is NOT the
		// visible rect of this line number list; this line number list has
		// visible rect == [0,0, insets.left-1,visibleRect.height-1].

		// We avoid using modelToView/viewToModel where possible, as these
		// methods trigger a parsing of the line into syntax tokens, which is
		// costly. It's cheaper to just grab the child views' bounds.

		final RSyntaxTextArea rsta = (RSyntaxTextArea) this.textArea;
		// boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		final Document doc = this.textArea.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int topPosition = this.textArea.viewToModel(new Point(this.visibleRect.x, this.visibleRect.y));
		final int topLine = root.getElementIndex(topPosition);

		final int topY = this.visibleRect.y;
		final int bottomY = this.visibleRect.y + this.visibleRect.height;
		final int cellHeight = this.textArea.getLineHeight();

		// Paint icons
		if (this.trackingIcons != null) {
			int lastLine = this.textArea.getLineCount() - 1;
			for (int i = this.trackingIcons.size() - 1; i >= 0; i--) { // Last to first
				final GutterIconInfo ti = this.getTrackingIcon(i);
				final Icon icon = ti.getIcon();
				if (icon != null) {
					final int iconH = icon.getIconHeight();
					final int offs = ti.getMarkedOffset();
					if (offs >= 0 && offs <= doc.getLength()) {
						final int line = root.getElementIndex(offs);
						if (line <= lastLine && line >= topLine)
							try {
								final int lineY = rsta.yForLine(line);
								if (lineY <= bottomY && lineY + iconH >= topY) {
									final int y2 = lineY + (cellHeight - iconH) / 2;
									ti.getIcon().paintIcon(this, g, 0, y2);
									lastLine = line - 1; // Paint only 1 icon per line
								}
							} catch (final BadLocationException ble) {
								ble.printStackTrace(); // Never happens
							}
						else if (line < topLine)
							break; // All other lines are above us, so quit now
					}
				}
			}
		}

	}

}