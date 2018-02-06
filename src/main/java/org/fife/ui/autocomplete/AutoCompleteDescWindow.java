/*
 * 12/21/2008
 *
 * AutoCompleteDescWindow.java - A window containing a description of the
 * currently selected completion.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;

/**
 * The optional "description" window that describes the currently selected item
 * in the auto-completion window.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class AutoCompleteDescWindow extends JWindow implements HyperlinkListener, DescWindowCallback {

	/**
	 * A completion and its cached summary text.
	 */
	private static class HistoryEntry {

		public String anchor;
		public Completion completion;
		public String summary;

		public HistoryEntry(final Completion completion, final String summary, final String anchor) {
			this.completion = completion;
			this.summary = summary;
			this.anchor = anchor;
		}

		/**
		 * Overridden to display a short name for the completion, since it's used in the
		 * tool tips for the "back" and "forward" buttons.
		 *
		 * @return A string representation of this history entry.
		 */
		@Override
		public String toString() {
			return this.completion.getInputText();
		}

	}

	/**
	 * Action that actually updates the summary text displayed.
	 */
	private class TimerAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private boolean addToHistory;
		private String anchor;
		private Completion completion;

		/**
		 * Called when the timer is fired.
		 */
		@Override
		public void actionPerformed(final ActionEvent e) {
			AutoCompleteDescWindow.this.setDisplayedDesc(this.completion, this.anchor, this.addToHistory);
		}

		public void setCompletion(final Completion c, final String anchor, final boolean addToHistory) {
			this.completion = c;
			this.anchor = anchor;
			this.addToHistory = addToHistory;
		}

	}

	/**
	 * Action that moves to the previous description displayed.
	 */
	class ToolBarBackAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ToolBarBackAction(final boolean ltr) {
			final String img = "org/fife/ui/autocomplete/arrow_" + (ltr ? "left.png" : "right.png");
			final ClassLoader cl = this.getClass().getClassLoader();
			final Icon icon = new ImageIcon(cl.getResource(img));
			this.putValue(Action.SMALL_ICON, icon);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompleteDescWindow.this.historyPos > 0) {
				final HistoryEntry pair = AutoCompleteDescWindow.this.history
						.get(--AutoCompleteDescWindow.this.historyPos);
				AutoCompleteDescWindow.this.descArea.setText(pair.summary);
				if (pair.anchor != null)
					// System.out.println("Scrolling to: " + pair.anchor);
					AutoCompleteDescWindow.this.descArea.scrollToReference(pair.anchor);
				else
					AutoCompleteDescWindow.this.descArea.setCaretPosition(0);
				AutoCompleteDescWindow.this.setActionStates();
			}
		}

	}

	/**
	 * Action that moves to the previous description displayed.
	 */
	class ToolBarForwardAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ToolBarForwardAction(final boolean ltr) {
			final String img = "org/fife/ui/autocomplete/arrow_" + (ltr ? "right.png" : "left.png");
			final ClassLoader cl = this.getClass().getClassLoader();
			final Icon icon = new ImageIcon(cl.getResource(img));
			this.putValue(Action.SMALL_ICON, icon);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompleteDescWindow.this.history != null
					&& AutoCompleteDescWindow.this.historyPos < AutoCompleteDescWindow.this.history.size() - 1) {
				final HistoryEntry pair = AutoCompleteDescWindow.this.history
						.get(++AutoCompleteDescWindow.this.historyPos);
				AutoCompleteDescWindow.this.descArea.setText(pair.summary);
				if (pair.anchor != null)
					// System.out.println("Scrolling to: " + pair.anchor);
					AutoCompleteDescWindow.this.descArea.scrollToReference(pair.anchor);
				else
					AutoCompleteDescWindow.this.descArea.setCaretPosition(0);
				AutoCompleteDescWindow.this.setActionStates();
			}
		}

	}

	/**
	 * The amount of time to wait after the user changes the selected completion to
	 * refresh the description. This delay is in place to help performance for
	 * {@link Completion}s that may be slow to compute their summary text.
	 */
	private static final int INITIAL_TIMER_DELAY = 120;

	/**
	 * The resource bundle name.
	 */
	private static final String MSG = "org.fife.ui.autocomplete.AutoCompleteDescWindow";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The parent AutoCompletion instance.
	 */
	private AutoCompletion ac;

	/**
	 * Action that goes to the previous description displayed.
	 */
	private Action backAction;

	/**
	 * The bottom panel, containing the toolbar and size grip.
	 */
	private JPanel bottomPanel;

	/**
	 * The resource bundle for this window.
	 */
	private ResourceBundle bundle;

	/**
	 * Renders the HTML description.
	 */
	private JEditorPane descArea;

	/**
	 * The toolbar with "back" and "forward" buttons.
	 */
	private JToolBar descWindowNavBar;

	/**
	 * Action that goes to the next description displayed.
	 */
	private Action forwardAction;

	/**
	 * History of descriptions displayed.
	 */
	private List<HistoryEntry> history;

	/**
	 * The current position in {@link #history}.
	 */
	private int historyPos;

	/**
	 * The scroll pane that {@link #descArea} is in.
	 */
	private JScrollPane scrollPane;

	/**
	 * Provides a slight delay between asking to set a description and actually
	 * displaying it, so that if the user is scrolling quickly through completions,
	 * those with slow-to-calculate summaries won't bog down the scrolling.
	 */
	private Timer timer;

	/**
	 * The action that listens for the timer to fire.
	 */
	private TimerAction timerAction;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent window.
	 * @param ac
	 *            The parent auto-completion.
	 */
	public AutoCompleteDescWindow(final Window owner, final AutoCompletion ac) {

		super(owner);
		this.ac = ac;

		final ComponentOrientation o = ac.getTextComponentOrientation();

		final JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(TipUtil.getToolTipBorder());

		this.descArea = new JEditorPane("text/html", null);
		TipUtil.tweakTipEditorPane(this.descArea);
		this.descArea.addHyperlinkListener(this);
		this.scrollPane = new JScrollPane(this.descArea);
		Border b = BorderFactory.createEmptyBorder();
		this.scrollPane.setBorder(b);
		this.scrollPane.setViewportBorder(b);
		this.scrollPane.setBackground(this.descArea.getBackground());
		this.scrollPane.getViewport().setBackground(this.descArea.getBackground());
		cp.add(this.scrollPane);

		this.descWindowNavBar = new JToolBar();
		this.backAction = new ToolBarBackAction(o.isLeftToRight());
		this.forwardAction = new ToolBarForwardAction(o.isLeftToRight());
		this.descWindowNavBar.setFloatable(false);
		this.descWindowNavBar.add(new JButton(this.backAction));
		this.descWindowNavBar.add(new JButton(this.forwardAction));

		this.bottomPanel = new JPanel(new BorderLayout());
		b = new AbstractBorder() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Insets getBorderInsets(final Component c) {
				return new Insets(1, 0, 0, 0);
			}

			@Override
			public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w,
					final int h) {
				g.setColor(UIManager.getColor("controlDkShadow"));
				g.drawLine(x, y, x + w - 1, y);
			}
		};
		this.bottomPanel.setBorder(b);
		final SizeGrip rp = new SizeGrip();
		this.bottomPanel.add(this.descWindowNavBar, BorderLayout.LINE_START);
		this.bottomPanel.add(rp, BorderLayout.LINE_END);
		cp.add(this.bottomPanel, BorderLayout.SOUTH);
		this.setContentPane(cp);

		this.applyComponentOrientation(o);
		this.setFocusableWindowState(false);

		// Give apps a chance to decorate us with drop shadows, etc.
		if (Util.getShouldAllowDecoratingMainAutoCompleteWindows()) {
			final PopupWindowDecorator decorator = PopupWindowDecorator.get();
			if (decorator != null)
				decorator.decorate(this);
		}

		this.history = new ArrayList<>(1); // Usually small
		this.historyPos = -1;

		this.timerAction = new TimerAction();
		this.timer = new Timer(AutoCompleteDescWindow.INITIAL_TIMER_DELAY, this.timerAction);
		this.timer.setRepeats(false);

	}

	/**
	 * Sets the currently displayed description and updates the history.
	 *
	 * @param historyItem
	 *            The item to add to the history.
	 */
	private void addToHistory(final HistoryEntry historyItem) {
		this.history.add(++this.historyPos, historyItem);
		this.clearHistoryAfterCurrentPos();
		this.setActionStates();
	}

	/**
	 * Clears the history of viewed descriptions.
	 */
	private void clearHistory() {
		this.history.clear(); // Try to free some memory.
		this.historyPos = -1;
		if (this.descWindowNavBar != null)
			this.setActionStates();
	}

	/**
	 * Makes the current history page the last one in the history.
	 */
	private void clearHistoryAfterCurrentPos() {
		for (int i = this.history.size() - 1; i > this.historyPos; i--)
			this.history.remove(i);
		this.setActionStates();
	}

	/**
	 * Copies from the description text area, if it is visible and there is a
	 * selection.
	 *
	 * @return Whether a copy occurred.
	 */
	public boolean copy() {
		if (this.isVisible() && this.descArea.getSelectionStart() != this.descArea.getSelectionEnd()) {
			this.descArea.copy();
			return true;
		}
		return false;
	}

	/**
	 * Returns the localized message for the specified key.
	 *
	 * @param key
	 *            The key.
	 * @return The localized message.
	 */
	private String getString(final String key) {
		if (this.bundle == null)
			this.bundle = ResourceBundle.getBundle(AutoCompleteDescWindow.MSG);
		return this.bundle.getString(key);
	}

	/**
	 * Called when a hyperlink is clicked.
	 *
	 * @param e
	 *            The event.
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {

		final HyperlinkEvent.EventType type = e.getEventType();
		if (!type.equals(HyperlinkEvent.EventType.ACTIVATED))
			return;

		// Users can redirect URL's, perhaps to a local copy of documentation.
		URL url = e.getURL();
		if (url != null) {
			final LinkRedirector redirector = AutoCompletion.getLinkRedirector();
			if (redirector != null) {
				final URL newUrl = redirector.possiblyRedirect(url);
				if (newUrl != null && newUrl != url) {
					url = newUrl;
					e = new HyperlinkEvent(e.getSource(), e.getEventType(), newUrl, e.getDescription(),
							e.getSourceElement());
				}
			}
		}

		// Custom hyperlink handler for this completion type
		final ExternalURLHandler handler = this.ac.getExternalURLHandler();
		if (handler != null) {
			final HistoryEntry current = this.history.get(this.historyPos);
			handler.urlClicked(e, current.completion, this);
			return;
		}

		// No custom handler...
		if (url != null)
			// Try loading in external browser (Java 6+ only).
			try {
				Util.browse(new URI(url.toString()));
			} catch (/* IO */final URISyntaxException ioe) {
				UIManager.getLookAndFeel().provideErrorFeedback(this.descArea);
				ioe.printStackTrace();
			}
		else { // Assume simple function name text, like in c.xml
				// FIXME: This is really a hack, and we assume we can find the
				// linked-to item in the same CompletionProvider.
			final AutoCompletePopupWindow parent = (AutoCompletePopupWindow) this.getParent();
			final CompletionProvider p = parent.getSelection().getProvider();
			if (p instanceof AbstractCompletionProvider) {
				final String name = e.getDescription();
				final List<Completion> l = ((AbstractCompletionProvider) p).getCompletionByInputText(name);
				if (l != null && !l.isEmpty()) {
					// Just use the 1st one if there's more than 1
					final Completion c = l.get(0);
					this.setDescriptionFor(c, true);
				} else
					UIManager.getLookAndFeel().provideErrorFeedback(this.descArea);
			}
		}

	}

	/**
	 * Enables or disables the back and forward actions as appropriate.
	 */
	private void setActionStates() {
		// TODO: Localize this text!
		String desc = null;
		if (this.historyPos > 0) {
			this.backAction.setEnabled(true);
			desc = "Back to " + this.history.get(this.historyPos - 1);
		} else
			this.backAction.setEnabled(false);
		this.backAction.putValue(Action.SHORT_DESCRIPTION, desc);
		if (this.historyPos > -1 && this.historyPos < this.history.size() - 1) {
			this.forwardAction.setEnabled(true);
			desc = "Forward to " + this.history.get(this.historyPos + 1);
		} else {
			this.forwardAction.setEnabled(false);
			desc = null;
		}
		this.forwardAction.putValue(Action.SHORT_DESCRIPTION, desc);
	}

	/**
	 * Sets the description displayed in this window. This clears the history.
	 *
	 * @param item
	 *            The item whose description you want to display.
	 */
	public void setDescriptionFor(final Completion item) {
		this.setDescriptionFor(item, false);
	}

	/**
	 * Sets the description displayed in this window.
	 *
	 * @param item
	 *            The item whose description you want to display.
	 * @param addToHistory
	 *            Whether to add this page to the page history (as opposed to
	 *            clearing it and starting anew).
	 */
	protected void setDescriptionFor(final Completion item, final boolean addToHistory) {
		this.setDescriptionFor(item, null, addToHistory);
	}

	/**
	 * Sets the description displayed in this window.
	 *
	 * @param item
	 *            The item whose description you want to display.
	 * @parma anchor The anchor to jump to, or <code>null</code> if none.
	 * @param addToHistory
	 *            Whether to add this page to the page history (as opposed to
	 *            clearing it and starting anew).
	 */
	protected void setDescriptionFor(final Completion item, final String anchor, final boolean addToHistory) {
		this.timer.stop();
		this.timerAction.setCompletion(item, anchor, addToHistory);
		this.timer.start();
	}

	private void setDisplayedDesc(final Completion completion, final String anchor, final boolean addToHistory) {

		String desc = completion == null ? null : completion.getSummary();
		if (desc == null)
			desc = "<html><em>" + this.getString("NoDescAvailable") + "</em>";
		this.descArea.setText(desc);
		if (anchor != null)
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					AutoCompleteDescWindow.this.descArea.scrollToReference(anchor);
				}
			});
		else
			this.descArea.setCaretPosition(0); // In case of scrolling

		if (!addToHistory)
			// Remove everything first if this is going to be the only
			// thing in history.
			this.clearHistory();
		this.addToHistory(new HistoryEntry(completion, desc, null));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setVisible(final boolean visible) {
		if (!visible)
			this.clearHistory();
		super.setVisible(visible);
	}

	/**
	 * Callback for custom <code>ExternalURLHandler</code>s.
	 *
	 * @param completion
	 *            The completion to display.
	 * @param anchor
	 *            The anchor in the HTML to jump to, or <code>null</code> if none.
	 */
	@Override
	public void showSummaryFor(final Completion completion, final String anchor) {
		this.setDescriptionFor(completion, anchor, true);
	}

	/**
	 * Called by the parent completion popup window the LookAndFeel is updated.
	 */
	public void updateUI() {
		SwingUtilities.updateComponentTreeUI(this);
		// Update editor pane for new font, bg, selection colors, etc.
		TipUtil.tweakTipEditorPane(this.descArea);
		this.scrollPane.setBackground(this.descArea.getBackground());
		this.scrollPane.getViewport().setBackground(this.descArea.getBackground());
		((JPanel) this.getContentPane()).setBorder(TipUtil.getToolTipBorder());
	}

}