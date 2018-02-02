/*
 * 01/27/2004
 *
 * RSyntaxTextArea.java - An extension of RTextArea that adds
 * the ability to syntax highlight certain programming languages.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;
import org.fife.ui.rsyntaxtextarea.folding.DefaultFoldManager;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ToolTipInfo;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaUI;
import org.fife.ui.rtextarea.RecordableTextAction;

/**
 * An extension of <code>RTextArea</code> that adds syntax highlighting of
 * certain programming languages to its list of features. Languages currently
 * supported include:
 *
 * <table summary="">
 * <tr>
 * <td style="vertical-align: top">
 * <ul>
 * <li>ActionScript
 * <li>Assembler (X86)
 * <li>BBCode
 * <li>C
 * <li>C++
 * <li>CSS
 * <li>C#
 * <li>Clojure
 * <li>Dart
 * <li>Delphi
 * <li>DTD
 * <li>Fortran
 * <li>Groovy
 * <li>HTML
 * <li>htaccess
 * <li>Java
 * <li>JavaScript
 * <li>.jshintrc
 * <li>JSP
 * </ul>
 * </td>
 * <td style="vertical-align: top">
 * <ul>
 * <li>LaTeX
 * <li>Lisp
 * <li>Lua
 * <li>Make
 * <li>MXML
 * <li>NSIS
 * <li>Perl
 * <li>PHP
 * <li>Properties files
 * <li>Python
 * <li>Ruby
 * <li>SAS
 * <li>Scala
 * <li>SQL
 * <li>Tcl
 * <li>UNIX shell scripts
 * <li>Visual Basic
 * <li>Windows batch
 * <li>XML files
 * </ul>
 * </td>
 * </tr>
 * </table>
 *
 * Other added features include:
 * <ul style="columns: 2 12em; column-gap: 1em">
 * <li>Code folding
 * <li>Bracket matching
 * <li>Auto-indentation
 * <li>Copy as RTF
 * <li>Clickable hyperlinks (if the language scanner being used supports it)
 * <li>A pluggable "parser" system that can be used to implement syntax
 * validation, spell checking, etc.
 * </ul>
 *
 * It is recommended that you use an instance of
 * {@link org.fife.ui.rtextarea.RTextScrollPane} instead of a regular
 * <code>JScrollPane</code> as this class allows you to add line numbers and
 * bookmarks easily to your text area.
 *
 * @author Robert Futrell
 * @version 2.5.8
 * @see TextEditorPane
 */
public class RSyntaxTextArea extends RTextArea implements SyntaxConstants {

	/**
	 * A timer that animates the "bracket matching" animation.
	 */
	private class BracketMatchingTimer extends Timer implements ActionListener {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private int pulseCount;

		BracketMatchingTimer() {
			super(20, null);
			this.addActionListener(this);
			this.setCoalesce(false);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (RSyntaxTextArea.this.isBracketMatchingEnabled()) {
				if (RSyntaxTextArea.this.match != null)
					this.updateAndInvalidate(RSyntaxTextArea.this.match);
				if (RSyntaxTextArea.this.dotRect != null && RSyntaxTextArea.this.getPaintMatchedBracketPair())
					this.updateAndInvalidate(RSyntaxTextArea.this.dotRect);
				if (++this.pulseCount == 8) {
					this.pulseCount = 0;
					this.stop();
				}
			}
		}

		private void init(final Rectangle r) {
			r.x += 3;
			r.y += 3;
			r.width -= 6;
			r.height -= 6; // So animation can "grow" match
		}

		@Override
		public void start() {
			this.init(RSyntaxTextArea.this.match);
			if (RSyntaxTextArea.this.dotRect != null && RSyntaxTextArea.this.getPaintMatchedBracketPair())
				this.init(RSyntaxTextArea.this.dotRect);
			this.pulseCount = 0;
			super.start();
		}

		private void updateAndInvalidate(final Rectangle r) {
			if (this.pulseCount < 5) {
				r.x--;
				r.y--;
				r.width += 2;
				r.height += 2;
				RSyntaxTextArea.this.repaint(r.x, r.y, r.width, r.height);
			} else if (this.pulseCount < 7) {
				r.x++;
				r.y++;
				r.width -= 2;
				r.height -= 2;
				RSyntaxTextArea.this.repaint(r.x - 2, r.y - 2, r.width + 5, r.height + 5);
			}
		}

	}

	/**
	 * Renders the text on the line containing the "matched bracket" after a delay.
	 */
	private final class MatchedBracketPopupTimer extends Timer implements ActionListener, CaretListener {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private int matchedBracketOffs;
		private int origDot;
		private MatchedBracketPopup popup;

		private MatchedBracketPopupTimer() {
			super(350, null);
			this.addActionListener(this);
			this.setRepeats(false);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {

			if (this.popup != null)
				this.popup.dispose();

			final Window window = SwingUtilities.getWindowAncestor(RSyntaxTextArea.this);
			this.popup = new MatchedBracketPopup(window, RSyntaxTextArea.this, this.matchedBracketOffs);
			this.popup.pack();
			this.popup.setVisible(true);

		}

		@Override
		public void caretUpdate(final CaretEvent e) {
			final int dot = e.getDot();
			if (dot != this.origDot) {
				this.stop();
				RSyntaxTextArea.this.removeCaretListener(this);
				if (this.popup != null)
					this.popup.dispose();
			}
		}

		/**
		 * Restarts this timer, and stores a new offset to paint.
		 *
		 * @param matchedBracketOffs
		 *            The offset of the new matched bracket.
		 */
		public void restart(final int matchedBracketOffs) {
			this.origDot = RSyntaxTextArea.this.getCaretPosition();
			this.matchedBracketOffs = matchedBracketOffs;
			this.restart();
		}

		@Override
		public void start() {
			super.start();
			RSyntaxTextArea.this.addCaretListener(this);
		}

	}

	/**
	 * Handles hyperlinks.
	 */
	private class RSyntaxTextAreaMutableCaretEvent extends RTextAreaMutableCaretEvent {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private Insets insets;

		protected RSyntaxTextAreaMutableCaretEvent(final RTextArea textArea) {
			super(textArea);
			this.insets = new Insets(0, 0, 0, 0);
		}

		private HyperlinkEvent createHyperlinkEvent() {
			HyperlinkEvent he = null;
			if (RSyntaxTextArea.this.linkGeneratorResult != null) {
				he = RSyntaxTextArea.this.linkGeneratorResult.execute();
				RSyntaxTextArea.this.linkGeneratorResult = null;
			} else {
				final Token t = RSyntaxTextArea.this.modelToToken(RSyntaxTextArea.this.hoveredOverLinkOffset);
				URL url = null;
				String desc = null;
				try {
					String temp = t.getLexeme();
					// URI's need "http://" prefix for web URL's to work.
					if (temp.startsWith("www."))
						temp = "http://" + temp;
					url = new URL(temp);
				} catch (final MalformedURLException mue) {
					desc = mue.getMessage();
				}
				he = new HyperlinkEvent(RSyntaxTextArea.this, HyperlinkEvent.EventType.ACTIVATED, url, desc);
			}
			return he;
		}

