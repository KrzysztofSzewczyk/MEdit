package org.fife.rsta.ui.demo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.BadLocationException;

import org.fife.rsta.ui.CollapsibleSectionPanel;
//import org.fife.rsta.ui.DocumentMap;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.SizeGripIcon;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

/**
 * An application that demonstrates use of the RSTAUI project. Please don't take
 * this as good application design; it's just a simple example.
 * <p>
 *
 * Unlike the library itself, this class is public domain.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RSTAUIDemoApp extends JFrame implements SearchListener {

	private class GoToLineAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public GoToLineAction() {
			super("Go To Line...");
			final int c = RSTAUIDemoApp.this.getToolkit().getMenuShortcutKeyMask();
			this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, c));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (RSTAUIDemoApp.this.findDialog.isVisible())
				RSTAUIDemoApp.this.findDialog.setVisible(false);
			if (RSTAUIDemoApp.this.replaceDialog.isVisible())
				RSTAUIDemoApp.this.replaceDialog.setVisible(false);
			final GoToDialog dialog = new GoToDialog(RSTAUIDemoApp.this);
			dialog.setMaxLineNumberAllowed(RSTAUIDemoApp.this.textArea.getLineCount());
			dialog.setVisible(true);
			final int line = dialog.getLineNumber();
			if (line > 0)
				try {
					RSTAUIDemoApp.this.textArea
							.setCaretPosition(RSTAUIDemoApp.this.textArea.getLineStartOffset(line - 1));
				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(RSTAUIDemoApp.this.textArea);
					ble.printStackTrace();
				}
		}

	}

	private class LookAndFeelAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final LookAndFeelInfo info;

		public LookAndFeelAction(final LookAndFeelInfo info) {
			this.putValue(Action.NAME, info.getName());
			this.info = info;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				UIManager.setLookAndFeel(this.info.getClassName());
				SwingUtilities.updateComponentTreeUI(RSTAUIDemoApp.this);
				if (RSTAUIDemoApp.this.findDialog != null) {
					RSTAUIDemoApp.this.findDialog.updateUI();
					RSTAUIDemoApp.this.replaceDialog.updateUI();
				}
				RSTAUIDemoApp.this.pack();
			} catch (final RuntimeException re) {
				throw re; // FindBugs
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private class ShowFindDialogAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ShowFindDialogAction() {
			super("Find...");
			final int c = RSTAUIDemoApp.this.getToolkit().getMenuShortcutKeyMask();
			this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (RSTAUIDemoApp.this.replaceDialog.isVisible())
				RSTAUIDemoApp.this.replaceDialog.setVisible(false);
			RSTAUIDemoApp.this.findDialog.setVisible(true);
		}

	}

	private class ShowReplaceDialogAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public ShowReplaceDialogAction() {
			super("Replace...");
			final int c = RSTAUIDemoApp.this.getToolkit().getMenuShortcutKeyMask();
			this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, c));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (RSTAUIDemoApp.this.findDialog.isVisible())
				RSTAUIDemoApp.this.findDialog.setVisible(false);
			RSTAUIDemoApp.this.replaceDialog.setVisible(true);
		}

	}

	private static class StatusBar extends JPanel {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final JLabel label;

		public StatusBar() {
			this.label = new JLabel("Ready");
			this.setLayout(new BorderLayout());
			this.add(this.label, BorderLayout.LINE_START);
			this.add(new JLabel(new SizeGripIcon()), BorderLayout.LINE_END);
		}

		public void setLabel(final String label) {
			this.label.setText(label);
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					// UIManager.setLookAndFeel("org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel");
				} catch (final Exception e) {
					e.printStackTrace();
				}
				new RSTAUIDemoApp().setVisible(true);
			}
		});
	}

	private final CollapsibleSectionPanel csp;

	private FindDialog findDialog;

	private FindToolBar findToolBar;

	private ReplaceDialog replaceDialog;

	private ReplaceToolBar replaceToolBar;

	private final StatusBar statusBar;

	private final RSyntaxTextArea textArea;

	public RSTAUIDemoApp() {

		this.initSearchDialogs();

		final JPanel contentPane = new JPanel(new BorderLayout());
		this.setContentPane(contentPane);
		this.csp = new CollapsibleSectionPanel();
		contentPane.add(this.csp);

		this.setJMenuBar(this.createMenuBar());

		this.textArea = new RSyntaxTextArea(25, 80);
		this.textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		this.textArea.setCodeFoldingEnabled(true);
		this.textArea.setMarkOccurrences(true);
		final RTextScrollPane sp = new RTextScrollPane(this.textArea);
		this.csp.add(sp);

		final ErrorStrip errorStrip = new ErrorStrip(this.textArea);
		contentPane.add(errorStrip, BorderLayout.LINE_END);
		// org.fife.rsta.ui.DocumentMap docMap = new
		// org.fife.rsta.ui.DocumentMap(textArea);
		// contentPane.add(docMap, BorderLayout.LINE_END);

		this.statusBar = new StatusBar();
		contentPane.add(this.statusBar, BorderLayout.SOUTH);

		this.setTitle("RSTAUI Demo Application");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.pack();
		this.setLocationRelativeTo(null);

	}

	private void addItem(final Action a, final ButtonGroup bg, final JMenu menu) {
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(a);
		bg.add(item);
		menu.add(item);
	}

	private JMenuBar createMenuBar() {

		final JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("Search");
		menu.add(new JMenuItem(new ShowFindDialogAction()));
		menu.add(new JMenuItem(new ShowReplaceDialogAction()));
		menu.add(new JMenuItem(new GoToLineAction()));
		menu.addSeparator();

		final int ctrl = this.getToolkit().getMenuShortcutKeyMask();
		final int shift = InputEvent.SHIFT_MASK;
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl | shift);
		Action a = this.csp.addBottomComponent(ks, this.findToolBar);
		a.putValue(Action.NAME, "Show Find Search Bar");
		menu.add(new JMenuItem(a));
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_H, ctrl | shift);
		a = this.csp.addBottomComponent(ks, this.replaceToolBar);
		a.putValue(Action.NAME, "Show Replace Search Bar");
		menu.add(new JMenuItem(a));

		mb.add(menu);

		menu = new JMenu("LookAndFeel");
		final ButtonGroup bg = new ButtonGroup();
		final LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
		for (final LookAndFeelInfo info : infos)
			this.addItem(new LookAndFeelAction(info), bg, menu);
		mb.add(menu);

		return mb;

	}

	@Override
	public String getSelectedText() {
		return this.textArea.getSelectedText();
	}

	/**
	 * Creates our Find and Replace dialogs.
	 */
	public void initSearchDialogs() {

		this.findDialog = new FindDialog(this, this);
		this.replaceDialog = new ReplaceDialog(this, this);

		// This ties the properties of the two dialogs together (match case,
		// regex, etc.).
		final SearchContext context = this.findDialog.getSearchContext();
		this.replaceDialog.setSearchContext(context);

		// Create tool bars and tie their search contexts together also.
		this.findToolBar = new FindToolBar(this);
		this.findToolBar.setSearchContext(context);
		this.replaceToolBar = new ReplaceToolBar(this);
		this.replaceToolBar.setSearchContext(context);

	}

	/**
	 * Listens for events from our search dialogs and actually does the dirty work.
	 */
	@Override
	public void searchEvent(final SearchEvent e) {

		final SearchEvent.Type type = e.getType();
		final SearchContext context = e.getSearchContext();
		SearchResult result = null;

		switch (type) {
		default: // Prevent FindBugs warning later
		case MARK_ALL:
			result = SearchEngine.markAll(this.textArea, context);
			break;
		case FIND:
			result = SearchEngine.find(this.textArea, context);
			if (!result.wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(this.textArea);
			break;
		case REPLACE:
			result = SearchEngine.replace(this.textArea, context);
			if (!result.wasFound())
				UIManager.getLookAndFeel().provideErrorFeedback(this.textArea);
			break;
		case REPLACE_ALL:
			result = SearchEngine.replaceAll(this.textArea, context);
			JOptionPane.showMessageDialog(null, result.getCount() + " occurrences replaced.");
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
		this.statusBar.setLabel(text);

	}

}