/*
 * 12/21/2008
 *
 * AutoCompletePopupWindow.java - A window containing a list of auto-complete
 * choices.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ListUI;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;

/**
 * The actual popup window of choices. When visible, this window intercepts
 * certain keystrokes in the parent text component and uses them to navigate the
 * completion choices instead. If Enter or Escape is pressed, the window hides
 * itself and notifies the {@link AutoCompletion} to insert the selected text.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class AutoCompletePopupWindow extends JWindow implements CaretListener, ListSelectionListener, MouseListener {

	class CopyAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			boolean doNormalCopy = false;
			if (AutoCompletePopupWindow.this.descWindow != null && AutoCompletePopupWindow.this.descWindow.isVisible())
				doNormalCopy = !AutoCompletePopupWindow.this.descWindow.copy();
			if (doNormalCopy)
				AutoCompletePopupWindow.this.ac.getTextComponent().copy();
		}

	}

	class DownAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectNextItem();
		}

	}

	class EndAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectLastItem();
		}

	}

	class EnterAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.insertSelectedCompletion();
		}

	}

	class EscapeAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.setVisible(false);
		}

	}

	class HomeAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectFirstItem();
		}

	}

	/**
	 * A mapping from a key (an Object) to an Action.
	 */
	private static class KeyActionPair {

		public Action action;
		public Object key;

		public KeyActionPair() {
		}

		public KeyActionPair(final Object key, final Action a) {
			this.key = key;
			this.action = a;
		}

	}

	class LeftAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible()) {
				final JTextComponent comp = AutoCompletePopupWindow.this.ac.getTextComponent();
				final Caret c = comp.getCaret();
				int dot = c.getDot();
				if (dot > 0) {
					c.setDot(--dot);
					// Ensure moving left hasn't moved us up a line, thus
					// hiding the popup window.
					if (comp.isVisible())
						if (AutoCompletePopupWindow.this.lastLine != -1)
							AutoCompletePopupWindow.this.doAutocomplete();
				}
			}
		}

	}

	class PageDownAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectPageDownItem();
		}

	}

	class PageUpAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectPageUpItem();
		}

	}

	/**
	 * The actual list of completion choices in this popup window.
	 */
	private class PopupList extends JList {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public PopupList(final CompletionListModel model) {
			super(model);
		}

		@Override
		public void setUI(ListUI ui) {
			if (Util.getUseSubstanceRenderers()
					&& AutoCompletePopupWindow.SUBSTANCE_LIST_UI.equals(ui.getClass().getName())) {
				// Substance requires its special ListUI be installed for
				// its renderers to actually render (!), but long completion
				// lists (e.g. PHPCompletionProvider in RSTALanguageSupport)
				// will simply populate too slowly on initial display (when
				// calculating preferred size of all items), so in this case
				// we give a prototype cell value.
				final CompletionProvider p = AutoCompletePopupWindow.this.ac.getCompletionProvider();
				final BasicCompletion bc = new BasicCompletion(p, "Hello world");
				this.setPrototypeCellValue(bc);
			} else {
				// Our custom UI that is faster for long HTML completion lists.
				ui = new FastListUI();
				this.setPrototypeCellValue(null);
			}
			super.setUI(ui);
		}

	}

	class RightAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible()) {
				final JTextComponent comp = AutoCompletePopupWindow.this.ac.getTextComponent();
				final Caret c = comp.getCaret();
				int dot = c.getDot();
				if (dot < comp.getDocument().getLength()) {
					c.setDot(++dot);
					// Ensure moving right hasn't moved us up a line, thus
					// hiding the popup window.
					if (comp.isVisible())
						if (AutoCompletePopupWindow.this.lastLine != -1)
							AutoCompletePopupWindow.this.doAutocomplete();
				}
			}
		}

	}

	class UpAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (AutoCompletePopupWindow.this.isVisible())
				AutoCompletePopupWindow.this.selectPreviousItem();
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The class name of the Substance List UI.
	 */
	private static final String SUBSTANCE_LIST_UI = "org.pushingpixels.substance.internal.ui.SubstanceListUI";
	/**
	 * The space between the caret and the completion popup.
	 */
	private static final int VERTICAL_SPACE = 1;

	/**
	 * Returns the copy keystroke to use for this platform.
	 *
	 * @return The copy keystroke.
	 */
	private static final KeyStroke getCopyKeyStroke() {
		final int key = KeyEvent.VK_C;
		final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		return KeyStroke.getKeyStroke(key, mask);
	}

	/**
	 * Whether the completion window and the optional description window should be
	 * displayed above the current caret position (as opposed to underneath it,
	 * which is preferred unless there is not enough space).
	 */
	private boolean aboveCaret;
	/**
	 * The parent AutoCompletion instance.
	 */
	private final AutoCompletion ac;
	private KeyActionPair ctrlCKap;
	/**
	 * Optional popup window containing a description of the currently selected
	 * completion.
	 */
	private AutoCompleteDescWindow descWindow;
	private KeyActionPair downKap;

	private KeyActionPair endKap;

	private KeyActionPair enterKap;

	private KeyActionPair escapeKap;

	private KeyActionPair homeKap;

	private boolean keyBindingsInstalled;

	private int lastLine;

	/**
	 * A hack to work around the fact that we clear our completion model (and our
	 * selection) when hiding the completion window. This allows us to still know
	 * what the user selected after the popup is hidden.
	 */
	private Completion lastSelection;

	private KeyActionPair leftKap;

	/**
	 * The list of completion choices.
	 */
	private final JList list;

	/**
	 * The contents of {@link #list()}.
	 */
	private final CompletionListModel model;

	private KeyActionPair oldEscape, oldUp, oldDown, oldLeft, oldRight, oldEnter, oldTab, oldHome, oldEnd, oldPageUp,
			oldPageDown, oldCtrlC;

	private KeyActionPair pageDownKap;

	private KeyActionPair pageUpKap;

	/**
	 * The preferred size of the optional description window. This field only exists
	 * because the user may (and usually will) set the size of the description
	 * window before it exists (it must be parented to a Window).
	 */
	private Dimension preferredDescWindowSize;

	private KeyActionPair rightKap;

	private KeyActionPair tabKap;

	private KeyActionPair upKap;

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            The parent window (hosting the text component).
	 * @param ac
	 *            The auto-completion instance.
	 */
	public AutoCompletePopupWindow(final Window parent, final AutoCompletion ac) {

		super(parent);
		final ComponentOrientation o = ac.getTextComponentOrientation();

		this.ac = ac;
		this.model = new CompletionListModel();
		this.list = new PopupList(this.model);

		this.list.setCellRenderer(new DelegatingCellRenderer());
		this.list.addListSelectionListener(this);
		this.list.addMouseListener(this);

		final JPanel contentPane = new JPanel(new BorderLayout());
		final JScrollPane sp = new JScrollPane(this.list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		// In 1.4, JScrollPane.setCorner() has a bug where it won't accept
		// JScrollPane.LOWER_TRAILING_CORNER, even though that constant is
		// defined. So we have to put the logic added in 1.5 to handle it
		// here.
		final JPanel corner = new SizeGrip();
		// sp.setCorner(JScrollPane.LOWER_TRAILING_CORNER, corner);
		final boolean isLeftToRight = o.isLeftToRight();
		final String str = isLeftToRight ? ScrollPaneConstants.LOWER_RIGHT_CORNER
				: ScrollPaneConstants.LOWER_LEFT_CORNER;
		sp.setCorner(str, corner);

		contentPane.add(sp);
		this.setContentPane(contentPane);
		this.applyComponentOrientation(o);

		// Give apps a chance to decorate us with drop shadows, etc.
		if (Util.getShouldAllowDecoratingMainAutoCompleteWindows()) {
			final PopupWindowDecorator decorator = PopupWindowDecorator.get();
			if (decorator != null)
				decorator.decorate(this);
		}

		this.pack();

		this.setFocusableWindowState(false);

		this.lastLine = -1;

	}

	@Override
	public void caretUpdate(final CaretEvent e) {
		if (this.isVisible()) { // Should always be true
			final int line = this.ac.getLineOfCaret();
			if (line != this.lastLine) {
				this.lastLine = -1;
				this.setVisible(false);
			} else
				this.doAutocomplete();
		} else if (AutoCompletion.getDebug())
			Thread.dumpStack();
	}

	/**
	 * Creates the description window.
	 *
	 * @return The description window.
	 */
	private AutoCompleteDescWindow createDescriptionWindow() {
		final AutoCompleteDescWindow dw = new AutoCompleteDescWindow(this, this.ac);
		dw.applyComponentOrientation(this.ac.getTextComponentOrientation());
		Dimension size = this.preferredDescWindowSize;
		if (size == null)
			size = this.getSize();
		dw.setSize(size);
		return dw;
	}

	/**
	 * Creates the mappings from keys to Actions we'll be putting into the text
	 * component's ActionMap and InputMap.
	 */
	private void createKeyActionPairs() {

		// Actions we'll install.
		final EnterAction enterAction = new EnterAction();
		this.escapeKap = new KeyActionPair("Escape", new EscapeAction());
		this.upKap = new KeyActionPair("Up", new UpAction());
		this.downKap = new KeyActionPair("Down", new DownAction());
		this.leftKap = new KeyActionPair("Left", new LeftAction());
		this.rightKap = new KeyActionPair("Right", new RightAction());
		this.enterKap = new KeyActionPair("Enter", enterAction);
		this.tabKap = new KeyActionPair("Tab", enterAction);
		this.homeKap = new KeyActionPair("Home", new HomeAction());
		this.endKap = new KeyActionPair("End", new EndAction());
		this.pageUpKap = new KeyActionPair("PageUp", new PageUpAction());
		this.pageDownKap = new KeyActionPair("PageDown", new PageDownAction());
		this.ctrlCKap = new KeyActionPair("CtrlC", new CopyAction());

		// Buffers for the actions we replace.
		this.oldEscape = new KeyActionPair();
		this.oldUp = new KeyActionPair();
		this.oldDown = new KeyActionPair();
		this.oldLeft = new KeyActionPair();
		this.oldRight = new KeyActionPair();
		this.oldEnter = new KeyActionPair();
		this.oldTab = new KeyActionPair();
		this.oldHome = new KeyActionPair();
		this.oldEnd = new KeyActionPair();
		this.oldPageUp = new KeyActionPair();
		this.oldPageDown = new KeyActionPair();
		this.oldCtrlC = new KeyActionPair();

	}

	protected void doAutocomplete() {
		this.lastLine = this.ac.refreshPopupWindow();
	}

	/**
	 * Returns the default list cell renderer used when a completion provider does
	 * not supply its own.
	 *
	 * @return The default list cell renderer.
	 * @see #setListCellRenderer(ListCellRenderer)
	 */
	public ListCellRenderer getListCellRenderer() {
		final DelegatingCellRenderer dcr = (DelegatingCellRenderer) this.list.getCellRenderer();
		return dcr.getFallbackCellRenderer();
	}

	/**
	 * Returns the selected value, or <code>null</code> if nothing is selected.
	 *
	 * @return The selected value.
	 */
	public Completion getSelection() {
		return this.isShowing() ? (Completion) this.list.getSelectedValue() : this.lastSelection;
	}

	/**
	 * Inserts the currently selected completion.
	 *
	 * @see #getSelection()
	 */
	private void insertSelectedCompletion() {
		final Completion comp = this.getSelection();
		this.ac.insertCompletion(comp);
	}

	/**
	 * Registers keyboard actions to listen for in the text component and intercept.
	 *
	 * @see #uninstallKeyBindings()
	 */
	private void installKeyBindings() {

		if (AutoCompletion.getDebug())
			System.out.println("PopupWindow: Installing keybindings");
		if (this.keyBindingsInstalled) {
			System.err.println("Error: key bindings were already installed");
			Thread.dumpStack();
			return;
		}

		if (this.escapeKap == null)
			this.createKeyActionPairs();

		final JTextComponent comp = this.ac.getTextComponent();
		final InputMap im = comp.getInputMap();
		final ActionMap am = comp.getActionMap();

		this.replaceAction(im, am, KeyEvent.VK_ESCAPE, this.escapeKap, this.oldEscape);
		if (AutoCompletion.getDebug() && this.oldEscape.action == this.escapeKap.action)
			Thread.dumpStack();
		this.replaceAction(im, am, KeyEvent.VK_UP, this.upKap, this.oldUp);
		this.replaceAction(im, am, KeyEvent.VK_LEFT, this.leftKap, this.oldLeft);
		this.replaceAction(im, am, KeyEvent.VK_DOWN, this.downKap, this.oldDown);
		this.replaceAction(im, am, KeyEvent.VK_RIGHT, this.rightKap, this.oldRight);
		this.replaceAction(im, am, KeyEvent.VK_ENTER, this.enterKap, this.oldEnter);
		this.replaceAction(im, am, KeyEvent.VK_TAB, this.tabKap, this.oldTab);
		this.replaceAction(im, am, KeyEvent.VK_HOME, this.homeKap, this.oldHome);
		this.replaceAction(im, am, KeyEvent.VK_END, this.endKap, this.oldEnd);
		this.replaceAction(im, am, KeyEvent.VK_PAGE_UP, this.pageUpKap, this.oldPageUp);
		this.replaceAction(im, am, KeyEvent.VK_PAGE_DOWN, this.pageDownKap, this.oldPageDown);

		// Make Ctrl+C copy from description window. This isn't done
		// automagically because the desc. window is not focusable, and copying
		// from text components can only be done from focused components.
		final KeyStroke ks = AutoCompletePopupWindow.getCopyKeyStroke();
		this.oldCtrlC.key = im.get(ks);
		im.put(ks, this.ctrlCKap.key);
		this.oldCtrlC.action = am.get(this.ctrlCKap.key);
		am.put(this.ctrlCKap.key, this.ctrlCKap.action);

		comp.addCaretListener(this);

		this.keyBindingsInstalled = true;

	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		if (e.getClickCount() == 2)
			this.insertSelectedCompletion();
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mousePressed(final MouseEvent e) {
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
	}

	/**
	 * Positions the description window relative to the completion choices window.
	 * We assume there is room on one side of the other for this entire window to
	 * fit.
	 */
	private void positionDescWindow() {

		final boolean showDescWindow = this.descWindow != null && this.ac.getShowDescWindow();
		if (!showDescWindow)
			return;

		// Don't use getLocationOnScreen() as this throws an exception if
		// window isn't visible yet, but getLocation() doesn't, and is in
		// screen coordinates!
		final Point p = this.getLocation();
		final Rectangle screenBounds = Util.getScreenBoundsForPoint(p.x, p.y);
		// Dimension screenSize = getToolkit().getScreenSize();
		// int totalH = Math.max(getHeight(), descWindow.getHeight());

		// Try to position to the right first (LTR)
		int x;
		if (this.ac.getTextComponentOrientation().isLeftToRight()) {
			x = this.getX() + this.getWidth() + 5;
			if (x + this.descWindow.getWidth() > screenBounds.x + screenBounds.width)
				x = this.getX() - 5 - this.descWindow.getWidth();
		} else { // RTL
			x = this.getX() - 5 - this.descWindow.getWidth();
			if (x < screenBounds.x)
				x = this.getX() + this.getWidth() + 5;
		}

		int y = this.getY();
		if (this.aboveCaret)
			y = y + this.getHeight() - this.descWindow.getHeight();

		if (x != this.descWindow.getX() || y != this.descWindow.getY())
			this.descWindow.setLocation(x, y);

	}

	/**
	 * "Puts back" the original key/Action pair for a keystroke. This is used when
	 * this popup is hidden.
	 *
	 * @param im
	 *            The input map.
	 * @param am
	 *            The action map.
	 * @param key
	 *            The keystroke whose key/Action pair to change.
	 * @param kap
	 *            The (original) key/Action pair.
	 * @see #replaceAction(InputMap, ActionMap, int, KeyActionPair, KeyActionPair)
	 */
	private void putBackAction(final InputMap im, final ActionMap am, final int key, final KeyActionPair kap) {
		final KeyStroke ks = KeyStroke.getKeyStroke(key, 0);
		am.put(im.get(ks), kap.action); // Original action for the "new" key
		im.put(ks, kap.key); // Original key for the keystroke.
	}

	/**
	 * Replaces a key/Action pair in an InputMap and ActionMap with a new pair.
	 *
	 * @param im
	 *            The input map.
	 * @param am
	 *            The action map.
	 * @param key
	 *            The keystroke whose information to replace.
	 * @param kap
	 *            The new key/Action pair for <code>key</code>.
	 * @param old
	 *            A buffer in which to place the old key/Action pair.
	 * @see #putBackAction(InputMap, ActionMap, int, KeyActionPair)
	 */
	private void replaceAction(final InputMap im, final ActionMap am, final int key, final KeyActionPair kap,
			final KeyActionPair old) {
		final KeyStroke ks = KeyStroke.getKeyStroke(key, 0);
		old.key = im.get(ks);
		im.put(ks, kap.key);
		old.action = am.get(kap.key);
		am.put(kap.key, kap.action);
	}

	/**
	 * Selects the first item in the completion list.
	 *
	 * @see #selectLastItem()
	 */
	private void selectFirstItem() {
		if (this.model.getSize() > 0) {
			this.list.setSelectedIndex(0);
			this.list.ensureIndexIsVisible(0);
		}
	}

	/**
	 * Selects the last item in the completion list.
	 *
	 * @see #selectFirstItem()
	 */
	private void selectLastItem() {
		final int index = this.model.getSize() - 1;
		if (index > -1) {
			this.list.setSelectedIndex(index);
			this.list.ensureIndexIsVisible(index);
		}
	}

	/**
	 * Selects the next item in the completion list.
	 *
	 * @see #selectPreviousItem()
	 */
	private void selectNextItem() {
		int index = this.list.getSelectedIndex();
		if (index > -1) {
			index = (index + 1) % this.model.getSize();
			this.list.setSelectedIndex(index);
			this.list.ensureIndexIsVisible(index);
		}
	}

	/**
	 * Selects the completion item one "page down" from the currently selected one.
	 *
	 * @see #selectPageUpItem()
	 */
	private void selectPageDownItem() {
		final int visibleRowCount = this.list.getVisibleRowCount();
		final int i = Math.min(this.list.getModel().getSize() - 1, this.list.getSelectedIndex() + visibleRowCount);
		this.list.setSelectedIndex(i);
		this.list.ensureIndexIsVisible(i);
	}

	/**
	 * Selects the completion item one "page up" from the currently selected one.
	 *
	 * @see #selectPageDownItem()
	 */
	private void selectPageUpItem() {
		final int visibleRowCount = this.list.getVisibleRowCount();
		final int i = Math.max(0, this.list.getSelectedIndex() - visibleRowCount);
		this.list.setSelectedIndex(i);
		this.list.ensureIndexIsVisible(i);
	}

	/**
	 * Selects the previous item in the completion list.
	 *
	 * @see #selectNextItem()
	 */
	private void selectPreviousItem() {
		int index = this.list.getSelectedIndex();
		switch (index) {
		case 0:
			index = this.list.getModel().getSize() - 1;
			break;
		case -1: // Check for an empty list (would be an error)
			index = this.list.getModel().getSize() - 1;
			if (index == -1)
				return;
			break;
		default:
			index = index - 1;
			break;
		}
		this.list.setSelectedIndex(index);
		this.list.ensureIndexIsVisible(index);
	}

	/**
	 * Sets the completions to display in the choices list. The first completion is
	 * selected.
	 *
	 * @param completions
	 *            The completions to display.
	 */
	public void setCompletions(final List<Completion> completions) {
		this.model.setContents(completions);
		this.selectFirstItem();
	}

	/**
	 * Sets the size of the description window.
	 *
	 * @param size
	 *            The new size. This cannot be <code>null</code>.
	 */
	public void setDescriptionWindowSize(final Dimension size) {
		if (this.descWindow != null)
			this.descWindow.setSize(size);
		else
			this.preferredDescWindowSize = size;
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
		final DelegatingCellRenderer dcr = (DelegatingCellRenderer) this.list.getCellRenderer();
		dcr.setFallbackCellRenderer(renderer);
	}

	/**
	 * Sets the location of this window to be "good" relative to the specified
	 * rectangle. That rectangle should be the location of the text component's
	 * caret, in screen coordinates.
	 *
	 * @param r
	 *            The text component's caret position, in screen coordinates.
	 */
	public void setLocationRelativeTo(final Rectangle r) {

		// Multi-monitor support - make sure the completion window (and
		// description window, if applicable) both fit in the same window in
		// a multi-monitor environment. To do this, we decide which monitor
		// the rectangle "r" is in, and use that one (just pick top-left corner
		// as the defining point).
		final Rectangle screenBounds = Util.getScreenBoundsForPoint(r.x, r.y);
		// Dimension screenSize = getToolkit().getScreenSize();

		final boolean showDescWindow = this.descWindow != null && this.ac.getShowDescWindow();
		int totalH = this.getHeight();
		if (showDescWindow)
			totalH = Math.max(totalH, this.descWindow.getHeight());

		// Try putting our stuff "below" the caret first. We assume that the
		// entire height of our stuff fits on the screen one way or the other.
		this.aboveCaret = false;
		int y = r.y + r.height + AutoCompletePopupWindow.VERTICAL_SPACE;
		if (y + totalH > screenBounds.height) {
			y = r.y - AutoCompletePopupWindow.VERTICAL_SPACE - this.getHeight();
			this.aboveCaret = true;
		}

		// Get x-coordinate of completions. Try to align left edge with the
		// caret first.
		int x = r.x;
		if (!this.ac.getTextComponentOrientation().isLeftToRight())
			x -= this.getWidth(); // RTL => align right edge
		if (x < screenBounds.x)
			x = screenBounds.x;
		else if (x + this.getWidth() > screenBounds.x + screenBounds.width)
			x = screenBounds.x + screenBounds.width - this.getWidth();

		this.setLocation(x, y);

		// Position the description window, if necessary.
		if (showDescWindow)
			this.positionDescWindow();

	}

	/**
	 * Toggles the visibility of this popup window.
	 *
	 * @param visible
	 *            Whether this window should be visible.
	 */
	@Override
	public void setVisible(final boolean visible) {

		if (visible != this.isVisible()) {

			if (visible) {
				this.installKeyBindings();
				this.lastLine = this.ac.getLineOfCaret();
				this.selectFirstItem();
				if (this.descWindow == null && this.ac.getShowDescWindow()) {
					this.descWindow = this.createDescriptionWindow();
					this.positionDescWindow();
				}
				// descWindow needs a kick-start the first time it's displayed.
				// Also, the newly-selected item in the choices list is
				// probably different from the previous one anyway.
				if (this.descWindow != null) {
					final Completion c = (Completion) this.list.getSelectedValue();
					if (c != null)
						this.descWindow.setDescriptionFor(c);
				}
			} else
				this.uninstallKeyBindings();

			super.setVisible(visible);

			// Some languages, such as Java, can use quite a lot of memory
			// when displaying hundreds of completion choices. We pro-actively
			// clear our list model here to make them available for GC.
			// Otherwise, they stick around, and consider the following: a
			// user starts code-completion for Java 5 SDK classes, then hides
			// the dialog, then changes the "class path" to use a Java 6 SDK
			// instead. On pressing Ctrl+space, a new array of Completions is
			// created. If this window holds on to the previous Completions,
			// you're getting roughly 2x the necessary Completions in memory
			// until the Completions are actually passed to this window.
			if (!visible) { // Do after super.setVisible(false)
				this.lastSelection = (Completion) this.list.getSelectedValue();
				this.model.clear();
			}

			// Must set descWindow's visibility one way or the other each time,
			// because of the way child JWindows' visibility is handled - in
			// some ways it's dependent on the parent, in other ways it's not.
			if (this.descWindow != null)
				this.descWindow.setVisible(visible && this.ac.getShowDescWindow());

		}

	}

	/**
	 * Stops intercepting certain keystrokes from the text component.
	 *
	 * @see #installKeyBindings()
	 */
	private void uninstallKeyBindings() {

		if (AutoCompletion.getDebug())
			System.out.println("PopupWindow: Removing keybindings");
		if (!this.keyBindingsInstalled)
			return;

		final JTextComponent comp = this.ac.getTextComponent();
		final InputMap im = comp.getInputMap();
		final ActionMap am = comp.getActionMap();

		this.putBackAction(im, am, KeyEvent.VK_ESCAPE, this.oldEscape);
		this.putBackAction(im, am, KeyEvent.VK_UP, this.oldUp);
		this.putBackAction(im, am, KeyEvent.VK_DOWN, this.oldDown);
		this.putBackAction(im, am, KeyEvent.VK_LEFT, this.oldLeft);
		this.putBackAction(im, am, KeyEvent.VK_RIGHT, this.oldRight);
		this.putBackAction(im, am, KeyEvent.VK_ENTER, this.oldEnter);
		this.putBackAction(im, am, KeyEvent.VK_TAB, this.oldTab);
		this.putBackAction(im, am, KeyEvent.VK_HOME, this.oldHome);
		this.putBackAction(im, am, KeyEvent.VK_END, this.oldEnd);
		this.putBackAction(im, am, KeyEvent.VK_PAGE_UP, this.oldPageUp);
		this.putBackAction(im, am, KeyEvent.VK_PAGE_DOWN, this.oldPageDown);

		// Ctrl+C
		final KeyStroke ks = AutoCompletePopupWindow.getCopyKeyStroke();
		am.put(im.get(ks), this.oldCtrlC.action); // Original action
		im.put(ks, this.oldCtrlC.key); // Original key

		comp.removeCaretListener(this);

		this.keyBindingsInstalled = false;

	}

	/**
	 * Updates the <tt>LookAndFeel</tt> of this window and the description window.
	 */
	public void updateUI() {
		SwingUtilities.updateComponentTreeUI(this);
		if (this.descWindow != null)
			this.descWindow.updateUI();
	}

	/**
	 * Called when a new item is selected in the popup list.
	 *
	 * @param e
	 *            The event.
	 */
	@Override
	public void valueChanged(final ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			final Object value = this.list.getSelectedValue();
			if (value != null && this.descWindow != null) {
				this.descWindow.setDescriptionFor((Completion) value);
				this.positionDescWindow();
			}
		}
	}

}