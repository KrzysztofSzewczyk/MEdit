/*
 * 09/20/2013
 *
 * ReplaceToolBar - A tool bar for "replace" operations in text areas.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.UIUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;

/**
 * A toolbar for replace operations in a text editor application. This provides
 * a more seamless experience than using a Find or Replace dialog.
 *
 * @author Robert Futrell
 * @version 0.5
 * @see FindToolBar
 * @see ReplaceDialog
 */
public class ReplaceToolBar extends FindToolBar {

	/**
	 * Listens for the user typing into the search field.
	 */
	protected class ReplaceFindFieldListener extends FindFieldListener {

		@Override
		protected void handleDocumentEvent(final DocumentEvent e) {
			super.handleDocumentEvent(e);
			final JTextComponent findField = UIUtil.getTextComponent(ReplaceToolBar.this.findCombo);
			final JTextComponent replaceField = UIUtil.getTextComponent(ReplaceToolBar.this.replaceCombo);
			if (e.getDocument().equals(findField.getDocument()))
				ReplaceToolBar.this.handleToggleButtons();
			if (e.getDocument() == replaceField.getDocument())
				ReplaceToolBar.this.getSearchContext().setReplaceWith(replaceField.getText());
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private JButton replaceAllButton;

	private JButton replaceButton;

	/**
	 * Our search listener, cached so we can grab its selected text easily.
	 */
	protected SearchListener searchListener;

	/**
	 * Creates the tool bar.
	 *
	 * @param listener
	 *            An entity listening for search events.
	 */
	public ReplaceToolBar(final SearchListener listener) {
		super(listener);
		this.searchListener = listener;
	}

	@Override
	public void addNotify() {
		super.addNotify();
		this.handleToggleButtons();
	}

	@Override
	protected Container createButtonPanel() {

		final Box panel = new Box(BoxLayout.LINE_AXIS);

		final JPanel bp = new JPanel(new GridLayout(2, 2, 5, 5));
		panel.add(bp);

		this.createFindButtons();

		final Component filler = Box.createRigidArea(new Dimension(5, 5));

		bp.add(this.findButton);
		bp.add(this.replaceButton);
		bp.add(this.replaceAllButton);
		bp.add(filler);
		panel.add(bp);

		final JPanel optionPanel = new JPanel(new SpringLayout());
		this.matchCaseCheckBox = this.createCB("MatchCase");
		this.regexCheckBox = this.createCB("RegEx");
		this.wholeWordCheckBox = this.createCB("WholeWord");
		this.markAllCheckBox = this.createCB("MarkAll");
		// We use a "spacing" middle row, instead of spacing in the call to
		// UIUtil.makeSpringCompactGrid(), as the latter adds trailing
		// spacing after the final "row", which screws up our alignment.
		final Dimension spacing = new Dimension(1, 5);
		final Component space1 = Box.createRigidArea(spacing);
		final Component space2 = Box.createRigidArea(spacing);

		final ComponentOrientation orientation = ComponentOrientation.getOrientation(this.getLocale());

		if (orientation.isLeftToRight()) {
			optionPanel.add(this.matchCaseCheckBox);
			optionPanel.add(this.wholeWordCheckBox);
			optionPanel.add(space1);
			optionPanel.add(space2);
			optionPanel.add(this.regexCheckBox);
			optionPanel.add(this.markAllCheckBox);
		} else {
			optionPanel.add(this.wholeWordCheckBox);
			optionPanel.add(this.matchCaseCheckBox);
			optionPanel.add(space2);
			optionPanel.add(space1);
			optionPanel.add(this.markAllCheckBox);
			optionPanel.add(this.regexCheckBox);
		}
		UIUtil.makeSpringCompactGrid(optionPanel, 3, 2, 0, 0, 0, 0);
		panel.add(optionPanel);

		return panel;

	}

	@Override
	protected Container createFieldPanel() {

		this.findFieldListener = new ReplaceFindFieldListener();

		final JPanel temp = new JPanel(new SpringLayout());

		final JLabel findLabel = new JLabel(FindToolBar.msg.getString("FindWhat"));
		final JLabel replaceLabel = new JLabel(FindToolBar.msg.getString("ReplaceWith"));

		this.findCombo = new SearchComboBox(this, false);
		final JTextComponent findField = UIUtil.getTextComponent(this.findCombo);
		this.findFieldListener.install(findField);
		final Container fcp = this.createContentAssistablePanel(this.findCombo);

		this.replaceCombo = new SearchComboBox(this, true);
		final JTextComponent replaceField = UIUtil.getTextComponent(this.replaceCombo);
		this.findFieldListener.install(replaceField);
		final Container rcp = this.createContentAssistablePanel(this.replaceCombo);

		// We use a "spacing" middle row, instead of spacing in the call to
		// UIUtil.makeSpringCompactGrid(), as the latter adds trailing
		// spacing after the final "row", which screws up our alignment.
		final Dimension spacing = new Dimension(1, 5);
		final Component space1 = Box.createRigidArea(spacing);
		final Component space2 = Box.createRigidArea(spacing);

		if (this.getComponentOrientation().isLeftToRight()) {
			temp.add(findLabel);
			temp.add(fcp);
			temp.add(space1);
			temp.add(space2);
			temp.add(replaceLabel);
			temp.add(rcp);
		} else {
			temp.add(fcp);
			temp.add(findLabel);
			temp.add(space2);
			temp.add(space1);
			temp.add(rcp);
			temp.add(replaceLabel);
		}
		UIUtil.makeSpringCompactGrid(temp, 3, 2, 0, 0, 0, 0);

		return temp;
	}

	@Override
	protected void createFindButtons() {

		super.createFindButtons();

		this.replaceButton = new JButton(FindToolBar.searchMsg.getString("Replace"));
		this.makeEnterActivateButton(this.replaceButton);
		this.replaceButton.setToolTipText(FindToolBar.msg.getString("Replace.ToolTip"));
		this.replaceButton.setActionCommand("Replace");
		this.replaceButton.addActionListener(this.listener);
		this.replaceButton.setEnabled(false);

		this.replaceAllButton = new JButton(FindToolBar.searchMsg.getString("ReplaceAll"));
		this.makeEnterActivateButton(this.replaceAllButton);
		this.replaceAllButton.setActionCommand("ReplaceAll");
		this.replaceAllButton.addActionListener(this.listener);
		this.replaceAllButton.setEnabled(false);

	}

	/**
	 * Called when the regex checkbox is clicked (or its value is modified via a
	 * change to the search context). Subclasses can override to add custom
	 * behavior, but should call the super implementation.
	 */
	@Override
	protected void handleRegExCheckBoxClicked() {
		super.handleRegExCheckBoxClicked();
		// "Content assist" support
		final boolean b = this.regexCheckBox.isSelected();
		this.replaceCombo.setAutoCompleteEnabled(b);
	}

	@Override
	protected void handleSearchAction(final ActionEvent e) {
		final String command = e.getActionCommand();
		super.handleSearchAction(e);
		if ("FindNext".equals(command) || "FindPrevious".equals(command))
			this.handleToggleButtons(); // Replace button could toggle state
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

	private boolean matchesSearchFor(final String text) {
		if (text == null || text.length() == 0)
			return false;
		final String searchFor = this.findCombo.getSelectedString();
		if (searchFor != null && searchFor.length() > 0) {
			final boolean matchCase = this.matchCaseCheckBox.isSelected();
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
	 * Overridden to possibly toggle the enabled state of the replace button.
	 */
	@Override
	public boolean requestFocusInWindow() {
		final boolean result = super.requestFocusInWindow();
		this.handleToggleButtons(); // Replace button state may change
		return result;
	}

}