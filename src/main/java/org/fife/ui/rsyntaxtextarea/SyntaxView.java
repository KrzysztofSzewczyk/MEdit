/*
 * 02/24/2004
 *
 * SyntaxView.java - The View object used by RSyntaxTextArea when word wrap is
 * disabled.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.TabExpander;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;

/**
 * The <code>javax.swing.text.View</code> object used by {@link RSyntaxTextArea}
 * when word wrap is disabled. It implements syntax highlighting for programming
 * languages using the colors and font styles specified by the
 * <code>RSyntaxTextArea</code>.
 * <p>
 *
 * You don't really have to do anything to use this class, as
 * {@link RSyntaxTextAreaUI} automatically sets the text area's view to be an
 * instance of this class if word wrap is disabled.
 * <p>
 *
 * The tokens that specify how to paint the syntax-highlighted text are gleaned
 * from the text area's {@link RSyntaxDocument}.
 *
 * @author Robert Futrell
 * @version 0.3
 */
public class SyntaxView extends View implements TabExpander, TokenOrientedView, RSTAView {

	private int ascent;

	private int clipEnd;

	private int clipStart;
	/**
	 * The default font used by the text area. If this changes we need to
	 * recalculate the longest line.
	 */
	private Font font;

	/**
	 * Cached for each paint() call so each drawLine() call has access to it.
	 */
	private RSyntaxTextArea host;
	/**
	 * Cached values to speed up the painting a tad.
	 */
	private int lineHeight = 0;

	/**
	 * The current longest line. This is used to calculate the preferred width of
	 * the view. Since the calculation is potentially expensive, we try to avoid it
	 * by stashing which line is currently the longest.
	 */
	private Element longLine;

	private float longLineWidth;
	/**
	 * Font metrics for the current font.
	 */
	private FontMetrics metrics;
	private int tabBase;
	private int tabSize;

	/**
	 * Temporary token used when we need to "modify" tokens for rendering purposes.
	 * Since tokens returned from RSyntaxDocuments are treated as immutable, we use
	 * this temporary token to do that work.
	 */
	private final TokenImpl tempToken;

	/**
	 * Constructs a new <code>SyntaxView</code> wrapped around an element.
	 *
	 * @param elem
	 *            The element representing the text to display.
	 */
	public SyntaxView(final Element elem) {
		super(elem);
		this.tempToken = new TokenImpl();
	}

	/**
	 * Iterate over the lines represented by the child elements of the element this
	 * view represents, looking for the line that is the longest. The
	 * <em>longLine</em> variable is updated to represent the longest line
	 * contained. The <em>font</em> variable is updated to indicate the font used to
	 * calculate the longest line.
	 */
	void calculateLongestLine() {
		final Component c = this.getContainer();
		this.font = c.getFont();
		this.metrics = c.getFontMetrics(this.font);
		this.tabSize = this.getTabSize() * this.metrics.charWidth(' ');
		final Element lines = this.getElement();
		final int n = lines.getElementCount();
		for (int i = 0; i < n; i++) {
			final Element line = lines.getElement(i);
			final float w = this.getLineWidth(i);
			if (w > this.longLineWidth) {
				this.longLineWidth = w;
				this.longLine = line;
			}
		}
	}

	/**
	 * Gives notification from the document that attributes were changed in a
	 * location that this view is responsible for.
	 *
	 * @param changes
	 *            the change information from the associated document
	 * @param a
	 *            the current allocation of the view
	 * @param f
	 *            the factory to use to rebuild if the view has children
	 * @see View#changedUpdate
	 */
	@Override
	public void changedUpdate(final DocumentEvent changes, final Shape a, final ViewFactory f) {
		this.updateDamage(changes, a, f);
	}

	/**
	 * Repaint the given line range.
	 *
	 * @param line0
	 *            The starting line number to repaint. This must be a valid line
	 *            number in the model.
	 * @param line1
	 *            The ending line number to repaint. This must be a valid line
	 *            number in the model.
	 * @param a
	 *            The region allocated for the view to render into.
	 * @param host
	 *            The component hosting the view (used to call repaint).
	 */
	protected void damageLineRange(final int line0, final int line1, final Shape a, final Component host) {
		if (a != null) {
			final Rectangle area0 = this.lineToRect(a, line0);
			final Rectangle area1 = this.lineToRect(a, line1);
			if (area0 != null && area1 != null) {
				final Rectangle dmg = area0.union(area1); // damage.
				host.repaint(dmg.x, dmg.y, dmg.width, dmg.height);
			} else
				host.repaint();
		}
	}

