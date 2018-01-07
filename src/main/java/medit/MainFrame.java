package medit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import java.util.ResourceBundle;

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
	private JLabel lblReady = new JLabel(Messages.getString("MainFrame.0")); //$NON-NLS-1$
	private JTextField searchTextField;
	private JTextField replaceWithTextField;
	private Tool[] tools = new Tool[32];
	private Script[] scripts = new Script[32];
	private int scriptAmount = 0;
	private int toolAmount = 0;
	private JTextPane toolConsole = new JTextPane();

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
		addKeyListener(new KeyAdapter() {
			private String getFileExtension(File file) {
				String name = file.getName();
				try {
					return name.substring(name.lastIndexOf(Messages.getString("MainFrame.1")) + 1); //$NON-NLS-1$
				} catch (Exception e) {
					return Messages.getString("MainFrame.2"); //$NON-NLS-1$
				}
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				char c = arg0.getKeyChar();
				if (!arg0.isAltDown())
					return;
				int found = -1;
				for (int i = 0; i < tools.length; i++) {
					if (tools[i].hotkey.charAt(0) == c) {
						found = i;
					}
				}
				if (found == -1) {
					// Keep on dispatching, dear Java.
				} else {
					int toolid = found;
					try {
						String copy = tools[toolid].commandline;
						copy = copy.replaceAll(Messages.getString("MainFrame.3"), //$NON-NLS-1$
								currentFile == null ? Messages.getString("MainFrame.4") : currentFile.getName()); //$NON-NLS-1$
						copy = copy.replaceAll(Messages.getString("MainFrame.5"), //$NON-NLS-1$
								currentFile == null ? Messages.getString("MainFrame.6") //$NON-NLS-1$
										: currentFile.getParentFile().getAbsolutePath());
						copy = copy.replaceAll(Messages.getString("MainFrame.7"), //$NON-NLS-1$
								currentFile == null ? Messages.getString("MainFrame.8") //$NON-NLS-1$
										: getFileExtension(currentFile));
						Process p = Runtime.getRuntime().exec(
								tools[toolid].path + Messages.getString("MainFrame.9") + tools[toolid].commandline); //$NON-NLS-1$
						new Thread(new Runnable() {
							@Override
							public void run() {
								BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

								BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

								toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.10")); //$NON-NLS-1$
								String s = null;
								try {
									while ((s = stdInput.readLine()) != null) {
										toolConsole.setText(toolConsole.getText() + s);
									}
								} catch (IOException e1) {
									Crash dialog = new Crash(e1);
									dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
									dialog.setVisible(true);
									return;
								}

								// read any errors from the attempted command
								toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.11")); //$NON-NLS-1$
								try {
									while ((s = stdError.readLine()) != null) {
										toolConsole.setText(toolConsole.getText() + s);
									}
								} catch (IOException e) {
									Crash dialog = new Crash(e);
									dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
									dialog.setVisible(true);
									return;
								}
								return;
							}
						}).start();
					} catch (IOException e1) {
						Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}
		});
		setIconImage(
				Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource(Messages.getString("MainFrame.12")))); //$NON-NLS-1$
		setTitle(Messages.getString("MainFrame.13")); //$NON-NLS-1$
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		this.setMinimumSize(new Dimension(700, 500));

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu(Messages.getString("MainFrame.14")); //$NON-NLS-1$
		menuBar.add(mnFile);

		JMenuItem mntmNew = new JMenuItem(Messages.getString("MainFrame.15")); //$NON-NLS-1$
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

		JMenuItem mntmOpen = new JMenuItem(Messages.getString("MainFrame.16")); //$NON-NLS-1$
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

		JMenuItem mntmSave = new JMenuItem(Messages.getString("MainFrame.17")); //$NON-NLS-1$
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText(Messages.getString("MainFrame.18")); //$NON-NLS-1$
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

		JMenuItem mntmSaveAs = new JMenuItem(Messages.getString("MainFrame.19")); //$NON-NLS-1$
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText(Messages.getString("MainFrame.20")); //$NON-NLS-1$
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

		JMenuItem mntmExit = new JMenuItem(Messages.getString("MainFrame.21")); //$NON-NLS-1$
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (instances == 0)
					return;
				instance.dispose();
			}
		});
		mnFile.add(mntmExit);

		JMenu mnEdit = new JMenu(Messages.getString("MainFrame.22")); //$NON-NLS-1$
		menuBar.add(mnEdit);

		JMenuItem mntmCut = new JMenuItem(Messages.getString("MainFrame.23")); //$NON-NLS-1$
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.cut();
			}
		});
		mnEdit.add(mntmCut);

		JMenuItem mntmCopy = new JMenuItem(Messages.getString("MainFrame.24")); //$NON-NLS-1$
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.copy();
			}
		});
		mnEdit.add(mntmCopy);

		JMenuItem mntmPaste = new JMenuItem(Messages.getString("MainFrame.25")); //$NON-NLS-1$
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.paste();
			}
		});
		mnEdit.add(mntmPaste);

		JMenuItem mntmDelete = new JMenuItem(Messages.getString("MainFrame.26")); //$NON-NLS-1$
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.replaceSelection(Messages.getString("MainFrame.27")); //$NON-NLS-1$
			}
		});
		mnEdit.add(mntmDelete);

		JSeparator separator_4 = new JSeparator();
		mnEdit.add(separator_4);

		JMenuItem mntmUndo = new JMenuItem(Messages.getString("MainFrame.28")); //$NON-NLS-1$
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.undoLastAction();
			}
		});
		mnEdit.add(mntmUndo);

		JMenuItem mntmRedo = new JMenuItem(Messages.getString("MainFrame.29")); //$NON-NLS-1$
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.redoLastAction();
			}
		});
		mnEdit.add(mntmRedo);

		JMenu mnLanguage = new JMenu(Messages.getString("MainFrame.30")); //$NON-NLS-1$
		menuBar.add(mnLanguage);

		JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem(Messages.getString("MainFrame.31")); //$NON-NLS-1$
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);

		JMenu mnSyntaxHighlighting = new JMenu(Messages.getString("MainFrame.32")); //$NON-NLS-1$
		menuBar.add(mnSyntaxHighlighting);

		JMenuItem mntmNo = new JMenuItem(Messages.getString("MainFrame.33")); //$NON-NLS-1$
		mntmNo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
			}
		});
		mnSyntaxHighlighting.add(mntmNo);

		JMenu mnA = new JMenu(Messages.getString("MainFrame.34")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnA);

		JMenuItem mntmActionscript = new JMenuItem(Messages.getString("MainFrame.35")); //$NON-NLS-1$
		mnA.add(mntmActionscript);

		JMenuItem mntmAssembler = new JMenuItem(Messages.getString("MainFrame.36")); //$NON-NLS-1$
		mnA.add(mntmAssembler);
		mntmAssembler.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
			}
		});
		mntmActionscript.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT);
			}
		});

		JMenuItem mntmBbcode = new JMenuItem(Messages.getString("MainFrame.37")); //$NON-NLS-1$
		mntmBbcode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BBCODE);
			}
		});
		mnSyntaxHighlighting.add(mntmBbcode);

		JMenu mnC = new JMenu(Messages.getString("MainFrame.38")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnC);

		JMenuItem mntmC = new JMenuItem(Messages.getString("MainFrame.39")); //$NON-NLS-1$
		mnC.add(mntmC);

		JMenuItem mntmC_1 = new JMenuItem(Messages.getString("MainFrame.40")); //$NON-NLS-1$
		mnC.add(mntmC_1);

		JMenuItem mntmC_2 = new JMenuItem(Messages.getString("MainFrame.41")); //$NON-NLS-1$
		mnC.add(mntmC_2);

		JMenuItem mntmClojure = new JMenuItem(Messages.getString("MainFrame.42")); //$NON-NLS-1$
		mnC.add(mntmClojure);
		mntmClojure.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CLOJURE);
			}
		});
		mntmC_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
			}
		});
		mntmC_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
			}
		});
		mntmC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
			}
		});

		JMenu mnD = new JMenu(Messages.getString("MainFrame.43")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnD);

		JMenuItem mntmDart = new JMenuItem(Messages.getString("MainFrame.44")); //$NON-NLS-1$
		mnD.add(mntmDart);

		JMenuItem mntmDelphi = new JMenuItem(Messages.getString("MainFrame.45")); //$NON-NLS-1$
		mnD.add(mntmDelphi);

		JMenuItem mntmDocker = new JMenuItem(Messages.getString("MainFrame.46")); //$NON-NLS-1$
		mnD.add(mntmDocker);

		JMenuItem mntmDtd = new JMenuItem(Messages.getString("MainFrame.47")); //$NON-NLS-1$
		mnD.add(mntmDtd);

		JMenuItem mntmD = new JMenuItem(Messages.getString("MainFrame.48")); //$NON-NLS-1$
		mnD.add(mntmD);
		mntmD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_D);
			}
		});
		mntmDtd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DTD);
			}
		});
		mntmDocker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE);
			}
		});
		mntmDelphi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DELPHI);
			}
		});
		mntmDart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DART);
			}
		});

		JMenuItem mntmFortan = new JMenuItem(Messages.getString("MainFrame.49")); //$NON-NLS-1$
		mntmFortan.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_FORTRAN);
			}
		});
		mnSyntaxHighlighting.add(mntmFortan);

		JMenuItem mntmGroovy = new JMenuItem(Messages.getString("MainFrame.50")); //$NON-NLS-1$
		mntmGroovy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
			}
		});
		mnSyntaxHighlighting.add(mntmGroovy);

		JMenu mnH = new JMenu(Messages.getString("MainFrame.51")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnH);

		JMenuItem mntmHtaccess = new JMenuItem(Messages.getString("MainFrame.52")); //$NON-NLS-1$
		mnH.add(mntmHtaccess);
		mntmHtaccess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTACCESS);
			}
		});

		JMenuItem mntmHosts = new JMenuItem(Messages.getString("MainFrame.53")); //$NON-NLS-1$
		mnH.add(mntmHosts);

		JMenuItem mntmHtml = new JMenuItem(Messages.getString("MainFrame.54")); //$NON-NLS-1$
		mnH.add(mntmHtml);
		mntmHtml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
			}
		});
		mntmHosts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HOSTS);
			}
		});

		JMenuItem mntmIni = new JMenuItem(Messages.getString("MainFrame.55")); //$NON-NLS-1$
		mntmIni.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_INI);
			}
		});
		mnSyntaxHighlighting.add(mntmIni);

		JMenu mnJ = new JMenu(Messages.getString("MainFrame.56")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnJ);

		JMenuItem mntmJavascript = new JMenuItem(Messages.getString("MainFrame.57")); //$NON-NLS-1$
		mnJ.add(mntmJavascript);

		JMenuItem mntmJava = new JMenuItem(Messages.getString("MainFrame.58")); //$NON-NLS-1$
		mnJ.add(mntmJava);

		JMenuItem mntmJshintrc = new JMenuItem(Messages.getString("MainFrame.59")); //$NON-NLS-1$
		mnJ.add(mntmJshintrc);

		JMenuItem mntmJsp = new JMenuItem(Messages.getString("MainFrame.60")); //$NON-NLS-1$
		mnJ.add(mntmJsp);
		mntmJsp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSP);
			}
		});
		mntmJshintrc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
			}
		});
		mntmJava.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			}
		});
		mntmJavascript.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
			}
		});

		JMenu mnL = new JMenu(Messages.getString("MainFrame.61")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnL);

		JMenuItem mntmLatex = new JMenuItem(Messages.getString("MainFrame.62")); //$NON-NLS-1$
		mnL.add(mntmLatex);

		JMenuItem mntmLess = new JMenuItem(Messages.getString("MainFrame.63")); //$NON-NLS-1$
		mnL.add(mntmLess);

		JMenuItem mntmLisp = new JMenuItem(Messages.getString("MainFrame.64")); //$NON-NLS-1$
		mnL.add(mntmLisp);

		JMenuItem mntmLua = new JMenuItem(Messages.getString("MainFrame.65")); //$NON-NLS-1$
		mnL.add(mntmLua);
		mntmLua.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
			}
		});
		mntmLisp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LISP);
			}
		});
		mntmLess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LESS);
			}
		});
		mntmLatex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX);
			}
		});

		JMenu mnM = new JMenu(Messages.getString("MainFrame.66")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnM);

		JMenuItem mntmMakeFile = new JMenuItem(Messages.getString("MainFrame.67")); //$NON-NLS-1$
		mnM.add(mntmMakeFile);

		JMenuItem mntmMxml = new JMenuItem(Messages.getString("MainFrame.68")); //$NON-NLS-1$
		mnM.add(mntmMxml);
		mntmMxml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MXML);
			}
		});
		mntmMakeFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MAKEFILE);
			}
		});

		JMenuItem mntmNsis = new JMenuItem(Messages.getString("MainFrame.69")); //$NON-NLS-1$
		mntmNsis.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NSIS);
			}
		});
		mnSyntaxHighlighting.add(mntmNsis);

		JMenu mnP = new JMenu(Messages.getString("MainFrame.70")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnP);

		JMenuItem mntmPerl = new JMenuItem(Messages.getString("MainFrame.71")); //$NON-NLS-1$
		mnP.add(mntmPerl);

		JMenuItem mntmPropertiesFile = new JMenuItem(Messages.getString("MainFrame.72")); //$NON-NLS-1$
		mntmPropertiesFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
			}
		});
		mnP.add(mntmPropertiesFile);

		JMenuItem mntmPython = new JMenuItem(Messages.getString("MainFrame.73")); //$NON-NLS-1$
		mntmPython.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
			}
		});
		mnP.add(mntmPython);
		mntmPerl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PERL);
			}
		});

		JMenu mnR = new JMenu(Messages.getString("MainFrame.74")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnR);

		JMenuItem mntmRuby = new JMenuItem(Messages.getString("MainFrame.75")); // Forever alone, Ruby. //$NON-NLS-1$
		mntmRuby.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
			}
		});
		mnR.add(mntmRuby);

		JMenu mnS = new JMenu(Messages.getString("MainFrame.76")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnS);

		JMenuItem mntmSas = new JMenuItem(Messages.getString("MainFrame.77")); //$NON-NLS-1$
		mntmSas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS);
			}
		});
		mnS.add(mntmSas);

		JMenuItem mntmSacala = new JMenuItem(Messages.getString("MainFrame.78")); //$NON-NLS-1$
		mntmSacala.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA);
			}
		});
		mnS.add(mntmSacala);

		JMenuItem mntmSql = new JMenuItem(Messages.getString("MainFrame.79")); //$NON-NLS-1$
		mntmSql.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			}
		});
		mnS.add(mntmSql);

		JMenu mnT = new JMenu(Messages.getString("MainFrame.80")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnT);

		JMenuItem mntmTcl = new JMenuItem(Messages.getString("MainFrame.81")); //$NON-NLS-1$
		mntmTcl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TCL);
			}
		});
		mnT.add(mntmTcl);

		JMenuItem mntmTypescript = new JMenuItem(Messages.getString("MainFrame.82")); //$NON-NLS-1$
		mntmTypescript.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
			}
		});
		mnT.add(mntmTypescript);

		JMenuItem mntmUnixShell = new JMenuItem(Messages.getString("MainFrame.83")); //$NON-NLS-1$
		mntmUnixShell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
			}
		});
		mnSyntaxHighlighting.add(mntmUnixShell);

		JMenuItem mntmVisualBasic = new JMenuItem(Messages.getString("MainFrame.84")); //$NON-NLS-1$
		mntmVisualBasic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC);
			}
		});
		mnSyntaxHighlighting.add(mntmVisualBasic);

		JMenuItem mntmWindowsBatch = new JMenuItem(Messages.getString("MainFrame.85")); //$NON-NLS-1$
		mntmWindowsBatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
			}
		});
		mnSyntaxHighlighting.add(mntmWindowsBatch);

		JMenuItem mntmXml = new JMenuItem(Messages.getString("MainFrame.86")); //$NON-NLS-1$
		mntmXml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			}
		});
		mnSyntaxHighlighting.add(mntmXml);

		JMenuItem mntmYaml = new JMenuItem(Messages.getString("MainFrame.87")); //$NON-NLS-1$
		mntmYaml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
			}
		});
		mnSyntaxHighlighting.add(mntmYaml);

		JMenu mnToolsPlugins = new JMenu(Messages.getString("MainFrame.88")); //$NON-NLS-1$
		menuBar.add(mnToolsPlugins);

		JMenuItem mntmAdd = new JMenuItem(Messages.getString("MainFrame.89")); //$NON-NLS-1$
		mntmAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.90")); //$NON-NLS-1$
				if (path == null)
					return;
				String cmdl = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.91")); //$NON-NLS-1$
				if (cmdl == null)
					return;
				String name = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.92")); //$NON-NLS-1$
				if (name == null)
					return;
				String hotkey = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.93")); //$NON-NLS-1$
				if (hotkey == null)
					return;
				System.out.println(toolAmount);
				tools[toolAmount] = new Tool();
				tools[toolAmount].commandline = cmdl;
				tools[toolAmount].name = name;
				tools[toolAmount].path = path;
				tools[toolAmount].hotkey = hotkey;
				ToolMenuItem tmpitem = new ToolMenuItem(tools[toolAmount].name);
				tmpitem.toolid = toolAmount;
				tmpitem.addActionListener(new ActionListener() {
					private String getFileExtension(File file) {
						String name = file.getName();
						try {
							return name.substring(name.lastIndexOf(Messages.getString("MainFrame.94")) + 1); //$NON-NLS-1$
						} catch (Exception e) {
							return Messages.getString("MainFrame.95"); //$NON-NLS-1$
						}
					}

					public void actionPerformed(ActionEvent e) {
						int toolid = tmpitem.toolid;
						try {
							String copy = tools[toolid].commandline;
							copy = copy.replaceAll(Messages.getString("MainFrame.96"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.97") : currentFile.getName()); //$NON-NLS-1$
							copy = copy.replaceAll(Messages.getString("MainFrame.98"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.99") //$NON-NLS-1$
											: currentFile.getParentFile().getAbsolutePath());
							copy = copy.replaceAll(Messages.getString("MainFrame.100"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.101") //$NON-NLS-1$
											: getFileExtension(currentFile));
							Process p = Runtime.getRuntime().exec(tools[toolid].path
									+ Messages.getString("MainFrame.102") + tools[toolid].commandline); //$NON-NLS-1$
							new Thread(new Runnable() {
								@Override
								public void run() {
									BufferedReader stdInput = new BufferedReader(
											new InputStreamReader(p.getInputStream()));

									BufferedReader stdError = new BufferedReader(
											new InputStreamReader(p.getErrorStream()));

									toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.103")); //$NON-NLS-1$
									String s = null;
									try {
										while ((s = stdInput.readLine()) != null) {
											toolConsole.setText(toolConsole.getText() + s);
										}
									} catch (IOException e1) {
										Crash dialog = new Crash(e1);
										dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
										dialog.setVisible(true);
										return;
									}

									// read any errors from the attempted command
									toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.104")); //$NON-NLS-1$
									try {
										while ((s = stdError.readLine()) != null) {
											toolConsole.setText(toolConsole.getText() + s);
										}
									} catch (IOException e) {
										Crash dialog = new Crash(e);
										dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
										dialog.setVisible(true);
										return;
									}
									return;
								}
							}).start();
						} catch (IOException e1) {
							Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						}
					}
				});
				tools[toolAmount].item = tmpitem;
				mnToolsPlugins.add(tmpitem);
				toolAmount++;
			}
		});
		mnToolsPlugins.add(mntmAdd);

		JMenuItem mntmRemove = new JMenuItem(Messages.getString("MainFrame.105")); //$NON-NLS-1$
		mntmRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int ans = Integer
							.parseInt(JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.106"))); //$NON-NLS-1$
					if (ans >= 0 && ans < 32 && ans < toolAmount) {
						if (tools[ans].item == null) {
							JOptionPane.showConfirmDialog(instance, Messages.getString("MainFrame.107")); //$NON-NLS-1$
							return;
						}
						mnToolsPlugins.remove(tools[ans].item);
						if (ans == 31) {
							tools[ans].commandline = Messages.getString("MainFrame.108"); //$NON-NLS-1$
							tools[ans].name = Messages.getString("MainFrame.109"); //$NON-NLS-1$
							tools[ans].path = Messages.getString("MainFrame.110"); //$NON-NLS-1$
							tools[ans].hotkey = Messages.getString("MainFrame.111"); //$NON-NLS-1$
							tools[ans].item = null;
						}
						for (int i = ans; i < 31; i++) {
							tools[ans] = tools[ans + 1];
						}
					} else {
						JOptionPane.showConfirmDialog(instance, Messages.getString("MainFrame.112")); //$NON-NLS-1$
						return;
					}
				} catch (Exception e1) {
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}

			}
		});
		mnToolsPlugins.add(mntmRemove);

		JMenuItem mntmSaveList = new JMenuItem(Messages.getString("MainFrame.113")); //$NON-NLS-1$
		mntmSaveList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (new File(Messages.getString("MainFrame.114")).exists()) //$NON-NLS-1$
						new File(Messages.getString("MainFrame.115")).delete(); //$NON-NLS-1$
					new File(Messages.getString("MainFrame.116")).createNewFile(); //$NON-NLS-1$
				} catch (Exception e1) {
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
				PrintWriter w = null;
				try {
					w = new PrintWriter(new File(Messages.getString("MainFrame.117"))); //$NON-NLS-1$
				} catch (FileNotFoundException e1) {
					// WTF?
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
				for (int i = 0; i < toolAmount; i++) {
					if (tools[i] == null) {
						w.close();
						return;
					}
					w.println(tools[i].path);
					w.println(tools[i].commandline);
					w.println(tools[i].name);
					w.println(tools[i].hotkey);
				}
				w.close();
			}
		});
		mnToolsPlugins.add(mntmSaveList);

		JSeparator separator_1 = new JSeparator();
		mnToolsPlugins.add(separator_1);

		JMenu mnScripts = new JMenu(Messages.getString("MainFrame.118")); //$NON-NLS-1$
		menuBar.add(mnScripts);

		JMenuItem mntmAdd_1 = new JMenuItem(Messages.getString("MainFrame.119")); //$NON-NLS-1$
		mntmAdd_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.120")); //$NON-NLS-1$
				if (path == null)
					return;
				String name = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.121")); //$NON-NLS-1$
				if (name == null)
					return;
				String hotkey = JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.122")); //$NON-NLS-1$
				if (hotkey == null)
					return;
				scripts[scriptAmount] = new Script();
				scripts[scriptAmount].name = name;
				scripts[scriptAmount].path = path;
				scripts[scriptAmount].hotkey = hotkey;
				ToolMenuItem tmpitem = new ToolMenuItem(scripts[scriptAmount].name);
				tmpitem.toolid = scriptAmount;
				tmpitem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int toolid = tmpitem.toolid;
						runScript(scripts[toolid]);
					}
				});
				scripts[scriptAmount].item = tmpitem;
				mnScripts.add(tmpitem);
				toolAmount++;
			}
		});
		mnScripts.add(mntmAdd_1);

		JMenuItem mntmRemove_1 = new JMenuItem(Messages.getString("MainFrame.123")); //$NON-NLS-1$
		mntmRemove_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int ans = Integer
							.parseInt(JOptionPane.showInputDialog(instance, Messages.getString("MainFrame.124"))); //$NON-NLS-1$
					if (ans >= 0 && ans < 32 && ans < toolAmount) {
						if (scripts[ans].item == null) {
							JOptionPane.showConfirmDialog(instance, Messages.getString("MainFrame.125")); //$NON-NLS-1$
							return;
						}
						mnToolsPlugins.remove(scripts[ans].item);
						if (ans == 31) {
							scripts[ans].name = Messages.getString("MainFrame.126"); //$NON-NLS-1$
							scripts[ans].path = Messages.getString("MainFrame.127"); //$NON-NLS-1$
							scripts[ans].hotkey = Messages.getString("MainFrame.128"); //$NON-NLS-1$
							scripts[ans].item = null;
						}
						for (int i = ans; i < 31; i++) {
							scripts[ans] = scripts[ans + 1];
						}
					} else {
						JOptionPane.showConfirmDialog(instance, Messages.getString("MainFrame.129")); //$NON-NLS-1$
						return;
					}
				} catch (Exception e1) {
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		mnScripts.add(mntmRemove_1);

		JMenuItem mntmSaveScripts = new JMenuItem(Messages.getString("MainFrame.130")); //$NON-NLS-1$
		mntmSaveScripts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (new File(Messages.getString("MainFrame.131")).exists()) //$NON-NLS-1$
						new File(Messages.getString("MainFrame.132")).delete(); //$NON-NLS-1$
					new File(Messages.getString("MainFrame.133")).createNewFile(); //$NON-NLS-1$
				} catch (Exception e1) {
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
				PrintWriter w = null;
				try {
					w = new PrintWriter(new File(Messages.getString("MainFrame.134"))); //$NON-NLS-1$
				} catch (FileNotFoundException e1) {
					// WTF?
					Crash dialog = new Crash(e1);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
				for (int i = 0; i < scriptAmount; i++) {
					if (scripts[i] == null) {
						w.close();
						return;
					}
					w.println(scripts[i].path);
					w.println(scripts[i].name);
					w.println(scripts[i].hotkey);
				}
				w.close();
			}
		});
		mnScripts.add(mntmSaveScripts);

		JSeparator separator_2 = new JSeparator();
		mnScripts.add(separator_2);

		JMenu mnAbout = new JMenu(Messages.getString("MainFrame.135")); //$NON-NLS-1$
		menuBar.add(mnAbout);

		JMenuItem mntmAbout = new JMenuItem(Messages.getString("MainFrame.136")); //$NON-NLS-1$
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

		JButton btnNewButton = new JButton(Messages.getString("MainFrame.137")); //$NON-NLS-1$
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
		btnNewButton.setToolTipText(Messages.getString("MainFrame.138")); //$NON-NLS-1$
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.139")))); //$NON-NLS-1$
		toolBar.add(btnNewButton);

		JButton btnOpenButton = new JButton(Messages.getString("MainFrame.140")); //$NON-NLS-1$
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
		btnOpenButton.setToolTipText(Messages.getString("MainFrame.141")); //$NON-NLS-1$
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.142")))); //$NON-NLS-1$
		toolBar.add(btnOpenButton);

		JButton btnSaveButton = new JButton(Messages.getString("MainFrame.143")); //$NON-NLS-1$
		btnSaveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentFile == null) {
					final JFileChooser SaveAs = new JFileChooser();
					SaveAs.setApproveButtonText(Messages.getString("MainFrame.144")); //$NON-NLS-1$
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
		btnSaveButton.setToolTipText(Messages.getString("MainFrame.145")); //$NON-NLS-1$
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.146")))); //$NON-NLS-1$
		toolBar.add(btnSaveButton);

		JButton btnCloseButton = new JButton(Messages.getString("MainFrame.147")); //$NON-NLS-1$
		btnCloseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (instances == 0)
					return;
				instance.dispose();
			}
		});
		btnCloseButton.setToolTipText(Messages.getString("MainFrame.148")); //$NON-NLS-1$
		btnCloseButton.setFocusPainted(false);
		btnCloseButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.149")))); //$NON-NLS-1$
		toolBar.add(btnCloseButton);

		JButton btnCutButton = new JButton(Messages.getString("MainFrame.150")); //$NON-NLS-1$
		btnCutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.cut();
			}
		});
		btnCutButton.setToolTipText(Messages.getString("MainFrame.151")); //$NON-NLS-1$
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.152")))); //$NON-NLS-1$
		toolBar.add(btnCutButton);

		JButton btnCopyButton = new JButton(Messages.getString("MainFrame.153")); //$NON-NLS-1$
		btnCopyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.copy();
			}
		});
		btnCopyButton.setToolTipText(Messages.getString("MainFrame.154")); //$NON-NLS-1$
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.155")))); //$NON-NLS-1$
		toolBar.add(btnCopyButton);

		JButton btnPasteButton = new JButton(Messages.getString("MainFrame.156")); //$NON-NLS-1$
		btnPasteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.paste();
			}
		});
		btnPasteButton.setToolTipText(Messages.getString("MainFrame.157")); //$NON-NLS-1$
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.158")))); //$NON-NLS-1$
		toolBar.add(btnPasteButton);

		JButton btnDeleteButton = new JButton(Messages.getString("MainFrame.159")); //$NON-NLS-1$
		btnDeleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.replaceSelection(Messages.getString("MainFrame.160")); //$NON-NLS-1$
			}
		});
		btnDeleteButton.setToolTipText(Messages.getString("MainFrame.161")); //$NON-NLS-1$
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.162")))); //$NON-NLS-1$
		toolBar.add(btnDeleteButton);

		JButton btnUndoButton = new JButton(Messages.getString("MainFrame.163")); //$NON-NLS-1$
		btnUndoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.undoLastAction();
			}
		});
		btnUndoButton.setToolTipText(Messages.getString("MainFrame.164")); //$NON-NLS-1$
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.165")))); //$NON-NLS-1$
		toolBar.add(btnUndoButton);

		JButton btnRedoButton = new JButton(Messages.getString("MainFrame.166")); //$NON-NLS-1$
		btnRedoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.redoLastAction();
			}
		});
		btnRedoButton.setToolTipText(Messages.getString("MainFrame.167")); //$NON-NLS-1$
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.168")))); //$NON-NLS-1$
		toolBar.add(btnRedoButton);

		RTextScrollPane scrollPane = new RTextScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		textPane.setFont(new Font(Messages.getString("MainFrame.169"), Font.PLAIN, 13)); //$NON-NLS-1$
		scrollPane.setViewportView(textPane);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel searchPanel = new JPanel();
		panel.add(searchPanel, BorderLayout.NORTH);

		JLabel lblSearch = new JLabel(Messages.getString("MainFrame.170")); //$NON-NLS-1$
		searchPanel.add(lblSearch);

		searchTextField = new JTextField();
		searchPanel.add(searchTextField);
		searchTextField.setColumns(10);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);

		JLabel lblReplace = new JLabel(Messages.getString("MainFrame.171")); //$NON-NLS-1$
		panel_2.add(lblReplace);

		replaceWithTextField = new JTextField();
		panel_2.add(replaceWithTextField);
		replaceWithTextField.setColumns(10);

		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel_3.add(panel_4, BorderLayout.NORTH);

		JButton btnSearch = new JButton(Messages.getString("MainFrame.172")); //$NON-NLS-1$
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
				final int l2 = searchTextField.getText().length();
				if (l1 == -1) {
					JOptionPane.showMessageDialog(instance, Messages.getString("MainFrame.173") //$NON-NLS-1$
							+ searchTextField.getText() + Messages.getString("MainFrame.174")); //$NON-NLS-1$
				} else {
					textPane.select(l1, l2 + l1);
				}
			}
		});
		panel_4.add(btnSearch);

		JButton btnReplace = new JButton(Messages.getString("MainFrame.175")); //$NON-NLS-1$
		btnReplace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
				final int l2 = searchTextField.getText().length();
				if (l1 == -1) {
					JOptionPane.showMessageDialog(instance, Messages.getString("MainFrame.176") //$NON-NLS-1$
							+ searchTextField.getText() + Messages.getString("MainFrame.177")); //$NON-NLS-1$
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

		JButton btnCountOccurences = new JButton(Messages.getString("MainFrame.178")); //$NON-NLS-1$
		btnCountOccurences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int amount = 0;
				while (true) {
					final int l1 = textPane.getText().indexOf(searchTextField.getText(), textPane.getCaretPosition());
					final int l2 = searchTextField.getText().length();
					if (l1 == -1) {
						break;
					} else {
						textPane.setCaretPosition(l1 + l2);
						amount++;
					}
				}
				JOptionPane.showMessageDialog(instance,
						Messages.getString("MainFrame.179") + amount + Messages.getString("MainFrame.180")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		panel_6.add(btnCountOccurences);

		JPanel panel_7 = new JPanel();
		panel_5.add(panel_7, BorderLayout.SOUTH);

		JButton btnBlack = new JButton(Messages.getString("MainFrame.181")); //$NON-NLS-1$
		btnBlack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme.load(getClass().getResourceAsStream(Messages.getString("MainFrame.182"))); //$NON-NLS-1$
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnBlack);

		JButton btnClassical = new JButton(Messages.getString("MainFrame.183")); //$NON-NLS-1$
		btnClassical.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme.load(getClass().getResourceAsStream(Messages.getString("MainFrame.184"))); //$NON-NLS-1$
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnClassical);

		JPanel panel_8 = new JPanel();
		panel_5.add(panel_8, BorderLayout.CENTER);
		panel_8.setLayout(new BorderLayout(0, 0));

		JPanel panel_9 = new JPanel();
		panel_8.add(panel_9, BorderLayout.SOUTH);

		JButton btnNewButton_1 = new JButton(Messages.getString("MainFrame.185")); //$NON-NLS-1$
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					Theme theme = Theme.load(getClass().getResourceAsStream(Messages.getString("MainFrame.186"))); //$NON-NLS-1$
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnNewButton_1);

		JButton btnMonokai = new JButton(Messages.getString("MainFrame.187")); //$NON-NLS-1$
		btnMonokai.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme.load(getClass().getResourceAsStream(Messages.getString("MainFrame.188"))); //$NON-NLS-1$
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnMonokai);

		JPanel panel_10 = new JPanel();
		panel_8.add(panel_10, BorderLayout.CENTER);
		panel_10.setLayout(new BorderLayout(0, 0));

		JPanel panel_11 = new JPanel();
		panel_10.add(panel_11, BorderLayout.SOUTH);

		JLabel lblTheme = new JLabel(Messages.getString("MainFrame.189")); //$NON-NLS-1$
		panel_11.add(lblTheme);

		JPanel panel_12 = new JPanel();
		panel_10.add(panel_12, BorderLayout.CENTER);
		panel_12.setLayout(new BorderLayout(0, 0));

		JLabel lblToolConsole = new JLabel(Messages.getString("MainFrame.190")); //$NON-NLS-1$
		panel_12.add(lblToolConsole, BorderLayout.NORTH);

		JScrollPane scrollPane_1 = new JScrollPane();
		panel_12.add(scrollPane_1, BorderLayout.CENTER);

		toolConsole.setEditable(false);
		scrollPane_1.setViewportView(toolConsole);

		JPanel panel_13 = new JPanel();
		panel_12.add(panel_13, BorderLayout.SOUTH);

		JButton btnOpenInDialog = new JButton(Messages.getString("MainFrame.191")); //$NON-NLS-1$
		btnOpenInDialog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				CommandOutputDialog dialog = new CommandOutputDialog(toolConsole.getText());
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		panel_13.add(btnOpenInDialog);

		JButton btnClear = new JButton(Messages.getString("MainFrame.192")); //$NON-NLS-1$
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toolConsole.setText(Messages.getString("MainFrame.193")); //$NON-NLS-1$
			}
		});
		panel_13.add(btnClear);

		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				lblReady.setText(Messages.getString("MainFrame.194") + textPane.getText().length() //$NON-NLS-1$
						+ Messages.getString("MainFrame.195") //$NON-NLS-1$
						+ (currentFile == null ? Messages.getString("MainFrame.196") : currentFile.getAbsolutePath()) //$NON-NLS-1$
						+ Messages.getString("MainFrame.197") //$NON-NLS-1$
						+ (currentFile == null ? Messages.getString("MainFrame.198") //$NON-NLS-1$
								: currentFile.getFreeSpace() / 1024)
						+ Messages.getString("MainFrame.199") //$NON-NLS-1$
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true
								? Messages.getString("MainFrame.200") //$NON-NLS-1$
								: Messages.getString("MainFrame.201")) //$NON-NLS-1$
						+ Messages.getString("MainFrame.202") //$NON-NLS-1$
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) == true
								? Messages.getString("MainFrame.203") //$NON-NLS-1$
								: Messages.getString("MainFrame.204")) //$NON-NLS-1$
						+ Messages.getString("MainFrame.205") //$NON-NLS-1$
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) == true
								? Messages.getString("MainFrame.206") //$NON-NLS-1$
								: Messages.getString("MainFrame.207"))); //$NON-NLS-1$
				if (instances == 0)
					System.exit(0);
			}
		}, 0, 1);
		textPane.clearParsers();
		textPane.setParserDelay(1);
		textPane.setAnimateBracketMatching(true);
		textPane.setAutoIndentEnabled(true);
		textPane.setAntiAliasingEnabled(true);
		textPane.setBracketMatchingEnabled(true);
		textPane.setCloseCurlyBraces(true);
		textPane.setCloseMarkupTags(true);
		textPane.setCodeFoldingEnabled(true);
		textPane.setHyperlinkForeground(Color.pink);
		textPane.setHyperlinksEnabled(true);
		textPane.setPaintMatchedBracketPair(true);
		textPane.setPaintTabLines(true);
		try {
			Theme theme = Theme.load(getClass().getResourceAsStream(Messages.getString("MainFrame.208"))); //$NON-NLS-1$
			theme.apply(textPane);
		} catch (IOException ioe) { // Never happens
			Crash dialog = new Crash(ioe);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);

		JPanel panel_14 = new JPanel();
		contentPane.add(panel_14, BorderLayout.SOUTH);
		panel_14.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar_1 = new JToolBar();
		panel_14.add(toolBar_1);
		toolBar_1.setFloatable(false);

		toolBar_1.add(lblReady);

		if (new File(Messages.getString("MainFrame.209")).exists()) { //$NON-NLS-1$
			Scanner s = null;
			try {
				s = new Scanner(new File(Messages.getString("MainFrame.210"))); //$NON-NLS-1$
			} catch (FileNotFoundException e1) {
				// WTF?
				Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			int counter = 0;
			while (s.hasNextLine()) {
				tools[counter] = new Tool();
				tools[counter].path = s.nextLine();
				tools[counter].commandline = s.nextLine();
				tools[counter].name = s.nextLine();
				tools[counter].hotkey = s.nextLine();
				ToolMenuItem tmpitem = new ToolMenuItem(tools[counter].name);
				tmpitem.toolid = toolAmount;
				tmpitem.addActionListener(new ActionListener() {
					private String getFileExtension(File file) {
						String name = file.getName();
						try {
							return name.substring(name.lastIndexOf(Messages.getString("MainFrame.211")) + 1); //$NON-NLS-1$
						} catch (Exception e) {
							return Messages.getString("MainFrame.212"); //$NON-NLS-1$
						}
					}

					public void actionPerformed(ActionEvent e) {
						int toolid = tmpitem.toolid;
						try {
							String copy = tools[toolid].commandline;
							copy = copy.replaceAll(Messages.getString("MainFrame.213"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.214") : currentFile.getName()); //$NON-NLS-1$
							copy = copy.replaceAll(Messages.getString("MainFrame.215"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.216") //$NON-NLS-1$
											: currentFile.getParentFile().getAbsolutePath());
							copy = copy.replaceAll(Messages.getString("MainFrame.217"), //$NON-NLS-1$
									currentFile == null ? Messages.getString("MainFrame.218") //$NON-NLS-1$
											: getFileExtension(currentFile));
							Process p = Runtime.getRuntime().exec(tools[toolid].path
									+ Messages.getString("MainFrame.219") + tools[toolid].commandline); //$NON-NLS-1$
							new Thread(new Runnable() {
								@Override
								public void run() {
									BufferedReader stdInput = new BufferedReader(
											new InputStreamReader(p.getInputStream()));

									BufferedReader stdError = new BufferedReader(
											new InputStreamReader(p.getErrorStream()));

									toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.220")); //$NON-NLS-1$
									String s = null;
									try {
										while ((s = stdInput.readLine()) != null) {
											toolConsole.setText(toolConsole.getText() + s);
										}
									} catch (IOException e1) {
										Crash dialog = new Crash(e1);
										dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
										dialog.setVisible(true);
										return;
									}

									// read any errors from the attempted command
									toolConsole.setText(toolConsole.getText() + Messages.getString("MainFrame.221")); //$NON-NLS-1$
									try {
										while ((s = stdError.readLine()) != null) {
											toolConsole.setText(toolConsole.getText() + s);
										}
									} catch (IOException e) {
										Crash dialog = new Crash(e);
										dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
										dialog.setVisible(true);
										return;
									}
									return;
								}
							}).start();
						} catch (IOException e1) {
							Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						}
					}
				});
				tools[counter].item = tmpitem;
				mnToolsPlugins.add(tmpitem);
				counter++;
			}
			toolAmount = counter;
		}
		if (new File(Messages.getString("MainFrame.222")).exists()) { //$NON-NLS-1$
			Scanner s = null;
			try {
				s = new Scanner(new File(Messages.getString("MainFrame.223"))); //$NON-NLS-1$
			} catch (FileNotFoundException e1) {
				// WTF?
				Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			int counter = 0;
			while (s.hasNextLine()) {
				scripts[counter] = new Script();
				scripts[counter].path = s.nextLine();
				scripts[counter].name = s.nextLine();
				scripts[counter].hotkey = s.nextLine();
				ToolMenuItem tmpitem = new ToolMenuItem(scripts[counter].name);
				tmpitem.toolid = scriptAmount;
				tmpitem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int toolid = tmpitem.toolid;
						runScript(scripts[toolid]);
					}
				});
				tools[counter].item = tmpitem;
				mnToolsPlugins.add(tmpitem);
				counter++;
			}
			toolAmount = counter;
		}
	}

	public void runScript(Script script) {
		try {
			@SuppressWarnings("resource")
			String content = new Scanner(new File(Messages.getString("MainFrame.224"))) //$NON-NLS-1$
					.useDelimiter(Messages.getString("MainFrame.225")).next(); //$NON-NLS-1$
			ScriptEngineManager mgr = new ScriptEngineManager();
			ScriptEngine jsEngine = mgr.getEngineByName(Messages.getString("MainFrame.226")); //$NON-NLS-1$
			Invocable invocable = (Invocable) jsEngine;
			try {
				invocable.invokeFunction(Messages.getString("MainFrame.227"), this, script, instances, textPane, //$NON-NLS-1$
						currentFile, lblReady, searchTextField, replaceWithTextField, tools, scripts, scriptAmount,
						toolAmount, toolConsole);
			} catch (NoSuchMethodException | ScriptException e) {
				Crash dialog = new Crash(e);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		} catch (FileNotFoundException e) {
			Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
	}

}
