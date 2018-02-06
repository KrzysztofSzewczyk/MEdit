package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * This dialog is the on that pops up after executing tool. It contains
 * information about tool execution status and it's output.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class CommandOutputDialog extends JDialog {

	private static final long serialVersionUID = 1482808449383821979L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public CommandOutputDialog(final String output) {
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(CommandOutputDialog.class.getResource("/medit/assets/categories/preferences-system.png")));
		this.setTitle("Tool Output");
		this.setBounds(100, 100, 450, 300);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(new BorderLayout(0, 0));
		{
			final JScrollPane scrollPane = new JScrollPane();
			this.contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				final JTextPane textPane = new JTextPane();
				scrollPane.setViewportView(textPane);
				textPane.setEditable(false);
				textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
				textPane.setText(output);
			}
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						CommandOutputDialog.this.dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