	/**
	 * Draws the passed-in text using syntax highlighting for the current language.
	 * It is assumed that the entire line is either not in a selected region, or
	 * painting with a selection-foreground color is turned off.
	 *
	 * @param painter
	 *            The painter to render the tokens.
	 * @param token
	 *            The list of tokens to draw.
	 * @param g
	 *            The graphics context in which to draw.
	 * @param x
	 *            The x-coordinate at which to draw.
	 * @param y
	 *            The y-coordinate at which to draw.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	private float drawLine(final TokenPainter painter, Token token, final Graphics2D g, final float x, final float y,
			final int line) {

		float nextX = x; // The x-value at the end of our text.
		final boolean paintBG = this.host.getPaintTokenBackgrounds(line, y);

		while (token != null && token.isPaintable() && nextX < this.clipEnd) {
			nextX = painter.paint(token, g, nextX, y, this.host, this, this.clipStart, paintBG);
			token = token.getNextToken();
		}

		// NOTE: We should re-use code from Token (paintBackground()) here,
		// but don't because I'm just too lazy.
		if (this.host.getEOLMarkersVisible()) {
			g.setColor(this.host.getForegroundForTokenType(TokenTypes.WHITESPACE));
			g.setFont(this.host.getFontForTokenType(TokenTypes.WHITESPACE));
			g.drawString("\u00B6", nextX, y);
		}

		// Return the x-coordinate at the end of the painted text.
		return nextX;

	}

	/**
	 * Draws the passed-in text using syntax highlighting for the current language.
	 * Tokens are checked for being in a selected region, and are rendered
	 * appropriately if they are.
	 *
	 * @param painter
	 *            The painter to render the tokens.
	 * @param token
	 *            The list of tokens to draw.
	 * @param g
	 *            The graphics context in which to draw.
	 * @param x
	 *            The x-coordinate at which to draw.
	 * @param y
	 *            The y-coordinate at which to draw.
	 * @param selStart
	 *            The start of the selection.
	 * @param selEnd
	 *            The end of the selection.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	private float drawLineWithSelection(final TokenPainter painter, Token token, final Graphics2D g, final float x,
			final float y, final int selStart, final int selEnd) {

		float nextX = x; // The x-value at the end of our text.
		final boolean useSTC = this.host.getUseSelectedTextColor();

		while (token != null && token.isPaintable() && nextX < this.clipEnd) {

			// Selection starts in this token
			if (token.containsPosition(selStart)) {

				if (selStart > token.getOffset()) {
					this.tempToken.copyFrom(token);
					this.tempToken.textCount = selStart - this.tempToken.getOffset();
					nextX = painter.paint(this.tempToken, g, nextX, y, this.host, this, this.clipStart);
					this.tempToken.textCount = token.length();
					this.tempToken.makeStartAt(selStart);
					// Clone required since token and tempToken must be
					// different tokens for else statement below
					token = new TokenImpl(this.tempToken);
				}

				final int tokenLen = token.length();
				final int selCount = Math.min(tokenLen, selEnd - token.getOffset());
				if (selCount == tokenLen)
					nextX = painter.paintSelected(token, g, nextX, y, this.host, this, this.clipStart, useSTC);
				else {
					this.tempToken.copyFrom(token);
					this.tempToken.textCount = selCount;
					nextX = painter.paintSelected(this.tempToken, g, nextX, y, this.host, this, this.clipStart, useSTC);
					this.tempToken.textCount = token.length();
					this.tempToken.makeStartAt(token.getOffset() + selCount);
					token = this.tempToken;
					nextX = painter.paint(token, g, nextX, y, this.host, this, this.clipStart);
				}

			}

			// Selection ends in this token
			else if (token.containsPosition(selEnd)) {
				this.tempToken.copyFrom(token);
				this.tempToken.textCount = selEnd - this.tempToken.getOffset();
				nextX = painter.paintSelected(this.tempToken, g, nextX, y, this.host, this, this.clipStart, useSTC);
				this.tempToken.textCount = token.length();
				this.tempToken.makeStartAt(selEnd);
				token = this.tempToken;
				nextX = painter.paint(token, g, nextX, y, this.host, this, this.clipStart);
			}

			// This token is entirely selected
			else if (token.getOffset() >= selStart && token.getEndOffset() <= selEnd)
				nextX = painter.paintSelected(token, g, nextX, y, this.host, this, this.clipStart, useSTC);
			else
				nextX = painter.paint(token, g, nextX, y, this.host, this, this.clipStart);

			token = token.getNextToken();

		}

		// NOTE: We should re-use code from Token (paintBackground()) here,
		// but don't because I'm just too lazy.
		if (this.host.getEOLMarkersVisible()) {
			g.setColor(this.host.getForegroundForTokenType(TokenTypes.WHITESPACE));
			g.setFont(this.host.getFontForTokenType(TokenTypes.WHITESPACE));
			g.drawString("\u00B6", nextX, y);
		}

		// Return the x-coordinate at the end of the painted text.
		return nextX;

	}

	/**
	 * Calculates the width of the line represented by the given element.
	 *
	 * @param line
	 *            The line for which to get the length.
	 * @param lineNumber
	 *            The line number of the specified line in the document.
	 * @return The width of the line.
	 */
	private float getLineWidth(final int lineNumber) {
		final Token tokenList = ((RSyntaxDocument) this.getDocument()).getTokenListForLine(lineNumber);
		return RSyntaxUtilities.getTokenListWidth(tokenList, (RSyntaxTextArea) this.getContainer(), this);
	}

