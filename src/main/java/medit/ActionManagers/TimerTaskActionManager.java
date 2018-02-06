package medit.ActionManagers;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.util.Timer;
import java.util.TimerTask;

import medit.MainFrame;

/**
 * This very important class is setting up timers, which free memory every
 * 60seconds, if amount of used memory is bigger or equal to 200mb. Please
 * remember that this also is using timer to update bottom label of main window
 * content.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class TimerTaskActionManager {

	/**
	 * MainFrame instance used by this class to reference bottombar.
	 */

	private final MainFrame instance;

	/**
	 * This is constructor that we pass MainFrame instance to.
	 *
	 * @param instance
	 */

	public TimerTaskActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Timers updating labels and running GC every minute, if ram ussage hits 300
	 * MB.
	 */

	public void SetUpTimers() {
		final Timer gctimer = new Timer();
		gctimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024 * 1024 >= 300)
					Runtime.getRuntime().gc();
			}
		}, 0, 60);
		final Timer labeltimer = new Timer();
		labeltimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerTaskActionManager.this.instance.lblReady.setText("Ready | Length: "
						+ TimerTaskActionManager.this.instance.textPane.getText().length() + " | Filename: \""
						+ (TimerTaskActionManager.this.instance.currentFile == null ? "Unnamed"
								: TimerTaskActionManager.this.instance.currentFile.getAbsolutePath())
						+ "\" | Maximum size: "
						+ (TimerTaskActionManager.this.instance.currentFile == null ? "?"
								: TimerTaskActionManager.this.instance.currentFile.getFreeSpace() / 1024)
						+ "KB | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true ? "NUM"
								: "NONUM")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) == true ? "SCR"
								: "NOSCR")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) == true ? "CAPS"
								: "NOCAPS"));
				if (MainFrame.instances == 0)
					System.exit(0);
			}
		}, 0, 1);
	}
}