		private boolean equal(final LinkGeneratorResult e1, final LinkGeneratorResult e2) {
			return e1.getSourceOffset() == e2.getSourceOffset();
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			if (RSyntaxTextArea.this.getHyperlinksEnabled() && RSyntaxTextArea.this.isScanningForLinks
					&& RSyntaxTextArea.this.hoveredOverLinkOffset > -1) {
				final HyperlinkEvent he = this.createHyperlinkEvent();
				if (he != null)
					RSyntaxTextArea.this.fireHyperlinkUpdate(he);
				RSyntaxTextArea.this.stopScanningForLinks();
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {

			super.mouseMoved(e);

			if (!RSyntaxTextArea.this.getHyperlinksEnabled())
				return;

			// If our link scanning mask is pressed...
			if ((e.getModifiersEx() & RSyntaxTextArea.this.linkScanningMask) == RSyntaxTextArea.this.linkScanningMask) {

				// GitHub issue #25 - links identified at "edges" of editor
				// should not be activated if mouse is in margin insets.
				this.insets = RSyntaxTextArea.this.getInsets(this.insets);
				if (this.insets != null) {
					final int x = e.getX();
					final int y = e.getY();
					if (x <= this.insets.left || y < this.insets.top) {
						if (RSyntaxTextArea.this.isScanningForLinks)
							RSyntaxTextArea.this.stopScanningForLinks();
						return;
					}
				}

				RSyntaxTextArea.this.isScanningForLinks = true;
				Token t = RSyntaxTextArea.this.viewToToken(e.getPoint());
				if (t != null)
					// Copy token, viewToModel() unfortunately modifies Token
					t = new TokenImpl(t);
				Cursor c2 = null;
				if (t != null && t.isHyperlink()) {
					if (RSyntaxTextArea.this.hoveredOverLinkOffset == -1
							|| RSyntaxTextArea.this.hoveredOverLinkOffset != t.getOffset()) {
						RSyntaxTextArea.this.hoveredOverLinkOffset = t.getOffset();
						RSyntaxTextArea.this.repaint();
					}
					c2 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				} else if (t != null && RSyntaxTextArea.this.linkGenerator != null) {
					final int offs = RSyntaxTextArea.this.viewToModel(e.getPoint());
					final LinkGeneratorResult newResult = RSyntaxTextArea.this.linkGenerator
							.isLinkAtOffset(RSyntaxTextArea.this, offs);
					if (newResult != null) {
						// Repaint if we're at a new link now.
						if (RSyntaxTextArea.this.linkGeneratorResult == null
								|| !this.equal(newResult, RSyntaxTextArea.this.linkGeneratorResult))
							RSyntaxTextArea.this.repaint();
						RSyntaxTextArea.this.linkGeneratorResult = newResult;
						RSyntaxTextArea.this.hoveredOverLinkOffset = t.getOffset();
						c2 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
					} else {
						// Repaint if we've moved off of a link.
						if (RSyntaxTextArea.this.linkGeneratorResult != null)
							RSyntaxTextArea.this.repaint();
						c2 = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
						RSyntaxTextArea.this.hoveredOverLinkOffset = -1;
						RSyntaxTextArea.this.linkGeneratorResult = null;
					}
				} else {
					c2 = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
					RSyntaxTextArea.this.hoveredOverLinkOffset = -1;
					RSyntaxTextArea.this.linkGeneratorResult = null;
				}
				if (RSyntaxTextArea.this.getCursor() != c2) {
					RSyntaxTextArea.this.setCursor(c2);
					// TODO: Repaint just the affected line(s).
					RSyntaxTextArea.this.repaint(); // Link either left or went into.
				}
			} else if (RSyntaxTextArea.this.isScanningForLinks)
				RSyntaxTextArea.this.stopScanningForLinks();

		}

	}

	public static final String ANIMATE_BRACKET_MATCHING_PROPERTY = "RSTA.animateBracketMatching";
	public static final String ANTIALIAS_PROPERTY = "RSTA.antiAlias";
	public static final String AUTO_INDENT_PROPERTY = "RSTA.autoIndent";
	public static final String BRACKET_MATCHING_PROPERTY = "RSTA.bracketMatching";
	public static final String CLEAR_WHITESPACE_LINES_PROPERTY = "RSTA.clearWhitespaceLines";
	public static final String CLOSE_CURLY_BRACES_PROPERTY = "RSTA.closeCurlyBraces";
	public static final String CLOSE_MARKUP_TAGS_PROPERTY = "RSTA.closeMarkupTags";
	public static final String CODE_FOLDING_PROPERTY = "RSTA.codeFolding";
	/** Handles code templates. */
	private static CodeTemplateManager codeTemplateManager;
	private static RecordableTextAction collapseAllCommentFoldsAction;
	private static RecordableTextAction collapseAllFoldsAction;
	private static final Color DEFAULT_BRACKET_MATCH_BG_COLOR = new Color(234, 234, 255);
	private static final Color DEFAULT_BRACKET_MATCH_BORDER_COLOR = new Color(0, 0, 128);
	private static final Color DEFAULT_SELECTION_COLOR = new Color(200, 200, 255);
	public static final String EOL_VISIBLE_PROPERTY = "RSTA.eolMarkersVisible";
	private static RecordableTextAction expandAllFoldsAction;
	public static final String FOCUSABLE_TIPS_PROPERTY = "RSTA.focusableTips";
	public static final String FRACTIONAL_FONTMETRICS_PROPERTY = "RSTA.fractionalFontMetrics";
	public static final String HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY = "RSTA.highlightSecondaryLanguages";
	public static final String HYPERLINKS_ENABLED_PROPERTY = "RSTA.hyperlinksEnabled";
	public static final String MARK_OCCURRENCES_PROPERTY = "RSTA.markOccurrences";

	public static final String MARKED_OCCURRENCES_CHANGED_PROPERTY = "RSTA.markedOccurrencesChanged";
	private static final String MSG = "org.fife.ui.rsyntaxtextarea.RSyntaxTextArea";
	public static final String PAINT_MATCHED_BRACKET_PAIR_PROPERTY = "RSTA.paintMatchedBracketPair";

	public static final String PARSER_NOTICES_PROPERTY = "RSTA.parserNotices";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final String SYNTAX_SCHEME_PROPERTY = "RSTA.syntaxScheme";
	public static final String SYNTAX_STYLE_PROPERTY = "RSTA.syntaxStyle";
	public static final String TAB_LINE_COLOR_PROPERTY = "RSTA.tabLineColor";
	public static final String TAB_LINES_PROPERTY = "RSTA.tabLines";

	/** Whether or not templates are enabled. */
	private static boolean templatesEnabled;

	private static RecordableTextAction toggleCurrentFoldAction;

	public static final String USE_SELECTED_TEXT_COLOR_PROPERTY = "RSTA.useSelectedTextColor";

	public static final String VISIBLE_WHITESPACE_PROPERTY = "RSTA.visibleWhitespace";

	/**
	 * See createPopupMenuActions() in RTextArea. TODO: Remove these horrible hacks
	 * and move localizing of actions into the editor kits, where it should be! The
	 * context menu should contain actions from the editor kits.
	 */
	private static void createRstaPopupMenuActions() {

		final ResourceBundle msg = ResourceBundle.getBundle(RSyntaxTextArea.MSG);

		RSyntaxTextArea.toggleCurrentFoldAction = new RSyntaxTextAreaEditorKit.ToggleCurrentFoldAction();
		RSyntaxTextArea.toggleCurrentFoldAction.setProperties(msg, "Action.ToggleCurrentFold");

		RSyntaxTextArea.collapseAllCommentFoldsAction = new RSyntaxTextAreaEditorKit.CollapseAllCommentFoldsAction();
		RSyntaxTextArea.collapseAllCommentFoldsAction.setProperties(msg, "Action.CollapseCommentFolds");

		RSyntaxTextArea.collapseAllFoldsAction = new RSyntaxTextAreaEditorKit.CollapseAllFoldsAction(true);
		RSyntaxTextArea.expandAllFoldsAction = new RSyntaxTextAreaEditorKit.ExpandAllFoldsAction(true);

	}

	/**
	 * Returns the code template manager for all instances of
	 * <code>RSyntaxTextArea</code>. The manager is lazily created.
	 *
	 * @return The code template manager.
	 * @see #setTemplatesEnabled(boolean)
	 */
	public static synchronized CodeTemplateManager getCodeTemplateManager() {
		if (RSyntaxTextArea.codeTemplateManager == null)
			RSyntaxTextArea.codeTemplateManager = new CodeTemplateManager();
		return RSyntaxTextArea.codeTemplateManager;
	}

	/**
	 * Returns the default bracket-match background color.
	 *
	 * @return The color.
	 * @see #getDefaultBracketMatchBorderColor
	 */
	public static final Color getDefaultBracketMatchBGColor() {
		return RSyntaxTextArea.DEFAULT_BRACKET_MATCH_BG_COLOR;
	}

	/**
	 * Returns the default bracket-match border color.
	 *
	 * @return The color.
	 * @see #getDefaultBracketMatchBGColor
	 */
	public static final Color getDefaultBracketMatchBorderColor() {
		return RSyntaxTextArea.DEFAULT_BRACKET_MATCH_BORDER_COLOR;
	}

	/**
	 * Returns the default selection color for this text area. This color was chosen
	 * because it's light and <code>RSyntaxTextArea</code> does not change text
	 * color between selected/unselected text for contrast like regular
	 * <code>JTextArea</code>s do.
	 *
	 * @return The default selection color.
	 */
	public static Color getDefaultSelectionColor() {
		return RSyntaxTextArea.DEFAULT_SELECTION_COLOR;
	}

	/**
	 * Returns whether or not templates are enabled for all instances of
	 * <code>RSyntaxTextArea</code>.
	 * <p>
	 *
	 * For more flexible boilerplate code insertion, consider using the <a href=
	 * "http://javadoc.fifesoft.com/autocomplete/org/fife/ui/autocomplete/TemplateCompletion.html">
	 * TemplateCompletion class</a> in the
	 * <a href="https://github.com/bobbylight/AutoComplete">AutoComplete add-on
	 * library</a>.
	 *
	 * @return Whether templates are enabled.
	 * @see #saveTemplates()
	 * @see #setTemplateDirectory(String)
	 * @see #setTemplatesEnabled(boolean)
	 */
	public static synchronized boolean getTemplatesEnabled() {
		return RSyntaxTextArea.templatesEnabled;
	}

	/**
	 * Attempts to save all currently-known templates to the current template
	 * directory, as set by <code>setTemplateDirectory</code>. Templates will be
	 * saved as XML files with names equal to their abbreviations; for example, a
	 * template that expands on the word "forb" will be saved as
	 * <code>forb.xml</code>.
	 *
	 * @return Whether or not the save was successful. The save will be unsuccessful
	 *         if the template directory does not exist or if it has not been set
	 *         (i.e., you have not yet called <code>setTemplateDirectory</code>).
	 * @see #getTemplatesEnabled
	 * @see #setTemplateDirectory
	 * @see #setTemplatesEnabled
	 */
	public static synchronized boolean saveTemplates() {
		if (!RSyntaxTextArea.getTemplatesEnabled())
			return false;
		return RSyntaxTextArea.getCodeTemplateManager().saveTemplates();
	}

	/**
	 * If templates are enabled, all currently-known templates are forgotten and all
	 * templates are loaded from all files in the specified directory ending in
	 * "*.xml". If templates aren't enabled, nothing happens.
	 *
	 * @param dir
	 *            The directory containing files ending in extension
	 *            <code>.xml</code> that contain templates to load.
	 * @return <code>true</code> if the load was successful; <code>false</code> if
	 *         either templates aren't currently enabled or the load failed somehow
	 *         (most likely, the directory doesn't exist).
	 * @see #getTemplatesEnabled
	 * @see #setTemplatesEnabled
	 * @see #saveTemplates
	 */
	public static synchronized boolean setTemplateDirectory(final String dir) {
		if (RSyntaxTextArea.getTemplatesEnabled() && dir != null) {
			final File directory = new File(dir);
			if (directory.isDirectory())
				return RSyntaxTextArea.getCodeTemplateManager().setTemplateDirectory(directory) > -1;
			final boolean created = directory.mkdir();
			if (created)
				return RSyntaxTextArea.getCodeTemplateManager().setTemplateDirectory(directory) > -1;
		}
		return false;
	}

	/**
	 * Enables or disables templates.
	 * <p>
	 *
	 * Templates are a set of "shorthand identifiers" that you can configure so that
	 * you only have to type a short identifier (such as "forb") to insert a larger
	 * amount of code into the document (such as:
	 * <p>
	 *
	 * <pre>
	 *   for (&lt;caret&gt;) {
	 *
	 *   }
	 * </pre>
	 *
	 * Templates are a shared resource among all instances of
	 * <code>RSyntaxTextArea</code>; that is, templates can only be enabled/disabled
	 * for all text areas globally, not individually, and all text areas have access
	 * of the same templates. This should not be an issue; rather, it should be
	 * beneficial as it promotes uniformity among all text areas in an application.
	 * <p>
	 *
	 * For more flexible boilerplate code insertion, consider using the <a href=
	 * "http://javadoc.fifesoft.com/autocomplete/org/fife/ui/autocomplete/TemplateCompletion.html">TemplateCompletion
	 * class</a> in the
	 * <a href="https://github.com/bobbylight/AutoComplete">AutoComplete add-on
	 * library</a>.
	 *
	 * @param enabled
	 *            Whether or not templates should be enabled.
	 * @see #getTemplatesEnabled()
	 */
	public static synchronized void setTemplatesEnabled(final boolean enabled) {
		RSyntaxTextArea.templatesEnabled = enabled;
	}

	/** Cached desktop anti-aliasing hints, if anti-aliasing is enabled. */
	private Map<?, ?> aaHints;

	/** Whether or not bracket matching is animated. */
	private boolean animateBracketMatching;

	/**
	 * Whether or not auto-indent is on.
	 */
	private boolean autoIndentEnabled;

	/**
	 * Used to store the location of the bracket at the caret position (either just
	 * before or just after it) and the location of its match.
	 */
	private Point bracketInfo;

	/** Whether or not bracket matching is enabled. */
	private boolean bracketMatchingEnabled;

	private BracketMatchingTimer bracketRepaintTimer;

	private String cachedTip;

	/** Used to work around an issue with Apple JVMs. */
	private Point cachedTipLoc;

	/**
	 * Whether or not lines with nothing but whitespace are "made empty".
	 */
	private boolean clearWhitespaceLines;

	/**
	 * Whether curly braces should be closed on Enter key presses, (if the current
	 * language supports it).
	 */
	private boolean closeCurlyBraces;

	/**
	 * Whether closing markup tags should be automatically completed when
	 * "<code>&lt;/</code>" is typed (if the current language is a markup language).
	 */
	private boolean closeMarkupTags;

	/** Metrics of the text area's font. */
	private FontMetrics defaultFontMetrics;

	/**
	 * The rectangle surrounding the current offset if both bracket matching and
	 * "match both brackets" are enabled.
	 */
	private Rectangle dotRect;

	/** Whether EOL markers should be visible at the end of each line. */
	private boolean eolMarkersVisible;

	/** The last focusable tip displayed. */
	private FocusableTip focusableTip;

	private JMenu foldingMenu;

	private FoldManager foldManager;

	private boolean fractionalFontMetricsEnabled;

	/** Whether secondary languages have their backgrounds colored. */
	private boolean highlightSecondaryLanguages;

	private int hoveredOverLinkOffset;

	/** The color to use when painting hyperlinks. */
	private Color hyperlinkFG;

	/**
	 * Whether hyperlinks are enabled (must be supported by the syntax scheme being
	 * used).
	 */
	private boolean hyperlinksEnabled;

	/**
	 * Whether the editor is currently scanning for hyperlinks on mouse movement.
	 */
	private boolean isScanningForLinks;
	/** The location of the last matched bracket. */
	private int lastBracketMatchPos;

	private int lineHeight; // Height of a line of text; same for default, bold & italic.

	private LinkGenerator linkGenerator;

	private LinkGeneratorResult linkGeneratorResult;
	/**
	 * Mask used to determine if the correct key is being held down to scan for
	 * hyperlinks (ctrl, meta, etc.).
	 */
	private int linkScanningMask;

	/** The color used to render "marked occurrences". */
	private Color markOccurrencesColor;

	/** The delay before occurrences are marked in the editor. */
	private int markOccurrencesDelay;

	/** Handles "mark occurrences" support. */
	private MarkOccurrencesSupport markOccurrencesSupport;

	/**
	 * The rectangle surrounding the "matched bracket" if bracket matching is
	 * enabled.
	 */
	private Rectangle match;

	/**
	 * Colors used for the "matched bracket" if bracket matching is enabled.
	 */
	private Color matchedBracketBGColor;

	private Color matchedBracketBorderColor;

	private MatchedBracketPopupTimer matchedBracketPopupTimer;

	private int maxAscent;
	private boolean metricsNeverRefreshed;
	/** Whether a border should be painted around marked occurrences. */
	private boolean paintMarkOccurrencesBorder;

	/** Whether <b>both</b> brackets are highlighted when bracket matching. */
	private boolean paintMatchedBracketPair;

	/** Whether tab lines are enabled. */
	private boolean paintTabLines;

	/** Manages running the parser. */
	private ParserManager parserManager;

	private int rhsCorrection;

	private Color[] secondaryLanguageBackgrounds;

	/** Whether a popup showing matched bracket lines when they're off-screen. */
	private boolean showMatchedBracketPopup;

	/** The colors used for syntax highlighting. */
	private SyntaxScheme syntaxScheme;

	/** The key for the syntax style to be highlighting. */
	private String syntaxStyleKey;

	/** The color to use when painting tab lines. */
	private Color tabLineColor;

	/** Renders tokens. */
	private TokenPainter tokenPainter;

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean useFocusableTips;

	/** Whether the "selected text" color should be used with selected text. */
	private boolean useSelectedTextColor;

	/** Whether we are displaying visible whitespace (spaces and tabs). */
	private boolean whitespaceVisible;

	/**
	 * Constructor.
	 */
	public RSyntaxTextArea() {
	}

	/**
	 * Creates a new <code>RSyntaxTextArea</code>.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 */
	public RSyntaxTextArea(final int textMode) {
		super(textMode);
	}

	/**
	 * Constructor.
	 *
	 * @param rows
	 *            The number of rows to display.
	 * @param cols
	 *            The number of columns to display.
	 * @throws IllegalArgumentException
	 *             If either <code>rows</code> or <code>cols</code> is negative.
	 */
	public RSyntaxTextArea(final int rows, final int cols) {
		super(rows, cols);
	}

	/**
	 * Constructor.
	 *
	 * @param doc
	 *            The document for the editor.
	 */
	public RSyntaxTextArea(final RSyntaxDocument doc) {
		super(doc);
		this.setSyntaxEditingStyle(doc.getSyntaxStyle());
	}

	/**
	 * Constructor.
	 *
	 * @param doc
	 *            The document for the editor.
	 * @param text
	 *            The initial text to display.
	 * @param rows
	 *            The number of rows to display.
	 * @param cols
	 *            The number of columns to display.
	 * @throws IllegalArgumentException
	 *             If either <code>rows</code> or <code>cols</code> is negative.
	 */
	public RSyntaxTextArea(final RSyntaxDocument doc, final String text, final int rows, final int cols) {
		super(doc, text, rows, cols);
	}

	/**
	 * Constructor.
	 *
	 * @param text
	 *            The initial text to display.
	 */
	public RSyntaxTextArea(final String text) {
		super(text);
	}

	/**
	 * Constructor.
	 *
	 * @param text
	 *            The initial text to display.
	 * @param rows
	 *            The number of rows to display.
	 * @param cols
	 *            The number of columns to display.
	 * @throws IllegalArgumentException
	 *             If either <code>rows</code> or <code>cols</code> is negative.
	 */
	public RSyntaxTextArea(final String text, final int rows, final int cols) {
		super(text, rows, cols);
	}

	/**
	 * Adds an "active line range" listener to this text area.
	 *
	 * @param l
	 *            The listener to add.
	 * @see #removeActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void addActiveLineRangeListener(final ActiveLineRangeListener l) {
		this.listenerList.add(ActiveLineRangeListener.class, l);
	}

	/**
	 * Adds a hyperlink listener to this text area.
	 *
	 * @param l
	 *            The listener to add.
	 * @see #removeHyperlinkListener(HyperlinkListener)
	 */
	public void addHyperlinkListener(final HyperlinkListener l) {
		this.listenerList.add(HyperlinkListener.class, l);
	}

	/**
	 * Updates the font metrics the first time we're displayed.
	 */
	@Override
	public void addNotify() {

		super.addNotify();

		// Some LookAndFeels (e.g. WebLaF) for some reason have a 0x0 parent
		// window initially (perhaps something to do with them fading in?),
		// which will cause an exception from getGraphics(), so we must be
		// careful here.
		if (this.metricsNeverRefreshed) {
			final Window parent = SwingUtilities.getWindowAncestor(this);
			if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
				this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
				this.metricsNeverRefreshed = false;
			}
		}

		// Re-start parsing if we were removed from one container and added
		// to another
		if (this.parserManager != null)
			this.parserManager.restartParsing();

	}

