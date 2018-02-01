package medit.ActionManagers;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import medit.AboutBox;

public class AboutActionManager {

	public void About(final JMenu parent) {
		final JMenuItem mntmAbout = new JMenuItem("About MEdit");
		mntmAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
		mntmAbout.addActionListener(arg0 -> {
			final AboutBox dialog = new AboutBox();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		});
		parent.add(mntmAbout);
	}

}
