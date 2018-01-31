package medit.ActionManagers;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import medit.MainFrame;

public class EditActionManager {

	private MainFrame instance;
	
	public EditActionManager(MainFrame instance) {
		this.instance = instance;
	}
	
	public void Cut(JMenuItem parent) {
		final JMenuItem mntmCut = new JMenuItem("Cut");
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(e -> instance.textPane.cut());
		parent.add(mntmCut);
	}
	
	public void Copy(JMenuItem parent) {
		final JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> instance.textPane.copy());
		parent.add(mntmCopy);
	}
	
	public void Paste(JMenuItem parent) {
		final JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> instance.textPane.paste());
		parent.add(mntmPaste);
	}
	
	public void Delete(JMenuItem parent) {
		final JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(e -> instance.textPane.replaceSelection(""));
		parent.add(mntmDelete);
	}
	
	public void Separator(JMenuItem parent) {
		final JSeparator separator_4 = new JSeparator();
		parent.add(separator_4);
	}
	
	public void Undo(JMenuItem parent) {
		final JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> instance.textPane.undoLastAction());
		parent.add(mntmUndo);
	}
	
	public void Redo(JMenuItem parent) {
		final JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(e -> instance.textPane.redoLastAction());
		parent.add(mntmRedo);
	}

}
