package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

/**
 * This window is reproducing look of Windows search form. It contains many
 * advanced options for searching.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class SearchWindow extends JDialog {

	private static final long serialVersionUID = 3599396860237329268L;
	private final JCheckBox chckbxMatchCase = new JCheckBox("Match Case");
	private final JCheckBox chckbxRegex = new JCheckBox("Regex");
	private final JCheckBox chckbxSearchWholeWord = new JCheckBox("Search Whole Word");
	private final JPanel contentPanel = new JPanel();
	private final JRadioButton rdbtnBackward = new JRadioButton("Backward");
	private final JRadioButton rdbtnForward = new JRadioButton("Forward");
	private SearchWindow sinstance;
	private JTextField textField;
	private JTextField textField_1;

	/**
	 * Create the dialog.
	 */
	public SearchWindow(final MainFrame instance) {
		this.sinstance = this;
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(SearchWindow.class.getResource("/medit/assets/actions/edit-find.png")));
		this.setTitle("Search & Replace");
		this.setBounds(100, 100, 400, 231);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(new BorderLayout(0, 0));
		{
			final JPanel panel = new JPanel();
			this.contentPanel.add(panel, BorderLayout.CENTER);
			panel.setLayout(null);

			final JLabel lblFind = new JLabel("Find:");
			lblFind.setBounds(10, 15, 46, 14);
			panel.add(lblFind);

			final JLabel lblReplaceWith = new JLabel("Replace With:");
			lblReplaceWith.setBounds(10, 40, 73, 14);
			panel.add(lblReplaceWith);

			this.textField = new JTextField();
			this.textField.setBounds(110, 12, 121, 20);
			panel.add(this.textField);
			this.textField.setColumns(15);

			this.textField_1 = new JTextField();
			this.textField_1.setBounds(110, 37, 121, 20);
			panel.add(this.textField_1);
			this.textField_1.setColumns(15);

			final JButton btnFind = new JButton("Find");
			btnFind.addActionListener(e -> {
				if (instance.textPane.getText().length() == 0)
					return;
				final SearchContext context = new SearchContext();
				context.setSearchFor(SearchWindow.this.sinstance.textField.getText());
				context.setMatchCase(SearchWindow.this.chckbxMatchCase.isSelected());
				context.setRegularExpression(SearchWindow.this.chckbxRegex.isSelected());
				context.setSearchForward(SearchWindow.this.rdbtnForward.isSelected());
				context.setWholeWord(SearchWindow.this.chckbxSearchWholeWord.isSelected());

				final boolean found = SearchEngine.find(instance.textPane, context).wasFound();
				if (!found)
					JOptionPane.showMessageDialog(SearchWindow.this.sinstance, "Text not found");
			});
			btnFind.setBounds(241, 11, 128, 23);
			panel.add(btnFind);

			final JButton btnReplace = new JButton("Replace");
			btnReplace.addActionListener(e -> {
				if (instance.textPane.getText().length() == 0)
					return;
				final SearchContext context = new SearchContext();
				context.setSearchFor(SearchWindow.this.sinstance.textField.getText());
				context.setMatchCase(SearchWindow.this.chckbxMatchCase.isSelected());
				context.setRegularExpression(SearchWindow.this.chckbxRegex.isSelected());
				context.setSearchForward(SearchWindow.this.rdbtnForward.isSelected());
				context.setWholeWord(SearchWindow.this.chckbxSearchWholeWord.isSelected());
				context.setReplaceWith(SearchWindow.this.sinstance.textField_1.getText());

				final boolean found = SearchEngine.replace(instance.textPane, context).wasFound();
				if (!found)
					JOptionPane.showMessageDialog(SearchWindow.this.sinstance, "Text not found");
			});
			btnReplace.setBounds(241, 36, 128, 23);
			panel.add(btnReplace);

			final JButton btnReplaceAll = new JButton("Replace All");
			btnReplaceAll.addActionListener(e -> {
				if (instance.textPane.getText().length() == 0)
					return;
				final SearchContext context = new SearchContext();
				context.setSearchFor(SearchWindow.this.sinstance.textField.getText());
				context.setMatchCase(SearchWindow.this.chckbxMatchCase.isSelected());
				context.setRegularExpression(SearchWindow.this.chckbxRegex.isSelected());
				context.setSearchForward(SearchWindow.this.rdbtnForward.isSelected());
				context.setWholeWord(SearchWindow.this.chckbxSearchWholeWord.isSelected());
				context.setReplaceWith(SearchWindow.this.sinstance.textField_1.getText());

				final boolean found = SearchEngine.replaceAll(instance.textPane, context).wasFound();
				if (!found)
					JOptionPane.showMessageDialog(SearchWindow.this.sinstance, "Text not found");
			});
			btnReplaceAll.setBounds(241, 60, 128, 23);
			panel.add(btnReplaceAll);

			final JButton btnCountOccurences = new JButton("Count occurences");
			btnCountOccurences.addActionListener(e -> {
				if (instance.textPane.getText().length() == 0)
					return;
				final SearchContext context = new SearchContext();
				context.setSearchFor(SearchWindow.this.sinstance.textField.getText());
				context.setMatchCase(SearchWindow.this.chckbxMatchCase.isSelected());
				context.setRegularExpression(SearchWindow.this.chckbxRegex.isSelected());
				context.setSearchForward(SearchWindow.this.rdbtnForward.isSelected());
				context.setWholeWord(SearchWindow.this.chckbxSearchWholeWord.isSelected());
				context.setReplaceWith(SearchWindow.this.sinstance.textField_1.getText());

				int amount = 0;

				while (true) {
					final boolean found = SearchEngine.find(instance.textPane, context).wasFound();
					if (!found)
						break;
					else
						amount++;
				}

				JOptionPane.showMessageDialog(SearchWindow.this.sinstance, "Found " + amount + " occurences");
			});
			btnCountOccurences.setBounds(241, 84, 128, 23);
			panel.add(btnCountOccurences);
			this.rdbtnForward.setSelected(true);

			this.rdbtnForward.addActionListener(new ActionListener() {
				@Override
				@SuppressWarnings("deprecation")
				public void actionPerformed(final ActionEvent e) {
					SearchWindow.this.rdbtnBackward.enable(false);
				}
			});
			this.rdbtnForward.setBounds(10, 70, 109, 23);
			panel.add(this.rdbtnForward);
			this.rdbtnBackward.addActionListener(new ActionListener() {
				@Override
				@SuppressWarnings("deprecation")
				public void actionPerformed(final ActionEvent e) {
					SearchWindow.this.rdbtnForward.enable(false);
				}
			});

			this.rdbtnBackward.setBounds(10, 94, 109, 23);
			panel.add(this.rdbtnBackward);

			this.chckbxMatchCase.setBounds(120, 70, 97, 23);
			panel.add(this.chckbxMatchCase);

			this.chckbxRegex.setBounds(120, 94, 97, 23);
			panel.add(this.chckbxRegex);

			this.chckbxSearchWholeWord.setBounds(120, 120, 121, 23);
			panel.add(this.chckbxSearchWholeWord);
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}
}
