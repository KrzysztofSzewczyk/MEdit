package medit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import medit.ActionManagers.AboutActionManager;
import medit.ActionManagers.BottombarActionManager;
import medit.ActionManagers.EditActionManager;
import medit.ActionManagers.FileActionManager;
import medit.ActionManagers.LanguageActionManager;
import medit.ActionManagers.ThemesActionManager;
import medit.ActionManagers.TimerTaskActionManager;
import medit.ActionManagers.ToolActionManager;
import medit.ActionManagers.WindowActionManager;

/**
 * Main frame for MEdit project.
 *
 * @author Krzysztof Szewczyk
 */

public class MainFrame extends JFrame {

	public static int instances = 1;
	public static final long serialVersionUID = 1L;
	public JPanel contentPane;
	public File currentFile = null;
	public MainFrame instance;
	public final JLabel lblReady = new JLabel(
			"Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
	public final RSyntaxTextArea textPane = new RSyntaxTextArea();

	/**
	 * Create the frame.
	 */
	public MainFrame() {

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

		/**
		 * Menu bar Setup
		 */
		final JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		/**
		 * Menus setup
		 */
		final JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		final JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		final JMenu mnLanguage = new JMenu("Language");
		menuBar.add(mnLanguage);

		final JMenu mnTextOperations = new JMenu("Text Operations");
		menuBar.add(mnTextOperations);
		final JMenu mnCase = new JMenu("Case");
		mnTextOperations.add(mnCase);
		final JMenuItem mntmThisWay = new JMenuItem("THIS WAY");
		mntmThisWay.addActionListener(e -> new Thread(() -> {
			try {
				MainFrame.this.textPane.replaceSelection(MainFrame.this.textPane.getSelectedText().toUpperCase());
			} catch (final Exception e2) {
			}
		}).start());
		mnCase.add(mntmThisWay);
		final JMenuItem mntmThisWay_1 = new JMenuItem("this way");
		mntmThisWay_1.addActionListener(e -> new Thread(() -> {
			try {
				MainFrame.this.textPane.replaceSelection(MainFrame.this.textPane.getSelectedText().toLowerCase());
			} catch (final Exception e2) {
			}
		}).start());
		mnCase.add(mntmThisWay_1);
		final JMenuItem mntmThisWay_2 = new JMenuItem("This Way");
		mntmThisWay_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				new Thread(() -> {
					try {
						final String text = MainFrame.this.textPane.getSelectedText();
						MainFrame.this.textPane.replaceSelection(this.toTitleCase(text));
					} catch (final Exception e3) {
					}
				}).start();
			}

			public String toTitleCase(final String givenString) {
				final String[] arr = givenString.split(" ");
				final StringBuffer sb = new StringBuffer();

				for (final String element : arr)
					sb.append(Character.toUpperCase(element.charAt(0))).append(element.substring(1)).append(" ");
				return sb.toString().trim();
			}
		});
		mnCase.add(mntmThisWay_2);
		final JMenuItem mntmThisWay_4 = new JMenuItem("ThIs WaY");
		mntmThisWay_4.addActionListener(e -> new Thread(() -> {
			try {
				final char[] text = MainFrame.this.textPane.getSelectedText().toCharArray();
				for (int i = 0; i < text.length; i++)
					if (i == text.length % 2)
						text[i] = Character.toUpperCase(text[i]);
					else
						text[i] = Character.toLowerCase(text[i]);
				MainFrame.this.textPane.replaceSelection(new String(text));
			} catch (final Exception e3) {
			}
		}).start());
		mnCase.add(mntmThisWay_4);
		final JMenuItem mntmRandom = new JMenuItem("RanDOm");
		mntmRandom.addActionListener(e -> new Thread(() -> {
			try {
				final char[] text = MainFrame.this.textPane.getSelectedText().toCharArray();
				for (int i = 0; i < text.length; i++)
					if (new Random().nextInt(3) == 1)
						text[i] = Character.toUpperCase(text[i]);
					else
						text[i] = Character.toLowerCase(text[i]);
				MainFrame.this.textPane.replaceSelection(new String(text));
			} catch (final Exception e3) {
			}
		}).start());
		mnCase.add(mntmRandom);
		final JMenu mnRow = new JMenu("Row");
		mnTextOperations.add(mnRow);
		final JMenuItem mntmGoToRow = new JMenuItem("Go to row ...");
		mntmGoToRow.addActionListener(e -> {
			final GoToLine gtlDlg = new GoToLine(MainFrame.this.instance);
			gtlDlg.setVisible(true);
			gtlDlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		});
		mnRow.add(mntmGoToRow);
		final JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");
		menuBar.add(mnSyntaxHighlighting);
		final JMenu mnThemes = new JMenu("Themes");
		menuBar.add(mnThemes);
		final JMenu mnTools = new JMenu("Tools");
		menuBar.add(mnTools);
		final JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);

		/**
		 * Menu action managers setup
		 */

		final WindowActionManager wam = new WindowActionManager(this);
		wam.Closing();

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

		final AboutActionManager aam = new AboutActionManager();
		aam.About(mnAbout);

		final LanguageActionManager lam = new LanguageActionManager(this);
		lam.SetUp(mnSyntaxHighlighting);

		final ThemesActionManager tam = new ThemesActionManager(this);
		tam.RegisterThemes(mnThemes);

		final TimerTaskActionManager ttam = new TimerTaskActionManager(this);
		ttam.SetUpTimers();

		final BottombarActionManager bbam = new BottombarActionManager(this);
		bbam.SetUpBottombar();

		final ToolActionManager toolam = new ToolActionManager(this);
		toolam.SetupTools(mnTools);

		/**
		 * Language submenu setup
		 */
		final JRadioButtonMenuItem rdbtnmntmEnglish = new JRadioButtonMenuItem("English");
		rdbtnmntmEnglish.setSelected(true);
		mnLanguage.add(rdbtnmntmEnglish);

		/**
		 * Toolbar setup.
		 */
		final JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		this.contentPane.add(toolBar, BorderLayout.NORTH);

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

		/**
		 * Editor setup
		 */
		final RTextScrollPane scrollPane = new RTextScrollPane();
		this.contentPane.add(scrollPane, BorderLayout.CENTER);

		this.textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(this.textPane);

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

	}

}
