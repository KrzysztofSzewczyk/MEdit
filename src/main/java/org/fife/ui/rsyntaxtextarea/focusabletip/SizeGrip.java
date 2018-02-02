/*
 * 12/23/2008
 *
 * SizeGrip.java - A size grip component that sits at the bottom of the window,
 * allowing the user to easily resize that window.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.focusabletip;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

/**
 * A component that allows its parent window to be resizable, similar to the
 * size grip seen on status bars.
 *
 * @author Robert Futrell
 * @version 1.0
 */
@SuppressWarnings({ "checkstyle:magicnumber" })
class SizeGrip extends JPanel {

	/**
	 * Listens for mouse events on this panel and resizes the parent window
	 * appropriately.
	 */
	private class MouseHandler extends MouseInputAdapter {

		/*
		 * NOTE: We use SwingUtilities.convertPointToScreen() instead of just using the
		 * locations relative to the corner component because the latter proved buggy -
		 * stretch the window too wide and some kind of arithmetic error started
		 * happening somewhere - our window would grow way too large.
		 */

		private Point origPos;

		@Override
		public void mouseDragged(final MouseEvent e) {
			final Point newPos = e.getPoint();
			SwingUtilities.convertPointToScreen(newPos, SizeGrip.this);
			final int xDelta = newPos.x - this.origPos.x;
			final int yDelta = newPos.y - this.origPos.y;
			final Window wind = SwingUtilities.getWindowAncestor(SizeGrip.this);
			if (wind != null) { // Should always be true
				if (SizeGrip.this.getComponentOrientation().isLeftToRight()) {
					int w = wind.getWidth();
					if (newPos.x >= wind.getX())
						w += xDelta;
					int h = wind.getHeight();
					if (newPos.y >= wind.getY())
						h += yDelta;
					wind.setSize(w, h);
				} else { // RTL
					final int newW = Math.max(1, wind.getWidth() - xDelta);
					final int newH = Math.max(1, wind.getHeight() + yDelta);
					wind.setBounds(newPos.x, wind.getY(), newW, newH);
				}
				// invalidate()/validate() needed pre-1.6.
				wind.invalidate();
				wind.validate();
			}
			this.origPos.setLocation(newPos);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			this.origPos = e.getPoint();
			SwingUtilities.convertPointToScreen(this.origPos, SizeGrip.this);
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			this.origPos = null;
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The size grip to use if we're on OS X.
	 */
	private transient Image osxSizeGrip;

	SizeGrip() {
		final MouseHandler adapter = new MouseHandler();
		this.addMouseListener(adapter);
		this.addMouseMotionListener(adapter);
		this.setPreferredSize(new Dimension(16, 16));
	}

	/**
	 * Overridden to ensure that the cursor for this component is appropriate for
	 * the orientation.
	 *
	 * @param o
	 *            The new orientation.
	 */
	@Override
	public void applyComponentOrientation(final ComponentOrientation o) {
		this.possiblyFixCursor(o.isLeftToRight());
		super.applyComponentOrientation(o);
	}

	/**
	 * Creates and returns the OS X size grip image.
	 *
	 * @return The OS X size grip.
	 */
	private Image createOSXSizeGrip() {
		final ClassLoader cl = this.getClass().getClassLoader();
		URL url = cl.getResource("org/fife/ui/rsyntaxtextarea/focusabletip/osx_sizegrip.png");
		if (url == null) {
			// We're not running in a jar - we may be debugging in Eclipse,
			// for example
			final File f = new File("../RSyntaxTextArea/src/org/fife/ui/rsyntaxtextarea/focusabletip/osx_sizegrip.png");
			if (f.isFile())
				try {
					url = f.toURI().toURL();
				} catch (final MalformedURLException mue) { // Never happens
					mue.printStackTrace();
					return null;
				}
			else
				return null; // Can't find resource or image file
		}
		Image image = null;
		try {
			image = ImageIO.read(url);
		} catch (final IOException ioe) { // Never happens
			ioe.printStackTrace();
		}
		return image;
	}

	/**
	 * Paints this panel.
	 *
	 * @param g
	 *            The graphics context.
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		super.paintComponent(g);

		final Dimension dim = this.getSize();

		if (this.osxSizeGrip != null) {
			g.drawImage(this.osxSizeGrip, dim.width - 16, dim.height - 16, null);
			return;
		}

		final Color c1 = UIManager.getColor("Label.disabledShadow");
		final Color c2 = UIManager.getColor("Label.disabledForeground");
		final ComponentOrientation orientation = this.getComponentOrientation();

		if (orientation.isLeftToRight()) {
			final int width = dim.width -= 3;
			final int height = dim.height -= 3;
			g.setColor(c1);
			g.fillRect(width - 9, height - 1, 3, 3);
			g.fillRect(width - 5, height - 1, 3, 3);
			g.fillRect(width - 1, height - 1, 3, 3);
			g.fillRect(width - 5, height - 5, 3, 3);
			g.fillRect(width - 1, height - 5, 3, 3);
			g.fillRect(width - 1, height - 9, 3, 3);
			g.setColor(c2);
			g.fillRect(width - 9, height - 1, 2, 2);
			g.fillRect(width - 5, height - 1, 2, 2);
			g.fillRect(width - 1, height - 1, 2, 2);
			g.fillRect(width - 5, height - 5, 2, 2);
			g.fillRect(width - 1, height - 5, 2, 2);
			g.fillRect(width - 1, height - 9, 2, 2);
		} else {
			final int height = dim.height -= 3;
			g.setColor(c1);
			g.fillRect(10, height - 1, 3, 3);
			g.fillRect(6, height - 1, 3, 3);
			g.fillRect(2, height - 1, 3, 3);
			g.fillRect(6, height - 5, 3, 3);
			g.fillRect(2, height - 5, 3, 3);
			g.fillRect(2, height - 9, 3, 3);
			g.setColor(c2);
			g.fillRect(10, height - 1, 2, 2);
			g.fillRect(6, height - 1, 2, 2);
			g.fillRect(2, height - 1, 2, 2);
			g.fillRect(6, height - 5, 2, 2);
			g.fillRect(2, height - 5, 2, 2);
			g.fillRect(2, height - 9, 2, 2);
		}

	}

	/**
	 * Ensures that the cursor for this component is appropriate for the
	 * orientation.
	 *
	 * @param ltr
	 *            Whether the current component orientation is LTR.
	 */
	protected void possiblyFixCursor(final boolean ltr) {
		int cursor = Cursor.NE_RESIZE_CURSOR;
		if (ltr)
			cursor = Cursor.NW_RESIZE_CURSOR;
		if (cursor != this.getCursor().getType())
			this.setCursor(Cursor.getPredefinedCursor(cursor));
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// TODO: Key off of Aqua LaF, not just OS X, as this size grip looks
		// bad on other LaFs on Mac such as Nimbus.
		if (System.getProperty("os.name").contains("OS X")) {
			if (this.osxSizeGrip == null)
				this.osxSizeGrip = this.createOSXSizeGrip();
		} else
			this.osxSizeGrip = null;

	}

}