	/**
	 * Adds the parser to "validate" the source code in this text area. This can be
	 * anything from a spell checker to a "compiler" that verifies source code.
	 *
	 * @param parser
	 *            The new parser. A value of <code>null</code> will do nothing.
	 * @see #getParser(int)
	 * @see #getParserCount()
	 * @see #removeParser(Parser)
	 */
	public void addParser(final Parser parser) {
		if (this.parserManager == null)
			this.parserManager = new ParserManager(this);
		this.parserManager.addParser(parser);
	}

	/**
	 * Appends a submenu with code folding options to this text component's popup
	 * menu.
	 *
	 * @param popup
	 *            The popup menu to append to.
	 * @see #createPopupMenu()
	 */
	protected void appendFoldingMenu(final JPopupMenu popup) {
		popup.addSeparator();
		final ResourceBundle bundle = ResourceBundle.getBundle(RSyntaxTextArea.MSG);
		this.foldingMenu = new JMenu(bundle.getString("ContextMenu.Folding"));
		this.foldingMenu.add(this.createPopupMenuItem(RSyntaxTextArea.toggleCurrentFoldAction));
		this.foldingMenu.add(this.createPopupMenuItem(RSyntaxTextArea.collapseAllCommentFoldsAction));
		this.foldingMenu.add(this.createPopupMenuItem(RSyntaxTextArea.collapseAllFoldsAction));
		this.foldingMenu.add(this.createPopupMenuItem(RSyntaxTextArea.expandAllFoldsAction));
		popup.add(this.foldingMenu);

	}

	/**
	 * Recalculates the height of a line in this text area and the maximum ascent of
	 * all fonts displayed.
	 */
	private void calculateLineHeight() {

		this.lineHeight = this.maxAscent = 0;

		// Each token style.
		for (int i = 0; i < this.syntaxScheme.getStyleCount(); i++) {
			final Style ss = this.syntaxScheme.getStyle(i);
			if (ss != null && ss.font != null) {
				final FontMetrics fm = this.getFontMetrics(ss.font);
				final int height = fm.getHeight();
				if (height > this.lineHeight)
					this.lineHeight = height;
				final int ascent = fm.getMaxAscent();
				if (ascent > this.maxAscent)
					this.maxAscent = ascent;
			}
		}

		// The text area's (default) font).
		final Font temp = this.getFont();
		final FontMetrics fm = this.getFontMetrics(temp);
		final int height = fm.getHeight();
		if (height > this.lineHeight)
			this.lineHeight = height;
		final int ascent = fm.getMaxAscent();
		if (ascent > this.maxAscent)
			this.maxAscent = ascent;

	}

	/**
	 * Removes all parsers from this text area.
	 *
	 * @see #removeParser(Parser)
	 */
	public void clearParsers() {
		if (this.parserManager != null)
			this.parserManager.clearParsers();
	}

	/**
	 * Clones a token list. This is necessary as tokens are reused in
	 * {@link RSyntaxDocument}, so we can't simply use the ones we are handed from
	 * it.
	 *
	 * @param t
	 *            The token list to clone.
	 * @return The clone of the token list.
	 */
	private TokenImpl cloneTokenList(Token t) {

		if (t == null)
			return null;

		final TokenImpl clone = new TokenImpl(t);
		TokenImpl cloneEnd = clone;

		while ((t = t.getNextToken()) != null) {
			final TokenImpl temp = new TokenImpl(t);
			cloneEnd.setNextToken(temp);
			cloneEnd = temp;
		}

		return clone;

	}

	/**
	 * Overridden to toggle the enabled state of various RSyntaxTextArea-specific
	 * menu items.
	 *
	 * If you set the popup menu via {@link #setPopupMenu(JPopupMenu)}, you will
	 * want to override this method, especially if you removed any of the menu items
	 * in the default popup menu.
	 *
	 * @param popupMenu
	 *            The popup menu. This will never be <code>null</code>.
	 * @see #createPopupMenu()
	 * @see #setPopupMenu(JPopupMenu)
	 */
	@Override
	protected void configurePopupMenu(final JPopupMenu popupMenu) {

		super.configurePopupMenu(popupMenu);

		// They may have overridden createPopupMenu()...
		if (popupMenu != null && popupMenu.getComponentCount() > 0 && this.foldingMenu != null)
			this.foldingMenu.setEnabled(this.foldManager.isCodeFoldingSupportedAndEnabled());
	}

	/**
	 * Copies the currently selected text to the system clipboard, with any
	 * necessary style information (font, foreground color and background color).
	 * Does nothing for <code>null</code> selections.
	 */
	public void copyAsRtf() {

		final int selStart = this.getSelectionStart();
		final int selEnd = this.getSelectionEnd();
		if (selStart == selEnd)
			return;

		// Make sure there is a system clipboard, and that we can write
		// to it.
		final SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			try {
				sm.checkSystemClipboardAccess();
			} catch (final SecurityException se) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}
		final Clipboard cb = this.getToolkit().getSystemClipboard();

		// Create the RTF selection.
		final RtfGenerator gen = new RtfGenerator();
		final Token tokenList = this.getTokenListFor(selStart, selEnd);
		for (Token t = tokenList; t != null; t = t.getNextToken())
			if (t.isPaintable())
				if (t.length() == 1 && t.charAt(0) == '\n')
					gen.appendNewline();
				else {
					final Font font = this.getFontForTokenType(t.getType());
					final Color bg = this.getBackgroundForToken(t);
					final boolean underline = this.getUnderlineForToken(t);
					// Small optimization - don't print fg color if this
					// is a whitespace color. Saves on RTF size.
					if (t.isWhitespace())
						gen.appendToDocNoFG(t.getLexeme(), font, bg, underline);
					else {
						final Color fg = this.getForegroundForToken(t);
						gen.appendToDoc(t.getLexeme(), font, fg, bg, underline);
					}
				}

		// Set the system clipboard contents to the RTF selection.
		final RtfTransferable contents = new RtfTransferable(gen.getRtf().getBytes());
		// System.out.println("*** " + new String(gen.getRtf().getBytes()));
		try {
			cb.setContents(contents, null);
		} catch (final IllegalStateException ise) {
			UIManager.getLookAndFeel().provideErrorFeedback(null);
			return;
		}

	}

	/**
	 * Returns the document to use for an <code>RSyntaxTextArea</code>.
	 *
	 * @return The document.
	 */
	@Override
	protected Document createDefaultModel() {
		return new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE);
	}

	/**
	 * Returns the caret event/mouse listener for <code>RTextArea</code>s.
	 *
	 * @return The caret event/mouse listener.
	 */
	@Override
	protected RTAMouseListener createMouseListener() {
		return new RSyntaxTextAreaMutableCaretEvent(this);
	}

	/**
	 * Overridden to add menu items related to cold folding.
	 *
	 * @return The popup menu.
	 * @see #appendFoldingMenu(JPopupMenu)
	 */
	@Override
	protected JPopupMenu createPopupMenu() {
		final JPopupMenu popup = super.createPopupMenu();
		this.appendFoldingMenu(popup);
		return popup;
	}

	/**
	 * Returns the a real UI to install on this text area.
	 *
	 * @return The UI.
	 */
	@Override
	protected RTextAreaUI createRTextAreaUI() {
		return new RSyntaxTextAreaUI(this);
	}

	/**
	 * If the caret is on a bracket, this method finds the matching bracket, and if
	 * it exists, highlights it.
	 */
	protected final void doBracketMatching() {

		// We always need to repaint the "matched bracket" highlight if it
		// exists.
		if (this.match != null) {
			this.repaint(this.match);
			if (this.dotRect != null)
				this.repaint(this.dotRect);
		}

		// If a matching bracket is found, get its bounds and paint it!
		final int lastCaretBracketPos = this.bracketInfo == null ? -1 : this.bracketInfo.x;
		this.bracketInfo = RSyntaxUtilities.getMatchingBracketPosition(this, this.bracketInfo);
		if (this.bracketInfo.y > -1
				&& (this.bracketInfo.y != this.lastBracketMatchPos || this.bracketInfo.x != lastCaretBracketPos))
			try {
				this.match = this.modelToView(this.bracketInfo.y);
				if (this.match != null) { // Happens if we're not yet visible
					if (this.getPaintMatchedBracketPair())
						this.dotRect = this.modelToView(this.bracketInfo.x);
					else
						this.dotRect = null;
					if (this.getAnimateBracketMatching())
						this.bracketRepaintTimer.restart();
					this.repaint(this.match);
					if (this.dotRect != null)
						this.repaint(this.dotRect);

					if (this.getShowMatchedBracketPopup()) {
						final Container parent = this.getParent();
						if (parent instanceof JViewport) {
							final Rectangle visibleRect = this.getVisibleRect();
							if (this.match.y + this.match.height < visibleRect.getY()) {
								if (this.matchedBracketPopupTimer == null)
									this.matchedBracketPopupTimer = new MatchedBracketPopupTimer();
								this.matchedBracketPopupTimer.restart(this.bracketInfo.y);
							}
						}
					}

				}
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Shouldn't happen.
			}
		else if (this.bracketInfo.y == -1) {
			// Set match to null so the old value isn't still repainted.
			this.match = null;
			this.dotRect = null;
			this.bracketRepaintTimer.stop();
		}
		this.lastBracketMatchPos = this.bracketInfo.y;

	}

	/**
	 * Notifies all listeners that the active line range has changed.
	 *
	 * @param min
	 *            The minimum "active" line, or <code>-1</code>.
	 * @param max
	 *            The maximum "active" line, or <code>-1</code>.
	 */
	private void fireActiveLineRangeEvent(final int min, final int max) {
		ActiveLineRangeEvent e = null; // Lazily created
		// Guaranteed to return a non-null array
		final Object[] listeners = this.listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
			if (listeners[i] == ActiveLineRangeListener.class) {
				if (e == null)
					e = new ActiveLineRangeEvent(this, min, max);
				((ActiveLineRangeListener) listeners[i + 1]).activeLineRangeChanged(e);
			}
	}

	/**
	 * Notifies all listeners that a caret change has occurred.
	 *
	 * @param e
	 *            The caret event.
	 */
	@Override
	protected void fireCaretUpdate(final CaretEvent e) {
		super.fireCaretUpdate(e);
		if (this.isBracketMatchingEnabled())
			this.doBracketMatching();
	}

	/**
	 * Notifies all listeners that have registered interest for notification on this
	 * event type. The listener list is processed last to first.
	 *
	 * @param e
	 *            The event to fire.
	 */
	private void fireHyperlinkUpdate(final HyperlinkEvent e) {
		// Guaranteed to return a non-null array
		final Object[] listeners = this.listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
			if (listeners[i] == HyperlinkListener.class)
				((HyperlinkListener) listeners[i + 1]).hyperlinkUpdate(e);
	}

