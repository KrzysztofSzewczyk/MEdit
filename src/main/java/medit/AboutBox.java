package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
		this.setIconImage(
				Toolkit.getDefaultToolkit().getImage(AboutBox.class.getResource(Messages.getString("AboutBox.0")))); //$NON-NLS-1$
		this.setTitle(Messages.getString("AboutBox.1")); //$NON-NLS-1$
		this.setBounds(100, 100, 632, 201);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setLayout(new FlowLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		{
			final JLabel lblMeditIsFree = new JLabel(Messages.getString("AboutBox.2")); //$NON-NLS-1$
			this.contentPanel.add(lblMeditIsFree);
		}
		{
			final JLabel lblDevcppIconsWere = new JLabel(Messages.getString("AboutBox.3")); //$NON-NLS-1$
			this.contentPanel.add(lblDevcppIconsWere);
		}
		{
			final JLabel lblCopyrightcBy = new JLabel(Messages.getString("AboutBox.4")); //$NON-NLS-1$
			this.contentPanel.add(lblCopyrightcBy);
		}
		{
			final JLabel lblPleaseSeeLicense = new JLabel(Messages.getString("AboutBox.5")); //$NON-NLS-1$
			this.contentPanel.add(lblPleaseSeeLicense);
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton(Messages.getString("AboutBox.6")); //$NON-NLS-1$
				okButton.addActionListener(e -> AboutBox.this.dispose());
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
