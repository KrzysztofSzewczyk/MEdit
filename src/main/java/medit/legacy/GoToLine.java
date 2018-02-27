package medit.legacy;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * This is GoToLine dialog, which we know from many text editors out there, that
 * is jumping to selected line in textPane, which we reference using internal
 * MainFrame instance.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class GoToLine extends JDialog {

	private static final long serialVersionUID = -4732961351244563966L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;

	/**
	 * Create the dialog.
	 */
	public GoToLine(final MainFrame instance) {
		this.setType(Type.POPUP);
		this.setTitle("Go to line");
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(GoToLine.class.getResource("/medit/assets/actions/format-indent-more.png")));
		this.setBounds(100, 100, 174, 112);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(new BorderLayout(0, 0));
		{
			final JPanel panel = new JPanel();
			this.contentPanel.add(panel, BorderLayout.NORTH);
			{
				final JLabel lblLine = new JLabel("Line: ");
				panel.add(lblLine);
			}
			{
				this.textField = new JTextField();
				panel.add(this.textField);
				this.textField.setColumns(10);
			}
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					/**
					 * ActionPerformed element of ActionListener, that is jumping to likne selected
					 * in textField. In case of error, it's displaying universal message box that is
					 * telling user that something is wrong.
					 */

					@Override
					public void actionPerformed(final ActionEvent arg0) {
						new Thread(new Runnable() {

							/**
							 * This function is calculating position that we need to set in textPane, to
							 * locate cursor at selected line.
							 *
							 * @param newlineno
							 * @return
							 */

							public int newCursor(final int newlineno) {
								int pos = 0;
								int i = 0;
								String line = "";
								final Scanner sc = new Scanner(instance.textPane.getText());
								while (sc.hasNextLine()) {
									line = sc.nextLine();
									i++;
									if (newlineno > i)
										pos = pos + line.length() + 1;
								}
								sc.close();
								return pos;
							}

							@Override
							public void run() {
								try {
									instance.textPane.setCaretPosition(
											this.newCursor(Integer.parseInt(GoToLine.this.textField.getText())));
								} catch (final Exception e) {
									JOptionPane.showMessageDialog(instance, "Please enter valid line number.", "Error.",
											JOptionPane.ERROR_MESSAGE);
									return;
								}
							}

						});
					}

				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
