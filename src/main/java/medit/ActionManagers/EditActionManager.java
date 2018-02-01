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
import javax.swing.WindowConstants;

import medit.MainFrame;
import medit.SearchWindow;

public class EditActionManager {

	private final MainFrame instance;

	public EditActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	public void Copy(final JMenuItem parent) {
		final JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.copy()).start());
		parent.add(mntmCopy);
	}

	public void Copy(final JToolBar toolBar) {
		final JButton btnCopyButton = new JButton("");
		btnCopyButton.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.copy()).start());
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);
	}

	public void Cut(final JMenuItem parent) {
		final JMenuItem mntmCut = new JMenuItem("Cut");
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.cut()).start());
		parent.add(mntmCut);
	}

	public void Cut(final JToolBar toolBar) {
		final JButton btnCutButton = new JButton("");
		btnCutButton.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.cut()).start());
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);
	}

	public void Delete(final JMenuItem parent) {
		final JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.replaceSelection("")).start());
		parent.add(mntmDelete);
	}

	public void Delete(final JToolBar toolBar) {
		final JButton btnDeleteButton = new JButton("");
		btnDeleteButton.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.replaceSelection("")).start());
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);
	}

	public void Paste(final JMenuItem parent) {
		final JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.paste()).start());
		parent.add(mntmPaste);
	}

	public void Paste(final JToolBar toolBar) {
		final JButton btnPasteButton = new JButton("");
		btnPasteButton
				.addActionListener(e -> new Thread(() -> EditActionManager.this.instance.textPane.paste()).start());
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);
	}

	public void Redo(final JMenuItem parent) {
		final JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.redoLastAction()).start());
		parent.add(mntmRedo);
	}

	public void Redo(final JToolBar toolBar) {
		final JButton btnRedoButton = new JButton("");
		btnRedoButton.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.redoLastAction()).start());
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);
	}

	public void Search(final JMenu parent) {
		final JMenuItem mntmUndo = new JMenuItem("Find/Replace/Count Occurences");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> new Thread(() -> {
			final SearchWindow dialog = new SearchWindow(EditActionManager.this.instance);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}).start());
		parent.add(mntmUndo);
	}

	public void Separator(final JMenuItem parent) {
		final JSeparator separator_4 = new JSeparator();
		parent.add(separator_4);
	}

	public void Undo(final JMenuItem parent) {
		final JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(
				e -> new Thread(() -> EditActionManager.this.instance.textPane.undoLastAction()).start());
		parent.add(mntmUndo);
	}

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
