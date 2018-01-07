package medit;

import java.awt.EventQueue;

import javax.swing.UIManager;

/**
 * Main class for MEdit project. This thing is launching MainFrame window.
 *
 * @author Krzysztof Szewczyk
 */

public class Start {

	/**
	 * Main function of Start class. Creating MainFrame object.
	 *
	 * @param args
	 * @see medit.MainFrame
	 */

	public static void main(final String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				final MainFrame frame = new MainFrame();
				frame.setVisible(true);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		});
	}

}