	/**
	 * Notifies listeners that the marked occurrences for this text area have
	 * changed.
	 */
	void fireMarkedOccurrencesChanged() {
		this.firePropertyChange(RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY, null, null);
	}

	/**
	 * Fires a notification that the parser notices for this text area have changed.
	 */
	void fireParserNoticesChange() {
		this.firePropertyChange(RSyntaxTextArea.PARSER_NOTICES_PROPERTY, null, null);
	}

	/**
	 * Called whenever a fold is collapsed or expanded. This causes the text editor
	 * to revalidate. This method is here because of poor design and should be
	 * removed.
	 *
	 * @param fold
	 *            The fold that was collapsed or expanded.
	 */
	public void foldToggled(final Fold fold) {
		this.match = null; // TODO: Update the bracket rect rather than hide it
		this.dotRect = null;
		if (this.getLineWrap())
			// NOTE: Without doing this later, the caret position is out of
			// sync with the Element structure when word wrap is enabled, and
			// causes BadLocationExceptions when an entire folded region is
			// deleted (see GitHub issue #22:
			// https://github.com/bobbylight/RSyntaxTextArea/issues/22)
			SwingUtilities.invokeLater(() -> RSyntaxTextArea.this.possiblyUpdateCurrentLineHighlightLocation());
		else
			this.possiblyUpdateCurrentLineHighlightLocation();
		this.revalidate();
		this.repaint();
	}

	/**
	 * Forces the given {@link Parser} to re-parse the content of this text area.
	 * <p>
	 *
	 * This method can be useful when a <code>Parser</code> can be configured as to
	 * what notices it returns. For example, if a Java language parser can be
	 * configured to set whether no serialVersionUID is a warning, error, or
	 * ignored, this method can be called after changing the expected notice type to
	 * have the document re-parsed.
	 *
	 * @param parser
	 *            The index of the <code>Parser</code> to re-run.
	 * @see #getParser(int)
	 */
	public void forceReparsing(final int parser) {
		this.parserManager.forceReparsing(parser);
	}

	/**
	 * Forces re-parsing with a specific parser. Note that if this parser is not
	 * installed on this text area, nothing will happen.
	 *
	 * @param parser
	 *            The parser that should re-parse this text area's contents. This
	 *            should be installed on this text area.
	 * @return Whether the parser was installed on this text area.
	 * @see #forceReparsing(int)
	 */
	public boolean forceReparsing(final Parser parser) {
		for (int i = 0; i < this.getParserCount(); i++)
			if (this.getParser(i) == parser) {
				this.forceReparsing(i);
				return true;
			}
		return false;
	}

	/**
	 * Returns whether bracket matching should be animated.
	 *
	 * @return Whether bracket matching should be animated.
	 * @see #setAnimateBracketMatching(boolean)
	 */
	public boolean getAnimateBracketMatching() {
		return this.animateBracketMatching;
	}

	/**
	 * Returns whether anti-aliasing is enabled in this editor.
	 *
	 * @return Whether anti-aliasing is enabled in this editor.
	 * @see #setAntiAliasingEnabled(boolean)
	 * @see #getFractionalFontMetricsEnabled()
	 */
	public boolean getAntiAliasingEnabled() {
		return this.aaHints != null;
	}

	/**
	 * Returns the background color for a token.
	 *
	 * @param token
	 *            The token.
	 * @return The background color to use for that token. If this value is is
	 *         <code>null</code> then this token has no special background color.
	 * @see #getForegroundForToken(Token)
	 */
	public Color getBackgroundForToken(final Token token) {
		Color c = null;
		if (this.getHighlightSecondaryLanguages()) {
			// 1-indexed, since 0 == main language.
			final int languageIndex = token.getLanguageIndex() - 1;
			if (languageIndex >= 0 && languageIndex < this.secondaryLanguageBackgrounds.length)
				c = this.secondaryLanguageBackgrounds[languageIndex];
		}
		if (c == null)
			c = this.syntaxScheme.getStyle(token.getType()).background;
		// Don't default to this.getBackground(), as Tokens simply don't
		// paint a background if they get a null Color.
		return c;
	}

	/**
	 * Returns whether curly braces should be automatically closed when a newline is
	 * entered after an opening curly brace. Note that this property is only honored
	 * for languages that use curly braces to denote code blocks.
	 *
	 * @return Whether curly braces should be automatically closed.
	 * @see #setCloseCurlyBraces(boolean)
	 */
	public boolean getCloseCurlyBraces() {
		return this.closeCurlyBraces;
	}

	/**
	 * Returns whether closing markup tags should be automatically completed when
	 * "<code>&lt;/</code>" is typed. Note that this property is only honored for
	 * markup languages, such as HTML, XML and PHP.
	 *
	 * @return Whether closing markup tags should be automatically completed.
	 * @see #setCloseMarkupTags(boolean)
	 */
	public boolean getCloseMarkupTags() {
		return this.closeMarkupTags;
	}

	/**
	 * Returns the "default" syntax highlighting color scheme. The colors used are
	 * somewhat standard among syntax highlighting text editors.
	 *
	 * @return The default syntax highlighting color scheme.
	 * @see #restoreDefaultSyntaxScheme()
	 * @see #getSyntaxScheme()
	 * @see #setSyntaxScheme(SyntaxScheme)
	 */
	public SyntaxScheme getDefaultSyntaxScheme() {
		return new SyntaxScheme(this.getFont());
	}

	/**
	 * Returns the caret's offset's rectangle, or <code>null</code> if there is
	 * currently no matched bracket, bracket matching is disabled, or "paint both
	 * matched brackets" is disabled. This should never be called by the programmer
	 * directly.
	 *
	 * @return The rectangle surrounding the matched bracket.
	 * @see #getMatchRectangle()
	 */
	Rectangle getDotRectangle() {
		return this.dotRect;
	}

	/**
	 * Returns whether an EOL marker should be drawn at the end of each line.
	 *
	 * @return Whether EOL markers should be visible.
	 * @see #setEOLMarkersVisible(boolean)
	 * @see #isWhitespaceVisible()
	 */
	public boolean getEOLMarkersVisible() {
		return this.eolMarkersVisible;
	}

	/**
	 * Returns the fold manager for this text area.
	 *
	 * @return The fold manager.
	 */
	public FoldManager getFoldManager() {
		return this.foldManager;
	}

	/**
	 * Returns the font for tokens of the specified type.
	 *
	 * @param type
	 *            The type of token.
	 * @return The font to use for that token type.
	 * @see #getFontMetricsForTokenType(int)
	 */
	public Font getFontForTokenType(final int type) {
		final Font f = this.syntaxScheme.getStyle(type).font;
		return f != null ? f : this.getFont();
	}

	/**
	 * Returns the font metrics for tokens of the specified type.
	 *
	 * @param type
	 *            The type of token.
	 * @return The font metrics to use for that token type.
	 * @see #getFontForTokenType(int)
	 */
	public FontMetrics getFontMetricsForTokenType(final int type) {
		final FontMetrics fm = this.syntaxScheme.getStyle(type).fontMetrics;
		return fm != null ? fm : this.defaultFontMetrics;
	}

	/**
	 * Returns the foreground color to use when painting a token.
	 *
	 * @param t
	 *            The token.
	 * @return The foreground color to use for that token. This value is never
	 *         <code>null</code>.
	 * @see #getBackgroundForToken(Token)
	 */
	public Color getForegroundForToken(final Token t) {
		if (this.getHyperlinksEnabled() && this.hoveredOverLinkOffset == t.getOffset()
				&& (t.isHyperlink() || this.linkGeneratorResult != null))
			return this.hyperlinkFG;
		return this.getForegroundForTokenType(t.getType());
	}

	/**
	 * Returns the foreground color to use when painting a token. This does not take
	 * into account whether the token is a hyperlink.
	 *
	 * @param type
	 *            The token type.
	 * @return The foreground color to use for that token. This value is never
	 *         <code>null</code>.
	 * @see #getForegroundForToken(Token)
	 */
	public Color getForegroundForTokenType(final int type) {
		final Color fg = this.syntaxScheme.getStyle(type).foreground;
		return fg != null ? fg : this.getForeground();
	}

	/**
	 * Returns whether fractional font metrics are enabled for this text area.
	 *
	 * @return Whether fractional font metrics are enabled.
	 * @see #setFractionalFontMetricsEnabled
	 * @see #getAntiAliasingEnabled()
	 */
	public boolean getFractionalFontMetricsEnabled() {
		return this.fractionalFontMetricsEnabled;
	}

	/**
	 * Returns a <code>Graphics2D</code> version of the specified graphics that has
	 * been initialized with the proper rendering hints.
	 *
	 * @param g
	 *            The graphics context for which to get a <code>Graphics2D</code>.
	 * @return The <code>Graphics2D</code>.
	 */
	private Graphics2D getGraphics2D(final Graphics g) {
		final Graphics2D g2d = (Graphics2D) g;
		if (this.aaHints != null)
			g2d.addRenderingHints(this.aaHints);
		if (this.fractionalFontMetricsEnabled)
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		return g2d;
	}

	/**
	 * Returns whether "secondary" languages should have their backgrounds colored
	 * differently to visually differentiate them. This feature imposes a fair
	 * performance penalty.
	 *
	 * @return Whether secondary languages have their backgrounds colored
	 *         differently.
	 * @see #setHighlightSecondaryLanguages(boolean)
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #getSecondaryLanguageCount()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 */
	public boolean getHighlightSecondaryLanguages() {
		return this.highlightSecondaryLanguages;
	}

	/**
	 * Returns the color to use when painting hyperlinks.
	 *
	 * @return The color to use when painting hyperlinks.
	 * @see #setHyperlinkForeground(Color)
	 * @see #getHyperlinksEnabled()
	 */
	public Color getHyperlinkForeground() {
		return this.hyperlinkFG;
	}

	/**
	 * Returns whether hyperlinks are enabled for this text area.
	 *
	 * @return Whether hyperlinks are enabled for this text area.
	 * @see #setHyperlinksEnabled(boolean)
	 */
	public boolean getHyperlinksEnabled() {
		return this.hyperlinksEnabled;
	}

	/**
	 * Returns the last visible offset in this text area. This may not be the length
	 * of the document if code folding is enabled.
	 *
	 * @return The last visible offset in this text area.
	 */
	public int getLastVisibleOffset() {
		if (this.isCodeFoldingEnabled()) {
			final int lastVisibleLine = this.foldManager.getLastVisibleLine();
			if (lastVisibleLine < this.getLineCount() - 1)
				try {
					return this.getLineEndOffset(lastVisibleLine) - 1;
				} catch (final BadLocationException ble) { // Never happens
					ble.printStackTrace();
				}
		}
		return this.getDocument().getLength();
	}

	/**
	 * Returns the height to use for a line of text in this text area.
	 *
	 * @return The height of a line of text in this text area.
	 */
	@Override
	public int getLineHeight() {
		// System.err.println("... getLineHeight() returning " + lineHeight);
		return this.lineHeight;
	}

	public LinkGenerator getLinkGenerator() {
		return this.linkGenerator;
	}

	/**
	 * Returns a list of "mark all" highlights in the text area. If there are no
	 * such highlights, this will be an empty list.
	 *
	 * @return The list of "mark all" highlight ranges.
	 */
	public List<DocumentRange> getMarkAllHighlightRanges() {
		return ((RSyntaxTextAreaHighlighter) this.getHighlighter()).getMarkAllHighlightRanges();
	}

	/**
	 * Returns a list of "marked occurrences" in the text area. If there are no
	 * marked occurrences, this will be an empty list.
	 *
	 * @return The list of marked occurrences.
	 */
	public List<DocumentRange> getMarkedOccurrences() {
		return ((RSyntaxTextAreaHighlighter) this.getHighlighter()).getMarkedOccurrences();
	}

	/**
	 * Returns whether "Mark Occurrences" is enabled.
	 *
	 * @return Whether "Mark Occurrences" is enabled.
	 * @see #setMarkOccurrences(boolean)
	 */
	public boolean getMarkOccurrences() {
		return this.markOccurrencesSupport != null;
	}

	/**
	 * Returns the color used for "mark occurrences" highlights.
	 *
	 * @return The mark occurrences color.
	 * @see #setMarkOccurrencesColor(Color)
	 */
	public Color getMarkOccurrencesColor() {
		return this.markOccurrencesColor;
	}

	/**
	 * Returns the delay between when the caret is moved and when "marked
	 * occurrences" are highlighted.
	 *
	 * @return The "mark occurrences" delay.
	 * @see #setMarkOccurrencesDelay(int)
	 */
	public int getMarkOccurrencesDelay() {
		return this.markOccurrencesDelay;
	}

	/**
	 * Returns whether tokens of the specified type should have "mark occurrences"
	 * enabled for the current programming language.
	 *
	 * @param type
	 *            The token type.
	 * @return Whether tokens of this type should have "mark occurrences" enabled.
	 */
	boolean getMarkOccurrencesOfTokenType(final int type) {
		final RSyntaxDocument doc = (RSyntaxDocument) this.getDocument();
		return doc.getMarkOccurrencesOfTokenType(type);
	}

