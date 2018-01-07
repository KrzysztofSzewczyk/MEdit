package medit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

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
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(Crash.class.getResource("/medit/assets/actions/process-stop.png")));
		setTitle("MEdit");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JLabel lblAnErrorOccured = new JLabel("An error occured.");
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
				txtr.setFont(new Font("Monospaced", Font.PLAIN, 13));
				scrollPane.setViewportView(txtr);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("Exit");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Continue");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

}
