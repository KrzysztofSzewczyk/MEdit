package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Toolkit;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * AboutBox is dialog containing information about MEdit.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class AboutBox extends JDialog {

	/**
	 * Serial Version UID required by Eclipse
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public AboutBox() {
		setIconImage(
				Toolkit.getDefaultToolkit().getImage(AboutBox.class.getResource(Messages.getString("AboutBox.0")))); //$NON-NLS-1$
		setTitle(Messages.getString("AboutBox.1")); //$NON-NLS-1$
		setBounds(100, 100, 632, 201);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setLayout(new FlowLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		{
			JLabel lblMeditIsFree = new JLabel(Messages.getString("AboutBox.2")); //$NON-NLS-1$
			contentPanel.add(lblMeditIsFree);
		}
		{
			JLabel lblDevcppIconsWere = new JLabel(Messages.getString("AboutBox.3")); //$NON-NLS-1$
			contentPanel.add(lblDevcppIconsWere);
		}
		{
			JLabel lblCopyrightcBy = new JLabel(Messages.getString("AboutBox.4")); //$NON-NLS-1$
			contentPanel.add(lblCopyrightcBy);
		}
		{
			JLabel lblPleaseSeeLicense = new JLabel(Messages.getString("AboutBox.5")); //$NON-NLS-1$
			contentPanel.add(lblPleaseSeeLicense);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton(Messages.getString("AboutBox.6")); //$NON-NLS-1$
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
