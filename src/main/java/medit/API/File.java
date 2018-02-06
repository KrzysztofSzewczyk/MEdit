package medit.API;

import medit.MainFrame;

/**
 * Interface for current file script management for MEdit B++ scripts.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class File {

	MainFrame instance;

	public File(MainFrame instance) {
		this.instance = instance;
	}

	public java.io.File getFile() {
		return instance.currentFile;
	}

	public void setFile(java.io.File file) {
		instance.currentFile = file;
	}

}
