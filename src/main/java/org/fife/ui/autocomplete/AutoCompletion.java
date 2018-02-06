/*
 * 12/21/2008
 *
 * AutoCompletion.java - Handles auto-completion for a text component.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

/**
 * Adds auto-completion to a text component. Provides a popup window with a list
 * of auto-complete choices on a given keystroke, such as Crtrl+Space.
 * <p>
 *
 * Depending on the {@link CompletionProvider} installed, the following
 * auto-completion features may be enabled:
 *
 * <ul>
 * <li>An auto-complete choices list made visible via e.g. Ctrl+Space</li>
 * <li>A "description" window displayed alongside the choices list that provides
 * documentation on the currently selected completion choice (as seen in Eclipse
 * and NetBeans).</li>
 * <li>Parameter assistance. If this is enabled, if the user enters a
 * "parameterized" completion, such as a method or a function, then they will
 * receive a tool tip describing the arguments they have to enter to the
 * completion. Also, the arguments can be navigated via tab and shift+tab (a la
 * Eclipse and NetBeans).</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 1.0
 */
/*
 * This class handles intercepting window and hierarchy events from the text
 * component, so the popup window is only visible when it should be visible. It
 * also handles communication between the CompletionProvider and the actual
 * popup Window.
 */
public class AutoCompletion {

	/**
	 * Listens for events in the text component to auto-activate the code completion
	 * popup.
	 */
	private class AutoActivationListener extends FocusAdapter
			implements DocumentListener, CaretListener, ActionListener {

		private boolean justInserted;
		private final Timer timer;

		public AutoActivationListener() {
			this.timer = new Timer(200, this);
			this.timer.setRepeats(false);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			AutoCompletion.this.doCompletion();
		}

		public void addTo(final JTextComponent tc) {
			tc.addFocusListener(this);
			tc.getDocument().addDocumentListener(this);
			tc.addCaretListener(this);
		}

		@Override
		public void caretUpdate(final CaretEvent e) {
			if (this.justInserted)
				this.justInserted = false;
			else
				this.timer.stop();
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
			// Ignore
		}

		@Override
		public void focusLost(final FocusEvent e) {
			this.timer.stop();
			// hideChildWindows(); Other listener will do this
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			this.justInserted = false;
			if (AutoCompletion.this.isAutoCompleteEnabled() && AutoCompletion.this.isAutoActivationEnabled()
					&& e.getLength() == 1) {
				if (AutoCompletion.this.provider.isAutoActivateOkay(AutoCompletion.this.textComponent)) {
					this.timer.restart();
					this.justInserted = true;
				} else
					this.timer.stop();
			} else
				this.timer.stop();
		}

		public void removeFrom(final JTextComponent tc) {
			tc.removeFocusListener(this);
			tc.getDocument().removeDocumentListener(this);
			tc.removeCaretListener(this);
			this.timer.stop();
			this.justInserted = false;
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			this.timer.stop();
		}

	}