	/**
	 * Gets the color used as the background for a matched bracket.
	 *
	 * @return The color used. If this is <code>null</code>, no special background
	 *         is painted behind a matched bracket.
	 * @see #setMatchedBracketBGColor
	 * @see #getMatchedBracketBorderColor
	 */
	public Color getMatchedBracketBGColor() {
		return this.matchedBracketBGColor;
	}

	/**
	 * Gets the color used as the border for a matched bracket.
	 *
	 * @return The color used.
	 * @see #setMatchedBracketBorderColor
	 * @see #getMatchedBracketBGColor
	 */
	public Color getMatchedBracketBorderColor() {
		return this.matchedBracketBorderColor;
	}

	/**
	 * Returns the matched bracket's rectangle, or <code>null</code> if there is
	 * currently no matched bracket. This should never be called by the programmer
	 * directly.
	 *
	 * @return The rectangle surrounding the matched bracket.
	 * @see #getDotRectangle()
	 */
	Rectangle getMatchRectangle() {
		return this.match;
	}

	/**
	 * Overridden to return the max ascent for any font used in the editor.
	 *
	 * @return The max ascent value.
	 */
	@Override
	public int getMaxAscent() {
		return this.maxAscent;
	}

	/**
	 * Returns whether a border is painted around marked occurrences.
	 *
	 * @return Whether a border is painted.
	 * @see #setPaintMarkOccurrencesBorder(boolean)
	 * @see #getMarkOccurrencesColor()
	 * @see #getMarkOccurrences()
	 */
	public boolean getPaintMarkOccurrencesBorder() {
		return this.paintMarkOccurrencesBorder;
	}

	/**
	 * Returns whether the bracket at the caret position is painted as a "match"
	 * when a matched bracket is found. Note that this property does nothing if
	 * {@link #isBracketMatchingEnabled()} returns <code>false</code>.
	 *
	 * @return Whether both brackets in a bracket pair are highlighted when bracket
	 *         matching is enabled.
	 * @see #setPaintMatchedBracketPair(boolean)
	 * @see #isBracketMatchingEnabled()
	 * @see #setBracketMatchingEnabled(boolean)
	 */
	public boolean getPaintMatchedBracketPair() {
		return this.paintMatchedBracketPair;
	}

	/**
	 * Returns whether tab lines are painted.
	 *
	 * @return Whether tab lines are painted.
	 * @see #setPaintTabLines(boolean)
	 * @see #getTabLineColor()
	 */
	public boolean getPaintTabLines() {
		return this.paintTabLines;
	}

	/**
	 * Returns whether to paint the backgrounds of tokens on the specified line
	 * (assuming they are not obstructed by e.g. selection).
	 *
	 * @param line
	 *            The line number.
	 * @param y
	 *            The y-offset of the line. This is used when line wrap is enabled,
	 *            since each logical line can be rendered as several physical lines.
	 * @return Whether to paint the token backgrounds on this line.
	 */
	boolean getPaintTokenBackgrounds(final int line, final float y) {
		// System.out.println(y + ", " + getCurrentCaretY() + "-" + (getCurrentCaretY()
		// + getLineHeight()));
		final int iy = (int) y;
		final int curCaretY = this.getCurrentCaretY();
		return iy < curCaretY || iy >= curCaretY + this.getLineHeight() || !this.getHighlightCurrentLine();
	}

	/**
	 * Returns the specified parser.
	 *
	 * @param index
	 *            The {@link Parser} to retrieve.
	 * @return The <code>Parser</code>.
	 * @see #getParserCount()
	 * @see #addParser(Parser)
	 */
	public Parser getParser(final int index) {
		return this.parserManager.getParser(index);
	}

	/**
	 * Returns the number of parsers operating on this text area.
	 *
	 * @return The parser count.
	 * @see #addParser(Parser)
	 */
	public int getParserCount() {
		return this.parserManager == null ? 0 : this.parserManager.getParserCount();
	}

	/**
	 * Returns the currently set parser delay. This is the delay that must occur
	 * between edits for any registered {@link Parser}s to run.
	 *
	 * @return The currently set parser delay, in milliseconds.
	 * @see #setParserDelay(int)
	 */
	public int getParserDelay() {
		return this.parserManager.getDelay();
	}

	/**
	 * Returns a list of the current parser notices for this text area. This method
	 * (like most Swing methods) should only be called on the EDT.
	 *
	 * @return The list of notices. This will be an empty list if there are none.
	 */
	public List<ParserNotice> getParserNotices() {
		if (this.parserManager == null)
			return Collections.emptyList();
		return this.parserManager.getParserNotices();
	}

	/**
	 * Workaround for JTextComponents allowing the caret to be rendered entirely
	 * off-screen if the entire "previous" character fit entirely.
	 *
	 * @return The amount of space to add to the x-axis preferred span.
	 * @see #setRightHandSideCorrection(int)
	 */
	public int getRightHandSideCorrection() {
		return this.rhsCorrection;
	}

	/**
	 * Returns the background color for the specified secondary language.
	 *
	 * @param index
	 *            The language index. Note that these are 1-based, not 0-based, and
	 *            should be in the range <code>1-getSecondaryLanguageCount()</code>,
	 *            inclusive.
	 * @return The color, or <code>null</code> if none.
	 * @see #getSecondaryLanguageCount()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getHighlightSecondaryLanguages()
	 */
	public Color getSecondaryLanguageBackground(final int index) {
		return this.secondaryLanguageBackgrounds[index - 1];
	}

	/**
	 * Returns the number of secondary language backgrounds.
	 *
	 * @return The number of secondary language backgrounds.
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getHighlightSecondaryLanguages()
	 */
	public int getSecondaryLanguageCount() {
		return this.secondaryLanguageBackgrounds.length;
	}

	/**
	 * If auto-indent is enabled, this method returns whether a new line after this
	 * one should be indented (based on the standard indentation rules for the
	 * current programming language). For example, in Java, for a line containing:
	 *
	 * <pre>
	 * for (int i=0; i&lt;10; i++) {
	 * </pre>
	 *
	 * the following line should be indented.
	 *
	 * @param line
	 *            The line to check.
	 * @return Whether a line inserted after this one should be auto-indented. If
	 *         auto-indentation is disabled, this will always return
	 *         <code>false</code>.
	 * @see #isAutoIndentEnabled()
	 */
	public boolean getShouldIndentNextLine(final int line) {
		if (this.isAutoIndentEnabled()) {
			final RSyntaxDocument doc = (RSyntaxDocument) this.getDocument();
			return doc.getShouldIndentNextLine(line);
		}
		return false;
	}

	/**
	 * Returns whether a small popup window should display the text on the line
	 * containing a matched bracket whenever a matched bracket is off- screen.
	 *
	 * @return Whether to show the popup.
	 * @see #setShowMatchedBracketPopup(boolean)
	 */
	public boolean getShowMatchedBracketPopup() {
		return this.showMatchedBracketPopup;
	}

	/**
	 * Returns what type of syntax highlighting this editor is doing.
	 *
	 * @return The style being used, such as
	 *         {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.
	 * @see #setSyntaxEditingStyle(String)
	 * @see SyntaxConstants
	 */
	public String getSyntaxEditingStyle() {
		return this.syntaxStyleKey;
	}

	/**
	 * Returns all of the colors currently being used in syntax highlighting by this
	 * text component.
	 *
	 * @return An instance of <code>SyntaxScheme</code> that represents the colors
	 *         currently being used for syntax highlighting.
	 * @see #setSyntaxScheme(SyntaxScheme)
	 */
	public SyntaxScheme getSyntaxScheme() {
		return this.syntaxScheme;
	}

	/**
	 * Returns the color used to paint tab lines.
	 *
	 * @return The color used to paint tab lines.
	 * @see #setTabLineColor(Color)
	 * @see #getPaintTabLines()
	 * @see #setPaintTabLines(boolean)
	 */
	public Color getTabLineColor() {
		return this.tabLineColor;
	}

	/**
	 * Returns a token list for the given range in the document.
	 *
	 * @param startOffs
	 *            The starting offset in the document.
	 * @param endOffs
	 *            The end offset in the document.
	 * @return The first token in the token list.
	 */
	private Token getTokenListFor(final int startOffs, final int endOffs) {

		TokenImpl tokenList = null;
		TokenImpl lastToken = null;

		final Element map = this.getDocument().getDefaultRootElement();
		final int startLine = map.getElementIndex(startOffs);
		final int endLine = map.getElementIndex(endOffs);

		for (int line = startLine; line <= endLine; line++) {
			TokenImpl t = (TokenImpl) this.getTokenListForLine(line);
			t = this.cloneTokenList(t);
			if (tokenList == null) {
				tokenList = t;
				lastToken = tokenList;
			} else
				lastToken.setNextToken(t);
			while (lastToken.getNextToken() != null && lastToken.getNextToken().isPaintable())
				lastToken = (TokenImpl) lastToken.getNextToken();
			if (line < endLine) {
				// Document offset MUST be correct to prevent exceptions
				// in getTokenListFor()
				final int docOffs = map.getElement(line).getEndOffset() - 1;
				t = new TokenImpl(new char[] { '\n' }, 0, 0, docOffs, TokenTypes.WHITESPACE, 0);
				lastToken.setNextToken(t);
				lastToken = t;
			}
		}

		// Trim the beginning and end of the token list so that it starts
		// at startOffs and ends at endOffs.

		// Be careful and check that startOffs is actually in the list.
		// startOffs can be < the token list's start if the end "newline"
		// character of a line is the first character selected (the token
		// list returned for that line will be null, so the first token in
		// the final token list will be from the next line and have a
		// starting offset > startOffs?).
		if (startOffs >= tokenList.getOffset()) {
			while (!tokenList.containsPosition(startOffs))
				tokenList = (TokenImpl) tokenList.getNextToken();
			tokenList.makeStartAt(startOffs);
		}

		TokenImpl temp = tokenList;
		// Be careful to check temp for null here. It is possible that no
		// token contains endOffs, if endOffs is at the end of a line.
		while (temp != null && !temp.containsPosition(endOffs))
			temp = (TokenImpl) temp.getNextToken();
		if (temp != null) {
			temp.textCount = endOffs - temp.getOffset();
			temp.setNextToken(null);
		}

		return tokenList;

	}

	/**
	 * Returns a list of tokens representing the given line.
	 *
	 * @param line
	 *            The line number to get tokens for.
	 * @return A linked list of tokens representing the line's text.
	 */
	public Token getTokenListForLine(final int line) {
		return ((RSyntaxDocument) this.getDocument()).getTokenListForLine(line);
	}

	/**
	 * Returns the painter to use for rendering tokens.
	 *
	 * @return The painter to use for rendering tokens.
	 */
	TokenPainter getTokenPainter() {
		return this.tokenPainter;
	}

	/**
	 * Returns the tool tip to display for a mouse event at the given location. This
	 * method is overridden to give a registered parser a chance to display a tool
	 * tip (such as an error description when the mouse is over an error highlight).
	 *
	 * @param e
	 *            The mouse event.
	 */
	@Override
	public String getToolTipText(final MouseEvent e) {

		// Apple JVMS (Java 6 and prior) have their ToolTipManager events
		// repeat for some reason, so this method gets called every 1 second
		// or so. We short-circuit that since some ToolTipManagers may do
		// expensive calculations (e.g. language supports).
		if (RSyntaxUtilities.getOS() == RSyntaxUtilities.OS_MAC_OSX) {
			final Point newLoc = e.getPoint();
			if (newLoc != null && newLoc.equals(this.cachedTipLoc))
				return this.cachedTip;
			this.cachedTipLoc = newLoc;
		}

		return this.cachedTip = this.getToolTipTextImpl(e);

	}

