package medit.ActionManagers;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
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
		mntmCut.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.cut();
			}
		}).start());
		parent.add(mntmCut);
	}

	public void Copy(JMenuItem parent) {
		final JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.copy();
			}
		}).start());
		parent.add(mntmCopy);
	}

	public void Paste(JMenuItem parent) {
		final JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.paste();
			}
		}).start());
		parent.add(mntmPaste);
	}

	public void Delete(JMenuItem parent) {
		final JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.replaceSelection("");
			}
		}).start());
		parent.add(mntmDelete);
	}

	public void Separator(JMenuItem parent) {
		final JSeparator separator_4 = new JSeparator();
		parent.add(separator_4);
	}

	public void Undo(JMenuItem parent) {
		final JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.undoLastAction();
			}
		}).start());
		parent.add(mntmUndo);
	}

	public void Redo(JMenuItem parent) {
		final JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.redoLastAction();
			}
		}).start());
		parent.add(mntmRedo);
	}

	public void Cut(JToolBar toolBar) {
		final JButton btnCutButton = new JButton("");
		btnCutButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.cut();
			}
		}).start());
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);
	}

	public void Copy(JToolBar toolBar) {
		final JButton btnCopyButton = new JButton("");
		btnCopyButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.copy();
			}
		}).start());
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);
	}

	public void Paste(JToolBar toolBar) {
		final JButton btnPasteButton = new JButton("");
		btnPasteButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.paste();
			}
		}).start());
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);
	}

	public void Delete(JToolBar toolBar) {
		final JButton btnDeleteButton = new JButton("");
		btnDeleteButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.replaceSelection("");
			}
		}).start());
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);
	}

	public void Undo(JToolBar toolBar) {
		final JButton btnUndoButton = new JButton("");
		btnUndoButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.undoLastAction();
			}
		}).start());
		btnUndoButton.setToolTipText("Undo");
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-undo.png")));
		toolBar.add(btnUndoButton);
	}

	public void Redo(JToolBar toolBar) {
		final JButton btnRedoButton = new JButton("");
		btnRedoButton.addActionListener(e -> new Thread(new Runnable() {
			@Override
			public void run() {
				instance.textPane.redoLastAction();
			}
		}).start());
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);
	}
}
