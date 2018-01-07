package medit;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

public class ToolMenuItem extends JMenuItem {

	private static final long serialVersionUID = -5890593284327977455L;

	public ToolMenuItem() {
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(Icon icon) {
		super(icon);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(String text) {
		super(text);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(Action a) {
		super(a);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(String text, Icon icon) {
		super(text, icon);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(String text, int mnemonic) {
		super(text, mnemonic);
		// TODO Auto-generated constructor stub
	}
	
	public int toolid;

}
