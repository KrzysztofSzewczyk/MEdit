package medit.ActionManagers;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.util.Timer;
import java.util.TimerTask;

import medit.MainFrame;

public class TimerTaskActionManager {

	private MainFrame instance;

	public TimerTaskActionManager(MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Timers updating labels and running GC every minute, if ram ussage hits 200
	 * MB.
	 */

	public void SetUpTimers() {
		final Timer gctimer = new Timer();
		gctimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1024 * 1024 >= 200) {
					Runtime.getRuntime().gc();
				}
			}
		}, 0, 60);
		final Timer labeltimer = new Timer();
		labeltimer.schedule(new TimerTask() {
			@Override
			public void run() {
				instance.lblReady.setText("Ready | Length: " + instance.textPane.getText().length() + " | Filename: \""
						+ (instance.currentFile == null ? "Unnamed" : instance.currentFile.getAbsolutePath())
						+ "\" | Maximum size: "
						+ (instance.currentFile == null ? "?" : instance.currentFile.getFreeSpace() / 1024) + "KB | "
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
