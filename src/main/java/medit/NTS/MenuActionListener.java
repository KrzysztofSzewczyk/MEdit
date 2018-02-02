package medit.NTS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class MenuActionListener implements ActionListener {

	public String code, name, exename;

	public MenuActionListener(final String nCode, final String nName, final String nExename) {
		this.code = nCode;
		this.name = nName;
		this.exename = nExename;
	}

	@Override
	public abstract void actionPerformed(ActionEvent e);

}
