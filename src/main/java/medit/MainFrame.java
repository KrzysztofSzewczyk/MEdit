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
	private final JLabel lblReady = new JLabel(
			"Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
	private JTextField replaceWithTextField;
	private JTextField searchTextField;
	private final RSyntaxTextArea textPane = new RSyntaxTextArea();
	JTextPane toolConsole;

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

		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(MainFrame.class.getResource("/medit/assets/apps/accessories-text-editor.png")));
		this.setTitle("MEdit");
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBounds(100, 100, 700, 500);
		this.setMinimumSize(new Dimension(700, 500));

		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		final JMenuItem mntmNew = new JMenuItem("New");
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

		final JMenuItem mntmOpen = new JMenuItem("Open");
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

		final JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(e -> {
			if (MainFrame.this.currentFile == null) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText("Save");
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

		final JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mntmSaveAs.addActionListener(e -> {
			final JFileChooser SaveAs = new JFileChooser();
			SaveAs.setApproveButtonText("Save");
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

		final JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExit.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			MainFrame.this.instance.dispose();
		});
		mnFile.add(mntmExit);

		final JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		final JMenuItem mntmCut = new JMenuItem("Cut");
		mntmCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		mntmCut.addActionListener(e -> MainFrame.this.textPane.cut());
		mnEdit.add(mntmCut);

		final JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		mntmCopy.addActionListener(e -> MainFrame.this.textPane.copy());
		mnEdit.add(mntmCopy);

		final JMenuItem mntmPaste = new JMenuItem("Paste");
		mntmPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		mntmPaste.addActionListener(e -> MainFrame.this.textPane.paste());
		mnEdit.add(mntmPaste);

		final JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
		mntmDelete.addActionListener(e -> MainFrame.this.textPane.replaceSelection(""));
		mnEdit.add(mntmDelete);

		final JSeparator separator_4 = new JSeparator();
		mnEdit.add(separator_4);

		final JMenuItem mntmUndo = new JMenuItem("Undo");
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmUndo.addActionListener(e -> MainFrame.this.textPane.undoLastAction());
		mnEdit.add(mntmUndo);

		final JMenuItem mntmRedo = new JMenuItem("Redo");
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		mntmRedo.addActionListener(e -> MainFrame.this.textPane.redoLastAction());
		mnEdit.add(mntmRedo);

		final JMenu mnLanguage = new JMenu("Language");
		menuBar.add(mnLanguage);

		final JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem("English");
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);

		final JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");
		menuBar.add(mnSyntaxHighlighting);

		final JMenuItem mntmNo = new JMenuItem("No");
		mntmNo.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE));
		mnSyntaxHighlighting.add(mntmNo);

		final JMenu mnA = new JMenu("A");
		mnSyntaxHighlighting.add(mnA);

		final JMenuItem mntmActionscript = new JMenuItem("ActionScript");
		mnA.add(mntmActionscript);

		final JMenuItem mntmAssembler = new JMenuItem("Assembly");
		mnA.add(mntmAssembler);
		mntmAssembler.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86));
		mntmActionscript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT));

		final JMenuItem mntmBbcode = new JMenuItem("BBCode");
		mntmBbcode.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BBCODE));
		mnSyntaxHighlighting.add(mntmBbcode);

		final JMenu mnC = new JMenu("C");
		mnSyntaxHighlighting.add(mnC);

		final JMenuItem mntmC = new JMenuItem("C");
		mnC.add(mntmC);

		final JMenuItem mntmC_1 = new JMenuItem("C++");
		mnC.add(mntmC_1);

		final JMenuItem mntmC_2 = new JMenuItem("C#");
		mnC.add(mntmC_2);

		final JMenuItem mntmClojure = new JMenuItem("Clojure");
		mnC.add(mntmClojure);
		mntmClojure.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CLOJURE));
		mntmC_2.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP));
		mntmC_1.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
		mntmC.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C));

		final JMenu mnD = new JMenu("D");
		mnSyntaxHighlighting.add(mnD);

		final JMenuItem mntmDart = new JMenuItem("Dart");
		mnD.add(mntmDart);

		final JMenuItem mntmDelphi = new JMenuItem("Delphi");
		mnD.add(mntmDelphi);

		final JMenuItem mntmDocker = new JMenuItem("Docker");
		mnD.add(mntmDocker);

		final JMenuItem mntmDtd = new JMenuItem("DTD");
		mnD.add(mntmDtd);

		final JMenuItem mntmD = new JMenuItem("D");
		mnD.add(mntmD);
		mntmD.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_D));
		mntmDtd.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DTD));
		mntmDocker.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE));
		mntmDelphi.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DELPHI));
		mntmDart.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DART));

		final JMenuItem mntmFortan = new JMenuItem("Fortan");
		mntmFortan.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_FORTRAN));
		mnSyntaxHighlighting.add(mntmFortan);

		final JMenuItem mntmGroovy = new JMenuItem("Groovy");
		mntmGroovy.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY));
		mnSyntaxHighlighting.add(mntmGroovy);

		final JMenu mnH = new JMenu("H");
		mnSyntaxHighlighting.add(mnH);

		final JMenuItem mntmHtaccess = new JMenuItem("HTAccess");
		mnH.add(mntmHtaccess);
		mntmHtaccess.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTACCESS));

		final JMenuItem mntmHosts = new JMenuItem("Hosts");
		mnH.add(mntmHosts);

		final JMenuItem mntmHtml = new JMenuItem("HTML");
		mnH.add(mntmHtml);
		mntmHtml.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML));
		mntmHosts.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HOSTS));

		final JMenuItem mntmIni = new JMenuItem("INI");
		mntmIni.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_INI));
		mnSyntaxHighlighting.add(mntmIni);

		final JMenu mnJ = new JMenu("J");
		mnSyntaxHighlighting.add(mnJ);

		final JMenuItem mntmJavascript = new JMenuItem("JavaScript");
		mnJ.add(mntmJavascript);

		final JMenuItem mntmJava = new JMenuItem("Java");
		mnJ.add(mntmJava);

		final JMenuItem mntmJshintrc = new JMenuItem("JSON");
		mnJ.add(mntmJshintrc);

		final JMenuItem mntmJsp = new JMenuItem("JSP");
		mnJ.add(mntmJsp);
		mntmJsp.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSP));
		mntmJshintrc.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON));
		mntmJava.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA));
		mntmJavascript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT));

		final JMenu mnL = new JMenu("L");
		mnSyntaxHighlighting.add(mnL);

		final JMenuItem mntmLatex = new JMenuItem("Latex");
		mnL.add(mntmLatex);

		final JMenuItem mntmLess = new JMenuItem("Less");
		mnL.add(mntmLess);

		final JMenuItem mntmLisp = new JMenuItem("Lisp");
		mnL.add(mntmLisp);

		final JMenuItem mntmLua = new JMenuItem("Lua");
		mnL.add(mntmLua);
		mntmLua.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA));
		mntmLisp.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LISP));
		mntmLess.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LESS));
		mntmLatex.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX));

		final JMenu mnM = new JMenu("M");
		mnSyntaxHighlighting.add(mnM);

		final JMenuItem mntmMakeFile = new JMenuItem("MakeFile");
		mnM.add(mntmMakeFile);

		final JMenuItem mntmMxml = new JMenuItem("MXML");
		mnM.add(mntmMxml);
		mntmMxml.addActionListener(
				arg0 -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MXML));
		mntmMakeFile.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MAKEFILE));

		final JMenuItem mntmNsis = new JMenuItem("NSIS");
		mntmNsis.addActionListener(
				arg0 -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NSIS));
		mnSyntaxHighlighting.add(mntmNsis);

		final JMenu mnP = new JMenu("P");
		mnSyntaxHighlighting.add(mnP);

		final JMenuItem mntmPerl = new JMenuItem("Perl");
		mnP.add(mntmPerl);

		final JMenuItem mntmPropertiesFile = new JMenuItem("Properties File");
		mntmPropertiesFile.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE));
		mnP.add(mntmPropertiesFile);

		final JMenuItem mntmPython = new JMenuItem("Python");
		mntmPython.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON));
		mnP.add(mntmPython);
		mntmPerl.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PERL));

		final JMenu mnR = new JMenu("R");
		mnSyntaxHighlighting.add(mnR);

		final JMenuItem mntmRuby = new JMenuItem("Ruby"); // Forever alone, Ruby.
		mntmRuby.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY));
		mnR.add(mntmRuby);

		final JMenu mnS = new JMenu("S");
		mnSyntaxHighlighting.add(mnS);

		final JMenuItem mntmSas = new JMenuItem("SAS");
		mntmSas.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS));
		mnS.add(mntmSas);

		final JMenuItem mntmSacala = new JMenuItem("Scala");
		mntmSacala.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA));
		mnS.add(mntmSacala);

		final JMenuItem mntmSql = new JMenuItem("SQL");
		mntmSql.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL));
		mnS.add(mntmSql);

		final JMenu mnT = new JMenu("T");
		mnSyntaxHighlighting.add(mnT);

		final JMenuItem mntmTcl = new JMenuItem("TCL");
		mntmTcl.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TCL));
		mnT.add(mntmTcl);

		final JMenuItem mntmTypescript = new JMenuItem("TypeScript");
		mntmTypescript.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT));
		mnT.add(mntmTypescript);

		final JMenuItem mntmUnixShell = new JMenuItem("Unix Shell");
		mntmUnixShell.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL));
		mnSyntaxHighlighting.add(mntmUnixShell);

		final JMenuItem mntmVisualBasic = new JMenuItem("Visual Basic");
		mntmVisualBasic.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC));
		mnSyntaxHighlighting.add(mntmVisualBasic);

		final JMenuItem mntmWindowsBatch = new JMenuItem("Windows Batch");
		mntmWindowsBatch.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH));
		mnSyntaxHighlighting.add(mntmWindowsBatch);

		final JMenuItem mntmXml = new JMenuItem("XML");
		mntmXml.addActionListener(e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML));
		mnSyntaxHighlighting.add(mntmXml);

		final JMenuItem mntmYaml = new JMenuItem("YAML");
		mntmYaml.addActionListener(
				e -> MainFrame.this.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML));
		mnSyntaxHighlighting.add(mntmYaml);

		final JMenu mnManageCompilers = new JMenu("Compilers");
		menuBar.add(mnManageCompilers);

		final JMenu mnAssembly = new JMenu("Assembly");
		mnManageCompilers.add(mnAssembly);

		final JMenuItem mntmNasm = new JMenuItem("NASM");
		mntmNasm.addActionListener(new ActionListener() {
			private static final int BUFFER_SIZE = 4096;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final String name = "nasm", compilername = "nasm";
				if (!new File("compilers\\" + name + "\\installed.dat").exists()) {
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please install " + name + " package.",
							"Error", JOptionPane.ERROR_MESSAGE);
					if (JOptionPane.showConfirmDialog(MainFrame.this.instance,
							"Do you want to download and install " + name
									+ " package?\n (By installing it you accept License provided with software)",
							"Package manager", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						URL url = null;
						try {
							url = new URL("https://raw.githubusercontent.com/KrzysztofSzewczyk/MEdit/master/packages/"
									+ name + ".zip");
						} catch (final MalformedURLException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
						try {
							final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
							new File("compilers/" + name).mkdirs();
							try (InputStream stream = con.getInputStream()) {
								Files.copy(stream, Paths.get("compilers/" + name + "/package.zip"));
							}
							this.unzip("compilers/" + name + "/package.zip", "compilers/");
							new File("compilers/" + name + "/package.zip").delete();
						} catch (final IOException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
					} else
						return;
				}
				if (MainFrame.this.currentFile == null)
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please save your work in order to compile.",
							"Eggs are supposed to be green!", JOptionPane.ERROR_MESSAGE);
				else {
					final String osString = OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS ? "macos"
							: OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows ? "windows" : "linux";
					final FileArrayProvider fap = new FileArrayProvider(); // FAP, huh... TODO: change name
					String[] lines = null;
					try {
						lines = fap.readLines("compilers/" + name + "/" + osString + "/options.txt");
					} catch (final IOException e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
						return;
					}
					final String[] command = this.concatAll(
							new String[] { "compilers/" + name + "/" + osString + "/" + compilername }, lines,
							new String[] { "\"" + MainFrame.this.currentFile.getAbsolutePath() + "\"" });
					System.out.println(command[0] + " " + command[1] + " " + command[2] + " " + command[3] + " ");
					final ProcessBuilder pb = new ProcessBuilder(command);
					try {
						pb.directory(new File(MainFrame.this.currentFile.getAbsoluteFile().getParent()));
					} catch (final Exception e1) {
						// I Don't care
					}
					try {
						final Process p = pb.start();
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "STDOUT:\n");
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
							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "\nSTDERR:\n");
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e) {
								final Crash dialog2 = new Crash(e);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}

							final CommandOutputDialog dialog3 = new CommandOutputDialog(
									MainFrame.this.toolConsole.getText());
							dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog3.setVisible(true);

							MainFrame.this.toolConsole.setText("");

							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}

			@SuppressWarnings("unchecked")
			public <T> T[] concatAll(final T[] first, final T[]... rest) {
				int totalLength = first.length;
				for (final T[] array : rest)
					totalLength += array.length;
				final T[] result = Arrays.copyOf(first, totalLength);
				int offset = first.length;
				for (final T[] array : rest) {
					System.arraycopy(array, 0, result, offset, array.length);
					offset += array.length;
				}
				return result;
			}

			private void extractFile(final ZipInputStream zipIn, final String filePath) throws IOException {
				final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				final byte[] bytesIn = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = zipIn.read(bytesIn)) != -1)
					bos.write(bytesIn, 0, read);
				bos.close();
			}

			public void unzip(final String zipFilePath, final String destDirectory) throws IOException {
				final File destDir = new File(destDirectory);
				if (!destDir.exists())
					destDir.mkdir();
				final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
				ZipEntry entry = zipIn.getNextEntry();
				// iterates over entries in the zip file
				while (entry != null) {
					final String filePath = destDirectory + File.separator + entry.getName();
					if (!entry.isDirectory())
						// if the entry is a file, extracts it
						this.extractFile(zipIn, filePath);
					else {
						// if the entry is a directory, make the directory
						final File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
			}
		});
		mnAssembly.add(mntmNasm);

		final JMenuItem mntmFasm = new JMenuItem("FASM (MacOS not supported)");
		mntmFasm.addActionListener(new ActionListener() {
			private static final int BUFFER_SIZE = 4096;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final String name = "fasm", compilername = "fasm";
				if (!new File("compilers\\" + name + "\\installed.dat").exists()) {
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please install " + name + " package.",
							"Error", JOptionPane.ERROR_MESSAGE);
					if (JOptionPane.showConfirmDialog(MainFrame.this.instance,
							"Do you want to download and install " + name
									+ " package?\n (By installing it you accept License provided with software)",
							"Package manager", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						URL url = null;
						try {
							url = new URL("https://raw.githubusercontent.com/KrzysztofSzewczyk/MEdit/master/packages/"
									+ name + ".zip");
						} catch (final MalformedURLException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
						try {
							final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
							new File("compilers/" + name).mkdirs();
							try (InputStream stream = con.getInputStream()) {
								Files.copy(stream, Paths.get("compilers/" + name + "/package.zip"));
							}
							this.unzip("compilers/" + name + "/package.zip", "compilers/");
							new File("compilers/" + name + "/package.zip").delete();
						} catch (final IOException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
					} else
						return;
				}
				if (MainFrame.this.currentFile == null)
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please save your work in order to compile.",
							"Eggs are supposed to be green!", JOptionPane.ERROR_MESSAGE);
				else {
					final String osString = OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS ? "macos"
							: OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows ? "windows" : "linux";
					final FileArrayProvider fap = new FileArrayProvider(); // FAP, huh... TODO: change name
					String[] lines = null;
					try {
						lines = fap.readLines("compilers/" + name + "/" + osString + "/options.txt");
					} catch (final IOException e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
						return;
					}
					final String[] command = this.concatAll(
							new String[] { "compilers/" + name + "/" + osString + "/" + compilername }, lines,
							new String[] { "\"" + MainFrame.this.currentFile.getAbsolutePath() + "\"" });
					System.out.println(command[0] + " " + command[1] + " " + command[2] + " " + command[3] + " ");
					final ProcessBuilder pb = new ProcessBuilder(command);
					try {
						pb.directory(new File(MainFrame.this.currentFile.getAbsoluteFile().getParent()));
					} catch (final Exception e1) {
						// I Don't care
					}
					try {
						final Process p = pb.start();
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "STDOUT:\n");
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
							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "\nSTDERR:\n");
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e) {
								final Crash dialog2 = new Crash(e);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}

							final CommandOutputDialog dialog3 = new CommandOutputDialog(
									MainFrame.this.toolConsole.getText());
							dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog3.setVisible(true);

							MainFrame.this.toolConsole.setText("");

							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}

			@SuppressWarnings("unchecked")
			public <T> T[] concatAll(final T[] first, final T[]... rest) {
				int totalLength = first.length;
				for (final T[] array : rest)
					totalLength += array.length;
				final T[] result = Arrays.copyOf(first, totalLength);
				int offset = first.length;
				for (final T[] array : rest) {
					System.arraycopy(array, 0, result, offset, array.length);
					offset += array.length;
				}
				return result;
			}

			private void extractFile(final ZipInputStream zipIn, final String filePath) throws IOException {
				final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				final byte[] bytesIn = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = zipIn.read(bytesIn)) != -1)
					bos.write(bytesIn, 0, read);
				bos.close();
			}

			public void unzip(final String zipFilePath, final String destDirectory) throws IOException {
				final File destDir = new File(destDirectory);
				if (!destDir.exists())
					destDir.mkdir();
				final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
				ZipEntry entry = zipIn.getNextEntry();
				// iterates over entries in the zip file
				while (entry != null) {
					final String filePath = destDirectory + File.separator + entry.getName();
					if (!entry.isDirectory())
						// if the entry is a file, extracts it
						this.extractFile(zipIn, filePath);
					else {
						// if the entry is a directory, make the directory
						final File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
			}
		});
		mnAssembly.add(mntmFasm);

		final JMenu mnC_1 = new JMenu("C");
		mnManageCompilers.add(mnC_1);

		final JMenuItem mntmTinycwindowsOnly = new JMenuItem("TinyC (Windows only)");
		mntmTinycwindowsOnly.addActionListener(new ActionListener() {
			private static final int BUFFER_SIZE = 4096;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final String name = "tinyc", compilername = "tcc";
				if (!new File("compilers\\" + name + "\\installed.dat").exists()) {
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please install " + name + " package.",
							"Error", JOptionPane.ERROR_MESSAGE);
					if (JOptionPane.showConfirmDialog(MainFrame.this.instance,
							"Do you want to download and install " + name
									+ " package?\n (By installing it you accept License provided with software)",
							"Package manager", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						URL url = null;
						try {
							url = new URL("https://raw.githubusercontent.com/KrzysztofSzewczyk/MEdit/master/packages/"
									+ name + ".zip");
						} catch (final MalformedURLException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
						try {
							final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
							new File("compilers/" + name).mkdirs();
							try (InputStream stream = con.getInputStream()) {
								Files.copy(stream, Paths.get("compilers/" + name + "/package.zip"));
							}
							this.unzip("compilers/" + name + "/package.zip", "compilers/");
							new File("compilers/" + name + "/package.zip").delete();
						} catch (final IOException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
					} else
						return;
				}
				if (MainFrame.this.currentFile == null)
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please save your work in order to compile.",
							"Eggs are supposed to be green!", JOptionPane.ERROR_MESSAGE);
				else {
					final String osString = OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS ? "macos"
							: OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows ? "windows" : "linux";
					final FileArrayProvider fap = new FileArrayProvider(); // FAP, huh... TODO: change name
					String[] lines = null;
					try {
						lines = fap.readLines("compilers/" + name + "/" + osString + "/options.txt");
					} catch (final IOException e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
						return;
					}
					final String[] command = this.concatAll(
							new String[] { "compilers/" + name + "/" + osString + "/" + compilername }, lines,
							new String[] { "\"" + MainFrame.this.currentFile.getAbsolutePath() + "\"" });
					System.out.println(command[0] + " " + command[1] + " " + command[2] + " " + command[3] + " ");
					final ProcessBuilder pb = new ProcessBuilder(command);
					try {
						pb.directory(new File(MainFrame.this.currentFile.getAbsoluteFile().getParent()));
					} catch (final Exception e1) {
						// I Don't care
					}
					try {
						final Process p = pb.start();
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "STDOUT:\n");
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
							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "\nSTDERR:\n");
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e) {
								final Crash dialog2 = new Crash(e);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}

							final CommandOutputDialog dialog3 = new CommandOutputDialog(
									MainFrame.this.toolConsole.getText());
							dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog3.setVisible(true);

							MainFrame.this.toolConsole.setText("");

							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}

			@SuppressWarnings("unchecked")
			public <T> T[] concatAll(final T[] first, final T[]... rest) {
				int totalLength = first.length;
				for (final T[] array : rest)
					totalLength += array.length;
				final T[] result = Arrays.copyOf(first, totalLength);
				int offset = first.length;
				for (final T[] array : rest) {
					System.arraycopy(array, 0, result, offset, array.length);
					offset += array.length;
				}
				return result;
			}

			private void extractFile(final ZipInputStream zipIn, final String filePath) throws IOException {
				final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				final byte[] bytesIn = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = zipIn.read(bytesIn)) != -1)
					bos.write(bytesIn, 0, read);
				bos.close();
			}

			public void unzip(final String zipFilePath, final String destDirectory) throws IOException {
				final File destDir = new File(destDirectory);
				if (!destDir.exists())
					destDir.mkdir();
				final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
				ZipEntry entry = zipIn.getNextEntry();
				// iterates over entries in the zip file
				while (entry != null) {
					final String filePath = destDirectory + File.separator + entry.getName();
					if (!entry.isDirectory())
						// if the entry is a file, extracts it
						this.extractFile(zipIn, filePath);
					else {
						// if the entry is a directory, make the directory
						final File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
			}
		});
		mnC_1.add(mntmTinycwindowsOnly);

		final JMenuItem mntmClang = new JMenuItem("Clang (Currently, Windows not supported)");
		mntmClang.addActionListener(new ActionListener() {
			private static final int BUFFER_SIZE = 4096;

			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final String name = "clang", compilername = "bin/clang";
				if (!new File("compilers\\" + name + "\\installed.dat").exists()) {
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please install " + name + " package.",
							"Error", JOptionPane.ERROR_MESSAGE);
					if (JOptionPane.showConfirmDialog(MainFrame.this.instance,
							"Do you want to download and install " + name
									+ " package?\n (By installing it you accept License provided with software)",
							"Package manager", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						URL url = null;
						try {
							url = new URL("https://raw.githubusercontent.com/KrzysztofSzewczyk/MEdit/master/packages/"
									+ name + ".zip");
						} catch (final MalformedURLException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
						try {
							final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
							new File("compilers/" + name).mkdirs();
							try (InputStream stream = con.getInputStream()) {
								Files.copy(stream, Paths.get("compilers/" + name + "/package.zip"));
							}
							this.unzip("compilers/" + name + "/package.zip", "compilers/");
							new File("compilers/" + name + "/package.zip").delete();
						} catch (final IOException e) {
							final Crash dialog = new Crash(e);
							dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
							return;
						}
					} else
						return;
				}
				if (MainFrame.this.currentFile == null)
					JOptionPane.showMessageDialog(MainFrame.this.instance, "Please save your work in order to compile.",
							"Eggs are supposed to be green!", JOptionPane.ERROR_MESSAGE);
				else {
					final String osString = OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS ? "macos"
							: OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows ? "windows" : "linux";
					final FileArrayProvider fap = new FileArrayProvider(); // FAP, huh... TODO: change name
					String[] lines = null;
					try {
						lines = fap.readLines("compilers/" + name + "/" + osString + "/options.txt");
					} catch (final IOException e2) {
						final Crash dialog = new Crash(e2);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
						return;
					}
					final String[] command = this.concatAll(
							new String[] { "compilers/" + name + "/" + osString + "/" + compilername }, lines,
							new String[] { "\"" + MainFrame.this.currentFile.getAbsolutePath() + "\"" });
					System.out.println(command[0] + " " + command[1] + " " + command[2] + " " + command[3] + " ");
					final ProcessBuilder pb = new ProcessBuilder(command);
					try {
						pb.directory(new File(MainFrame.this.currentFile.getAbsoluteFile().getParent()));
					} catch (final Exception e1) {
						// I Don't care
					}
					try {
						final Process p = pb.start();
						new Thread(() -> {
							final BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));

							final BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));

							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "STDOUT:\n");
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
							MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + "\nSTDERR:\n");
							try {
								while ((s = stdError.readLine()) != null)
									MainFrame.this.toolConsole.setText(MainFrame.this.toolConsole.getText() + s);
							} catch (final IOException e) {
								final Crash dialog2 = new Crash(e);
								dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog2.setVisible(true);
								return;
							}

							final CommandOutputDialog dialog3 = new CommandOutputDialog(
									MainFrame.this.toolConsole.getText());
							dialog3.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							dialog3.setVisible(true);

							MainFrame.this.toolConsole.setText("");

							return;
						}).start();
					} catch (final IOException e1) {
						final Crash dialog = new Crash(e1);
						dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						dialog.setVisible(true);
					}
				}
			}

			@SuppressWarnings("unchecked")
			public <T> T[] concatAll(final T[] first, final T[]... rest) {
				int totalLength = first.length;
				for (final T[] array : rest)
					totalLength += array.length;
				final T[] result = Arrays.copyOf(first, totalLength);
				int offset = first.length;
				for (final T[] array : rest) {
					System.arraycopy(array, 0, result, offset, array.length);
					offset += array.length;
				}
				return result;
			}

			private void extractFile(final ZipInputStream zipIn, final String filePath) throws IOException {
				final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
				final byte[] bytesIn = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = zipIn.read(bytesIn)) != -1)
					bos.write(bytesIn, 0, read);
				bos.close();
			}

			public void unzip(final String zipFilePath, final String destDirectory) throws IOException {
				final File destDir = new File(destDirectory);
				if (!destDir.exists())
					destDir.mkdir();
				final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
				ZipEntry entry = zipIn.getNextEntry();
				// iterates over entries in the zip file
				while (entry != null) {
					final String filePath = destDirectory + File.separator + entry.getName();
					if (!entry.isDirectory())
						// if the entry is a file, extracts it
						this.extractFile(zipIn, filePath);
					else {
						// if the entry is a directory, make the directory
						final File dir = new File(filePath);
						dir.mkdir();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
				zipIn.close();
			}
		});
		mnC_1.add(mntmClang);

		final JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);

		final JMenuItem mntmAbout = new JMenuItem("About MEdit");
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

		final JButton btnNewButton = new JButton("");
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
		btnNewButton.setToolTipText("Create new file");
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-new.png")));
		toolBar.add(btnNewButton);

		final JButton btnOpenButton = new JButton("");
		btnOpenButton.addActionListener(e -> {
			final JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(MainFrame.this.instance) != JFileChooser.APPROVE_OPTION)
				return;
			try {
				MainFrame.this.currentFile = chooser.getSelectedFile();
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
		btnOpenButton.setToolTipText("Open existing file");
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-open.png")));
		toolBar.add(btnOpenButton);

		final JButton btnSaveButton = new JButton("");
		btnSaveButton.addActionListener(e -> {
			if (MainFrame.this.currentFile == null) {
				final JFileChooser SaveAs = new JFileChooser();
				SaveAs.setApproveButtonText("Save");
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
		btnSaveButton.setToolTipText("Save file");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-save.png")));
		toolBar.add(btnSaveButton);

		final JButton btnCloseButton = new JButton("");
		btnCloseButton.addActionListener(e -> {
			if (MainFrame.instances == 0)
				return;
			MainFrame.this.instance.dispose();
		});
		btnCloseButton.setToolTipText("Close file");
		btnCloseButton.setFocusPainted(false);
		btnCloseButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/status/image-missing.png")));
		toolBar.add(btnCloseButton);

		final JButton btnCutButton = new JButton("");
		btnCutButton.addActionListener(e -> MainFrame.this.textPane.cut());
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);

		final JButton btnCopyButton = new JButton("");
		btnCopyButton.addActionListener(e -> MainFrame.this.textPane.copy());
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);

		final JButton btnPasteButton = new JButton("");
		btnPasteButton.addActionListener(e -> MainFrame.this.textPane.paste());
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setFocusPainted(false);
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);

		final JButton btnDeleteButton = new JButton("");
		btnDeleteButton.addActionListener(e -> MainFrame.this.textPane.replaceSelection(""));
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setFocusPainted(false);
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);

		final JButton btnUndoButton = new JButton("");
		btnUndoButton.addActionListener(e -> MainFrame.this.textPane.undoLastAction());
		btnUndoButton.setToolTipText("Undo");
		btnUndoButton.setFocusPainted(false);
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-undo.png")));
		toolBar.add(btnUndoButton);

		final JButton btnRedoButton = new JButton("");
		btnRedoButton.addActionListener(e -> MainFrame.this.textPane.redoLastAction());
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setFocusPainted(false);
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);

		final RTextScrollPane scrollPane = new RTextScrollPane();
		this.contentPane.add(scrollPane, BorderLayout.CENTER);

		this.textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(this.textPane);

		final JPanel panel = new JPanel();
		this.contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));

		final JPanel searchPanel = new JPanel();
		panel.add(searchPanel, BorderLayout.NORTH);

		final JLabel lblSearch = new JLabel("Search");
		searchPanel.add(lblSearch);

		this.searchTextField = new JTextField();
		searchPanel.add(this.searchTextField);
		this.searchTextField.setColumns(10);

		final JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		final JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);

		final JLabel lblReplace = new JLabel("Replace with");
		panel_2.add(lblReplace);

		this.replaceWithTextField = new JTextField();
		panel_2.add(this.replaceWithTextField);
		this.replaceWithTextField.setColumns(10);

		final JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));

		final JPanel panel_4 = new JPanel();
		panel_3.add(panel_4, BorderLayout.NORTH);

		final JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(e -> {
			final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
					MainFrame.this.textPane.getCaretPosition());
			final int l2 = MainFrame.this.searchTextField.getText().length();
			if (l1 == -1)
				JOptionPane.showMessageDialog(MainFrame.this.instance,
						"\"" + MainFrame.this.searchTextField.getText() + "\" not found");
			else
				MainFrame.this.textPane.select(l1, l2 + l1);
		});
		panel_4.add(btnSearch);

		final JButton btnReplace = new JButton("Replace");
		btnReplace.addActionListener(e -> {
			final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
					MainFrame.this.textPane.getCaretPosition());
			final int l2 = MainFrame.this.searchTextField.getText().length();
			if (l1 == -1)
				JOptionPane.showMessageDialog(MainFrame.this.instance,
						"\"" + MainFrame.this.searchTextField.getText() + "\" not found");
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

		final JButton btnCountOccurences =  new JButton("Count Occurences");
		btnCountOccurences.addActionListener(e -> {
			int amount = 0;
			while (true) {
				final int l1 = MainFrame.this.textPane.getText().indexOf(MainFrame.this.searchTextField.getText(),
						MainFrame.this.textPane.getCaretPosition());
				final int l2 = MainFrame.this.searchTextField.getText().length();
				if(l1 >= l2) break;
				if (l1 == -1)
					break;
				else {
					MainFrame.this.textPane.setCaretPosition(l1 + l2);
					amount++;
				}
			}
			JOptionPane.showMessageDialog(MainFrame.this.instance, "Found " + amount + " occurences.");
		});
		panel_6.add(btnCountOccurences);

		final JPanel panel_7 = new JPanel();
		panel_5.add(panel_7, BorderLayout.SOUTH);

		final JButton btnBlack = new JButton("Dark");
		btnBlack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_7.add(btnBlack);

		final JButton btnClassical = new JButton("Default");
		btnClassical.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
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

		final JButton btnNewButton_1 = new JButton("Extra Default");
		btnNewButton_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default-alt.xml"));
					theme.apply(MainFrame.this.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		panel_9.add(btnNewButton_1);

		final JButton btnMonokai = new JButton("Monokai");
		btnMonokai.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
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

		final JLabel lblTheme = new JLabel("Theme:");
		panel_11.add(lblTheme);

		final JPanel panel_12 = new JPanel();
		panel_10.add(panel_12, BorderLayout.CENTER);
		panel_12.setLayout(new BorderLayout(0, 0));

		this.toolConsole = new JTextPane();
		panel_12.add(this.toolConsole, BorderLayout.CENTER);
		this.toolConsole.setVisible(false);

		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				MainFrame.this.lblReady.setText("Ready | Length: " + MainFrame.this.textPane.getText().length()
						+ " | Filename: \""
						+ (MainFrame.this.currentFile == null ? "Unnamed"
								: MainFrame.this.currentFile.getAbsolutePath())
						+ "\" | Maximum size: "
						+ (MainFrame.this.currentFile == null ? "?" : MainFrame.this.currentFile.getFreeSpace() / 1024)
						+ "KB | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK) == true ? "NUM"
								: "NONUM")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_SCROLL_LOCK) == true ? "SCR"
								: "NOSCR")
						+ " | "
						+ (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK) == true ? "CAPS"
								: "NOCAPS"));
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
			final Theme theme = Theme
					.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
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
	}

}
