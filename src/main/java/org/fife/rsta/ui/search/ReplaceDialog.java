/*
 * 11/14/2003
 *
 * ReplaceDialog.java - Dialog for replacing text in a GUI.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.AssistanceIconPanel;
import org.fife.rsta.ui.ResizableFrameContentPane;
import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

/**
 * A "Replace" dialog similar to those found in most Windows text editing
 * applications. Contains many search options, including:<br>
 * <ul>
 * <li>Match Case
 * <li>Match Whole Word
 * <li>Use Regular Expressions
 * <li>Search Forwards or Backwards
 * </ul>
 * The dialog also remembers your previous several selections in a combo box.
 * <p>
 * An application can use a <code>ReplaceDialog</code> as follows. It is
 * suggested that you create an <code>Action</code> or something similar to
 * facilitate "bringing up" the Replace dialog. Have the main application
 * contain an object that implements <code>ActionListener</code>. This object
 * will receive the following action events from the Replace dialog:
 * <ul>
 * <li>{@link SearchEvent.Type#FIND} action when the user clicks the "Find"
 * button.
 * <li>{@link SearchEvent.Type#REPLACE} action when the user clicks the
 * "Replace" button.
 * <li>{@link SearchEvent.Type#REPLACE_ALL} action when the user clicks the
 * "Replace All" button.
 * </ul>
 * The application can then call i.e.
 * {@link SearchEngine#find(javax.swing.JTextArea, org.fife.ui.rtextarea.SearchContext)
 * SearchEngine.find()} or
 * {@link SearchEngine#replace(org.fife.ui.rtextarea.RTextArea, org.fife.ui.rtextarea.SearchContext)
 * SearchEngine.replace()} to actually execute the search.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class ReplaceDialog extends AbstractFindReplaceDialog {

	/**
	 * Listens for changes in the text field (find search field).
	 */
	private class ReplaceDocumentListener implements DocumentListener {

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			final JTextComponent findWhatTextField = UIUtil.getTextComponent(ReplaceDialog.this.findTextCombo);
			if (e.getDocument().equals(findWhatTextField.getDocument()))
				ReplaceDialog.this.handleToggleButtons();
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			final JTextComponent findWhatTextField = UIUtil.getTextComponent(ReplaceDialog.this.findTextCombo);
			if (e.getDocument().equals(findWhatTextField.getDocument()) && e.getDocument().getLength() == 0) {
				ReplaceDialog.this.findNextButton.setEnabled(false);
				ReplaceDialog.this.replaceButton.setEnabled(false);
				ReplaceDialog.this.replaceAllButton.setEnabled(false);
			} else
				ReplaceDialog.this.handleToggleButtons();
		}

	}

	/**
	 * Listens for the text fields gaining focus.
	 */
	private class ReplaceFocusAdapter extends FocusAdapter {

		@Override
		public void focusGained(final FocusEvent e) {

			final JTextComponent textField = (JTextComponent) e.getSource();
			textField.selectAll();

			if (textField == UIUtil.getTextComponent(ReplaceDialog.this.findTextCombo))
				// Remember what it originally was, in case they tabbed out.
				ReplaceDialog.this.lastSearchString = ReplaceDialog.this.findTextCombo.getSelectedString();
			else
				// Remember what it originally was, in case they tabbed out.
				ReplaceDialog.this.lastReplaceString = ReplaceDialog.this.replaceWithCombo.getSelectedString();

			// Replace button's state might need to be changed.
			ReplaceDialog.this.handleToggleButtons();

		}

	}

	/**
	 * Listens for key presses in the replace dialog.
	 */
	private class ReplaceKeyListener extends KeyAdapter {

		@Override
		public void keyReleased(final KeyEvent e) {

			// This is an ugly hack to get around JComboBox's insistence on
			// eating the first Enter keypress it receives when it has focus.
			if (e.getKeyCode() == KeyEvent.VK_ENTER && AbstractSearchDialog.isPreJava6JRE())
				if (e.getSource() == UIUtil.getTextComponent(ReplaceDialog.this.findTextCombo)) {
					final String replaceString = ReplaceDialog.this.replaceWithCombo.getSelectedString();
					ReplaceDialog.this.lastReplaceString = replaceString; // Just in case it changed too.
					final String searchString = ReplaceDialog.this.findTextCombo.getSelectedString();
					if (!searchString.equals(ReplaceDialog.this.lastSearchString)) {
						ReplaceDialog.this.findNextButton.doClick(0);
						ReplaceDialog.this.lastSearchString = searchString;
						UIUtil.getTextComponent(ReplaceDialog.this.findTextCombo).selectAll();
					}
				} else { // if (e.getSource()==getTextComponent(replaceWithComboBox)) {
					final String searchString = ReplaceDialog.this.findTextCombo.getSelectedString();
					ReplaceDialog.this.lastSearchString = searchString; // Just in case it changed too.
					final String replaceString = ReplaceDialog.this.replaceWithCombo.getSelectedString();
					if (!replaceString.equals(ReplaceDialog.this.lastReplaceString)) {
						ReplaceDialog.this.findNextButton.doClick(0);
						ReplaceDialog.this.lastReplaceString = replaceString;
						UIUtil.getTextComponent(ReplaceDialog.this.replaceWithCombo).selectAll();
					}
				}

		}

	}

	private static final long serialVersionUID = 1L;

	private String lastReplaceString;

	// This helps us work around the "bug" where JComboBox eats the first Enter
	// press.
	private String lastSearchString;
	private JButton replaceAllButton;

	private JButton replaceButton;

	private JLabel replaceFieldLabel;

	private SearchComboBox replaceWithCombo;

	/**
	 * Our search listener, cached so we can grab its selected text easily.
	 */
	protected SearchListener searchListener;

	/**
	 * Creates a new <code>ReplaceDialog</code>.
	 *
	 * @param owner
	 *            The main window that owns this dialog.
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	public ReplaceDialog(final Dialog owner, final SearchListener listener) {
		super(owner);
		this.init(listener);
	}

	/**
	 * Creates a new <code>ReplaceDialog</code>.
	 *
	 * @param owner
	 *            The main window that owns this dialog.
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	public ReplaceDialog(final Frame owner, final SearchListener listener) {
		super(owner);
		this.init(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		final String command = e.getActionCommand();

		if (SearchEvent.Type.REPLACE.name().equals(command) || SearchEvent.Type.REPLACE_ALL.name().equals(command)) {

			this.context.setSearchFor(this.getSearchString());
			this.context.setReplaceWith(this.replaceWithCombo.getSelectedString());

			JTextComponent tc = UIUtil.getTextComponent(this.findTextCombo);
			this.findTextCombo.addItem(tc.getText());

			tc = UIUtil.getTextComponent(this.replaceWithCombo);
			final String replaceText = tc.getText();
			if (replaceText.length() != 0)
				this.replaceWithCombo.addItem(replaceText);

			this.fireSearchEvent(e); // Let parent application know

		}

		else {
			super.actionPerformed(e);
			if (SearchEvent.Type.FIND.name().equals(command))
				this.handleToggleButtons(); // Replace button could toggle state
		}

	}

	@Override
	protected void escapePressed() {
		// Workaround for the strange behavior (Java bug?) that sometimes
		// the Escape keypress "gets through" from the AutoComplete's
		// registered key Actions, and gets to this EscapableDialog, which
		// hides the entire dialog. Reproduce by doing the following:
		// 1. In an empty find field, press Ctrl+Space
		// 2. Type "\\".
		// 3. Press Escape.
		// The entire dialog will hide, instead of the completion popup.
		// Further, bringing the Find dialog back up, the completion popup
		// will still be visible.
		if (this.replaceWithCombo.hideAutoCompletePopups())
			return;
		super.escapePressed();
	}

	/**
	 * Returns the text on the "Replace All" button.
	 *
	 * @return The text on the Replace All button.
	 * @see #setReplaceAllButtonText
	 */
	public final String getReplaceAllButtonText() {
		return this.replaceAllButton.getText();
	}

	/**
	 * Returns the text on the "Replace" button.
	 *
	 * @return The text on the Replace button.
	 * @see #setReplaceButtonText
	 */
	public final String getReplaceButtonText() {
		return this.replaceButton.getText();
	}

	/**
	 * Returns the <code>java.lang.String</code> to replace with.
	 *
	 * @return The <code>String</code> the user wants to replace the text to find
	 *         with.
	 */
	public String getReplaceString() {
		String text = this.replaceWithCombo.getSelectedString();
		if (text == null)
			text = "";
		return text;
	}

	/**
	 * Returns the label on the "Replace with" text field.
	 *
	 * @return The text on the "Replace with" text field.
	 * @see #setReplaceWithLabelText
	 */
	public final String getReplaceWithLabelText() {
		return this.replaceFieldLabel.getText();
	}

	/**
	 * Called when the regex checkbox is clicked. Subclasses can override to add
	 * custom behavior, but should call the super implementation.
	 */
	@Override
	protected void handleRegExCheckBoxClicked() {
		super.handleRegExCheckBoxClicked();
		// "Content assist" support
		final boolean b = this.regexCheckBox.isSelected();
		// Always true except when debugging. findTextCombo done in parent
		this.replaceWithCombo.setAutoCompleteEnabled(b);
	}

	@Override
	protected void handleSearchContextPropertyChanged(final PropertyChangeEvent e) {

		final String prop = e.getPropertyName();

		if (SearchContext.PROPERTY_REPLACE_WITH.equals(prop)) {
			String newValue = (String) e.getNewValue();
			if (newValue == null)
				newValue = "";
			final String oldValue = this.getReplaceString();
			// Prevents IllegalStateExceptions
			if (!newValue.equals(oldValue))
				this.setReplaceString(newValue);
		} else
			super.handleSearchContextPropertyChanged(e);

	}

	@Override
	protected FindReplaceButtonsEnableResult handleToggleButtons() {

		final FindReplaceButtonsEnableResult er = super.handleToggleButtons();
		boolean shouldReplace = er.getEnable();
		this.replaceAllButton.setEnabled(shouldReplace);

		// "Replace" is only enabled if text to search for is selected in
		// the UI.
		if (shouldReplace) {
			final String text = this.searchListener.getSelectedText();
			shouldReplace = this.matchesSearchFor(text);
		}
		this.replaceButton.setEnabled(shouldReplace);

		return er;

	}

	/**
	 * Does replace dialog-specific initialization stuff.
	 *
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	private void init(final SearchListener listener) {

		this.searchListener = listener;

		final ComponentOrientation orientation = ComponentOrientation.getOrientation(this.getLocale());

		// Create a panel for the "Find what" and "Replace with" text fields.
		final JPanel searchPanel = new JPanel(new SpringLayout());

		// Create listeners for the combo boxes.
		final ReplaceFocusAdapter replaceFocusAdapter = new ReplaceFocusAdapter();
		final ReplaceKeyListener replaceKeyListener = new ReplaceKeyListener();
		final ReplaceDocumentListener replaceDocumentListener = new ReplaceDocumentListener();

		// Create the "Find what" text field.
		JTextComponent textField = UIUtil.getTextComponent(this.findTextCombo);
		textField.addFocusListener(replaceFocusAdapter);
		textField.addKeyListener(replaceKeyListener);
		textField.getDocument().addDocumentListener(replaceDocumentListener);

		// Create the "Replace with" text field.
		this.replaceWithCombo = new SearchComboBox(null, true);
		textField = UIUtil.getTextComponent(this.replaceWithCombo);
		textField.addFocusListener(replaceFocusAdapter);
		textField.addKeyListener(replaceKeyListener);
		textField.getDocument().addDocumentListener(replaceDocumentListener);

		// Create the "Replace with" label.
		this.replaceFieldLabel = UIUtil.newLabel(this.getBundle(), "ReplaceWith", this.replaceWithCombo);

		JPanel temp = new JPanel(new BorderLayout());
		temp.add(this.findTextCombo);
		final AssistanceIconPanel aip = new AssistanceIconPanel(this.findTextCombo);
		temp.add(aip, BorderLayout.LINE_START);
		final JPanel temp2 = new JPanel(new BorderLayout());
		temp2.add(this.replaceWithCombo);
		final AssistanceIconPanel aip2 = new AssistanceIconPanel(this.replaceWithCombo);
		temp2.add(aip2, BorderLayout.LINE_START);

		// Orient things properly.
		if (orientation.isLeftToRight()) {
			searchPanel.add(this.findFieldLabel);
			searchPanel.add(temp);
			searchPanel.add(this.replaceFieldLabel);
			searchPanel.add(temp2);
		} else {
			searchPanel.add(temp);
			searchPanel.add(this.findFieldLabel);
			searchPanel.add(temp2);
			searchPanel.add(this.replaceFieldLabel);
		}

		UIUtil.makeSpringCompactGrid(searchPanel, 2, 2, // rows, cols
				0, 0, // initX, initY
				6, 6); // xPad, yPad

		// Make a panel containing the inherited search direction radio
		// buttons and the inherited search options.
		final JPanel bottomPanel = new JPanel(new BorderLayout());
		temp = new JPanel(new BorderLayout());
		bottomPanel.setBorder(UIUtil.getEmpty5Border());
		temp.add(this.searchConditionsPanel, BorderLayout.LINE_START);
		temp.add(this.dirPanel);
		bottomPanel.add(temp, BorderLayout.LINE_START);

		// Now, make a panel containing all the above stuff.
		final JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(searchPanel);
		leftPanel.add(bottomPanel);

		// Make a panel containing the action buttons.
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(4, 1, 5, 5));
		final ResourceBundle msg = this.getBundle();
		this.replaceButton = UIUtil.newButton(msg, "Replace");
		this.replaceButton.setActionCommand(SearchEvent.Type.REPLACE.name());
		this.replaceButton.addActionListener(this);
		this.replaceButton.setEnabled(false);
		this.replaceButton.setIcon(null);
		this.replaceButton.setToolTipText(null);
		this.replaceAllButton = UIUtil.newButton(msg, "ReplaceAll");
		this.replaceAllButton.setActionCommand(SearchEvent.Type.REPLACE_ALL.name());
		this.replaceAllButton.addActionListener(this);
		this.replaceAllButton.setEnabled(false);
		this.replaceAllButton.setIcon(null);
		this.replaceAllButton.setToolTipText(null);
		buttonPanel.add(this.findNextButton);
		buttonPanel.add(this.replaceButton);
		buttonPanel.add(this.replaceAllButton);
		buttonPanel.add(this.cancelButton); // Defined in superclass.
		final JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(buttonPanel, BorderLayout.NORTH);

		// Put it all together!
		final JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		contentPane.add(leftPanel);
		contentPane.add(rightPanel, BorderLayout.LINE_END);
		temp = new ResizableFrameContentPane(new BorderLayout());
		temp.add(contentPane, BorderLayout.NORTH);
		this.setContentPane(temp);
		this.getRootPane().setDefaultButton(this.findNextButton);
		this.setTitle(AbstractSearchDialog.getString("ReplaceDialogTitle"));
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo(this.getParent());

		this.setSearchContext(new SearchContext());
		this.addSearchListener(listener);

		this.applyComponentOrientation(orientation);

	}

	/**
	 * Sets the text on the "Replace All" button.
	 *
	 * @param text
	 *            The text for the Replace All button.
	 * @see #getReplaceAllButtonText
	 */
	public final void setReplaceAllButtonText(final String text) {
		this.replaceAllButton.setText(text);
	}

	/**
	 * Sets the text on the "Replace" button.
	 *
	 * @param text
	 *            The text for the Replace button.
	 * @see #getReplaceButtonText
	 */
	public final void setReplaceButtonText(final String text) {
		this.replaceButton.setText(text);
	}

	/**
	 * Sets the <code>java.lang.String</code> to replace with.
	 *
	 * @param newReplaceString
	 *            The <code>String</code> to put into the replace field.
	 */
	public void setReplaceString(final String newReplaceString) {
		this.replaceWithCombo.addItem(newReplaceString);
	}

	/**
	 * Sets the label on the "Replace with" text field.
	 *
	 * @param text
	 *            The text for the "Replace with" text field's label.
	 * @see #getReplaceWithLabelText
	 */
	public final void setReplaceWithLabelText(final String text) {
		this.replaceFieldLabel.setText(text);
	}

	/**
	 * Overrides <code>JDialog</code>'s <code>setVisible</code> method; decides
	 * whether or not buttons are enabled.
	 *
	 * @param visible
	 *            Whether or not the dialog should be visible.
	 */
	@Override
	public void setVisible(final boolean visible) {

		if (visible) {

			// Select text entered in the UI
			final String text = this.searchListener.getSelectedText();
			if (text != null)
				this.findTextCombo.addItem(text);

			final String selectedItem = this.findTextCombo.getSelectedString();
			if (selectedItem == null || selectedItem.length() == 0) {
				this.findNextButton.setEnabled(false);
				this.replaceButton.setEnabled(false);
				this.replaceAllButton.setEnabled(false);
			} else
				this.handleToggleButtons();

			super.setVisible(true);
			this.focusFindTextField();

		} else
			super.setVisible(false);

	}

	/**
	 * This method should be called whenever the <code>LookAndFeel</code> of the
	 * application changes. This calls
	 * <code>SwingUtilities.updateComponentTreeUI(this)</code> and does other
	 * necessary things.
	 * <p>
	 * Note that this is <em>not</em> an override, as JDialogs don't have an
	 * <code>updateUI()</code> method.
	 */
	public void updateUI() {

		SwingUtilities.updateComponentTreeUI(this);
		this.pack();

		// Create listeners for the combo boxes.
		final ReplaceFocusAdapter replaceFocusAdapter = new ReplaceFocusAdapter();
		final ReplaceKeyListener replaceKeyListener = new ReplaceKeyListener();
		final ReplaceDocumentListener replaceDocumentListener = new ReplaceDocumentListener();

		// Fix the Find What combo box's listeners.
		JTextComponent textField = UIUtil.getTextComponent(this.findTextCombo);
		textField.addFocusListener(replaceFocusAdapter);
		textField.addKeyListener(replaceKeyListener);
		textField.getDocument().addDocumentListener(replaceDocumentListener);

		// Fix the Replace With combo box's listeners.
		textField = UIUtil.getTextComponent(this.replaceWithCombo);
		textField.addFocusListener(replaceFocusAdapter);
		textField.addKeyListener(replaceKeyListener);
		textField.getDocument().addDocumentListener(replaceDocumentListener);

	}

}