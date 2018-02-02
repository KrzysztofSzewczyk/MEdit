/*
 * 11/10/2004
 *
 * ChangableHighlightPainter.java - A highlight painter whose color you can
 * change.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * An extension of <code>LayerPainter</code> that allows the user to change
 * several of its properties:
 *
 * <ul>
 * <li>Its color/fill style (can use a <code>GradientPaint</code>, for
 * example).</li>
 * <li>Whether the edges of a painted highlight are rounded.</li>
 * <li>Whether painted highlights have translucency.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 0.6
 */
public class ChangeableHighlightPainter extends LayeredHighlighter.LayerPainter implements Serializable {

	private static final int ARCHEIGHT = 8;

	private static final int ARCWIDTH = 8;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The alpha value used in computing translucency. This should stay in the range
	 * <code>0.0f</code> (completely invisible) to <code>1.0f</code> (completely
	 * opaque).
	 */
	private float alpha;

	/**
	 * The alpha composite used to render with translucency.
	 */
	private transient AlphaComposite alphaComposite;

	/**
	 * The <code>Paint</code>/<code>Color</code> of this highlight.
	 */
	private Paint paint;
	/**
	 * Whether selections have rounded edges.
	 */
	private boolean roundedEdges;

	/**
	 * Creates a new <code>ChangableHighlightPainter</code> that paints highlights
	 * with the text area's selection color (i.e., behaves exactly like
	 * <code>javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
	 * </code>).
	 */
	public ChangeableHighlightPainter() {
		this(null);
	}

	/**
	 * Creates a new highlight painter using the specified <code>Paint</code>
	 * without rounded edges.
	 *
	 * @param paint
	 *            The <code>Paint</code> (usually a <code>java.awt.Color</code>)
	 *            with which to paint the highlights.
	 */
	public ChangeableHighlightPainter(final Paint paint) {
		this(paint, false);
	}

	/**
	 * Creates a new highlight painter.
	 *
	 * @param paint
	 *            The <code>Paint</code> (usually a <code>java.awt.Color</code>)
	 *            with which to paint the highlights.
	 * @param rounded
	 *            Whether to use rounded edges on the highlights.
	 */
	public ChangeableHighlightPainter(final Paint paint, final boolean rounded) {
		this(paint, rounded, 1.0f);
	}

	/**
	 * Creates a new highlight painter.
	 *
	 * @param paint
	 *            The <code>Paint</code> (usually a <code>java.awt.Color</code>)
	 *            with which to paint the highlights.
	 * @param rounded
	 *            Whether to use rounded edges on the highlights.
	 * @param alpha
	 *            The alpha value to use when painting highlights. This value should
	 *            be in the range <code>0.0f</code> (completely transparent) through
	 *            <code>1.0f</code> (opaque).
	 */
	public ChangeableHighlightPainter(final Paint paint, final boolean rounded, final float alpha) {
		this.setPaint(paint);
		this.setRoundedEdges(rounded);
		this.setAlpha(alpha);
	}

	/**
	 * Returns the alpha value used in computing the translucency of these
	 * highlights. A value of <code>1.0f</code> (the default) means that no
	 * translucency is used; there is no performance hit for this value. For all
	 * other values (<code>[0.0f..1.0f)</code>), there will be a performance hit.
	 *
	 * @return The alpha value.
	 * @see #setAlpha
	 */
	public float getAlpha() {
		return this.alpha;
	}

