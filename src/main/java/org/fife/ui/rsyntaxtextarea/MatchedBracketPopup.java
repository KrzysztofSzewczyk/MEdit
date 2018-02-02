/*
 * 07/03/2016
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.focusabletip.TipUtil;

/**
 * A tool tip-like popup that shows the line of code containing the bracket
 * matched to that at the caret position, if it is scrolled out of the user's
 * viewport.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class MatchedBracketPopup extends JWindow {

	/**
	 * Action performed when Escape is pressed in this popup.
	 */
	private class EscapeAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			MatchedBracketPopup.this.listener.uninstallAndHide();
		}

	}

	/**
	 * Listens for events in this popup.
	 */
	private class Listener extends WindowAdapter implements ComponentListener {

		Listener() {

			MatchedBracketPopup.this.addWindowFocusListener(this);

			// If anything happens to the "parent" window, hide this popup
			final Window parent = (Window) MatchedBracketPopup.this.getParent();
			parent.addWindowFocusListener(this);
			parent.addWindowListener(this);
			parent.addComponentListener(this);

		}

		private boolean checkForParentWindowEvent(final WindowEvent e) {
			if (e.getSource() == MatchedBracketPopup.this.getParent()) {
				this.uninstallAndHide();
				return true;
			}
			return false;
		}

		@Override
		public void componentHidden(final ComponentEvent e) {
			this.uninstallAndHide();
		}

		@Override
		public void componentMoved(final ComponentEvent e) {
			this.uninstallAndHide();
		}

		@Override
		public void componentResized(final ComponentEvent e) {
			this.uninstallAndHide();
		}

		@Override
		public void componentShown(final ComponentEvent e) {
			this.uninstallAndHide();
		}

		private void uninstallAndHide() {
			final Window parent = (Window) MatchedBracketPopup.this.getParent();
			parent.removeWindowFocusListener(this);
			parent.removeWindowListener(this);
			parent.removeComponentListener(this);
			MatchedBracketPopup.this.removeWindowFocusListener(this);
			MatchedBracketPopup.this.setVisible(false);
			MatchedBracketPopup.this.dispose();
		}

		@Override
		public void windowActivated(final WindowEvent e) {
			this.checkForParentWindowEvent(e);
		}

		@Override
		public void windowIconified(final WindowEvent e) {
			this.checkForParentWindowEvent(e);
		}

		@Override
		public void windowLostFocus(final WindowEvent e) {
			this.uninstallAndHide();
		}

	}

	private static final int LEFT_EMPTY_BORDER = 5;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private transient Listener listener;

	private final RSyntaxTextArea textArea;

	MatchedBracketPopup(final Window parent, final RSyntaxTextArea textArea, final int offsToRender) {

		super(parent);
		this.textArea = textArea;
		final JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createCompoundBorder(TipUtil.getToolTipBorder(),
				BorderFactory.createEmptyBorder(2, MatchedBracketPopup.LEFT_EMPTY_BORDER, 5, 5)));
		cp.setBackground(TipUtil.getToolTipBackground());
		this.setContentPane(cp);

		cp.add(new JLabel(this.getText(offsToRender)));

		this.installKeyBindings();
		this.listener = new Listener();
		this.setLocation();

	}

	/**
	 * Overridden to ensure this popup stays in a specific size range.
	 */
	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();
		if (size != null)
			size.width = Math.min(size.width, 800);
		return size;
	}

	private String getText(final int offsToRender) {

		int line = 0;
		try {
			line = this.textArea.getLineOfOffset(offsToRender);
		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
			return null;
		}

		final int lastLine = line + 1;

		// Render prior line if the open brace line has no other text on it
		if (line > 0)
			try {
				final int startOffs = this.textArea.getLineStartOffset(line);
				final int length = this.textArea.getLineEndOffset(line) - startOffs;
				final String text = this.textArea.getText(startOffs, length);
				if (text.trim().length() == 1)
					line--;
			} catch (final BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(this.textArea);
				ble.printStackTrace();
			}

		final Font font = this.textArea.getFontForTokenType(TokenTypes.IDENTIFIER);
		final StringBuilder sb = new StringBuilder("<html>");
		sb.append("<style>body { font-size:\"").append(font.getSize());
		sb.append("pt\" }</style><nobr>");
		while (line < lastLine) {
			Token t = this.textArea.getTokenListForLine(line);
			while (t != null && t.isPaintable()) {
				t.appendHTMLRepresentation(sb, this.textArea, true, true);
				t = t.getNextToken();
			}
			sb.append("<br>");
			line++;
		}

		return sb.toString();

	}

	/**
	 * Adds key bindings to this popup.
	 */
	private void installKeyBindings() {

		final InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final ActionMap am = this.getRootPane().getActionMap();

		final KeyStroke escapeKS = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		im.put(escapeKS, "onEscape");
		am.put("onEscape", new EscapeAction());
	}

	/**
	 * Positions this popup to be in the top right-hand corner of the parent editor.
	 */
	private void setLocation() {
		final Point topLeft = this.textArea.getVisibleRect().getLocation();
		SwingUtilities.convertPointToScreen(topLeft, this.textArea);
		topLeft.y = Math.max(topLeft.y - 24, 0);
		this.setLocation(topLeft.x - MatchedBracketPopup.LEFT_EMPTY_BORDER, topLeft.y);
	}
}