package medit.ActionManagers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import medit.MainFrame;

/**
 * This function is watching on amount of MainFrame instances and exiting
 * program, if there are no instances left.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class WindowActionManager {
	/**
	 * This is MainFrame instance used by this class.
	 */

	private final MainFrame instance;

	/**
	 * Constructor assigning passed MainFrame instance to internal MainFrame
	 * instance.
	 *
	 * @param instance
	 */

	public WindowActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * This function is setting up handlers for MainFrame.
	 */

	public void Closing() {
		this.instance.addWindowListener(new WindowAdapter() {
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
