/*
 * 04/08/2004
 *
 * AbstractFindReplaceSearchDialog.java - Base class for FindDialog and
 * ReplaceDialog.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rtextarea.SearchContext;

/**
 * This is the base class for {@link FindDialog} and {@link ReplaceDialog}. It
 * is basically all of the features common to the two dialogs that weren't taken
 * care of in {@link AbstractSearchDialog}.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public abstract class AbstractFindReplaceDialog extends AbstractSearchDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Property fired when the user toggles the search direction radio buttons.
	 */
	public static final String SEARCH_DOWNWARD_PROPERTY = "SearchDialog.SearchDownward";

	protected JPanel dirPanel;
	private String dirPanelTitle;
	protected JRadioButton downButton;
	protected JLabel findFieldLabel;
	protected JButton findNextButton;
	/**
	 * Folks listening for events in this dialog.
	 */
	private EventListenerList listenerList;

	/**
	 * The "mark all" check box.
	 */
	protected JCheckBox markAllCheckBox;

	// The radio buttons for changing the search direction.
	protected JRadioButton upButton;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The dialog that owns this search dialog.
	 */
	public AbstractFindReplaceDialog(final Dialog owner) {
		super(owner);
		this.init();
	}

	/**
	 * Constructor. Does initializing for parts common to <code>FindDialog</code>
	 * and <code>ReplaceDialog</code> that isn't taken care of in
	 * <code>AbstractSearchDialog</code>'s constructor.
	 *
	 * @param owner
	 *            The window that owns this search dialog.
	 */
	public AbstractFindReplaceDialog(final Frame owner) {
		super(owner);
		this.init();
	}

	/**
	 * Listens for action events in this dialog.
	 *
	 * @param e
	 *            The event that occurred.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		final String command = e.getActionCommand();

		if ("UpRadioButtonClicked".equals(command))
			this.context.setSearchForward(false);
		else if ("DownRadioButtonClicked".equals(command))
			this.context.setSearchForward(true);
		else if ("MarkAll".equals(command)) {
			final boolean checked = this.markAllCheckBox.isSelected();
			this.context.setMarkAll(checked);
		}

		else if (SearchEvent.Type.FIND.name().equals(command)) {

			// Add the item to the combo box's list, if it isn't already there.
			final JTextComponent tc = UIUtil.getTextComponent(this.findTextCombo);
			this.findTextCombo.addItem(tc.getText());
			this.context.setSearchFor(this.getSearchString());

			this.fireSearchEvent(e); // Let parent application know

		} else
			super.actionPerformed(e);

	}

	/**
	 * Adds a {@link SearchListener} to this dialog. This listener will be notified
	 * when find or replace operations are triggered. For example, for a Replace
	 * dialog, a listener will receive notification when the user clicks "Find",
	 * "Replace", or "Replace All".
	 *
	 * @param l
	 *            The listener to add.
	 * @see #removeSearchListener(SearchListener)
	 */
	public void addSearchListener(final SearchListener l) {
		this.listenerList.add(SearchListener.class, l);
	}

	/**
	 * Notifies all listeners that have registered interest for notification on this
	 * event type. The event instance is lazily created using the <code>event</code>
	 * parameter.
	 *
	 * @param event
	 *            The <code>ActionEvent</code> object coming from a child component.
	 */
	protected void fireSearchEvent(final ActionEvent event) {
		// Guaranteed to return a non-null array
		final Object[] listeners = this.listenerList.getListenerList();
		SearchEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
			if (listeners[i] == SearchListener.class) {
				// Lazily create the event:
				if (e == null) {
					final String command = event.getActionCommand();
					final SearchEvent.Type type = SearchEvent.Type.valueOf(command);
					e = new SearchEvent(this, type, this.context);
				}
				((SearchListener) listeners[i + 1]).searchEvent(e);
			}
	}

	/**
	 * Returns the text for the "Down" radio button.
	 *
	 * @return The text for the "Down" radio button.
	 * @see #setDownRadioButtonText
	 */
	public final String getDownRadioButtonText() {
		return this.downButton.getText();
	}

	/**
	 * Returns the text on the "Find" button.
	 *
	 * @return The text on the Find button.
	 * @see #setFindButtonText
	 */
	public final String getFindButtonText() {
		return this.findNextButton.getText();
	}

	/**
	 * Returns the label on the "Find what" text field.
	 *
	 * @return The text on the "Find what" text field.
	 * @see #setFindWhatLabelText
	 */
	public final String getFindWhatLabelText() {
		return this.findFieldLabel.getText();
	}

	/**
	 * Returns the text for the search direction's radio buttons' border.
	 *
	 * @return The text for the search radio buttons' border.
	 * @see #setSearchButtonsBorderText
	 */
	public final String getSearchButtonsBorderText() {
		return this.dirPanelTitle;
	}

	/**
	 * Returns the text for the "Up" radio button.
	 *
	 * @return The text for the "Up" radio button.
	 * @see #setUpRadioButtonText
	 */
	public final String getUpRadioButtonText() {
		return this.upButton.getText();
	}

	/**
	 * Called whenever a property in the search context is modified. Subclasses
	 * should override if they listen for additional properties.
	 *
	 * @param e
	 *            The property change event fired.
	 */
	@Override
	protected void handleSearchContextPropertyChanged(final PropertyChangeEvent e) {

		final String prop = e.getPropertyName();

		if (SearchContext.PROPERTY_SEARCH_FORWARD.equals(prop)) {
			final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
			final JRadioButton button = newValue ? this.downButton : this.upButton;
			button.setSelected(true);
		}

		else if (SearchContext.PROPERTY_MARK_ALL.equals(prop)) {
			final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
			this.markAllCheckBox.setSelected(newValue);
		} else
			super.handleSearchContextPropertyChanged(e);

	}

	@Override
	protected FindReplaceButtonsEnableResult handleToggleButtons() {

		final FindReplaceButtonsEnableResult er = super.handleToggleButtons();
		final boolean enable = er.getEnable();

		this.findNextButton.setEnabled(enable);

		// setBackground doesn't show up with XP Look and Feel!
		// findTextComboBox.setBackground(enable ?
		// UIManager.getColor("ComboBox.background") : Color.PINK);
		final JTextComponent tc = UIUtil.getTextComponent(this.findTextCombo);
		tc.setForeground(enable ? UIManager.getColor("TextField.foreground") : UIUtil.getErrorTextForeground());

		final String tooltip = SearchUtil.getToolTip(er);
		tc.setToolTipText(tooltip); // Always set, even if null

		return er;

	}

	private void init() {

		this.listenerList = new EventListenerList();

		// Make a panel containing the "search up/down" radio buttons.
		this.dirPanel = new JPanel();
		this.dirPanel.setLayout(new BoxLayout(this.dirPanel, BoxLayout.LINE_AXIS));
		this.setSearchButtonsBorderText(AbstractSearchDialog.getString("Direction"));
		final ButtonGroup bg = new ButtonGroup();
		this.upButton = new JRadioButton(AbstractSearchDialog.getString("Up"), false);
		this.upButton.setMnemonic((int) AbstractSearchDialog.getString("UpMnemonic").charAt(0));
		this.downButton = new JRadioButton(AbstractSearchDialog.getString("Down"), true);
		this.downButton.setMnemonic((int) AbstractSearchDialog.getString("DownMnemonic").charAt(0));
		this.upButton.setActionCommand("UpRadioButtonClicked");
		this.upButton.addActionListener(this);
		this.downButton.setActionCommand("DownRadioButtonClicked");
		this.downButton.addActionListener(this);
		bg.add(this.upButton);
		bg.add(this.downButton);
		this.dirPanel.add(this.upButton);
		this.dirPanel.add(this.downButton);

		// Initialize the "mark all" button.
		this.markAllCheckBox = new JCheckBox(AbstractSearchDialog.getString("MarkAll"));
		this.markAllCheckBox.setMnemonic((int) AbstractSearchDialog.getString("MarkAllMnemonic").charAt(0));
		this.markAllCheckBox.setActionCommand("MarkAll");
		this.markAllCheckBox.addActionListener(this);

		// Rearrange the search conditions panel.
		this.searchConditionsPanel.removeAll();
		this.searchConditionsPanel.setLayout(new BorderLayout());
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.PAGE_AXIS));
		temp.add(this.caseCheckBox);
		temp.add(this.wholeWordCheckBox);
		this.searchConditionsPanel.add(temp, BorderLayout.LINE_START);
		temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.PAGE_AXIS));
		temp.add(this.regexCheckBox);
		temp.add(this.markAllCheckBox);
		this.searchConditionsPanel.add(temp, BorderLayout.LINE_END);

		// Create the "Find what" label.
		this.findFieldLabel = UIUtil.newLabel(this.getBundle(), "FindWhat", this.findTextCombo);

		// Create a "Find Next" button.
		this.findNextButton = UIUtil.newButton(this.getBundle(), "Find");
		this.findNextButton.setActionCommand(SearchEvent.Type.FIND.name());
		this.findNextButton.addActionListener(this);
		this.findNextButton.setDefaultCapable(true);
		this.findNextButton.setEnabled(false); // Initially, nothing to look for.

		this.installKeyboardActions();

	}

	/**
	 * Adds extra keyboard actions for Find and Replace dialogs.
	 */
	private void installKeyboardActions() {

		final JRootPane rootPane = this.getRootPane();
		final InputMap im = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final ActionMap am = rootPane.getActionMap();

		final int modifier = this.getToolkit().getMenuShortcutKeyMask();
		final KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier);
		im.put(ctrlF, "focusSearchForField");
		am.put("focusSearchForField", new AbstractAction() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				AbstractFindReplaceDialog.this.requestFocus();
			}
		});
	}

	/**
	 * Overridden to initialize UI elements specific to this subclass.
	 */
	@Override
	protected void refreshUIFromContext() {
		if (this.markAllCheckBox == null)
			return; // First time through, UI not realized yet
		super.refreshUIFromContext();
		this.markAllCheckBox.setSelected(this.context.getMarkAll());
		final boolean searchForward = this.context.getSearchForward();
		this.upButton.setSelected(!searchForward);
		this.downButton.setSelected(searchForward);
	}

	/**
	 * Removes a {@link SearchListener} from this dialog.
	 *
	 * @param l
	 *            The listener to remove
	 * @see #addSearchListener(SearchListener)
	 */
	public void removeSearchListener(final SearchListener l) {
		this.listenerList.remove(SearchListener.class, l);
	}

	/**
	 * Sets the text label for the "Down" radio button.
	 *
	 * @param text
	 *            The new text label for the "Down" radio button.
	 * @see #getDownRadioButtonText
	 */
	public void setDownRadioButtonText(final String text) {
		this.downButton.setText(text);
	}

	/**
	 * Sets the text on the "Find" button.
	 *
	 * @param text
	 *            The text for the Find button.
	 * @see #getFindButtonText
	 */
	public final void setFindButtonText(final String text) {
		this.findNextButton.setText(text);
	}

	/**
	 * Sets the label on the "Find what" text field.
	 *
	 * @param text
	 *            The text for the "Find what" text field's label.
	 * @see #getFindWhatLabelText
	 */
	public void setFindWhatLabelText(final String text) {
		this.findFieldLabel.setText(text);
	}

	/**
	 * Sets the text for the search direction's radio buttons' border.
	 *
	 * @param text
	 *            The text for the search radio buttons' border.
	 * @see #getSearchButtonsBorderText
	 */
	public final void setSearchButtonsBorderText(final String text) {
		this.dirPanelTitle = text;
		this.dirPanel.setBorder(this.createTitledBorder(this.dirPanelTitle));
	}

	/**
	 * Sets the text label for the "Up" radio button.
	 *
	 * @param text
	 *            The new text label for the "Up" radio button.
	 * @see #getUpRadioButtonText
	 */
	public void setUpRadioButtonText(final String text) {
		this.upButton.setText(text);
	}

}