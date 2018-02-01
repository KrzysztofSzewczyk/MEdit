package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.util.Scanner;
import java.awt.event.ActionEvent;

public class GoToLine extends JDialog {

	private static final long serialVersionUID = -4732961351244563966L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;

	/**
	 * Create the dialog.
	 */
	public GoToLine(MainFrame instance) {
		setType(Type.POPUP);
		setTitle("Go to line");
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(GoToLine.class.getResource("/medit/assets/actions/format-indent-more.png")));
		setBounds(100, 100, 174, 112);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel, BorderLayout.NORTH);
			{
				JLabel lblLine = new JLabel("Line: ");
				panel.add(lblLine);
			}
			{
				textField = new JTextField();
				panel.add(textField);
				textField.setColumns(10);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public int newCursor(int newlineno) {
						int pos = 0;
						int i = 0;
						String line = "";
						Scanner sc = new Scanner(instance.textPane.getText());
						while (sc.hasNextLine()) {
							line = sc.nextLine();
							i++;
							if (newlineno > i) {
								pos = pos + line.length() + 1;
							}
						}
						sc.close();
						return pos;
					}

					public void actionPerformed(ActionEvent arg0) {
						try {
							instance.textPane.setCaretPosition(newCursor(Integer.parseInt(textField.getText())));
						} catch (Exception e) {
							JOptionPane.showMessageDialog(instance, "Please enter valid line number.", "Error.",
									JOptionPane.ERROR_MESSAGE);
							return;
						}

					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
