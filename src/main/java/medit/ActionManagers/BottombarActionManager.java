package medit.ActionManagers;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import medit.MainFrame;

/**
 * This class is small, but it was needed to split it away from MainFrame to
 * keep code climate and internal structure of MEdit. It's setting up bottombar
 * containing information about current opened document.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class BottombarActionManager {

	/**
	 * MainFrame instance used by this class to create bottombar and it's text.
	 */

	private final MainFrame instance;

	/**
	 * This is constructor creating internal MainFrame instance.
	 * 
	 * @param instance
	 */

	public BottombarActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * This is function that is setting up bottom bar in internal MainFrame
	 * instance, that is passed to this class using constructor.
	 */

	public void SetUpBottombar() {
		final JPanel panel_14 = new JPanel();
		this.instance.contentPane.add(panel_14, BorderLayout.SOUTH);
		panel_14.setLayout(new BorderLayout(0, 0));

		final JToolBar toolBar_1 = new JToolBar();
		panel_14.add(toolBar_1);
		toolBar_1.setFloatable(false);

		toolBar_1.add(this.instance.lblReady);
	}

}
