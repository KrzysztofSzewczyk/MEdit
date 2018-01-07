package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Crash dialog that appears after some exception is thrown.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class Crash extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public Crash(final Exception E1) {
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(Crash.class.getResource(Messages.getString("Crash.0")))); //$NON-NLS-1$
		this.setTitle(Messages.getString("Crash.1")); //$NON-NLS-1$
		this.setBounds(100, 100, 450, 300);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(new BorderLayout(0, 0));
		{
			final JLabel lblAnErrorOccured = new JLabel(Messages.getString("Crash.2")); //$NON-NLS-1$
			lblAnErrorOccured.setHorizontalAlignment(SwingConstants.CENTER);
			this.contentPanel.add(lblAnErrorOccured, BorderLayout.NORTH);
		}
		{
			final JScrollPane scrollPane = new JScrollPane();
			this.contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				final JTextArea txtr = new JTextArea();
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				E1.printStackTrace(pw);
				final String sStackTrace = sw.toString();
				txtr.setText(sStackTrace);
				txtr.setFont(new Font(Messages.getString("Crash.3"), Font.PLAIN, 13)); //$NON-NLS-1$
				scrollPane.setViewportView(txtr);
			}
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton(Messages.getString("Crash.4")); //$NON-NLS-1$
				okButton.addActionListener(e -> System.exit(0));
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
			{
				final JButton cancelButton = new JButton(Messages.getString("Crash.5")); //$NON-NLS-1$
				cancelButton.addActionListener(e -> Crash.this.dispose());
				buttonPane.add(cancelButton);
			}
		}
	}

}
