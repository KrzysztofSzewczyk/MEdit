package medit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;

/**
 * Main frame for MEdit project.
 * 
 * @author Krzysztof Szewczyk
 */

public class MainFrame extends JFrame {

	/**
	 * Serial version UID required by Eclipse
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private static int instances = 1;
	private MainFrame instance;
	private RSyntaxTextArea textPane = new RSyntaxTextArea();
	private File currentFile = null;
	private JLabel lblReady = new JLabel(
			"Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
	private JTextField searchTextField;
	private JTextField replaceWithTextField;

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		instance = this;
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				if (instances == 0)
					System.exit(0);
				else
					instances--;
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				if (instances == 0)
					System.exit(0);
				else
					instances--;
			}
		});
		setIconImage(Toolkit.getDefaultToolkit()
				.getImage(MainFrame.class.getResource("/medit/assets/apps/accessories-text-editor.png")));
		setTitle("MEdit");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		this.setMinimumSize(new Dimension(700, 500));

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mntmNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							MainFrame frame = new MainFrame();
							frame.setVisible(true);
							instances++;
							textPane.requestFocus();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		mnFile.add(mntmNew);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(instance) != JFileChooser.APPROVE_OPTION)
					return;
				try {
					FileReader reader = new FileReader(chooser.getSelectedFile());
					BufferedReader br = new BufferedReader(reader);
					textPane.read(br, null);
					br.close();
					textPane.requestFocus();
					currentFile = chooser.getSelectedFile();
				} catch (Exception e2) {
					Crash dialog = new Crash(e2);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		mnFile.add(mntmOpen);

		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText("Save");
					int actionDialog = SaveAs.showSaveDialog(instance);
					if (actionDialog != JFileChooser.APPROVE_OPTION) {
						return;
					}

					File fileName = SaveAs.getSelectedFile();
					BufferedWriter outFile = null;
					try {
						outFile = new BufferedWriter(new FileWriter(fileName));
						textPane.write(outFile);
					} catch (IOException ex) {
						Crash dialog = new Crash(ex);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					} finally {
						if (outFile != null) {
							try {
								outFile.close();
							} catch (IOException e1) {
								Crash dialog = new Crash(e1);
								dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);
							}
						}
					}
					currentFile = fileName;
					textPane.requestFocus();
				} else {
					File fileName = currentFile;
					BufferedWriter outFile = null;
					try {
						outFile = new BufferedWriter(new FileWriter(fileName));
						textPane.write(outFile);
					} catch (IOException ex) {
						Crash dialog = new Crash(ex);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					} finally {
						if (outFile != null) {
							try {
								outFile.close();
							} catch (IOException e1) {
								Crash dialog = new Crash(e1);
								dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);
							}
						}
					}
					textPane.requestFocus();
				}
			}
		});
		mnFile.add(mntmSave);

		JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText("Save");
				int actionDialog = SaveAs.showSaveDialog(instance);
				if (actionDialog != JFileChooser.APPROVE_OPTION) {
					return;
				}

				File fileName = SaveAs.getSelectedFile();
				BufferedWriter outFile = null;
				try {
					outFile = new BufferedWriter(new FileWriter(fileName));
					textPane.write(outFile);
				} catch (IOException ex) {
					Crash dialog = new Crash(ex);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} finally {
					if (outFile != null) {
						try {
							outFile.close();
						} catch (IOException e1) {
							Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						}
					}
				}
				currentFile = fileName;
				textPane.requestFocus();
			}
		});
		mnFile.add(mntmSaveAs);

		JSeparator separator = new JSeparator();
		mnFile.add(separator);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (instances == 0)
					return;
				instance.dispose();
			}
		});
		mnFile.add(mntmExit);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mntmCut = new JMenuItem("Cut");
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.cut();
			}
		});
		mnEdit.add(mntmCut);

		JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.copy();
			}
		});
		mnEdit.add(mntmCopy);

		JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.paste();
			}
		});
		mnEdit.add(mntmPaste);

		JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.replaceSelection("");
			}
		});
		mnEdit.add(mntmDelete);

		JSeparator separator_4 = new JSeparator();
		mnEdit.add(separator_4);

		JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.undoLastAction();
			}
		});
		mnEdit.add(mntmUndo);

		JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.redoLastAction();
			}
		});
		mnEdit.add(mntmRedo);

		JMenu mnLanguage = new JMenu("Language");
		menuBar.add(mnLanguage);
		
		JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem("English");
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);
		
		JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");
		menuBar.add(mnSyntaxHighlighting);
		
		JRadioButtonMenuItem rdbtnmntmNo = new JRadioButtonMenuItem("No");
		rdbtnmntmNo.setSelected(true);
		mnSyntaxHighlighting.add(rdbtnmntmNo);

		JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);

		JMenuItem mntmPreferences = new JMenuItem("Preferences");
		mntmPreferences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertiesBox dialog = new PropertiesBox(instance);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		mntmPreferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
		mnAbout.add(mntmPreferences);

		JMenuItem mntmAbout = new JMenuItem("About MEdit");
		mntmAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
		mntmAbout.addActionListener(new ActionListener() {
			/**
			 * MEdit About Box action listener.
			 */
			public void actionPerformed(ActionEvent arg0) {
				AboutBox dialog = new AboutBox();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		mnAbout.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		contentPane.add(toolBar, BorderLayout.NORTH);

		JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							MainFrame frame = new MainFrame();
							frame.setVisible(true);
							instances++;
						} catch (Exception e) {
							e.printStackTrace();
						}
						textPane.requestFocus();
					}
				});
			}
		});
		btnNewButton.setToolTipText("Create new file");
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-new.png")));
		toolBar.add(btnNewButton);

		JButton btnOpenButton = new JButton("");
		btnOpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				if (chooser.showOpenDialog(instance) != JFileChooser.APPROVE_OPTION)
					return;
				try {
					FileReader reader = new FileReader(chooser.getSelectedFile());
					BufferedReader br = new BufferedReader(reader);
					textPane.read(br, null);
					br.close();
					textPane.requestFocus();
				} catch (Exception e2) {
					Crash dialog = new Crash(e2);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		btnOpenButton.setToolTipText("Open existing file");
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-open.png")));
		toolBar.add(btnOpenButton);

		JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText("Save");
					int actionDialog = SaveAs.showSaveDialog(instance);
					if (actionDialog != JFileChooser.APPROVE_OPTION) {
						return;
					}

					File fileName = SaveAs.getSelectedFile();
					BufferedWriter outFile = null;
					try {
						outFile = new BufferedWriter(new FileWriter(fileName));
						textPane.write(outFile);
					} catch (IOException ex) {
						Crash dialog = new Crash(ex);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					} finally {
						if (outFile != null) {
							try {
								outFile.close();
							} catch (IOException e1) {
								Crash dialog = new Crash(e1);
								dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);
							}
						}
					}
					textPane.requestFocus();
				} else {
					File fileName = currentFile;
					BufferedWriter outFile = null;
					try {
						outFile = new BufferedWriter(new FileWriter(fileName));
						textPane.write(outFile);
					} catch (IOException ex) {
						Crash dialog = new Crash(ex);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					} finally {
						if (outFile != null) {
							try {
								outFile.close();
							} catch (IOException e1) {
								Crash dialog = new Crash(e1);
								dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);
							}
						}
					}
					textPane.requestFocus();
				}
			}
		});
		btnSaveButton.setToolTipText("Save file");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-save.png")));
		toolBar.add(btnSaveButton);

		JButton btnCloseButton = new JButton("");
		btnCloseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (instances == 0)
					return;
				instance.dispose();
			}
		});
		btnCloseButton.setToolTipText("Close file");
		btnCloseButton.setFocusPainted(false);
		btnCloseButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/status/image-missing.png")));
		toolBar.add(btnCloseButton);

		JButton btnCutButton = new JButton("");
		btnCutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.cut();
			}
		});
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);

		JButton btnCopyButton = new JButton("");
		btnCopyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.copy();
			}
		});
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);

		JButton btnPasteButton = new JButton("");
		btnPasteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.paste();
			}
		});
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);

		JButton btnDeleteButton = new JButton("");
		btnDeleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.replaceSelection("");
			}
		});
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);

		JButton btnUndoButton = new JButton("");
		btnUndoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.undoLastAction();
			}
		});
		btnUndoButton.setToolTipText("Undo");
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-undo.png")));
		toolBar.add(btnUndoButton);

		JButton btnRedoButton = new JButton("");
		btnRedoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.redoLastAction();
			}
		});
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);

		JButton btnPreferencesButton = new JButton("");
		btnPreferencesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertiesBox dialog = new PropertiesBox(instance);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		btnPreferencesButton.setToolTipText("Preferences");
		btnPreferencesButton.setFocusPainted(false);
		btnPreferencesButton.setIcon(
				new ImageIcon(MainFrame.class.getResource("/medit/assets/categories/applications-system.png")));
		toolBar.add(btnPreferencesButton);

		JToolBar toolBar_1 = new JToolBar();
		toolBar_1.setFloatable(false);
		contentPane.add(toolBar_1, BorderLayout.SOUTH);

		toolBar_1.add(lblReady);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(textPane);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel searchPanel = new JPanel();
		panel.add(searchPanel, BorderLayout.NORTH);

		JLabel lblSearch = new JLabel("Search");
		searchPanel.add(lblSearch);

		searchTextField = new JTextField();
		searchPanel.add(searchTextField);
		searchTextField.setColumns(10);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);

		JLabel lblReplace = new JLabel("Replace with");
		panel_2.add(lblReplace);

		replaceWithTextField = new JTextField();
		panel_2.add(replaceWithTextField);
		replaceWithTextField.setColumns(10);

		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel_3.add(panel_4, BorderLayout.NORTH);

		JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
				final int l2 = searchTextField.getText().length();
				if (l1 == -1) {
					JOptionPane.showMessageDialog(instance, "\"" + searchTextField.getText() + "\" not found");
				} else {
					textPane.select(l1, l2 + l1);
				}
			}
		});
		panel_4.add(btnSearch);

		JButton btnReplace = new JButton("Replace");
		btnReplace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
				final int l2 = searchTextField.getText().length();
				if (l1 == -1) {
					JOptionPane.showMessageDialog(instance, "\"" + searchTextField.getText() + "\" not found");
				} else {
					textPane.select(l1, l2 + l1);
					textPane.replaceSelection(replaceWithTextField.getText());
					textPane.select(l1, l2 + l1);
				}
			}
		});
		panel_4.add(btnReplace);

		JPanel panel_5 = new JPanel();
		panel_3.add(panel_5, BorderLayout.CENTER);
		panel_5.setLayout(new BorderLayout(0, 0));

		JPanel panel_6 = new JPanel();
		panel_5.add(panel_6, BorderLayout.NORTH);

		JButton btnCountOccurences = new JButton("Count Occurences");
		btnCountOccurences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int amount = 0;
				while(true) {
					final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
					final int l2 = searchTextField.getText().length();
					if (l1 == -1) {
						break;
					} else {
						textPane.setCaretPosition(l1+l2);
						amount++;
					}
				}
				JOptionPane.showMessageDialog(instance, "Found " + amount + " occurences.");
			}
		});
		panel_6.add(btnCountOccurences);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				lblReady.setText("Ready | Length: " + textPane.getText().length() + " | Filename: \""
						+ (currentFile == null ? "Unnamed" : currentFile.getAbsolutePath()) + "\" | Maximum size: "
						+ (currentFile == null ? "?" : currentFile.getFreeSpace() / 1024) + "KB | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true ? "NUM"
								: "NONUM")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) == true ? "SCR"
								: "NOSCR")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) == true ? "CAPS"
								: "NOCAPS"));
				if (instances == 0)
					System.exit(0);
			}
		}, 0, 1);
	}

}
