/*
 * 02/17/2009
 *
 * Gutter.java - Manages line numbers, icons, etc. on the left-hand side of
 * an RTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.ActiveLineRangeEvent;
import org.fife.ui.rsyntaxtextarea.ActiveLineRangeListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;

/**
 * The gutter is the component on the left-hand side of the text area that
 * displays optional information such as line numbers, fold regions, and icons
 * (for bookmarks, debugging breakpoints, error markers, etc.).
 * <p>
 *
 * Icons can be added on a per-line basis to visually mark syntax errors, lines
 * with breakpoints set on them, etc. To add icons to the gutter, you must first
 * call {@link RTextScrollPane#setIconRowHeaderEnabled(boolean)} on the parent
 * scroll pane, to make the icon area visible. Then, you can add icons that
 * track either lines in the document, or offsets, via
 * {@link #addLineTrackingIcon(int, Icon)} and
 * {@link #addOffsetTrackingIcon(int, Icon)}, respectively. To remove an icon
 * you've added, use {@link #removeTrackingIcon(GutterIconInfo)}.
 * <p>
 *
 * In addition to support for arbitrary per-line icons, this component also has
 * built-in support for displaying icons representing "bookmarks;" that is,
 * lines a user can cycle through via F2 and Shift+F2. Bookmarked lines are
 * toggled via Ctrl+F2. In order to enable bookmarking, you must first assign an
 * icon to represent a bookmarked line, then actually enable the feature:
 *
 * <pre>
 * Gutter gutter = scrollPane.getGutter();
 * gutter.setBookmarkIcon(new ImageIcon("bookmark.png"));
 * gutter.setBookmarkingEnabled(true);
 * </pre>
 *
 * @author Robert Futrell
 * @version 1.0
 * @see GutterIconInfo
 */
public class Gutter extends JPanel {

	/**
	 * The border used by the gutter.
	 */
	public static class GutterBorder extends EmptyBorder {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private Color color;
		private Rectangle visibleRect;

		public GutterBorder(final int top, final int left, final int bottom, final int right) {
			super(top, left, bottom, right);
			this.color = new Color(221, 221, 221);
			this.visibleRect = new Rectangle();
		}

		public Color getColor() {
			return this.color;
		}

		@Override
		public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width,
				final int height) {

			this.visibleRect = g.getClipBounds(this.visibleRect);
			if (this.visibleRect == null)
				this.visibleRect = ((JComponent) c).getVisibleRect();

			g.setColor(this.color);
			if (this.left == 1)
				g.drawLine(0, this.visibleRect.y, 0, this.visibleRect.y + this.visibleRect.height);
			else
				g.drawLine(width - 1, this.visibleRect.y, width - 1, this.visibleRect.y + this.visibleRect.height);

		}

		public void setColor(final Color color) {
			this.color = color;
		}

