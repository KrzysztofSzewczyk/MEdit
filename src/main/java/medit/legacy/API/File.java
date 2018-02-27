package medit.legacy.API;

import medit.legacy.MainFrame;

/**
 * Interface for current file script management for MEdit B++ scripts.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class File {

	MainFrame instance;

	public File(final MainFrame instance) {
		this.instance = instance;
	}

	public java.io.File getFile() {
		return this.instance.currentFile;
	}

	public void setFile(final java.io.File file) {
		this.instance.currentFile = file;
	}

}
