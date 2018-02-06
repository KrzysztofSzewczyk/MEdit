package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import medit.AboutBox;

/**
 * This action manager was splitted a little bit uselessly, but to keep
 * structure of MEdit, it's there.
 *
 * This class is creating menu item in selected menu that after clicking
 * displays an AboutBox dialog.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class AboutActionManager {

	/**
	 * This function is creating menu item in selected menu.
	 *
	 * @param parent
	 */

	public void About(final JMenu parent) {
		final JMenuItem mntmAbout = new JMenuItem("About MEdit");
		mntmAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
		mntmAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				final AboutBox dialog = new AboutBox();
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		parent.add(mntmAbout);
	}

}
