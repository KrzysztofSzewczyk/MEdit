package medit;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

public class ToolMenuItem extends JMenuItem {

	private static final long serialVersionUID = -5890593284327977455L;

	public int toolid;

	public ToolMenuItem() {
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(final Action a) {
		super(a);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(final Icon icon) {
		super(icon);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(final String text) {
		super(text);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(final String text, final Icon icon) {
		super(text, icon);
		// TODO Auto-generated constructor stub
	}

	public ToolMenuItem(final String text, final int mnemonic) {
		super(text, mnemonic);
		// TODO Auto-generated constructor stub
	}

}
