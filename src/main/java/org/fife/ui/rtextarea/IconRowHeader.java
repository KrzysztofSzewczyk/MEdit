/*
 * 02/17/2009
 *
 * IconRowHeader.java - Renders icons in the gutter.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * Renders icons in the {@link Gutter}. This can be used to visually mark lines
 * containing syntax errors, lines with breakpoints set on them, etc.
 * <p>
 *
 * This component has built-in support for displaying icons representing
 * "bookmarks;" that is, lines a user can cycle through via F2 and Shift+F2.
 * Bookmarked lines are toggled via Ctrl+F2, or by clicking in the icon area at
 * the line to bookmark. In order to enable bookmarking, you must first assign
 * an icon to represent a bookmarked line, then actually enable the feature.
 * This is actually done on the parent {@link Gutter} component:
 * <p>
 *
 * <pre>
 * Gutter gutter = scrollPane.getGutter();
 * gutter.setBookmarkIcon(new ImageIcon("bookmark.png"));
 * gutter.setBookmarkingEnabled(true);
 * </pre>
 *
 * @author Robert Futrell
 * @version 1.0
 * @see org.fife.ui.rsyntaxtextarea.FoldingAwareIconRowHeader
 */
public class IconRowHeader extends AbstractGutterComponent implements MouseListener {

	/**
	 * Implementation of the icons rendered.
	 */
	private static class GutterIconImpl implements GutterIconInfo, Comparable<GutterIconInfo> {

		private final Icon icon;
		private final Position pos;
		private final String toolTip;

		GutterIconImpl(final Icon icon, final Position pos, final String toolTip) {
			this.icon = icon;
			this.pos = pos;
			this.toolTip = toolTip;
		}

		@Override
		public int compareTo(final GutterIconInfo other) {
			if (other != null)
				return this.pos.getOffset() - other.getMarkedOffset();
			return -1;
		}

		@Override
		public boolean equals(final Object o) {
			return o == this;
		}

		@Override
		public Icon getIcon() {
			return this.icon;
		}

		@Override
		public int getMarkedOffset() {
			return this.pos.getOffset();
		}

		@Override
		public String getToolTip() {
			return this.toolTip;
		}

