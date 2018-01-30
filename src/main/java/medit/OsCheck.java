package medit;

import java.util.Locale;

public final class OsCheck {
	public enum OSType {
		Linux, MacOS, Other, Windows
	};

	protected static OSType detectedOS;

	public static OSType getOperatingSystemType() {
		if (OsCheck.detectedOS == null) {
			final String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
			if (OS.indexOf("mac") >= 0 || OS.indexOf("darwin") >= 0)
				OsCheck.detectedOS = OSType.MacOS;
			else if (OS.indexOf("win") >= 0)
				OsCheck.detectedOS = OSType.Windows;
			else if (OS.indexOf("nux") >= 0)
				OsCheck.detectedOS = OSType.Linux;
			else
				OsCheck.detectedOS = OSType.Other;
		}
		return OsCheck.detectedOS;
	}
}