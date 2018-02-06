/*
 * 07/30/2011
 *
 * DecorativeIconPanel.java - Displays a small decorative icon beside some
 * other component.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.fife.ui.autocomplete.EmptyIcon;

/**
 * A panel that displays an 8x8 decorative icon for a component, such as a text
 * field or combo box. This can be used to display error icons, warning icons,
 * etc.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see AssistanceIconPanel
 */
public class DecorativeIconPanel extends JPanel {

	protected static final EmptyIcon EMPTY_ICON = new EmptyIcon(DecorativeIconPanel.WIDTH);

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The width of this icon panel, to help align the components we're listening to
	 * with other combo boxes or text fields without a DecorativeIconPanel.
	 */
	public static final int WIDTH = 8;
	private JLabel iconLabel;
	private boolean showIcon;

	private String tip;

	/**
	 * Constructor.
	 */
	public DecorativeIconPanel() {
		this.setLayout(new BorderLayout());
		this.iconLabel = new JLabel(DecorativeIconPanel.EMPTY_ICON) {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(final MouseEvent e) {
				return DecorativeIconPanel.this.showIcon ? DecorativeIconPanel.this.tip : null;
			}
		};
		this.iconLabel.setVerticalAlignment(SwingConstants.TOP);
		ToolTipManager.sharedInstance().registerComponent(this.iconLabel);
		this.add(this.iconLabel, BorderLayout.NORTH);
	}

	/**
	 * Returns the icon being displayed.
	 *
	 * @return The icon.
	 * @see #setIcon(Icon)
	 */
	public Icon getIcon() {
		return this.iconLabel.getIcon();
	}

	/**
	 * Returns whether the icon (if any) is being rendered.
	 *
	 * @return Whether the icon is being rendered.
	 * @see #setShowIcon(boolean)
	 */
	public boolean getShowIcon() {
		return this.showIcon;
	}

	/**
	 * Returns the tool tip displayed when the mouse hovers over the icon. If the
	 * icon is not being displayed, this parameter is ignored.
	 *
	 * @return The tool tip text.
	 * @see #setToolTipText(String)
	 */
	@Override
	public String getToolTipText() {
		return this.tip;
	}

	/**
	 * Paints any child components. Overridden so the user can explicitly hide the
	 * icon.
	 *
	 * @param g
	 *            The graphics context.
	 * @see #setShowIcon(boolean)
	 */
	@Override
	protected void paintChildren(final Graphics g) {
		if (this.showIcon)
			super.paintChildren(g);
	}

	/**
	 * Sets the icon to display.
	 *
	 * @param icon
	 *            The new icon to display.
	 * @see #getIcon()
	 */
	public void setIcon(Icon icon) {
		if (icon == null)
			icon = DecorativeIconPanel.EMPTY_ICON;
		this.iconLabel.setIcon(icon);
	}

	/**
	 * Toggles whether the icon should be shown.
	 *
	 * @param show
	 *            Whether to show the icon.
	 * @see #getShowIcon()
	 */
	public void setShowIcon(final boolean show) {
		if (show != this.showIcon) {
			this.showIcon = show;
			this.repaint();
		}
	}

	/**
	 * Sets the tool tip text to display when the mouse is over the icon. This
	 * parameter is ignored if the icon is not being displayed.
	 *
	 * @param tip
	 *            The tool tip text to display.
	 * @see #getToolTipText()
	 */
	@Override
	public void setToolTipText(final String tip) {
		this.tip = tip;
	}

}