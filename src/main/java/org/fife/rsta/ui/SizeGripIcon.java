/*
 * 09/07/2006
 *
 * SizeGripIcon.java - An icon that paints a size grip.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * An icon that looks like a Windows 98 or XP-style size grip.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class SizeGripIcon implements Icon {

	private static final int SIZE = 16;

	/**
	 * Returns the height of this icon.
	 *
	 * @return This icon's height.
	 */
	@Override
	public int getIconHeight() {
		return SizeGripIcon.SIZE;
	}

	/**
	 * Returns the width of this icon.
	 *
	 * @return This icon's width.
	 */
	@Override
	public int getIconWidth() {
		return SizeGripIcon.SIZE;
	}

	/**
	 * Paints this icon.
	 *
	 * @param c
	 *            The component to paint on.
	 * @param g
	 *            The graphics context.
	 * @param x
	 *            The x-coordinate at which to paint.
	 * @param y
	 *            The y-coordinate at which to paint.
	 */
	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y) {

		final Dimension dim = c.getSize();
		final Color c1 = UIManager.getColor("Label.disabledShadow");
		final Color c2 = UIManager.getColor("Label.disabledForeground");

		final ComponentOrientation orientation = c.getComponentOrientation();
		final int height = dim.height -= 3;

		if (orientation.isLeftToRight()) {
			final int width = dim.width -= 3;
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

}