package medit.NTS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is MenuActionListener class that is allowing
 * NTS to use various variables inside ActionListeners.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public abstract class MenuActionListener implements ActionListener {

	public String code, name, exename;

	/**
	 * This function is assigning parameters to
	 * their internal copies.
	 * @param nCode
	 * @param nName
	 * @param nExename
	 */
	
	public MenuActionListener(final String nCode, final String nName, final String nExename) {
		this.code = nCode;
		this.name = nName;
		this.exename = nExename;
	}

	/**
	 * We want to keep it abstract.
	 */
	
	@Override
	public abstract void actionPerformed(ActionEvent e);

}
