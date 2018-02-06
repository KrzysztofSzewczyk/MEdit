package medit.ActionManagers;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import medit.MainFrame;

/**
 * This big class is a bit different from other ActionManagers, because it's
 * split into many methods. This class is setting up edit menu and edit toolbar
 * things.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class EditActionManager {

	/**
	 * This is MainFrame instance used by this class.
	 */

	private final MainFrame instance;

	/**
	 * This is constructor, that it's required to pass MainFrame instance to.
	 * 
	 * @param instance
	 */

	public EditActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * This function is creating copy menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Copy(final JMenuItem parent) {
		final JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.copy()).start());
		parent.add(mntmCopy);
	}

	/**
	 * This function is creating copy button in selected parent JToolBar.
	 * 
	 * @param toolBar
	 */

	public void Copy(final JToolBar toolBar) {
		final JButton btnCopyButton = new JButton("");
		btnCopyButton.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.copy()).start());
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);
	}

	/**
	 * This function is creating cut menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Cut(final JMenuItem parent) {
		final JMenuItem mntmCut = new JMenuItem("Cut");
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.cut()).start());
		parent.add(mntmCut);
	}

	/**
	 * This function is creating cut button in selected JToolBar.
	 * 
	 * @param toolBar
	 */

	public void Cut(final JToolBar toolBar) {
		final JButton btnCutButton = new JButton("");
		btnCutButton.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.cut()).start());
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);
	}

	/**
	 * This function is creating delete menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Delete(final JMenuItem parent) {
		final JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.replaceSelection("")).start());
		parent.add(mntmDelete);
	}

	/**
	 * This function is creating delete button in selected parent JToolBar
	 * 
	 * @param toolBar
	 */

	public void Delete(final JToolBar toolBar) {
		final JButton btnDeleteButton = new JButton("");
		btnDeleteButton.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.replaceSelection("")).start());
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);
	}

	/**
	 * This function is creating paste menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Paste(final JMenuItem parent) {
		final JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.paste()).start());
		parent.add(mntmPaste);
	}

	/**
	 * This function is creating paste button in selected JToolBar.
	 * 
	 * @param toolBar
	 */

	public void Paste(final JToolBar toolBar) {
		final JButton btnPasteButton = new JButton("");
		btnPasteButton
				.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.paste()).start());
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);
	}

	/**
	 * This function is creating redo menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Redo(final JMenuItem parent) {
		final JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.redoLastAction()).start());
		parent.add(mntmRedo);
	}

	/**
	 * This function is creating redo button in selected parent JToolBar.
	 * 
	 * @param toolBar
	 */

	public void Redo(final JToolBar toolBar) {
		final JButton btnRedoButton = new JButton("");
		btnRedoButton.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.redoLastAction()).start());
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);
	}

	/**
	 * This function is creating search menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Search(final JMenu parent) {
		final JMenuItem mntmUndo = new JMenuItem("Find");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> new Thread(() -> {
			instance.findDialog.setVisible(true);
		}).start());
		parent.add(mntmUndo);

		final JMenuItem mntmUndo2 = new JMenuItem("Replace");
		mntmUndo2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK));
		mntmUndo2.addActionListener(e -> new Thread(() -> {
			instance.replaceDialog.setVisible(true);
		}).start());
		parent.add(mntmUndo2);
	}

	/**
	 * This function is creating separator in selected parent.
	 * 
	 * @param parent
	 */

	public void Separator(final JMenuItem parent) {
		final JSeparator separator_4 = new JSeparator();
		parent.add(separator_4);
	}

	/**
	 * This function is creating Undo menu item in selected parent.
	 * 
	 * @param parent
	 */

	public void Undo(final JMenuItem parent) {
		final JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.undoLastAction()).start());
		parent.add(mntmUndo);
	}

	/**
	 * This function is creating undo menu item in selected parent JToolBar.
	 * 
	 * @param toolBar
	 */

	public void Undo(final JToolBar toolBar) {
		final JButton btnUndoButton = new JButton("");
		btnUndoButton.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.undoLastAction()).start());
		btnUndoButton.setToolTipText("Undo");
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-undo.png")));
		toolBar.add(btnUndoButton);
	}
}
