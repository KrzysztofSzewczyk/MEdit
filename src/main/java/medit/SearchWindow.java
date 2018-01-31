package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

public class SearchWindow extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	private JTextField textField_1;
	private JRadioButton rdbtnBackward = new JRadioButton("Backward");
	private JRadioButton rdbtnForward = new JRadioButton("Forward");

	/**
	 * Create the dialog.
	 */
	public SearchWindow(MainFrame instance) {
		setIconImage(Toolkit.getDefaultToolkit().getImage(SearchWindow.class.getResource("/medit/assets/actions/edit-find.png")));
		setTitle("Search & Replace");
		setBounds(100, 100, 400, 199);
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
				}
			});
			btnFind.setBounds(241, 11, 128, 23);
			panel.add(btnFind);
			
			JButton btnReplace = new JButton("Replace");
			btnReplace.setBounds(241, 36, 128, 23);
			panel.add(btnReplace);
			
			JButton btnReplaceAll = new JButton("Replace All");
			btnReplaceAll.setBounds(241, 60, 128, 23);
			panel.add(btnReplaceAll);
			
			JButton btnCountOccurences = new JButton("Count occurences");
			btnCountOccurences.setBounds(241, 84, 128, 23);
			panel.add(btnCountOccurences);
			
			rdbtnForward.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					rdbtnBackward.enable(false);
				}
			});
			rdbtnForward.setBounds(10, 70, 109, 23);
			panel.add(rdbtnForward);
			rdbtnBackward.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					rdbtnForward.enable(false);
				}
			});
			
			rdbtnBackward.setBounds(10, 94, 109, 23);
			panel.add(rdbtnBackward);
			
			JCheckBox chckbxMatchCase = new JCheckBox("Match Case");
			chckbxMatchCase.setBounds(120, 70, 97, 23);
			panel.add(chckbxMatchCase);
			
			JCheckBox chckbxRegex = new JCheckBox("Regex");
			chckbxRegex.setBounds(120, 94, 97, 23);
			panel.add(chckbxRegex);
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
