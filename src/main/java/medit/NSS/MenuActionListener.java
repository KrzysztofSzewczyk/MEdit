package medit.NSS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class MenuActionListener implements ActionListener {

	public String codefn, name;

	public MenuActionListener(final String nCodeFN, final String nName) {
		this.codefn = nCodeFN;
		this.name = nName;
	}

	@Override
	public abstract void actionPerformed(ActionEvent e);

}