	/**
	 * Returns the alpha composite to use when rendering highlights with this
	 * painter.
	 *
	 * @return The alpha composite.
	 */
	private AlphaComposite getAlphaComposite() {
		if (this.alphaComposite == null)
			this.alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.alpha);
		return this.alphaComposite;
	}

	/**
	 * Returns the <code>Paint</code> (usually a <code>java.awt.Color</code>) being
	 * used to paint highlights.
	 *
	 * @return The <code>Paint</code>.
	 * @see #setPaint
	 */
	public Paint getPaint() {
		return this.paint;
	}

	/**
	 * Returns whether rounded edges are used when painting selections with this
	 * highlight painter.
	 *
	 * @return Whether rounded edges are used.
	 * @see #setRoundedEdges
	 */
	public boolean getRoundedEdges() {
		return this.roundedEdges;
	}

	/**
	 * Paints a highlight.
	 *
	 * @param g
	 *            the graphics context
	 * @param offs0
	 *            the starting model offset &gt;= 0
	 * @param offs1
	 *            the ending model offset &gt;= offs1
	 * @param bounds
	 *            the bounding box for the highlight
	 * @param c
	 *            the editor
	 */
	@Override
	public void paint(final Graphics g, final int offs0, final int offs1, final Shape bounds, final JTextComponent c) {

		final Rectangle alloc = bounds.getBounds();

		// Set up translucency if necessary.
		final Graphics2D g2d = (Graphics2D) g;
		Composite originalComposite = null;
		if (this.getAlpha() < 1.0f) {
			originalComposite = g2d.getComposite();
			g2d.setComposite(this.getAlphaComposite());
		}

		try {

			// Determine locations.
			final TextUI mapper = c.getUI();
			final Rectangle p0 = mapper.modelToView(c, offs0);
			final Rectangle p1 = mapper.modelToView(c, offs1);
			final Paint paint = this.getPaint();
			if (paint == null)
				g2d.setColor(c.getSelectionColor());
			else
				g2d.setPaint(paint);

			// Entire highlight is on one line.
			if (p0.y == p1.y) {
				// Standard Swing views return 0 width for chars, but ours
				// returns the char's width. Set this to 0 here since p1 is
				// technically an exclusive boundary.
				p1.width = 0;
				final Rectangle r = p0.union(p1);
				g2d.fillRect(r.x, r.y, r.width, r.height);
			}

			// Highlight spans lines.
			else {
				final int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
				g2d.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
				if (p0.y + p0.height != p1.y)
					g2d.fillRect(alloc.x, p0.y + p0.height, alloc.width, p1.y - (p0.y + p0.height));
				g2d.fillRect(alloc.x, p1.y, p1.x - alloc.x, p1.height);
			}

		} catch (final BadLocationException e) {
			// Never happens.
			e.printStackTrace();
		} finally {
			// Restore state from before translucency if necessary.
			if (this.getAlpha() < 1.0f)
				g2d.setComposite(originalComposite);
		}

	}

	/**
	 * Paints a portion of a highlight.
	 *
	 * @param g
	 *            the graphics context
	 * @param offs0
	 *            the starting model offset &gt;= 0
	 * @param offs1
	 *            the ending model offset &gt;= offs1
	 * @param bounds
	 *            the bounding box of the view, which is not necessarily the region
	 *            to paint.
	 * @param c
	 *            the editor
	 * @param view
	 *            View painting for
	 * @return region drawing occurred in
	 */
	@Override
	public Shape paintLayer(final Graphics g, final int offs0, final int offs1, final Shape bounds,
			final JTextComponent c, final View view) {

		// Set up translucency if necessary.
		final Graphics2D g2d = (Graphics2D) g;
		Composite originalComposite = null;
		if (this.getAlpha() < 1.0f) {
			originalComposite = g2d.getComposite();
			g2d.setComposite(this.getAlphaComposite());
		}

		// Set the color (our own if defined, otherwise text area's).
		final Paint paint = this.getPaint();
		if (paint == null)
			g2d.setColor(c.getSelectionColor());
		else
			g2d.setPaint(paint);

		// This special case isn't needed for most standard Swing Views (which
		// always return a width of 1 for modelToView() calls), but it is
		// needed for RSTA views, which actually return the width of chars for
		// modelToView calls. But this should be faster anyway, as we
		// short-circuit and do only one modelToView() for one offset.
		if (offs0 == offs1)
			try {
				final Shape s = view.modelToView(offs0, bounds, Position.Bias.Forward);
				final Rectangle r = s.getBounds();
				g.drawLine(r.x, r.y, r.x, r.y + r.height);
				return r;
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
				return null;
			}

		// Contained in view, can just use bounds.
		if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {

			Rectangle alloc;
			if (bounds instanceof Rectangle)
				alloc = (Rectangle) bounds;
			else
				alloc = bounds.getBounds();

			g2d.fillRect(alloc.x, alloc.y, alloc.width, alloc.height);

			// Restore state from before translucency if necessary.
			if (this.getAlpha() < 1.0f)
				g2d.setComposite(originalComposite);

			return alloc;

		} else
			try {

				final Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward,
						bounds);
				final Rectangle r = shape instanceof Rectangle ? (Rectangle) shape : shape.getBounds();
				if (this.roundedEdges)
					g2d.fillRoundRect(r.x, r.y, r.width, r.height, ChangeableHighlightPainter.ARCWIDTH,
							ChangeableHighlightPainter.ARCHEIGHT);
				else
					g2d.fillRect(r.x, r.y, r.width, r.height);

				// Restore state from before translucency if necessary.
				if (this.getAlpha() < 1.0f)
					g2d.setComposite(originalComposite);

				return r;

			} catch (final BadLocationException ble) {
				ble.printStackTrace();
			} finally {
				// Restore state from before translucency if necessary.
				if (this.getAlpha() < 1.0f)
					g2d.setComposite(originalComposite);
			}

		// Only if exception
		return null;

	}

	/**
	 * Deserializes a painter.
	 *
	 * @param s
	 *            The stream to read from.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(final ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		// We cheat and always serialize the Paint as a Color. "-1" means
		// no Paint (i.e. use system selection color when painting).
		final int rgb = s.readInt();
		this.paint = rgb == -1 ? null : new Color(rgb);
		this.alphaComposite = null; // Keep FindBugs happy. This will get set later
	}

	/**
	 * Sets the alpha value used in rendering highlights. If this value is
	 * <code>1.0f</code> (the default), the highlights are rendered completely
	 * opaque. This behavior matches that of <code>DefaultHighlightPainter</code>
	 * and imposes no performance hit. If this value is below <code>1.0f</code>, it
	 * represents how opaque the highlight will be. There will be a small
	 * performance hit for values less than <code>1.0f</code>.
	 *
	 * @param alpha
	 *            The new alpha value to use for transparency.
	 * @see #getAlpha
	 */
	public void setAlpha(final float alpha) {
		this.alpha = alpha;
		this.alpha = Math.max(alpha, 0.0f);
		this.alpha = Math.min(1.0f, alpha);
		this.alphaComposite = null; // So it is recreated with new alpha.
	}

	/**
	 * Sets the <code>Paint</code> (usually a <code>java.awt.Color</code>) used to
	 * paint this highlight.
	 *
	 * @param paint
	 *            The new <code>Paint</code>.
	 * @see #getPaint
	 */
	public void setPaint(final Paint paint) {
		this.paint = paint;
	}

	/**
	 * Sets whether rounded edges are used when painting this highlight.
	 *
	 * @param rounded
	 *            Whether rounded edges should be used.
	 * @see #getRoundedEdges
	 */
	public void setRoundedEdges(final boolean rounded) {
		this.roundedEdges = rounded;
	}

	/**
	 * Serializes this painter.
	 *
	 * @param s
	 *            The stream to write to.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private void writeObject(final ObjectOutputStream s) throws IOException {
		s.defaultWriteObject();
		int rgb = -1; // No Paint -> Use JTextComponent's selection color
		if (this.paint != null) {
			// NOTE: We cheat and always serialize the Paint as a Color.
			// This is (practically) always the case anyway.
			final Color c = this.paint instanceof Color ? (Color) this.paint : SystemColor.textHighlight;
			rgb = c.getRGB();
		}
		s.writeInt(rgb);
	}

}