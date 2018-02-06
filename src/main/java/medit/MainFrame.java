package medit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import medit.ActionManagers.AboutActionManager;
import medit.ActionManagers.BottombarActionManager;
import medit.ActionManagers.CodeCompletionActionManager;
import medit.ActionManagers.EditActionManager;
import medit.ActionManagers.FileActionManager;
import medit.ActionManagers.LanguageActionManager;
import medit.ActionManagers.ScriptsActionManager;
import medit.ActionManagers.TextOPActionManager;
import medit.ActionManagers.ThemesActionManager;
import medit.ActionManagers.TimerTaskActionManager;
import medit.ActionManagers.ToolActionManager;
import medit.ActionManagers.WindowActionManager;

/**
 * Main frame for MEdit project. That's where the whole magic is done. It was
 * split to many files, which are located in ActionManagers.
 *
 * @author Krzysztof Szewczyk
 */

public class MainFrame extends JFrame implements SearchListener {

	/**
	 * Many public variables, that were privatized before. They are public, because
	 * our MainFrame is not standalone class now and it references many
	 * ActionManagers.
	 */

	public static int instances = 1;
	public static final long serialVersionUID = 1L;
	public JPanel contentPane;
	public CollapsibleSectionPanel csp;
	public File currentFile = null;
	public FindDialog findDialog;
	public FindToolBar findToolBar;

