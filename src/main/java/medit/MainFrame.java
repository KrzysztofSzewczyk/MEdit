package medit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import medit.ActionManagers.EditActionManager;
import medit.ActionManagers.FileActionManager;
import medit.ActionManagers.LanguageActionManager;
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
	public JTextField replaceWithTextField;
	public JTextField searchTextField;
	public final RSyntaxTextArea textPane = new RSyntaxTextArea();
	public JTextPane toolConsole;

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
		final JMenu mnSyntaxHighlighting = new JMenu("Syntax Highlighting");
		menuBar.add(mnSyntaxHighlighting);
		final JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);
		
		/**
		 * Menu action managers setup
		 */
		WindowActionManager wam = new WindowActionManager(this);
		wam.Closing();
		FileActionManager fam = new FileActionManager(this);
		fam.New(mnFile);
		fam.Open(mnFile);
		fam.Save(mnFile);
		fam.ReloadFromDisk(mnFile);
		fam.OpenDir(mnFile);
		fam.SaveAs(mnFile);
		fam.Separator(mnFile);
		fam.Exit(mnFile);
		EditActionManager eam = new EditActionManager(this);
		eam.Cut(mnEdit);
		eam.Copy(mnEdit);
		eam.Paste(mnEdit);
		eam.Delete(mnEdit);
		eam.Separator(mnEdit);
		eam.Undo(mnEdit);
		eam.Redo(mnEdit);
		AboutActionManager aam = new AboutActionManager();
		aam.About(mnAbout);
		LanguageActionManager lam = new LanguageActionManager(this);
		lam.SetUp(mnSyntaxHighlighting);
		
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

		/**
		 * Search box setup. Dirty at the moment, TODO: Clean up.
		 */
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

		/**
		 * Editor themes. TODO: Clean up.
		 */
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

		/**
		 * Misc tasks, TODO: Clean up
		 */
		
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
