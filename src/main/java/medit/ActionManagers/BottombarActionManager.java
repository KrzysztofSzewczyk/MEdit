package medit.ActionManagers;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import medit.MainFrame;

public class BottombarActionManager {

	private MainFrame instance;

	public BottombarActionManager(MainFrame instance) {
		this.instance = instance;
	}
	
	public void SetUpBottombar() {
		final JPanel panel_14 = new JPanel();
		instance.contentPane.add(panel_14, BorderLayout.SOUTH);
		panel_14.setLayout(new BorderLayout(0, 0));

		final JToolBar toolBar_1 = new JToolBar();
		panel_14.add(toolBar_1);
		toolBar_1.setFloatable(false);

		toolBar_1.add(instance.lblReady);
	}
	
}