	public MainFrame instance;
	public final JLabel lblReady = new JLabel(
			"Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
	private int LoadValue = 1;
	public final JMenu mnAbout = new JMenu("About");
	public final JMenu mnEdit = new JMenu("Edit");
	public final JMenu mnFile = new JMenu("File");
	public final JMenu mnLanguage = new JMenu("Language");
	public final JMenu mnScripts = new JMenu("Scripts");
	public final JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");

	public final JMenu mnTextOperations = new JMenu("Text Operations");
	public final JMenu mnThemes = new JMenu("Themes");
	public final JMenu mnTools = new JMenu("Tools");
	private final JPanel panel = new JPanel();
	public ReplaceDialog replaceDialog;

	public ReplaceToolBar replaceToolBar;
	private final String[] sLoadValues = { "Loading classes", "Menubar setup", "Menu items setup", "Window AM setup",
			"File AM setup", "Text OP AM setup", "About AM setup", "Code Completion AM setup", "Language AM setup",
			"Themes AM setup", "Timer task AM setup", "Bottombar setup", "Tool AM setup", "Script AM setup",
			"Language AM setup", "Toolbar setup", "Editor look setup", "Editor functionality setup", "Gutter setup",
			"Default theme setup", "Starting editor", "Done loading." };
	public final RSyntaxTextArea textPane = new RSyntaxTextArea();

	/**
	 * Create the frame.
	 *
	 * @param instance2
	 */
	public MainFrame(final SplashScreen splashScreen) {

		/**
		 * SplashScreen state updater.
		 */

		final Timer gctimer = new Timer();
		gctimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (splashScreen == null)
					return;
				splashScreen.progressBar.setValue(MainFrame.this.LoadValue);
				splashScreen.progressBar
						.setString("Current Task (Done): " + MainFrame.this.sLoadValues[MainFrame.this.LoadValue] + " ("
								+ MainFrame.this.LoadValue + "/" + MainFrame.this.sLoadValues.length + ")");
			}
		}, 0, 1);

		/**
		 * Frame setup
		 */
		this.instance = this;
		this.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(MainFrame.class.getResource("/medit/assets/apps/accessories-text-editor.png")));
		this.setTitle("MEdit");
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBounds(100, 100, 700, 500);
		this.setMinimumSize(new Dimension(700, 500));
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(this.contentPane);
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.LoadValue++;
		/**
		 * Menu bar Setup
		 */
		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);
		this.LoadValue++;
		/**
		 * Menus setup
		 */
		menuBar.add(this.mnFile);
		menuBar.add(this.mnEdit);
		menuBar.add(this.mnLanguage);
		menuBar.add(this.mnSyntaxHighlighting);
		menuBar.add(this.mnThemes);
		menuBar.add(this.mnTools);
		menuBar.add(this.mnScripts);
		menuBar.add(this.mnAbout);
		menuBar.add(this.mnTextOperations);
		this.LoadValue++;
		/**
		 * Menu action managers setup.
		 */

		final WindowActionManager wam = new WindowActionManager(this);
		wam.Closing();
		this.LoadValue++;
		final FileActionManager fam = new FileActionManager(this);
		fam.New(this.mnFile);
		fam.Open(this.mnFile);
		fam.Save(this.mnFile);
		fam.SaveAs(this.mnFile);
		fam.Print(this.mnFile);
		fam.Separator(this.mnFile);
		fam.ReloadFromDisk(this.mnFile);
		fam.OpenDir(this.mnFile);
		fam.RemoveFromDisk(this.mnFile);
		fam.Separator(this.mnFile);
		fam.Exit(this.mnFile);
		this.LoadValue++;
		final EditActionManager eam = new EditActionManager(this);
		eam.Cut(this.mnEdit);
		eam.Copy(this.mnEdit);
		eam.Paste(this.mnEdit);
		eam.Delete(this.mnEdit);
		eam.Separator(this.mnEdit);
		eam.Undo(this.mnEdit);
		eam.Redo(this.mnEdit);
		eam.Separator(this.mnEdit);
		eam.Search(this.mnEdit);
		this.LoadValue++;
		final TextOPActionManager topam = new TextOPActionManager(this);
		topam.SetupTextOP(this.mnTextOperations);
		this.LoadValue++;
		final AboutActionManager aam = new AboutActionManager();
		aam.About(this.mnAbout);
		final CodeCompletionActionManager ccam = new CodeCompletionActionManager(this);
		ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_NONE);
		this.LoadValue++;
		final LanguageActionManager lam = new LanguageActionManager(this);
		lam.SetUp(this.mnSyntaxHighlighting, ccam);
		this.LoadValue++;
		final ThemesActionManager tam = new ThemesActionManager(this);
		tam.RegisterThemes(this.mnThemes);
		this.LoadValue++;
		final TimerTaskActionManager ttam = new TimerTaskActionManager(this);
		ttam.SetUpTimers();
		this.LoadValue++;
		final BottombarActionManager bbam = new BottombarActionManager(this);
		bbam.SetUpBottombar();
		this.LoadValue++;
		final ToolActionManager toolam = new ToolActionManager(this);
		toolam.SetupTools(this.mnTools);
		this.LoadValue++;
		final ScriptsActionManager sam = new ScriptsActionManager(this);
		sam.SetupScripts(this.mnScripts);
		this.LoadValue++;
		final JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem("English");
		rdbtnmntmEnglish.setSelected(true);
		this.mnLanguage.add(rdbtnmntmEnglish);
		this.LoadValue++;

		/**
		 * Editor setup
		 */
		final RTextScrollPane scrollPane = new RTextScrollPane();
		this.contentPane.add(scrollPane, BorderLayout.CENTER);

		this.textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(this.textPane);
		this.LoadValue++;

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
		this.LoadValue++;

		scrollPane.setIconRowHeaderEnabled(true);
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);
		this.LoadValue++;

		final ErrorStrip errorStrip = new ErrorStrip(this.textPane);
		this.contentPane.add(errorStrip, BorderLayout.LINE_END);

		this.findDialog = new FindDialog(this, this);
		this.replaceDialog = new ReplaceDialog(this, this);

		final SearchContext context = this.findDialog.getSearchContext();
		this.replaceDialog.setSearchContext(context);
		this.findToolBar = new FindToolBar(this);
		this.findToolBar.setSearchContext(context);
		this.replaceToolBar = new ReplaceToolBar(this);
		this.replaceToolBar.setSearchContext(context);

		try {
			final Theme theme = Theme
					.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
			theme.apply(this.textPane);

			this.contentPane.add(this.panel, BorderLayout.NORTH);
			this.panel.setLayout(new BorderLayout(0, 0));
			/**
			 * Toolbar setup.
			 */
			final JToolBar toolBar = new JToolBar();
			this.panel.add(toolBar, BorderLayout.WEST);
			toolBar.setFloatable(false);

			fam.New(toolBar);
			fam.Open(toolBar);
			fam.Save(toolBar);
			fam.Exit(toolBar);

			eam.Cut(toolBar);
			eam.Copy(toolBar);
			eam.Paste(toolBar);
			eam.Delete(toolBar);
			eam.Undo(toolBar);
			eam.Redo(toolBar);
		} catch (final IOException ioe) { // Never happens
			final Crash dialog = new Crash(ioe);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		this.LoadValue++;

		this.setVisible(true);
		gctimer.cancel();
	}

	@Override
	public String getSelectedText() {
		return null;
	}

	@Override
	public void searchEvent(final SearchEvent e) {
		final SearchEvent.Type type = e.getType();
		final SearchContext context = e.getSearchContext();
		SearchResult result = null;

		switch (type) {
		default:
		case MARK_ALL:
			result = SearchEngine.markAll(this.textPane, context);
			break;
		case FIND:
			result = SearchEngine.find(this.textPane, context);
			if (!result.wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(this.textPane);
			break;
		case REPLACE:
			result = SearchEngine.replace(this.textPane, context);
			if (!result.wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(this.textPane);
			break;
		case REPLACE_ALL:
			result = SearchEngine.replaceAll(this.textPane, context);
			JOptionPane.showMessageDialog(this, result.getCount() + " occurrences replaced.");
			break;
		}

		String text = null;
		if (result.wasFound())
			text = "Text found; occurrences marked: " + result.getMarkedCount();
		else if (type == SearchEvent.Type.MARK_ALL) {
			if (result.getMarkedCount() > 0)
				text = "Occurrences marked: " + result.getMarkedCount();
			else
				text = "";
		} else
			text = "Text not found";
		JOptionPane.showMessageDialog(this, text);
	}

}
