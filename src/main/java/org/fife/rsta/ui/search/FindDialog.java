/*
 * 11/14/2003
 *
 * FindDialog - Dialog for finding text in a GUI.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui.search;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
 * A "Find" dialog similar to those found in most Windows text editing
 * applications. Contains many search options, including:<br>
 * <ul>
 * <li>Match Case
 * <li>Match Whole Word
 * <li>Use Regular Expressions
 * <li>Search Forwards or Backwards
 * <li>Mark all
 * </ul>
 * The dialog also remembers your previous several selections in a combo box.
 * <p>
 * An application can use a <code>FindDialog</code> as follows. It is suggested
 * that you create an <code>Action</code> or something similar to facilitate
 * "bringing up" the Find dialog. Have the main application contain an object
 * that implements {@link SearchListener}. This object will receive
 * {@link SearchEvent}s of the following types from the Find dialog:
 * <ul>
 * <li>{@link SearchEvent.Type#FIND} action when the user clicks the "Find"
 * button.
 * </ul>
 * The application can then call i.e.
 * {@link SearchEngine#find(javax.swing.JTextArea, org.fife.ui.rtextarea.SearchContext)
 * SearchEngine.find()} to actually execute the search.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see FindToolBar
 */
public class FindDialog extends AbstractFindReplaceDialog {

	/**
	 * Listens for changes in the text field (find search field).
	 */
	private class FindDocumentListener implements DocumentListener {

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			FindDialog.this.handleToggleButtons();
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			final JTextComponent comp = UIUtil.getTextComponent(FindDialog.this.findTextCombo);
			if (comp.getDocument().getLength() == 0)
				FindDialog.this.findNextButton.setEnabled(false);
			else
				FindDialog.this.handleToggleButtons();
		}

	}

	/**
	 * Listens for the text field gaining focus. All it does is select all text in
	 * the combo box's text area.
	 */
	private class FindFocusAdapter extends FocusAdapter {

		@Override
		public void focusGained(final FocusEvent e) {
			UIUtil.getTextComponent(FindDialog.this.findTextCombo).selectAll();
			// Remember what it originally was, in case they tabbed out.
			FindDialog.this.lastSearchString = (String) FindDialog.this.findTextCombo.getSelectedItem();
		}

	}

	/**
	 * Listens for key presses in the find dialog.
	 */
	private class FindKeyListener implements KeyListener {

		// Listens for the user pressing a key down.
		@Override
		public void keyPressed(final KeyEvent e) {
		}

		// Listens for a user releasing a key.
		@Override
		public void keyReleased(final KeyEvent e) {

			// This is an ugly hack to get around JComboBox's
			// insistence on eating the first Enter keypress
			// it receives when it has focus and its selected item
			// has changed since the last time it lost focus.
			if (e.getKeyCode() == KeyEvent.VK_ENTER && AbstractSearchDialog.isPreJava6JRE()) {
				final String searchString = (String) FindDialog.this.findTextCombo.getSelectedItem();
				if (!searchString.equals(FindDialog.this.lastSearchString)) {
					FindDialog.this.findNextButton.doClick(0);
					FindDialog.this.lastSearchString = searchString;
					UIUtil.getTextComponent(FindDialog.this.findTextCombo).selectAll();
				}
			}

		}

		// Listens for a key being typed.
		@Override
		public void keyTyped(final KeyEvent e) {
		}

	}

	private static final long serialVersionUID = 1L;

	// This helps us work around the "bug" where JComboBox eats the first Enter
	// press.
	private String lastSearchString;

	/**
	 * Our search listener, cached so we can grab its selected text easily.
	 */
	protected SearchListener searchListener;

	/**
	 * Creates a new <code>FindDialog</code>.
	 *
	 * @param owner
	 *            The parent dialog.
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	public FindDialog(final Dialog owner, final SearchListener listener) {
		super(owner);
		this.init(listener);
	}

	/**
	 * Creates a new <code>FindDialog</code>.
	 *
	 * @param owner
	 *            The main window that owns this dialog.
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	public FindDialog(final Frame owner, final SearchListener listener) {
		super(owner);
		this.init(listener);
	}

	/**
	 * Initializes find dialog-specific initialization stuff.
	 *
	 * @param listener
	 *            The component that listens for {@link SearchEvent}s.
	 */
	private void init(final SearchListener listener) {

		this.searchListener = listener;

		final ComponentOrientation orientation = ComponentOrientation.getOrientation(this.getLocale());

		// Make a panel containing the "Find" edit box.
		final JPanel enterTextPane = new JPanel(new SpringLayout());
		enterTextPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		final JTextComponent textField = UIUtil.getTextComponent(this.findTextCombo);
		textField.addFocusListener(new FindFocusAdapter());
		textField.addKeyListener(new FindKeyListener());
		textField.getDocument().addDocumentListener(new FindDocumentListener());
		JPanel temp = new JPanel(new BorderLayout());
		temp.add(this.findTextCombo);
		final AssistanceIconPanel aip = new AssistanceIconPanel(this.findTextCombo);
		temp.add(aip, BorderLayout.LINE_START);
		if (orientation.isLeftToRight()) {
			enterTextPane.add(this.findFieldLabel);
			enterTextPane.add(temp);
		} else {
			enterTextPane.add(temp);
			enterTextPane.add(this.findFieldLabel);
		}

		UIUtil.makeSpringCompactGrid(enterTextPane, 1, 2, // rows, cols
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
		leftPanel.add(enterTextPane);
		leftPanel.add(bottomPanel);

		// Make a panel containing the action buttons.
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2, 1, 5, 5));
		buttonPanel.add(this.findNextButton);
		buttonPanel.add(this.cancelButton);
		final JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(buttonPanel, BorderLayout.NORTH);

		// Put everything into a neat little package.
		final JPanel contentPane = new JPanel(new BorderLayout());
		if (orientation.isLeftToRight())
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 5));
		else
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
		contentPane.add(leftPanel);
		contentPane.add(rightPanel, BorderLayout.LINE_END);
		temp = new ResizableFrameContentPane(new BorderLayout());
		temp.add(contentPane, BorderLayout.NORTH);
		this.setContentPane(temp);
		this.getRootPane().setDefaultButton(this.findNextButton);
		this.setTitle(AbstractSearchDialog.getString("FindDialogTitle"));
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo(this.getParent());

		this.setSearchContext(new SearchContext());
		this.addSearchListener(listener);

		this.applyComponentOrientation(orientation);

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
			final boolean nonEmpty = selectedItem != null && selectedItem.length() > 0;
			this.findNextButton.setEnabled(nonEmpty);
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
		final JTextComponent textField = UIUtil.getTextComponent(this.findTextCombo);
		textField.addFocusListener(new FindFocusAdapter());
		textField.addKeyListener(new FindKeyListener());
		textField.getDocument().addDocumentListener(new FindDocumentListener());
	}

}