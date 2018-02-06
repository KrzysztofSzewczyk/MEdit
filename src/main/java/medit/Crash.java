package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(Crash.class.getResource("/medit/assets/actions/process-stop.png")));
		this.setTitle("MEdit");
		this.setBounds(100, 100, 450, 300);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(new BorderLayout(0, 0));
		{
			final JLabel lblAnErrorOccured = new JLabel("An error occured.");
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
				txtr.setFont(new Font("Monospaced", Font.PLAIN, 13));
				scrollPane.setViewportView(txtr);
			}
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton("Exit");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						System.exit(0);
					}
				});
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
			{
				final JButton cancelButton = new JButton("Continue");
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Crash.this.dispose();
					}
				});
				buttonPane.add(cancelButton);
			}
		}
	}

}