	/**
	 * Provides a way to determine the next visually represented model location that
	 * one might place a caret. Some views may not be visible, they might not be in
	 * the same order found in the model, or they just might not allow access to
	 * some of the locations in the model.
	 *
	 * @param pos
	 *            the position to convert &gt;= 0
	 * @param a
	 *            the allocated region to render into
	 * @param direction
	 *            the direction from the current position that can be thought of as
	 *            the arrow keys typically found on a keyboard. This may be
	 *            SwingConstants.WEST, SwingConstants.EAST, SwingConstants.NORTH, or
	 *            SwingConstants.SOUTH.
	 * @return the location within the model that best represents the next location
	 *         visual position.
	 * @exception BadLocationException
	 * @exception IllegalArgumentException
	 *                for an invalid direction
	 */
	@Override
	public int getNextVisualPositionFrom(final int pos, final Position.Bias b, final Shape a, final int direction,
			final Position.Bias[] biasRet) throws BadLocationException {
		return RSyntaxUtilities.getNextVisualPositionFrom(pos, b, a, direction, biasRet, this);
	}

	/**
	 * Determines the preferred span for this view along an axis.
	 *
	 * @param axis
	 *            may be either View.X_AXIS or View.Y_AXIS
	 * @return the span the view would like to be rendered into &gt;= 0. Typically
	 *         the view is told to render into the span that is returned, although
	 *         there is no guarantee. The parent may choose to resize or break the
	 *         view.
	 * @exception IllegalArgumentException
	 *                for an invalid axis
	 */
	@Override
	public float getPreferredSpan(final int axis) {
		this.updateMetrics();
		switch (axis) {
		case View.X_AXIS:
			float span = this.longLineWidth + this.getRhsCorrection(); // fudge factor
			if (this.host.getEOLMarkersVisible())
				span += this.metrics.charWidth('\u00B6');
			return span;
		case View.Y_AXIS:
			// We update lineHeight here as when this method is first
			// called, lineHeight isn't initialized. If we don't do it
			// here, we get no vertical scrollbar (as lineHeight==0).
			this.lineHeight = this.host != null ? this.host.getLineHeight() : this.lineHeight;
			// return getElement().getElementCount() * lineHeight;
			int visibleLineCount = this.getElement().getElementCount();
			if (this.host.isCodeFoldingEnabled())
				visibleLineCount -= this.host.getFoldManager().getHiddenLineCount();
			return visibleLineCount * (float) this.lineHeight;
		default:
			throw new IllegalArgumentException("Invalid axis: " + axis);
		}
	}

