/*
 * 04/08/2004
 *
 * AbstractSearchDialog.java - Base class for all search dialogs
 * (find, replace, etc.).
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.EscapableDialog;
import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.SearchContext;

/**
 * Base class for all search dialogs (find, replace, find in files, etc.). This
 * class is not useful on its own; you should use either FindDialog or
 * ReplaceDialog, or extend this class to create your own search dialog.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class AbstractSearchDialog extends EscapableDialog implements ActionListener {

	/**
	 * Listens for properties changing in the search context.
	 */
	private class SearchContextListener implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent e) {
			AbstractSearchDialog.this.handleSearchContextPropertyChanged(e);
		}

	}

	/**
	 * The image to use beside a text component when content assist is available.
	 */
	private static Image contentAssistImage;
	private static final ResourceBundle msg = ResourceBundle.getBundle("org.fife.rsta.ui.search.Search");

	private static final long serialVersionUID = 1L;

	/**
	 * Returns the image to display beside text components when content assist is
	 * available.
	 *
	 * @return The image to use.
	 */
	public static Image getContentAssistImage() {
		if (AbstractSearchDialog.contentAssistImage == null) {
			final URL url = AbstractSearchDialog.class.getResource("lightbulb.png");
			try {
				AbstractSearchDialog.contentAssistImage = ImageIO.read(url);
			} catch (final IOException ioe) { // Never happens
				ioe.printStackTrace();
			}
		}
		return AbstractSearchDialog.contentAssistImage;
	}

	public static String getString(final String key) {
		return AbstractSearchDialog.msg.getString(key);
	}

	/**
	 * This method allows us to check if the current JRE is 1.4 or 1.5. This is used
	 * to workaround some Java bugs, for example, pre 1.6, JComboBoxes would
	 * "swallow" enter key presses in them when their content changed. This causes
	 * the user to have to press Enter twice when entering text to search for in a
	 * "Find" dialog, so instead we detect if a JRE is old enough to have this
	 * behavior and, if so, programmitcally press the Find button.
	 *
	 * @return Whether this is a 1.4 or 1.5 JRE.
	 */
	protected static boolean isPreJava6JRE() {
		// We only support 1.4+, so no need to check 1.3, etc.
		final String version = System.getProperty("java.specification.version");
		return version.startsWith("1.5") || version.startsWith("1.4");
	}

	/**
	 * Returns whether the characters on either side of
	 * <code>substr(searchIn,startPos,startPos+searchStringLength)</code> are
	 * whitespace. While this isn't the best definition of "whole word", it's the
	 * one we're going to use for now.
	 *
	 * @param searchIn
	 *            The text to search in.
	 * @param offset
	 *            The offset of the possible word.
	 * @param len
	 *            The length of the possible word.
	 * @return Whether the specified range represents a "whole word".
	 */
	public static final boolean isWholeWord(final CharSequence searchIn, final int offset, final int len) {

		boolean wsBefore, wsAfter;

		try {
			wsBefore = Character.isWhitespace(searchIn.charAt(offset - 1));
		} catch (final IndexOutOfBoundsException e) {
			wsBefore = true;
		}
		try {
			wsAfter = Character.isWhitespace(searchIn.charAt(offset + len));
		} catch (final IndexOutOfBoundsException e) {
			wsAfter = true;
		}

		return wsBefore && wsAfter;

	}

	// Miscellaneous other stuff.
	protected JButton cancelButton;

	// Conditions check boxes and the panel they go in.
	// This should be added in the actual layout of the search dialog.
	protected JCheckBox caseCheckBox;

	protected SearchContext context;

	private SearchContextListener contextListener;

	/**
	 * The combo box where the user enters the text for which to search.
	 */
	protected SearchComboBox findTextCombo;

	protected JCheckBox regexCheckBox;

	protected JPanel searchConditionsPanel;

	protected JCheckBox wholeWordCheckBox;

	/**
	 * Constructor. Does initializing for parts common to all search dialogs.
	 *
	 * @param owner
	 *            The dialog that owns this search dialog.
	 */
	public AbstractSearchDialog(final Dialog owner) {
		super(owner);
		this.init();
	}

	/**
	 * Constructor. Does initializing for parts common to all search dialogs.
	 *
	 * @param owner
	 *            The window that owns this search dialog.
	 */
	public AbstractSearchDialog(final Frame owner) {
		super(owner);
		this.init();
	}

	/**
	 * Listens for actions in this search dialog.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		final String command = e.getActionCommand();

		// They check/uncheck the "Match Case" checkbox on the Find dialog.
		if (command.equals("FlipMatchCase")) {
			final boolean matchCase = this.caseCheckBox.isSelected();
			this.context.setMatchCase(matchCase);
		}

		// They check/uncheck the "Whole word" checkbox on the Find dialog.
		else if (command.equals("FlipWholeWord")) {
			final boolean wholeWord = this.wholeWordCheckBox.isSelected();
			this.context.setWholeWord(wholeWord);
		}

		// They check/uncheck the "Regular expression" checkbox.
		else if (command.equals("FlipRegEx")) {
			final boolean useRegEx = this.regexCheckBox.isSelected();
			this.context.setRegularExpression(useRegEx);
		}

		// If they press the "Cancel" button.
		else if (command.equals("Cancel"))
			this.setVisible(false);

	}

	private JCheckBox createCheckBox(final ResourceBundle msg, final String keyRoot) {
		final JCheckBox cb = new JCheckBox(msg.getString(keyRoot));
		cb.setMnemonic((int) msg.getString(keyRoot + "Mnemonic").charAt(0));
		cb.setActionCommand("Flip" + keyRoot);
		cb.addActionListener(this);
		return cb;
	}

	/**
	 * Returns the default search context to use for this dialog. Applications that
	 * create new subclasses of this class can provide customized search contexts
	 * here.
	 *
	 * @return The default search context.
	 */
	protected SearchContext createDefaultSearchContext() {
		return new SearchContext();
	}

	/**
	 * Returns a titled border for panels on search dialogs.
	 *
	 * @param title
	 *            The title for the border.
	 * @return The border.
	 */
	protected Border createTitledBorder(String title) {
		if (title != null && title.charAt(title.length() - 1) != ':')
			title += ":";
		return BorderFactory.createTitledBorder(title);
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
		if (this.findTextCombo.hideAutoCompletePopups())
			return;
		super.escapePressed();
	}

	/**
	 * Makes the "Find text" field active.
	 */
	protected void focusFindTextField() {
		final JTextComponent textField = UIUtil.getTextComponent(this.findTextCombo);
		textField.requestFocusInWindow();
		textField.selectAll();
	}

	protected ResourceBundle getBundle() {
		return AbstractSearchDialog.msg;
	}

	/**
	 * Returns the text on the Cancel button.
	 *
	 * @return The text on the Cancel button.
	 * @see #setCancelButtonText
	 */
	public final String getCancelButtonText() {
		return this.cancelButton.getText();
	}

	/**
	 * Returns the text for the "Match Case" check box.
	 *
	 * @return The text for the "Match Case" check box.
	 * @see #setMatchCaseCheckboxText
	 */
	public final String getMatchCaseCheckboxText() {
		return this.caseCheckBox.getText();
	}

	/**
	 * Returns the text for the "Regular Expression" check box.
	 *
	 * @return The text for the "Regular Expression" check box.
	 * @see #setRegularExpressionCheckboxText
	 */
	public final String getRegularExpressionCheckboxText() {
		return this.regexCheckBox.getText();
	}

	/**
	 * Returns the search context used by this dialog.
	 *
	 * @return The search context.
	 * @see #setSearchContext(SearchContext)
	 */
	public SearchContext getSearchContext() {
		return this.context;
	}

	/**
	 * Returns the text to search for.
	 *
	 * @return The text the user wants to search for.
	 */
	public String getSearchString() {
		return this.findTextCombo.getSelectedString();
	}

	/**
	 * Returns the text for the "Whole Word" check box.
	 *
	 * @return The text for the "Whole Word" check box.
	 * @see #setWholeWordCheckboxText
	 */
	public final String getWholeWordCheckboxText() {
		return this.wholeWordCheckBox.getText();
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
		this.findTextCombo.setAutoCompleteEnabled(b);
	}

	/**
	 * Called whenever a property in the search context is modified. Subclasses
	 * should override if they listen for additional properties.
	 *
	 * @param e
	 *            The property change event fired.
	 */
	protected void handleSearchContextPropertyChanged(final PropertyChangeEvent e) {

		// A property changed on the context itself.
		final String prop = e.getPropertyName();

		if (SearchContext.PROPERTY_MATCH_CASE.equals(prop)) {
			final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
			this.caseCheckBox.setSelected(newValue);
		} else if (SearchContext.PROPERTY_MATCH_WHOLE_WORD.equals(prop)) {
			final boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
			this.wholeWordCheckBox.setSelected(newValue);
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
			this.regexCheckBox.setSelected(newValue);
			this.handleRegExCheckBoxClicked();
		} else if (SearchContext.PROPERTY_SEARCH_FOR.equals(prop)) {
			final String newValue = (String) e.getNewValue();
			final String oldValue = this.getSearchString();
			// Prevents IllegalStateExceptions
			if (!newValue.equals(oldValue))
				this.setSearchString(newValue);
		}

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

		// String text = getSearchString();
		final JTextComponent tc = UIUtil.getTextComponent(this.findTextCombo);
		final String text = tc.getText();
		if (text.length() == 0)
			return new FindReplaceButtonsEnableResult(false, null);
		if (this.regexCheckBox.isSelected())
			try {
				Pattern.compile(text);
			} catch (final PatternSyntaxException pse) {
				return new FindReplaceButtonsEnableResult(false, pse.getMessage());
			}
		return new FindReplaceButtonsEnableResult(true, null);
	}

	private void init() {

		// The user should set a shared instance between all subclass
		// instances, but to be safe we set individual ones.
		this.contextListener = new SearchContextListener();
		this.setSearchContext(this.createDefaultSearchContext());

		// Make a panel containing the option check boxes.
		this.searchConditionsPanel = new JPanel();
		this.searchConditionsPanel.setLayout(new BoxLayout(this.searchConditionsPanel, BoxLayout.Y_AXIS));
		this.caseCheckBox = this.createCheckBox(AbstractSearchDialog.msg, "MatchCase");
		this.searchConditionsPanel.add(this.caseCheckBox);
		this.wholeWordCheckBox = this.createCheckBox(AbstractSearchDialog.msg, "WholeWord");
		this.searchConditionsPanel.add(this.wholeWordCheckBox);
		this.regexCheckBox = this.createCheckBox(AbstractSearchDialog.msg, "RegEx");
		this.searchConditionsPanel.add(this.regexCheckBox);

		// Initialize any text fields.
		this.findTextCombo = new SearchComboBox(null, false);

		// Initialize other stuff.
		this.cancelButton = new JButton(AbstractSearchDialog.getString("Cancel"));
		// cancelButton.setMnemonic((int)getString("CancelMnemonic").charAt(0));
		this.cancelButton.setActionCommand("Cancel");
		this.cancelButton.addActionListener(this);

	}

	protected boolean matchesSearchFor(final String text) {
		if (text == null || text.length() == 0)
			return false;
		final String searchFor = this.findTextCombo.getSelectedString();
		if (searchFor != null && searchFor.length() > 0) {
			final boolean matchCase = this.caseCheckBox.isSelected();
			if (this.regexCheckBox.isSelected()) {
				int flags = Pattern.MULTILINE; // '^' and '$' are done per line.
				flags = RSyntaxUtilities.getPatternFlags(matchCase, flags);
				Pattern pattern = null;
				try {
					pattern = Pattern.compile(searchFor, flags);
				} catch (final PatternSyntaxException pse) {
					pse.printStackTrace(); // Never happens
					return false;
				}
				return pattern.matcher(text).matches();
			} else {
				if (matchCase)
					return searchFor.equals(text);
				return searchFor.equalsIgnoreCase(text);
			}
		}
		return false;
	}

	/**
	 * Initializes the UI in this tool bar from a search context. This is called
	 * whenever a new search context is installed on this tool bar (which should
	 * practically be never).
	 */
	protected void refreshUIFromContext() {
		if (this.caseCheckBox == null)
			return; // First time through, UI not realized yet
		this.caseCheckBox.setSelected(this.context.getMatchCase());
		this.regexCheckBox.setSelected(this.context.isRegularExpression());
		this.wholeWordCheckBox.setSelected(this.context.getWholeWord());
	}

	/**
	 * Overridden to ensure the "Find text" field gets focused.
	 */
	@Override
	public void requestFocus() {
		super.requestFocus();
		this.focusFindTextField();
	}

	/**
	 * Sets the text on the Cancel button.
	 *
	 * @param text
	 *            The text for the Cancel button.
	 * @see #getCancelButtonText
	 */
	public final void setCancelButtonText(final String text) {
		this.cancelButton.setText(text);
	}

	/**
	 * Sets the text for the "Match Case" check box.
	 *
	 * @param text
	 *            The text for the "Match Case" check box.
	 * @see #getMatchCaseCheckboxText
	 */
	public final void setMatchCaseCheckboxText(final String text) {
		this.caseCheckBox.setText(text);
	}

	/**
	 * Sets the text for the "Regular Expression" check box.
	 *
	 * @param text
	 *            The text for the "Regular Expression" check box.
	 * @see #getRegularExpressionCheckboxText
	 */
	public final void setRegularExpressionCheckboxText(final String text) {
		this.regexCheckBox.setText(text);
	}

	/**
	 * Sets the search context for this dialog. You'll usually want to call this
	 * method for all search dialogs and give them the same search context, so that
	 * their options (match case, etc.) stay in sync with one another.
	 *
	 * @param context
	 *            The new search context. This cannot be <code>null</code>.
	 * @see #getSearchContext()
	 */
	public void setSearchContext(final SearchContext context) {
		if (this.context != null)
			this.context.removePropertyChangeListener(this.contextListener);
		this.context = context;
		this.context.addPropertyChangeListener(this.contextListener);
		this.refreshUIFromContext();
	}

	/**
	 * Sets the <code>java.lang.String</code> to search for.
	 *
	 * @param newSearchString
	 *            The <code>tring</code> to put into the search field.
	 */
	public void setSearchString(final String newSearchString) {
		this.findTextCombo.addItem(newSearchString);
	}

	/**
	 * Sets the text for the "Whole Word" check box.
	 *
	 * @param text
	 *            The text for the "Whole Word" check box.
	 * @see #getWholeWordCheckboxText
	 */
	public final void setWholeWordCheckboxText(final String text) {
		this.wholeWordCheckBox.setText(text);
	}

}