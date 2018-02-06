/*
 * 04/29/2010
 *
 * EmptyIcon.java - The canonical icon that paints nothing.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * A standard icon that doesn't paint anything. This can be used when some
 * <code>Completion</code>s have icons and others don't, to visually align the
 * text of all completions.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class EmptyIcon implements Icon, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final int size;

	public EmptyIcon(final int size) {
		this.size = size;
	}

	@Override
	public int getIconHeight() {
		return this.size;
	}

	@Override
	public int getIconWidth() {
		return this.size;
	}

	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
	}

}