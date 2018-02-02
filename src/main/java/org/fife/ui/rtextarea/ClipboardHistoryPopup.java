/*
 * 08/16/2014
 *
 * ClipboardHistoryPopup.java - Shows clipboard history in a popup window.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.Caret;

import org.fife.ui.rsyntaxtextarea.focusabletip.TipUtil;

/**
 * A popup window that displays the most recent snippets added to the clipboard
 * of an <code>RSyntaxTextArea</code>. Selecting one pastes that snippet.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class ClipboardHistoryPopup extends JWindow {

	/**
	 * The list component used in this popup.
	 */
	@SuppressWarnings("rawtypes")
	private static final class ChoiceList extends JList {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		private ChoiceList() {
			super(new DefaultListModel());
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.installKeyboardActions();
		}

		private void installKeyboardActions() {

			final InputMap im = this.getInputMap();
			final ActionMap am = this.getActionMap();

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "onDown");
			am.put("onDown", new AbstractAction() {
				/**
				 *
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					final int index = (ChoiceList.this.getSelectedIndex() + 1) % ChoiceList.this.getModel().getSize();
					ChoiceList.this.ensureIndexIsVisible(index);
					ChoiceList.this.setSelectedIndex(index);
				}
			});

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "onUp");
			am.put("onUp", new AbstractAction() {
				/**
				 *
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					int index = ChoiceList.this.getSelectedIndex() - 1;
					if (index < 0)
						index += ChoiceList.this.getModel().getSize();
					ChoiceList.this.ensureIndexIsVisible(index);
					ChoiceList.this.setSelectedIndex(index);
				}
			});

		}

		@SuppressWarnings("unchecked")
		private void setContents(final List<String> contents) {
			final DefaultListModel model = (DefaultListModel) this.getModel();
			model.clear();
			for (final String str : contents)
				model.addElement(new LabelValuePair(str));
			this.setVisibleRowCount(Math.min(model.getSize(), 8));
		}

	}

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
			ClipboardHistoryPopup.this.listener.uninstallAndHide();
		}

	}

	/**
	 * Entries in the choices list are of this type. This truncates entries that are
	 * too long. In the future it can provide more information (line count for
	 * multi-line pastes, etc.).
	 */
	private static class LabelValuePair {

		private static final int LABEL_MAX_LENGTH = 50;
		private String label;

		private String value;

		LabelValuePair(final String value) {
			this.label = this.value = value;
			final int newline = this.label.indexOf('\n');
			boolean multiLine = false;
			if (newline > -1) {
				this.label = this.label.substring(0, newline);
				multiLine = true;
			}
			if (this.label.length() > LabelValuePair.LABEL_MAX_LENGTH)
				this.label = this.label.substring(0, LabelValuePair.LABEL_MAX_LENGTH) + "...";
			else if (multiLine) {
				final int toRemove = 3 - (LabelValuePair.LABEL_MAX_LENGTH - this.label.length());
				if (toRemove > 0)
					this.label = this.label.substring(0, this.label.length() - toRemove);
				this.label += "...";
			}
		}

		@Override
		public String toString() {
			return this.label;
		}

	}

	/**
	 * Listens for events in this popup.
	 */
	private class Listener extends WindowAdapter implements ComponentListener {

		Listener() {

			ClipboardHistoryPopup.this.addWindowFocusListener(this);
			ClipboardHistoryPopup.this.list.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					if (e.getClickCount() == 2)
						ClipboardHistoryPopup.this.insertSelectedItem();
				}
			});
			ClipboardHistoryPopup.this.list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "onEnter");
			ClipboardHistoryPopup.this.list.getActionMap().put("onEnter", new AbstractAction() {
				/**
				 *
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					ClipboardHistoryPopup.this.insertSelectedItem();
				}
			});

			// If anything happens to the "parent" window, hide this popup
			final Window parent = (Window) ClipboardHistoryPopup.this.getParent();
			parent.addWindowFocusListener(this);
			parent.addWindowListener(this);
			parent.addComponentListener(this);

		}

		private boolean checkForParentWindowEvent(final WindowEvent e) {
			if (e.getSource() == ClipboardHistoryPopup.this.getParent()) {
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
			final Window parent = (Window) ClipboardHistoryPopup.this.getParent();
			parent.removeWindowFocusListener(this);
			parent.removeWindowListener(this);
			parent.removeComponentListener(this);
			ClipboardHistoryPopup.this.removeWindowFocusListener(this);
			ClipboardHistoryPopup.this.setVisible(false);
			ClipboardHistoryPopup.this.dispose();
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
			if (e.getSource() == ClipboardHistoryPopup.this)
				this.uninstallAndHide();
		}

	}

	private static final String MSG = "org.fife.ui.rtextarea.RTextArea";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The space between the caret and the completion popup.
	 */
	private static final int VERTICAL_SPACE = 1;

	private final ChoiceList list;

	private transient Listener listener;

	private boolean prevCaretAlwaysVisible;

	private final RTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            The parent window containing <code>textArea</code>.
	 * @param textArea
	 *            The text area to paste into.
	 */
	ClipboardHistoryPopup(final Window parent, final RTextArea textArea) {

		super(parent);
		this.textArea = textArea;

		final JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(
				BorderFactory.createCompoundBorder(org.fife.ui.rsyntaxtextarea.focusabletip.TipUtil.getToolTipBorder(),
						BorderFactory.createEmptyBorder(2, 5, 5, 5)));
		cp.setBackground(org.fife.ui.rsyntaxtextarea.focusabletip.TipUtil.getToolTipBackground());
		this.setContentPane(cp);

		final ResourceBundle msg = ResourceBundle.getBundle(ClipboardHistoryPopup.MSG);
		final JLabel title = new JLabel(msg.getString("Action.ClipboardHistory.Popup.Label"));
		cp.add(title, BorderLayout.NORTH);

		this.list = new ChoiceList();
		final JScrollPane sp = new JScrollPane(this.list);
		sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		cp.add(sp);

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
		if (size != null) {
			size.width = Math.min(size.width, 300);
			size.width = Math.max(size.width, 200);
		}
		return size;
	}

	/**
	 * Inserts the selected item into the editor and disposes of this popup.
	 */
	private void insertSelectedItem() {
		final Object lvp = this.list.getSelectedValue();
		if (lvp != null) {
			this.listener.uninstallAndHide();
			final String text = ((LabelValuePair) lvp).value;
			this.textArea.replaceSelection(text);
			ClipboardHistory.get().add(text); // Move this item to the top
		}
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
		this.list.getInputMap().remove(escapeKS);
	}

	public void setContents(final List<String> contents) {
		this.list.setContents(contents);
		// Must re-size since we now have data!
		this.pack();
	}

	/**
	 * Positions this popup to be in the top right-hand corner of the parent editor.
	 */
	private void setLocation() {

		Rectangle r = null;
		try {
			r = this.textArea.modelToView(this.textArea.getCaretPosition());
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}
		final Point p = r.getLocation();
		SwingUtilities.convertPointToScreen(p, this.textArea);
		r.x = p.x;
		r.y = p.y;

		final Rectangle screenBounds = TipUtil.getScreenBoundsForPoint(r.x, r.y);
		// Dimension screenSize = getToolkit().getScreenSize();

		final int totalH = this.getHeight();

		// Try putting our stuff "below" the caret first. We assume that the
		// entire height of our stuff fits on the screen one way or the other.
		int y = r.y + r.height + ClipboardHistoryPopup.VERTICAL_SPACE;
		if (y + totalH > screenBounds.height)
			y = r.y - ClipboardHistoryPopup.VERTICAL_SPACE - this.getHeight();

		// Get x-coordinate of completions. Try to align left edge with the
		// caret first.
		int x = r.x;
		if (!this.textArea.getComponentOrientation().isLeftToRight())
			x -= this.getWidth(); // RTL => align right edge
		if (x < screenBounds.x)
			x = screenBounds.x;
		else if (x + this.getWidth() > screenBounds.x + screenBounds.width)
			x = screenBounds.x + screenBounds.width - this.getWidth();

		this.setLocation(x, y);

	}

	@Override
	public void setVisible(final boolean visible) {
		if (this.list.getModel().getSize() == 0) {
			UIManager.getLookAndFeel().provideErrorFeedback(this.textArea);
			return;
		}
		super.setVisible(visible);
		this.updateTextAreaCaret(visible);
		if (visible)
			SwingUtilities.invokeLater(() -> {
				ClipboardHistoryPopup.this.requestFocus();
				if (ClipboardHistoryPopup.this.list.getModel().getSize() > 0)
					ClipboardHistoryPopup.this.list.setSelectedIndex(0);
				ClipboardHistoryPopup.this.list.requestFocusInWindow();
			});
	}

	/**
	 * (Possibly) toggles the "always visible" state of the text area's caret.
	 *
	 * @param visible
	 *            Whether this popup window was just made visible (as opposed to
	 *            hidden).
	 */
	private void updateTextAreaCaret(final boolean visible) {
		final Caret caret = this.textArea.getCaret();
		if (caret instanceof ConfigurableCaret) { // Always true by default
			final ConfigurableCaret cc = (ConfigurableCaret) caret;
			if (visible) {
				this.prevCaretAlwaysVisible = cc.isAlwaysVisible();
				cc.setAlwaysVisible(true);
			} else
				cc.setAlwaysVisible(this.prevCaretAlwaysVisible);
		}
	}

}