	/**
	 * Workaround for JTextComponents allowing the caret to be rendered entirely
	 * off-screen if the entire "previous" character fit entirely.
	 *
	 * @return The amount of space to add to the x-axis preferred span.
	 */
	private int getRhsCorrection() {
		int rhsCorrection = 10;
		if (this.host != null)
			rhsCorrection = this.host.getRightHandSideCorrection();
		return rhsCorrection;
	}

	/**
	 * Returns the tab size set for the document, defaulting to 5.
	 *
	 * @return The tab size.
	 */
	private int getTabSize() {
		final Integer i = (Integer) this.getDocument().getProperty(PlainDocument.tabSizeAttribute);
		final int size = i != null ? i.intValue() : 5;
		return size;
	}

	/**
	 * Returns a token list for the <i>physical</i> line above the physical line
	 * containing the specified offset into the document. Note that for this plain
	 * (non-wrapped) view, this is simply the token list for the logical line above
	 * the line containing <code>offset</code>, since lines are not wrapped.
	 *
	 * @param offset
	 *            The offset in question.
	 * @return A token list for the physical (and in this view, logical) line before
	 *         this one. If <code>offset</code> is in the first line in the
	 *         document, <code>null</code> is returned.
	 */
	@Override
	public Token getTokenListForPhysicalLineAbove(final int offset) {
		final RSyntaxDocument document = (RSyntaxDocument) this.getDocument();
		final Element map = document.getDefaultRootElement();
		int line = map.getElementIndex(offset);
		final FoldManager fm = this.host.getFoldManager();
		if (fm == null) {
			line--;
			if (line >= 0)
				return document.getTokenListForLine(line);
		} else {
			line = fm.getVisibleLineAbove(line);
			if (line >= 0)
				return document.getTokenListForLine(line);
		}
		// int line = map.getElementIndex(offset) - 1;
		// if (line>=0)
		// return document.getTokenListForLine(line);
		return null;
	}

	/**
	 * Returns a token list for the <i>physical</i> line below the physical line
	 * containing the specified offset into the document. Note that for this plain
	 * (non-wrapped) view, this is simply the token list for the logical line below
	 * the line containing <code>offset</code>, since lines are not wrapped.
	 *
	 * @param offset
	 *            The offset in question.
	 * @return A token list for the physical (and in this view, logical) line after
	 *         this one. If <code>offset</code> is in the last physical line in the
	 *         document, <code>null</code> is returned.
	 */
	@Override
	public Token getTokenListForPhysicalLineBelow(final int offset) {
		final RSyntaxDocument document = (RSyntaxDocument) this.getDocument();
		final Element map = document.getDefaultRootElement();
		final int lineCount = map.getElementCount();
		int line = map.getElementIndex(offset);
		if (!this.host.isCodeFoldingEnabled()) {
			if (line < lineCount - 1)
				return document.getTokenListForLine(line + 1);
		} else {
			final FoldManager fm = this.host.getFoldManager();
			line = fm.getVisibleLineBelow(line);
			if (line >= 0 && line < lineCount)
				return document.getTokenListForLine(line);
		}
		// int line = map.getElementIndex(offset);
		// int lineCount = map.getElementCount();
		// if (line<lineCount-1)
		// return document.getTokenListForLine(line+1);
		return null;
	}

	/**
	 * Gives notification that something was inserted into the document in a
	 * location that this view is responsible for.
	 *
	 * @param changes
	 *            The change information from the associated document.
	 * @param a
	 *            The current allocation of the view.
	 * @param f
	 *            The factory to use to rebuild if the view has children.
	 */
	@Override
	public void insertUpdate(final DocumentEvent changes, final Shape a, final ViewFactory f) {
		this.updateDamage(changes, a, f);
	}

