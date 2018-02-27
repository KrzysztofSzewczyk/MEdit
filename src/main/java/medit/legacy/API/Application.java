package medit.legacy.API;

import javax.swing.JMenuBar;

import medit.legacy.MainFrame;

/**
 * Main B++ API object.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class Application {

	private BottombarLabel bbLabel;

	private File currentFile;
	private MainFrame instance;
	private JMenuBar menuBar;

	/**
	 * Constructor that start.b++ will call
	 *
	 * @param instance
	 */

	public Application(final MainFrame instance) {
		this.instance = instance;
		this.bbLabel = new BottombarLabel(instance.lblReady);
		this.currentFile = new File(instance);
		this.menuBar = instance.getJMenuBar();
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is getter for BottombarLabel instance stored in this class.
	 *
	 * @return
	 */

	public BottombarLabel getBottombar() {
		return this.bbLabel;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is getter for currentFile instance stored in this class.
	 *
	 * @return
	 */

	public File getCurrentFile() {
		return this.currentFile;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is getter for MainFrame instance stored in this class.
	 *
	 * @return
	 */

	public MainFrame getInstance() {
		return this.instance;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is getter for JMenuBar instance stored in this class.
	 *
	 * @return
	 */

	public JMenuBar getMenuBar() {
		return this.menuBar;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is setter for BottombarLabel instance stored in this class.
	 *
	 * WARNING: MAIN WINDOW BOTTOMBAR WILL NOT BE UP TO DATE WITH INSTANCE STORED
	 * INSIDE THIS CLASS.
	 *
	 * @return
	 */

	public void setBottombar(final BottombarLabel l) {
		this.bbLabel = l;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is setter for currentFile instance stored in this class.
	 *
	 * WARNING: MAIN WINDOW CURRENT FILE WILL NOT BE UP TO DATE WITH INSTANCE STORED
	 * INSIDE THIS CLASS.
	 *
	 * @return
	 */

	public void setCurrentFile(final File currentFile) {
		this.currentFile = currentFile;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is setter for MainFrame instance stored in this class.
	 *
	 * WARNING: MAIN WINDOW INSTANCE WILL NOT BE UP TO DATE WITH INSTANCE STORED
	 * INSIDE THIS CLASS.
	 *
	 * @return
	 */

	public void setInstance(final MainFrame f) {
		this.instance = f;
	}

	/**
	 * Various functions to be used in scripts.
	 *
	 * This function is setter for JMenuBar instance stored in this class.
	 *
	 * WARNING: MAIN WINDOW JMENUBAR WILL NOT BE UP TO DATE WITH INSTANCE STORED
	 * INSIDE THIS CLASS.
	 *
	 * @return
	 */

	public void setMenuBar(final JMenuBar menuBar) {
		this.menuBar = menuBar;
	}

}