		@Override
		public int hashCode() {
			return this.icon.hashCode(); // FindBugs
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The color used to highlight the active code block.
	 */
	private Color activeLineRangeColor;

	/**
	 * The end line in the active line range.
	 */
	protected int activeLineRangeEnd;

	/**
	 * The first line in the active line range.
	 */
	protected int activeLineRangeStart;

	/**
	 * The icon to use for bookmarks.
	 */
	private Icon bookmarkIcon;

	/**
	 * Whether this component listens for mouse clicks and toggles "bookmark" icons
	 * on them.
	 */
	private boolean bookmarkingEnabled;

	/**
	 * Whether this component should use the gutter's background color (as opposed
	 * to using a LookAndFeel-dependent color, which is the default behavior).
	 */
	private boolean inheritsGutterBackground;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on each
	 * paint.
	 */
	protected Insets textAreaInsets;

	/**
	 * The icons to render.
	 */
	protected List<GutterIconImpl> trackingIcons;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on each
	 * paint.
	 */
	protected Rectangle visibleRect;

	/**
	 * The width of this component.
	 */
	protected int width;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The parent text area.
	 */
	public IconRowHeader(final RTextArea textArea) {
		super(textArea);
	}

	/**
	 * Adds an icon that tracks an offset in the document, and is displayed adjacent
	 * to the line numbers. This is useful for marking things such as source code
	 * errors.
	 *
	 * @param offs
	 *            The offset to track.
	 * @param icon
	 *            The icon to display. This should be small (say 16x16).
	 * @return A tag for this icon.
	 * @throws BadLocationException
	 *             If <code>offs</code> is an invalid offset into the text area.
	 * @see #removeTrackingIcon(Object)
	 */
	public GutterIconInfo addOffsetTrackingIcon(final int offs, final Icon icon) throws BadLocationException {
		return this.addOffsetTrackingIcon(offs, icon, null);
	}

	/**
	 * Adds an icon that tracks an offset in the document, and is displayed adjacent
	 * to the line numbers. This is useful for marking things such as source code
	 * errors.
	 *
	 * @param offs
	 *            The offset to track.
	 * @param icon
	 *            The icon to display. This should be small (say 16x16).
	 * @param tip
	 *            A tool tip for the icon.
	 * @return A tag for this icon.
	 * @throws BadLocationException
	 *             If <code>offs</code> is an invalid offset into the text area.
	 * @see #removeTrackingIcon(Object)
	 */
	public GutterIconInfo addOffsetTrackingIcon(final int offs, final Icon icon, final String tip)
			throws BadLocationException {
		// Despite its documentation, AbstractDocument does *not* throw BLEs
		// when creating sticky positions for offsets that do not exist.
		// We must check for that ourselves.
		if (offs < 0 || offs > this.textArea.getDocument().getLength())
			throw new BadLocationException(
					"Offset " + offs + " not in " + "required range of 0-" + this.textArea.getDocument().getLength(),
					offs);
		final Position pos = this.textArea.getDocument().createPosition(offs);
		final GutterIconImpl ti = new GutterIconImpl(icon, pos, tip);
		if (this.trackingIcons == null)
			this.trackingIcons = new ArrayList<>(1); // Usually small
		int index = Collections.binarySearch(this.trackingIcons, ti);
		if (index < 0)
			index = -(index + 1);
		this.trackingIcons.add(index, ti);
		this.repaint();
		return ti;
	}

	/**
	 * Clears the active line range.
	 *
	 * @see #setActiveLineRange(int, int)
	 */
	public void clearActiveLineRange() {
		if (this.activeLineRangeStart != -1 || this.activeLineRangeEnd != -1) {
			this.activeLineRangeStart = this.activeLineRangeEnd = -1;
			this.repaint();
		}
	}

	/**
	 * Returns the color used to paint the active line range, if any.
	 *
	 * @return The color.
	 * @see #setActiveLineRangeColor(Color)
	 */
	public Color getActiveLineRangeColor() {
		return this.activeLineRangeColor;
	}

	/**
	 * Returns the icon to use for bookmarks.
	 *
	 * @return The icon to use for bookmarks. If this is <code>null</code>,
	 *         bookmarking is effectively disabled.
	 * @see #setBookmarkIcon(Icon)
	 * @see #isBookmarkingEnabled()
	 */
	public Icon getBookmarkIcon() {
		return this.bookmarkIcon;
	}

	/**
	 * Returns the bookmarks known to this gutter.
	 *
	 * @return The bookmarks. If there are no bookmarks, an empty array is returned.
	 */
	public GutterIconInfo[] getBookmarks() {

		final List<GutterIconInfo> retVal = new ArrayList<>(1);

		if (this.trackingIcons != null)
			for (int i = 0; i < this.trackingIcons.size(); i++) {
				final GutterIconImpl ti = this.getTrackingIcon(i);
				if (ti.getIcon() == this.bookmarkIcon)
					retVal.add(ti);
			}

		final GutterIconInfo[] array = new GutterIconInfo[retVal.size()];
		return retVal.toArray(array);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dimension getPreferredSize() {
		final int h = this.textArea != null ? this.textArea.getHeight() : 100; // Arbitrary
		return new Dimension(this.width, h);
	}

	/**
	 * Overridden to display the tool tip of any icons on this line.
	 *
	 * @param e
	 *            The location the mouse is hovering over.
	 */
	@Override
	public String getToolTipText(final MouseEvent e) {
		try {
			final int line = this.viewToModelLine(e.getPoint());
			if (line > -1) {
				final GutterIconInfo[] infos = this.getTrackingIcons(line);
				if (infos.length > 0)
					// TODO: Display all messages?
					return infos[infos.length - 1].getToolTip();
			}
		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}
		return null;
	}

	protected GutterIconImpl getTrackingIcon(final int index) {
		return this.trackingIcons.get(index);
	}

	/**
	 * Returns the tracking icons at the specified line.
	 *
	 * @param line
	 *            The line.
	 * @return The tracking icons at that line. If there are no tracking icons
	 *         there, this will be an empty array.
	 * @throws BadLocationException
	 *             If <code>line</code> is invalid.
	 */
	public GutterIconInfo[] getTrackingIcons(final int line) throws BadLocationException {

		final List<GutterIconInfo> retVal = new ArrayList<>(1);

		if (this.trackingIcons != null) {
			final int start = this.textArea.getLineStartOffset(line);
			int end = this.textArea.getLineEndOffset(line);
			if (line == this.textArea.getLineCount() - 1)
				end++; // Hack
			for (int i = 0; i < this.trackingIcons.size(); i++) {
				final GutterIconImpl ti = this.getTrackingIcon(i);
				final int offs = ti.getMarkedOffset();
				if (offs >= start && offs < end)
					retVal.add(ti);
				else if (offs >= end)
					break; // Quit early
			}
		}

		final GutterIconInfo[] array = new GutterIconInfo[retVal.size()];
		return retVal.toArray(array);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void handleDocumentEvent(final DocumentEvent e) {
		final int newLineCount = this.textArea.getLineCount();
		if (newLineCount != this.currentLineCount) {
			this.currentLineCount = newLineCount;
			this.repaint();
		}
	}

	@Override
	protected void init() {

		super.init();

		this.visibleRect = new Rectangle();
		this.width = 16;
		this.addMouseListener(this);
		this.activeLineRangeStart = this.activeLineRangeEnd = -1;
		this.setActiveLineRangeColor(null);

		// Must explicitly set our background color, otherwise we inherit that
		// of the parent Gutter.
		this.updateBackground();

		ToolTipManager.sharedInstance().registerComponent(this);

	}

	/**
	 * Returns whether bookmarking is enabled.
	 *
	 * @return Whether bookmarking is enabled.
	 * @see #setBookmarkingEnabled(boolean)
	 */
	public boolean isBookmarkingEnabled() {
		return this.bookmarkingEnabled;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void lineHeightsChanged() {
		this.repaint();
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		if (this.bookmarkingEnabled && this.bookmarkIcon != null)
			try {
				final int line = this.viewToModelLine(e.getPoint());
				if (line > -1)
					this.toggleBookmark(line);
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
	}

	/**
	 * Paints the background of this component.
	 *
	 * @param g
	 *            The graphics context.
	 * @param visibleRect
	 *            The visible bounds of this component.
	 */
	protected void paintBackgroundImpl(final Graphics g, final Rectangle visibleRect) {
		Color bg = this.getBackground();
		if (this.inheritsGutterBackground && this.getGutter() != null)
			bg = this.getGutter().getBackground();
		g.setColor(bg);
		g.fillRect(0, visibleRect.y, this.width, visibleRect.height);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		if (this.textArea == null)
			return;

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

		// Get the first and last lines to paint.
		final int cellHeight = this.textArea.getLineHeight();
		final int topLine = (this.visibleRect.y - this.textAreaInsets.top) / cellHeight;
		final int bottomLine = Math.min(topLine + this.visibleRect.height / cellHeight + 1, root.getElementCount());

		// Get where to start painting (top of the row).
		// We need to be "scrolled up" up just enough for the missing part of
		// the first line.
		final int y = topLine * cellHeight + this.textAreaInsets.top;

		if (this.activeLineRangeStart >= topLine && this.activeLineRangeStart <= bottomLine
				|| this.activeLineRangeEnd >= topLine && this.activeLineRangeEnd <= bottomLine
				|| this.activeLineRangeStart <= topLine && this.activeLineRangeEnd >= bottomLine) {

			g.setColor(this.activeLineRangeColor);
			final int firstLine = Math.max(this.activeLineRangeStart, topLine);
			final int y1 = firstLine * cellHeight + this.textAreaInsets.top;
			final int lastLine = Math.min(this.activeLineRangeEnd, bottomLine);
			final int y2 = (lastLine + 1) * cellHeight + this.textAreaInsets.top - 1;

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

			if (firstLine == this.activeLineRangeStart)
				g.drawLine(0, y1, this.getWidth(), y1);
			if (lastLine == this.activeLineRangeEnd)
				g.drawLine(0, y2, this.getWidth(), y2);

		}

		if (this.trackingIcons != null) {
			int lastLine = bottomLine;
			for (int i = this.trackingIcons.size() - 1; i >= 0; i--) { // Last to first
				final GutterIconInfo ti = this.getTrackingIcon(i);
				final int offs = ti.getMarkedOffset();
				if (offs >= 0 && offs <= doc.getLength()) {
					final int line = root.getElementIndex(offs);
					if (line <= lastLine && line >= topLine) {
						final Icon icon = ti.getIcon();
						if (icon != null) {
							int y2 = y + (line - topLine) * cellHeight;
							y2 += (cellHeight - icon.getIconHeight()) / 2;
							ti.getIcon().paintIcon(this, g, 0, y2);
							lastLine = line - 1; // Paint only 1 icon per line
						}
					} else if (line < topLine)
						break;
				}
			}
		}

	}

	/**
	 * Paints icons when line wrapping is enabled.
	 *
	 * @param g
	 *            The graphics context.
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
		// - offset (<=0) is the y-coordinate at which we begin painting when
		// we begin painting with the first logical line. This can be
		// negative, signifying that we've scrolled past the actual topmost
		// part of this line.

		// The algorithm is as follows:
		// - Get the starting y-coordinate at which to paint. This may be
		// above the first visible y-coordinate as we're in line-wrapping
		// mode, but we always paint entire logical lines.
		// - Paint that line's line number and highlight, if appropriate.
		// Increment y to be just below the are we just painted (i.e., the
		// beginning of the next logical line's view area).
		// - Get the ending visual position for that line. We can now loop
		// back, paint this line, and continue until our y-coordinate is
		// past the last visible y-value.

		// We avoid using modelToView/viewToModel where possible, as these
		// methods trigger a parsing of the line into syntax tokens, which is
		// costly. It's cheaper to just grab the child views' bounds.

		final RTextAreaUI ui = (RTextAreaUI) this.textArea.getUI();
		final View v = ui.getRootView(this.textArea).getView(0);
		// boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		final Document doc = this.textArea.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int lineCount = root.getElementCount();
		final int topPosition = this.textArea.viewToModel(new Point(this.visibleRect.x, this.visibleRect.y));
		int topLine = root.getElementIndex(topPosition);

		// Compute the y at which to begin painting text, taking into account
		// that 1 logical line => at least 1 physical line, so it may be that
		// y<0. The computed y-value is the y-value of the top of the first
		// (possibly) partially-visible view.
		final Rectangle visibleEditorRect = ui.getVisibleEditorRect();
		Rectangle r = AbstractGutterComponent.getChildViewBounds(v, topLine, visibleEditorRect);
		int y = r.y;

		final int visibleBottom = this.visibleRect.y + this.visibleRect.height;

		// Get the first possibly visible icon index.
		int currentIcon = -1;
		if (this.trackingIcons != null)
			for (int i = 0; i < this.trackingIcons.size(); i++) {
				final GutterIconImpl icon = this.getTrackingIcon(i);
				final int offs = icon.getMarkedOffset();
				if (offs >= 0 && offs <= doc.getLength()) {
					final int line = root.getElementIndex(offs);
					if (line >= topLine) {
						currentIcon = i;
						break;
					}
				}
			}

		// Keep painting lines until our y-coordinate is past the visible
		// end of the text area.
		g.setColor(this.getForeground());
		final int cellHeight = this.textArea.getLineHeight();
		while (y < visibleBottom) {

			r = AbstractGutterComponent.getChildViewBounds(v, topLine, visibleEditorRect);
			// int lineEndY = r.y+r.height;

			/*
			 * // Highlight the current line's line number, if desired. if
			 * (currentLineHighlighted && topLine==currentLine) {
			 * g.setColor(textArea.getCurrentLineHighlightColor()); g.fillRect(0,y,
			 * width,lineEndY-y); g.setColor(getForeground()); }
			 */

			// Possibly paint an icon.
			if (currentIcon > -1) {
				// We want to paint the last icon added for this line.
				GutterIconImpl toPaint = null;
				while (currentIcon < this.trackingIcons.size()) {
					final GutterIconImpl ti = this.getTrackingIcon(currentIcon);
					final int offs = ti.getMarkedOffset();
					if (offs >= 0 && offs <= doc.getLength()) {
						final int line = root.getElementIndex(offs);
						if (line == topLine)
							toPaint = ti;
						else if (line > topLine)
							break;
					}
					currentIcon++;
				}
				if (toPaint != null) {
					final Icon icon = toPaint.getIcon();
					if (icon != null) {
						final int y2 = y + (cellHeight - icon.getIconHeight()) / 2;
						icon.paintIcon(this, g, 0, y2);
					}
				}
			}

			// The next possible y-coordinate is just after the last line
			// painted.
			y += r.height;

			// Update topLine (we're actually using it for our "current line"
			// variable now).
			topLine++;
			if (topLine >= lineCount)
				break;

		}

	}

	/**
	 * Removes all tracking icons.
	 *
	 * @see #removeTrackingIcon(Object)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeAllTrackingIcons() {
		if (this.trackingIcons != null && this.trackingIcons.size() > 0) {
			this.trackingIcons.clear();
			this.repaint();
		}
	}

	/**
	 * Removes all bookmark tracking icons.
	 */
	private void removeBookmarkTrackingIcons() {
		if (this.trackingIcons != null)
			for (final Iterator<GutterIconImpl> i = this.trackingIcons.iterator(); i.hasNext();) {
				final GutterIconImpl ti = i.next();
				if (ti.getIcon() == this.bookmarkIcon)
					i.remove();
			}
	}

	/**
	 * Removes the specified tracking icon.
	 *
	 * @param tag
	 *            A tag for a tracking icon.
	 * @see #removeAllTrackingIcons()
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeTrackingIcon(final Object tag) {
		if (this.trackingIcons != null && this.trackingIcons.remove(tag))
			this.repaint();
	}

	/**
	 * Highlights a range of lines in the icon area.
	 *
	 * @param startLine
	 *            The start of the line range.
	 * @param endLine
	 *            The end of the line range.
	 * @see #clearActiveLineRange()
	 */
	public void setActiveLineRange(final int startLine, final int endLine) {
		if (startLine != this.activeLineRangeStart || endLine != this.activeLineRangeEnd) {
			this.activeLineRangeStart = startLine;
			this.activeLineRangeEnd = endLine;
			this.repaint();
		}
	}

	/**
	 * Sets the color to use to render active line ranges.
	 *
	 * @param color
	 *            The color to use. If this is null, then the default color is used.
	 * @see #getActiveLineRangeColor()
	 * @see Gutter#DEFAULT_ACTIVE_LINE_RANGE_COLOR
	 */
	public void setActiveLineRangeColor(Color color) {
		if (color == null)
			color = Gutter.DEFAULT_ACTIVE_LINE_RANGE_COLOR;
		if (!color.equals(this.activeLineRangeColor)) {
			this.activeLineRangeColor = color;
			this.repaint();
		}
	}

	/**
	 * Sets the icon to use for bookmarks. Any previous bookmark icons are removed.
	 *
	 * @param icon
	 *            The new bookmark icon. If this is <code>null</code>, bookmarking
	 *            is effectively disabled.
	 * @see #getBookmarkIcon()
	 * @see #isBookmarkingEnabled()
	 */
	public void setBookmarkIcon(final Icon icon) {
		this.removeBookmarkTrackingIcons();
		this.bookmarkIcon = icon;
		this.repaint();
	}

	/**
	 * Sets whether bookmarking is enabled. Note that a bookmarking icon must be set
	 * via {@link #setBookmarkIcon(Icon)} before bookmarks are truly enabled.
	 *
	 * @param enabled
	 *            Whether bookmarking is enabled. If this is <code>false</code>, any
	 *            bookmark icons are removed.
	 * @see #isBookmarkingEnabled()
	 * @see #setBookmarkIcon(Icon)
	 */
	public void setBookmarkingEnabled(final boolean enabled) {
		if (enabled != this.bookmarkingEnabled) {
			this.bookmarkingEnabled = enabled;
			if (!enabled)
				this.removeBookmarkTrackingIcons();
			this.repaint();
		}
	}

	/**
	 * Sets whether the icon area inherits the gutter background (as opposed to
	 * painting with its own, default "panel" color, which is the default).
	 *
	 * @param inherits
	 *            Whether the gutter background should be used in the icon row
	 *            header. If this is <code>false</code>, a default,
	 *            Look-and-feel-dependent color is used.
	 */
	public void setInheritsGutterBackground(final boolean inherits) {
		if (inherits != this.inheritsGutterBackground) {
			this.inheritsGutterBackground = inherits;
			this.repaint();
		}
	}

	/**
	 * Sets the text area being displayed. This will clear any tracking icons
	 * currently displayed.
	 *
	 * @param textArea
	 *            The text area.
	 */
	@Override
	public void setTextArea(final RTextArea textArea) {
		this.removeAllTrackingIcons();
		super.setTextArea(textArea);
	}

	/**
	 * Programatically toggles whether there is a bookmark for the specified line.
	 * If bookmarking is not enabled, this method does nothing.
	 *
	 * @param line
	 *            The line.
	 * @return Whether a bookmark is now at the specified line.
	 * @throws BadLocationException
	 *             If <code>line</code> is an invalid line number in the text area.
	 */
	public boolean toggleBookmark(final int line) throws BadLocationException {

		if (!this.isBookmarkingEnabled() || this.getBookmarkIcon() == null)
			return false;

		final GutterIconInfo[] icons = this.getTrackingIcons(line);
		if (icons.length == 0) {
			final int offs = this.textArea.getLineStartOffset(line);
			this.addOffsetTrackingIcon(offs, this.bookmarkIcon);
			return true;
		}

		boolean found = false;
		for (final GutterIconInfo icon : icons)
			if (icon.getIcon() == this.bookmarkIcon) {
				this.removeTrackingIcon(icon);
				found = true;
				// Don't quit, in case they manipulate the document so > 1
				// bookmark is on a single line (kind of flaky, but it
				// works...). If they delete all chars in the document,
				// AbstractDocument gets a little flaky with the returned line
				// number for viewToModel(), so this is just us trying to save
				// face a little.
			}
		if (!found) {
			final int offs = this.textArea.getLineStartOffset(line);
			this.addOffsetTrackingIcon(offs, this.bookmarkIcon);
		}

		return !found;

	}

	/**
	 * Sets our background color to that of standard "panels" in this LookAndFeel.
	 * This is necessary because, otherwise, we'd inherit the background color of
	 * our parent component (the Gutter).
	 */
	private void updateBackground() {
		Color bg = UIManager.getColor("Panel.background");
		if (bg == null)
			bg = new JPanel().getBackground();
		this.setBackground(bg);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateUI() {
		super.updateUI(); // Does nothing
		this.updateBackground();
	}

	/**
	 * Returns the line rendered at the specified location.
	 *
	 * @param p
	 *            The location in this row header.
	 * @return The corresponding line in the editor.
	 * @throws BadLocationException
	 *             ble If an error occurs.
	 */
	private int viewToModelLine(final Point p) throws BadLocationException {
		final int offs = this.textArea.viewToModel(p);
		return offs > -1 ? this.textArea.getLineOfOffset(offs) : -1;
	}

}