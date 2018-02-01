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

public class SearchWindow extends JDialog {

	private static final long serialVersionUID = 3599396860237329268L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	private JTextField textField_1;
	private JRadioButton rdbtnBackward = new JRadioButton("Backward");
	private JRadioButton rdbtnForward = new JRadioButton("Forward");
	private JCheckBox chckbxMatchCase = new JCheckBox("Match Case");
	private JCheckBox chckbxRegex = new JCheckBox("Regex");
	private JCheckBox chckbxSearchWholeWord = new JCheckBox("Search Whole Word");
	private SearchWindow sinstance;

	/**
	 * Create the dialog.
	 */
	public SearchWindow(MainFrame instance) {
		this.sinstance = this;
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(SearchWindow.class.getResource("/medit/assets/actions/edit-find.png")));
		setTitle("Search & Replace");
		setBounds(100, 100, 400, 231);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel, BorderLayout.CENTER);
			panel.setLayout(null);

			JLabel lblFind = new JLabel("Find:");
			lblFind.setBounds(10, 15, 46, 14);
			panel.add(lblFind);

			JLabel lblReplaceWith = new JLabel("Replace With:");
			lblReplaceWith.setBounds(10, 40, 73, 14);
			panel.add(lblReplaceWith);

			textField = new JTextField();
			textField.setBounds(110, 12, 121, 20);
			panel.add(textField);
			textField.setColumns(15);

			textField_1 = new JTextField();
			textField_1.setBounds(110, 37, 121, 20);
			panel.add(textField_1);
			textField_1.setColumns(15);

			JButton btnFind = new JButton("Find");
			btnFind.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (instance.textPane.getText().length() == 0)
						return;
					SearchContext context = new SearchContext();
					context.setSearchFor(sinstance.textField.getText());
					context.setMatchCase(chckbxMatchCase.isSelected());
					context.setRegularExpression(chckbxRegex.isSelected());
					context.setSearchForward(rdbtnForward.isSelected());
					context.setWholeWord(chckbxSearchWholeWord.isSelected());

					boolean found = SearchEngine.find(instance.textPane, context).wasFound();
					if (!found) {
						JOptionPane.showMessageDialog(sinstance, "Text not found");
					}
				}
			});
			btnFind.setBounds(241, 11, 128, 23);
			panel.add(btnFind);

			JButton btnReplace = new JButton("Replace");
			btnReplace.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (instance.textPane.getText().length() == 0)
						return;
					SearchContext context = new SearchContext();
					context.setSearchFor(sinstance.textField.getText());
					context.setMatchCase(chckbxMatchCase.isSelected());
					context.setRegularExpression(chckbxRegex.isSelected());
					context.setSearchForward(rdbtnForward.isSelected());
					context.setWholeWord(chckbxSearchWholeWord.isSelected());
					context.setReplaceWith(sinstance.textField_1.getText());

					boolean found = SearchEngine.replace(instance.textPane, context).wasFound();
					if (!found) {
						JOptionPane.showMessageDialog(sinstance, "Text not found");
					}
				}
			});
			btnReplace.setBounds(241, 36, 128, 23);
			panel.add(btnReplace);

			JButton btnReplaceAll = new JButton("Replace All");
			btnReplaceAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (instance.textPane.getText().length() == 0)
						return;
					SearchContext context = new SearchContext();
					context.setSearchFor(sinstance.textField.getText());
					context.setMatchCase(chckbxMatchCase.isSelected());
					context.setRegularExpression(chckbxRegex.isSelected());
					context.setSearchForward(rdbtnForward.isSelected());
					context.setWholeWord(chckbxSearchWholeWord.isSelected());
					context.setReplaceWith(sinstance.textField_1.getText());

					boolean found = SearchEngine.replaceAll(instance.textPane, context).wasFound();
					if (!found) {
						JOptionPane.showMessageDialog(sinstance, "Text not found");
					}
				}
			});
			btnReplaceAll.setBounds(241, 60, 128, 23);
			panel.add(btnReplaceAll);

			JButton btnCountOccurences = new JButton("Count occurences");
			btnCountOccurences.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (instance.textPane.getText().length() == 0)
						return;
					SearchContext context = new SearchContext();
					context.setSearchFor(sinstance.textField.getText());
					context.setMatchCase(chckbxMatchCase.isSelected());
					context.setRegularExpression(chckbxRegex.isSelected());
					context.setSearchForward(rdbtnForward.isSelected());
					context.setWholeWord(chckbxSearchWholeWord.isSelected());
					context.setReplaceWith(sinstance.textField_1.getText());

					int amount = 0;

					while (true) {
						boolean found = SearchEngine.find(instance.textPane, context).wasFound();
						if (!found) {
							break;
						} else
							amount++;
					}

					JOptionPane.showMessageDialog(sinstance, "Found " + amount + " occurences");
				}
			});
			btnCountOccurences.setBounds(241, 84, 128, 23);
			panel.add(btnCountOccurences);
			rdbtnForward.setSelected(true);

			rdbtnForward.addActionListener(new ActionListener() {
				@SuppressWarnings("deprecation")
				public void actionPerformed(ActionEvent e) {
					rdbtnBackward.enable(false);
				}
			});
			rdbtnForward.setBounds(10, 70, 109, 23);
			panel.add(rdbtnForward);
			rdbtnBackward.addActionListener(new ActionListener() {
				@SuppressWarnings("deprecation")
				public void actionPerformed(ActionEvent e) {
					rdbtnForward.enable(false);
				}
			});

			rdbtnBackward.setBounds(10, 94, 109, 23);
			panel.add(rdbtnBackward);

			chckbxMatchCase.setBounds(120, 70, 97, 23);
			panel.add(chckbxMatchCase);

			chckbxRegex.setBounds(120, 94, 97, 23);
			panel.add(chckbxRegex);

			chckbxSearchWholeWord.setBounds(120, 120, 121, 23);
			panel.add(chckbxSearchWholeWord);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}
}
