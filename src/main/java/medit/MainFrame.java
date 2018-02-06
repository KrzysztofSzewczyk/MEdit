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
	public File currentFile = null;
	public MainFrame instance;
	public final JLabel lblReady = new JLabel(
			"Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
	public final RSyntaxTextArea textPane = new RSyntaxTextArea();
	
	public final JMenu mnFile = new JMenu("File");
	public final JMenu mnEdit = new JMenu("Edit");
	public final JMenu mnLanguage = new JMenu("Language");
	public final JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");
	public final JMenu mnThemes = new JMenu("Themes");
	public final JMenu mnTools = new JMenu("Tools");
	public final JMenu mnScripts = new JMenu("Scripts");
	public final JMenu mnAbout = new JMenu("About");
	public final JMenu mnTextOperations = new JMenu("Text Operations");
	
	public CollapsibleSectionPanel csp;
	public FindDialog findDialog;
	public ReplaceDialog replaceDialog;
	public FindToolBar findToolBar;
	public ReplaceToolBar replaceToolBar;
	
	private int LoadValue = 1;
	private String[] sLoadValues = {
			"Loading classes",
			"Menubar setup",
			"Menu items setup",
			"Window AM setup",
			"File AM setup",
			"Text OP AM setup",
			"About AM setup",
			"Code Completion AM setup",
			"Language AM setup",
			"Themes AM setup",
			"Timer task AM setup",
			"Bottombar setup",
			"Tool AM setup",
			"Script AM setup",
			"Language AM setup",
			"Toolbar setup",
			"Editor look setup",
			"Editor functionality setup",
			"Gutter setup",
			"Default theme setup",
			"Starting editor",
			"Done loading."
	};
	private final JPanel panel = new JPanel();

	/**
	 * Create the frame.
	 * @param instance2 
	 */
	public MainFrame(SplashScreen splashScreen) {

		/**
		 * SplashScreen state updater.
		 */
		
		final Timer gctimer = new Timer();
		gctimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(splashScreen==null) return;
				splashScreen.progressBar.setValue(LoadValue);
				splashScreen.progressBar.setString("Current Task (Done): " + sLoadValues [LoadValue] + " (" + LoadValue + "/" + sLoadValues.length + ")");
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
		LoadValue++;
		/**
		 * Menu bar Setup
		 */
		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);
		LoadValue++;
		/**
		 * Menus setup
		 */
		menuBar.add(mnFile);
		menuBar.add(mnEdit);
		menuBar.add(mnLanguage);
		menuBar.add(mnSyntaxHighlighting);
		menuBar.add(mnThemes);
		menuBar.add(mnTools);
		menuBar.add(mnScripts);
		menuBar.add(mnAbout);
		menuBar.add(mnTextOperations);
		LoadValue++;
		/**
		 * Menu action managers setup.
		 */

		final WindowActionManager wam = new WindowActionManager(this);
		wam.Closing();
		LoadValue++;
		final FileActionManager fam = new FileActionManager(this);
		fam.New(mnFile);
		fam.Open(mnFile);
		fam.Save(mnFile);
		fam.SaveAs(mnFile);
		fam.Print(mnFile);
		fam.Separator(mnFile);
		fam.ReloadFromDisk(mnFile);
		fam.OpenDir(mnFile);
		fam.RemoveFromDisk(mnFile);
		fam.Separator(mnFile);
		fam.Exit(mnFile);
		LoadValue++;
		final EditActionManager eam = new EditActionManager(this);
		eam.Cut(mnEdit);
		eam.Copy(mnEdit);
		eam.Paste(mnEdit);
		eam.Delete(mnEdit);
		eam.Separator(mnEdit);
		eam.Undo(mnEdit);
		eam.Redo(mnEdit);
		eam.Separator(mnEdit);
		eam.Search(mnEdit);
		LoadValue++;
		final TextOPActionManager topam = new TextOPActionManager(this);
		topam.SetupTextOP(mnTextOperations);
		LoadValue++;
		final AboutActionManager aam = new AboutActionManager();
		aam.About(mnAbout);
		final CodeCompletionActionManager ccam = new CodeCompletionActionManager(this);
		ccam.SetUpCodeCompletion(SyntaxConstants.SYNTAX_STYLE_NONE);
		LoadValue++;
		final LanguageActionManager lam = new LanguageActionManager(this);
		lam.SetUp(mnSyntaxHighlighting, ccam);
		LoadValue++;
		final ThemesActionManager tam = new ThemesActionManager(this);
		tam.RegisterThemes(mnThemes);
		LoadValue++;
		final TimerTaskActionManager ttam = new TimerTaskActionManager(this);
		ttam.SetUpTimers();
		LoadValue++;
		final BottombarActionManager bbam = new BottombarActionManager(this);
		bbam.SetUpBottombar();
		LoadValue++;
		final ToolActionManager toolam = new ToolActionManager(this);
		toolam.SetupTools(mnTools);
		LoadValue++;
		final ScriptsActionManager sam = new ScriptsActionManager(this);
		sam.SetupScripts(mnScripts);
		LoadValue++;
		final JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem("English");
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);
		LoadValue++;

		/**
		 * Editor setup
		 */
		final RTextScrollPane scrollPane = new RTextScrollPane();
		this.contentPane.add(scrollPane, BorderLayout.CENTER);

		this.textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(this.textPane);
		LoadValue++;
		
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
		LoadValue++;
		
		scrollPane.setIconRowHeaderEnabled(true);
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);
		LoadValue++;
		
		ErrorStrip errorStrip = new ErrorStrip(textPane);
		contentPane.add(errorStrip, BorderLayout.LINE_END);
		
		findDialog = new FindDialog(this, this);
		replaceDialog = new ReplaceDialog(this, this);

		// This ties the properties of the two dialogs together (match case,
		// regex, etc.).
		SearchContext context = findDialog.getSearchContext();
		replaceDialog.setSearchContext(context);

		// Create tool bars and tie their search contexts together also.
		findToolBar = new FindToolBar(this);
		findToolBar.setSearchContext(context);
		replaceToolBar = new ReplaceToolBar(this);
		replaceToolBar.setSearchContext(context);
		
		try {
			final Theme theme = Theme
					.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
			theme.apply(this.textPane);
			
			contentPane.add(panel, BorderLayout.NORTH);
			panel.setLayout(new BorderLayout(0, 0));
			/**
			 * Toolbar setup.
			 */
			final JToolBar toolBar = new JToolBar();
			panel.add(toolBar, BorderLayout.WEST);
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
		LoadValue++;
		
		setVisible(true);
		gctimer.cancel();
	}

	@Override
	public void searchEvent(SearchEvent e) {
		SearchEvent.Type type = e.getType();
		SearchContext context = e.getSearchContext();
		SearchResult result = null;

		switch (type) {
			default: // Prevent FindBugs warning later
			case MARK_ALL:
				result = SearchEngine.markAll(textPane, context);
				break;
			case FIND:
				result = SearchEngine.find(textPane, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(textPane);
				}
				break;
			case REPLACE:
				result = SearchEngine.replace(textPane, context);
				if (!result.wasFound()) {
					UIManager.getLookAndFeel().provideErrorFeedback(textPane);
				}
				break;
			case REPLACE_ALL:
				result = SearchEngine.replaceAll(textPane, context);
				JOptionPane.showMessageDialog(this, result.getCount() +
						" occurrences replaced.");
				break;
		}

		String text = null;
		if (result.wasFound()) {
			text = "Text found; occurrences marked: " + result.getMarkedCount();
		}
		else if (type==SearchEvent.Type.MARK_ALL) {
			if (result.getMarkedCount()>0) {
				text = "Occurrences marked: " + result.getMarkedCount();
			}
			else {
				text = "";
			}
		}
		else {
			text = "Text not found";
		}
		JOptionPane.showMessageDialog(this, text);
	}

	@Override
	public String getSelectedText() {
		// TODO Auto-generated method stub
		return null;
	}

}
