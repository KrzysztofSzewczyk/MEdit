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
import javax.swing.WindowConstants;
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

	private static int instances = 1;
	/**
	 * Serial version UID required by Eclipse
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private File currentFile = null;
	private MainFrame instance;
	private final JLabel lblReady = new JLabel(Messages.getString("MainFrame.0")); //$NON-NLS-1$
	private JTextField replaceWithTextField;
	private final int scriptAmount = 0;
	private final Script[] scripts = new Script[32];
	private JTextField searchTextField;
	private final RSyntaxTextArea textPane = new RSyntaxTextArea();
	private int toolAmount = 0;
	private final JTextPane toolConsole = new JTextPane();
	private final Tool[] tools = new Tool[32];

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		this.instance = this;
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent arg0) {
				if (MainFrame.instances == 0)
					System.exit(0);
				else
					MainFrame.instances--;
			}

			@Override
			public void windowClosing(final WindowEvent arg0) {
				if (MainFrame.instances == 0)
					System.exit(0);
				else
					MainFrame.instances--;
			}
		});
		this.addKeyListener(new KeyAdapter() {
			private String getFileExtension(final File file) {
				final String name = file.getName();
				try {
					return name.substring(name.lastIndexOf(Messages.getString("MainFrame.1")) + 1); //$NON-NLS-1$
				} catch (final Exception e) {
					return Messages.getString("MainFrame.2"); //$NON-NLS-1$
				}
			}

			@Override
			public void keyTyped(final KeyEvent arg0) {
				final char c = arg0.getKeyChar();
				if (!arg0.isAltDown())
					return;
				int found = -1;
				for (int i = 0; i < MainFrame.this.tools.length; i++)
					if (MainFrame.this.tools[i].hotkey.charAt(0) == c)
						found = i;
				if (found == -1) {
					// Keep on dispatching, dear Java.
				} else {
					final int toolid = found;
					try {
						String copy = MainFrame.this.tools[toolid].commandline;
						copy = copy.replaceAll(Messages.getString("MainFrame.3"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.4") //$NON-NLS-1$
										: MainFrame.this.currentFile.getName());
						copy = copy.replaceAll(Messages.getString("MainFrame.5"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.6") //$NON-NLS-1$
										: MainFrame.this.currentFile.getParentFile().getAbsolutePath());
						copy = copy.replaceAll(Messages.getString("MainFrame.7"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.8") //$NON-NLS-1$
										: this.getFileExtension(MainFrame.this.currentFile));
						final Process p = Runtime.getRuntime().exec(MainFrame.this.tools[toolid].path
								+ Messages.getString("MainFrame.9") + MainFrame.this.tools[toolid].commandline); //$NON-NLS-1$
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole
									.setText(MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.10")); //$NON-NLS-1$
							String s = null;
							try {
								while ((s = stdInput.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e1) {
								final Crash dialog1 = new Crash(e1);
								dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog1.setVisible(true);
								return;
							}

							// read any errors from the attempted command
							MainFrame.this.toolConsole
									.setText(MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.11")); //$NON-NLS-1$
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e) {
								final Crash dialog2 = new Crash(e);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}
							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}
		});
		this.setIconImage(
				Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource(Messages.getString("MainFrame.12")))); //$NON-NLS-1$
		this.setTitle(Messages.getString("MainFrame.13")); //$NON-NLS-1$
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBounds(100, 100, 700, 500);
		this.setMinimumSize(new Dimension(700, 500));

		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu(Messages.getString("MainFrame.14")); //$NON-NLS-1$
		menuBar.add(mnFile);

		final JMenuItem mntmNew = new JMenuItem(Messages.getString("MainFrame.15")); //$NON-NLS-1$
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mntmNew.addActionListener(arg0 -> EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				final MainFrame frame = new MainFrame();
				frame.setVisible(true);
				MainFrame.instances++;
				MainFrame.this.textPane.requestFocus();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}));
		mnFile.add(mntmNew);

		final JMenuItem mntmOpen = new JMenuItem(Messages.getString("MainFrame.16")); //$NON-NLS-1$
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.addActionListener(arg0 -> {
			final JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(MainFrame.this.instance) != JFileChooser.APPROVE_OPTION)
				return;
			try {
				final FileReader reader = new FileReader(chooser.getSelectedFile());
				final BufferedReader br = new BufferedReader(reader);
				MainFrame.this.textPane.read(br, null);
				br.close();
				MainFrame.this.textPane.requestFocus();
				MainFrame.this.currentFile = chooser.getSelectedFile();
			} catch (final Exception e2) {
				final Crash dialog = new Crash(e2);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		mnFile.add(mntmOpen);

		final JMenuItem mntmSave = new JMenuItem(Messages.getString("MainFrame.17")); //$NON-NLS-1$
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(e -> {
			if (MainFrame.this.currentFile == null) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText(Messages.getString("MainFrame.18")); //$NON-NLS-1$
				final int actionDialog = SaveAs.showSaveDialog(MainFrame.this.instance);
				if (actionDialog != JFileChooser.APPROVE_OPTION)
					return;

				final File fileName1 = SaveAs.getSelectedFile();
				BufferedWriter outFile1 = null;
				try {
					outFile1 = new BufferedWriter(new FileWriter(fileName1));
					MainFrame.this.textPane.write(outFile1);
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
				MainFrame.this.currentFile = fileName1;
				MainFrame.this.textPane.requestFocus();
			} else {
				final File fileName2 = MainFrame.this.currentFile;
				BufferedWriter outFile2 = null;
				try {
					outFile2 = new BufferedWriter(new FileWriter(fileName2));
					MainFrame.this.textPane.write(outFile2);
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
				MainFrame.this.textPane.requestFocus();
			}
		});
		mnFile.add(mntmSave);

		final JMenuItem mntmSaveAs = new JMenuItem(Messages.getString("MainFrame.19")); //$NON-NLS-1$
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			final JFileChooser SaveAs = new JFileChooser();
			SaveAs.setApproveButtonText(Messages.getString("MainFrame.20")); //$NON-NLS-1$
			final int actionDialog = SaveAs.showSaveDialog(MainFrame.this.instance);
			if (actionDialog != JFileChooser.APPROVE_OPTION)
				return;

			final File fileName = SaveAs.getSelectedFile();
			BufferedWriter outFile = null;
			try {
				outFile = new BufferedWriter(new FileWriter(fileName));
				MainFrame.this.textPane.write(outFile);
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
			MainFrame.this.currentFile = fileName;
			MainFrame.this.textPane.requestFocus();
		});
		mnFile.add(mntmSaveAs);

		final JSeparator separator = new JSeparator();
		mnFile.add(separator);

		final JMenuItem mntmExit = new JMenuItem(Messages.getString("MainFrame.21")); //$NON-NLS-1$
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			MainFrame.this.instance.dispose();
		});
		mnFile.add(mntmExit);

		final JMenu mnEdit = new JMenu(Messages.getString("MainFrame.22")); //$NON-NLS-1$
		menuBar.add(mnEdit);

		final JMenuItem mntmCut = new JMenuItem(Messages.getString("MainFrame.23")); //$NON-NLS-1$
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(e -> MainFrame.this.textPane.cut());
		mnEdit.add(mntmCut);

		final JMenuItem mntmCopy = new JMenuItem(Messages.getString("MainFrame.24")); //$NON-NLS-1$
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> MainFrame.this.textPane.copy());
		mnEdit.add(mntmCopy);

		final JMenuItem mntmPaste = new JMenuItem(Messages.getString("MainFrame.25")); //$NON-NLS-1$
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> MainFrame.this.textPane.paste());
		mnEdit.add(mntmPaste);

		final JMenuItem mntmDelete = new JMenuItem(Messages.getString("MainFrame.26")); //$NON-NLS-1$
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(e -> MainFrame.this.textPane.replaceSelection(Messages.getString("MainFrame.27")));
		mnEdit.add(mntmDelete);

		final JSeparator separator_4 = new JSeparator();
		mnEdit.add(separator_4);

		final JMenuItem mntmUndo = new JMenuItem(Messages.getString("MainFrame.28")); //$NON-NLS-1$
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> MainFrame.this.textPane.undoLastAction());
		mnEdit.add(mntmUndo);

		final JMenuItem mntmRedo = new JMenuItem(Messages.getString("MainFrame.29")); //$NON-NLS-1$
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(e -> MainFrame.this.textPane.redoLastAction());
		mnEdit.add(mntmRedo);

		final JMenu mnLanguage = new JMenu(Messages.getString("MainFrame.30")); //$NON-NLS-1$
		menuBar.add(mnLanguage);

		final JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem(Messages.getString("MainFrame.31")); //$NON-NLS-1$
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);

		final JMenu mnSyntaxHighlighting = new JMenu(Messages.getString("MainFrame.32")); //$NON-NLS-1$
		menuBar.add(mnSyntaxHighlighting);

		final JMenuItem mntmNo = new JMenuItem(Messages.getString("MainFrame.33")); //$NON-NLS-1$
		mntmNo.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE));
		mnSyntaxHighlighting.add(mntmNo);

		final JMenu mnA = new JMenu(Messages.getString("MainFrame.34")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnA);

		final JMenuItem mntmActionscript = new JMenuItem(Messages.getString("MainFrame.35")); //$NON-NLS-1$
		mnA.add(mntmActionscript);

		final JMenuItem mntmAssembler = new JMenuItem(Messages.getString("MainFrame.36")); //$NON-NLS-1$
		mnA.add(mntmAssembler);
		mntmAssembler.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86));
		mntmActionscript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT));

		final JMenuItem mntmBbcode = new JMenuItem(Messages.getString("MainFrame.37")); //$NON-NLS-1$
		mntmBbcode.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BBCODE));
		mnSyntaxHighlighting.add(mntmBbcode);

		final JMenu mnC = new JMenu(Messages.getString("MainFrame.38")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnC);

		final JMenuItem mntmC = new JMenuItem(Messages.getString("MainFrame.39")); //$NON-NLS-1$
		mnC.add(mntmC);

		final JMenuItem mntmC_1 = new JMenuItem(Messages.getString("MainFrame.40")); //$NON-NLS-1$
		mnC.add(mntmC_1);

		final JMenuItem mntmC_2 = new JMenuItem(Messages.getString("MainFrame.41")); //$NON-NLS-1$
		mnC.add(mntmC_2);

		final JMenuItem mntmClojure = new JMenuItem(Messages.getString("MainFrame.42")); //$NON-NLS-1$
		mnC.add(mntmClojure);
		mntmClojure.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CLOJURE));
		mntmC_2.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP));
		mntmC_1.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
		mntmC.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C));

		final JMenu mnD = new JMenu(Messages.getString("MainFrame.43")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnD);

		final JMenuItem mntmDart = new JMenuItem(Messages.getString("MainFrame.44")); //$NON-NLS-1$
		mnD.add(mntmDart);

		final JMenuItem mntmDelphi = new JMenuItem(Messages.getString("MainFrame.45")); //$NON-NLS-1$
		mnD.add(mntmDelphi);

		final JMenuItem mntmDocker = new JMenuItem(Messages.getString("MainFrame.46")); //$NON-NLS-1$
		mnD.add(mntmDocker);

		final JMenuItem mntmDtd = new JMenuItem(Messages.getString("MainFrame.47")); //$NON-NLS-1$
		mnD.add(mntmDtd);

		final JMenuItem mntmD = new JMenuItem(Messages.getString("MainFrame.48")); //$NON-NLS-1$
		mnD.add(mntmD);
		mntmD.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_D));
		mntmDtd.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DTD));
		mntmDocker.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE));
		mntmDelphi.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DELPHI));
		mntmDart.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DART));

		final JMenuItem mntmFortan = new JMenuItem(Messages.getString("MainFrame.49")); //$NON-NLS-1$
		mntmFortan.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_FORTRAN));
		mnSyntaxHighlighting.add(mntmFortan);

		final JMenuItem mntmGroovy = new JMenuItem(Messages.getString("MainFrame.50")); //$NON-NLS-1$
		mntmGroovy.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY));
		mnSyntaxHighlighting.add(mntmGroovy);

		final JMenu mnH = new JMenu(Messages.getString("MainFrame.51")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnH);

		final JMenuItem mntmHtaccess = new JMenuItem(Messages.getString("MainFrame.52")); //$NON-NLS-1$
		mnH.add(mntmHtaccess);
		mntmHtaccess.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTACCESS));

		final JMenuItem mntmHosts = new JMenuItem(Messages.getString("MainFrame.53")); //$NON-NLS-1$
		mnH.add(mntmHosts);

		final JMenuItem mntmHtml = new JMenuItem(Messages.getString("MainFrame.54")); //$NON-NLS-1$
		mnH.add(mntmHtml);
		mntmHtml.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML));
		mntmHosts.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HOSTS));

		final JMenuItem mntmIni = new JMenuItem(Messages.getString("MainFrame.55")); //$NON-NLS-1$
		mntmIni.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_INI));
		mnSyntaxHighlighting.add(mntmIni);

		final JMenu mnJ = new JMenu(Messages.getString("MainFrame.56")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnJ);

		final JMenuItem mntmJavascript = new JMenuItem(Messages.getString("MainFrame.57")); //$NON-NLS-1$
		mnJ.add(mntmJavascript);

		final JMenuItem mntmJava = new JMenuItem(Messages.getString("MainFrame.58")); //$NON-NLS-1$
		mnJ.add(mntmJava);

		final JMenuItem mntmJshintrc = new JMenuItem(Messages.getString("MainFrame.59")); //$NON-NLS-1$
		mnJ.add(mntmJshintrc);

		final JMenuItem mntmJsp = new JMenuItem(Messages.getString("MainFrame.60")); //$NON-NLS-1$
		mnJ.add(mntmJsp);
		mntmJsp.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSP));
		mntmJshintrc.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON));
		mntmJava.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA));
		mntmJavascript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT));

		final JMenu mnL = new JMenu(Messages.getString("MainFrame.61")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnL);

		final JMenuItem mntmLatex = new JMenuItem(Messages.getString("MainFrame.62")); //$NON-NLS-1$
		mnL.add(mntmLatex);

		final JMenuItem mntmLess = new JMenuItem(Messages.getString("MainFrame.63")); //$NON-NLS-1$
		mnL.add(mntmLess);

		final JMenuItem mntmLisp = new JMenuItem(Messages.getString("MainFrame.64")); //$NON-NLS-1$
		mnL.add(mntmLisp);

		final JMenuItem mntmLua = new JMenuItem(Messages.getString("MainFrame.65")); //$NON-NLS-1$
		mnL.add(mntmLua);
		mntmLua.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA));
		mntmLisp.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LISP));
		mntmLess.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LESS));
		mntmLatex.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX));

		final JMenu mnM = new JMenu(Messages.getString("MainFrame.66")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnM);

		final JMenuItem mntmMakeFile = new JMenuItem(Messages.getString("MainFrame.67")); //$NON-NLS-1$
		mnM.add(mntmMakeFile);

		final JMenuItem mntmMxml = new JMenuItem(Messages.getString("MainFrame.68")); //$NON-NLS-1$
		mnM.add(mntmMxml);
		mntmMxml.addActionListener(
				arg0 -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MXML));
		mntmMakeFile.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MAKEFILE));

		final JMenuItem mntmNsis = new JMenuItem(Messages.getString("MainFrame.69")); //$NON-NLS-1$
		mntmNsis.addActionListener(
				arg0 -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NSIS));
		mnSyntaxHighlighting.add(mntmNsis);

		final JMenu mnP = new JMenu(Messages.getString("MainFrame.70")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnP);

		final JMenuItem mntmPerl = new JMenuItem(Messages.getString("MainFrame.71")); //$NON-NLS-1$
		mnP.add(mntmPerl);

		final JMenuItem mntmPropertiesFile = new JMenuItem(Messages.getString("MainFrame.72")); //$NON-NLS-1$
		mntmPropertiesFile.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE));
		mnP.add(mntmPropertiesFile);

		final JMenuItem mntmPython = new JMenuItem(Messages.getString("MainFrame.73")); //$NON-NLS-1$
		mntmPython.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON));
		mnP.add(mntmPython);
		mntmPerl.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PERL));

		final JMenu mnR = new JMenu(Messages.getString("MainFrame.74")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnR);

		final JMenuItem mntmRuby = new JMenuItem(Messages.getString("MainFrame.75")); // Forever alone, //$NON-NLS-1$
																						// Ruby.
		mntmRuby.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY));
		mnR.add(mntmRuby);

		final JMenu mnS = new JMenu(Messages.getString("MainFrame.76")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnS);

		final JMenuItem mntmSas = new JMenuItem(Messages.getString("MainFrame.77")); //$NON-NLS-1$
		mntmSas.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS));
		mnS.add(mntmSas);

		final JMenuItem mntmSacala = new JMenuItem(Messages.getString("MainFrame.78")); //$NON-NLS-1$
		mntmSacala.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA));
		mnS.add(mntmSacala);

		final JMenuItem mntmSql = new JMenuItem(Messages.getString("MainFrame.79")); //$NON-NLS-1$
		mntmSql.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL));
		mnS.add(mntmSql);

		final JMenu mnT = new JMenu(Messages.getString("MainFrame.80")); //$NON-NLS-1$
		mnSyntaxHighlighting.add(mnT);

		final JMenuItem mntmTcl = new JMenuItem(Messages.getString("MainFrame.81")); //$NON-NLS-1$
		mntmTcl.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TCL));
		mnT.add(mntmTcl);

		final JMenuItem mntmTypescript = new JMenuItem(Messages.getString("MainFrame.82")); //$NON-NLS-1$
		mntmTypescript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT));
		mnT.add(mntmTypescript);

		final JMenuItem mntmUnixShell = new JMenuItem(Messages.getString("MainFrame.83")); //$NON-NLS-1$
		mntmUnixShell.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL));
		mnSyntaxHighlighting.add(mntmUnixShell);

		final JMenuItem mntmVisualBasic = new JMenuItem(Messages.getString("MainFrame.84")); //$NON-NLS-1$
		mntmVisualBasic.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC));
		mnSyntaxHighlighting.add(mntmVisualBasic);

		final JMenuItem mntmWindowsBatch = new JMenuItem(Messages.getString("MainFrame.85")); //$NON-NLS-1$
		mntmWindowsBatch.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH));
		mnSyntaxHighlighting.add(mntmWindowsBatch);

		final JMenuItem mntmXml = new JMenuItem(Messages.getString("MainFrame.86")); //$NON-NLS-1$
		mntmXml.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML));
		mnSyntaxHighlighting.add(mntmXml);

		final JMenuItem mntmYaml = new JMenuItem(Messages.getString("MainFrame.87")); //$NON-NLS-1$
		mntmYaml.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML));
		mnSyntaxHighlighting.add(mntmYaml);

		final JMenu mnToolsPlugins = new JMenu(Messages.getString("MainFrame.88")); //$NON-NLS-1$
		menuBar.add(mnToolsPlugins);

		final JMenuItem mntmAdd = new JMenuItem(Messages.getString("MainFrame.89")); //$NON-NLS-1$
		mntmAdd.addActionListener(e -> {
			final String path = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.90")); //$NON-NLS-1$
			if (path == null)
				return;
			final String cmdl = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.91")); //$NON-NLS-1$
			if (cmdl == null)
				return;
			final String name = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.92")); //$NON-NLS-1$
			if (name == null)
				return;
			final String hotkey = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.93")); //$NON-NLS-1$
			if (hotkey == null)
				return;
			System.out.println(MainFrame.this.toolAmount);
			MainFrame.this.tools[MainFrame.this.toolAmount] = new Tool();
			MainFrame.this.tools[MainFrame.this.toolAmount].commandline = cmdl;
			MainFrame.this.tools[MainFrame.this.toolAmount].name = name;
			MainFrame.this.tools[MainFrame.this.toolAmount].path = path;
			MainFrame.this.tools[MainFrame.this.toolAmount].hotkey = hotkey;
			final ToolMenuItem tmpitem = new ToolMenuItem(MainFrame.this.tools[MainFrame.this.toolAmount].name);
			tmpitem.toolid = MainFrame.this.toolAmount;
			tmpitem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final int toolid = tmpitem.toolid;
					try {
						String copy = MainFrame.this.tools[toolid].commandline;
						copy = copy.replaceAll(Messages.getString("MainFrame.96"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.97") //$NON-NLS-1$
										: MainFrame.this.currentFile.getName());
						copy = copy.replaceAll(Messages.getString("MainFrame.98"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.99") //$NON-NLS-1$
										: MainFrame.this.currentFile.getParentFile().getAbsolutePath());
						copy = copy.replaceAll(Messages.getString("MainFrame.100"), //$NON-NLS-1$
								MainFrame.this.currentFile == null ? Messages.getString("MainFrame.101") //$NON-NLS-1$
										: this.getFileExtension(MainFrame.this.currentFile));
						final Process p = Runtime.getRuntime().exec(MainFrame.this.tools[toolid].path
								+ Messages.getString("MainFrame.102") + MainFrame.this.tools[toolid].commandline); //$NON-NLS-1$
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole.setText(
									MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.103")); //$NON-NLS-1$
							String s = null;
							try {
								while ((s = stdInput.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e1) {
								final Crash dialog1 = new Crash(e1);
								dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog1.setVisible(true);
								return;
							}

							// read any errors from the attempted command
							MainFrame.this.toolConsole.setText(
									MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.104")); //$NON-NLS-1$
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e2) {
								final Crash dialog2 = new Crash(e2);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}
							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}

				private String getFileExtension(final File file) {
					final String name = file.getName();
					try {
						return name.substring(name.lastIndexOf(Messages.getString("MainFrame.94")) + 1); //$NON-NLS-1$
					} catch (final Exception e) {
						return Messages.getString("MainFrame.95"); //$NON-NLS-1$
					}
				}
			});
			MainFrame.this.tools[MainFrame.this.toolAmount].item = tmpitem;
			mnToolsPlugins.add(tmpitem);
			MainFrame.this.toolAmount++;
		});
		mnToolsPlugins.add(mntmAdd);

		final JMenuItem mntmRemove = new JMenuItem(Messages.getString("MainFrame.105")); //$NON-NLS-1$
		mntmRemove.addActionListener(e -> {
			try {
				final int ans = Integer.parseInt(
						JOptionPane.showInputDialog(MainFrame.this.instance, Messages.getString("MainFrame.106"))); //$NON-NLS-1$
				if (ans >= 0 && ans < 32 && ans < MainFrame.this.toolAmount) {
					if (MainFrame.this.tools[ans].item == null) {
						JOptionPane.showConfirmDialog(MainFrame.this.instance, Messages.getString("MainFrame.107")); //$NON-NLS-1$
						return;
					}
					mnToolsPlugins.remove(MainFrame.this.tools[ans].item);
					if (ans == 31) {
						MainFrame.this.tools[ans].commandline = Messages.getString("MainFrame.108"); //$NON-NLS-1$
						MainFrame.this.tools[ans].name = Messages.getString("MainFrame.109"); //$NON-NLS-1$
						MainFrame.this.tools[ans].path = Messages.getString("MainFrame.110"); //$NON-NLS-1$
						MainFrame.this.tools[ans].hotkey = Messages.getString("MainFrame.111"); //$NON-NLS-1$
						MainFrame.this.tools[ans].item = null;
					}
					for (int i = ans; i < 31; i++)
						MainFrame.this.tools[ans] = MainFrame.this.tools[ans + 1];
				} else {
					JOptionPane.showConfirmDialog(MainFrame.this.instance, Messages.getString("MainFrame.112")); //$NON-NLS-1$
					return;
				}
			} catch (final Exception e1) {
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}

		});
		mnToolsPlugins.add(mntmRemove);

		final JMenuItem mntmSaveList = new JMenuItem(Messages.getString("MainFrame.113")); //$NON-NLS-1$
		mntmSaveList.addActionListener(e -> {
			try {
				if (new File(Messages.getString("MainFrame.114")).exists()) //$NON-NLS-1$
					new File(Messages.getString("MainFrame.115")).delete(); //$NON-NLS-1$
				new File(Messages.getString("MainFrame.116")).createNewFile(); //$NON-NLS-1$
			} catch (final Exception e11) {
				final Crash dialog1 = new Crash(e11);
				dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog1.setVisible(true);
			}
			PrintWriter w = null;
			try {
				w = new PrintWriter(new File(Messages.getString("MainFrame.117"))); //$NON-NLS-1$
			} catch (final FileNotFoundException e12) {
				// WTF?
				final Crash dialog2 = new Crash(e12);
				dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog2.setVisible(true);
			}
			for (int i = 0; i < MainFrame.this.toolAmount; i++) {
				if (MainFrame.this.tools[i] == null) {
					w.close();
					return;
				}
				w.println(MainFrame.this.tools[i].path);
				w.println(MainFrame.this.tools[i].commandline);
				w.println(MainFrame.this.tools[i].name);
				w.println(MainFrame.this.tools[i].hotkey);
			}
			w.close();
		});
		mnToolsPlugins.add(mntmSaveList);

		final JSeparator separator_1 = new JSeparator();
		mnToolsPlugins.add(separator_1);

		final JMenu mnScripts = new JMenu(Messages.getString("MainFrame.118")); //$NON-NLS-1$
		menuBar.add(mnScripts);

		final JMenuItem mntmAdd_1 = new JMenuItem(Messages.getString("MainFrame.119")); //$NON-NLS-1$
		mntmAdd_1.addActionListener(e -> {
			final String path = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.120")); //$NON-NLS-1$
			if (path == null)
				return;
			final String name = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.121")); //$NON-NLS-1$
			if (name == null)
				return;
			final String hotkey = JOptionPane.showInputDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.122")); //$NON-NLS-1$
			if (hotkey == null)
				return;
			MainFrame.this.scripts[MainFrame.this.scriptAmount] = new Script();
			MainFrame.this.scripts[MainFrame.this.scriptAmount].name = name;
			MainFrame.this.scripts[MainFrame.this.scriptAmount].path = path;
			MainFrame.this.scripts[MainFrame.this.scriptAmount].hotkey = hotkey;
			final ToolMenuItem tmpitem = new ToolMenuItem(MainFrame.this.scripts[MainFrame.this.scriptAmount].name);
			tmpitem.toolid = MainFrame.this.scriptAmount;
			tmpitem.addActionListener(e1 -> {
				final int toolid = tmpitem.toolid;
				MainFrame.this.runScript(MainFrame.this.scripts[toolid]);
			});
			MainFrame.this.scripts[MainFrame.this.scriptAmount].item = tmpitem;
			mnScripts.add(tmpitem);
			MainFrame.this.toolAmount++;
		});
		mnScripts.add(mntmAdd_1);

		final JMenuItem mntmRemove_1 = new JMenuItem(Messages.getString("MainFrame.123")); //$NON-NLS-1$
		mntmRemove_1.addActionListener(e -> {
			try {
				final int ans = Integer.parseInt(
						JOptionPane.showInputDialog(MainFrame.this.instance, Messages.getString("MainFrame.124"))); //$NON-NLS-1$
				if (ans >= 0 && ans < 32 && ans < MainFrame.this.toolAmount) {
					if (MainFrame.this.scripts[ans].item == null) {
						JOptionPane.showConfirmDialog(MainFrame.this.instance, Messages.getString("MainFrame.125")); //$NON-NLS-1$
						return;
					}
					mnToolsPlugins.remove(MainFrame.this.scripts[ans].item);
					if (ans == 31) {
						MainFrame.this.scripts[ans].name = Messages.getString("MainFrame.126"); //$NON-NLS-1$
						MainFrame.this.scripts[ans].path = Messages.getString("MainFrame.127"); //$NON-NLS-1$
						MainFrame.this.scripts[ans].hotkey = Messages.getString("MainFrame.128"); //$NON-NLS-1$
						MainFrame.this.scripts[ans].item = null;
					}
					for (int i = ans; i < 31; i++)
						MainFrame.this.scripts[ans] = MainFrame.this.scripts[ans + 1];
				} else {
					JOptionPane.showConfirmDialog(MainFrame.this.instance, Messages.getString("MainFrame.129")); //$NON-NLS-1$
					return;
				}
			} catch (final Exception e1) {
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		mnScripts.add(mntmRemove_1);

		final JMenuItem mntmSaveScripts = new JMenuItem(Messages.getString("MainFrame.130")); //$NON-NLS-1$
		mntmSaveScripts.addActionListener(arg0 -> {
			try {
				if (new File(Messages.getString("MainFrame.131")).exists()) //$NON-NLS-1$
					new File(Messages.getString("MainFrame.132")).delete(); //$NON-NLS-1$
				new File(Messages.getString("MainFrame.133")).createNewFile(); //$NON-NLS-1$
			} catch (final Exception e11) {
				final Crash dialog1 = new Crash(e11);
				dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog1.setVisible(true);
			}
			PrintWriter w = null;
			try {
				w = new PrintWriter(new File(Messages.getString("MainFrame.134"))); //$NON-NLS-1$
			} catch (final FileNotFoundException e12) {
				// WTF?
				final Crash dialog2 = new Crash(e12);
				dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog2.setVisible(true);
			}
			for (int i = 0; i < MainFrame.this.scriptAmount; i++) {
				if (MainFrame.this.scripts[i] == null) {
					w.close();
					return;
				}
				w.println(MainFrame.this.scripts[i].path);
				w.println(MainFrame.this.scripts[i].name);
				w.println(MainFrame.this.scripts[i].hotkey);
			}
			w.close();
		});
		mnScripts.add(mntmSaveScripts);

		final JSeparator separator_2 = new JSeparator();
		mnScripts.add(separator_2);

		final JMenu mnAbout = new JMenu(Messages.getString("MainFrame.135")); //$NON-NLS-1$
		menuBar.add(mnAbout);

		final JMenuItem mntmAbout = new JMenuItem(Messages.getString("MainFrame.136")); //$NON-NLS-1$
		mntmAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK));
		mntmAbout.addActionListener(arg0 -> {
			final AboutBox dialog = new AboutBox();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		});
		mnAbout.add(mntmAbout);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(this.contentPane);
		this.contentPane.setLayout(new BorderLayout(0, 0));

		final JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		this.contentPane.add(toolBar, BorderLayout.NORTH);

		final JButton btnNewButton = new JButton(Messages.getString("MainFrame.137")); //$NON-NLS-1$
		btnNewButton.addActionListener(e -> EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				final MainFrame frame = new MainFrame();
				frame.setVisible(true);
				MainFrame.instances++;
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			MainFrame.this.textPane.requestFocus();
		}));
		btnNewButton.setToolTipText(Messages.getString("MainFrame.138")); //$NON-NLS-1$
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.139")))); //$NON-NLS-1$
		toolBar.add(btnNewButton);

		final JButton btnOpenButton = new JButton(Messages.getString("MainFrame.140")); //$NON-NLS-1$
		btnOpenButton.addActionListener(e -> {
			final JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(MainFrame.this.instance) != JFileChooser.APPROVE_OPTION)
				return;
			try {
				final FileReader reader = new FileReader(chooser.getSelectedFile());
				final BufferedReader br = new BufferedReader(reader);
				MainFrame.this.textPane.read(br, null);
				br.close();
				MainFrame.this.textPane.requestFocus();
			} catch (final Exception e2) {
				final Crash dialog = new Crash(e2);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		btnOpenButton.setToolTipText(Messages.getString("MainFrame.141")); //$NON-NLS-1$
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.142")))); //$NON-NLS-1$
		toolBar.add(btnOpenButton);

		final JButton btnSaveButton = new JButton(Messages.getString("MainFrame.143")); //$NON-NLS-1$
		btnSaveButton.addActionListener(e -> {
			if (MainFrame.this.currentFile == null) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText(Messages.getString("MainFrame.144")); //$NON-NLS-1$
				final int actionDialog = SaveAs.showSaveDialog(MainFrame.this.instance);
				if (actionDialog != JFileChooser.APPROVE_OPTION)
					return;

				final File fileName1 = SaveAs.getSelectedFile();
				BufferedWriter outFile1 = null;
				try {
					outFile1 = new BufferedWriter(new FileWriter(fileName1));
					MainFrame.this.textPane.write(outFile1);
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
				MainFrame.this.textPane.requestFocus();
			} else {
				final File fileName2 = MainFrame.this.currentFile;
				BufferedWriter outFile2 = null;
				try {
					outFile2 = new BufferedWriter(new FileWriter(fileName2));
					MainFrame.this.textPane.write(outFile2);
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
				MainFrame.this.textPane.requestFocus();
			}
		});
		btnSaveButton.setToolTipText(Messages.getString("MainFrame.145")); //$NON-NLS-1$
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.146")))); //$NON-NLS-1$
		toolBar.add(btnSaveButton);

		final JButton btnCloseButton = new JButton(Messages.getString("MainFrame.147")); //$NON-NLS-1$
		btnCloseButton.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			MainFrame.this.instance.dispose();
		});
		btnCloseButton.setToolTipText(Messages.getString("MainFrame.148")); //$NON-NLS-1$
		btnCloseButton.setFocusPainted(false);
		btnCloseButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.149")))); //$NON-NLS-1$
		toolBar.add(btnCloseButton);

		final JButton btnCutButton = new JButton(Messages.getString("MainFrame.150")); //$NON-NLS-1$
		btnCutButton.addActionListener(e -> MainFrame.this.textPane.cut());
		btnCutButton.setToolTipText(Messages.getString("MainFrame.151")); //$NON-NLS-1$
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.152")))); //$NON-NLS-1$
		toolBar.add(btnCutButton);

		final JButton btnCopyButton = new JButton(Messages.getString("MainFrame.153")); //$NON-NLS-1$
		btnCopyButton.addActionListener(e -> MainFrame.this.textPane.copy());
		btnCopyButton.setToolTipText(Messages.getString("MainFrame.154")); //$NON-NLS-1$
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.155")))); //$NON-NLS-1$
		toolBar.add(btnCopyButton);

		final JButton btnPasteButton = new JButton(Messages.getString("MainFrame.156")); //$NON-NLS-1$
		btnPasteButton.addActionListener(e -> MainFrame.this.textPane.paste());
		btnPasteButton.setToolTipText(Messages.getString("MainFrame.157")); //$NON-NLS-1$
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.158")))); //$NON-NLS-1$
		toolBar.add(btnPasteButton);

		final JButton btnDeleteButton = new JButton(Messages.getString("MainFrame.159")); //$NON-NLS-1$
		btnDeleteButton
				.addActionListener(e -> MainFrame.this.textPane.replaceSelection(Messages.getString("MainFrame.160")));
		btnDeleteButton.setToolTipText(Messages.getString("MainFrame.161")); //$NON-NLS-1$
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.162")))); //$NON-NLS-1$
		toolBar.add(btnDeleteButton);

		final JButton btnUndoButton = new JButton(Messages.getString("MainFrame.163")); //$NON-NLS-1$
		btnUndoButton.addActionListener(e -> MainFrame.this.textPane.undoLastAction());
		btnUndoButton.setToolTipText(Messages.getString("MainFrame.164")); //$NON-NLS-1$
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.165")))); //$NON-NLS-1$
		toolBar.add(btnUndoButton);

		final JButton btnRedoButton = new JButton(Messages.getString("MainFrame.166")); //$NON-NLS-1$
		btnRedoButton.addActionListener(e -> MainFrame.this.textPane.redoLastAction());
		btnRedoButton.setToolTipText(Messages.getString("MainFrame.167")); //$NON-NLS-1$
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource(Messages.getString("MainFrame.168")))); //$NON-NLS-1$
		toolBar.add(btnRedoButton);

		final RTextScrollPane scrollPane = new RTextScrollPane();
		this.contentPane.add(scrollPane, BorderLayout.CENTER);

		this.textPane.setFont(new Font(Messages.getString("MainFrame.169"), Font.PLAIN, 13)); //$NON-NLS-1$
		scrollPane.setViewportView(this.textPane);

		final JPanel panel = new JPanel();
		this.contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));

		final JPanel searchPanel = new JPanel();
		panel.add(searchPanel, BorderLayout.NORTH);

		final JLabel lblSearch = new JLabel(Messages.getString("MainFrame.170")); //$NON-NLS-1$
		searchPanel.add(lblSearch);

		this.searchTextField = new JTextField();
		searchPanel.add(this.searchTextField);
		this.searchTextField.setColumns(10);

		final JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		final JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);

		final JLabel lblReplace = new JLabel(Messages.getString("MainFrame.171")); //$NON-NLS-1$
		panel_2.add(lblReplace);

		this.replaceWithTextField = new JTextField();
		panel_2.add(this.replaceWithTextField);
		this.replaceWithTextField.setColumns(10);

		final JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));

		final JPanel panel_4 = new JPanel();
		panel_3.add(panel_4, BorderLayout.NORTH);

		final JButton btnSearch = new JButton(Messages.getString("MainFrame.172")); //$NON-NLS-1$
		btnSearch.addActionListener(e -> {
			final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
					MainFrame.this.textPane.getCaretPosition());
			final int l2 = MainFrame.this.searchTextField.getText().length();
			if (l1 == -1)
				JOptionPane.showMessageDialog(MainFrame.this.instance, Messages.getString("MainFrame.173") //$NON-NLS-1$
						+ MainFrame.this.searchTextField.getText() + Messages.getString("MainFrame.174")); //$NON-NLS-1$
			else
				MainFrame.this.textPane.select(l1, l2 + l1);
		});
		panel_4.add(btnSearch);

		final JButton btnReplace = new JButton(Messages.getString("MainFrame.175")); //$NON-NLS-1$
		btnReplace.addActionListener(e -> {
			final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
					MainFrame.this.textPane.getCaretPosition());
			final int l2 = MainFrame.this.searchTextField.getText().length();
			if (l1 == -1)
				JOptionPane.showMessageDialog(MainFrame.this.instance, Messages.getString("MainFrame.176") //$NON-NLS-1$
						+ MainFrame.this.searchTextField.getText() + Messages.getString("MainFrame.177")); //$NON-NLS-1$
			else {
				MainFrame.this.textPane.select(l1, l2 + l1);
				MainFrame.this.textPane.replaceSelection(MainFrame.this.replaceWithTextField.getText());
				MainFrame.this.textPane.select(l1, l2 + l1);
			}
		});
		panel_4.add(btnReplace);

		final JPanel panel_5 = new JPanel();
		panel_3.add(panel_5, BorderLayout.CENTER);
		panel_5.setLayout(new BorderLayout(0, 0));

		final JPanel panel_6 = new JPanel();
		panel_5.add(panel_6, BorderLayout.NORTH);

		final JButton btnCountOccurences = new JButton(Messages.getString("MainFrame.178")); //$NON-NLS-1$
		btnCountOccurences.addActionListener(e -> {
			int amount = 0;
			while (true) {
				final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
						MainFrame.this.textPane.getCaretPosition());
				final int l2 = MainFrame.this.searchTextField.getText().length();
				if (l1 == -1)
					break;
				else {
					MainFrame.this.textPane.setCaretPosition(l1 + l2);
					amount++;
				}
			}
			JOptionPane.showMessageDialog(MainFrame.this.instance,
					Messages.getString("MainFrame.179") + amount + Messages.getString("MainFrame.180")); //$NON-NLS-1$ //$NON-NLS-2$
		});
		panel_6.add(btnCountOccurences);

		final JPanel panel_7 = new JPanel();
		panel_5.add(panel_7, BorderLayout.SOUTH);

		final JButton btnBlack = new JButton(Messages.getString("MainFrame.181")); //$NON-NLS-1$
		btnBlack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream(Messages.getString("MainFrame.182"))); //$NON-NLS-1$
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnBlack);

		final JButton btnClassical = new JButton(Messages.getString("MainFrame.183")); //$NON-NLS-1$
		btnClassical.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream(Messages.getString("MainFrame.184"))); //$NON-NLS-1$
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnClassical);

		final JPanel panel_8 = new JPanel();
		panel_5.add(panel_8, BorderLayout.CENTER);
		panel_8.setLayout(new BorderLayout(0, 0));

		final JPanel panel_9 = new JPanel();
		panel_8.add(panel_9, BorderLayout.SOUTH);

		final JButton btnNewButton_1 = new JButton(Messages.getString("MainFrame.185")); //$NON-NLS-1$
		btnNewButton_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream(Messages.getString("MainFrame.186"))); //$NON-NLS-1$
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnNewButton_1);

		final JButton btnMonokai = new JButton(Messages.getString("MainFrame.187")); //$NON-NLS-1$
		btnMonokai.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream(Messages.getString("MainFrame.188"))); //$NON-NLS-1$
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnMonokai);

		final JPanel panel_10 = new JPanel();
		panel_8.add(panel_10, BorderLayout.CENTER);
		panel_10.setLayout(new BorderLayout(0, 0));

		final JPanel panel_11 = new JPanel();
		panel_10.add(panel_11, BorderLayout.SOUTH);

		final JLabel lblTheme = new JLabel(Messages.getString("MainFrame.189")); //$NON-NLS-1$
		panel_11.add(lblTheme);

		final JPanel panel_12 = new JPanel();
		panel_10.add(panel_12, BorderLayout.CENTER);
		panel_12.setLayout(new BorderLayout(0, 0));

		final JLabel lblToolConsole = new JLabel(Messages.getString("MainFrame.190")); //$NON-NLS-1$
		panel_12.add(lblToolConsole, BorderLayout.NORTH);

		final JScrollPane scrollPane_1 = new JScrollPane();
		panel_12.add(scrollPane_1, BorderLayout.CENTER);

		this.toolConsole.setEditable(false);
		scrollPane_1.setViewportView(this.toolConsole);

		final JPanel panel_13 = new JPanel();
		panel_12.add(panel_13, BorderLayout.SOUTH);

		final JButton btnOpenInDialog = new JButton(Messages.getString("MainFrame.191")); //$NON-NLS-1$
		btnOpenInDialog.addActionListener(e -> {
			final CommandOutputDialog dialog = new CommandOutputDialog(MainFrame.this.toolConsole.getText());
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		});
		panel_13.add(btnOpenInDialog);

		final JButton btnClear = new JButton(Messages.getString("MainFrame.192")); //$NON-NLS-1$
		btnClear.addActionListener(e -> MainFrame.this.toolConsole.setText(Messages.getString("MainFrame.193")));
		panel_13.add(btnClear);

		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				MainFrame.this.lblReady
						.setText(Messages.getString("MainFrame.194") + MainFrame.this.textPane.getText().length() //$NON-NLS-1$
								+ Messages.getString("MainFrame.195") //$NON-NLS-1$
								+ (MainFrame.this.currentFile == null ? Messages.getString("MainFrame.196") //$NON-NLS-1$
										: MainFrame.this.currentFile.getAbsolutePath()) + Messages.getString("MainFrame.197") //$NON-NLS-1$
								+ (MainFrame.this.currentFile == null ? Messages.getString("MainFrame.198") //$NON-NLS-1$
										: MainFrame.this.currentFile.getFreeSpace() / 1024)
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
				if (MainFrame.instances == 0)
					System.exit(0);
			}
		}, 0, 1);
		this.textPane.clearParsers();
		this.textPane.setParserDelay(1);
		this.textPane.setAnimateBracketMatching(true);
		this.textPane.setAutoIndentEnabled(true);
		this.textPane.setAntiAliasingEnabled(true);
		this.textPane.setBracketMatchingEnabled(true);
		this.textPane.setCloseCurlyBraces(true);
		this.textPane.setCloseMarkupTags(true);
		this.textPane.setCodeFoldingEnabled(true);
		this.textPane.setHyperlinkForeground(Color.pink);
		this.textPane.setHyperlinksEnabled(true);
		this.textPane.setPaintMatchedBracketPair(true);
		this.textPane.setPaintTabLines(true);
		try {
			final Theme theme = Theme.load(this.getClass().getResourceAsStream(Messages.getString("MainFrame.208"))); //$NON-NLS-1$
			theme.apply(this.textPane);
		} catch (final IOException ioe) { // Never happens
			final Crash dialog = new Crash(ioe);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);

		final JPanel panel_14 = new JPanel();
		this.contentPane.add(panel_14, BorderLayout.SOUTH);
		panel_14.setLayout(new BorderLayout(0, 0));

		final JToolBar toolBar_1 = new JToolBar();
		panel_14.add(toolBar_1);
		toolBar_1.setFloatable(false);

		toolBar_1.add(this.lblReady);

		if (new File(Messages.getString("MainFrame.209")).exists()) { //$NON-NLS-1$
			Scanner s = null;
			try {
				s = new Scanner(new File(Messages.getString("MainFrame.210"))); //$NON-NLS-1$
			} catch (final FileNotFoundException e1) {
				// WTF?
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			int counter = 0;
			while (s.hasNextLine()) {
				this.tools[counter] = new Tool();
				this.tools[counter].path = s.nextLine();
				this.tools[counter].commandline = s.nextLine();
				this.tools[counter].name = s.nextLine();
				this.tools[counter].hotkey = s.nextLine();
				final ToolMenuItem tmpitem = new ToolMenuItem(this.tools[counter].name);
				tmpitem.toolid = this.toolAmount;
				tmpitem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final int toolid = tmpitem.toolid;
						try {
							String copy = MainFrame.this.tools[toolid].commandline;
							copy = copy.replaceAll(Messages.getString("MainFrame.213"), //$NON-NLS-1$
									MainFrame.this.currentFile == null ? Messages.getString("MainFrame.214") //$NON-NLS-1$
											: MainFrame.this.currentFile.getName());
							copy = copy.replaceAll(Messages.getString("MainFrame.215"), //$NON-NLS-1$
									MainFrame.this.currentFile == null ? Messages.getString("MainFrame.216") //$NON-NLS-1$
											: MainFrame.this.currentFile.getParentFile().getAbsolutePath());
							copy = copy.replaceAll(Messages.getString("MainFrame.217"), //$NON-NLS-1$
									MainFrame.this.currentFile == null ? Messages.getString("MainFrame.218") //$NON-NLS-1$
											: this.getFileExtension(MainFrame.this.currentFile));
							final Process p = Runtime.getRuntime().exec(MainFrame.this.tools[toolid].path
									+ Messages.getString("MainFrame.219") + MainFrame.this.tools[toolid].commandline); //$NON-NLS-1$
							new Thread(() -> {
								final BufferedReader stdInput = new BufferedReader(
										new InputStreamReader(p.getInputStream()));

								final BufferedReader stdError = new BufferedReader(
										new InputStreamReader(p.getErrorStream()));

								MainFrame.this.toolConsole.setText(
										MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.220")); //$NON-NLS-1$
								String s1 = null;
								try {
									while ((s1 = stdInput.readLine()) != null)
										MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s1);
								} catch (final IOException e1) {
									final Crash dialog1 = new Crash(e1);
									dialog1.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
									dialog1.setVisible(true);
									return;
								}

								// read any errors from the attempted command
								MainFrame.this.toolConsole.setText(
										MainFrame.this.toolConsole.getText() + Messages.getString("MainFrame.221")); //$NON-NLS-1$
								try {
									while ((s1 = stdError.readLine()) != null)
										MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s1);
								} catch (final IOException e2) {
									final Crash dialog2 = new Crash(e2);
									dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
									dialog2.setVisible(true);
									return;
								}
								return;
							}).start();
						} catch (final IOException e1) {
							final Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						}
					}

					private String getFileExtension(final File file) {
						final String name = file.getName();
						try {
							return name.substring(name.lastIndexOf(Messages.getString("MainFrame.211")) + 1); //$NON-NLS-1$
						} catch (final Exception e) {
							return Messages.getString("MainFrame.212"); //$NON-NLS-1$
						}
					}
				});
				this.tools[counter].item = tmpitem;
				mnToolsPlugins.add(tmpitem);
				counter++;
			}
			this.toolAmount = counter;
		}
		if (new File(Messages.getString("MainFrame.222")).exists()) { //$NON-NLS-1$
			Scanner s = null;
			try {
				s = new Scanner(new File(Messages.getString("MainFrame.223"))); //$NON-NLS-1$
			} catch (final FileNotFoundException e1) {
				// WTF?
				final Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			int counter = 0;
			while (s.hasNextLine()) {
				this.scripts[counter] = new Script();
				this.scripts[counter].path = s.nextLine();
				this.scripts[counter].name = s.nextLine();
				this.scripts[counter].hotkey = s.nextLine();
				final ToolMenuItem tmpitem = new ToolMenuItem(this.scripts[counter].name);
				tmpitem.toolid = this.scriptAmount;
				tmpitem.addActionListener(e -> {
					final int toolid = tmpitem.toolid;
					MainFrame.this.runScript(MainFrame.this.scripts[toolid]);
				});
				this.tools[counter].item = tmpitem;
				mnToolsPlugins.add(tmpitem);
				counter++;
			}
			this.toolAmount = counter;
		}
	}

	public void runScript(final Script script) {
		try {
			new Scanner(new File(Messages.getString("MainFrame.224"))) //$NON-NLS-1$
					.useDelimiter(Messages.getString("MainFrame.225")).next();
			final ScriptEngineManager mgr = new ScriptEngineManager();
			final ScriptEngine jsEngine = mgr.getEngineByName(Messages.getString("MainFrame.226")); //$NON-NLS-1$
			final Invocable invocable = (Invocable) jsEngine;
			try {
				invocable.invokeFunction(Messages.getString("MainFrame.227"), this, script, MainFrame.instances, //$NON-NLS-1$
						this.textPane, this.currentFile, this.lblReady, this.searchTextField, this.replaceWithTextField, this.tools,
						this.scripts, this.scriptAmount, this.toolAmount, this.toolConsole);
			} catch (NoSuchMethodException | ScriptException e) {
				final Crash dialog = new Crash(e);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		} catch (final FileNotFoundException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
	}

}
