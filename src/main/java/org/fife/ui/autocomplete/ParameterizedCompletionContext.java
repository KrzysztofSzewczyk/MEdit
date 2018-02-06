/*
 * 06/17/2012
 *
 * ParameritizedCompletionContext.java - Manages the state of parameterized
 * completion-related UI components during code completion.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;
import org.fife.ui.autocomplete.ParameterizedCompletionInsertionInfo.ReplacementCopy;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ChangeableHighlightPainter;

/**
 * Manages UI and state specific to parameterized completions - the parameter
 * description tool tip, the parameter completion choices list, the actual
 * highlights in the editor, etc. This component installs new key bindings when
 * appropriate to allow the user to cycle through the parameters of the
 * completion, and optionally cycle through completion choices for those
 * parameters.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class ParameterizedCompletionContext {

	/**
	 * Called when the user types the character marking the closing of the parameter
	 * list, such as '<code>)</code>'.
	 */
	private class ClosingAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			final JTextComponent tc = ParameterizedCompletionContext.this.ac.getTextComponent();
			final int dot = tc.getCaretPosition();
			final char end = ParameterizedCompletionContext.this.pc.getProvider().getParameterListEnd();

			// Are they at or past the end of the parameters?
			if (dot >= ParameterizedCompletionContext.this.maxPos.getOffset() - 2) { // ">=" for overwrite mode

				// Try to decide if we're closing a paren that is a part
				// of the (last) arg being typed.
				final String text = ParameterizedCompletionContext.this.getArgumentText(dot);
				if (text != null) {
					final char start = ParameterizedCompletionContext.this.pc.getProvider().getParameterListStart();
					final int startCount = this.getCount(text, start);
					final int endCount = this.getCount(text, end);
					if (startCount > endCount) { // Just closing a paren
						tc.replaceSelection(Character.toString(end));
						return;
					}
				}
				// tc.setCaretPosition(maxPos.getOffset());
				tc.setCaretPosition(Math.min(tc.getCaretPosition() + 1, tc.getDocument().getLength()));

				ParameterizedCompletionContext.this.deactivate();

			} else
				tc.replaceSelection(Character.toString(end));

		}

		public int getCount(final String text, final char ch) {
			int count = 0;
			int old = 0;
			int pos = 0;
			while ((pos = text.indexOf(ch, old)) > -1) {
				count++;
				old = pos + 1;
			}

			return count;
		}

	}

	/**
	 * Called when the user presses Enter while entering parameters.
	 */
	private class GotoEndAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {

			// If the param choices window is visible and something is chosen,
			// replace the parameter with it and move to the next one.
			if (ParameterizedCompletionContext.this.paramChoicesWindow != null
					&& ParameterizedCompletionContext.this.paramChoicesWindow.isVisible())
				if (ParameterizedCompletionContext.this.insertSelectedChoice())
					return;

			// Otherwise, just move to the end.
			ParameterizedCompletionContext.this.deactivate();
			final JTextComponent tc = ParameterizedCompletionContext.this.ac.getTextComponent();
			final int dot = tc.getCaretPosition();
			if (dot != ParameterizedCompletionContext.this.defaultEndOffs.getOffset())
				tc.setCaretPosition(ParameterizedCompletionContext.this.defaultEndOffs.getOffset());
			else {
				// oldEnterAction isn't what we're looking for (wrong key)
				final Action a = this.getDefaultEnterAction(tc);
				if (a != null)
					a.actionPerformed(e);
				else
					tc.replaceSelection("\n");
			}

		}

		private Action getDefaultEnterAction(final JTextComponent tc) {
			final ActionMap am = tc.getActionMap();
			return am.get(DefaultEditorKit.insertBreakAction);
		}

	}

	/**
	 * Action performed when the user hits the escape key.
	 */
	private class HideAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			// On first escape press, if the param choices window is visible,
			// just remove it, but keep ability to tab through params. If
			// param choices window isn't visible, or second escape press,
			// exit tabbing through params entirely.
			if (ParameterizedCompletionContext.this.paramChoicesWindow != null
					&& ParameterizedCompletionContext.this.paramChoicesWindow.isVisible()) {
				ParameterizedCompletionContext.this.paramChoicesWindow.setVisible(false);
				ParameterizedCompletionContext.this.paramChoicesWindow = null;
			} else
				ParameterizedCompletionContext.this.deactivate();
		}

	}

	/**
	 * Listens for various events in the text component while this tool tip is
	 * visible.
	 */
	private class Listener implements FocusListener, CaretListener, DocumentListener {

		private boolean markOccurrencesEnabled;

		/**
		 * Called when the text component's caret moves.
		 *
		 * @param e
		 *            The event.
		 */
		@Override
		public void caretUpdate(final CaretEvent e) {
			if (ParameterizedCompletionContext.this.maxPos == null) { // Sanity check
				ParameterizedCompletionContext.this.deactivate();
				return;
			}
			final int dot = e.getDot();
			if (dot < ParameterizedCompletionContext.this.minPos
					|| dot > ParameterizedCompletionContext.this.maxPos.getOffset()) {
				ParameterizedCompletionContext.this.deactivate();
				return;
			}
			ParameterizedCompletionContext.this.paramPrefix = ParameterizedCompletionContext.this.updateToolTipText();
			if (ParameterizedCompletionContext.this.active)
				ParameterizedCompletionContext.this.prepareParamChoicesWindow();
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
		}

		/**
		 * Called when the text component gains focus.
		 *
		 * @param e
		 *            The event.
		 */
		@Override
		public void focusGained(final FocusEvent e) {
			// Do nothing
		}

		/**
		 * Called when the text component loses focus.
		 *
		 * @param e
		 *            The event.
		 */
		@Override
		public void focusLost(final FocusEvent e) {
			ParameterizedCompletionContext.this.deactivate();
		}

		private void handleDocumentEvent(final DocumentEvent e) {
			if (!ParameterizedCompletionContext.this.ignoringDocumentEvents) {
				ParameterizedCompletionContext.this.ignoringDocumentEvents = true;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						ParameterizedCompletionContext.this.possiblyUpdateParamCopies(e.getDocument());
						ParameterizedCompletionContext.this.ignoringDocumentEvents = false;
					}
				});
			}
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

		/**
		 * Installs this listener onto a text component.
		 *
		 * @param tc
		 *            The text component to install onto.
		 * @see #uninstall()
		 */
		public void install(final JTextComponent tc) {

			boolean replaceTabs = false;
			if (tc instanceof RSyntaxTextArea) {
				final RSyntaxTextArea textArea = (RSyntaxTextArea) tc;
				this.markOccurrencesEnabled = textArea.getMarkOccurrences();
				textArea.setMarkOccurrences(false);
				replaceTabs = textArea.getTabsEmulated();
			}

			final Highlighter h = tc.getHighlighter();

			try {

				// Insert the parameter text
				final ParameterizedCompletionInsertionInfo info = ParameterizedCompletionContext.this.pc
						.getInsertionInfo(tc, replaceTabs);
				tc.replaceSelection(info.getTextToInsert());

				// Add highlights around the parameters.
				final int replacementCount = info.getReplacementCount();
				for (int i = 0; i < replacementCount; i++) {
					final DocumentRange dr = info.getReplacementLocation(i);
					final HighlightPainter painter = i < replacementCount - 1 ? ParameterizedCompletionContext.this.p
							: ParameterizedCompletionContext.this.endingP;
					// "-1" is a workaround for Java Highlight issues.
					ParameterizedCompletionContext.this.tags
							.add(h.addHighlight(dr.getStartOffset() - 1, dr.getEndOffset(), painter));
				}
				for (int i = 0; i < info.getReplacementCopyCount(); i++) {
					final ReplacementCopy rc = info.getReplacementCopy(i);
					ParameterizedCompletionContext.this.paramCopyInfos.add(new ParamCopyInfo(rc.getId(), (Highlight) h
							.addHighlight(rc.getStart(), rc.getEnd(), ParameterizedCompletionContext.this.paramCopyP)));
				}

				// Go back and start at the first parameter.
				tc.setCaretPosition(info.getSelectionStart());
				if (info.hasSelection())
					tc.moveCaretPosition(info.getSelectionEnd());

				ParameterizedCompletionContext.this.minPos = info.getMinOffset();
				ParameterizedCompletionContext.this.maxPos = info.getMaxOffset();
				try {
					final Document doc = tc.getDocument();
					if (ParameterizedCompletionContext.this.maxPos.getOffset() == 0)
						// Positions at offset 0 don't track document changes,
						// so we must manually do this here. This is not a
						// common occurrence.
						ParameterizedCompletionContext.this.maxPos = doc
								.createPosition(info.getTextToInsert().length());
					ParameterizedCompletionContext.this.defaultEndOffs = doc.createPosition(info.getDefaultEndOffs());
				} catch (final BadLocationException ble) {
					ble.printStackTrace(); // Never happens
				}

				// Listen for document events AFTER we insert
				tc.getDocument().addDocumentListener(this);

			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

			// Add listeners to the text component, AFTER text insertion.
			tc.addCaretListener(this);
			tc.addFocusListener(this);
			ParameterizedCompletionContext.this.installKeyBindings();

		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			this.handleDocumentEvent(e);
		}

		/**
		 * Uninstalls this listener from the current text component.
		 */
		public void uninstall() {

			final JTextComponent tc = ParameterizedCompletionContext.this.ac.getTextComponent();
			tc.removeCaretListener(this);
			tc.removeFocusListener(this);
			tc.getDocument().removeDocumentListener(this);
			ParameterizedCompletionContext.this.uninstallKeyBindings();

			if (this.markOccurrencesEnabled)
				((RSyntaxTextArea) tc).setMarkOccurrences(this.markOccurrencesEnabled);

			// Remove WeakReferences in javax.swing.text.
			ParameterizedCompletionContext.this.maxPos = null;
			ParameterizedCompletionContext.this.minPos = -1;
			ParameterizedCompletionContext.this.removeParameterHighlights();

		}

	}

	/**
	 * Action performed when the user presses the up or down arrow keys and the
	 * parameter completion choices popup is visible.
	 */
	private class NextChoiceAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final int amount;
		private final Action oldAction;

		public NextChoiceAction(final int amount, final Action oldAction) {
			this.amount = amount;
			this.oldAction = oldAction;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (ParameterizedCompletionContext.this.paramChoicesWindow != null
					&& ParameterizedCompletionContext.this.paramChoicesWindow.isVisible())
				ParameterizedCompletionContext.this.paramChoicesWindow.incSelection(this.amount);
			else if (this.oldAction != null)
				this.oldAction.actionPerformed(e);
			else
				ParameterizedCompletionContext.this.deactivate();
		}

	}

	/**
	 * Action performed when the user hits the tab key.
	 */
	private class NextParamAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			ParameterizedCompletionContext.this.moveToNextParam();
		}

	}

	private static class ParamCopyInfo {

		private Highlight h;
		private final String paramName;

		public ParamCopyInfo(final String paramName, final Highlight h) {
			this.paramName = paramName;
			this.h = h;
		}

	}

	/**
	 * Action performed when the user hits shift+tab.
	 */
	private class PrevParamAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			ParameterizedCompletionContext.this.moveToPreviousParam();
		}

	}

	private static final String IM_KEY_CLOSING = "ParamCompKey.Closing";

	private static final String IM_KEY_DOWN = "ParamCompKey.Down";

	private static final String IM_KEY_ENTER = "ParamCompKey.Enter";

	private static final String IM_KEY_ESCAPE = "ParamCompKey.Escape";

	private static final String IM_KEY_SHIFT_TAB = "ParamCompKey.ShiftTab";

	private static final String IM_KEY_TAB = "ParamCompKey.Tab";

	private static final String IM_KEY_UP = "ParamCompKey.Up";

	/**
	 * Returns the highlight from a list that comes "first" in a list. Even though
	 * most parameter highlights are ordered, sometimes they aren't (e.g. the
	 * "cursor" parameter in a template completion is always last, even though it
	 * can be anywhere in the template).
	 *
	 * @param highlights
	 *            The list of highlights. Assumed to be non-empty.
	 * @return The highlight that comes first in the document.
	 * @see #getLastHighlight(List)
	 */
	private static final int getFirstHighlight(final List<Highlight> highlights) {
		int first = -1;
		Highlight firstH = null;
		for (int i = 0; i < highlights.size(); i++) {
			final Highlight h = highlights.get(i);
			if (firstH == null || h.getStartOffset() < firstH.getStartOffset()) {
				firstH = h;
				first = i;
			}
		}
		return first;
	}

	/**
	 * Returns the highlight from a list that comes "last" in that list. Even though
	 * most parameter highlights are ordered, sometimes they aren't (e.g. the
	 * "cursor" parameter in a template completion is always last, even though it
	 * can be anywhere in the template.
	 *
	 * @param highlights
	 *            The list of highlights. Assumed to be non-empty.
	 * @return The highlight that comes last in the document.
	 * @see #getFirstHighlight(List)
	 */
	private static final int getLastHighlight(final List<Highlight> highlights) {
		int last = -1;
		Highlight lastH = null;
		for (int i = highlights.size() - 1; i >= 0; i--) {
			final Highlight h = highlights.get(i);
			if (lastH == null || h.getStartOffset() > lastH.getStartOffset()) {
				lastH = h;
				last = i;
			}
		}
		return last;
	}

	/**
	 * The parent AutoCompletion instance.
	 */
	private final AutoCompletion ac;

	/**
	 * Whether parameterized completion assistance is active.
	 */
	private boolean active;
	private Position defaultEndOffs;
	private final Highlighter.HighlightPainter endingP;
	private transient boolean ignoringDocumentEvents;
	/**
	 * The currently "selected" parameter in the displayed text.
	 */
	private int lastSelectedParam;
	/**
	 * Listens for events in the text component while this window is visible.
	 */
	private final Listener listener;
	/**
	 * The maximum offset into the document that the caret can move to before this
	 * tool tip disappears.
	 */
	private Position maxPos; // Moves with text inserted.
	/**
	 * The minimum offset into the document that the caret can move to before this
	 * tool tip disappears.
	 */
	private int minPos;
	private Action oldClosingAction;
	private Object oldClosingKey;
	private Action oldDownAction;
	private Object oldDownKey;
	private Action oldEnterAction;
	private Object oldEnterKey;

	private Action oldEscapeAction;
	private Object oldEscapeKey;
	private Action oldShiftTabAction;
	private Object oldShiftTabKey;
	private Action oldTabAction;
	private Object oldTabKey;
	private Action oldUpAction;

	private Object oldUpKey;

	/**
	 * The painter to paint borders around the variables.
	 */
	private final Highlighter.HighlightPainter p;

	/**
	 * A small popup window giving likely choices for parameterized completions.
	 */
	private ParameterizedCompletionChoicesWindow paramChoicesWindow;

	private final List<ParamCopyInfo> paramCopyInfos;

	private final Highlighter.HighlightPainter paramCopyP;

	/**
	 * The text before the caret for the current parameter. If
	 * {@link #paramChoicesWindow} is non-<code>null</code>, this is used to
	 * determine what parameter choices to actually show.
	 */
	private String paramPrefix;

	/**
	 * The parent window.
	 */
	private final Window parentWindow;

	/**
	 * The completion being described.
	 */
	private final ParameterizedCompletion pc;

	/**
	 * The tags for the highlights around parameters.
	 */
	private final List<Object> tags;

	/**
	 * A tool tip displaying the currently edited parameter name and type.
	 */
	private ParameterizedCompletionDescriptionToolTip tip;

	/**
	 * Constructor.
	 */
	public ParameterizedCompletionContext(final Window owner, final AutoCompletion ac,
			final ParameterizedCompletion pc) {

		this.parentWindow = owner;
		this.ac = ac;
		this.pc = pc;
		this.listener = new Listener();

		final AutoCompletionStyleContext sc = AutoCompletion.getStyleContext();
		this.p = new OutlineHighlightPainter(sc.getParameterOutlineColor());
		this.endingP = new OutlineHighlightPainter(sc.getParameterizedCompletionCursorPositionColor());
		this.paramCopyP = new ChangeableHighlightPainter(sc.getParameterCopyColor());
		this.tags = new ArrayList<>(1); // Usually small
		this.paramCopyInfos = new ArrayList<>(1);

	}

	/**
	 * Activates parameter completion support.
	 *
	 * @see #deactivate()
	 */
	public void activate() {

		if (this.active)
			return;

		this.active = true;
		final JTextComponent tc = this.ac.getTextComponent();
		this.lastSelectedParam = -1;

		if (this.pc.getShowParameterToolTip()) {
			this.tip = new ParameterizedCompletionDescriptionToolTip(this.parentWindow, this, this.ac, this.pc);
			try {
				final int dot = tc.getCaretPosition();
				final Rectangle r = tc.modelToView(dot);
				final Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, tc);
				r.x = p.x;
				r.y = p.y;
				this.tip.setLocationRelativeTo(r);
				this.tip.setVisible(true);
			} catch (final BadLocationException ble) { // Should never happen
				UIManager.getLookAndFeel().provideErrorFeedback(tc);
				ble.printStackTrace();
				this.tip = null;
			}
		}

		this.listener.install(tc);
		// First time through, we'll need to create this window.
		if (this.paramChoicesWindow == null)
			this.paramChoicesWindow = this.createParamChoicesWindow();
		this.lastSelectedParam = this.getCurrentParameterIndex();
		this.prepareParamChoicesWindow();
		this.paramChoicesWindow.setVisible(true);

	}

	/**
	 * Creates the completion window offering suggestions for parameters.
	 *
	 * @return The window.
	 */
	private ParameterizedCompletionChoicesWindow createParamChoicesWindow() {
		final ParameterizedCompletionChoicesWindow pcw = new ParameterizedCompletionChoicesWindow(this.parentWindow,
				this.ac, this);
		pcw.initialize(this.pc);
		return pcw;
	}

	/**
	 * Hides any popup windows and terminates parameterized completion assistance.
	 *
	 * @see #activate()
	 */
	public void deactivate() {
		if (!this.active)
			return;
		this.active = false;
		this.listener.uninstall();
		if (this.tip != null)
			this.tip.setVisible(false);
		if (this.paramChoicesWindow != null)
			this.paramChoicesWindow.setVisible(false);
	}

	/**
	 * Returns the text inserted for the parameter containing the specified offset.
	 *
	 * @param offs
	 *            The offset into the document.
	 * @return The text of the parameter containing the offset, or <code>null</code>
	 *         if the offset is not in a parameter.
	 */
	public String getArgumentText(final int offs) {
		final List<Highlight> paramHighlights = this.getParameterHighlights();
		if (paramHighlights == null || paramHighlights.size() == 0)
			return null;
		for (final Highlight h : paramHighlights)
			if (offs >= h.getStartOffset() && offs <= h.getEndOffset()) {
				final int start = h.getStartOffset() + 1;
				final int len = h.getEndOffset() - start;
				final JTextComponent tc = this.ac.getTextComponent();
				final Document doc = tc.getDocument();
				try {
					return doc.getText(start, len);
				} catch (final BadLocationException ble) {
					UIManager.getLookAndFeel().provideErrorFeedback(tc);
					ble.printStackTrace();
					return null;
				}
			}
		return null;
	}

	/**
	 * Returns the highlight of the current parameter.
	 *
	 * @return The current parameter's highlight, or <code>null</code> if the caret
	 *         is not in a parameter's bounds.
	 * @see #getCurrentParameterStartOffset()
	 */
	private Highlight getCurrentParameterHighlight() {

		final JTextComponent tc = this.ac.getTextComponent();
		int dot = tc.getCaretPosition();
		if (dot > 0)
			dot--; // Workaround for Java Highlight issues

		final List<Highlight> paramHighlights = this.getParameterHighlights();
		for (final Highlight h : paramHighlights)
			if (dot >= h.getStartOffset() && dot < h.getEndOffset())
				return h;

		return null;

	}

	private int getCurrentParameterIndex() {

		final JTextComponent tc = this.ac.getTextComponent();
		int dot = tc.getCaretPosition();
		if (dot > 0)
			dot--; // Workaround for Java Highlight issues

		final List<Highlight> paramHighlights = this.getParameterHighlights();
		for (int i = 0; i < paramHighlights.size(); i++) {
			final Highlight h = paramHighlights.get(i);
			if (dot >= h.getStartOffset() && dot < h.getEndOffset())
				return i;
		}

		return -1;

	}

	/**
	 * Returns the starting offset of the current parameter.
	 *
	 * @return The current parameter's starting offset, or <code>-1</code> if the
	 *         caret is not in a parameter's bounds.
	 * @see #getCurrentParameterHighlight()
	 */
	private int getCurrentParameterStartOffset() {
		final Highlight h = this.getCurrentParameterHighlight();
		return h != null ? h.getStartOffset() + 1 : -1;
	}

	public List<Highlight> getParameterHighlights() {
		final List<Highlight> paramHighlights = new ArrayList<>(2);
		final JTextComponent tc = this.ac.getTextComponent();
		final Highlight[] highlights = tc.getHighlighter().getHighlights();
		for (final Highlight highlight : highlights) {
			final HighlightPainter painter = highlight.getPainter();
			if (painter == this.p || painter == this.endingP)
				paramHighlights.add(highlight);
		}
		return paramHighlights;
	}

	/**
	 * Inserts the choice selected in the parameter choices window.
	 *
	 * @return Whether the choice was inserted. This will be <code>false</code> if
	 *         the window is not visible, or no choice is selected.
	 */
	boolean insertSelectedChoice() {
		if (this.paramChoicesWindow != null && this.paramChoicesWindow.isVisible()) {
			final String choice = this.paramChoicesWindow.getSelectedChoice();
			if (choice != null) {
				final JTextComponent tc = this.ac.getTextComponent();
				final Highlight h = this.getCurrentParameterHighlight();
				if (h != null) {
					// "+1" is a workaround for Java Highlight issues.
					tc.setSelectionStart(h.getStartOffset() + 1);
					tc.setSelectionEnd(h.getEndOffset());
					tc.replaceSelection(choice);
					this.moveToNextParam();
				} else
					UIManager.getLookAndFeel().provideErrorFeedback(tc);
				return true;
			}
		}
		return false;
	}

	/**
	 * Installs key bindings on the text component that facilitate the user editing
	 * this completion's parameters.
	 *
	 * @see #uninstallKeyBindings()
	 */
	private void installKeyBindings() {

		if (AutoCompletion.getDebug())
			System.out.println("CompletionContext: Installing keybindings");

		final JTextComponent tc = this.ac.getTextComponent();
		final InputMap im = tc.getInputMap();
		final ActionMap am = tc.getActionMap();

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		this.oldTabKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_TAB);
		this.oldTabAction = am.get(ParameterizedCompletionContext.IM_KEY_TAB);
		am.put(ParameterizedCompletionContext.IM_KEY_TAB, new NextParamAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK);
		this.oldShiftTabKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_SHIFT_TAB);
		this.oldShiftTabAction = am.get(ParameterizedCompletionContext.IM_KEY_SHIFT_TAB);
		am.put(ParameterizedCompletionContext.IM_KEY_SHIFT_TAB, new PrevParamAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		this.oldUpKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_UP);
		this.oldUpAction = am.get(ParameterizedCompletionContext.IM_KEY_UP);
		am.put(ParameterizedCompletionContext.IM_KEY_UP, new NextChoiceAction(-1, this.oldUpAction));

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		this.oldDownKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_DOWN);
		this.oldDownAction = am.get(ParameterizedCompletionContext.IM_KEY_DOWN);
		am.put(ParameterizedCompletionContext.IM_KEY_DOWN, new NextChoiceAction(1, this.oldDownAction));

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		this.oldEnterKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_ENTER);
		this.oldEnterAction = am.get(ParameterizedCompletionContext.IM_KEY_ENTER);
		am.put(ParameterizedCompletionContext.IM_KEY_ENTER, new GotoEndAction());

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		this.oldEscapeKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_ESCAPE);
		this.oldEscapeAction = am.get(ParameterizedCompletionContext.IM_KEY_ESCAPE);
		am.put(ParameterizedCompletionContext.IM_KEY_ESCAPE, new HideAction());

		final char end = this.pc.getProvider().getParameterListEnd();
		ks = KeyStroke.getKeyStroke(end);
		this.oldClosingKey = im.get(ks);
		im.put(ks, ParameterizedCompletionContext.IM_KEY_CLOSING);
		this.oldClosingAction = am.get(ParameterizedCompletionContext.IM_KEY_CLOSING);
		am.put(ParameterizedCompletionContext.IM_KEY_CLOSING, new ClosingAction());

	}

	/**
	 * Moves to and selects the next parameter.
	 *
	 * @see #moveToPreviousParam()
	 */
	private void moveToNextParam() {

		final JTextComponent tc = this.ac.getTextComponent();
		final int dot = tc.getCaretPosition();
		final int tagCount = this.tags.size();
		if (tagCount == 0) {
			tc.setCaretPosition(this.maxPos.getOffset());
			this.deactivate();
		}

		Highlight currentNext = null;
		int pos = -1;
		final List<Highlight> highlights = this.getParameterHighlights();
		for (int i = 0; i < highlights.size(); i++) {
			final Highlight hl = highlights.get(i);
			// Check "< dot", not "<= dot" as OutlineHighlightPainter paints
			// starting at one char AFTER the highlight starts, to work around
			// Java issue. Thanks to Matthew Adereth!
			if (currentNext == null || currentNext.getStartOffset() < /* = */dot
					|| hl.getStartOffset() > dot && hl.getStartOffset() <= currentNext.getStartOffset()) {
				currentNext = hl;
				pos = i;
			}
		}

		// No params after caret - go to first one
		if (currentNext.getStartOffset() + 1 <= dot) {
			final int nextIndex = ParameterizedCompletionContext.getFirstHighlight(highlights);
			currentNext = highlights.get(nextIndex);
			pos = 0;
		}

		// "+1" is a workaround for Java Highlight issues.
		tc.setSelectionStart(currentNext.getStartOffset() + 1);
		tc.setSelectionEnd(currentNext.getEndOffset());
		this.updateToolTipText(pos);

	}

	/**
	 * Moves to and selects the previous parameter.
	 *
	 * @see #moveToNextParam()
	 */
	private void moveToPreviousParam() {

		final JTextComponent tc = this.ac.getTextComponent();

		final int tagCount = this.tags.size();
		if (tagCount == 0) { // Should never happen
			tc.setCaretPosition(this.maxPos.getOffset());
			this.deactivate();
		}

		final int dot = tc.getCaretPosition();
		final int selStart = tc.getSelectionStart() - 1; // Workaround for Java Highlight issues.
		Highlight currentPrev = null;
		int pos = 0;
		final List<Highlight> highlights = this.getParameterHighlights();

		for (int i = 0; i < highlights.size(); i++) {
			final Highlight h = highlights.get(i);
			if (currentPrev == null || currentPrev.getStartOffset() >= dot || h.getStartOffset() < selStart
					&& (h.getStartOffset() > currentPrev.getStartOffset() || pos == this.lastSelectedParam)) {
				currentPrev = h;
				pos = i;
			}
		}

		// Loop back from param 0 to last param.
		final int firstIndex = ParameterizedCompletionContext.getFirstHighlight(highlights);
		// if (pos==0 && lastSelectedParam==0 && highlights.size()>1) {
		if (pos == firstIndex && this.lastSelectedParam == firstIndex && highlights.size() > 1) {
			pos = ParameterizedCompletionContext.getLastHighlight(highlights);
			currentPrev = highlights.get(pos);
			// "+1" is a workaround for Java Highlight issues.
			tc.setSelectionStart(currentPrev.getStartOffset() + 1);
			tc.setSelectionEnd(currentPrev.getEndOffset());
			this.updateToolTipText(pos);
		} else if (currentPrev != null && dot > currentPrev.getStartOffset()) {
			// "+1" is a workaround for Java Highlight issues.
			tc.setSelectionStart(currentPrev.getStartOffset() + 1);
			tc.setSelectionEnd(currentPrev.getEndOffset());
			this.updateToolTipText(pos);
		} else {
			tc.setCaretPosition(this.maxPos.getOffset());
			this.deactivate();
		}

	}

	private void possiblyUpdateParamCopies(final Document doc) {

		final int index = this.getCurrentParameterIndex();
		// FunctionCompletions add an extra param at end of inserted text
		if (index > -1 && index < this.pc.getParamCount()) {

			// Typing in an "end parameter" => stop parameter assistance.
			final Parameter param = this.pc.getParam(index);
			if (param.isEndParam()) {
				this.deactivate();
				return;
			}

			// Get the current value of the current parameter.
			final List<Highlight> paramHighlights = this.getParameterHighlights();
			final Highlight h = paramHighlights.get(index);
			final int start = h.getStartOffset() + 1; // param offsets are offset (!) by 1
			final int len = h.getEndOffset() - start;
			String replacement = null;
			try {
				replacement = doc.getText(start, len);
			} catch (final BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

			// Replace any param copies tracking this parameter with the
			// value of this parameter.
			for (final ParamCopyInfo pci : this.paramCopyInfos)
				if (pci.paramName.equals(param.getName()))
					pci.h = this.replaceHighlightedText(doc, pci.h, replacement);

		} else
			this.deactivate();

	}

	/**
	 * Updates the optional window listing likely completion choices,
	 */
	private void prepareParamChoicesWindow() {

		// If this window was set to null, the user pressed Escape to hide it
		if (this.paramChoicesWindow != null) {

			final int offs = this.getCurrentParameterStartOffset();
			if (offs == -1) {
				this.paramChoicesWindow.setVisible(false);
				return;
			}

			final JTextComponent tc = this.ac.getTextComponent();
			try {
				final Rectangle r = tc.modelToView(offs);
				final Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, tc);
				r.x = p.x;
				r.y = p.y;
				this.paramChoicesWindow.setLocationRelativeTo(r);
			} catch (final BadLocationException ble) { // Should never happen
				UIManager.getLookAndFeel().provideErrorFeedback(tc);
				ble.printStackTrace();
			}

			// Toggles visibility, if necessary.
			this.paramChoicesWindow.setParameter(this.lastSelectedParam, this.paramPrefix);

		}

	}

	/**
	 * Removes the bounding boxes around parameters.
	 */
	private void removeParameterHighlights() {
		final JTextComponent tc = this.ac.getTextComponent();
		final Highlighter h = tc.getHighlighter();
		for (int i = 0; i < this.tags.size(); i++)
			h.removeHighlight(this.tags.get(i));
		this.tags.clear();
		for (final ParamCopyInfo pci : this.paramCopyInfos)
			h.removeHighlight(pci.h);
		this.paramCopyInfos.clear();
	}

	/**
	 * Replaces highlighted text with new text. Takes special care so that the
	 * highlight stays just around the newly-highlighted text, since Swing's
	 * <code>Highlight</code> classes are funny about insertions at their start
	 * offsets.
	 *
	 * @param doc
	 *            The document.
	 * @param h
	 *            The highlight whose text to change.
	 * @param replacement
	 *            The new text to be in the highlight.
	 * @return The replacement highlight for <code>h</code>.
	 */
	private Highlight replaceHighlightedText(final Document doc, Highlight h, final String replacement) {
		try {

			final int start = h.getStartOffset();
			final int len = h.getEndOffset() - start;
			final Highlighter highlighter = this.ac.getTextComponent().getHighlighter();
			highlighter.removeHighlight(h);

			if (doc instanceof AbstractDocument)
				((AbstractDocument) doc).replace(start, len, replacement, null);
			else {
				doc.remove(start, len);
				doc.insertString(start, replacement, null);
			}

			final int newEnd = start + replacement.length();
			h = (Highlight) highlighter.addHighlight(start, newEnd, this.paramCopyP);
			return h;

		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return null;

	}

	/**
	 * Removes the key bindings we installed.
	 *
	 * @see #installKeyBindings()
	 */
	private void uninstallKeyBindings() {

		if (AutoCompletion.getDebug())
			System.out.println("CompletionContext Uninstalling keybindings");

		final JTextComponent tc = this.ac.getTextComponent();
		final InputMap im = tc.getInputMap();
		final ActionMap am = tc.getActionMap();

		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		im.put(ks, this.oldTabKey);
		am.put(ParameterizedCompletionContext.IM_KEY_TAB, this.oldTabAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK);
		im.put(ks, this.oldShiftTabKey);
		am.put(ParameterizedCompletionContext.IM_KEY_SHIFT_TAB, this.oldShiftTabAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		im.put(ks, this.oldUpKey);
		am.put(ParameterizedCompletionContext.IM_KEY_UP, this.oldUpAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		im.put(ks, this.oldDownKey);
		am.put(ParameterizedCompletionContext.IM_KEY_DOWN, this.oldDownAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		im.put(ks, this.oldEnterKey);
		am.put(ParameterizedCompletionContext.IM_KEY_ENTER, this.oldEnterAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		im.put(ks, this.oldEscapeKey);
		am.put(ParameterizedCompletionContext.IM_KEY_ESCAPE, this.oldEscapeAction);

		final char end = this.pc.getProvider().getParameterListEnd();
		ks = KeyStroke.getKeyStroke(end);
		im.put(ks, this.oldClosingKey);
		am.put(ParameterizedCompletionContext.IM_KEY_CLOSING, this.oldClosingAction);

	}

	/**
	 * Updates the text in the tool tip to have the current parameter displayed in
	 * bold. The "current parameter" is determined from the current caret position.
	 *
	 * @return The "prefix" of text in the caret's parameter before the caret.
	 */
	private String updateToolTipText() {

		final JTextComponent tc = this.ac.getTextComponent();
		final int dot = tc.getSelectionStart();
		final int mark = tc.getSelectionEnd();
		int index = -1;
		String paramPrefix = null;

		final List<Highlight> paramHighlights = this.getParameterHighlights();
		for (int i = 0; i < paramHighlights.size(); i++) {
			final Highlight h = paramHighlights.get(i);
			// "+1" because of param hack - see OutlineHighlightPainter
			final int start = h.getStartOffset() + 1;
			if (dot >= start && dot <= h.getEndOffset()) {
				try {
					// All text selected => offer all suggestions, otherwise
					// use prefix before selection
					if (dot != start || mark != h.getEndOffset())
						paramPrefix = tc.getText(start, dot - start);
				} catch (final BadLocationException ble) {
					ble.printStackTrace();
				}
				index = i;
				break;
			}
		}

		this.updateToolTipText(index);
		return paramPrefix;

	}

	private void updateToolTipText(final int selectedParam) {
		if (selectedParam != this.lastSelectedParam) {
			if (this.tip != null)
				this.tip.updateText(selectedParam);
			this.lastSelectedParam = selectedParam;
		}
	}

	/**
	 * Updates the <code>LookAndFeel</code> of all popup windows this context
	 * manages.
	 */
	public void updateUI() {
		if (this.tip != null)
			this.tip.updateUI();
		if (this.paramChoicesWindow != null)
			this.paramChoicesWindow.updateUI();
	}

}