	/**
	 * Determine the rectangle that represents the given line.
	 *
	 * @param a
	 *            The region allocated for the view to render into
	 * @param line
	 *            The line number to find the region of. This must be a valid line
	 *            number in the model.
	 */
	protected Rectangle lineToRect(final Shape a, int line) {
		Rectangle r = null;
		this.updateMetrics();
		if (this.metrics != null) {
			final Rectangle alloc = a.getBounds();
			// NOTE: lineHeight is not initially set here, leading to the
			// current line not being highlighted when a document is first
			// opened. So, we set it here just in case.
			this.lineHeight = this.host != null ? this.host.getLineHeight() : this.lineHeight;
			if (this.host != null && this.host.isCodeFoldingEnabled()) {
				final FoldManager fm = this.host.getFoldManager();
				final int hiddenCount = fm.getHiddenLineCountAbove(line);
				line -= hiddenCount;
			}
			r = new Rectangle(alloc.x, alloc.y + line * this.lineHeight, alloc.width, this.lineHeight);
		}
		return r;
	}

	/**
	 * Provides a mapping, for a given region, from the document model coordinate
	 * space to the view coordinate space. The specified region is created as a
	 * union of the first and last character positions.
	 * <p>
	 *
	 * This is implemented to subtract the width of the second character, as this
	 * view's <code>modelToView</code> actually returns the width of the character
	 * instead of "1" or "0" like the View implementations in
	 * <code>javax.swing.text</code>. Thus, if we don't override this method, the
	 * <code>View</code> implementation will return one character's width too much
	 * for its consumers (implementations of
	 * <code>javax.swing.text.Highlighter</code>).
	 *
	 * @param p0
	 *            the position of the first character (&gt;=0)
	 * @param b0
	 *            The bias of the first character position, toward the previous
	 *            character or the next character represented by the offset, in case
	 *            the position is a boundary of two views; <code>b0</code> will have
	 *            one of these values:
	 *            <ul>
	 *            <li><code>Position.Bias.Forward</code>
	 *            <li><code>Position.Bias.Backward</code>
	 *            </ul>
	 * @param p1
	 *            the position of the last character (&gt;=0)
	 * @param b1
	 *            the bias for the second character position, defined one of the
	 *            legal values shown above
	 * @param a
	 *            the area of the view, which encompasses the requested region
	 * @return the bounding box which is a union of the region specified by the
	 *         first and last character positions
	 * @exception BadLocationException
	 *                if the given position does not represent a valid location in
	 *                the associated document
	 * @exception IllegalArgumentException
	 *                if <code>b0</code> or <code>b1</code> are not one of the legal
	 *                <code>Position.Bias</code> values listed above
	 * @see View#viewToModel
	 */
	@Override
	public Shape modelToView(final int p0, final Position.Bias b0, final int p1, final Position.Bias b1, final Shape a)
			throws BadLocationException {

		final Shape s0 = this.modelToView(p0, a, b0);
		Shape s1;
		if (p1 == this.getEndOffset()) {
			try {
				s1 = this.modelToView(p1, a, b1);
			} catch (final BadLocationException ble) {
				s1 = null;
			}
			if (s1 == null) {
				// Assume extends left to right.
				final Rectangle alloc = a instanceof Rectangle ? (Rectangle) a : a.getBounds();
				s1 = new Rectangle(alloc.x + alloc.width - 1, alloc.y, 1, alloc.height);
			}
		} else
			s1 = this.modelToView(p1, a, b1);
		final Rectangle r0 = s0 instanceof Rectangle ? (Rectangle) s0 : s0.getBounds();
		final Rectangle r1 = s1 instanceof Rectangle ? (Rectangle) s1 : s1.getBounds();
		if (r0.y != r1.y) {
			// If it spans lines, force it to be the width of the view.
			final Rectangle alloc = a instanceof Rectangle ? (Rectangle) a : a.getBounds();
			r0.x = alloc.x;
			r0.width = alloc.width;
		}

		r0.add(r1);
		// The next line is the only difference between this method and
		// View's implementation. We're subtracting the width of the second
		// character. This is because this method is used by Highlighter
		// implementations to get the area to "highlight", and if we don't do
		// this, one character too many is highlighted thanks to our
		// modelToView() implementation returning the actual width of the
		// character requested!
		if (p1 > p0)
			r0.width -= r1.width;

		return r0;

	}

