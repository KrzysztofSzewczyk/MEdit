package medit;

import java.awt.EventQueue;

import javax.swing.UIManager;

/**
 * Main class for MEdit project.
 * This thing is launching MainFrame window.
 * @author Krzysztof Szewczyk
 */

public class Start {

	/**
	 * Main function of Start class.
	 * Creating MainFrame object.
	 * @param args
	 * @see medit.MainFrame
	 */
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
