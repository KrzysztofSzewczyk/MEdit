/*
 * 09/20/2013
 *
 * FindToolBar - A tool bar for "find" operations in text areas.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.AssistanceIconPanel;
import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rtextarea.SearchContext;

/**
 * A toolbar for search operations in a text editor application. This provides a
 * more seamless experience than using a Find or Replace dialog.
 *
 * @author Robert Futrell
 * @version 0.5
 * @see FindDialog
 */
public class FindToolBar extends JPanel {

	/**
	 * Listens for events in the Find (and Replace, in the subclass) search field.
	 */
	protected class FindFieldListener extends KeyAdapter implements DocumentListener, FocusListener {

		protected boolean selectAll;

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		@Override
		public void focusGained(final FocusEvent e) {
			final JTextField field = (JTextField) e.getComponent();
			if (this.selectAll)
				field.selectAll();
			this.selectAll = true;
		}

		@Override
		public void focusLost(final FocusEvent e) {
		}

		protected void handleDocumentEvent(final DocumentEvent e) {
			FindToolBar.this.handleToggleButtons();
			if (!FindToolBar.this.settingFindTextFromEvent) {
				final JTextComponent findField = UIUtil.getTextComponent(FindToolBar.this.findCombo);
				if (e.getDocument() == findField.getDocument()) {
					FindToolBar.this.context.setSearchFor(findField.getText());
					if (FindToolBar.this.context.getMarkAll())
						FindToolBar.this.doMarkAll(true);
				} else { // Replace field's document
					final JTextComponent replaceField = UIUtil.getTextComponent(FindToolBar.this.replaceCombo);
					FindToolBar.this.context.setReplaceWith(replaceField.getText());
					// Don't re-fire "mark all" events for "replace" text edits
				}
			}
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

		public void install(final JTextComponent field) {
			field.getDocument().addDocumentListener(this);
			field.addKeyListener(this);
			field.addFocusListener(this);
		}

		@Override
		public void keyTyped(final KeyEvent e) {
			if (e.getKeyChar() == '\n') {
				final int mod = e.getModifiers();
				final int ctrlShift = InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK;
				final boolean forward = (mod & ctrlShift) == 0;
				FindToolBar.this.doSearch(forward);
			}
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

	}

	/**
	 * Called when the user edits the "Find" field's contents, after a slight delay.
	 * Fires a "mark all" search event for applications that want to display "mark
	 * all" results on the fly.
	 */
	private class MarkAllEventNotifier implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent e) {
			FindToolBar.this.fireMarkAllEvent();
		}

	}

	/**
	 * Listens for events in this tool bar. Keeps the UI in sync with the search
	 * context and vice versa.
	 */
	private class ToolBarListener extends MouseAdapter implements ActionListener, PropertyChangeListener {

