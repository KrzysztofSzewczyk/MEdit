package medit.API;

import javax.swing.JMenuBar;

import medit.MainFrame;

/**
 * Main B++ API object.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class Application {

	private MainFrame instance;

	private BottombarLabel bbLabel;
	private File currentFile;
	private JMenuBar menuBar;

	/**
	 * Constructor that start.b++ will call
	 * 
	 * @param instance
	 */

	public Application(MainFrame instance) {
		this.instance = instance;
		this.bbLabel = new BottombarLabel(instance.lblReady);
		this.currentFile = new File(instance);
		this.menuBar = instance.getJMenuBar();
	}

	/**
	 * Various functions to be used in scripts.
	 * 
	 * This function is getter for MainFrame instance stored in this class.
	 * 
	 * @return
	 */

	public MainFrame getInstance() {
		return instance;
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

	public void setInstance(MainFrame f) {
		instance = f;
	}

	/**
	 * Various functions to be used in scripts.
	 * 
	 * This function is getter for BottombarLabel instance stored in this class.
	 * 
	 * @return
	 */

	public BottombarLabel getBottombar() {
		return bbLabel;
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

	public void setBottombar(BottombarLabel l) {
		bbLabel = l;
	}

	/**
	 * Various functions to be used in scripts.
	 * 
	 * This function is getter for currentFile instance stored in this class.
	 * 
	 * @return
	 */

	public File getCurrentFile() {
		return currentFile;
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

	public void setCurrentFile(File currentFile) {
		this.currentFile = currentFile;
	}

	/**
	 * Various functions to be used in scripts.
	 * 
	 * This function is getter for JMenuBar instance stored in this class.
	 * 
	 * @return
	 */

	public JMenuBar getMenuBar() {
		return menuBar;
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

	public void setMenuBar(JMenuBar menuBar) {
		this.menuBar = menuBar;
	}

}