	/**
	 * Provides a mapping from the document model coordinate space to the coordinate
	 * space of the view mapped to it.
	 *
	 * @param pos
	 *            the position to convert &gt;= 0
	 * @param a
	 *            the allocated region to render into
	 * @return the bounding box of the given position
	 * @exception BadLocationException
	 *                if the given position does not represent a valid location in
	 *                the associated document
	 * @see View#modelToView
	 */
	@Override
	public Shape modelToView(final int pos, final Shape a, final Position.Bias b) throws BadLocationException {

		// line coordinates
		final Element map = this.getElement();
		final RSyntaxDocument doc = (RSyntaxDocument) this.getDocument();
		final int lineIndex = map.getElementIndex(pos);
		final Token tokenList = doc.getTokenListForLine(lineIndex);
		Rectangle lineArea = this.lineToRect(a, lineIndex);
		this.tabBase = lineArea.x; // Used by listOffsetToView().

		// int x = (int)RSyntaxUtilities.getTokenListWidthUpTo(tokenList,
		// (RSyntaxTextArea)getContainer(),
		// this, 0, pos);
		// We use this method instead as it returns the actual bounding box,
		// not just the x-coordinate.
		lineArea = tokenList.listOffsetToView((RSyntaxTextArea) this.getContainer(), this, pos, this.tabBase, lineArea);

		return lineArea;

	}

	/**
	 * Returns the next tab stop position after a given reference position. This
	 * implementation does not support things like centering so it ignores the
	 * tabOffset argument.
	 *
	 * @param x
	 *            the current position &gt;= 0
	 * @param tabOffset
	 *            the position within the text stream that the tab occurred at &gt;=
	 *            0.
	 * @return the tab stop, measured in points &gt;= 0
	 */
	@Override
	public float nextTabStop(final float x, final int tabOffset) {
		if (this.tabSize == 0)
			return x;
		final int ntabs = ((int) x - this.tabBase) / this.tabSize;
		return this.tabBase + (ntabs + 1f) * this.tabSize;
	}

	/**
	 * Actually paints the text area. Only lines that have been damaged are
	 * repainted.
	 *
	 * @param g
	 *            The graphics context with which to paint.
	 * @param a
	 *            The allocated region in which to render.
	 */
	@Override
	public void paint(final Graphics g, final Shape a) {

		final RSyntaxDocument document = (RSyntaxDocument) this.getDocument();

		final Rectangle alloc = a.getBounds();

		this.tabBase = alloc.x;
		this.host = (RSyntaxTextArea) this.getContainer();

		final Rectangle clip = g.getClipBounds();
		// An attempt to speed things up for files with long lines. Note that
		// this will actually slow things down a bit for the common case of
		// regular-length lines, but it doesn't make a perceivable difference.
		this.clipStart = clip.x;
		this.clipEnd = this.clipStart + clip.width;

		this.lineHeight = this.host.getLineHeight();
		this.ascent = this.host.getMaxAscent();// metrics.getAscent();
		final int heightAbove = clip.y - alloc.y;
		int linesAbove = Math.max(0, heightAbove / this.lineHeight);

		final FoldManager fm = this.host.getFoldManager();
		linesAbove += fm.getHiddenLineCountAbove(linesAbove, true);
		final Rectangle lineArea = this.lineToRect(a, linesAbove);
		int y = lineArea.y + this.ascent;
		final int x = lineArea.x;
		final Element map = this.getElement();
		final int lineCount = map.getElementCount();

		// Whether token styles should always be painted, even in selections
		final int selStart = this.host.getSelectionStart();
		final int selEnd = this.host.getSelectionEnd();

		final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.host.getHighlighter();

		final Graphics2D g2d = (Graphics2D) g;
		Token token;
		// System.err.println("Painting lines: " + linesAbove + " to " + (endLine-1));

		final TokenPainter painter = this.host.getTokenPainter();
		int line = linesAbove;
		// int count = 0;
		while (y < clip.y + clip.height + this.ascent && line < lineCount) {

			Fold fold = fm.getFoldForLine(line);
			final Element lineElement = map.getElement(line);
			final int startOffset = lineElement.getStartOffset();
			// int endOffset = (line==lineCount ? lineElement.getEndOffset()-1 :
			// lineElement.getEndOffset()-1);
			final int endOffset = lineElement.getEndOffset() - 1; // Why always "-1"?
			h.paintLayeredHighlights(g2d, startOffset, endOffset, a, this.host, this);

			// Paint a line of text.
			token = document.getTokenListForLine(line);
			if (selStart == selEnd || startOffset >= selEnd || endOffset < selStart)
				this.drawLine(painter, token, g2d, x, y, line);
			else
				// System.out.println("Drawing line with selection: " + line);
				this.drawLineWithSelection(painter, token, g2d, x, y, selStart, selEnd);

			if (fold != null && fold.isCollapsed()) {

				// Visible indicator of collapsed lines
				final Color c = RSyntaxUtilities.getFoldedLineBottomColor(this.host);
				if (c != null) {
					g.setColor(c);
					g.drawLine(x, y + this.lineHeight - this.ascent - 1, this.host.getWidth(),
							y + this.lineHeight - this.ascent - 1);
				}

				// Skip to next line to paint, taking extra care for lines with
				// block ends and begins together, e.g. "} else {"
				do {
					final int hiddenLineCount = fold.getLineCount();
					if (hiddenLineCount == 0)
						// Fold parser identified a zero-line fold region.
						// This is really a bug, but we'll be graceful here
						// and avoid an infinite loop.
						break;
					line += hiddenLineCount;
					fold = fm.getFoldForLine(line);
				} while (fold != null && fold.isCollapsed());

			}

			y += this.lineHeight;
			line++;
			// count++;

		}

		// System.out.println("SyntaxView: lines painted=" + count);

	}