		@Override
		public void actionPerformed(final ActionEvent e) {

			final Object source = e.getSource();

			if (source == FindToolBar.this.matchCaseCheckBox) {
				FindToolBar.this.context.setMatchCase(FindToolBar.this.matchCaseCheckBox.isSelected());
				if (FindToolBar.this.markAllCheckBox.isSelected())
					FindToolBar.this.doMarkAll(false);
			} else if (source == FindToolBar.this.wholeWordCheckBox) {
				FindToolBar.this.context.setWholeWord(FindToolBar.this.wholeWordCheckBox.isSelected());
				if (FindToolBar.this.markAllCheckBox.isSelected())
					FindToolBar.this.doMarkAll(false);
			} else if (source == FindToolBar.this.regexCheckBox) {
				FindToolBar.this.context.setRegularExpression(FindToolBar.this.regexCheckBox.isSelected());
				if (FindToolBar.this.markAllCheckBox.isSelected())
					FindToolBar.this.doMarkAll(false);
			} else if (source == FindToolBar.this.markAllCheckBox) {
				FindToolBar.this.context.setMarkAll(FindToolBar.this.markAllCheckBox.isSelected());
				FindToolBar.this.fireMarkAllEvent(); // Force an event to be fired
			} else
				FindToolBar.this.handleSearchAction(e);

		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			if (e.getSource() instanceof JCheckBox) { // Always true
				FindToolBar.this.findFieldListener.selectAll = false;
				FindToolBar.this.findCombo.requestFocusInWindow();
			}
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			// A property changed on the context itself.
			final String prop = e.getPropertyName();

			if (SearchContext.PROPERTY_MATCH_CASE.equals(prop)) {
				final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
				FindToolBar.this.matchCaseCheckBox.setSelected(newValue);
			} else if (SearchContext.PROPERTY_MATCH_WHOLE_WORD.equals(prop)) {
				final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
				FindToolBar.this.wholeWordCheckBox.setSelected(newValue);
			}
			// else if (SearchContext.PROPERTY_SEARCH_FORWARD.equals(prop)) {
			// boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
			// ...
			// }
			// else if (SearchContext.PROPERTY_SELECTION_ONLY.equals(prop)) {
			// boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
			// ...
			// }
			else if (SearchContext.PROPERTY_USE_REGEX.equals(prop)) {
				final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
				FindToolBar.this.regexCheckBox.setSelected(newValue);
				FindToolBar.this.handleRegExCheckBoxClicked();
			} else if (SearchContext.PROPERTY_MARK_ALL.equals(prop)) {
				final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
				FindToolBar.this.markAllCheckBox.setSelected(newValue);
				// firing event handled in ActionListener, to prevent "other"
				// tool bar from firing a second event
			} else if (SearchContext.PROPERTY_SEARCH_FOR.equals(prop)) {
				final String newValue = (String) e.getNewValue();
				final String oldValue = FindToolBar.this.getFindText();
				// Prevents IllegalStateExceptions
				if (!newValue.equals(oldValue)) {
					FindToolBar.this.settingFindTextFromEvent = true;
					FindToolBar.this.setFindText(newValue);
					FindToolBar.this.settingFindTextFromEvent = false;
				}
			} else if (SearchContext.PROPERTY_REPLACE_WITH.equals(prop)) {
				final String newValue = (String) e.getNewValue();
				final String oldValue = FindToolBar.this.getReplaceText();
				// Prevents IllegalStateExceptions
				if (!newValue.equals(oldValue)) {
					FindToolBar.this.settingFindTextFromEvent = true;
					FindToolBar.this.setReplaceText(newValue);
					FindToolBar.this.settingFindTextFromEvent = false;
				}
			}

		}

	}

	protected static final ResourceBundle msg = ResourceBundle.getBundle("org.fife.rsta.ui.search.SearchToolBar");
	protected static final ResourceBundle searchMsg = ResourceBundle.getBundle("org.fife.rsta.ui.search.Search");
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private SearchContext context;
	protected JButton findButton;
	protected SearchComboBox findCombo;
	protected FindFieldListener findFieldListener;
	protected JButton findPrevButton;
	private final JLabel infoLabel;
	protected ToolBarListener listener;
	protected JCheckBox markAllCheckBox;

	private final Timer markAllTimer;

	protected JCheckBox matchCaseCheckBox;
	protected JCheckBox regexCheckBox;

	protected SearchComboBox replaceCombo;

	/**
	 * Flag to prevent double-modification of SearchContext when e.g. a FindToolBar
	 * and ReplaceToolBar share the same SearchContext.
	 */
	private boolean settingFindTextFromEvent;

	protected JCheckBox wholeWordCheckBox;

