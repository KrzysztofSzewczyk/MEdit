package medit.legacy;

import java.awt.EventQueue;

import javax.swing.UIManager;

public class Start {

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					final MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(0); // Fixing #4
				}
			}
		});
	}

}
