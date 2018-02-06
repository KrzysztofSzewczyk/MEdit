package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import medit.MainFrame;

/**
 * This is propably best organized actionmanager in the whole project. It's task
 * is to create many menu items (>100) to supply very big amount of languages to
 * support (~92).
 *
 * @author Krzysztof Szewczyk
 *
 */

public class LanguageActionManager {

	/**
	 * This is MainFrame class instance used by this project.
	 */
	private final MainFrame instance;

	/**
	 * This is constructor of LanguageActionManager assigning passed instance to
	 * internal instance.
	 *
	 * @param instance
	 */

	public LanguageActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Set up syntax highlighting menu contents.
	 *
	 * @param parent
	 * @param ccam
	 */

	public void SetUp(final JMenu parent, final CodeCompletionActionManager ccam) {
		/**
		 * First, let's create every language group and separate JMenuItem for each.
		 */

		final JMenuItem mntmNo = new JMenuItem("No");
		final JMenu mnA = new JMenu("A");
		final JMenuItem mntmActionscript = new JMenuItem("ActionScript");
		final JMenuItem mntmAssembler = new JMenuItem("Assembly"); // Like this one
		final JMenu mnB = new JMenu("B");
		final JMenuItem mntmBbcode = new JMenuItem("BBCode");
		final JMenuItem mntmBpp = new JMenuItem("B++ (MEdit scripting language)");
		final JMenu mnC = new JMenu("C");
		final JMenuItem mntmC = new JMenuItem("C");
		final JMenuItem mntmC_1 = new JMenuItem("C++");
		final JMenuItem mntmC_2 = new JMenuItem("C#");
		final JMenuItem mntmCSS = new JMenuItem("CSS");
		final JMenuItem mntmClojure = new JMenuItem("Clojure");
		final JMenu mnD = new JMenu("D");
		final JMenuItem mntmDart = new JMenuItem("Dart");
		final JMenuItem mntmDelphi = new JMenuItem("Delphi");
		final JMenuItem mntmDocker = new JMenuItem("Docker");
		final JMenuItem mntmDtd = new JMenuItem("DTD");
		final JMenuItem mntmD = new JMenuItem("D");
		final JMenuItem mntmFortan = new JMenuItem("Fortan");
		final JMenuItem mntmGroovy = new JMenuItem("Groovy");
		final JMenu mnH = new JMenu("H");
		final JMenuItem mntmHtaccess = new JMenuItem("HTAccess");
		final JMenuItem mntmHosts = new JMenuItem("Hosts");
		final JMenuItem mntmHtml = new JMenuItem("HTML");
		final JMenuItem mntmIni = new JMenuItem("INI");
		final JMenu mnJ = new JMenu("J");
		final JMenuItem mntmJavascript = new JMenuItem("JavaScript");
		final JMenuItem mntmJava = new JMenuItem("Java");
		final JMenuItem mntmJshintrc = new JMenuItem("JSON with C++ style comments");
		final JMenuItem mntmJson = new JMenuItem("JSON");
		final JMenuItem mntmJsp = new JMenuItem("JSP");
		final JMenu mnL = new JMenu("L");
		final JMenuItem mntmLatex = new JMenuItem("Latex");
		final JMenuItem mntmLess = new JMenuItem("Less");
		final JMenuItem mntmLisp = new JMenuItem("Lisp");
		final JMenuItem mntmLua = new JMenuItem("Lua");
		final JMenu mnM = new JMenu("M");
		final JMenuItem mntmMakeFile = new JMenuItem("Makefile");
		final JMenuItem mntmMxml = new JMenuItem("MXML");
		final JMenuItem mntmNsis = new JMenuItem("NSIS");
		final JMenu mnP = new JMenu("P");
		final JMenuItem mntmPerl = new JMenuItem("Perl");
		final JMenuItem mntmPropertiesFile = new JMenuItem("Properties File");
		final JMenuItem mntmPHP = new JMenuItem("PHP");
		final JMenuItem mntmPython = new JMenuItem("Python"); // Hate this one
		final JMenu mnR = new JMenu("R");
		final JMenuItem mntmRuby = new JMenuItem("Ruby");
		final JMenu mnS = new JMenu("S");
		final JMenuItem mntmSas = new JMenuItem("SAS");
		final JMenuItem mntmSacala = new JMenuItem("Scala");
		final JMenuItem mntmSql = new JMenuItem("SQL");
		final JMenu mnT = new JMenu("T");
		final JMenuItem mntmTcl = new JMenuItem("TCL");
		final JMenuItem mntmTypescript = new JMenuItem("TypeScript");
		final JMenuItem mntmUnixShell = new JMenuItem("Unix Shell");
		final JMenuItem mntmVisualBasic = new JMenuItem("Visual Basic");
		final JMenuItem mntmWindowsBatch = new JMenuItem("Windows Batch");
		final JMenuItem mntmXml = new JMenuItem("XML");
		final JMenuItem mntmYaml = new JMenuItem("YAML");

		/**
		 * Now, let's add groups and separate ungrouped languages.
		 */

		parent.add(mntmNo);
		parent.add(mnA);
		parent.add(mnB);
		parent.add(mnC);
		parent.add(mnD);
		parent.add(mntmFortan);
		parent.add(mntmGroovy);
		parent.add(mnH);
		parent.add(mntmIni);
		parent.add(mnJ);
		parent.add(mnL);
		parent.add(mnM);
		parent.add(mntmNsis);
		parent.add(mnP);
		parent.add(mnR);
		parent.add(mnS);
		parent.add(mnT);
		parent.add(mntmUnixShell);
		parent.add(mntmVisualBasic);
		parent.add(mntmWindowsBatch);
		parent.add(mntmXml);
		parent.add(mntmYaml);

		/**
		 * Now, let's set up action listeners for each of buttons, to change syntax
		 * highlighting style for MainFrame' textPane component.
		 */

		mntmNo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_NONE);
			}
		});
		mntmAssembler.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
			}
		});
		mntmActionscript.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT);
			}
		});
		mntmBbcode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BBCODE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_BBCODE);
			}
		});
		mntmBpp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_BPP);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_BPP);
			}
		});
		mntmClojure.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CLOJURE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_CLOJURE);
			}
		});
		mntmC_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_CSHARP);
			}
		});
		mntmC_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
			}
		});
		mntmCSS.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_CSS);
			}
		});
		mntmC.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_C);
			}
		});
		mntmD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_D);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_D);
			}
		});
		mntmDtd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DTD);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_DTD);
			}
		});
		mntmDocker.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE);
			}
		});
		mntmDelphi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DELPHI);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_DELPHI);
			}
		});
		mntmDart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DART);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_DART);
			}
		});
		mntmFortan.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_FORTRAN);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_FORTRAN);
			}
		});
		mntmGroovy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_GROOVY);
			}
		});
		mntmHtaccess.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTACCESS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_HTACCESS);
			}
		});
		mntmHtml.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_HTML);
			}
		});
		mntmHosts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HOSTS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_HOSTS);
			}
		});
		mntmIni.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_INI);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_INI);
			}
		});
		mntmJsp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSP);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_JSP);
			}
		});
		mntmJshintrc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
			}
		});
		mntmJson.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_JSON);
			}
		});
		mntmJava.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_JAVA);
			}
		});
		mntmJavascript.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
			}
		});
		mntmLua.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_LUA);
			}
		});
		mntmLisp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LISP);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_LISP);
			}
		});
		mntmLess.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LESS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_LESS);
			}
		});
		mntmLatex.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LATEX);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_LATEX);
			}
		});
		mntmMxml.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MXML);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_MXML);
			}
		});
		mntmMakeFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MAKEFILE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_MAKEFILE);
			}
		});
		mntmNsis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NSIS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_NSIS);
			}
		});
		mntmPropertiesFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
			}
		});
		mntmPHP.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PHP);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_PHP);
			}
		});
		mntmPython.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_PYTHON);
			}
		});
		mntmPerl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PERL);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_PERL);
			}
		});
		mntmRuby.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_RUBY);
			}
		});
		mntmSas.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SAS);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_SAS);
			}
		});
		mntmSacala.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_SCALA);
			}
		});
		mntmSql.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_SQL);
			}
		});
		mntmTcl.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TCL);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_TCL);
			}
		});
		mntmTypescript.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
			}
		});
		mntmUnixShell.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
			}
		});
		mntmVisualBasic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC);
			}
		});
		mntmWindowsBatch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane
						.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
			}
		});
		mntmXml.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_XML);
			}
		});
		mntmYaml.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LanguageActionManager.this.instance.textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
				ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_YAML);
			}
		});

		/**
		 * This part is adding grouped languages to it's parents.
		 */

		mnA.add(mntmActionscript);
		mnA.add(mntmAssembler);
		mnB.add(mntmBpp);
		mnB.add(mntmBbcode);
		mnC.add(mntmC);
		mnC.add(mntmC_1);
		mnC.add(mntmC_2);
		mnC.add(mntmCSS);
		mnC.add(mntmClojure);
		mnD.add(mntmDart);
		mnD.add(mntmDelphi);
		mnD.add(mntmDocker);
		mnD.add(mntmDtd);
		mnD.add(mntmD);
		mnH.add(mntmHtaccess);
		mnH.add(mntmHosts);
		mnH.add(mntmHtml);
		mnJ.add(mntmJavascript);
		mnJ.add(mntmJava);
		mnJ.add(mntmJson);
		mnJ.add(mntmJshintrc);
		mnJ.add(mntmJsp);
		mnL.add(mntmLatex);
		mnL.add(mntmLess);
		mnL.add(mntmLisp);
		mnL.add(mntmLua);
		mnM.add(mntmMakeFile);
		mnM.add(mntmMxml);
		mnP.add(mntmPerl);
		mnP.add(mntmPropertiesFile);
		mnP.add(mntmPython);
		mnR.add(mntmRuby);
		mnS.add(mntmSas);
		mnS.add(mntmSacala);
		mnS.add(mntmSql);
		mnT.add(mntmTcl);
		mnT.add(mntmTypescript);
	}
}