	/**
	 * If the passed-in line is longer than the current longest line, then the
	 * longest line is updated.
	 *
	 * @param line
	 *            The line to test against the current longest.
	 * @param lineNumber
	 *            The line number of the passed-in line.
	 * @return <code>true</code> iff the current longest line was updated.
	 */
	private boolean possiblyUpdateLongLine(final Element line, final int lineNumber) {
		final float w = this.getLineWidth(lineNumber);
		if (w > this.longLineWidth) {
			this.longLineWidth = w;
			this.longLine = line;
			return true;
		}
		return false;
	}

	/**
	 * Gives notification that something was removed from the document in a location
	 * that this view is responsible for.
	 *
	 * @param changes
	 *            the change information from the associated document
	 * @param a
	 *            the current allocation of the view
	 * @param f
	 *            the factory to use to rebuild if the view has children
	 */
	@Override
	public void removeUpdate(final DocumentEvent changes, final Shape a, final ViewFactory f) {
		this.updateDamage(changes, a, f);
	}

	@Override
	public void setSize(final float width, final float height) {
		super.setSize(width, height);
		this.updateMetrics();
	}

	/**
	 * Repaint the region of change covered by the given document event. Damages the
	 * line that begins the range to cover the case when the insert/remove is only
	 * on one line. If lines are added or removed, damages the whole view. The
	 * longest line is checked to see if it has changed.
	 */
	protected void updateDamage(final DocumentEvent changes, final Shape a, final ViewFactory f) {
		final Component host = this.getContainer();
		this.updateMetrics();
		final Element elem = this.getElement();
		final DocumentEvent.ElementChange ec = changes.getChange(elem);
		final Element[] added = ec != null ? ec.getChildrenAdded() : null;
		final Element[] removed = ec != null ? ec.getChildrenRemoved() : null;
		if (added != null && added.length > 0 || removed != null && removed.length > 0) {
			// lines were added or removed...
			if (added != null) {
				final int addedAt = ec.getIndex(); // FIXME: Is this correct?????
				for (int i = 0; i < added.length; i++)
					this.possiblyUpdateLongLine(added[i], addedAt + i);
			}
			if (removed != null)
				for (final Element element : removed)
					if (element == this.longLine) {
						this.longLineWidth = -1; // Must do this!!
						this.calculateLongestLine();
						break;
					}
			this.preferenceChanged(null, true, true);
			host.repaint();
		}

		// This occurs when syntax highlighting only changes on lines
		// (i.e. beginning a multiline comment).
		else if (changes.getType() == DocumentEvent.EventType.CHANGE) {
			// System.err.println("Updating the damage due to a CHANGE event...");
			final int startLine = changes.getOffset();
			final int endLine = changes.getLength();
			this.damageLineRange(startLine, endLine, a, host);
		}

		else {
			final Element map = this.getElement();
			final int line = map.getElementIndex(changes.getOffset());
			this.damageLineRange(line, line, a, host);
			if (changes.getType() == DocumentEvent.EventType.INSERT) {
				// check to see if the line is longer than current
				// longest line.
				final Element e = map.getElement(line);
				if (e == this.longLine) {
					// We must recalculate longest line's width here
					// because it has gotten longer.
					this.longLineWidth = this.getLineWidth(line);
					this.preferenceChanged(null, true, false);
				} else // If long line gets updated, update the status bars too.
				if (this.possiblyUpdateLongLine(e, line))
					this.preferenceChanged(null, true, false);
			} else if (changes.getType() == DocumentEvent.EventType.REMOVE)
				if (map.getElement(line) == this.longLine) {
					// removed from longest line... recalc
					this.longLineWidth = -1; // Must do this!
					this.calculateLongestLine();
					this.preferenceChanged(null, true, false);
				}
		}
	}