		public void setEdges(final int top, final int left, final int bottom, final int right) {
			this.top = top;
			this.left = left;
			this.bottom = bottom;
			this.right = right;
		}

	}

	/**
	 * Listens for the text area resizing.
	 */
	private class TextAreaListener extends ComponentAdapter
			implements DocumentListener, PropertyChangeListener, ActiveLineRangeListener {

		// This is necessary to keep child components the same height as the text
		// area. The worse case is when the user toggles word-wrap and it changes
		// the height of the text area. In that case, if we listen for the
		// "lineWrap" property change, we get notified BEFORE the text area
		// decides on its new size, thus we cannot resize properly. We listen
		// instead for ComponentEvents so we change size after the text area has
		// resized.

		private boolean installed;

		/**
		 * Modifies the "active line range" that is painted in this component.
		 *
		 * @param e
		 *            Information about the new "active line range."
		 */
		@Override
		public void activeLineRangeChanged(final ActiveLineRangeEvent e) {
			if (e.getMin() == -1)
				Gutter.this.clearActiveLineRange();
			else
				Gutter.this.setActiveLineRange(e.getMin(), e.getMax());
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		@Override
		public void componentResized(final java.awt.event.ComponentEvent e) {
			Gutter.this.revalidate();
		}

		protected void handleDocumentEvent(final DocumentEvent e) {
			for (int i = 0; i < Gutter.this.getComponentCount(); i++) {
				final AbstractGutterComponent agc = (AbstractGutterComponent) Gutter.this.getComponent(i);
				agc.handleDocumentEvent(e);
			}
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

		public void install(final RTextArea textArea) {
			if (this.installed)
				this.uninstall();
			textArea.addComponentListener(this);
			textArea.getDocument().addDocumentListener(this);
			textArea.addPropertyChangeListener(this);
			if (textArea instanceof RSyntaxTextArea) {
				final RSyntaxTextArea rsta = (RSyntaxTextArea) textArea;
				rsta.addActiveLineRangeListener(this);
				rsta.getFoldManager().addPropertyChangeListener(this);
			}
			this.installed = true;
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			final String name = e.getPropertyName();

			// If they change the text area's font, we need to update cell
			// heights to match the font's height.
			if ("font".equals(name) || RSyntaxTextArea.SYNTAX_SCHEME_PROPERTY.equals(name))
				for (int i = 0; i < Gutter.this.getComponentCount(); i++) {
					final AbstractGutterComponent agc = (AbstractGutterComponent) Gutter.this.getComponent(i);
					agc.lineHeightsChanged();
				}
			else if (RSyntaxTextArea.CODE_FOLDING_PROPERTY.equals(name)) {
				final boolean foldingEnabled = ((Boolean) e.getNewValue()).booleanValue();
				if (Gutter.this.lineNumberList != null)
					// lineNumberList.revalidate();
					Gutter.this.lineNumberList.updateCellWidths();
				Gutter.this.setFoldIndicatorEnabled(foldingEnabled);
			}

			// If code folds are updated...
			else if (FoldManager.PROPERTY_FOLDS_UPDATED.equals(name))
				Gutter.this.repaint();
			else if ("document".equals(name)) {
				// The document switched out from under us
				final RDocument old = (RDocument) e.getOldValue();
				if (old != null)
					old.removeDocumentListener(this);
				final RDocument newDoc = (RDocument) e.getNewValue();
				if (newDoc != null)
					newDoc.addDocumentListener(this);
			}

		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

		public void uninstall() {
			if (this.installed) {
				Gutter.this.textArea.removeComponentListener(this);
				Gutter.this.textArea.getDocument().removeDocumentListener(this);
				Gutter.this.textArea.removePropertyChangeListener(this);
				if (Gutter.this.textArea instanceof RSyntaxTextArea) {
					final RSyntaxTextArea rsta = (RSyntaxTextArea) Gutter.this.textArea;
					rsta.removeActiveLineRangeListener(this);
					rsta.getFoldManager().removePropertyChangeListener(this);
				}
				this.installed = false;
			}
		}

	}

	/**
	 * The color used to highlight active line ranges if none is specified.
	 */
	public static final Color DEFAULT_ACTIVE_LINE_RANGE_COLOR = new Color(51, 153, 255);

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Shows lines that are code-foldable.
	 */
	private FoldIndicator foldIndicator;

	/**
	 * Renders bookmark icons, breakpoints, error icons, etc.
	 */
	private IconRowHeader iconArea;

	/**
	 * Whether the icon area inherits the gutter background (as opposed to painting
	 * with its own, default "panel" color).
	 */
	private boolean iconRowHeaderInheritsGutterBackground;

	/**
	 * The color used to render line numbers.
	 */
	private Color lineNumberColor;

	/**
	 * The font used to render line numbers.
	 */
	private Font lineNumberFont;

	/**
	 * The starting index for line numbers in the gutter.
	 */
	private int lineNumberingStartIndex;

	/**
	 * Renders line numbers.
	 */
	private LineNumberList lineNumberList;

	/**
	 * Listens for events in our text area.
	 */
	private transient TextAreaListener listener;

	/**
	 * The text area.
	 */
	private RTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The parent text area.
	 */
	public Gutter(final RTextArea textArea) {

		this.listener = new TextAreaListener();
		this.lineNumberColor = Color.gray;
		this.lineNumberFont = RTextAreaBase.getDefaultFont();
		this.lineNumberingStartIndex = 1;
		this.iconRowHeaderInheritsGutterBackground = false;

		this.setTextArea(textArea);
		this.setLayout(new BorderLayout());
		if (this.textArea != null) {
			// Enable line numbers our first time through if they give us
			// a text area.
			this.setLineNumbersEnabled(true);
			if (this.textArea instanceof RSyntaxTextArea) {
				final RSyntaxTextArea rsta = (RSyntaxTextArea) this.textArea;
				this.setFoldIndicatorEnabled(rsta.isCodeFoldingEnabled());
			}
		}

		this.setBorder(new GutterBorder(0, 0, 0, 1)); // Assume ltr

		Color bg = null;
		if (textArea != null)
			bg = textArea.getBackground(); // May return null if image bg
		this.setBackground(bg != null ? bg : Color.WHITE);

	}

	/**
	 * Adds an icon that tracks an offset in the document, and is displayed adjacent
	 * to the line numbers. This is useful for marking things such as source code
	 * errors.
	 *
	 * @param line
	 *            The line to track (zero-based).
	 * @param icon
	 *            The icon to display. This should be small (say 16x16).
	 * @return A tag for this icon. This can later be used in a call to
	 *         {@link #removeTrackingIcon(GutterIconInfo)} to remove this icon.
	 * @throws BadLocationException
	 *             If <code>offs</code> is an invalid offset into the text area.
	 * @see #addLineTrackingIcon(int, Icon, String)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 * @see #removeTrackingIcon(GutterIconInfo)
	 */
	public GutterIconInfo addLineTrackingIcon(final int line, final Icon icon) throws BadLocationException {
		return this.addLineTrackingIcon(line, icon, null);
	}

	/**
	 * Adds an icon that tracks an offset in the document, and is displayed adjacent
	 * to the line numbers. This is useful for marking things such as source code
	 * errors.
	 *
	 * @param line
	 *            The line to track (zero-based).
	 * @param icon
	 *            The icon to display. This should be small (say 16x16).
	 * @param tip
	 *            An optional tool tip for the icon.
	 * @return A tag for this icon. This can later be used in a call to
	 *         {@link #removeTrackingIcon(GutterIconInfo)} to remove this icon.
	 * @throws BadLocationException
	 *             If <code>offs</code> is an invalid offset into the text area.
	 * @see #addLineTrackingIcon(int, Icon)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 * @see #removeTrackingIcon(GutterIconInfo)
	 */
	public GutterIconInfo addLineTrackingIcon(final int line, final Icon icon, final String tip)
			throws BadLocationException {
		final int offs = this.textArea.getLineStartOffset(line);
		return this.addOffsetTrackingIcon(offs, icon, tip);
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
	 * @see #addOffsetTrackingIcon(int, Icon, String)
	 * @see #addLineTrackingIcon(int, Icon)
	 * @see #removeTrackingIcon(GutterIconInfo)
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
	 *            An optional tool tip for the icon.
	 * @return A tag for this icon.
	 * @throws BadLocationException
	 *             If <code>offs</code> is an invalid offset into the text area.
	 * @see #addOffsetTrackingIcon(int, Icon)
	 * @see #addLineTrackingIcon(int, Icon)
	 * @see #removeTrackingIcon(GutterIconInfo)
	 */
	public GutterIconInfo addOffsetTrackingIcon(final int offs, final Icon icon, final String tip)
			throws BadLocationException {
		return this.iconArea.addOffsetTrackingIcon(offs, icon, tip);
	}

	/**
	 * Clears the active line range.
	 *
	 * @see #setActiveLineRange(int, int)
	 */
	private void clearActiveLineRange() {
		this.iconArea.clearActiveLineRange();
	}

	/**
	 * Returns the color used to paint the active line range, if any.
	 *
	 * @return The color.
	 * @see #setActiveLineRangeColor(Color)
	 */
	public Color getActiveLineRangeColor() {
		return this.iconArea.getActiveLineRangeColor();
	}

	/**
	 * Returns the background color used by the (default) fold icons when they are
	 * armed.
	 *
	 * @return The background color.
	 * @see #setArmedFoldBackground(Color)
	 * @see #getFoldBackground()
	 */
	public Color getArmedFoldBackground() {
		return this.foldIndicator.getFoldIconArmedBackground();
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
		return this.iconArea.getBookmarkIcon();
	}

	/**
	 * Returns the bookmarks known to this gutter.
	 *
	 * @return The bookmarks. If there are no bookmarks, an empty array is returned.
	 * @see #toggleBookmark(int)
	 */
	public GutterIconInfo[] getBookmarks() {
		return this.iconArea.getBookmarks();
	}

	/**
	 * Returns the color of the "border" line.
	 *
	 * @return The color.
	 * @see #setBorderColor(Color)
	 */
	public Color getBorderColor() {
		return ((GutterBorder) this.getBorder()).getColor();
	}

	/**
	 * Returns the background color used by the (default) fold icons.
	 *
	 * @return The background color.
	 * @see #setFoldBackground(Color)
	 */
	public Color getFoldBackground() {
		return this.foldIndicator.getFoldIconBackground();
	}

	/**
	 * Returns the foreground color of the fold indicator.
	 *
	 * @return The foreground color of the fold indicator.
	 * @see #setFoldIndicatorForeground(Color)
	 */
	public Color getFoldIndicatorForeground() {
		return this.foldIndicator.getForeground();
	}

	/**
	 * Returns whether the icon area inherits the gutter background (as opposed to
	 * painting with its own, default "panel" color, which is the default).
	 *
	 * @return Whether the gutter background is used in the icon row header.
	 * @see #setIconRowHeaderInheritsGutterBackground(boolean)
	 */
	public boolean getIconRowHeaderInheritsGutterBackground() {
		return this.iconRowHeaderInheritsGutterBackground;
	}

	/**
	 * Returns the color to use to paint line numbers.
	 *
	 * @return The color used when painting line numbers.
	 * @see #setLineNumberColor(Color)
	 */
	public Color getLineNumberColor() {
		return this.lineNumberColor;
	}

	/**
	 * Returns the font used for line numbers.
	 *
	 * @return The font used for line numbers.
	 * @see #setLineNumberFont(Font)
	 */
	public Font getLineNumberFont() {
		return this.lineNumberFont;
	}

	/**
	 * Returns the starting line's line number. The default value is <code>1</code>.
	 *
	 * @return The index
	 * @see #setLineNumberingStartIndex(int)
	 */
	public int getLineNumberingStartIndex() {
		return this.lineNumberingStartIndex;
	}

	/**
	 * Returns <code>true</code> if the line numbers are enabled and visible.
	 *
	 * @return Whether or not line numbers are visible.
	 */
	public boolean getLineNumbersEnabled() {
		for (int i = 0; i < this.getComponentCount(); i++)
			if (this.getComponent(i) == this.lineNumberList)
				return true;
		return false;
	}

	/**
	 * Returns whether tool tips are displayed showing the contents of collapsed
	 * fold regions when the mouse hovers over a +/- icon.
	 *
	 * @return Whether these tool tips are displayed.
	 * @see #setShowCollapsedRegionToolTips(boolean)
	 */
	public boolean getShowCollapsedRegionToolTips() {
		return this.foldIndicator.getShowCollapsedRegionToolTips();
	}

	/**
	 * Returns the tracking icons at the specified view position.
	 *
	 * @param p
	 *            The view position.
	 * @return The tracking icons at that position. If there are no tracking icons
	 *         there, this will be an empty array.
	 * @throws BadLocationException
	 *             If <code>p</code> is invalid.
	 */
	public GutterIconInfo[] getTrackingIcons(final Point p) throws BadLocationException {
		final int offs = this.textArea.viewToModel(new Point(0, p.y));
		final int line = this.textArea.getLineOfOffset(offs);
		return this.iconArea.getTrackingIcons(line);
	}

	/**
	 * Returns whether bookmarking is enabled.
	 *
	 * @return Whether bookmarking is enabled.
	 * @see #setBookmarkingEnabled(boolean)
	 */
	public boolean isBookmarkingEnabled() {
		return this.iconArea.isBookmarkingEnabled();
	}

	/**
	 * Returns whether the fold indicator is enabled.
	 *
	 * @return Whether the fold indicator is enabled.
	 * @see #setFoldIndicatorEnabled(boolean)
	 */
	public boolean isFoldIndicatorEnabled() {
		for (int i = 0; i < this.getComponentCount(); i++)
			if (this.getComponent(i) == this.foldIndicator)
				return true;
		return false;
	}

	/**
	 * Returns whether the icon row header is enabled.
	 *
	 * @return Whether the icon row header is enabled.
	 */
	public boolean isIconRowHeaderEnabled() {
		for (int i = 0; i < this.getComponentCount(); i++)
			if (this.getComponent(i) == this.iconArea)
				return true;
		return false;
	}

	/**
	 * Removes all tracking icons.
	 *
	 * @see #removeTrackingIcon(GutterIconInfo)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeAllTrackingIcons() {
		this.iconArea.removeAllTrackingIcons();
	}

	/**
	 * Removes the specified tracking icon.
	 *
	 * @param tag
	 *            A tag for an icon in the gutter, as returned from either
	 *            {@link #addLineTrackingIcon(int, Icon)} or
	 *            {@link #addOffsetTrackingIcon(int, Icon)}.
	 * @see #removeAllTrackingIcons()
	 * @see #addLineTrackingIcon(int, Icon)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeTrackingIcon(final GutterIconInfo tag) {
		this.iconArea.removeTrackingIcon(tag);
	}

	/**
	 * Highlights a range of lines in the icon area. This, of course, will only be
	 * visible if the icon area is visible.
	 *
	 * @param startLine
	 *            The start of the line range.
	 * @param endLine
	 *            The end of the line range.
	 * @see #clearActiveLineRange()
	 */
	private void setActiveLineRange(final int startLine, final int endLine) {
		this.iconArea.setActiveLineRange(startLine, endLine);
	}

	/**
	 * Sets the color to use to render active line ranges.
	 *
	 * @param color
	 *            The color to use. If this is null, then the default color is used.
	 * @see #getActiveLineRangeColor()
	 * @see #DEFAULT_ACTIVE_LINE_RANGE_COLOR
	 */
	public void setActiveLineRangeColor(final Color color) {
		this.iconArea.setActiveLineRangeColor(color);
	}

	/**
	 * Sets the background color used by the (default) fold icons when they are
	 * armed.
	 *
	 * @param bg
	 *            The new background color. If this is {@code null}, then armed fold
	 *            icons will not render with a special color.
	 * @see #getArmedFoldBackground()
	 * @see #setFoldBackground(Color)
	 */
	public void setArmedFoldBackground(final Color bg) {
		this.foldIndicator.setFoldIconArmedBackground(bg);
	}

	/**
	 * Sets the icon to use for bookmarks.
	 *
	 * @param icon
	 *            The new bookmark icon. If this is <code>null</code>, bookmarking
	 *            is effectively disabled.
	 * @see #getBookmarkIcon()
	 * @see #isBookmarkingEnabled()
	 */
	public void setBookmarkIcon(final Icon icon) {
		this.iconArea.setBookmarkIcon(icon);
	}

	/**
	 * Sets whether bookmarking is enabled. Note that a bookmarking icon must be set
	 * via {@link #setBookmarkIcon(Icon)} before bookmarks are truly enabled.
	 *
	 * @param enabled
	 *            Whether bookmarking is enabled.
	 * @see #isBookmarkingEnabled()
	 * @see #setBookmarkIcon(Icon)
	 */
	public void setBookmarkingEnabled(final boolean enabled) {
		this.iconArea.setBookmarkingEnabled(enabled);
		if (enabled && !this.isIconRowHeaderEnabled())
			this.setIconRowHeaderEnabled(true);
	}

	// public void setUI(ComponentUI ui) {
	//
	// Border gutterBorder = getBorder();
	//
	// super.setUI(ui);
	//
	// // Some LaFs, such as WebLookAndFeel, override borders even when
	// // they aren't UIResources.
	// Border border = getBorder();
	// if (border != gutterBorder) {
	// setBorder(gutterBorder);
	// }
	//
	// }
	//
	//
	@Override
	public void setBorder(final Border border) {
		if (border instanceof GutterBorder)
			super.setBorder(border);
	}

	/**
	 * Sets the color for the "border" line.
	 *
	 * @param color
	 *            The new color.
	 * @see #getBorderColor()
	 */
	public void setBorderColor(final Color color) {
		((GutterBorder) this.getBorder()).setColor(color);
		this.repaint();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setComponentOrientation(final ComponentOrientation o) {

		// Some LaFs might do fun stuff, resulting in this method being called
		// before a border is installed.

		if (this.getBorder() instanceof GutterBorder)
			// Reuse the border to preserve its color.
			if (o.isLeftToRight())
				((GutterBorder) this.getBorder()).setEdges(0, 0, 0, 1);
			else
				((GutterBorder) this.getBorder()).setEdges(0, 1, 0, 0);
		super.setComponentOrientation(o);
	}

	/**
	 * Sets the background color used by the (default) fold icons.
	 *
	 * @param bg
	 *            The new background color.
	 * @see #getFoldBackground()
	 * @see #setArmedFoldBackground(Color)
	 */
	public void setFoldBackground(Color bg) {
		if (bg == null)
			bg = FoldIndicator.DEFAULT_FOLD_BACKGROUND;
		this.foldIndicator.setFoldIconBackground(bg);
	}

	/**
	 * Sets the icons to use to represent collapsed and expanded folds.
	 *
	 * @param collapsedIcon
	 *            The collapsed fold icon. This cannot be <code>null</code>.
	 * @param expandedIcon
	 *            The expanded fold icon. This cannot be <code>null</code>.
	 */
	public void setFoldIcons(final Icon collapsedIcon, final Icon expandedIcon) {
		if (this.foldIndicator != null)
			this.foldIndicator.setFoldIcons(collapsedIcon, expandedIcon);
	}

	/**
	 * Toggles whether the fold indicator is enabled.
	 *
	 * @param enabled
	 *            Whether the fold indicator should be enabled.
	 * @see #isFoldIndicatorEnabled()
	 */
	public void setFoldIndicatorEnabled(final boolean enabled) {
		if (this.foldIndicator != null) {
			if (enabled)
				this.add(this.foldIndicator, BorderLayout.LINE_END);
			else
				this.remove(this.foldIndicator);
			this.revalidate();
		}
	}

	/**
	 * Sets the foreground color used by the fold indicator.
	 *
	 * @param fg
	 *            The new fold indicator foreground.
	 * @see #getFoldIndicatorForeground()
	 */
	public void setFoldIndicatorForeground(Color fg) {
		if (fg == null)
			fg = FoldIndicator.DEFAULT_FOREGROUND;
		this.foldIndicator.setForeground(fg);
	}

	/**
	 * Toggles whether the icon row header (used for breakpoints, bookmarks, etc.)
	 * is enabled.
	 *
	 * @param enabled
	 *            Whether the icon row header is enabled.
	 * @see #isIconRowHeaderEnabled()
	 */
	void setIconRowHeaderEnabled(final boolean enabled) {
		if (this.iconArea != null) {
			if (enabled)
				this.add(this.iconArea, BorderLayout.LINE_START);
			else
				this.remove(this.iconArea);
			this.revalidate();
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
	 * @see #getIconRowHeaderInheritsGutterBackground()
	 */
	public void setIconRowHeaderInheritsGutterBackground(final boolean inherits) {
		if (inherits != this.iconRowHeaderInheritsGutterBackground) {
			this.iconRowHeaderInheritsGutterBackground = inherits;
			if (this.iconArea != null)
				this.iconArea.setInheritsGutterBackground(inherits);
		}
	}

	/**
	 * Sets the color to use to paint line numbers.
	 *
	 * @param color
	 *            The color to use when painting line numbers.
	 * @see #getLineNumberColor()
	 */
	public void setLineNumberColor(final Color color) {
		if (color != null && !color.equals(this.lineNumberColor)) {
			this.lineNumberColor = color;
			if (this.lineNumberList != null)
				this.lineNumberList.setForeground(color);
		}
	}

	/**
	 * Sets the font used for line numbers.
	 *
	 * @param font
	 *            The font to use. This cannot be <code>null</code>.
	 * @see #getLineNumberFont()
	 */
	public void setLineNumberFont(final Font font) {
		if (font == null)
			throw new IllegalArgumentException("font cannot be null");
		if (!font.equals(this.lineNumberFont)) {
			this.lineNumberFont = font;
			if (this.lineNumberList != null)
				this.lineNumberList.setFont(font);
		}
	}

	/**
	 * Sets the starting line's line number. The default value is <code>1</code>.
	 * Applications can call this method to change this value if they are displaying
	 * a subset of lines in a file, for example.
	 *
	 * @param index
	 *            The new index.
	 * @see #getLineNumberingStartIndex()
	 */
	public void setLineNumberingStartIndex(final int index) {
		if (index != this.lineNumberingStartIndex) {
			this.lineNumberingStartIndex = index;
			this.lineNumberList.setLineNumberingStartIndex(index);
		}
	}

	/**
	 * Toggles whether or not line numbers are visible.
	 *
	 * @param enabled
	 *            Whether or not line numbers should be visible.
	 * @see #getLineNumbersEnabled()
	 */
	void setLineNumbersEnabled(final boolean enabled) {
		if (this.lineNumberList != null) {
			if (enabled)
				this.add(this.lineNumberList);
			else
				this.remove(this.lineNumberList);
			this.revalidate();
		}
	}

	/**
	 * Toggles whether tool tips should be displayed showing the contents of
	 * collapsed fold regions when the mouse hovers over a +/- icon.
	 *
	 * @param show
	 *            Whether to show these tool tips.
	 * @see #getShowCollapsedRegionToolTips()
	 */
	public void setShowCollapsedRegionToolTips(final boolean show) {
		if (this.foldIndicator != null)
			this.foldIndicator.setShowCollapsedRegionToolTips(show);
	}

	/**
	 * Sets the text area being displayed. This will clear any tracking icons
	 * currently displayed.
	 *
	 * @param textArea
	 *            The text area.
	 */
	void setTextArea(final RTextArea textArea) {

		if (this.textArea != null)
			this.listener.uninstall();

		if (textArea != null) {

			final RTextAreaEditorKit kit = (RTextAreaEditorKit) textArea.getUI().getEditorKit(textArea);

			if (this.lineNumberList == null) {
				this.lineNumberList = kit.createLineNumberList(textArea);
				this.lineNumberList.setFont(this.getLineNumberFont());
				this.lineNumberList.setForeground(this.getLineNumberColor());
				this.lineNumberList.setLineNumberingStartIndex(this.getLineNumberingStartIndex());
			} else
				this.lineNumberList.setTextArea(textArea);
			if (this.iconArea == null) {
				this.iconArea = kit.createIconRowHeader(textArea);
				this.iconArea.setInheritsGutterBackground(this.getIconRowHeaderInheritsGutterBackground());
			} else
				this.iconArea.setTextArea(textArea);
			if (this.foldIndicator == null)
				this.foldIndicator = new FoldIndicator(textArea);
			else
				this.foldIndicator.setTextArea(textArea);

			this.listener.install(textArea);

		}

		this.textArea = textArea;

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
	 * @see #getBookmarks()
	 */
	public boolean toggleBookmark(final int line) throws BadLocationException {
		return this.iconArea.toggleBookmark(line);
	}

}