	/**
	 * The <code>Action</code> that displays the popup window if auto-completion is
	 * enabled.
	 */
	protected class AutoCompleteAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletion.this.isAutoCompleteEnabled())
				AutoCompletion.this.refreshPopupWindow();
			else if (AutoCompletion.this.oldTriggerAction != null)
				AutoCompletion.this.oldTriggerAction.actionPerformed(e);
		}

	}

	/**
	 * Listens for LookAndFeel changes and updates the various popup windows
	 * involved in auto-completion accordingly.
	 */
	private class LookAndFeelChangeListener implements PropertyChangeListener {

		@Override
		public void propertyChange(final PropertyChangeEvent e) {
			final String name = e.getPropertyName();
			if ("lookAndFeel".equals(name))
				AutoCompletion.this.updateUI();
		}

	}

	/**
	 * Action that starts a parameterized completion, e.g. after '(' is typed.
	 */
	private class ParameterizedCompletionStartAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final String start;

		public ParameterizedCompletionStartAction(final char ch) {
			this.start = Character.toString(ch);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {

			// Prevents keystrokes from messing up
			final boolean wasVisible = AutoCompletion.this.hidePopupWindow();

			// Only proceed if they were selecting a completion
			if (!wasVisible || !AutoCompletion.this.isParameterAssistanceEnabled()) {
				AutoCompletion.this.textComponent.replaceSelection(this.start);
				return;
			}

			final Completion c = AutoCompletion.this.popupWindow.getSelection();
			if (c instanceof ParameterizedCompletion)
				// Fixes capitalization of the entered text.
				AutoCompletion.this.insertCompletion(c, true);

		}

	}

	/**
	 * Listens for events in the parent window of the text component with
	 * auto-completion enabled.
	 */
	private class ParentWindowListener extends ComponentAdapter implements WindowFocusListener {

		public void addTo(final Window w) {
			w.addComponentListener(this);
			w.addWindowFocusListener(this);
		}

		@Override
		public void componentHidden(final ComponentEvent e) {
			AutoCompletion.this.hideChildWindows();
		}

		@Override
		public void componentMoved(final ComponentEvent e) {
			AutoCompletion.this.hideChildWindows();
		}

		@Override
		public void componentResized(final ComponentEvent e) {
			AutoCompletion.this.hideChildWindows();
		}

		public void removeFrom(final Window w) {
			w.removeComponentListener(this);
			w.removeWindowFocusListener(this);
		}

		@Override
		public void windowGainedFocus(final WindowEvent e) {
		}

		@Override
		public void windowLostFocus(final WindowEvent e) {
			AutoCompletion.this.hideChildWindows();
		}

	}

	/**
	 * Listens for events from the popup window.
	 */
	private class PopupWindowListener extends ComponentAdapter {

		@Override
		public void componentHidden(final ComponentEvent e) {
			AutoCompletion.this.fireAutoCompletionEvent(AutoCompletionEvent.Type.POPUP_HIDDEN);
		}

		@Override
		public void componentShown(final ComponentEvent e) {
			AutoCompletion.this.fireAutoCompletionEvent(AutoCompletionEvent.Type.POPUP_SHOWN);
		}

		public void install(final AutoCompletePopupWindow popupWindow) {
			popupWindow.addComponentListener(this);
		}

		public void uninstall(final AutoCompletePopupWindow popupWindow) {
			if (popupWindow != null)
				popupWindow.removeComponentListener(this);
		}

	}

	/**
	 * Listens for events from the text component we're installed on.
	 */
	private class TextComponentListener extends FocusAdapter implements HierarchyListener {

		void addTo(final JTextComponent tc) {
			tc.addFocusListener(this);
			tc.addHierarchyListener(this);
		}

		/**
		 * Hide the auto-completion windows when the text component loses focus.
		 */
		@Override
		public void focusLost(final FocusEvent e) {
			AutoCompletion.this.hideChildWindows();
		}

		/**
		 * Called when the component hierarchy for our text component changes. When the
		 * text component is added to a new {@link Window}, this method registers
		 * listeners on that <code>Window</code>.
		 *
		 * @param e
		 *            The event.
		 */
		@Override
		public void hierarchyChanged(final HierarchyEvent e) {

			// NOTE: e many be null as we call this method at other times.
			// System.out.println("Hierarchy changed! " + e);

			final Window oldParentWindow = AutoCompletion.this.parentWindow;
			AutoCompletion.this.parentWindow = SwingUtilities.getWindowAncestor(AutoCompletion.this.textComponent);
			if (AutoCompletion.this.parentWindow != oldParentWindow) {
				if (oldParentWindow != null)
					AutoCompletion.this.parentWindowListener.removeFrom(oldParentWindow);
				if (AutoCompletion.this.parentWindow != null)
					AutoCompletion.this.parentWindowListener.addTo(AutoCompletion.this.parentWindow);
			}

		}

		public void removeFrom(final JTextComponent tc) {
			tc.removeFocusListener(this);
			tc.removeHierarchyListener(this);
		}

	}

	/**
	 * Whether debug messages should be printed to stdout as AutoCompletion runs.
	 */
	private static final boolean DEBUG = AutoCompletion.initDebug();

	/**
	 * An optional redirector that converts URL's to some other location before
	 * being handed over to <code>externalURLHandler</code>.
	 */
	private static LinkRedirector linkRedirector;

	/**
	 * Key used in the input map for the parameter completion action.
	 */
	private static final String PARAM_COMPLETE_KEY = "AutoCompletion.FunctionStart";

	/**
	 * The key used in the input map for the AutoComplete action.
	 */
	private static final String PARAM_TRIGGER_KEY = "AutoComplete";

	/**
	 * Stores how to render auto-completion-specific highlights in text components.
	 */
	private static final AutoCompletionStyleContext styleContext = new AutoCompletionStyleContext();

	/**
	 * Returns whether debug is enabled for AutoCompletion.
	 *
	 * @return Whether debug is enabled.
	 */
	static boolean getDebug() {
		return AutoCompletion.DEBUG;
	}

	/**
	 * Returns the default auto-complete "trigger key" for this OS. For Windows, for
	 * example, it is Ctrl+Space.
	 *
	 * @return The default auto-complete trigger key.
	 */
	public static KeyStroke getDefaultTriggerKey() {
		// Default to CTRL, even on Mac, since Ctrl+Space activates Spotlight
		final int mask = InputEvent.CTRL_MASK;
		return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, mask);
	}

	/**
	 * Returns the link redirector, if any.
	 *
	 * @return The link redirector, or <code>null</code> if none.
	 * @see #setLinkRedirector(LinkRedirector)
	 */
	public static LinkRedirector getLinkRedirector() {
		return AutoCompletion.linkRedirector;
	}

	/**
	 * Returns the style context describing how auto-completion related highlights
	 * in the editor are rendered.
	 *
	 * @return The style context.
	 */
	public static AutoCompletionStyleContext getStyleContext() {
		return AutoCompletion.styleContext;
	}

	/**
	 * Determines whether debug should be enabled for the AutoCompletion library.
	 * This method checks a system property, but takes care of
	 * {@link SecurityException}s in case we're in an applet or WebStart.
	 *
	 * @return Whether debug should be enabled.
	 */
	private static final boolean initDebug() {
		boolean debug = false;
		try {
			debug = Boolean.getBoolean("AutoCompletion.debug");
		} catch (final SecurityException se) { // We're in an applet or WebStart.
			debug = false;
		}
		return debug;
	}

	/**
	 * Sets the redirector for external URL's found in code completion
	 * documentation. When a non-local link in completion popups is clicked, this
	 * redirector is given the chance to modify the URL fetched and displayed.
	 *
	 * @param linkRedirector
	 *            The link redirector, or <code>null</code> for none.
	 * @see #getLinkRedirector()
	 */
	public static void setLinkRedirector(final LinkRedirector linkRedirector) {
		AutoCompletion.linkRedirector = linkRedirector;
	}

	/**
	 * Whether the auto-activation of auto-complete (after a delay, after the user
	 * types an appropriate character) is enabled.
	 */
	private boolean autoActivationEnabled;

	/**
	 * Listens for events in the text component that cause the popup windows to
	 * automatically activate.
	 */
	private final AutoActivationListener autoActivationListener;

	/**
	 * Whether auto-complete is enabled.
	 */
	private boolean autoCompleteEnabled;

	/**
	 * Whether or not, when there is only a single auto-complete option that matches
	 * the text at the current text position, that text should be auto-inserted,
	 * instead of the completion window displaying.
	 */
	private boolean autoCompleteSingleChoices;

	/**
	 * The handler to use when an external URL is clicked in the help documentation.
	 */
	private ExternalURLHandler externalURLHandler;

	/**
	 * Whether or not the popup should be hidden when the CompletionProvider
	 * changes. If set to false, caller has to ensure refresh of the popup content.
	 * Defaults to true.
	 */
	private boolean hideOnCompletionProviderChange;

	/**
	 * Whether or not the popup should be hidden when user types a space (or any
	 * character that resets the completion list to "all completions"). Defaults to
	 * true.
	 */
	private boolean hideOnNoText;

	/**
	 * Listens for LAF changes so the auto-complete windows automatically update
	 * themselves accordingly.
	 */
	private final LookAndFeelChangeListener lafListener;

	/**
	 * All listeners registered on this component.
	 */
	private final EventListenerList listeners;

	/**
	 * The action previously assigned to the parameter completion key, so we can
	 * reset it when we uninstall.
	 */
	private Action oldParenAction;

	/**
	 * The previous key in the text component's <code>InputMap</code> for the
	 * parameter completion trigger key.
	 */
	private Object oldParenKey;

	/**
	 * The action previously assigned to {@link #trigger}, so we can reset it if the
	 * user disables auto-completion.
	 */
	private Action oldTriggerAction;

	/**
	 * The previous key in the text component's <code>InputMap</code> for the
	 * trigger key.
	 */
	private Object oldTriggerKey;

	/**
	 * A renderer used for {@link Completion}s in the optional parameter choices
	 * popup window (displayed when a {@link ParameterizedCompletion} is
	 * code-completed). If this isn't set, a default renderer is used.
	 */
	private ListCellRenderer paramChoicesRenderer;

	/**
	 * Whether parameter assistance is enabled.
	 */
	private boolean parameterAssistanceEnabled;

	/**
	 * The parent window of {@link #textComponent}.
	 */
	private Window parentWindow;

	/**
	 * Listens for events in the parent window that affect the visibility of the
	 * popup windows.
	 */
	private final ParentWindowListener parentWindowListener;

	/**
	 * Manages any parameterized completions that are inserted.
	 */
	private ParameterizedCompletionContext pcc;

	/**
	 * The popup window containing completion choices.
	 */
	private AutoCompletePopupWindow popupWindow;

	/**
	 * Listens for events from the popup window.
	 */
	private final PopupWindowListener popupWindowListener;

	/**
	 * The preferred size of the completion choices window. This field exists
	 * because the user will likely set the preferred size of the window before it
	 * is actually created.
	 */
	private Dimension preferredChoicesWindowSize;

	/**
	 * The preferred size of the optional description window. This field only exists
	 * because the user may (and usually will) set the size of the description
	 * window before it exists (it must be parented to a Window).
	 */
	private Dimension preferredDescWindowSize;

	/**
	 * Provides the completion options relevant to the current caret position.
	 */
	private CompletionProvider provider;

	/**
	 * The renderer to use for the completion choices. If this is <code>null</code>,
	 * then a default renderer is used.
	 */
	private ListCellRenderer renderer;

	/**
	 * Whether the description window should be displayed along with the completion
	 * choice window.
	 */
	private boolean showDescWindow;

	/**
	 * The text component we're providing completion for.
	 */
	private JTextComponent textComponent;

	/**
	 * Listens for events from the text component that affect the visibility of the
	 * popup windows.
	 */
	private final TextComponentListener textComponentListener;

	/**
	 * The keystroke that triggers the completion window.
	 */
	private KeyStroke trigger;

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The completion provider. This cannot be <code>null</code>
	 */
	public AutoCompletion(final CompletionProvider provider) {

		this.setChoicesWindowSize(350, 200);
		this.setDescriptionWindowSize(350, 250);

		this.setCompletionProvider(provider);
		this.setTriggerKey(AutoCompletion.getDefaultTriggerKey());
		this.setAutoCompleteEnabled(true);
		this.setAutoCompleteSingleChoices(true);
		this.setAutoActivationEnabled(false);
		this.setShowDescWindow(false);
		this.setHideOnCompletionProviderChange(true);
		this.setHideOnNoText(true);
		this.parentWindowListener = new ParentWindowListener();
		this.textComponentListener = new TextComponentListener();
		this.autoActivationListener = new AutoActivationListener();
		this.lafListener = new LookAndFeelChangeListener();
		this.popupWindowListener = new PopupWindowListener();
		this.listeners = new EventListenerList();

	}

	/**
	 * Adds a listener interested in popup window events from this instance.
	 *
	 * @param l
	 *            The listener to add.
	 * @see #removeAutoCompletionListener(AutoCompletionListener)
	 */
	public void addAutoCompletionListener(final AutoCompletionListener l) {
		this.listeners.add(AutoCompletionListener.class, l);
	}

	/**
	 * Creates and returns the action to call when the user presses the
	 * auto-completion trigger key (e.g. ctrl+space). This is a hook for subclasses
	 * that want to provide their own behavior in this scenario. The default
	 * implementation returns an {@link AutoCompleteAction}.
	 *
	 * @return The action to use.
	 * @see AutoCompleteAction
	 */
	protected Action createAutoCompleteAction() {
		return new AutoCompleteAction();
	}

	/**
	 * Displays the popup window. Hosting applications can call this method to
	 * programmatically begin an auto-completion operation.
	 */
	public void doCompletion() {
		this.refreshPopupWindow();
	}

	/**
	 * Fires an {@link AutoCompletionEvent} of the specified type.
	 *
	 * @param type
	 *            The type of event to fire.
	 */
	protected void fireAutoCompletionEvent(final AutoCompletionEvent.Type type) {

		// Guaranteed to return a non-null array
		final Object[] listeners = this.listeners.getListenerList();
		AutoCompletionEvent e = null;

		// Process the listeners last to first, notifying those that are
		// interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
			if (listeners[i] == AutoCompletionListener.class) {
				if (e == null)
					e = new AutoCompletionEvent(this, type);
				((AutoCompletionListener) listeners[i + 1]).autoCompleteUpdate(e);
			}

	}

	/**
	 * Returns the delay between when the user types a character and when the code
	 * completion popup should automatically appear (if applicable).
	 *
	 * @return The delay, in milliseconds.
	 * @see #setAutoActivationDelay(int)
	 */
	public int getAutoActivationDelay() {
		return this.autoActivationListener.timer.getDelay();
	}

	/**
	 * Returns whether, if a single auto-complete choice is available, it should be
	 * automatically inserted, without displaying the popup menu.
	 *
	 * @return Whether to auto-complete single choices.
	 * @see #setAutoCompleteSingleChoices(boolean)
	 */
	public boolean getAutoCompleteSingleChoices() {
		return this.autoCompleteSingleChoices;
	}

	/**
	 * Returns the completion provider.
	 *
	 * @return The completion provider.
	 */
	public CompletionProvider getCompletionProvider() {
		return this.provider;
	}

	/**
	 * Returns the handler to use when an external URL is clicked in the description
	 * window.
	 *
	 * @return The handler.
	 * @see #setExternalURLHandler(ExternalURLHandler)
	 * @see #getLinkRedirector()
	 */
	public ExternalURLHandler getExternalURLHandler() {
		return this.externalURLHandler;
	}

	int getLineOfCaret() {
		final Document doc = this.textComponent.getDocument();
		final Element root = doc.getDefaultRootElement();
		return root.getElementIndex(this.textComponent.getCaretPosition());
	}

	/**
	 * Returns the default list cell renderer used when a completion provider does
	 * not supply its own.
	 *
	 * @return The default list cell renderer.
	 * @see #setListCellRenderer(ListCellRenderer)
	 */
	public ListCellRenderer getListCellRenderer() {
		return this.renderer;
	}

	/**
	 * Returns the renderer to use for {@link Completion}s in the optional parameter
	 * choices popup window (displayed when a {@link ParameterizedCompletion} is
	 * code-completed). If this returns <code>null</code>, a default renderer is
	 * used.
	 *
	 * @return The renderer to use.
	 * @see #setParamChoicesRenderer(ListCellRenderer)
	 * @see #isParameterAssistanceEnabled()
	 */
	public ListCellRenderer getParamChoicesRenderer() {
		return this.paramChoicesRenderer;
	}

	/**
	 * Returns the text to replace with in the document. This is a "last-chance"
	 * hook for subclasses to make special modifications to the completion text
	 * inserted. The default implementation simply returns
	 * <tt>c.getReplacementText()</tt>. You usually will not need to modify this
	 * method.
	 *
	 * @param c
	 *            The completion being inserted.
	 * @param doc
	 *            The document being modified.
	 * @param start
	 *            The start of the text being replaced.
	 * @param len
	 *            The length of the text being replaced.
	 * @return The text to replace with.
	 */
	protected String getReplacementText(final Completion c, final Document doc, final int start, final int len) {
		return c.getReplacementText();
	}

	/**
	 * Returns whether the "description window" should be shown alongside the
	 * completion window.
	 *
	 * @return Whether the description window should be shown.
	 * @see #setShowDescWindow(boolean)
	 */
	public boolean getShowDescWindow() {
		return this.showDescWindow;
	}

	/**
	 * Returns the text component for which auto-completion is enabled.
	 *
	 * @return The text component, or <code>null</code> if this
	 *         {@link AutoCompletion} is not installed on any text component.
	 * @see #install(JTextComponent)
	 */
	public JTextComponent getTextComponent() {
		return this.textComponent;
	}

	/**
	 * Returns the orientation of the text component we're installed to.
	 *
	 * @return The orientation of the text component, or <code>null</code> if we are
	 *         not installed on one.
	 */
	ComponentOrientation getTextComponentOrientation() {
		return this.textComponent == null ? null : this.textComponent.getComponentOrientation();
	}

	/**
	 * Returns the "trigger key" used for auto-complete.
	 *
	 * @return The trigger key.
	 * @see #setTriggerKey(KeyStroke)
	 */
	public KeyStroke getTriggerKey() {
		return this.trigger;
	}

	/**
	 * Hides any child windows being displayed by the auto-completion system.
	 *
	 * @return Whether any windows were visible.
	 */
	public boolean hideChildWindows() {
		// return hidePopupWindow() || hideToolTipWindow();
		boolean res = this.hidePopupWindow();
		res |= this.hideParameterCompletionPopups();
		return res;
	}

	/**
	 * Hides and disposes of any parameter completion-related popups.
	 *
	 * @return Whether any such windows were visible (and thus hidden).
	 */
	private boolean hideParameterCompletionPopups() {
		if (this.pcc != null) {
			this.pcc.deactivate();
			this.pcc = null;
			return true;
		}
		return false;
	}

	/**
	 * Hides the popup window, if it is visible.
	 *
	 * @return Whether the popup window was visible.
	 */
	protected boolean hidePopupWindow() {
		if (this.popupWindow != null)
			if (this.popupWindow.isVisible()) {
				this.setPopupVisible(false);
				return true;
			}
		return false;
	}

	/**
	 * Inserts a completion. Any time a code completion event occurs, the actual
	 * text insertion happens through this method.
	 *
	 * @param c
	 *            A completion to insert. This cannot be <code>null</code>.
	 */
	protected final void insertCompletion(final Completion c) {
		this.insertCompletion(c, false);
	}

	/**
	 * Inserts a completion. Any time a code completion event occurs, the actual
	 * text insertion happens through this method.
	 *
	 * @param c
	 *            A completion to insert. This cannot be <code>null</code>.
	 * @param typedParamListStartChar
	 *            Whether the parameterized completion start character was typed
	 *            (typically <code>'('</code>).
	 */
	protected void insertCompletion(final Completion c, final boolean typedParamListStartChar) {

		final JTextComponent textComp = this.getTextComponent();
		final String alreadyEntered = c.getAlreadyEntered(textComp);
		this.hidePopupWindow();
		final Caret caret = textComp.getCaret();

		final int dot = caret.getDot();
		final int len = alreadyEntered.length();
		final int start = dot - len;
		final String replacement = this.getReplacementText(c, textComp.getDocument(), start, len);

		caret.setDot(start);
		caret.moveDot(dot);
		textComp.replaceSelection(replacement);

		if (this.isParameterAssistanceEnabled() && c instanceof ParameterizedCompletion) {
			final ParameterizedCompletion pc = (ParameterizedCompletion) c;
			this.startParameterizedCompletionAssistance(pc, typedParamListStartChar);
		}

	}

	/**
	 * Installs this auto-completion on a text component. If this
	 * {@link AutoCompletion} is already installed on another text component, it is
	 * uninstalled first.
	 *
	 * @param c
	 *            The text component.
	 * @see #uninstall()
	 */
	public void install(final JTextComponent c) {

		if (this.textComponent != null)
			this.uninstall();

		this.textComponent = c;
		this.installTriggerKey(this.getTriggerKey());

		// Install the function completion key, if there is one.
		// NOTE: We cannot do this if the start char is ' ' (e.g. just a space
		// between the function name and parameters) because it overrides
		// RSTA's special space action. It seems KeyStorke.getKeyStroke(' ')
		// hoses ctrl+space, shift+space, etc., even though I think it
		// shouldn't...
		final char start = this.provider.getParameterListStart();
		if (start != 0 && start != ' ') {
			final InputMap im = c.getInputMap();
			final ActionMap am = c.getActionMap();
			final KeyStroke ks = KeyStroke.getKeyStroke(start);
			this.oldParenKey = im.get(ks);
			im.put(ks, AutoCompletion.PARAM_COMPLETE_KEY);
			this.oldParenAction = am.get(AutoCompletion.PARAM_COMPLETE_KEY);
			am.put(AutoCompletion.PARAM_COMPLETE_KEY, new ParameterizedCompletionStartAction(start));
		}

		this.textComponentListener.addTo(this.textComponent);
		// In case textComponent is already in a window...
		this.textComponentListener.hierarchyChanged(null);

		if (this.isAutoActivationEnabled())
			this.autoActivationListener.addTo(this.textComponent);

		UIManager.addPropertyChangeListener(this.lafListener);
		this.updateUI(); // In case there have been changes since we uninstalled

	}

	/**
	 * Installs a "trigger key" action onto the current text component.
	 *
	 * @param ks
	 *            The keystroke that should trigger the action.
	 * @see #uninstallTriggerKey()
	 */
	private void installTriggerKey(final KeyStroke ks) {
		final InputMap im = this.textComponent.getInputMap();
		this.oldTriggerKey = im.get(ks);
		im.put(ks, AutoCompletion.PARAM_TRIGGER_KEY);
		final ActionMap am = this.textComponent.getActionMap();
		this.oldTriggerAction = am.get(AutoCompletion.PARAM_TRIGGER_KEY);
		am.put(AutoCompletion.PARAM_TRIGGER_KEY, this.createAutoCompleteAction());
	}

	/**
	 * Returns whether auto-activation is enabled (that is, whether the completion
	 * popup will automatically appear after a delay when the user types an
	 * appropriate character). Note that this parameter will be ignored if
	 * auto-completion is disabled.
	 *
	 * @return Whether auto-activation is enabled.
	 * @see #setAutoActivationEnabled(boolean)
	 * @see #getAutoActivationDelay()
	 * @see #isAutoCompleteEnabled()
	 */
	public boolean isAutoActivationEnabled() {
		return this.autoActivationEnabled;
	}

	/**
	 * Returns whether auto-completion is enabled.
	 *
	 * @return Whether auto-completion is enabled.
	 * @see #setAutoCompleteEnabled(boolean)
	 */
	public boolean isAutoCompleteEnabled() {
		return this.autoCompleteEnabled;
	}

	/**
	 * Whether or not the popup should be hidden when the CompletionProvider
	 * changes. If set to false, caller has to ensure refresh of the popup content.
	 *
	 * @return Whether the popup should be hidden when the completion provider
	 *         changes.
	 * @see #setHideOnCompletionProviderChange(boolean)
	 */
	protected boolean isHideOnCompletionProviderChange() {
		return this.hideOnCompletionProviderChange;
	}

	/**
	 * Whether or not the popup should be hidden when user types a space (or any
	 * character that resets the completion list to "all completions").
	 *
	 * @return Whether the popup should be hidden when the completion list is reset
	 *         to show all completions.
	 * @see #setHideOnNoText(boolean)
	 */
	protected boolean isHideOnNoText() {
		return this.hideOnNoText;
	}

	/**
	 * Returns whether parameter assistance is enabled.
	 *
	 * @return Whether parameter assistance is enabled.
	 * @see #setParameterAssistanceEnabled(boolean)
	 */
	public boolean isParameterAssistanceEnabled() {
		return this.parameterAssistanceEnabled;
	}

	/**
	 * Returns whether the completion popup window is visible.
	 *
	 * @return Whether the completion popup window is visible.
	 */
	public boolean isPopupVisible() {
		return this.popupWindow != null && this.popupWindow.isVisible();
	}

	/**
	 * Refreshes the popup window. First, this method gets the possible completions
	 * for the current caret position. If there are none, and the popup is visible,
	 * it is hidden. If there are some completions and the popup is hidden, it is
	 * made visible and made to display the completions. If there are some
	 * completions and the popup is visible, its list is updated to the current set
	 * of completions.
	 *
	 * @return The current line number of the caret.
	 */
	protected int refreshPopupWindow() {

		// A return value of null => don't suggest completions
		final String text = this.provider.getAlreadyEnteredText(this.textComponent);
		if (text == null && !this.isPopupVisible())
			return this.getLineOfCaret();

		// If the popup is currently visible, and they type a space (or any
		// character that resets the completion list to "all completions"),
		// the popup window should be hidden instead of being reset to show
		// everything.
		final int textLen = text == null ? 0 : text.length();
		if (textLen == 0 && this.isHideOnNoText())
			if (this.isPopupVisible()) {
				this.hidePopupWindow();
				return this.getLineOfCaret();
			}

		final List<Completion> completions = this.provider.getCompletions(this.textComponent);
		final int count = completions == null ? 0 : completions.size();

		if (count > 1 || count == 1 && (this.isPopupVisible() || textLen == 0)
				|| count == 1 && !this.getAutoCompleteSingleChoices()) {

			if (this.popupWindow == null) {
				this.popupWindow = new AutoCompletePopupWindow(this.parentWindow, this);
				this.popupWindowListener.install(this.popupWindow);
				// Completion is usually done for code, which is always done
				// LTR, so make completion stuff RTL only if text component is
				// also RTL.
				this.popupWindow.applyComponentOrientation(this.getTextComponentOrientation());
				if (this.renderer != null)
					this.popupWindow.setListCellRenderer(this.renderer);
				if (this.preferredChoicesWindowSize != null)
					this.popupWindow.setSize(this.preferredChoicesWindowSize);
				if (this.preferredDescWindowSize != null)
					this.popupWindow.setDescriptionWindowSize(this.preferredDescWindowSize);
			}

			this.popupWindow.setCompletions(completions);

			if (!this.popupWindow.isVisible()) {
				Rectangle r = null;
				try {
					r = this.textComponent.modelToView(this.textComponent.getCaretPosition());
				} catch (final BadLocationException ble) {
					ble.printStackTrace();
					return -1;
				}
				final Point p = new Point(r.x, r.y);
				SwingUtilities.convertPointToScreen(p, this.textComponent);
				r.x = p.x;
				r.y = p.y;
				this.popupWindow.setLocationRelativeTo(r);
				this.setPopupVisible(true);
			}

		}

		else if (count == 1)
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					AutoCompletion.this.insertCompletion(completions.get(0));
				}
			});
		else
			this.hidePopupWindow();

		return this.getLineOfCaret();

	}

	/**
	 * Removes a listener interested in popup window events from this instance.
	 *
	 * @param l
	 *            The listener to remove.
	 * @see #addAutoCompletionListener(AutoCompletionListener)
	 */
	public void removeAutoCompletionListener(final AutoCompletionListener l) {
		this.listeners.remove(AutoCompletionListener.class, l);
	}

	/**
	 * Sets the delay between when the user types a character and when the code
	 * completion popup should automatically appear (if applicable).
	 *
	 * @param ms
	 *            The delay, in milliseconds. This should be greater than zero.
	 * @see #getAutoActivationDelay()
	 */
	public void setAutoActivationDelay(int ms) {
		ms = Math.max(0, ms);
		this.autoActivationListener.timer.stop();
		this.autoActivationListener.timer.setInitialDelay(ms);
	}

	/**
	 * Toggles whether auto-activation is enabled. Note that auto-activation also
	 * depends on auto-completion itself being enabled.
	 *
	 * @param enabled
	 *            Whether auto-activation is enabled.
	 * @see #isAutoActivationEnabled()
	 * @see #setAutoActivationDelay(int)
	 */
	public void setAutoActivationEnabled(final boolean enabled) {
		if (enabled != this.autoActivationEnabled) {
			this.autoActivationEnabled = enabled;
			if (this.textComponent != null)
				if (this.autoActivationEnabled)
					this.autoActivationListener.addTo(this.textComponent);
				else
					this.autoActivationListener.removeFrom(this.textComponent);
		}
	}

	/**
	 * Sets whether auto-completion is enabled.
	 *
	 * @param enabled
	 *            Whether auto-completion is enabled.
	 * @see #isAutoCompleteEnabled()
	 */
	public void setAutoCompleteEnabled(final boolean enabled) {
		if (enabled != this.autoCompleteEnabled) {
			this.autoCompleteEnabled = enabled;
			this.hidePopupWindow();
		}
	}

	/**
	 * Sets whether, if a single auto-complete choice is available, it should be
	 * automatically inserted, without displaying the popup menu.
	 *
	 * @param autoComplete
	 *            Whether to auto-complete single choices.
	 * @see #getAutoCompleteSingleChoices()
	 */
	public void setAutoCompleteSingleChoices(final boolean autoComplete) {
		this.autoCompleteSingleChoices = autoComplete;
	}

	/**
	 * Sets the size of the completion choices window.
	 *
	 * @param w
	 *            The new width.
	 * @param h
	 *            The new height.
	 * @see #setDescriptionWindowSize(int, int)
	 */
	public void setChoicesWindowSize(final int w, final int h) {
		this.preferredChoicesWindowSize = new Dimension(w, h);
		if (this.popupWindow != null)
			this.popupWindow.setSize(this.preferredChoicesWindowSize);
	}

	/**
	 * Sets the completion provider being used.
	 *
	 * @param provider
	 *            The new completion provider. This cannot be <code>null</code>.
	 * @throws IllegalArgumentException
	 *             If <code>provider</code> is <code>null</code>.
	 */
	public void setCompletionProvider(final CompletionProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException("provider cannot be null");
		this.provider = provider;
		if (this.isHideOnCompletionProviderChange())
			this.hidePopupWindow(); // In case new choices should be displayed.
	}

	/**
	 * Sets the size of the description window.
	 *
	 * @param w
	 *            The new width.
	 * @param h
	 *            The new height.
	 * @see #setChoicesWindowSize(int, int)
	 */
	public void setDescriptionWindowSize(final int w, final int h) {
		this.preferredDescWindowSize = new Dimension(w, h);
		if (this.popupWindow != null)
			this.popupWindow.setDescriptionWindowSize(this.preferredDescWindowSize);
	}

	/**
	 * Sets the handler to use when an external URL is clicked in the description
	 * window. This handler can perform some action, such as open the URL in a web
	 * browser. The default implementation will open the URL in a browser, but only
	 * if running in Java 6. If you want browser support for Java 5 and below, or
	 * otherwise want to respond to hyperlink clicks, you will have to install your
	 * own handler to do so.
	 *
	 * @param handler
	 *            The new handler.
	 * @see #getExternalURLHandler()
	 */
	public void setExternalURLHandler(final ExternalURLHandler handler) {
		this.externalURLHandler = handler;
	}

	/**
	 * Sets whether or not the popup should be hidden when the CompletionProvider
	 * changes. If set to false, caller has to ensure refresh of the popup content.
	 *
	 * @param hideOnCompletionProviderChange
	 *            Whether the popup should be hidden when the completion provider
	 *            changes.
	 * @see #isHideOnCompletionProviderChange()
	 */
	protected void setHideOnCompletionProviderChange(final boolean hideOnCompletionProviderChange) {
		this.hideOnCompletionProviderChange = hideOnCompletionProviderChange;
	}

	/**
	 * Sets whether or not the popup should be hidden when user types a space (or
	 * any character that resets the completion list to "all completions").
	 *
	 * @param hideOnNoText
	 *            Whether the popup sh ould be hidden when the completion list is
	 *            reset to show all completions.
	 * @see #isHideOnNoText()
	 */
	protected void setHideOnNoText(final boolean hideOnNoText) {
		this.hideOnNoText = hideOnNoText;
	}

	/**
	 * Sets the default list cell renderer to use when a completion provider does
	 * not supply its own.
	 *
	 * @param renderer
	 *            The renderer to use. If this is <code>null</code>, a default
	 *            renderer is used.
	 * @see #getListCellRenderer()
	 */
	public void setListCellRenderer(final ListCellRenderer renderer) {
		this.renderer = renderer;
		if (this.popupWindow != null) {
			this.popupWindow.setListCellRenderer(renderer);
			this.hidePopupWindow();
		}
	}

	/**
	 * Sets the renderer to use for {@link Completion}s in the optional parameter
	 * choices popup window (displayed when a {@link ParameterizedCompletion} is
	 * code-completed). If this isn't set, a default renderer is used.
	 *
	 * @param r
	 *            The renderer to use.
	 * @see #getParamChoicesRenderer()
	 * @see #setParameterAssistanceEnabled(boolean)
	 */
	public void setParamChoicesRenderer(final ListCellRenderer r) {
		this.paramChoicesRenderer = r;
	}

	/**
	 * Sets whether parameter assistance is enabled. If parameter assistance is
	 * enabled, and a "parameterized" completion (such as a function or method) is
	 * inserted, the user will get "assistance" in inserting the parameters in the
	 * form of a popup window with documentation and easy tabbing through the
	 * arguments (as seen in Eclipse and NetBeans).
	 *
	 * @param enabled
	 *            Whether parameter assistance should be enabled.
	 * @see #isParameterAssistanceEnabled()
	 */
	public void setParameterAssistanceEnabled(final boolean enabled) {
		this.parameterAssistanceEnabled = enabled;
	}

	/**
	 * Toggles the visibility of the auto-completion popup window. This fires an
	 * {@link AutoCompletionEvent} of the appropriate type.
	 *
	 * @param visible
	 *            Whether the window should be made visible or hidden.
	 * @see #isPopupVisible()
	 */
	protected void setPopupVisible(final boolean visible) {
		if (visible != this.popupWindow.isVisible())
			this.popupWindow.setVisible(visible);
	}

	/**
	 * Sets whether the "description window" should be shown beside the completion
	 * window.
	 *
	 * @param show
	 *            Whether to show the description window.
	 * @see #getShowDescWindow()
	 */
	public void setShowDescWindow(final boolean show) {
		this.hidePopupWindow(); // Needed to force it to take effect
		this.showDescWindow = show;
	}

	/**
	 * Sets the keystroke that should be used to trigger the auto-complete popup
	 * window.
	 *
	 * @param ks
	 *            The keystroke.
	 * @throws IllegalArgumentException
	 *             If <code>ks</code> is <code>null</code>.
	 * @see #getTriggerKey()
	 */
	public void setTriggerKey(final KeyStroke ks) {
		if (ks == null)
			throw new IllegalArgumentException("trigger key cannot be null");
		if (!ks.equals(this.trigger)) {
			if (this.textComponent != null) {
				// Put old trigger action back.
				this.uninstallTriggerKey();
				// Grab current action for new trigger and replace it.
				this.installTriggerKey(ks);
			}
			this.trigger = ks;
		}
	}

	/**
	 * Displays a "tool tip" detailing the inputs to the function just entered.
	 *
	 * @param pc
	 *            The completion.
	 * @param typedParamListStartChar
	 *            Whether the parameterized completion list starting character was
	 *            typed.
	 */
	private void startParameterizedCompletionAssistance(ParameterizedCompletion pc,
			final boolean typedParamListStartChar) {

		// Get rid of the previous tool tip window, if there is one.
		this.hideParameterCompletionPopups();

		// Don't bother with a tool tip if there are no parameters, but if
		// they typed e.g. the opening '(', make them overtype the ')'.
		if (pc.getParamCount() == 0 && !(pc instanceof TemplateCompletion)) {
			final CompletionProvider p = pc.getProvider();
			final char end = p.getParameterListEnd(); // Might be '\0'
			String text = end == '\0' ? "" : Character.toString(end);
			if (typedParamListStartChar) {
				final String template = "${}" + text + "${cursor}";
				this.textComponent.replaceSelection(Character.toString(p.getParameterListStart()));
				final TemplateCompletion tc = new TemplateCompletion(p, null, null, template);
				pc = tc;
			} else {
				text = p.getParameterListStart() + text;
				this.textComponent.replaceSelection(text);
				return;
			}
		}

		this.pcc = new ParameterizedCompletionContext(this.parentWindow, this, pc);
		this.pcc.activate();

	}

	/**
	 * Uninstalls this auto-completion from its text component. If it is not
	 * installed on any text component, nothing happens.
	 *
	 * @see #install(JTextComponent)
	 */
	public void uninstall() {

		if (this.textComponent != null) {

			this.hidePopupWindow(); // Unregisters listeners, actions, etc.

			this.uninstallTriggerKey();

			// Uninstall the function completion key.
			final char start = this.provider.getParameterListStart();
			if (start != 0) {
				final KeyStroke ks = KeyStroke.getKeyStroke(start);
				final InputMap im = this.textComponent.getInputMap();
				im.put(ks, this.oldParenKey);
				final ActionMap am = this.textComponent.getActionMap();
				am.put(AutoCompletion.PARAM_COMPLETE_KEY, this.oldParenAction);
			}

			this.textComponentListener.removeFrom(this.textComponent);
			if (this.parentWindow != null)
				this.parentWindowListener.removeFrom(this.parentWindow);

			if (this.isAutoActivationEnabled())
				this.autoActivationListener.removeFrom(this.textComponent);

			UIManager.removePropertyChangeListener(this.lafListener);

			this.textComponent = null;
			this.popupWindowListener.uninstall(this.popupWindow);
			this.popupWindow = null;

		}

	}

	/**
	 * Replaces the "trigger key" action with the one that was there before
	 * auto-completion was installed.
	 *
	 * @see #installTriggerKey(KeyStroke)
	 */
	private void uninstallTriggerKey() {
		final InputMap im = this.textComponent.getInputMap();
		im.put(this.trigger, this.oldTriggerKey);
		final ActionMap am = this.textComponent.getActionMap();
		am.put(AutoCompletion.PARAM_TRIGGER_KEY, this.oldTriggerAction);
	}

	/**
	 * Updates the LookAndFeel of the popup window. Applications can call this
	 * method as appropriate if they support changing the LookAndFeel at runtime.
	 */
	private void updateUI() {
		if (this.popupWindow != null)
			this.popupWindow.updateUI();
		if (this.pcc != null)
			this.pcc.updateUI();
		// Will practically always be a JComponent (a JLabel)
		if (this.paramChoicesRenderer instanceof JComponent)
			((JComponent) this.paramChoicesRenderer).updateUI();
	}

}