	/**
	 * Checks to see if the font metrics and longest line are up-to-date.
	 */
	private void updateMetrics() {
		this.host = (RSyntaxTextArea) this.getContainer();
		final Font f = this.host.getFont();
		if (this.font != f)
			// The font changed, we need to recalculate the longest line!
			// This also updates cached font and tab size.
			this.calculateLongestLine();
	}

	/**
	 * Provides a mapping from the view coordinate space to the logical coordinate
	 * space of the model.
	 *
	 * @param fx
	 *            the X coordinate &gt;= 0
	 * @param fy
	 *            the Y coordinate &gt;= 0
	 * @param a
	 *            the allocated region to render into
	 * @return the location within the model that best represents the given point in
	 *         the view &gt;= 0
	 */
	@Override
	public int viewToModel(final float fx, final float fy, final Shape a, final Position.Bias[] bias) {

		bias[0] = Position.Bias.Forward;

		final Rectangle alloc = a.getBounds();
		final RSyntaxDocument doc = (RSyntaxDocument) this.getDocument();
		final int x = (int) fx;
		final int y = (int) fy;

		// If they're asking about a view position above the area covered by
		// this view, then the position is assumed to be the starting position
		// of this view.
		if (y < alloc.y)
			return this.getStartOffset();
		else if (y > alloc.y + alloc.height)
			return this.host.getLastVisibleOffset();
		else {

			final Element map = doc.getDefaultRootElement();
			int lineIndex = Math.abs((y - alloc.y) / this.lineHeight);// metrics.getHeight() );
			final FoldManager fm = this.host.getFoldManager();
			// System.out.print("--- " + lineIndex);
			lineIndex += fm.getHiddenLineCountAbove(lineIndex, true);
			// System.out.println(" => " + lineIndex);
			if (lineIndex >= map.getElementCount())
				return this.host.getLastVisibleOffset();

			final Element line = map.getElement(lineIndex);

			// If the point is to the left of the line...
			if (x < alloc.x)
				return line.getStartOffset();
			else if (x > alloc.x + alloc.width)
				return line.getEndOffset() - 1;
			else {
				// Determine the offset into the text
				final int p0 = line.getStartOffset();
				final Token tokenList = doc.getTokenListForLine(lineIndex);
				this.tabBase = alloc.x;
				final int offs = tokenList.getListOffset((RSyntaxTextArea) this.getContainer(), this, this.tabBase, x);
				return offs != -1 ? offs : p0;
			}

		} // End of else.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int yForLine(final Rectangle alloc, int line) throws BadLocationException {

		// Rectangle lineArea = lineToRect(alloc, lineIndex);
		this.updateMetrics();
		if (this.metrics != null) {
			// NOTE: lineHeight is not initially set here, leading to the
			// current line not being highlighted when a document is first
			// opened. So, we set it here just in case.
			this.lineHeight = this.host != null ? this.host.getLineHeight() : this.lineHeight;
			if (this.host != null) {
				final FoldManager fm = this.host.getFoldManager();
				if (!fm.isLineHidden(line)) {
					line -= fm.getHiddenLineCountAbove(line);
					return alloc.y + line * this.lineHeight;
				}
			}
		}

		return -1;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int yForLineContaining(final Rectangle alloc, final int offs) throws BadLocationException {
		final Element map = this.getElement();
		final int line = map.getElementIndex(offs);
		return this.yForLine(alloc, line);
	}

}