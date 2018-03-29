package medit.legacy;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.WindowConstants;

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
					final MainFrame frame = new MainFrame(args);
					frame.setVisible(true);
				} catch (final Exception e) {
					final Crash dialog = new Crash(e);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
					System.exit(0); // Fixing #4
				}
			}
		});
	}

}
