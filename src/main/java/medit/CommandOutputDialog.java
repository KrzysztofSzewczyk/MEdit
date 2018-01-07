package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

public class CommandOutputDialog extends JDialog {

	private static final long serialVersionUID = 1482808449383821979L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public CommandOutputDialog(final String output) {
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(CommandOutputDialog.class.getResource(Messages.getString("CommandOutputDialog.0")))); //$NON-NLS-1$
		this.setTitle(Messages.getString("CommandOutputDialog.1")); //$NON-NLS-1$
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
				textPane.setFont(new Font(Messages.getString("CommandOutputDialog.2"), Font.PLAIN, 13)); //$NON-NLS-1$
				textPane.setText(output);
			}
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton(Messages.getString("CommandOutputDialog.3")); //$NON-NLS-1$
				okButton.addActionListener(e -> CommandOutputDialog.this.dispose());
				okButton.setActionCommand(Messages.getString("CommandOutputDialog.4")); //$NON-NLS-1$
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