	/**
	 * Creates the tool bar.
	 *
	 * @param listener
	 *            An entity listening for search events.
	 */
	public FindToolBar(final SearchListener listener) {

		// Keep focus in this component when tabbing through search controls
		this.setFocusCycleRoot(true);
		this.installKeyboardShortcuts();

		this.markAllTimer = new Timer(300, new MarkAllEventNotifier());
		this.markAllTimer.setRepeats(false);

		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
		this.addSearchListener(listener);
		this.listener = new ToolBarListener();

		// The user should set a shared instance between all subclass
		// instances, but to be safe we set individual ones.
		this.setSearchContext(new SearchContext());

		final ComponentOrientation orientation = ComponentOrientation.getOrientation(this.getLocale());

		this.add(Box.createHorizontalStrut(5));

		this.add(this.createFieldPanel());

		final Box rest = new Box(BoxLayout.LINE_AXIS);
		this.add(rest, BorderLayout.LINE_END);

		rest.add(Box.createHorizontalStrut(5));
		rest.add(this.createButtonPanel());
		rest.add(Box.createHorizontalStrut(15));

		this.infoLabel = new JLabel();
		rest.add(this.infoLabel);

		rest.add(Box.createHorizontalGlue());

		// Get ready to go.
		this.applyComponentOrientation(orientation);

	}

	/**
	 * Adds a {@link SearchListener} to this tool bar. This listener will be
	 * notified when find or replace operations are triggered.
	 *
	 * @param l
	 *            The listener to add.
	 * @see #removeSearchListener(SearchListener)
	 */
	public void addSearchListener(final SearchListener l) {
		this.listenerList.add(SearchListener.class, l);
	}

	protected Container createButtonPanel() {

		final Box panel = new Box(BoxLayout.LINE_AXIS);
		this.createFindButtons();

		// JPanel bp = new JPanel(new GridLayout(1,2, 5,0));
		// bp.add(findButton); bp.add(findPrevButton);
		final JPanel filler = new JPanel(new BorderLayout());
		filler.setBorder(BorderFactory.createEmptyBorder());
		filler.add(this.findButton);// bp);
		panel.add(filler);
		panel.add(Box.createHorizontalStrut(5));

		this.matchCaseCheckBox = this.createCB("MatchCase");
		panel.add(this.matchCaseCheckBox);

		this.regexCheckBox = this.createCB("RegEx");
		panel.add(this.regexCheckBox);

		this.wholeWordCheckBox = this.createCB("WholeWord");
		panel.add(this.wholeWordCheckBox);

		this.markAllCheckBox = this.createCB("MarkAll");
		panel.add(this.markAllCheckBox);

		return panel;

	}

	protected JCheckBox createCB(final String key) {
		final JCheckBox cb = new JCheckBox(FindToolBar.searchMsg.getString(key));
		cb.addActionListener(this.listener);
		cb.addMouseListener(this.listener);
		return cb;
	}

	/**
	 * Wraps the specified component in a panel with a leading "content assist
	 * available" icon in front of it.
	 *
	 * @param comp
	 *            The component with content assist.
	 * @return The wrapper panel.
	 */
	protected Container createContentAssistablePanel(final JComponent comp) {
		final JPanel temp = new JPanel(new BorderLayout());
		temp.add(comp);
		final AssistanceIconPanel aip = new AssistanceIconPanel(comp);
		temp.add(aip, BorderLayout.LINE_START);
		return temp;
	}

	protected Container createFieldPanel() {

		this.findFieldListener = new FindFieldListener();
		final JPanel temp = new JPanel(new BorderLayout());

		this.findCombo = new SearchComboBox(this, false);
		final JTextComponent findField = UIUtil.getTextComponent(this.findCombo);
		this.findFieldListener.install(findField);
		temp.add(this.createContentAssistablePanel(this.findCombo));

		return temp;
	}

