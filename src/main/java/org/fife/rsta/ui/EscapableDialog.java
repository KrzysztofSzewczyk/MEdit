/*
 * 08/06/2012
 *
 * EscapableDialog.java - A dialog that can be dismissed via the Escape key.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * A dialog that closes itself when the Escape key is pressed. Subclasses can
 * extend the {@link #escapePressed()} method and provide custom handling logic
 * (parameter validation, custom closing, etc.).
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class EscapableDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The key in an <code>InputMap</code> for the Escape key action.
	 */
	private static final String ESCAPE_KEY = "OnEsc";

	/**
	 * Constructor.
	 */
	public EscapableDialog() {
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent dialog.
	 */
	public EscapableDialog(final Dialog owner) {
		super(owner);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent dialog.
	 * @param modal
	 *            Whether this dialog is modal.
	 */
	public EscapableDialog(final Dialog owner, final boolean modal) {
		super(owner, modal);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent dialog.
	 * @param title
	 *            The title of this dialog.
	 */
	public EscapableDialog(final Dialog owner, final String title) {
		super(owner, title);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent dialog.
	 * @param title
	 *            The title of this dialog.
	 * @param modal
	 *            Whether this dialog is modal.
	 */
	public EscapableDialog(final Dialog owner, final String title, final boolean modal) {
		super(owner, title, modal);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent frame.
	 */
	public EscapableDialog(final Frame owner) {
		super(owner);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent frame.
	 * @param modal
	 *            Whether this dialog is modal.
	 */
	public EscapableDialog(final Frame owner, final boolean modal) {
		super(owner, modal);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent frame.
	 * @param title
	 *            The title of this dialog.
	 */
	public EscapableDialog(final Frame owner, final String title) {
		super(owner, title);
		this.init();
	}

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent frame.
	 * @param title
	 *            The title of this dialog.
	 * @param modal
	 *            Whether this dialog is modal.
	 */
	public EscapableDialog(final Frame owner, final String title, final boolean modal) {
		super(owner, title, modal);
		this.init();
	}

	/**
	 * Called when the Escape key is pressed in this dialog. Subclasses can override
	 * to handle any custom "Cancel" logic. The default implementation hides the
	 * dialog (via <code>setVisible(false);</code>).
	 */
	protected void escapePressed() {
		this.setVisible(false);
	}

	/**
	 * Initializes this dialog.
	 */
	private void init() {
		this.setEscapeClosesDialog(true);
	}

	/**
	 * Toggles whether the Escape key closes this dialog.
	 *
	 * @param closes
	 *            Whether Escape should close this dialog (actually, whether
	 *            {@link #escapePressed()} should be called when Escape is pressed).
	 */
	public void setEscapeClosesDialog(final boolean closes) {

		final JRootPane rootPane = this.getRootPane();
		final InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = rootPane.getActionMap();
		final KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

		if (closes) {
			im.put(ks, EscapableDialog.ESCAPE_KEY);
			actionMap.put(EscapableDialog.ESCAPE_KEY, new AbstractAction() {
				/**
				 *
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					EscapableDialog.this.escapePressed();
				}
			});
		} else {
			im.remove(ks);
			actionMap.remove(EscapableDialog.ESCAPE_KEY);
		}

	}

}