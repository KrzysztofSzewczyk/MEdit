package medit.NTS;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class MenuActionListener implements ActionListener {

	public String code, name, exename;
	public MenuActionListener(String nCode, String nName, String nExename) {
		code = nCode;
		name = nName;
		exename = nExename;
	}
	
	@Override
	public abstract void actionPerformed(ActionEvent e);

}
