/*
 * 02/11/2009
 *
 * LineNumberList.java - Renders line numbers in an RTextScrollPane.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.MouseInputListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;

/**
 * Renders line numbers in the gutter.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LineNumberList extends AbstractGutterComponent implements MouseInputListener {

	/**
	 * Listens for events in the text area we're interested in.
	 */
	private class Listener implements CaretListener, PropertyChangeListener {

		private boolean installed;

		@Override
		public void caretUpdate(final CaretEvent e) {

			final int dot = LineNumberList.this.textArea.getCaretPosition();

			// We separate the line wrap/no line wrap cases because word wrap
			// can make a single line from the model (document) be on multiple
			// lines on the screen (in the view); thus, we have to enhance the
			// logic for that case a bit - we check the actual y-coordinate of
			// the caret when line wrap is enabled. For the no-line-wrap case,
			// getting the line number of the caret suffices. This increases
			// efficiency in the no-line-wrap case.

			if (!LineNumberList.this.textArea.getLineWrap()) {
				final int line = LineNumberList.this.textArea.getDocument().getDefaultRootElement()
						.getElementIndex(dot);
				if (LineNumberList.this.currentLine != line) {
					LineNumberList.this.repaintLine(line);
					LineNumberList.this.repaintLine(LineNumberList.this.currentLine);
					LineNumberList.this.currentLine = line;
				}
			} else
				try {
					final int y = LineNumberList.this.textArea.yForLineContaining(dot);
					if (y != LineNumberList.this.lastY) {
						LineNumberList.this.lastY = y;
						LineNumberList.this.currentLine = LineNumberList.this.textArea.getDocument()
								.getDefaultRootElement().getElementIndex(dot);
						LineNumberList.this.repaint(); // *Could* be optimized...
					}
				} catch (final BadLocationException ble) {
					ble.printStackTrace();
				}

		}

		public void install(final RTextArea textArea) {
			if (!this.installed) {
				// System.out.println("Installing");
				textArea.addCaretListener(this);
				textArea.addPropertyChangeListener(this);
				this.caretUpdate(null); // Force current line highlight repaint
				this.installed = true;
			}
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			final String name = e.getPropertyName();

			// If they change the current line highlight in any way...
			if (RTextAreaBase.HIGHLIGHT_CURRENT_LINE_PROPERTY.equals(name)
					|| RTextAreaBase.CURRENT_LINE_HIGHLIGHT_COLOR_PROPERTY.equals(name))
				LineNumberList.this.repaintLine(LineNumberList.this.currentLine);

		}

		public void uninstall(final RTextArea textArea) {
			if (this.installed) {
				// System.out.println("Uninstalling");
				textArea.removeCaretListener(this);
				textArea.removePropertyChangeListener(this);
				this.installed = false;
			}
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Map<?, ?> aaHints;
	private int ascent; // The ascent to use when painting line numbers.

	private int cellHeight; // Height of a line number "cell" when word wrap is off.
	private int cellWidth; // The width used for all line number cells.
	private int currentLine; // The last line the caret was on.

	/**
	 * Listens for events from the current text area.
	 */
	private Listener l;

	private int lastVisibleLine;// Last line index painted.

	private int lastY = -1; // Used to check if caret changes lines when line wrap is enabled.

	/**
	 * The index at which line numbering should start. The default value is
	 * <code>1</code>, but applications can change this if, for example, they are
	 * displaying a subset of lines in a file.
	 */
	private int lineNumberingStartIndex;

	private int mouseDragStartOffset;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on each
	 * paint.
	 */
	private Insets textAreaInsets;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on each
	 * paint.
	 */
	private Rectangle visibleRect;

	/**
	 * Constructs a new <code>LineNumberList</code> using default values for line
	 * number color (gray) and highlighting the current line.
	 *
	 * @param textArea
	 *            The text component for which line numbers will be displayed.
	 */
	public LineNumberList(final RTextArea textArea) {
		this(textArea, null);
	}

	/**
	 * Constructs a new <code>LineNumberList</code>.
	 *
	 * @param textArea
	 *            The text component for which line numbers will be displayed.
	 * @param numberColor
	 *            The color to use for the line numbers. If this is
	 *            <code>null</code>, gray will be used.
	 */
	public LineNumberList(final RTextArea textArea, final Color numberColor) {

		super(textArea);

		if (numberColor != null)
			this.setForeground(numberColor);
		else
			this.setForeground(Color.GRAY);

	}

	/**
	 * Overridden to set width of this component correctly when we are first
	 * displayed (as keying off of the RTextArea gives us (0,0) when it isn't yet
	 * displayed.
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		if (this.textArea != null)
			this.l.install(this.textArea); // Won't double-install
		this.updateCellWidths();
		this.updateCellHeights();
	}

	/**
	 * Calculates the last line number index painted in this component.
	 *
	 * @return The last line number index painted in this component.
	 */
	private int calculateLastVisibleLineNumber() {
		int lastLine = 0;
		if (this.textArea != null)
			lastLine = this.textArea.getLineCount() + this.getLineNumberingStartIndex() - 1;
		return lastLine;
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
	 * {@inheritDoc}
	 */
	@Override
	public Dimension getPreferredSize() {
		final int h = this.textArea != null ? this.textArea.getHeight() : 100; // Arbitrary
		return new Dimension(this.cellWidth, h);
	}

	/**
	 * Returns the width of the empty border on this component's right-hand side (or
	 * left-hand side, if the orientation is RTL).
	 *
	 * @return The border width.
	 */
	private int getRhsBorderWidth() {
		int w = 4;
		if (this.textArea instanceof RSyntaxTextArea)
			if (((RSyntaxTextArea) this.textArea).isCodeFoldingEnabled())
				w = 0;
		return w;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void handleDocumentEvent(final DocumentEvent e) {
		final int newLastLine = this.calculateLastVisibleLineNumber();
		if (newLastLine != this.lastVisibleLine) {
			// Adjust the amount of space the line numbers take up,
			// if necessary.
			if (newLastLine / 10 != this.lastVisibleLine / 10)
				this.updateCellWidths();
			this.lastVisibleLine = newLastLine;
			this.repaint();
		}
	}

	@Override
	protected void init() {

		super.init();

		// Initialize currentLine; otherwise, the current line won't start
		// off as highlighted.
		this.currentLine = 0;
		this.setLineNumberingStartIndex(1);

		this.visibleRect = new Rectangle(); // Must be initialized

		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		this.aaHints = RSyntaxUtilities.getDesktopAntiAliasHints();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void lineHeightsChanged() {
		this.updateCellHeights();
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
	}

	@Override
	public void mouseDragged(final MouseEvent e) {
		if (this.mouseDragStartOffset > -1) {
			final int pos = this.textArea.viewToModel(new Point(0, e.getY()));
			if (pos >= 0) { // Not -1
				this.textArea.setCaretPosition(this.mouseDragStartOffset);
				this.textArea.moveCaretPosition(pos);
			}
		}
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		if (this.textArea == null)
			return;
		if (e.getButton() == MouseEvent.BUTTON1) {
			final int pos = this.textArea.viewToModel(new Point(0, e.getY()));
			if (pos >= 0)
				this.textArea.setCaretPosition(pos);
			this.mouseDragStartOffset = pos;
		} else
			this.mouseDragStartOffset = -1;
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
	}

	/**
	 * Paints this component.
	 *
	 * @param g
	 *            The graphics context.
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		if (this.textArea == null)
			return;

		this.visibleRect = g.getClipBounds(this.visibleRect);
		if (this.visibleRect == null)
			this.visibleRect = this.getVisibleRect();
		// System.out.println("LineNumberList repainting: " + visibleRect);
		if (this.visibleRect == null)
			return;

		Color bg = this.getBackground();
		if (this.getGutter() != null)
			bg = this.getGutter().getBackground();
		g.setColor(bg);
		g.fillRect(0, this.visibleRect.y, this.cellWidth, this.visibleRect.height);
		g.setFont(this.getFont());
		if (this.aaHints != null)
			((Graphics2D) g).addRenderingHints(this.aaHints);

		if (this.textArea.getLineWrap()) {
			this.paintWrappedLineNumbers(g, this.visibleRect);
			return;
		}

		// Get where to start painting (top of the row), and where to paint
		// the line number (drawString expects y==baseline).
		// We need to be "scrolled up" just enough for the missing part of
		// the first line.
		this.textAreaInsets = this.textArea.getInsets(this.textAreaInsets);
		if (this.visibleRect.y < this.textAreaInsets.top) {
			this.visibleRect.height -= this.textAreaInsets.top - this.visibleRect.y;
			this.visibleRect.y = this.textAreaInsets.top;
		}
		int topLine = (this.visibleRect.y - this.textAreaInsets.top) / this.cellHeight;
		final int actualTopY = topLine * this.cellHeight + this.textAreaInsets.top;
		int y = actualTopY + this.ascent;

		// Get the actual first line to paint, taking into account folding.
		FoldManager fm = null;
		if (this.textArea instanceof RSyntaxTextArea) {
			fm = ((RSyntaxTextArea) this.textArea).getFoldManager();
			topLine += fm.getHiddenLineCountAbove(topLine, true);
		}
		final int rhsBorderWidth = this.getRhsBorderWidth();

		/*
		 * // Highlight the current line's line number, if desired. if
		 * (textArea.getHighlightCurrentLine() && currentLine>=topLine &&
		 * currentLine<=bottomLine) {
		 * g.setColor(textArea.getCurrentLineHighlightColor());
		 * g.fillRect(0,actualTopY+(currentLine-topLine)*cellHeight,
		 * cellWidth,cellHeight); }
		 */

		// Paint line numbers
		g.setColor(this.getForeground());
		final boolean ltr = this.getComponentOrientation().isLeftToRight();
		if (ltr) {
			final FontMetrics metrics = g.getFontMetrics();
			final int rhs = this.getWidth() - rhsBorderWidth;
			int line = topLine + 1;
			while (y < this.visibleRect.y + this.visibleRect.height + this.ascent
					&& line <= this.textArea.getLineCount()) {
				final String number = Integer.toString(line + this.getLineNumberingStartIndex() - 1);
				final int width = metrics.stringWidth(number);
				g.drawString(number, rhs - width, y);
				y += this.cellHeight;
				if (fm != null) {
					Fold fold = fm.getFoldForLine(line - 1);
					// Skip to next line to paint, taking extra care for lines with
					// block ends and begins together, e.g. "} else {"
					while (fold != null && fold.isCollapsed()) {
						final int hiddenLineCount = fold.getLineCount();
						if (hiddenLineCount == 0)
							// Fold parser identified a 0-line fold region... This
							// is really a bug, but we'll handle it gracefully.
							break;
						line += hiddenLineCount;
						fold = fm.getFoldForLine(line - 1);
					}
				}
				line++;
			}
		} else { // rtl
			int line = topLine + 1;
			while (y < this.visibleRect.y + this.visibleRect.height && line < this.textArea.getLineCount()) {
				final String number = Integer.toString(line + this.getLineNumberingStartIndex() - 1);
				g.drawString(number, rhsBorderWidth, y);
				y += this.cellHeight;
				if (fm != null) {
					Fold fold = fm.getFoldForLine(line - 1);
					// Skip to next line to paint, taking extra care for lines with
					// block ends and begins together, e.g. "} else {"
					while (fold != null && fold.isCollapsed()) {
						line += fold.getLineCount();
						fold = fm.getFoldForLine(line);
					}
				}
				line++;
			}
		}

	}

	/**
	 * Paints line numbers for text areas with line wrap enabled.
	 *
	 * @param g
	 *            The graphics context.
	 * @param visibleRect
	 *            The visible rectangle of these line numbers.
	 */
	private void paintWrappedLineNumbers(final Graphics g, final Rectangle visibleRect) {

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

		// Some variables we'll be using.
		final int width = this.getWidth();

		final RTextAreaUI ui = (RTextAreaUI) this.textArea.getUI();
		final View v = ui.getRootView(this.textArea).getView(0);
		// boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		final Document doc = this.textArea.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int lineCount = root.getElementCount();
		final int topPosition = this.textArea.viewToModel(new Point(visibleRect.x, visibleRect.y));
		int topLine = root.getElementIndex(topPosition);
		FoldManager fm = null;
		if (this.textArea instanceof RSyntaxTextArea)
			fm = ((RSyntaxTextArea) this.textArea).getFoldManager();

		// Compute the y at which to begin painting text, taking into account
		// that 1 logical line => at least 1 physical line, so it may be that
		// y<0. The computed y-value is the y-value of the top of the first
		// (possibly) partially-visible view.
		final Rectangle visibleEditorRect = ui.getVisibleEditorRect();
		Rectangle r = AbstractGutterComponent.getChildViewBounds(v, topLine, visibleEditorRect);
		int y = r.y;
		final int rhsBorderWidth = this.getRhsBorderWidth();
		int rhs;
		final boolean ltr = this.getComponentOrientation().isLeftToRight();
		if (ltr)
			rhs = width - rhsBorderWidth;
		else
			rhs = rhsBorderWidth;
		final int visibleBottom = visibleRect.y + visibleRect.height;
		final FontMetrics metrics = g.getFontMetrics();

		// Keep painting lines until our y-coordinate is past the visible
		// end of the text area.
		g.setColor(this.getForeground());

		while (y < visibleBottom) {

			r = AbstractGutterComponent.getChildViewBounds(v, topLine, visibleEditorRect);

			/*
			 * // Highlight the current line's line number, if desired. if
			 * (currentLineHighlighted && topLine==currentLine) {
			 * g.setColor(textArea.getCurrentLineHighlightColor()); g.fillRect(0,y,
			 * width,(r.y+r.height)-y); g.setColor(getForeground()); }
			 */

			// Paint the line number.
			final int index = topLine + 1 + this.getLineNumberingStartIndex() - 1;
			final String number = Integer.toString(index);
			if (ltr) {
				final int strWidth = metrics.stringWidth(number);
				g.drawString(number, rhs - strWidth, y + this.ascent);
			} else {
				final int x = rhsBorderWidth;
				g.drawString(number, x, y + this.ascent);
			}

			// The next possible y-coordinate is just after the last line
			// painted.
			y += r.height;

			// Update topLine (we're actually using it for our "current line"
			// variable now).
			if (fm != null) {
				final Fold fold = fm.getFoldForLine(topLine);
				if (fold != null && fold.isCollapsed())
					topLine += fold.getCollapsedLineCount();
			}
			topLine++;
			if (topLine >= lineCount)
				break;

		}

	}

	/**
	 * Called when this component is removed from the view hierarchy.
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		if (this.textArea != null)
			this.l.uninstall(this.textArea);
	}

	/**
	 * Repaints a single line in this list.
	 *
	 * @param line
	 *            The line to repaint.
	 */
	private void repaintLine(final int line) {
		int y = this.textArea.getInsets().top;
		y += line * this.cellHeight;
		this.repaint(0, y, this.cellWidth, this.cellHeight);
	}

	/**
	 * Overridden to ensure line number cell sizes are updated with the font size
	 * change.
	 *
	 * @param font
	 *            The new font to use for line numbers.
	 */
	@Override
	public void setFont(final Font font) {
		super.setFont(font);
		this.updateCellWidths();
		this.updateCellHeights();
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
			this.updateCellWidths();
			this.repaint();
		}
	}

	/**
	 * Sets the text area being displayed.
	 *
	 * @param textArea
	 *            The text area.
	 */
	@Override
	public void setTextArea(final RTextArea textArea) {

		if (this.l == null)
			this.l = new Listener();

		if (this.textArea != null)
			this.l.uninstall(textArea);

		super.setTextArea(textArea);
		this.lastVisibleLine = this.calculateLastVisibleLineNumber();

		if (textArea != null) {
			this.l.install(textArea); // Won't double-install
			this.updateCellHeights();
			this.updateCellWidths();
		}

	}

	/**
	 * Changes the height of the cells in the JList so that they are as tall as
	 * font. This function should be called whenever the user changes the Font of
	 * <code>textArea</code>.
	 */
	private void updateCellHeights() {
		if (this.textArea != null) {
			this.cellHeight = this.textArea.getLineHeight();
			this.ascent = this.textArea.getMaxAscent();
		} else {
			this.cellHeight = 20; // Arbitrary number.
			this.ascent = 5; // Also arbitrary
		}
		this.repaint();
	}

	/**
	 * Changes the width of the cells in the JList so you can see every digit of
	 * each.
	 */
	void updateCellWidths() {

		final int oldCellWidth = this.cellWidth;
		this.cellWidth = this.getRhsBorderWidth();

		// Adjust the amount of space the line numbers take up, if necessary.
		if (this.textArea != null) {
			final Font font = this.getFont();
			if (font != null) {
				final FontMetrics fontMetrics = this.getFontMetrics(font);
				int count = 0;
				int lineCount = this.textArea.getLineCount() + this.getLineNumberingStartIndex() - 1;
				do {
					lineCount = lineCount / 10;
					count++;
				} while (lineCount >= 10);
				this.cellWidth += fontMetrics.charWidth('9') * (count + 1) + 3;
			}
		}

		if (this.cellWidth != oldCellWidth)
			this.revalidate();

	}

}