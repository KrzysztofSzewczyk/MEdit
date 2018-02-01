package medit.ActionManagers;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import medit.Crash;
import medit.MainFrame;

public class FileActionManager {

	private final MainFrame instance;

	public FileActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	public void Exit(final JMenu parent) {
		final JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			this.instance.dispose();
		});
		parent.add(mntmExit);
	}

	public void Exit(final JToolBar toolBar) {
		final JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			this.instance.dispose();
		});
		btnSaveButton.setToolTipText("Close");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/list-remove.png")));
		toolBar.add(btnSaveButton);
	}

	public void New(final JMenu parent) {
		final JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mntmNew.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
			try {
				new Thread(() -> {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
							| UnsupportedLookAndFeelException e) {
						final Crash dialog = new Crash(e);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
					final MainFrame frame = new MainFrame();
					frame.setVisible(true);
					MainFrame.instances++;
				}).start();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}));
		parent.add(mntmNew);
	}

	public void New(final JToolBar toolBar) {
		final JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(e -> EventQueue.invokeLater(() -> {
			try {
				new Thread(() -> {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
							| UnsupportedLookAndFeelException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
					final MainFrame frame = new MainFrame();
					frame.setVisible(true);
					MainFrame.instances++;
				}).start();
			} catch (final Exception e1) {
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			this.instance.textPane.requestFocus();
		}));
		btnNewButton.setToolTipText("Create new file");
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-new.png")));
		toolBar.add(btnNewButton);
	}

	public void Open(final JMenu parent) {
		final JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.addActionListener(arg0 -> {
			new Thread(() -> {
				final JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(FileActionManager.this.instance) != JFileChooser.APPROVE_OPTION)
					return;
				try {
					final FileReader reader = new FileReader(chooser.getSelectedFile());
					final BufferedReader br = new BufferedReader(reader);
					FileActionManager.this.instance.textPane.read(br, null);
					br.close();
					FileActionManager.this.instance.textPane.requestFocus();
					FileActionManager.this.instance.currentFile = chooser.getSelectedFile();
				} catch (final Exception e2) {
					final Crash dialog = new Crash(e2);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}).start();

		});
		parent.add(mntmOpen);
	}

	public void Open(final JToolBar toolBar) {
		final JButton btnOpenButton = new JButton("");
		btnOpenButton.addActionListener(e -> {
			new Thread(() -> {
				final JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(FileActionManager.this.instance) != JFileChooser.APPROVE_OPTION)
					return;
				try {
					FileActionManager.this.instance.currentFile = chooser.getSelectedFile();
					final FileReader reader = new FileReader(chooser.getSelectedFile());
					final BufferedReader br = new BufferedReader(reader);
					FileActionManager.this.instance.textPane.read(br, null);
					br.close();
					FileActionManager.this.instance.textPane.requestFocus();
				} catch (final Exception e2) {
					final Crash dialog = new Crash(e2);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}).start();
		});
		btnOpenButton.setToolTipText("Open existing file");
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-open.png")));
		toolBar.add(btnOpenButton);
	}

	public void OpenDir(final JMenu parent) {
		final JMenuItem mntmOpenContainingDirectory = new JMenuItem("Open containing directory");
		mntmOpenContainingDirectory.addActionListener(e -> new Thread(() -> {
			if (Desktop.isDesktopSupported())
				try {
					Desktop.getDesktop().open(FileActionManager.this.instance.currentFile.getParentFile());
				} catch (final IOException e1) {
					final Crash dialog4 = new Crash(e1);
					dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog4.setVisible(true);
				}
		}).start());
		mntmOpenContainingDirectory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		parent.add(mntmOpenContainingDirectory);
	}

	public void Print(final JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Print ...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(() -> PrintActionManager.printComponent(FileActionManager.this.instance.textPane)).start();
		});
		parent.add(mntmSaveAs);
	}

	public void ReloadFromDisk(final JMenu parent) {
		final JMenuItem mntmReloadFileFrom = new JMenuItem("Reload file from disk");
		mntmReloadFileFrom.addActionListener(e -> new Thread(() -> {
			if (FileActionManager.this.instance.currentFile == null)
				return;
			else {
				FileReader reader = null;
				try {
					reader = new FileReader(FileActionManager.this.instance.currentFile);
				} catch (final FileNotFoundException e2) {
					final Crash dialog41 = new Crash(e2);
					dialog41.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog41.setVisible(true);
				}
				final BufferedReader br = new BufferedReader(reader);
				try {
					FileActionManager.this.instance.textPane.read(br, null);
				} catch (final IOException e11) {
					final Crash dialog42 = new Crash(e11);
					dialog42.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog42.setVisible(true);
				}
				try {
					br.close();
				} catch (final IOException e12) {
					final Crash dialog43 = new Crash(e12);
					dialog43.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog43.setVisible(true);
				}
				FileActionManager.this.instance.textPane.requestFocus();
			}
		}).start());
		mntmReloadFileFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		parent.add(mntmReloadFileFrom);
	}

	public void RemoveFromDisk(final JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Remove from disk");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(() -> {
				try {
					FileActionManager.this.instance.currentFile.delete();
				} catch (final Exception E) {
					final Crash dialog2 = new Crash(E);
					dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog2.setVisible(true);
				}
			}).start();
		});
		parent.add(mntmSaveAs);
	}

	public void Save(final JMenu parent) {
		final JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(e -> {
			new Thread(() -> {
				if (FileActionManager.this.instance.currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText("Save");
					final int actionDialog = SaveAs.showSaveDialog(FileActionManager.this.instance);
					if (actionDialog != JFileChooser.APPROVE_OPTION)
						return;

					final File fileName1 = SaveAs.getSelectedFile();
					FileActionManager.this.instance.currentFile = SaveAs.getSelectedFile();
					BufferedWriter outFile1 = null;
					try {
						outFile1 = new BufferedWriter(new FileWriter(fileName1));
						FileActionManager.this.instance.textPane.write(outFile1);
					} catch (final IOException ex1) {
						final Crash dialog1 = new Crash(ex1);
						dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog1.setVisible(true);
					} finally {
						if (outFile1 != null)
							try {
								outFile1.close();
							} catch (final IOException e11) {
								final Crash dialog2 = new Crash(e11);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
							}
					}
					FileActionManager.this.instance.textPane.requestFocus();
				} else {
					final File fileName2 = FileActionManager.this.instance.currentFile;
					BufferedWriter outFile2 = null;
					try {
						outFile2 = new BufferedWriter(new FileWriter(fileName2));
						FileActionManager.this.instance.textPane.write(outFile2);
					} catch (final IOException ex2) {
						final Crash dialog3 = new Crash(ex2);
						dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog3.setVisible(true);
					} finally {
						if (outFile2 != null)
							try {
								outFile2.close();
							} catch (final IOException e12) {
								final Crash dialog4 = new Crash(e12);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
					}
					FileActionManager.this.instance.textPane.requestFocus();
				}
			}).start();
		});
		parent.add(mntmSave);
	}

	public void Save(final JToolBar toolBar) {
		final JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(e -> {
			new Thread(() -> {
				if (FileActionManager.this.instance.currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText("Save");
					final int actionDialog = SaveAs.showSaveDialog(FileActionManager.this.instance);
					if (actionDialog != JFileChooser.APPROVE_OPTION)
						return;

					final File fileName1 = SaveAs.getSelectedFile();
					FileActionManager.this.instance.currentFile = SaveAs.getSelectedFile();
					BufferedWriter outFile1 = null;
					try {
						outFile1 = new BufferedWriter(new FileWriter(fileName1));
						FileActionManager.this.instance.textPane.write(outFile1);
					} catch (final IOException ex1) {
						final Crash dialog1 = new Crash(ex1);
						dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog1.setVisible(true);
					} finally {
						if (outFile1 != null)
							try {
								outFile1.close();
							} catch (final IOException e11) {
								final Crash dialog2 = new Crash(e11);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
							}
					}
					FileActionManager.this.instance.textPane.requestFocus();
				} else {
					final File fileName2 = FileActionManager.this.instance.currentFile;
					BufferedWriter outFile2 = null;
					try {
						outFile2 = new BufferedWriter(new FileWriter(fileName2));
						FileActionManager.this.instance.textPane.write(outFile2);
					} catch (final IOException ex2) {
						final Crash dialog3 = new Crash(ex2);
						dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog3.setVisible(true);
					} finally {
						if (outFile2 != null)
							try {
								outFile2.close();
							} catch (final IOException e12) {
								final Crash dialog4 = new Crash(e12);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
					}
					FileActionManager.this.instance.textPane.requestFocus();
				}
			}).start();
		});
		btnSaveButton.setToolTipText("Save file");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-save.png")));
		toolBar.add(btnSaveButton);
	}

	public void SaveAs(final JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(() -> {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText("Save");
				final int actionDialog = SaveAs.showSaveDialog(FileActionManager.this.instance);
				if (actionDialog != JFileChooser.APPROVE_OPTION)
					return;

				final File fileName = SaveAs.getSelectedFile();
				BufferedWriter outFile = null;
				try {
					outFile = new BufferedWriter(new FileWriter(fileName));
					FileActionManager.this.instance.textPane.write(outFile);
				} catch (final IOException ex) {
					final Crash dialog1 = new Crash(ex);
					dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog1.setVisible(true);
				} finally {
					if (outFile != null)
						try {
							outFile.close();
						} catch (final IOException e1) {
							final Crash dialog2 = new Crash(e1);
							dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog2.setVisible(true);
						}
				}
				FileActionManager.this.instance.currentFile = fileName;
				FileActionManager.this.instance.textPane.requestFocus();
			}).start();
		});
		parent.add(mntmSaveAs);
	}

	public void Separator(final JMenu parent) {
		final JSeparator separator = new JSeparator();
		parent.add(separator);
	}

}