	/**
	 * Does the dirty work of getting the tool tip text.
	 *
	 * @param e
	 *            The mouse event.
	 * @return The tool tip text.
	 */
	protected String getToolTipTextImpl(final MouseEvent e) {

		// Check parsers for tool tips first.
		String text = null;
		URL imageBase = null;
		if (this.parserManager != null) {
			final ToolTipInfo info = this.parserManager.getToolTipText(e);
			if (info != null) { // Should always be true
				text = info.getToolTipText(); // May be null
				imageBase = info.getImageBase(); // May be null
			}
		}
		if (text == null)
			text = super.getToolTipText(e);

		// Do we want to use "focusable" tips?
		if (this.getUseFocusableTips()) {
			if (text != null) {
				if (this.focusableTip == null)
					this.focusableTip = new FocusableTip(this, this.parserManager);
				this.focusableTip.setImageBase(imageBase);
				this.focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (this.focusableTip != null)
				this.focusableTip.possiblyDisposeOfTipWindow();
			return null;
		}

		return text; // Standard tool tips

	}

	/**
	 * Returns whether the specified token should be underlined. A token is
	 * underlined if its syntax style includes underlining, or if it is a hyperlink
	 * and hyperlinks are enabled.
	 *
	 * @param t
	 *            The token.
	 * @return Whether the specified token should be underlined.
	 */
	public boolean getUnderlineForToken(final Token t) {
		return this.getHyperlinksEnabled()
				&& (t.isHyperlink() || this.linkGeneratorResult != null
						&& this.linkGeneratorResult.getSourceOffset() == t.getOffset())
				|| this.syntaxScheme.getStyle(t.getType()).underline;
	}

	/**
	 * Returns whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on, resize, copy
	 * from, and click links in.
	 *
	 * @return Whether to use focusable tool tips.
	 * @see #setUseFocusableTips(boolean)
	 * @see FocusableTip
	 */
	public boolean getUseFocusableTips() {
		return this.useFocusableTips;
	}

	/**
	 * Returns whether selected text should use the "selected text color" property
	 * set via {@link #setSelectedTextColor(Color)}. This is the typical behavior of
	 * text components. By default, RSyntaxTextArea does not do this, so that token
	 * styles are visible even in selected regions of text.
	 *
	 * @return Whether the "selected text" color is used when painting text in
	 *         selected regions.
	 * @see #setUseSelectedTextColor(boolean)
	 */
	public boolean getUseSelectedTextColor() {
		return this.useSelectedTextColor;
	}

	/**
	 * Called by constructors to initialize common properties of the text editor.
	 */
	@Override
	protected void init() {

		super.init();
		this.metricsNeverRefreshed = true;

		this.tokenPainter = new DefaultTokenPainter();

		// NOTE: Our actions are created here instead of in a static block
		// so they are only created when the first RTextArea is instantiated,
		// not before. There have been reports of users calling static getters
		// (e.g. RSyntaxTextArea.getDefaultBracketMatchBGColor()) which would
		// cause these actions to be created and (possibly) incorrectly
		// localized, if they were in a static block.
		if (RSyntaxTextArea.toggleCurrentFoldAction == null)
			RSyntaxTextArea.createRstaPopupMenuActions();

		// Set some RSyntaxTextArea default values.
		this.syntaxStyleKey = SyntaxConstants.SYNTAX_STYLE_NONE;
		this.setMatchedBracketBGColor(RSyntaxTextArea.getDefaultBracketMatchBGColor());
		this.setMatchedBracketBorderColor(RSyntaxTextArea.getDefaultBracketMatchBorderColor());
		this.setBracketMatchingEnabled(true);
		this.setAnimateBracketMatching(true);
		this.lastBracketMatchPos = -1;
		this.setSelectionColor(RSyntaxTextArea.getDefaultSelectionColor());
		this.setTabLineColor(null);
		this.setMarkOccurrencesColor(MarkOccurrencesSupport.DEFAULT_COLOR);
		this.setMarkOccurrencesDelay(MarkOccurrencesSupport.DEFAULT_DELAY_MS);

		this.foldManager = new DefaultFoldManager(this);

		// Set auto-indent related stuff.
		this.setAutoIndentEnabled(true);
		this.setCloseCurlyBraces(true);
		this.setCloseMarkupTags(true);
		this.setClearWhitespaceLinesEnabled(true);

		this.setHyperlinksEnabled(true);
		this.setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);
		this.setHyperlinkForeground(Color.BLUE);
		this.isScanningForLinks = false;
		this.setUseFocusableTips(true);

		// setAntiAliasingEnabled(true);
		this.setDefaultAntiAliasingState();
		this.restoreDefaultSyntaxScheme();

		this.setHighlightSecondaryLanguages(true);
		this.secondaryLanguageBackgrounds = new Color[3];
		this.secondaryLanguageBackgrounds[0] = new Color(0xfff0cc);
		this.secondaryLanguageBackgrounds[1] = new Color(0xdafeda);
		this.secondaryLanguageBackgrounds[2] = new Color(0xffe0f0);

		this.setRightHandSideCorrection(0);
		this.setShowMatchedBracketPopup(true);

	}

	/**
	 * Returns whether or not auto-indent is enabled.
	 *
	 * @return Whether or not auto-indent is enabled.
	 * @see #setAutoIndentEnabled(boolean)
	 */
	public boolean isAutoIndentEnabled() {
		return this.autoIndentEnabled;
	}

	/**
	 * Returns whether or not bracket matching is enabled.
	 *
	 * @return <code>true</code> iff bracket matching is enabled.
	 * @see #setBracketMatchingEnabled
	 */
	public final boolean isBracketMatchingEnabled() {
		return this.bracketMatchingEnabled;
	}

	/**
	 * Returns whether or not lines containing nothing but whitespace are made into
	 * blank lines when Enter is pressed in them.
	 *
	 * @return Whether or not whitespace-only lines are cleared when the user
	 *         presses Enter on them.
	 * @see #setClearWhitespaceLinesEnabled(boolean)
	 */
	public boolean isClearWhitespaceLinesEnabled() {
		return this.clearWhitespaceLines;
	}

	/**
	 * Returns whether code folding is enabled. Note that only certain languages
	 * support code folding; those that do not will ignore this property.
	 *
	 * @return Whether code folding is enabled.
	 * @see #setCodeFoldingEnabled(boolean)
	 */
	public boolean isCodeFoldingEnabled() {
		return this.foldManager.isCodeFoldingEnabled();
	}

	/**
	 * Returns whether whitespace (spaces and tabs) is visible.
	 *
	 * @return Whether whitespace is visible.
	 * @see #setWhitespaceVisible(boolean)
	 * @see #getEOLMarkersVisible()
	 */
	public boolean isWhitespaceVisible() {
		return this.whitespaceVisible;
	}

	/**
	 * Returns the token at the specified position in the model.
	 *
	 * @param offs
	 *            The position in the model.
	 * @return The token, or <code>null</code> if no token is at that position.
	 * @see #viewToToken(Point)
	 */
	public Token modelToToken(final int offs) {
		if (offs >= 0)
			try {
				final int line = this.getLineOfOffset(offs);
				final Token t = this.getTokenListForLine(line);
				return RSyntaxUtilities.getTokenAtOffset(t, offs);
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		return null;
	}

	/**
	 * The <code>paintComponent</code> method is overridden so we apply any
	 * necessary rendering hints to the Graphics object.
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		// A call to refreshFontMetrics() used to be in addNotify(), but
		// unfortunately we cannot always get the graphics context there. If
		// the parent frame/dialog is LAF-decorated, there is a chance that the
		// window's width and/or height is still == 0 at addNotify() (e.g.
		// WebLaF). So unfortunately it's safest to do this here, with a flag
		// to only allow it to happen once.
		if (this.metricsNeverRefreshed) {
			this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
			this.metricsNeverRefreshed = false;
		}

		super.paintComponent(this.getGraphics2D(g));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void redoLastAction() {
		super.redoLastAction();
		// Occasionally marked occurrences' Positions are in invalid states
		// due to how javax.swing.text.AbstractDocument tracks the start and
		// end offsets. This is usually not needed, but can be when the last
		// token in the Document is a marked occurrence, and an undo or redo
		// occurs which clears most of the document text. In that case it is
		// possible for the end Position to be reset to something small, but
		// the start offset to be its prior valid (start > end).
		((RSyntaxTextAreaHighlighter) this.getHighlighter()).clearMarkOccurrencesHighlights();
	}

	private void refreshFontMetrics(final Graphics2D g2d) {
		// It is assumed that any rendering hints are already applied to g2d.
		this.defaultFontMetrics = g2d.getFontMetrics(this.getFont());
		this.syntaxScheme.refreshFontMetrics(g2d);
		if (!this.getLineWrap()) {
			// HORRIBLE HACK! The un-wrapped view needs to refresh its cached
			// longest line information.
			final SyntaxView sv = (SyntaxView) this.getUI().getRootView(this).getView(0);
			sv.calculateLongestLine();
		}
	}

	/**
	 * Removes an "active line range" listener from this text area.
	 *
	 * @param l
	 *            The listener to remove.
	 * @see #removeActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void removeActiveLineRangeListener(final ActiveLineRangeListener l) {
		this.listenerList.remove(ActiveLineRangeListener.class, l);
	}

	/**
	 * Removes a hyperlink listener from this text area.
	 *
	 * @param l
	 *            The listener to remove.
	 * @see #addHyperlinkListener(HyperlinkListener)
	 */
	public void removeHyperlinkListener(final HyperlinkListener l) {
		this.listenerList.remove(HyperlinkListener.class, l);
	}

	/**
	 * Overridden so we stop this text area's parsers, if any.
	 */
	@Override
	public void removeNotify() {
		if (this.parserManager != null)
			this.parserManager.stopParsing();
		super.removeNotify();
	}

	/**
	 * Removes a parser from this text area.
	 *
	 * @param parser
	 *            The {@link Parser} to remove.
	 * @return Whether the parser was found and removed.
	 * @see #clearParsers()
	 * @see #addParser(Parser)
	 * @see #getParser(int)
	 */
	public boolean removeParser(final Parser parser) {
		boolean removed = false;
		if (this.parserManager != null)
			removed = this.parserManager.removeParser(parser);
		return removed;
	}

	/**
	 * Sets the colors used for syntax highlighting to their defaults.
	 *
	 * @see #setSyntaxScheme(SyntaxScheme)
	 * @see #getSyntaxScheme()
	 * @see #getDefaultSyntaxScheme()
	 */
	public void restoreDefaultSyntaxScheme() {
		this.setSyntaxScheme(this.getDefaultSyntaxScheme());
	}

	/**
	 * Sets the "active line range." Note that this <code>RSyntaxTextArea</code>
	 * itself does nothing with this information, but if it is contained inside an
	 * {@link org.fife.ui.rtextarea.RTextScrollPane}, the active line range may be
	 * displayed in the icon area of the {@link org.fife.ui.rtextarea.Gutter}.
	 * <p>
	 *
	 * Note that basic users of <code>RSyntaxTextArea</code> will not call this
	 * method directly; rather, it is usually called by instances of
	 * <code>LanguageSupport</code> in the <code>RSTALangaugeSupport</code> library.
	 * See <a href="http://fifesoft.com">http://fifesoft.com</a> for more
	 * information about this library.
	 *
	 * @param min
	 *            The "minimum" line in the active line range, or <code>-1</code> if
	 *            the range is being cleared.
	 * @param max
	 *            The "maximum" line in the active line range, or <code>-1</code> if
	 *            the range is being cleared.
	 * @see #addActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void setActiveLineRange(final int min, int max) {
		if (min == -1)
			max = -1; // Force max to be -1 if min is.
		this.fireActiveLineRangeEvent(min, max);
	}

	/**
	 * Sets whether bracket matching should be animated. This fires a property
	 * change event of type {@link #ANIMATE_BRACKET_MATCHING_PROPERTY}.
	 *
	 * @param animate
	 *            Whether to animate bracket matching.
	 * @see #getAnimateBracketMatching()
	 */
	public void setAnimateBracketMatching(final boolean animate) {
		if (animate != this.animateBracketMatching) {
			this.animateBracketMatching = animate;
			if (animate && this.bracketRepaintTimer == null)
				this.bracketRepaintTimer = new BracketMatchingTimer();
			this.firePropertyChange(RSyntaxTextArea.ANIMATE_BRACKET_MATCHING_PROPERTY, !animate, animate);
		}
	}

	/**
	 * Sets whether anti-aliasing is enabled in this editor. This method fires a
	 * property change event of type {@link #ANTIALIAS_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether anti-aliasing is enabled.
	 * @see #getAntiAliasingEnabled()
	 */
	public void setAntiAliasingEnabled(final boolean enabled) {

		final boolean currentlyEnabled = this.aaHints != null;

		if (enabled != currentlyEnabled) {

			if (enabled) {
				this.aaHints = RSyntaxUtilities.getDesktopAntiAliasHints();
				// If the desktop query method comes up empty, use the standard
				// Java2D greyscale method. Note this will likely NOT be as
				// nice as what would be used if the getDesktopAntiAliasHints()
				// call worked.
				if (this.aaHints == null) {
					final Map<RenderingHints.Key, Object> temp = new HashMap<>();
					temp.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					this.aaHints = temp;
				}
			} else
				this.aaHints = null;

			// We must be connected to a screen resource for our graphics
			// to be non-null.
			if (this.isDisplayable())
				this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
			this.firePropertyChange(RSyntaxTextArea.ANTIALIAS_PROPERTY, !enabled, enabled);
			this.repaint();

		}

	}

	/**
	 * Sets whether or not auto-indent is enabled. This fires a property change
	 * event of type {@link #AUTO_INDENT_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether or not auto-indent is enabled.
	 * @see #isAutoIndentEnabled()
	 */
	public void setAutoIndentEnabled(final boolean enabled) {
		if (this.autoIndentEnabled != enabled) {
			this.autoIndentEnabled = enabled;
			this.firePropertyChange(RSyntaxTextArea.AUTO_INDENT_PROPERTY, !enabled, enabled);
		}
	}

	/**
	 * Sets whether bracket matching is enabled. This fires a property change event
	 * of type {@link #BRACKET_MATCHING_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether or not bracket matching should be enabled.
	 * @see #isBracketMatchingEnabled()
	 */
	public void setBracketMatchingEnabled(final boolean enabled) {
		if (enabled != this.bracketMatchingEnabled) {
			this.bracketMatchingEnabled = enabled;
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.BRACKET_MATCHING_PROPERTY, !enabled, enabled);
		}
	}

