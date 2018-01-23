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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;
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
	JTextPane toolConsole;

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

		JMenuItem mntmNo = new JMenuItem("No");
		mntmNo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
			}
		});
		mnSyntaxHighlighting.add(mntmNo);

		JMenu mnA = new JMenu("A");
		mnSyntaxHighlighting.add(mnA);

		JMenuItem mntmActionscript = new JMenuItem("ActionScript");
		mnA.add(mntmActionscript);

		JMenuItem mntmAssembler = new JMenuItem("Assembly");
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

		JMenuItem mntmBbcode = new JMenuItem("BBCode");
		mntmBbcode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BBCODE);
			}
		});
		mnSyntaxHighlighting.add(mntmBbcode);

		JMenu mnC = new JMenu("C");
		mnSyntaxHighlighting.add(mnC);

		JMenuItem mntmC = new JMenuItem("C");
		mnC.add(mntmC);

		JMenuItem mntmC_1 = new JMenuItem("C++");
		mnC.add(mntmC_1);

		JMenuItem mntmC_2 = new JMenuItem("C#");
		mnC.add(mntmC_2);

		JMenuItem mntmClojure = new JMenuItem("Clojure");
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

		JMenu mnD = new JMenu("D");
		mnSyntaxHighlighting.add(mnD);

		JMenuItem mntmDart = new JMenuItem("Dart");
		mnD.add(mntmDart);

		JMenuItem mntmDelphi = new JMenuItem("Delphi");
		mnD.add(mntmDelphi);

		JMenuItem mntmDocker = new JMenuItem("Docker");
		mnD.add(mntmDocker);

		JMenuItem mntmDtd = new JMenuItem("DTD");
		mnD.add(mntmDtd);

		JMenuItem mntmD = new JMenuItem("D");
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

		JMenuItem mntmFortan = new JMenuItem("Fortan");
		mntmFortan.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_FORTRAN);
			}
		});
		mnSyntaxHighlighting.add(mntmFortan);

		JMenuItem mntmGroovy = new JMenuItem("Groovy");
		mntmGroovy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
			}
		});
		mnSyntaxHighlighting.add(mntmGroovy);

		JMenu mnH = new JMenu("H");
		mnSyntaxHighlighting.add(mnH);

		JMenuItem mntmHtaccess = new JMenuItem("HTAccess");
		mnH.add(mntmHtaccess);
		mntmHtaccess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTACCESS);
			}
		});

		JMenuItem mntmHosts = new JMenuItem("Hosts");
		mnH.add(mntmHosts);

		JMenuItem mntmHtml = new JMenuItem("HTML");
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

		JMenuItem mntmIni = new JMenuItem("INI");
		mntmIni.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_INI);
			}
		});
		mnSyntaxHighlighting.add(mntmIni);

		JMenu mnJ = new JMenu("J");
		mnSyntaxHighlighting.add(mnJ);

		JMenuItem mntmJavascript = new JMenuItem("JavaScript");
		mnJ.add(mntmJavascript);

		JMenuItem mntmJava = new JMenuItem("Java");
		mnJ.add(mntmJava);

		JMenuItem mntmJshintrc = new JMenuItem("JSON");
		mnJ.add(mntmJshintrc);

		JMenuItem mntmJsp = new JMenuItem("JSP");
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

		JMenu mnL = new JMenu("L");
		mnSyntaxHighlighting.add(mnL);

		JMenuItem mntmLatex = new JMenuItem("Latex");
		mnL.add(mntmLatex);

		JMenuItem mntmLess = new JMenuItem("Less");
		mnL.add(mntmLess);

		JMenuItem mntmLisp = new JMenuItem("Lisp");
		mnL.add(mntmLisp);

		JMenuItem mntmLua = new JMenuItem("Lua");
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

		JMenu mnM = new JMenu("M");
		mnSyntaxHighlighting.add(mnM);

		JMenuItem mntmMakeFile = new JMenuItem("MakeFile");
		mnM.add(mntmMakeFile);

		JMenuItem mntmMxml = new JMenuItem("MXML");
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

		JMenuItem mntmNsis = new JMenuItem("NSIS");
		mntmNsis.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NSIS);
			}
		});
		mnSyntaxHighlighting.add(mntmNsis);

		JMenu mnP = new JMenu("P");
		mnSyntaxHighlighting.add(mnP);

		JMenuItem mntmPerl = new JMenuItem("Perl");
		mnP.add(mntmPerl);

		JMenuItem mntmPropertiesFile = new JMenuItem("Properties File");
		mntmPropertiesFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
			}
		});
		mnP.add(mntmPropertiesFile);

		JMenuItem mntmPython = new JMenuItem("Python");
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

		JMenu mnR = new JMenu("R");
		mnSyntaxHighlighting.add(mnR);

		JMenuItem mntmRuby = new JMenuItem("Ruby"); // Forever alone, Ruby.
		mntmRuby.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
			}
		});
		mnR.add(mntmRuby);

		JMenu mnS = new JMenu("S");
		mnSyntaxHighlighting.add(mnS);

		JMenuItem mntmSas = new JMenuItem("SAS");
		mntmSas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS);
			}
		});
		mnS.add(mntmSas);

		JMenuItem mntmSacala = new JMenuItem("Scala");
		mntmSacala.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA);
			}
		});
		mnS.add(mntmSacala);

		JMenuItem mntmSql = new JMenuItem("SQL");
		mntmSql.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
			}
		});
		mnS.add(mntmSql);

		JMenu mnT = new JMenu("T");
		mnSyntaxHighlighting.add(mnT);

		JMenuItem mntmTcl = new JMenuItem("TCL");
		mntmTcl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TCL);
			}
		});
		mnT.add(mntmTcl);

		JMenuItem mntmTypescript = new JMenuItem("TypeScript");
		mntmTypescript.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
			}
		});
		mnT.add(mntmTypescript);

		JMenuItem mntmUnixShell = new JMenuItem("Unix Shell");
		mntmUnixShell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
			}
		});
		mnSyntaxHighlighting.add(mntmUnixShell);

		JMenuItem mntmVisualBasic = new JMenuItem("Visual Basic");
		mntmVisualBasic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC);
			}
		});
		mnSyntaxHighlighting.add(mntmVisualBasic);

		JMenuItem mntmWindowsBatch = new JMenuItem("Windows Batch");
		mntmWindowsBatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
			}
		});
		mnSyntaxHighlighting.add(mntmWindowsBatch);

		JMenuItem mntmXml = new JMenuItem("XML");
		mntmXml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			}
		});
		mnSyntaxHighlighting.add(mntmXml);

		JMenuItem mntmYaml = new JMenuItem("YAML");
		mntmYaml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
			}
		});
		mnSyntaxHighlighting.add(mntmYaml);

		JMenu mnManageCompilers = new JMenu("Compilers");
		menuBar.add(mnManageCompilers);

		JMenu mnAssembly = new JMenu("Assembly");
		mnManageCompilers.add(mnAssembly);

		JMenuItem mntmNasm = new JMenuItem("NASM");
		mntmNasm.addActionListener(new ActionListener() {
			private static final int BUFFER_SIZE = 4096;

			public void unzip(String zipFilePath, String destDirectory) throws IOException {
				File destDir = new File(destDirectory);
				if (!destDir.exists()) {
					destDir.mkdir();
				}
				ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
				ZipEntry entry = zipIn.getNextEntry();
				// iterates over entries in the zip file
				while (entry != null) {
					String filePath = destDirectory + File.separator + entry.getName();
					if (!entry.isDirectory()) {
						// if the entry is a file, extracts it
						extractFile(zipIn, filePath);
					} else {
						// if the entry is a directory, make the directory
						File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
			}

			private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				byte[] bytesIn = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = zipIn.read(bytesIn)) != -1) {
					bos.write(bytesIn, 0, read);
				}
				bos.close();
			}

			@SuppressWarnings("unchecked")
			public <T> T[] concatAll(T[] first, T[]... rest) {
				int totalLength = first.length;
				for (T[] array : rest) {
					totalLength += array.length;
				}
				T[] result = Arrays.copyOf(first, totalLength);
				int offset = first.length;
				for (T[] array : rest) {
					System.arraycopy(array, 0, result, offset, array.length);
					offset += array.length;
				}
				return result;
			}

			public void actionPerformed(ActionEvent arg0) {
				String name = "nasm", compilername = "nasm";
				if (!new File("compilers\\" + name + "\\installed.dat").exists()) {
					JOptionPane.showMessageDialog(instance, "Please install " + name + " package.", "Error",
							JOptionPane.ERROR_MESSAGE);
					if (JOptionPane.showConfirmDialog(instance,
							"Do you want to download and install " + name
									+ " package?\n (By installing it you accept License provided with software)",
							"Package manager", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						URL url = null;
						try {
							url = new URL("https://raw.githubusercontent.com/KrzysztofSzewczyk/MEdit/master/packages/"
									+ name + ".zip");
						} catch (MalformedURLException e) {
							Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
						try {
							HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
							new File("packages//" + name).mkdirs();
							try (InputStream stream = con.getInputStream()) {
								Files.copy(stream, Paths.get("packages//" + name + "//package.zip"));
							}
							unzip("packages//" + name + "//package.zip", "packages//" + name);
							new File("packages//" + name + "//package.zip").delete();
						} catch (IOException e) {
							Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
					} else
						return;
				}
				if (currentFile == null) {
					JOptionPane.showMessageDialog(instance, "Please save your work in order to compile.",
							"Eggs are supposed to be green!", JOptionPane.ERROR_MESSAGE);
				} else {
					String osString = OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS ? "macos"
							: OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows ? "windows" : "linux";
					FileArrayProvider fap = new FileArrayProvider(); // FAP, huh... TODO: change name
					String[] lines = null;
					try {
						lines = fap.readLines("compilers\\" + name + "\\" + osString + "\\options.txt");
					} catch (IOException e2) {
						Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
						return;
					}
					String[] command = concatAll(new String[] { "compilers\\" + name + "\\" + osString + "\\" + compilername }, lines, new String[] { "\"" + currentFile.getAbsolutePath() + "\"" });
					System.out.println(command[0] + " " + command[1] + " " + command[2] + " " + command[3] + " ");
					ProcessBuilder pb = new ProcessBuilder(command);
					try {
						pb.directory(new File(currentFile.getAbsoluteFile().getParent()));
					} catch (Exception e1) {
						// I Don't care
					}
					try {
						Process p = pb.start();
						new Thread(new Runnable() {
							@Override
							public void run() {
								BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

								BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

								toolConsole.setText(toolConsole.getText() + "STDOUT:\n");
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
								toolConsole.setText(toolConsole.getText() + "\nSTDERR:\n");
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

								CommandOutputDialog dialog = new CommandOutputDialog(toolConsole.getText());
								dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);

								toolConsole.setText("");

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
		mnAssembly.add(mntmNasm);

		JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);

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
					currentFile = chooser.getSelectedFile();
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

		RTextScrollPane scrollPane = new RTextScrollPane();
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
				JOptionPane.showMessageDialog(instance, "Found " + amount + " occurences.");
			}
		});
		panel_6.add(btnCountOccurences);

		JPanel panel_7 = new JPanel();
		panel_5.add(panel_7, BorderLayout.SOUTH);

		JButton btnBlack = new JButton("Dark");
		btnBlack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme
							.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnBlack);

		JButton btnClassical = new JButton("Default");
		btnClassical.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme
							.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
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

		JButton btnNewButton_1 = new JButton("Extra Default");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					Theme theme = Theme.load(
							getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default-alt.xml"));
					theme.apply(textPane);
				} catch (IOException ioe) { // Never happens
					Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnNewButton_1);

		JButton btnMonokai = new JButton("Monokai");
		btnMonokai.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Theme theme = Theme
							.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
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

		JLabel lblTheme = new JLabel("Theme:");
		panel_11.add(lblTheme);

		JPanel panel_12 = new JPanel();
		panel_10.add(panel_12, BorderLayout.CENTER);
		panel_12.setLayout(new BorderLayout(0, 0));

		toolConsole = new JTextPane();
		panel_12.add(toolConsole, BorderLayout.CENTER);
		toolConsole.setVisible(false);

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
			Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
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
	}

}
