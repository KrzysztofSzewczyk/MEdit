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
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(AboutBox.class.getResource("/medit/assets/apps/help-browser.png")));
		this.setTitle("About MEdit");
		this.setBounds(100, 100, 632, 201);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setLayout(new FlowLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		{
			final JLabel lblMeditIsFree = new JLabel("MEdit is free software redistributed under MIT license");
			this.contentPanel.add(lblMeditIsFree);
		}
		{
			final JLabel lblDevcppIconsWere = new JLabel(
					"Tango Icon Library was used. It belongs to it's author(s) and it's redistributed under GPLv2 license.");
			this.contentPanel.add(lblDevcppIconsWere);
		}
		{
			final JLabel lblCopyrightcBy = new JLabel("Copyright (C) by Krzysztof Szewczyk 2018. All rights reserved.");
			this.contentPanel.add(lblCopyrightcBy);
		}
		{
			final JLabel lblPleaseSeeLicense = new JLabel(" Please see LICENSE for details. Every package that I didn't made, has own LICENSE inside.");
			this.contentPanel.add(lblPleaseSeeLicense);
		}
		{
			final JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			this.getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				final JButton okButton = new JButton("OK");
				okButton.addActionListener(e -> AboutBox.this.dispose());
				buttonPane.add(okButton);
				this.getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