	/**
	 * Sets whether or not lines containing nothing but whitespace are made into
	 * blank lines when Enter is pressed in them. This method fires a property
	 * change event of type {@link #CLEAR_WHITESPACE_LINES_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether or not whitespace-only lines are cleared when the user
	 *            presses Enter on them.
	 * @see #isClearWhitespaceLinesEnabled()
	 */
	public void setClearWhitespaceLinesEnabled(final boolean enabled) {
		if (enabled != this.clearWhitespaceLines) {
			this.clearWhitespaceLines = enabled;
			this.firePropertyChange(RSyntaxTextArea.CLEAR_WHITESPACE_LINES_PROPERTY, !enabled, enabled);
		}
	}

	/**
	 * Toggles whether curly braces should be automatically closed when a newline is
	 * entered after an opening curly brace. Note that this property is only honored
	 * for languages that use curly braces to denote code blocks.
	 * <p>
	 *
	 * This method fires a property change event of type
	 * {@link #CLOSE_CURLY_BRACES_PROPERTY}.
	 *
	 * @param close
	 *            Whether curly braces should be automatically closed.
	 * @see #getCloseCurlyBraces()
	 */
	public void setCloseCurlyBraces(final boolean close) {
		if (close != this.closeCurlyBraces) {
			this.closeCurlyBraces = close;
			this.firePropertyChange(RSyntaxTextArea.CLOSE_CURLY_BRACES_PROPERTY, !close, close);
		}
	}

	/**
	 * Sets whether closing markup tags should be automatically completed when
	 * "<code>&lt;/</code>" is typed. Note that this property is only honored for
	 * markup languages, such as HTML, XML and PHP.
	 * <p>
	 *
	 * This method fires a property change event of type
	 * {@link #CLOSE_MARKUP_TAGS_PROPERTY}.
	 *
	 * @param close
	 *            Whether closing markup tags should be automatically completed.
	 * @see #getCloseMarkupTags()
	 */
	public void setCloseMarkupTags(final boolean close) {
		if (close != this.closeMarkupTags) {
			this.closeMarkupTags = close;
			this.firePropertyChange(RSyntaxTextArea.CLOSE_MARKUP_TAGS_PROPERTY, !close, close);
		}
	}

	/**
	 * Sets whether code folding is enabled. Note that only certain languages will
	 * support code folding out of the box. Those languages which do not support
	 * folding will ignore this property.
	 * <p>
	 * This method fires a property change event of type
	 * {@link #CODE_FOLDING_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether code folding should be enabled.
	 * @see #isCodeFoldingEnabled()
	 */
	public void setCodeFoldingEnabled(final boolean enabled) {
		if (enabled != this.foldManager.isCodeFoldingEnabled()) {
			this.foldManager.setCodeFoldingEnabled(enabled);
			this.firePropertyChange(RSyntaxTextArea.CODE_FOLDING_PROPERTY, !enabled, enabled);
		}
	}

	/**
	 * Sets anti-aliasing to whatever the user's desktop value is.
	 *
	 * @see #getAntiAliasingEnabled()
	 */
	private void setDefaultAntiAliasingState() {

		// Most accurate technique, but not available on all OSes.
		this.aaHints = RSyntaxUtilities.getDesktopAntiAliasHints();
		if (this.aaHints == null) {

			final Map<RenderingHints.Key, Object> temp = new HashMap<>();

			// In Java 6+, you can figure out what text AA hint Swing uses for
			// JComponents...
			final JLabel label = new JLabel();
			final FontMetrics fm = label.getFontMetrics(label.getFont());
			Object hint = null;
			// FontRenderContext frc = fm.getFontRenderContext();
			// hint = fm.getAntiAliasingHint();
			try {
				Method m = FontMetrics.class.getMethod("getFontRenderContext");
				final FontRenderContext frc = (FontRenderContext) m.invoke(fm);
				m = FontRenderContext.class.getMethod("getAntiAliasingHint");
				hint = m.invoke(frc);
			} catch (final RuntimeException re) {
				throw re; // FindBugs
			} catch (final Exception e) {
				// Swallow, either Java 1.5, or running in an applet
			}

			// If not running Java 6+, default to AA enabled on Windows where
			// the software AA is pretty fast, and default (e.g. disabled) on
			// non-Windows. Note that OS X always uses AA no matter what
			// rendering hints you give it, so this is a moot point there.
			// System.out.println("Rendering hint: " + hint);
			if (hint == null) {
				final String os = System.getProperty("os.name").toLowerCase();
				if (os.contains("windows"))
					hint = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
				else
					hint = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
			}
			temp.put(RenderingHints.KEY_TEXT_ANTIALIASING, hint);

			this.aaHints = temp;

		}

		// We must be connected to a screen resource for our graphics
		// to be non-null.
		if (this.isDisplayable())
			this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
		this.repaint();

	}

	/**
	 * Sets the document used by this text area. This is overridden so that only
	 * instances of {@link RSyntaxDocument} are accepted; for all others, an
	 * exception will be thrown.
	 *
	 * @param document
	 *            The new document for this text area.
	 * @throws IllegalArgumentException
	 *             If the document is not an <code>RSyntaxDocument</code>.
	 */
	@Override
	public void setDocument(final Document document) {
		if (!(document instanceof RSyntaxDocument))
			throw new IllegalArgumentException(
					"Documents for " + "RSyntaxTextArea must be instances of " + "RSyntaxDocument!");
		if (this.markOccurrencesSupport != null)
			this.markOccurrencesSupport.clear();
		super.setDocument(document);
		this.setSyntaxEditingStyle(((RSyntaxDocument) document).getSyntaxStyle());
		if (this.markOccurrencesSupport != null)
			this.markOccurrencesSupport.doMarkOccurrences();
	}

