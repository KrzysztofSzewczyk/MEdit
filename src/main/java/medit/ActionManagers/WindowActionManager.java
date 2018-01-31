package medit.ActionManagers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import medit.MainFrame;

public class WindowActionManager {
	private MainFrame instance;
	
	public WindowActionManager(MainFrame instance) {
		this.instance = instance;
	}
	
	public void Closing() {
		instance.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent arg0) {
				if (MainFrame.instances == 0)
					System.exit(0);
				else
					MainFrame.instances--;
			}
			
			@Override
			public void windowClosing(final WindowEvent arg0) {
				if (MainFrame.instances == 0)
					System.exit(0);
				else
					MainFrame.instances--;
			}
		});
	}
}
