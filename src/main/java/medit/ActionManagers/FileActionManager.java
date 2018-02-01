package medit.ActionManagers;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

	private MainFrame instance;

	public FileActionManager(MainFrame instance) {
		this.instance = instance;
	}

	public void New(JToolBar toolBar) {
		final JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(e -> EventQueue.invokeLater(() -> {
			try {
				new Thread(new Runnable() {
					@Override
					public void run() {
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
					}
				}).start();
			} catch (final Exception e1) {
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			instance.textPane.requestFocus();
		}));
		btnNewButton.setToolTipText("Create new file");
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-new.png")));
		toolBar.add(btnNewButton);
	}

	public void New(JMenu parent) {
		final JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mntmNew.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
			try {
				new Thread(new Runnable() {
					@Override
					public void run() {
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
					}
				}).start();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}));
		parent.add(mntmNew);
	}

	public void Open(JToolBar toolBar) {
		final JButton btnOpenButton = new JButton("");
		btnOpenButton.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					final JFileChooser chooser = new JFileChooser();
					if (chooser.showOpenDialog(instance) != JFileChooser.APPROVE_OPTION)
						return;
					try {
						instance.currentFile = chooser.getSelectedFile();
						final FileReader reader = new FileReader(chooser.getSelectedFile());
						final BufferedReader br = new BufferedReader(reader);
						instance.textPane.read(br, null);
						br.close();
						instance.textPane.requestFocus();
					} catch (final Exception e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}).start();
		});
		btnOpenButton.setToolTipText("Open existing file");
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-open.png")));
		toolBar.add(btnOpenButton);
	}

	public void Open(JMenu parent) {
		final JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.addActionListener(arg0 -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					final JFileChooser chooser = new JFileChooser();
					if (chooser.showOpenDialog(instance) != JFileChooser.APPROVE_OPTION)
						return;
					try {
						final FileReader reader = new FileReader(chooser.getSelectedFile());
						final BufferedReader br = new BufferedReader(reader);
						instance.textPane.read(br, null);
						br.close();
						instance.textPane.requestFocus();
						instance.currentFile = chooser.getSelectedFile();
					} catch (final Exception e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}).start();

		});
		parent.add(mntmOpen);
	}

	public void Save(JToolBar toolBar) {
		final JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (instance.currentFile == null) {
						final JFileChooser SaveAs = new JFileChooser();
						SaveAs.setApproveButtonText("Save");
						final int actionDialog = SaveAs.showSaveDialog(instance);
						if (actionDialog != JFileChooser.APPROVE_OPTION)
							return;

						final File fileName1 = SaveAs.getSelectedFile();
						instance.currentFile = SaveAs.getSelectedFile();
						BufferedWriter outFile1 = null;
						try {
							outFile1 = new BufferedWriter(new FileWriter(fileName1));
							instance.textPane.write(outFile1);
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
						instance.textPane.requestFocus();
					} else {
						final File fileName2 = instance.currentFile;
						BufferedWriter outFile2 = null;
						try {
							outFile2 = new BufferedWriter(new FileWriter(fileName2));
							instance.textPane.write(outFile2);
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
						instance.textPane.requestFocus();
					}
				}
			}).start();
		});
		btnSaveButton.setToolTipText("Save file");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-save.png")));
		toolBar.add(btnSaveButton);
	}

	public void Save(JMenu parent) {
		final JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (instance.currentFile == null) {
						final JFileChooser SaveAs = new JFileChooser();
						SaveAs.setApproveButtonText("Save");
						final int actionDialog = SaveAs.showSaveDialog(instance);
						if (actionDialog != JFileChooser.APPROVE_OPTION)
							return;

						final File fileName1 = SaveAs.getSelectedFile();
						instance.currentFile = SaveAs.getSelectedFile();
						BufferedWriter outFile1 = null;
						try {
							outFile1 = new BufferedWriter(new FileWriter(fileName1));
							instance.textPane.write(outFile1);
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
						instance.textPane.requestFocus();
					} else {
						final File fileName2 = instance.currentFile;
						BufferedWriter outFile2 = null;
						try {
							outFile2 = new BufferedWriter(new FileWriter(fileName2));
							instance.textPane.write(outFile2);
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
						instance.textPane.requestFocus();
					}
				}
			}).start();
		});
		parent.add(mntmSave);
	}

	public void ReloadFromDisk(JMenu parent) {
		JMenuItem mntmReloadFileFrom = new JMenuItem("Reload file from disk");
		mntmReloadFileFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (instance.currentFile == null)
							return;
						else {
							FileReader reader = null;
							try {
								reader = new FileReader(instance.currentFile);
							} catch (FileNotFoundException e2) {
								final Crash dialog4 = new Crash(e2);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
							final BufferedReader br = new BufferedReader(reader);
							try {
								instance.textPane.read(br, null);
							} catch (IOException e1) {
								final Crash dialog4 = new Crash(e1);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
							try {
								br.close();
							} catch (IOException e1) {
								final Crash dialog4 = new Crash(e1);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
							instance.textPane.requestFocus();
						}
					}
				}).start();
			}
		});
		mntmReloadFileFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		parent.add(mntmReloadFileFrom);
	}

	public void OpenDir(JMenu parent) {
		JMenuItem mntmOpenContainingDirectory = new JMenuItem("Open containing directory");
		mntmOpenContainingDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (Desktop.isDesktopSupported()) {
							try {
								Desktop.getDesktop().open(instance.currentFile.getParentFile());
							} catch (IOException e1) {
								final Crash dialog4 = new Crash(e1);
								dialog4.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog4.setVisible(true);
							}
						}
					}
				}).start();
			}
		});
		mntmOpenContainingDirectory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		parent.add(mntmOpenContainingDirectory);
	}

	public void SaveAs(JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText("Save");
					final int actionDialog = SaveAs.showSaveDialog(instance);
					if (actionDialog != JFileChooser.APPROVE_OPTION)
						return;

					final File fileName = SaveAs.getSelectedFile();
					BufferedWriter outFile = null;
					try {
						outFile = new BufferedWriter(new FileWriter(fileName));
						instance.textPane.write(outFile);
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
					instance.currentFile = fileName;
					instance.textPane.requestFocus();
				}
			}).start();
		});
		parent.add(mntmSaveAs);
	}

	public void Exit(JToolBar toolBar) {
		final JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			instance.dispose();
		});
		btnSaveButton.setToolTipText("Close");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/list-remove.png")));
		toolBar.add(btnSaveButton);
	}

	public void Exit(JMenu parent) {
		final JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			instance.dispose();
		});
		parent.add(mntmExit);
	}

	public void Separator(JMenu parent) {
		final JSeparator separator = new JSeparator();
		parent.add(separator);
	}

	public void RemoveFromDisk(JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Remove from disk");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						instance.currentFile.delete();
					} catch (Exception E) {
						final Crash dialog2 = new Crash(E);
						dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog2.setVisible(true);
					}
				}
			}).start();
		});
		parent.add(mntmSaveAs);
	}

	public void Print(JMenu parent) {
		final JMenuItem mntmSaveAs = new JMenuItem("Print ...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					PrintActionManager.printComponent(instance.textPane);
				}
			}).start();
		});
		parent.add(mntmSaveAs);
	}

}