	/**
	 * Creates the buttons for this tool bar.
	 */
	protected void createFindButtons() {

		this.findPrevButton = new JButton(FindToolBar.msg.getString("FindPrev"));
		this.makeEnterActivateButton(this.findPrevButton);
		this.findPrevButton.setActionCommand("FindPrevious");
		this.findPrevButton.addActionListener(this.listener);
		this.findPrevButton.setEnabled(false);

		this.findButton = new JButton(FindToolBar.searchMsg.getString("Find")) {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return FindToolBar.this.findPrevButton.getPreferredSize(); // Always bigger
			}
		};
		this.makeEnterActivateButton(this.findButton);
		this.findButton.setToolTipText(FindToolBar.msg.getString("Find.ToolTip"));
		this.findButton.setActionCommand("FindNext");
		this.findButton.addActionListener(this.listener);
		this.findButton.setEnabled(false);

	}

	/**
	 * Forces a "mark all" event to be sent out, if "mark all" is enabled.
	 *
	 * @param delay
	 *            If the delay should be honored.
	 */
	protected void doMarkAll(final boolean delay) {
		if (this.context.getMarkAll() && !this.settingFindTextFromEvent)
			if (delay)
				this.markAllTimer.restart();
			else
				this.fireMarkAllEvent();
	}

	void doSearch(final boolean forward) {
		if (forward)
			this.findButton.doClick(0);
		else
			this.findPrevButton.doClick(0);
	}

	/**
	 * Fires a "mark all" search event.
	 */
	private void fireMarkAllEvent() {
		final SearchEvent se = new SearchEvent(this, SearchEvent.Type.MARK_ALL, this.context);
		this.fireSearchEvent(se);
	}

	/**
	 * Notifies all listeners that have registered interest for notification on this
	 * event type. The event instance is lazily created using the <code>event</code>
	 * parameter.
	 *
	 * @param e
	 *            The <code>ActionEvent</code> object coming from a child component.
	 */
	protected void fireSearchEvent(final SearchEvent e) {
		// Process the listeners last to first, notifying
		// those that are interested in this event
		final SearchListener[] listeners = this.listenerList.getListeners(SearchListener.class);
		final int count = listeners == null ? 0 : listeners.length;
		for (int i = count - 1; i >= 0; i--)
			listeners[i].searchEvent(e);
	}

	protected String getFindText() {
		return UIUtil.getTextComponent(this.findCombo).getText();
	}

	/**
	 * Returns the delay between when the user types and when a "mark all" event is
	 * fired (assuming "mark all" is enabled), in milliseconds.
	 *
	 * @return The delay.
	 * @see #setMarkAllDelay(int)
	 */
	public int getMarkAllDelay() {
		return this.markAllTimer.getInitialDelay();
	}

	protected String getReplaceText() {
		if (this.replaceCombo == null)
			return null;
		return UIUtil.getTextComponent(this.replaceCombo).getText();
	}

	/**
	 * Returns the search context for this tool bar.
	 *
	 * @return The search context.
	 * @see #setSearchContext(SearchContext)
	 */
	public SearchContext getSearchContext() {
		return this.context;
	}

	/**
	 * Called when the regex checkbox is clicked (or its value is modified via a
	 * change to the search context). Subclasses can override to add custom
	 * behavior, but should call the super implementation.
	 */
	protected void handleRegExCheckBoxClicked() {
		this.handleToggleButtons();
		// "Content assist" support
		final boolean b = this.regexCheckBox.isSelected();
		this.findCombo.setAutoCompleteEnabled(b);
	}

	/**
	 * Creates a search event object and notifies all registered listeners.
	 *
	 * @param e
	 *            The event.
	 */
	protected void handleSearchAction(final ActionEvent e) {

		SearchEvent.Type type = null;
		boolean forward = true;
		final String action = e.getActionCommand();
		// JTextField returns *_DOWN_* modifiers, JButton returns the others (!)
		final int allowedModifiers = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | // field
				InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK; // JButton

		if ("FindNext".equals(action)) {
			type = SearchEvent.Type.FIND;
			final int mods = e.getModifiers();
			forward = (mods & allowedModifiers) == 0;
			// Add the item to the combo box's list, if it isn't already there.
			final JTextComponent tc = UIUtil.getTextComponent(this.findCombo);
			this.findCombo.addItem(tc.getText());
		} else if ("FindPrevious".equals(action)) {
			type = SearchEvent.Type.FIND;
			forward = false;
			// Add the item to the combo box's list, if it isn't already there.
			final JTextComponent tc = UIUtil.getTextComponent(this.findCombo);
			this.findCombo.addItem(tc.getText());
		} else if ("Replace".equals(action)) {
			type = SearchEvent.Type.REPLACE;
			final int mods = e.getModifiers();
			forward = (mods & allowedModifiers) == 0;
			// Add the item to the combo box's list, if it isn't already there.
			JTextComponent tc = UIUtil.getTextComponent(this.findCombo);
			this.findCombo.addItem(tc.getText());
			tc = UIUtil.getTextComponent(this.replaceCombo);
			this.replaceCombo.addItem(tc.getText());
		} else if ("ReplaceAll".equals(action)) {
			type = SearchEvent.Type.REPLACE_ALL;
			// Add the item to the combo box's list, if it isn't already there.
			JTextComponent tc = UIUtil.getTextComponent(this.findCombo);
			this.findCombo.addItem(tc.getText());
			tc = UIUtil.getTextComponent(this.replaceCombo);
			this.replaceCombo.addItem(tc.getText());
		}

		this.context.setSearchFor(this.getFindText());
		if (this.replaceCombo != null)
			this.context.setReplaceWith(this.replaceCombo.getSelectedString());

		// Note: This will toggle the "search forward" radio buttons in the
		// Find/Replace dialogs if the application is using them AND these tool
		// bars, but that is a rare occurrence. Cloning the context is out
		// since that may cause problems for the application if it caches it.
		this.context.setSearchForward(forward);

		final SearchEvent se = new SearchEvent(this, type, this.context);
		this.fireSearchEvent(se);
		this.handleToggleButtons(); // Replace button could toggle state

	}

	/**
	 * Returns whether any action-related buttons (Find Next, Replace, etc.) should
	 * be enabled. Subclasses can call this method when the "Find What" or "Replace
	 * With" text fields are modified. They can then enable/disable any components
	 * as appropriate.
	 *
	 * @return Whether the buttons should be enabled.
	 */
	protected FindReplaceButtonsEnableResult handleToggleButtons() {

		FindReplaceButtonsEnableResult result = new FindReplaceButtonsEnableResult(true, null);

		final String text = this.getFindText();
		if (text.length() == 0)
			result = new FindReplaceButtonsEnableResult(false, null);
		else if (this.regexCheckBox.isSelected())
			try {
				Pattern.compile(text);
			} catch (final PatternSyntaxException pse) {
				result = new FindReplaceButtonsEnableResult(false, pse.getMessage());
			}

		final boolean enable = result.getEnable();
		this.findButton.setEnabled(enable);
		this.findPrevButton.setEnabled(enable);

		// setBackground doesn't show up with XP Look and Feel!
		// findTextComboBox.setBackground(enable ?
		// UIManager.getColor("ComboBox.background") : Color.PINK);
		final JTextComponent tc = UIUtil.getTextComponent(this.findCombo);
		tc.setForeground(enable ? UIManager.getColor("TextField.foreground") : UIUtil.getErrorTextForeground());

		final String tooltip = SearchUtil.getToolTip(result);
		tc.setToolTipText(tooltip); // Always set, even if null

		return result;

	}

	/**
	 * Initializes the UI in this tool bar from a search context. This is called
	 * whenever a new search context is installed on this tool bar (which should
	 * practically be never).
	 */
	protected void initUIFromContext() {
		if (this.findCombo == null)
			return;
		this.setFindText(this.context.getSearchFor());
		if (this.replaceCombo != null)
			this.setReplaceText(this.context.getReplaceWith());
		this.matchCaseCheckBox.setSelected(this.context.getMatchCase());
		this.wholeWordCheckBox.setSelected(this.context.getWholeWord());
		this.regexCheckBox.setSelected(this.context.isRegularExpression());
		this.markAllCheckBox.setSelected(this.context.getMarkAll());
	}

	private void installKeyboardShortcuts() {

		final InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final ActionMap am = this.getActionMap();

		KeyStroke ks = KeyStroke.getKeyStroke("ENTER");
		im.put(ks, "searchForward");
		am.put("searchForward", new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				FindToolBar.this.doSearch(true);
			}
		});

		final int shift = InputEvent.SHIFT_MASK;
		int ctrl = InputEvent.CTRL_MASK;
		if (System.getProperty("os.name").toLowerCase().contains("os x"))
			ctrl = InputEvent.META_MASK;
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, shift);
		im.put(ks, "searchBackward");
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ctrl);
		im.put(ks, "searchBackward");
		am.put("searchForward", new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				FindToolBar.this.doSearch(false);
			}
		});

	}

	/**
	 * Makes the Enter key activate the button. In Swing, this is a complicated
	 * thing. It's LAF-dependent whether or not this works automatically; on most
	 * LAFs, it doesn't happen. In WindowsLookAndFeel it does, but *only* if the
	 * current window has a "default" button specified. Since these tool bars will
	 * typically be used in "main" application windows, which don't have default
	 * buttons, we'll just enable this property here and now.
	 *
	 * @param button
	 *            The button that should respond to the Enter key.
	 */
	protected void makeEnterActivateButton(final JButton button) {

		final InputMap im = button.getInputMap();

		// Make "enter" being typed simulate clicking
		im.put(KeyStroke.getKeyStroke("ENTER"), "pressed");
		im.put(KeyStroke.getKeyStroke("released ENTER"), "released");

		// Make "shift+enter" being typed simulate clicking also. The listener
		// will handle the backwards searching. Not sure why the commented-out
		// versions don't work, possibly SHIFT_MASK vs. SHIFT_DOWN_MASK issue.
		// im.put(KeyStroke.getKeyStroke("pressed SHIFT ENTER"), "pressed");
		// im.put(KeyStroke.getKeyStroke("released SHIFT ENTER"), "released");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK, false), "pressed");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK, true), "released");

	}

	/**
	 * Removes a {@link SearchListener} from this tool bar.
	 *
	 * @param l
	 *            The listener to remove
	 * @see #addSearchListener(SearchListener)
	 */
	public void removeSearchListener(final SearchListener l) {
		this.listenerList.remove(SearchListener.class, l);
	}

	/**
	 * Makes the find field on this toolbar request focus. If it is already focused,
	 * its text is selected.
	 */
	@Override
	public boolean requestFocusInWindow() {
		final JTextComponent findField = UIUtil.getTextComponent(this.findCombo);
		findField.selectAll();
		return findField.requestFocusInWindow();
	}

	/**
	 * Callback called when a contained combo box has its LookAndFeel modified. This
	 * is a hack for us to add listeners back to it.
	 *
	 * @param combo
	 *            The combo box.
	 */
	void searchComboUpdateUICallback(final SearchComboBox combo) {
		this.findFieldListener.install(UIUtil.getTextComponent(combo));
	}

	protected void setFindText(final String text) {
		UIUtil.getTextComponent(this.findCombo).setText(text);
		// findCombo.setSelectedItem(text);
	}

	/**
	 * Sets the delay between when the user types and when a "mark all" event is
	 * fired (assuming "mark all" is enabled), in milliseconds.
	 *
	 * @param millis
	 *            The new delay. This should be &gt;= zero.
	 * @see #getMarkAllDelay()
	 */
	public void setMarkAllDelay(final int millis) {
		this.markAllTimer.setInitialDelay(millis);
	}

	protected void setReplaceText(final String text) {
		if (this.replaceCombo != null)
			UIUtil.getTextComponent(this.replaceCombo).setText(text);
		// replaceCombo.setSelectedItem(text);
	}

	/**
	 * Sets the search context for this tool bar. You'll usually want to call this
	 * method for all tool bars and give them the same search context, so that their
	 * options (match case, etc.) stay in sync with one another.
	 *
	 * @param context
	 *            The new search context. This cannot be <code>null</code>.
	 * @see #getSearchContext()
	 */
	public void setSearchContext(final SearchContext context) {
		if (this.context != null)
			this.context.removePropertyChangeListener(this.listener);
		this.context = context;
		this.context.addPropertyChangeListener(this.listener);
		this.initUIFromContext();
	}

}