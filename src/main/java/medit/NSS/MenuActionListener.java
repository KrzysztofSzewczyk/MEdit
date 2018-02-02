package medit.NSS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is MenuActionListener class that is allowing
 * NSS to use various variables inside ActionListeners.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public abstract class MenuActionListener implements ActionListener {

	public String codefn, name;

	/**
	 * This constructor is assigning data to this class'
	 * variables.
	 * @param nCodeFN
	 * @param nName
	 */
	
	public MenuActionListener(final String nCodeFN, final String nName) {
		this.codefn = nCodeFN;
		this.name = nName;
	}

	/**
	 * We want to keep it abstract.
	 */
	
	@Override
	public abstract void actionPerformed(ActionEvent e);

}
