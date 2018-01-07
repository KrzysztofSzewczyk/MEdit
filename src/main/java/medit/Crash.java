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
	public Crash(Exception E1) {
		setIconImage(Toolkit.getDefaultToolkit().getImage(Crash.class.getResource(Messages.getString("Crash.0")))); //$NON-NLS-1$
		setTitle(Messages.getString("Crash.1")); //$NON-NLS-1$
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JLabel lblAnErrorOccured = new JLabel(Messages.getString("Crash.2")); //$NON-NLS-1$
			lblAnErrorOccured.setHorizontalAlignment(SwingConstants.CENTER);
			contentPanel.add(lblAnErrorOccured, BorderLayout.NORTH);
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				JTextArea txtr = new JTextArea();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				E1.printStackTrace(pw);
				String sStackTrace = sw.toString();
				txtr.setText(sStackTrace);
				txtr.setFont(new Font(Messages.getString("Crash.3"), Font.PLAIN, 13)); //$NON-NLS-1$
				scrollPane.setViewportView(txtr);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Messages.getString("Crash.4")); //$NON-NLS-1$
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.exit(0);
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton(Messages.getString("Crash.5")); //$NON-NLS-1$
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				buttonPane.add(cancelButton);
			}
		}
	}

}