	/**
	 * Sets whether EOL markers are visible at the end of each line. This method
	 * fires a property change of type {@link #EOL_VISIBLE_PROPERTY}.
	 *
	 * @param visible
	 *            Whether EOL markers are visible.
	 * @see #getEOLMarkersVisible()
	 * @see #setWhitespaceVisible(boolean)
	 */
	public void setEOLMarkersVisible(final boolean visible) {
		if (visible != this.eolMarkersVisible) {
			this.eolMarkersVisible = visible;
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.EOL_VISIBLE_PROPERTY, !visible, visible);
		}
	}

	/**
	 * Sets the font used by this text area. Note that if some token styles are
	 * using a different font, they will not be changed by calling this method. To
	 * set different fonts on individual token types, use the text area's
	 * <code>SyntaxScheme</code>.
	 *
	 * @param font
	 *            The font.
	 * @see SyntaxScheme#getStyle(int)
	 */
	@Override
	public void setFont(final Font font) {

		final Font old = super.getFont();
		super.setFont(font); // Do this first.

		// Usually programmers keep a single font for all token types, but
		// may use bold or italic for styling some.
		final SyntaxScheme scheme = this.getSyntaxScheme();
		if (scheme != null && old != null) {
			scheme.changeBaseFont(old, font);
			this.calculateLineHeight();
		}

		// We must be connected to a screen resource for our
		// graphics to be non-null.
		if (this.isDisplayable()) {
			this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
			// Updates the margin line.
			this.updateMarginLineX();
			// Force the current line highlight to be repainted, even
			// though the caret's location hasn't changed.
			this.forceCurrentLineHighlightRepaint();
			// Get line number border in text area to repaint again
			// since line heights have updated.
			this.firePropertyChange("font", old, font);
			// So parent JScrollPane will have its scrollbars updated.
			this.revalidate();
		}

	}

	/**
	 * Sets whether fractional font metrics are enabled. This method fires a
	 * property change event of type {@link #FRACTIONAL_FONTMETRICS_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether fractional font metrics are enabled.
	 * @see #getFractionalFontMetricsEnabled()
	 */
	public void setFractionalFontMetricsEnabled(final boolean enabled) {
		if (this.fractionalFontMetricsEnabled != enabled) {
			this.fractionalFontMetricsEnabled = enabled;
			// We must be connected to a screen resource for our graphics to be
			// non-null.
			if (this.isDisplayable())
				this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));
			this.firePropertyChange(RSyntaxTextArea.FRACTIONAL_FONTMETRICS_PROPERTY, !enabled, enabled);
		}
	}

	/**
	 * Sets the highlighter used by this text area.
	 *
	 * @param h
	 *            The highlighter.
	 * @throws IllegalArgumentException
	 *             If <code>h</code> is not an instance of
	 *             {@link RSyntaxTextAreaHighlighter}.
	 */
	@Override
	public void setHighlighter(Highlighter h) {

		// Ugh, many RSTA methods assume a non-null highlighter. This is kind
		// of icky, but most applications never *don't* want a highlighter.
		// See #189 - BasicTextUI clears highlighter by setting it to null there
		if (h == null)
			h = new RSyntaxTextAreaHighlighter();

		if (!(h instanceof RSyntaxTextAreaHighlighter))
			throw new IllegalArgumentException(
					"RSyntaxTextArea requires " + "an RSyntaxTextAreaHighlighter for its Highlighter");
		super.setHighlighter(h);
	}

	/**
	 * Sets whether "secondary" languages should have their backgrounds colored
	 * differently to visually differentiate them. This feature imposes a fair
	 * performance penalty. This method fires a property change event of type
	 * {@link #HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY}.
	 *
	 * @see #getHighlightSecondaryLanguages()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getSecondaryLanguageCount()
	 */
	public void setHighlightSecondaryLanguages(final boolean highlight) {
		if (this.highlightSecondaryLanguages != highlight) {
			this.highlightSecondaryLanguages = highlight;
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY, !highlight, highlight);
		}
	}

	/**
	 * Sets the color to use when painting hyperlinks.
	 *
	 * @param fg
	 *            The color to use when painting hyperlinks.
	 * @throws NullPointerException
	 *             If <code>fg</code> is <code>null</code>.
	 * @see #getHyperlinkForeground()
	 * @see #setHyperlinksEnabled(boolean)
	 */
	public void setHyperlinkForeground(final Color fg) {
		if (fg == null)
			throw new NullPointerException("fg cannot be null");
		this.hyperlinkFG = fg;
	}

	/**
	 * Sets whether hyperlinks are enabled for this text area. This method fires a
	 * property change event of type {@link #HYPERLINKS_ENABLED_PROPERTY}.
	 *
	 * @param enabled
	 *            Whether hyperlinks are enabled.
	 * @see #getHyperlinksEnabled()
	 */
	public void setHyperlinksEnabled(final boolean enabled) {
		if (this.hyperlinksEnabled != enabled) {
			this.hyperlinksEnabled = enabled;
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.HYPERLINKS_ENABLED_PROPERTY, !enabled, enabled);
		}
	}

	public void setLinkGenerator(final LinkGenerator generator) {
		this.linkGenerator = generator;
	}

	/**
	 * Sets the mask for the key used to toggle whether we are scanning for
	 * hyperlinks with mouse hovering. The default value is
	 * <code>CTRL_DOWN_MASK</code>.
	 *
	 * @param mask
	 *            The mask to use. This should be some bitwise combination of
	 *            {@link InputEvent#CTRL_DOWN_MASK},
	 *            {@link InputEvent#ALT_DOWN_MASK},
	 *            {@link InputEvent#SHIFT_DOWN_MASK} or
	 *            {@link InputEvent#META_DOWN_MASK}. For invalid values, behavior is
	 *            undefined.
	 * @see InputEvent
	 */
	public void setLinkScanningMask(int mask) {
		mask &= InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK
				| InputEvent.SHIFT_DOWN_MASK;
		if (mask == 0)
			throw new IllegalArgumentException(
					"mask argument should be " + "some combination of InputEvent.*_DOWN_MASK fields");
		this.linkScanningMask = mask;
	}

	/**
	 * Toggles whether "mark occurrences" is enabled. This method fires a property
	 * change event of type {@link #MARK_OCCURRENCES_PROPERTY}.
	 *
	 * @param markOccurrences
	 *            Whether "Mark Occurrences" should be enabled.
	 * @see #getMarkOccurrences()
	 * @see #setMarkOccurrencesColor(Color)
	 */
	public void setMarkOccurrences(final boolean markOccurrences) {
		if (markOccurrences) {
			if (this.markOccurrencesSupport == null) {
				this.markOccurrencesSupport = new MarkOccurrencesSupport();
				this.markOccurrencesSupport.install(this);
				this.firePropertyChange(RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, false, true);
			}
		} else if (this.markOccurrencesSupport != null) {
			this.markOccurrencesSupport.uninstall();
			this.markOccurrencesSupport = null;
			this.firePropertyChange(RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, true, false);
		}
	}

	/**
	 * Sets the "mark occurrences" color.
	 *
	 * @param color
	 *            The new color. This cannot be <code>null</code>.
	 * @see #getMarkOccurrencesColor()
	 * @see #setMarkOccurrences(boolean)
	 */
	public void setMarkOccurrencesColor(final Color color) {
		this.markOccurrencesColor = color;
		if (this.markOccurrencesSupport != null)
			this.markOccurrencesSupport.setColor(color);
	}

	/**
	 * Sets the delay between when the caret is moved and when "marked occurrences"
	 * are highlighted.
	 *
	 * @param delay
	 *            The new delay. This must be greater than {@code 0}.
	 * @see #getMarkOccurrencesDelay()
	 * @see #getMarkOccurrences()
	 */
	public void setMarkOccurrencesDelay(final int delay) {
		if (delay <= 0)
			throw new IllegalArgumentException("Delay must be > 0");
		if (delay != this.markOccurrencesDelay) {
			this.markOccurrencesDelay = delay;
			if (this.markOccurrencesSupport != null)
				this.markOccurrencesSupport.setDelay(delay);
		}
	}

	/**
	 * Sets the color used as the background for a matched bracket.
	 *
	 * @param color
	 *            The color to use. If this is <code>null</code>, then no special
	 *            background is painted behind a matched bracket.
	 * @see #getMatchedBracketBGColor
	 * @see #setMatchedBracketBorderColor
	 * @see #setPaintMarkOccurrencesBorder(boolean)
	 */
	public void setMatchedBracketBGColor(final Color color) {
		this.matchedBracketBGColor = color;
		if (this.match != null)
			this.repaint();
	}

	/**
	 * Sets the color used as the border for a matched bracket.
	 *
	 * @param color
	 *            The color to use.
	 * @see #getMatchedBracketBorderColor
	 * @see #setMatchedBracketBGColor
	 */
	public void setMatchedBracketBorderColor(final Color color) {
		this.matchedBracketBorderColor = color;
		if (this.match != null)
			this.repaint();
	}

	/**
	 * Toggles whether a border should be painted around marked occurrences.
	 *
	 * @param paintBorder
	 *            Whether to paint a border.
	 * @see #getPaintMarkOccurrencesBorder()
	 * @see #setMarkOccurrencesColor(Color)
	 * @see #setMarkOccurrences(boolean)
	 */
	public void setPaintMarkOccurrencesBorder(final boolean paintBorder) {
		this.paintMarkOccurrencesBorder = paintBorder;
		if (this.markOccurrencesSupport != null)
			this.markOccurrencesSupport.setPaintBorder(paintBorder);
	}

	/**
	 * Sets whether the bracket at the caret position is painted as a "match" when a
	 * matched bracket is found. Note that this property does nothing if
	 * {@link #isBracketMatchingEnabled()} returns <code>false</code>.
	 * <p>
	 *
	 * This method fires a property change event of type
	 * {@link #PAINT_MATCHED_BRACKET_PAIR_PROPERTY}.
	 *
	 * @param paintPair
	 *            Whether both brackets in a bracket pair should be highlighted when
	 *            bracket matching is enabled.
	 * @see #getPaintMatchedBracketPair()
	 * @see #isBracketMatchingEnabled()
	 * @see #setBracketMatchingEnabled(boolean)
	 */
	public void setPaintMatchedBracketPair(final boolean paintPair) {
		if (paintPair != this.paintMatchedBracketPair) {
			this.paintMatchedBracketPair = paintPair;
			this.doBracketMatching();
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.PAINT_MATCHED_BRACKET_PAIR_PROPERTY, !this.paintMatchedBracketPair,
					this.paintMatchedBracketPair);
		}
	}

	/**
	 * Toggles whether tab lines are painted. This method fires a property change
	 * event of type {@link #TAB_LINES_PROPERTY}.
	 *
	 * @param paint
	 *            Whether tab lines are painted.
	 * @see #getPaintTabLines()
	 * @see #setTabLineColor(Color)
	 */
	public void setPaintTabLines(final boolean paint) {
		if (paint != this.paintTabLines) {
			this.paintTabLines = paint;
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.TAB_LINES_PROPERTY, !paint, paint);
		}
	}

	/**
	 * Sets the parser delay. This is the delay that must occur between edits for
	 * any registered {@link Parser}s to run.
	 *
	 * @param millis
	 *            The new parser delay, in milliseconds. This must be greater than
	 *            zero.
	 * @see #getParserDelay()
	 */
	public void setParserDelay(final int millis) {
		if (this.parserManager == null)
			this.parserManager = new ParserManager(this);
		this.parserManager.setDelay(millis);
	}

	/**
	 * Applications typically have no need to modify this value.
	 * <p>
	 *
	 * Workaround for JTextComponents allowing the caret to be rendered entirely
	 * off-screen if the entire "previous" character fit entirely.
	 *
	 * @param rhsCorrection
	 *            The amount of space to add to the x-axis preferred span. This
	 *            should be non-negative.
	 * @see #getRightHandSideCorrection()
	 */
	public void setRightHandSideCorrection(final int rhsCorrection) {
		if (rhsCorrection < 0)
			throw new IllegalArgumentException("correction should be > 0");
		if (rhsCorrection != this.rhsCorrection) {
			this.rhsCorrection = rhsCorrection;
			this.revalidate();
			this.repaint();
		}
	}

	/**
	 * Sets the background color to use for a secondary language.
	 *
	 * @param index
	 *            The language index. Note that these are 1-based, not 0-based, and
	 *            should be in the range <code>1-getSecondaryLanguageCount()</code>,
	 *            inclusive.
	 * @param color
	 *            The new color, or <code>null</code> for none.
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #getSecondaryLanguageCount()
	 */
	public void setSecondaryLanguageBackground(int index, final Color color) {
		index--;
		final Color old = this.secondaryLanguageBackgrounds[index];
		if (color == null && old != null || color != null && !color.equals(old)) {
			this.secondaryLanguageBackgrounds[index] = color;
			if (this.getHighlightSecondaryLanguages())
				this.repaint();
		}
	}

	/**
	 * Sets whether a small popup window should display the text on the line
	 * containing a matched bracket whenever a matched bracket is off- screen.
	 *
	 * @param show
	 *            Whether to show the popup.
	 * @see #getShowMatchedBracketPopup()
	 */
	public void setShowMatchedBracketPopup(final boolean show) {
		this.showMatchedBracketPopup = show;
	}

	/**
	 * Sets what type of syntax highlighting this editor is doing. This method fires
	 * a property change of type {@link #SYNTAX_STYLE_PROPERTY}.
	 *
	 * @param styleKey
	 *            The syntax editing style to use, for example,
	 *            {@link SyntaxConstants#SYNTAX_STYLE_NONE} or
	 *            {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.
	 * @see #getSyntaxEditingStyle()
	 * @see SyntaxConstants
	 */
	public void setSyntaxEditingStyle(String styleKey) {
		if (styleKey == null)
			styleKey = SyntaxConstants.SYNTAX_STYLE_NONE;
		if (!styleKey.equals(this.syntaxStyleKey)) {
			final String oldStyle = this.syntaxStyleKey;
			this.syntaxStyleKey = styleKey;
			((RSyntaxDocument) this.getDocument()).setSyntaxStyle(styleKey);
			this.firePropertyChange(RSyntaxTextArea.SYNTAX_STYLE_PROPERTY, oldStyle, styleKey);
			this.setActiveLineRange(-1, -1);
		}

	}

	/**
	 * Sets all of the colors used in syntax highlighting to the colors specified.
	 * This uses a shallow copy of the color scheme so that multiple text areas can
	 * share the same color scheme and have their properties changed simultaneously.
	 * <p>
	 *
	 * This method fires a property change event of type
	 * {@link #SYNTAX_SCHEME_PROPERTY}.
	 *
	 * @param scheme
	 *            The instance of <code>SyntaxScheme</code> to use.
	 * @see #getSyntaxScheme()
	 */
	public void setSyntaxScheme(final SyntaxScheme scheme) {

		// NOTE: We don't check whether colorScheme is the same as the
		// current scheme because DecreaseFontSizeAction and
		// IncreaseFontSizeAction need it this way.
		// FIXME: Find a way around this.

		final SyntaxScheme old = this.syntaxScheme;
		this.syntaxScheme = scheme;

		// Recalculate the line height. We do this here instead of in
		// refreshFontMetrics() as this method is called less often and we
		// don't need the rendering hints to get the font's height.
		this.calculateLineHeight();

		if (this.isDisplayable())
			this.refreshFontMetrics(this.getGraphics2D(this.getGraphics()));

		// Updates the margin line and "matched bracket" highlight
		this.updateMarginLineX();
		this.lastBracketMatchPos = -1;
		this.doBracketMatching();

		// Force the current line highlight to be repainted, even though
		// the caret's location hasn't changed.
		this.forceCurrentLineHighlightRepaint();

		// So encompassing JScrollPane will have its scrollbars updated.
		this.revalidate();

		this.firePropertyChange(RSyntaxTextArea.SYNTAX_SCHEME_PROPERTY, old, this.syntaxScheme);

	}

	/**
	 * Sets the color use to paint tab lines. This method fires a property change
	 * event of type {@link #TAB_LINE_COLOR_PROPERTY}.
	 *
	 * @param c
	 *            The color. If this value is <code>null</code>, the default (gray)
	 *            is used.
	 * @see #getTabLineColor()
	 * @see #setPaintTabLines(boolean)
	 * @see #getPaintTabLines()
	 */
	public void setTabLineColor(Color c) {

		if (c == null)
			c = Color.gray;

		if (!c.equals(this.tabLineColor)) {
			final Color old = this.tabLineColor;
			this.tabLineColor = c;
			if (this.getPaintTabLines())
				this.repaint();
			this.firePropertyChange(RSyntaxTextArea.TAB_LINE_COLOR_PROPERTY, old, this.tabLineColor);
		}

	}

	/**
	 * Sets whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on, resize, copy
	 * from, and clink links in. This method fires a property change event of type
	 * {@link #FOCUSABLE_TIPS_PROPERTY}.
	 *
	 * @param use
	 *            Whether to use focusable tool tips.
	 * @see #getUseFocusableTips()
	 * @see FocusableTip
	 */
	public void setUseFocusableTips(final boolean use) {
		if (use != this.useFocusableTips) {
			this.useFocusableTips = use;
			this.firePropertyChange(RSyntaxTextArea.FOCUSABLE_TIPS_PROPERTY, !use, use);
		}
	}

	/**
	 * Sets whether selected text should use the "selected text color" property (set
	 * via {@link #setSelectedTextColor(Color)}). This is the typical behavior of
	 * text components. By default, RSyntaxTextArea does not do this, so that token
	 * styles are visible even in selected regions of text. This method fires a
	 * property change event of type {@link #USE_SELECTED_TEXT_COLOR_PROPERTY}.
	 *
	 * @param use
	 *            Whether to use the "selected text" color when painting text in
	 *            selected regions.
	 * @see #getUseSelectedTextColor()
	 */
	public void setUseSelectedTextColor(final boolean use) {
		if (use != this.useSelectedTextColor) {
			this.useSelectedTextColor = use;
			this.firePropertyChange(RSyntaxTextArea.USE_SELECTED_TEXT_COLOR_PROPERTY, !use, use);
		}
	}

	/**
	 * Sets whether whitespace is visible. This method fires a property change of
	 * type {@link #VISIBLE_WHITESPACE_PROPERTY}.
	 *
	 * @param visible
	 *            Whether whitespace should be visible.
	 * @see #isWhitespaceVisible()
	 */
	public void setWhitespaceVisible(final boolean visible) {
		if (this.whitespaceVisible != visible) {
			this.whitespaceVisible = visible;
			this.tokenPainter = visible ? new VisibleWhitespaceTokenPainter()
					: (TokenPainter) new DefaultTokenPainter();
			this.repaint();
			this.firePropertyChange(RSyntaxTextArea.VISIBLE_WHITESPACE_PROPERTY, !visible, visible);
		}
	}

	/**
	 * Resets the editor state after the user clicks on a hyperlink or releases the
	 * hyperlink modifier.
	 */
	private void stopScanningForLinks() {
		if (this.isScanningForLinks) {
			final Cursor c = this.getCursor();
			this.isScanningForLinks = false;
			this.linkGeneratorResult = null;
			this.hoveredOverLinkOffset = -1;
			if (c != null && c.getType() == Cursor.HAND_CURSOR) {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
				this.repaint(); // TODO: Repaint just the affected line.
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void undoLastAction() {
		super.undoLastAction();
		// Occasionally marked occurrences' Positions are in invalid states
		// due to how javax.swing.text.AbstractDocument tracks the start and
		// end offsets. This is usually not needed, but can be when the last
		// token in the Document is a marked occurrence, and an undo or redo
		// occurs which clears most of the document text. In that case it is
		// possible for the end Position to be reset to something small, but
		// the start offset to be its prior valid (start > end).
		((RSyntaxTextAreaHighlighter) this.getHighlighter()).clearMarkOccurrencesHighlights();
	}

	/**
	 * Returns the token at the specified position in the view.
	 *
	 * @param p
	 *            The position in the view.
	 * @return The token, or <code>null</code> if no token is at that position.
	 * @see #modelToToken(int)
	 */
	/*
	 * TODO: This is a little inefficient. This should convert view coordinates to
	 * the underlying token (if any). The way things currently are, we're calling
	 * getTokenListForLine() twice (once in viewToModel() and once here).
	 */
	public Token viewToToken(final Point p) {
		return this.modelToToken(this.viewToModel(p));
	}

}