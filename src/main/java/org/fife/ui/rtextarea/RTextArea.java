/*
 * 11/14/2003
 *
 * RTextArea.java - An extension of JTextArea that adds many features.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.fife.print.RPrintUtilities;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rtextarea.Macro.MacroRecord;

/**
 * An extension of <code>JTextArea</code> that adds the following features:
 * <ul>
 * <li>Insert/Overwrite modes (can be toggled via the Insert key)
 * <li>A right-click popup menu with standard editing options
 * <li>Macro support
 * <li>"Mark all" functionality.
 * <li>A way to change the background to an image (gif/png/jpg)
 * <li>Highlight the current line (can be toggled)
 * <li>An easy way to print its text (implements Printable)
 * <li>Hard/soft (emulated with spaces) tabs
 * <li>Fixes a bug with setTabSize
 * <li>Other handy new methods
 * </ul>
 * NOTE: If the background for an <code>RTextArea</code> is set to a color, its
 * opaque property is set to <code>true</code> for performance reasons. If the
 * background is set to an image, then the opaque property is set to
 * <code>false</code>. This slows things down a little, but if it didn't happen
 * then we would see garbage on-screen when the user scrolled through a document
 * using the arrow keys (not the page-up/down keys though). You should never
 * have to set the opaque property yourself; it is always done for you.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RTextArea extends RTextAreaBase implements Printable {

	/**
	 * Modified from <code>MutableCaretEvent</code> in <code>JTextComponent</code>
	 * so that mouse events get fired when the user is selecting text with the mouse
	 * as well. This class also displays the popup menu when the user right-clicks
	 * in the text area.
	 */
	protected class RTextAreaMutableCaretEvent extends RTAMouseListener {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		protected RTextAreaMutableCaretEvent(final RTextArea textArea) {
			super(textArea);
		}

		@Override
		public void focusGained(final FocusEvent e) {
			final Caret c = RTextArea.this.getCaret();
			final boolean enabled = c.getDot() != c.getMark();
			RTextArea.cutAction.setEnabled(enabled);
			RTextArea.copyAction.setEnabled(enabled);
			RTextArea.this.undoManager.updateActions(); // To reflect this text area.
		}

		@Override
		public void focusLost(final FocusEvent e) {
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			// WORKAROUND: Since JTextComponent only updates the caret
			// location on mouse clicked and released, we'll do it on dragged
			// events when the left mouse button is clicked.
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				final Caret caret = RTextArea.this.getCaret();
				this.dot = caret.getDot();
				this.mark = caret.getMark();
				RTextArea.this.fireCaretUpdate(this);
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			if (e.isPopupTrigger())
				this.showPopup(e);
			else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				final Caret caret = RTextArea.this.getCaret();
				this.dot = caret.getDot();
				this.mark = caret.getMark();
				RTextArea.this.fireCaretUpdate(this);
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (e.isPopupTrigger())
				this.showPopup(e);
		}

		/**
		 * Shows a popup menu with cut, copy, paste, etc. options if the user clicked
		 * the right button.
		 *
		 * @param e
		 *            The mouse event that caused this method to be called.
		 */
		private void showPopup(final MouseEvent e) {
			final JPopupMenu popupMenu = RTextArea.this.getPopupMenu();
			if (popupMenu != null) {
				RTextArea.this.configurePopupMenu(popupMenu);
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
				e.consume();
			}
		}

	}

	public static final int COPY_ACTION = 0;

	private static RecordableTextAction copyAction;

	private static Macro currentMacro;

	public static final int CUT_ACTION = 1;

	private static RecordableTextAction cutAction;

	private static final Color DEFAULT_MARK_ALL_COLOR = new Color(0xffc800);
	public static final int DELETE_ACTION = 2;
	private static RecordableTextAction deleteAction;
	private static IconGroup iconGroup; // Info on icons for actions.
	/**
	 * Constant representing insert mode.
	 *
	 * @see #setCaretStyle(int, CaretStyle)
	 */
	public static final int INSERT_MODE = 0;
	/**
	 * The property fired when the "mark all" color changes.
	 */
	public static final String MARK_ALL_COLOR_PROPERTY = "RTA.markAllColor";
	/**
	 * The property fired when what ranges are labeled "mark all" changes.
	 */
	public static final String MARK_ALL_OCCURRENCES_CHANGED_PROPERTY = "RTA.markAllOccurrencesChanged";
	/**
	 * The property fired when the "mark all on occurrence" property changes.
	 */
	public static final String MARK_ALL_ON_OCCURRENCE_SEARCHES_PROPERTY = "RTA.markAllOnOccurrenceSearches";
	private static final int MAX_ACTION_CONSTANT = 6;

	/*
	 * Constants for all actions.
	 */
	private static final int MIN_ACTION_CONSTANT = 0;

	private static final String MSG = "org.fife.ui.rtextarea.RTextArea";

	/**
	 * Constant representing overwrite mode.
	 *
	 * @see #setCaretStyle(int, CaretStyle)
	 */
	public static final int OVERWRITE_MODE = 1;
	public static final int PASTE_ACTION = 3;

	private static RecordableTextAction pasteAction;

	// All macros are shared across all RTextAreas.
	private static boolean recordingMacro; // Whether we're recording a macro.
	public static final int REDO_ACTION = 4;
	private static RecordableTextAction redoAction;
	private static StringBuilder repTabsSB;
	private static Segment repTabsSeg = new Segment();

	public static final int SELECT_ALL_ACTION = 5;

	private static RecordableTextAction selectAllAction;

	/**
	 * The text last searched for via Ctrl+K or Ctrl+Shift+K.
	 */
	private static String selectedOccurrenceText;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final int UNDO_ACTION = 6;
	private static RecordableTextAction undoAction;

	/**
	 * Adds an action event to the current macro. This shouldn't be called directly,
	 * as it is called by the actions themselves.
	 *
	 * @param id
	 *            The ID of the recordable text action.
	 * @param actionCommand
	 *            The "command" of the action event passed to it.
	 */
	static synchronized void addToCurrentMacro(final String id, final String actionCommand) {
		RTextArea.currentMacro.addMacroRecord(new Macro.MacroRecord(id, actionCommand));
	}

	/**
	 * Begins recording a macro. After this method is called, all input/caret
	 * events, etc. are recorded until <code>endMacroRecording</code> is called. If
	 * this method is called but the text component is already recording a macro,
	 * nothing happens (but the macro keeps recording).
	 *
	 * @see #isRecordingMacro()
	 * @see #endRecordingMacro()
	 */
	public static synchronized void beginRecordingMacro() {
		if (RTextArea.isRecordingMacro())
			// System.err.println("Macro already being recorded!");
			return;
		// JOptionPane.showMessageDialog(this, "Now recording a macro");
		if (RTextArea.currentMacro != null)
			RTextArea.currentMacro = null; // May help gc?
		RTextArea.currentMacro = new Macro();
		RTextArea.recordingMacro = true;
	}

	/**
	 * Creates the actions used in the popup menu and retrievable by
	 * {@link #getAction(int)}. TODO: Remove these horrible hacks and move
	 * localizing of actions into the editor kits, where it should be! The context
	 * menu should contain actions from the editor kits.
	 */
	private static void createPopupMenuActions() {

		// Create actions for right-click popup menu.
		// 1.5.2004/pwy: Replaced the CTRL_MASK with the cross-platform version...
		final int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		final ResourceBundle msg = ResourceBundle.getBundle(RTextArea.MSG);

		RTextArea.cutAction = new RTextAreaEditorKit.CutAction();
		RTextArea.cutAction.setProperties(msg, "Action.Cut");
		RTextArea.cutAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, mod));
		RTextArea.copyAction = new RTextAreaEditorKit.CopyAction();
		RTextArea.copyAction.setProperties(msg, "Action.Copy");
		RTextArea.copyAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, mod));
		RTextArea.pasteAction = new RTextAreaEditorKit.PasteAction();
		RTextArea.pasteAction.setProperties(msg, "Action.Paste");
		RTextArea.pasteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, mod));
		RTextArea.deleteAction = new RTextAreaEditorKit.DeleteNextCharAction();
		RTextArea.deleteAction.setProperties(msg, "Action.Delete");
		RTextArea.deleteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		RTextArea.undoAction = new RTextAreaEditorKit.UndoAction();
		RTextArea.undoAction.setProperties(msg, "Action.Undo");
		RTextArea.undoAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod));
		RTextArea.redoAction = new RTextAreaEditorKit.RedoAction();
		RTextArea.redoAction.setProperties(msg, "Action.Redo");
		RTextArea.redoAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod));
		RTextArea.selectAllAction = new RTextAreaEditorKit.SelectAllAction();
		RTextArea.selectAllAction.setProperties(msg, "Action.SelectAll");
		RTextArea.selectAllAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, mod));

	}

	/**
	 * Ends recording a macro. If this method is called but the text component is
	 * not recording a macro, nothing happens.
	 *
	 * @see #isRecordingMacro()
	 * @see #beginRecordingMacro()
	 */
	/*
	 * FIXME: This should throw an exception if we're not recording a macro.
	 */
	public static synchronized void endRecordingMacro() {
		if (!RTextArea.isRecordingMacro())
			// System.err.println("Not recording a macro!");
			return;
		RTextArea.recordingMacro = false;
	}

	/**
	 * Provides a way to gain access to the editor actions on the right-click popup
	 * menu. This way you can make toolbar/menu bar items use the actual actions
	 * used by all <code>RTextArea</code>s, so that icons stay synchronized and you
	 * don't have to worry about enabling/disabling them yourself.
	 * <p>
	 * Keep in mind that these actions are shared across all instances of
	 * <code>RTextArea</code>, so a change to any action returned by this method is
	 * global across all <code>RTextArea</code> editors in your application.
	 *
	 * @param action
	 *            The action to retrieve, such as {@link #CUT_ACTION}. If the action
	 *            name is invalid, <code>null</code> is returned.
	 * @return The action, or <code>null</code> if an invalid action is requested.
	 */
	public static RecordableTextAction getAction(final int action) {
		if (action < RTextArea.MIN_ACTION_CONSTANT || action > RTextArea.MAX_ACTION_CONSTANT)
			return null;
		switch (action) {
		case COPY_ACTION:
			return RTextArea.copyAction;
		case CUT_ACTION:
			return RTextArea.cutAction;
		case DELETE_ACTION:
			return RTextArea.deleteAction;
		case PASTE_ACTION:
			return RTextArea.pasteAction;
		case REDO_ACTION:
			return RTextArea.redoAction;
		case SELECT_ALL_ACTION:
			return RTextArea.selectAllAction;
		case UNDO_ACTION:
			return RTextArea.undoAction;
		}
		return null;
	}

	/**
	 * Returns the macro currently stored in this <code>RTextArea</code>. Since
	 * macros are shared, all <code>RTextArea</code>s in the currently- running
	 * application are using this macro.
	 *
	 * @return The current macro, or <code>null</code> if no macro has been
	 *         recorded/loaded.
	 * @see #loadMacro(Macro)
	 */
	public static synchronized Macro getCurrentMacro() {
		return RTextArea.currentMacro;
	}

	/**
	 * Returns the default color used for "mark all" highlights.
	 *
	 * @return The color.
	 * @see #getMarkAllHighlightColor()
	 * @see #setMarkAllHighlightColor(Color)
	 */
	public static final Color getDefaultMarkAllHighlightColor() {
		return RTextArea.DEFAULT_MARK_ALL_COLOR;
	}

	/**
	 * Returns the icon group being used for the actions of this text area.
	 *
	 * @return The icon group.
	 * @see #setIconGroup(IconGroup)
	 */
	public static IconGroup getIconGroup() {
		return RTextArea.iconGroup;
	}

	/**
	 * Returns the text last selected and used in a Ctrl+K operation.
	 *
	 * @return The text, or <code>null</code> if none.
	 * @see #setSelectedOccurrenceText(String)
	 */
	public static String getSelectedOccurrenceText() {
		return RTextArea.selectedOccurrenceText;
	}

	/**
	 * Returns whether or not a macro is being recorded.
	 *
	 * @return Whether or not a macro is being recorded.
	 * @see #beginRecordingMacro()
	 * @see #endRecordingMacro()
	 */
	public static synchronized boolean isRecordingMacro() {
		return RTextArea.recordingMacro;
	}

	/**
	 * Loads a macro to be used by all <code>RTextArea</code>s in the current
	 * application.
	 *
	 * @param macro
	 *            The macro to load.
	 * @see #getCurrentMacro()
	 */
	public static synchronized void loadMacro(final Macro macro) {
		RTextArea.currentMacro = macro;
	}

	/**
	 * Sets the properties of one of the actions this text area owns.
	 *
	 * @param action
	 *            The action to modify; for example, {@link #CUT_ACTION}.
	 * @param name
	 *            The new name for the action.
	 * @param mnemonic
	 *            The new mnemonic for the action.
	 * @param accelerator
	 *            The new accelerator key for the action.
	 */
	public static void setActionProperties(final int action, final String name, final char mnemonic,
			final KeyStroke accelerator) {
		RTextArea.setActionProperties(action, name, Integer.valueOf(mnemonic), accelerator);
	}

	/**
	 * Sets the properties of one of the actions this text area owns.
	 *
	 * @param action
	 *            The action to modify; for example, {@link #CUT_ACTION}.
	 * @param name
	 *            The new name for the action.
	 * @param mnemonic
	 *            The new mnemonic for the action.
	 * @param accelerator
	 *            The new accelerator key for the action.
	 */
	public static void setActionProperties(final int action, final String name, final Integer mnemonic,
			final KeyStroke accelerator) {

		Action tempAction = null;

		switch (action) {
		case CUT_ACTION:
			tempAction = RTextArea.cutAction;
			break;
		case COPY_ACTION:
			tempAction = RTextArea.copyAction;
			break;
		case PASTE_ACTION:
			tempAction = RTextArea.pasteAction;
			break;
		case DELETE_ACTION:
			tempAction = RTextArea.deleteAction;
			break;
		case SELECT_ALL_ACTION:
			tempAction = RTextArea.selectAllAction;
			break;
		case UNDO_ACTION:
		case REDO_ACTION:
		default:
			return;
		}

		tempAction.putValue(Action.NAME, name);
		tempAction.putValue(Action.SHORT_DESCRIPTION, name);
		tempAction.putValue(Action.ACCELERATOR_KEY, accelerator);
		tempAction.putValue(Action.MNEMONIC_KEY, mnemonic);

	}

	/**
	 * Sets the path in which to find images to associate with the editor's actions.
	 * The path MUST contain the following images (with the appropriate extension as
	 * defined by the icon group):<br>
	 * <ul>
	 * <li>cut</li>
	 * <li>copy</li>
	 * <li>paste</li>
	 * <li>delete</li>
	 * <li>undo</li>
	 * <li>redo</li>
	 * <li>selectall</li>
	 * </ul>
	 * If any of the above images don't exist, the corresponding action will not
	 * have an icon.
	 *
	 * @param group
	 *            The icon group to load.
	 * @see #getIconGroup()
	 */
	public static synchronized void setIconGroup(final IconGroup group) {
		Icon icon = group.getIcon("cut");
		RTextArea.cutAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("copy");
		RTextArea.copyAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("paste");
		RTextArea.pasteAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("delete");
		RTextArea.deleteAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("undo");
		RTextArea.undoAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("redo");
		RTextArea.redoAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("selectall");
		RTextArea.selectAllAction.putValue(Action.SMALL_ICON, icon);
		RTextArea.iconGroup = group;
	}

	/**
	 * Sets the text last selected/Ctrl+K'd in an <code>RTextArea</code>. This text
	 * will be searched for in subsequent Ctrl+K/Ctrl+Shift+K actions (Cmd+K on OS
	 * X).
	 * <p>
	 *
	 * Since the selected occurrence actions are built into RTextArea, applications
	 * usually do not have to call this method directly, but can choose to do so if
	 * they wish (for example, if they wish to set this value when the user does a
	 * search via a Find dialog).
	 *
	 * @param text
	 *            The selected text.
	 * @see #getSelectedOccurrenceText()
	 */
	public static void setSelectedOccurrenceText(final String text) {
		RTextArea.selectedOccurrenceText = text;
	}

	private CaretStyle[] carets; // Index 0=>insert caret, 1=>overwrite.

	private JMenuItem cutMenuItem;

	private JMenuItem deleteMenuItem;

	private transient LineHighlightManager lineHighlightManager;

	private SmartHighlightPainter markAllHighlightPainter;

	private boolean markAllOnOccurrenceSearches;

	private JMenuItem pasteMenuItem;

	/**
	 * This text area's popup menu.
	 */
	private JPopupMenu popupMenu;

	/**
	 * Whether the popup menu has been created.
	 */
	private boolean popupMenuCreated;

	private JMenuItem redoMenuItem;

	/**
	 * The current text mode ({@link #INSERT_MODE} or {@link #OVERWRITE_MODE}).
	 */
	private int textMode;

	/**
	 * Can return tool tips for this text area. Subclasses can install a supplier as
	 * a means of adding custom tool tips without subclassing <tt>RTextArea</tt>.
	 * {@link #getToolTipText()} checks this supplier before calling the super
	 * class's version.
	 */
	private ToolTipSupplier toolTipSupplier;

	private transient RUndoManager undoManager;

	private JMenuItem undoMenuItem;

	/**
	 * Constructor.
	 */
	public RTextArea() {
	}

	/**
	 * Constructor.
	 *
	 * @param doc
	 *            The document for the editor.
	 */
	public RTextArea(final AbstractDocument doc) {
		super(doc);
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
	public RTextArea(final AbstractDocument doc, final String text, final int rows, final int cols) {
		super(doc, text, rows, cols);
	}

	/**
	 * Creates a new <code>RTextArea</code>.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 */
	public RTextArea(final int textMode) {
		this.setTextMode(textMode);
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
	public RTextArea(final int rows, final int cols) {
		super(rows, cols);
	}

	/**
	 * Constructor.
	 *
	 * @param text
	 *            The initial text to display.
	 */
	public RTextArea(final String text) {
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
	public RTextArea(final String text, final int rows, final int cols) {
		super(text, rows, cols);
	}

	/**
	 * Adds a line highlight.
	 *
	 * @param line
	 *            The line to highlight. This is zero-based.
	 * @param color
	 *            The color to highlight the line with.
	 * @throws BadLocationException
	 *             If <code>line</code> is an invalid line number.
	 * @see #removeLineHighlight(Object)
	 * @see #removeAllLineHighlights()
	 */
	public Object addLineHighlight(final int line, final Color color) throws BadLocationException {
		if (this.lineHighlightManager == null)
			this.lineHighlightManager = new LineHighlightManager(this);
		return this.lineHighlightManager.addLineHighlight(line, color);
	}

	/**
	 * Begins an "atomic edit." All text editing operations between this call and
	 * the next call to <tt>endAtomicEdit()</tt> will be treated as a single
	 * operation by the undo manager.
	 * <p>
	 *
	 * Using this method should be done with great care. You should probably wrap
	 * the call to <tt>endAtomicEdit()</tt> in a <tt>finally</tt> block:
	 *
	 * <pre>
	 * textArea.beginAtomicEdit();
	 * try {
	 * 	// Do editing
	 * } finally {
	 * 	textArea.endAtomicEdit();
	 * }
	 * </pre>
	 *
	 * @see #endAtomicEdit()
	 */
	public void beginAtomicEdit() {
		this.undoManager.beginInternalAtomicEdit();
	}

	/**
	 * Returns whether a redo is possible.
	 *
	 * @see #canUndo()
	 * @see #redoLastAction()
	 */
	public boolean canRedo() {
		return this.undoManager.canRedo();
	}

	/**
	 * Returns whether an undo is possible.
	 *
	 * @see #canRedo()
	 * @see #undoLastAction()
	 */
	public boolean canUndo() {
		return this.undoManager.canUndo();
	}

	/**
	 * Clears any "mark all" highlights, if any.
	 *
	 * @see #markAll(List)
	 * @see #getMarkAllHighlightColor()
	 * @see #setMarkAllHighlightColor(Color)
	 */
	void clearMarkAllHighlights() {
		((RTextAreaHighlighter) this.getHighlighter()).clearMarkAllHighlights();
		// markedWord = null;
		this.repaint();
	}

	/**
	 * Configures the popup menu for this text area. This method is called right
	 * before it is displayed, so a hosting application can do any custom
	 * configuration (configuring actions, adding/removing items, etc.).
	 * <p>
	 *
	 * The default implementation does nothing.
	 * <p>
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
	protected void configurePopupMenu(final JPopupMenu popupMenu) {

		final boolean canType = this.isEditable() && this.isEnabled();

		// Since the user can customize the popup menu, these actions may not
		// have been created.
		if (this.undoMenuItem != null) {
			this.undoMenuItem.setEnabled(RTextArea.undoAction.isEnabled() && canType);
			this.redoMenuItem.setEnabled(RTextArea.redoAction.isEnabled() && canType);
			this.cutMenuItem.setEnabled(RTextArea.cutAction.isEnabled() && canType);
			this.pasteMenuItem.setEnabled(RTextArea.pasteAction.isEnabled() && canType);
			this.deleteMenuItem.setEnabled(RTextArea.deleteAction.isEnabled() && canType);
		}

	}

	/**
	 * Creates the default implementation of the model to be used at construction if
	 * one isn't explicitly given. A new instance of RDocument is returned.
	 *
	 * @return The default document.
	 */
	@Override
	protected Document createDefaultModel() {
		return new RDocument();
	}

	/**
	 * Returns the caret event/mouse listener for <code>RTextArea</code>s.
	 *
	 * @return The caret event/mouse listener.
	 */
	@Override
	protected RTAMouseListener createMouseListener() {
		return new RTextAreaMutableCaretEvent(this);
	}

	/**
	 * Creates the right-click popup menu. Subclasses can override this method to
	 * replace or augment the popup menu returned.
	 *
	 * @return The popup menu.
	 * @see #setPopupMenu(JPopupMenu)
	 * @see #configurePopupMenu(JPopupMenu)
	 * @see #createPopupMenuItem(Action)
	 */
	protected JPopupMenu createPopupMenu() {
		final JPopupMenu menu = new JPopupMenu();
		menu.add(this.undoMenuItem = this.createPopupMenuItem(RTextArea.undoAction));
		menu.add(this.redoMenuItem = this.createPopupMenuItem(RTextArea.redoAction));
		menu.addSeparator();
		menu.add(this.cutMenuItem = this.createPopupMenuItem(RTextArea.cutAction));
		menu.add(this.createPopupMenuItem(RTextArea.copyAction));
		menu.add(this.pasteMenuItem = this.createPopupMenuItem(RTextArea.pasteAction));
		menu.add(this.deleteMenuItem = this.createPopupMenuItem(RTextArea.deleteAction));
		menu.addSeparator();
		menu.add(this.createPopupMenuItem(RTextArea.selectAllAction));
		return menu;
	}

	/**
	 * Creates and configures a menu item for used in the popup menu.
	 *
	 * @param a
	 *            The action for the menu item.
	 * @return The menu item.
	 * @see #createPopupMenu()
	 */
	protected JMenuItem createPopupMenuItem(final Action a) {
		final JMenuItem item = new JMenuItem(a) {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void setToolTipText(final String text) {
				// Ignore! Actions (e.g. undo/redo) set this when changing
				// their text due to changing enabled state.
			}
		};
		item.setAccelerator(null);
		return item;
	}

	/**
	 * Returns the a real UI to install on this text area.
	 *
	 * @return The UI.
	 */
	@Override
	protected RTextAreaUI createRTextAreaUI() {
		return new RTextAreaUI(this);
	}

	/**
	 * Creates a string of space characters of the specified size.
	 *
	 * @param size
	 *            The number of spaces.
	 * @return The string of spaces.
	 */
	private String createSpacer(final int size) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++)
			sb.append(' ');
		return sb.toString();
	}

	/**
	 * Creates an undo manager for use in this text area.
	 *
	 * @return The undo manager.
	 */
	protected RUndoManager createUndoManager() {
		return new RUndoManager(this);
	}

	/**
	 * Removes all undoable edits from this document's undo manager. This method
	 * also makes the undo/redo actions disabled.
	 */
	/*
	 * NOTE: For some reason, it appears I have to create an entirely new
	 * <code>undoManager</code> for undo/redo to continue functioning properly; if I
	 * don't, it only ever lets you do one undo. Not too sure why this is...
	 */
	public void discardAllEdits() {
		this.undoManager.discardAllEdits();
		this.getDocument().removeUndoableEditListener(this.undoManager);
		this.undoManager = this.createUndoManager();
		this.getDocument().addUndoableEditListener(this.undoManager);
		this.undoManager.updateActions();
	}

	/**
	 * Completes an "atomic" edit.
	 *
	 * @see #beginAtomicEdit()
	 */
	public void endAtomicEdit() {
		this.undoManager.endInternalAtomicEdit();
	}

	/**
	 * Notifies all listeners that a caret change has occurred.
	 *
	 * @param e
	 *            The caret event.
	 */
	@Override
	protected void fireCaretUpdate(final CaretEvent e) {

		// Decide whether we need to repaint the current line background.
		this.possiblyUpdateCurrentLineHighlightLocation();

		// Now, if there is a highlighted region of text, allow them to cut
		// and copy.
		if (e != null && e.getDot() != e.getMark()) {// && !cutAction.isEnabled()) {
			RTextArea.cutAction.setEnabled(true);
			RTextArea.copyAction.setEnabled(true);
		}

		// Otherwise, if there is no highlighted region, don't let them cut
		// or copy. The condition here should speed things up, because this
		// way, we will only enable the actions the first time the selection
		// becomes nothing.
		else if (RTextArea.cutAction.isEnabled()) {
			RTextArea.cutAction.setEnabled(false);
			RTextArea.copyAction.setEnabled(false);
		}

		super.fireCaretUpdate(e);

	}

	/**
	 * Removes the "Ctrl+H <=> Backspace" behavior that Java shows, for some odd
	 * reason...
	 */
	private void fixCtrlH() {
		final InputMap inputMap = this.getInputMap();
		final KeyStroke char010 = KeyStroke.getKeyStroke("typed \010");
		InputMap parent = inputMap;
		while (parent != null) {
			parent.remove(char010);
			parent = parent.getParent();
		}
		if (inputMap != null) { // Just for Sonar
			final KeyStroke backspace = KeyStroke.getKeyStroke("BACK_SPACE");
			inputMap.put(backspace, DefaultEditorKit.deletePrevCharAction);
		}
	}

	/**
	 * Returns the line highlight manager.
	 *
	 * @return The line highlight manager. This may be <code>null</code>.
	 */
	LineHighlightManager getLineHighlightManager() {
		return this.lineHighlightManager;
	}

	/**
	 * Returns the color used in "mark all" highlights.
	 *
	 * @return The color.
	 * @see #setMarkAllHighlightColor(Color)
	 */
	public Color getMarkAllHighlightColor() {
		return (Color) this.markAllHighlightPainter.getPaint();
	}

	/**
	 * Returns whether "mark all" should be enabled when a user does a "find
	 * next/find previous" action via Ctrl+K or Ctrl+Shift+K (the default shortcut
	 * keys for this action). The default value is {@code true}.
	 *
	 * @return Whether "mark all" should be enabled.
	 * @see #setMarkAllOnOccurrenceSearches(boolean)
	 */
	public boolean getMarkAllOnOccurrenceSearches() {
		return this.markAllOnOccurrenceSearches;
	}

	/**
	 * Returns the maximum ascent of all fonts used in this text area. In the case
	 * of a standard <code>RTextArea</code>, this is simply the ascent of the
	 * current font.
	 * <p>
	 *
	 * This value could be useful, for example, to implement a line-numbering
	 * scheme.
	 *
	 * @return The ascent of the current font.
	 */
	public int getMaxAscent() {
		return this.getFontMetrics(this.getFont()).getAscent();
	}

	/**
	 * Returns the popup menu for this component, lazily creating it if necessary.
	 *
	 * @return The popup menu.
	 * @see #createPopupMenu()
	 * @see #setPopupMenu(JPopupMenu)
	 */
	public JPopupMenu getPopupMenu() {
		if (!this.popupMenuCreated) {
			this.popupMenu = this.createPopupMenu();
			if (this.popupMenu != null) {
				final ComponentOrientation orientation = ComponentOrientation.getOrientation(Locale.getDefault());
				this.popupMenu.applyComponentOrientation(orientation);
			}
			this.popupMenuCreated = true;
		}
		return this.popupMenu;
	}

	/**
	 * Returns the text mode this editor pane is currently in.
	 *
	 * @return Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @see #setTextMode(int)
	 */
	public final int getTextMode() {
		return this.textMode;
	}

	/**
	 * Returns the tool tip supplier.
	 *
	 * @return The tool tip supplier, or <code>null</code> if one isn't installed.
	 * @see #setToolTipSupplier(ToolTipSupplier)
	 */
	public ToolTipSupplier getToolTipSupplier() {
		return this.toolTipSupplier;
	}

	/**
	 * Returns the tooltip to display for a mouse event at the given location. This
	 * method is overridden to check for a {@link ToolTipSupplier}; if there is one
	 * installed, it is queried for tool tip text before using the super class's
	 * implementation of this method.
	 *
	 * @param e
	 *            The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 * @see #getToolTipSupplier()
	 * @see #setToolTipSupplier(ToolTipSupplier)
	 */
	@Override
	public String getToolTipText(final MouseEvent e) {
		String tip = null;
		if (this.getToolTipSupplier() != null)
			tip = this.getToolTipSupplier().getToolTipText(this, e);
		return tip != null ? tip : super.getToolTipText();
	}

	/**
	 * Does the actual dirty-work of replacing the selected text in this text area
	 * (i.e., in its document). This method provides a hook for subclasses to handle
	 * this in a different way.
	 *
	 * @param content
	 *            The content to add.
	 */
	protected void handleReplaceSelection(final String content) {
		// Call into super to handle composed text.
		super.replaceSelection(content);
	}

	@Override
	protected void init() {

		super.init();

		// NOTE: Our actions are created here instead of in a static block
		// so they are only created when the first RTextArea is instantiated,
		// not before. There have been reports of users calling static getters
		// (e.g. RSyntaxTextArea.getDefaultBracketMatchBGColor()) which would
		// cause these actions to be created and (possibly) incorrectly
		// localized, if they were in a static block.
		if (RTextArea.cutAction == null)
			RTextArea.createPopupMenuActions();

		// Install the undo manager.
		this.undoManager = this.createUndoManager();
		this.getDocument().addUndoableEditListener(this.undoManager);

		// Set the defaults for various stuff.
		final Color markAllHighlightColor = RTextArea.getDefaultMarkAllHighlightColor();
		this.markAllHighlightPainter = new SmartHighlightPainter(markAllHighlightColor);
		this.setMarkAllHighlightColor(markAllHighlightColor);
		this.carets = new CaretStyle[2];
		this.setCaretStyle(RTextArea.INSERT_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
		this.setCaretStyle(RTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_STYLE);
		this.setDragEnabled(true); // Enable drag-and-drop.

		this.setTextMode(RTextArea.INSERT_MODE); // Carets array must be created first!
		this.setMarkAllOnOccurrenceSearches(true);

		// Fix the odd "Ctrl+H <=> Backspace" Java behavior.
		this.fixCtrlH();

	}

	/**
	 * Marks all ranges specified with the "mark all" highlighter. Typically, this
	 * method is called indirectly from {@link SearchEngine} when doing a fine or
	 * replace operation.
	 * <p>
	 *
	 * This method fires a property change event of type
	 * {@link #MARK_ALL_OCCURRENCES_CHANGED_PROPERTY}.
	 *
	 * @param ranges
	 *            The ranges to mark. This should not be <code>null</code>.
	 * @see SearchEngine
	 * @see SearchContext#setMarkAll(boolean)
	 * @see #clearMarkAllHighlights()
	 * @see #getMarkAllHighlightColor()
	 * @see #setMarkAllHighlightColor(Color)
	 */
	void markAll(final List<DocumentRange> ranges) {

		final RTextAreaHighlighter h = (RTextAreaHighlighter) this.getHighlighter();
		if (/* toMark!=null && !toMark.equals(markedWord) && */h != null) {

			// markedWord = toMark;
			if (ranges != null)
				for (final DocumentRange range : ranges)
					try {
						h.addMarkAllHighlight(range.getStartOffset(), range.getEndOffset(),
								this.markAllHighlightPainter);
					} catch (final BadLocationException ble) {
						ble.printStackTrace();
					}

			this.repaint();
			this.firePropertyChange(RTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, null, ranges);

		}

	}

	@Override
	public void paste() {
		// Treat paste operations as atomic, otherwise the removal and
		// insertion are treated as two separate undo-able operations.
		this.beginAtomicEdit();
		try {
			super.paste();
		} finally {
			this.endAtomicEdit();
		}
	}

	/**
	 * "Plays back" the last recorded macro in this text area.
	 */
	public synchronized void playbackLastMacro() {
		if (RTextArea.currentMacro != null) {
			final List<MacroRecord> macroRecords = RTextArea.currentMacro.getMacroRecords();
			if (!macroRecords.isEmpty()) {
				final Action[] actions = this.getActions();
				this.undoManager.beginInternalAtomicEdit();
				try {
					for (final MacroRecord record : macroRecords)
						for (final Action action : actions)
							if (action instanceof RecordableTextAction
									&& record.id.equals(((RecordableTextAction) action).getMacroID())) {
								action.actionPerformed(
										new ActionEvent(this, ActionEvent.ACTION_PERFORMED, record.actionCommand));
								break;
							}
				} finally {
					this.undoManager.endInternalAtomicEdit();
				}
			}
		}
	}

	/**
	 * Method called when it's time to print this badboy (the old-school, AWT way).
	 *
	 * @param g
	 *            The context into which the page is drawn.
	 * @param pageFormat
	 *            The size and orientation of the page being drawn.
	 * @param pageIndex
	 *            The zero based index of the page to be drawn.
	 */
	@Override
	public int print(final Graphics g, final PageFormat pageFormat, final int pageIndex) {
		return RPrintUtilities.printDocumentWordWrap(g, this, this.getFont(), pageIndex, pageFormat, this.getTabSize());
	}

	/**
	 * We override this method because the super version gives us an entirely new
	 * <code>Document</code>, thus requiring us to re-attach our Undo manager. With
	 * this version we just replace the text.
	 */
	@Override
	public void read(final Reader in, final Object desc) throws IOException {

		final RTextAreaEditorKit kit = (RTextAreaEditorKit) this.getUI().getEditorKit(this);
		this.setText(null);
		final Document doc = this.getDocument();
		if (desc != null)
			doc.putProperty(Document.StreamDescriptionProperty, desc);
		try {
			// NOTE: Resets the "line separator" property.
			kit.read(in, doc, 0);
		} catch (final BadLocationException e) {
			throw new IOException(e.getMessage());
		}

	}

	/**
	 * De-serializes a text area.
	 *
	 * @param s
	 *            The stream to read from.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(final ObjectInputStream s) throws ClassNotFoundException, IOException {

		s.defaultReadObject();

		// UndoManagers cannot be serialized without Exceptions. See
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4275892
		this.undoManager = this.createUndoManager();
		this.getDocument().addUndoableEditListener(this.undoManager);

		this.lineHighlightManager = null; // Keep FindBugs happy.

	}

	/**
	 * Attempt to redo the last action.
	 *
	 * @see #undoLastAction()
	 */
	public void redoLastAction() {
		// NOTE: The try/catch block shouldn't be necessary...
		try {
			if (this.undoManager.canRedo())
				this.undoManager.redo();
		} catch (final CannotRedoException cre) {
			cre.printStackTrace();
		}
	}

	/**
	 * Removes all line highlights.
	 *
	 * @see #removeLineHighlight(Object)
	 */
	public void removeAllLineHighlights() {
		if (this.lineHighlightManager != null)
			this.lineHighlightManager.removeAllLineHighlights();
	}

	/**
	 * Removes a line highlight.
	 *
	 * @param tag
	 *            The tag of the line highlight to remove.
	 * @see #removeAllLineHighlights()
	 * @see #addLineHighlight(int, Color)
	 */
	public void removeLineHighlight(final Object tag) {
		if (this.lineHighlightManager != null)
			this.lineHighlightManager.removeLineHighlight(tag);
	}

	/**
	 * Replaces text from the indicated start to end position with the new text
	 * specified. Does nothing if the model is null. Simply does a delete if the new
	 * string is null or empty.
	 * <p>
	 * This method is thread safe, although most Swing methods are not.
	 * <p>
	 * This method is overridden so that our Undo manager remembers it as a single
	 * operation (it has trouble with this, especially for
	 * <code>RSyntaxTextArea</code> and the "auto-indent" feature).
	 *
	 * @param str
	 *            the text to use as the replacement
	 * @param start
	 *            the start position &gt;= 0
	 * @param end
	 *            the end position &gt;= start
	 * @exception IllegalArgumentException
	 *                if part of the range is an invalid position in the model
	 * @see #insert(String, int)
	 * @see #replaceRange(String, int, int)
	 */
	@Override
	public void replaceRange(final String str, final int start, final int end) {
		if (end < start)
			throw new IllegalArgumentException("end before start");
		final Document doc = this.getDocument();
		if (doc != null)
			try {
				// Without this, in some cases we'll have to do two undos
				// for one logical operation (for example, try editing a
				// Java source file in an RSyntaxTextArea, and moving a line
				// with text already on it down via Enter. Without this
				// line, doing a single "undo" moves all later text up,
				// but the first line moved down isn't there! Doing a
				// second undo puts it back.
				this.undoManager.beginInternalAtomicEdit();
				((AbstractDocument) doc).replace(start, end - start, str, null);
			} catch (final BadLocationException e) {
				throw new IllegalArgumentException(e.getMessage());
			} finally {
				this.undoManager.endInternalAtomicEdit();
			}
	}

	/**
	 * This method overrides <code>JTextComponent</code>'s
	 * <code>replaceSelection</code>, so that if <code>textMode</code> is
	 * {@link #OVERWRITE_MODE}, it actually overwrites.
	 *
	 * @param text
	 *            The content to replace the selection with.
	 */
	@Override
	public void replaceSelection(String text) {

		// It's legal for null to be used here...
		if (text == null) {
			this.handleReplaceSelection(text);
			return;
		}

		if (this.getTabsEmulated()) {
			final int firstTab = text.indexOf('\t');
			if (firstTab > -1) {
				final int docOffs = this.getSelectionStart();
				try {
					text = this.replaceTabsWithSpaces(text, docOffs, firstTab);
				} catch (final BadLocationException ble) { // Never happens
					ble.printStackTrace();
				}
			}
		}

		// If the user wants to overwrite text...
		if (this.textMode == RTextArea.OVERWRITE_MODE && !"\n".equals(text)) {

			final Caret caret = this.getCaret();
			int caretPos = caret.getDot();
			final Document doc = this.getDocument();
			final Element map = doc.getDefaultRootElement();
			final int curLine = map.getElementIndex(caretPos);
			final int lastLine = map.getElementCount() - 1;

			try {

				// If we're not at the end of a line, select the characters
				// that will be overwritten (otherwise JTextArea will simply
				// insert in front of them).
				final int curLineEnd = this.getLineEndOffset(curLine);
				if (caretPos == caret.getMark() && caretPos != curLineEnd) {
					if (curLine == lastLine)
						caretPos = Math.min(caretPos + text.length(), curLineEnd);
					else
						caretPos = Math.min(caretPos + text.length(), curLineEnd - 1);
					caret.moveDot(caretPos);// moveCaretPosition(caretPos);
				}

			} catch (final BadLocationException ble) { // Never happens
				UIManager.getLookAndFeel().provideErrorFeedback(this);
				ble.printStackTrace();
			}

		} // End of if (textMode==OVERWRITE_MODE).

		// Now, actually do the inserting/replacing. Our undoManager will
		// take care of remembering the remove/insert as atomic if we are in
		// overwrite mode.
		this.handleReplaceSelection(text);

	}

	/**
	 * Replaces all instances of the tab character in <code>text</code> with the
	 * number of spaces equivalent to a tab in this text area.
	 * <p>
	 *
	 * This method should only be called from thread-safe methods, such as
	 * {@link #replaceSelection(String)}.
	 *
	 * @param text
	 *            The <code>java.lang.String</code> in which to replace tabs with
	 *            spaces. This has already been verified to have at least one tab
	 *            character in it.
	 * @param docOffs
	 *            The offset in the document at which the text is being inserted.
	 * @param firstTab
	 *            The offset into <code>text</code> of the first tab. Assumed to be
	 *            &gt;= 0.
	 * @return A <code>String</code> just like <code>text</code>, but with spaces
	 *         instead of tabs.
	 */
	private String replaceTabsWithSpaces(final String text, final int docOffs, final int firstTab)
			throws BadLocationException {

		final int tabSize = this.getTabSize();

		// Get how many chars into the current line we are
		final Document doc = this.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int lineIndex = root.getElementIndex(docOffs);
		final Element line = root.getElement(lineIndex);
		final int lineStart = line.getStartOffset();
		int charCount = docOffs - lineStart;

		// Figure out how many chars into the "current tab" we are
		if (charCount > 0) {
			doc.getText(lineStart, charCount, RTextArea.repTabsSeg);
			charCount = 0;
			for (int i = 0; i < RTextArea.repTabsSeg.count; i++) {
				final char ch = RTextArea.repTabsSeg.array[RTextArea.repTabsSeg.offset + i];
				if (ch == '\t')
					charCount = 0;
				else
					charCount = (charCount + 1) % tabSize;
			}
		}

		// Common case: The user's entering a single tab (pressed the tab key).
		if (text.length() == 1)
			return this.createSpacer(tabSize - charCount);

		// Otherwise, there may be more than one tab.

		if (RTextArea.repTabsSB == null)
			RTextArea.repTabsSB = new StringBuilder();
		RTextArea.repTabsSB.setLength(0);
		final char[] array = text.toCharArray();
		int lastPos = 0;
		int offsInLine = charCount; // Accurate enough for our start
		for (int pos = firstTab; pos < array.length; pos++) {
			final char ch = array[pos];
			switch (ch) {
			case '\t':
				if (pos > lastPos)
					RTextArea.repTabsSB.append(array, lastPos, pos - lastPos);
				final int thisTabSize = tabSize - offsInLine % tabSize;
				RTextArea.repTabsSB.append(this.createSpacer(thisTabSize));
				lastPos = pos + 1;
				offsInLine = 0;
				break;
			case '\n':
				offsInLine = 0;
				break;
			default:
				offsInLine++;
				break;
			}
		}
		if (lastPos < array.length)
			RTextArea.repTabsSB.append(array, lastPos, array.length - lastPos);

		return RTextArea.repTabsSB.toString();

	}

	/**
	 * Sets the caret to use in this text area. It is strongly encouraged to use
	 * {@link ConfigurableCaret}s (which is used by default), or a subclass, since
	 * they know how to render themselves differently when the user toggles between
	 * insert and overwrite modes.
	 *
	 * @param caret
	 *            The caret to use.
	 * @see #setCaretStyle(int, CaretStyle)
	 */
	@Override
	public void setCaret(final Caret caret) {
		super.setCaret(caret);
		if (this.carets != null && // Called by setUI() before carets is initialized
				caret instanceof ConfigurableCaret)
			((ConfigurableCaret) caret).setStyle(this.carets[this.getTextMode()]);
	}

	/**
	 * Sets the style of caret used when in insert or overwrite mode.
	 *
	 * @param mode
	 *            Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @param style
	 *            The style for the caret.
	 * @see ConfigurableCaret
	 */
	public void setCaretStyle(final int mode, CaretStyle style) {
		if (style == null)
			style = CaretStyle.THICK_VERTICAL_LINE_STYLE;
		this.carets[mode] = style;
		if (mode == this.getTextMode() && this.getCaret() instanceof ConfigurableCaret)
			// Will repaint the caret if necessary.
			((ConfigurableCaret) this.getCaret()).setStyle(style);
	}

	/**
	 * Sets the document used by this text area.
	 *
	 * @param document
	 *            The new document to use.
	 * @throws IllegalArgumentException
	 *             If the document is not an instance of {@link RDocument}.
	 */
	@Override
	public void setDocument(final Document document) {
		if (!(document instanceof RDocument))
			throw new IllegalArgumentException("RTextArea requires " + "instances of RDocument for its document");
		if (this.undoManager != null) { // First time through, undoManager==null
			final Document old = this.getDocument();
			if (old != null)
				old.removeUndoableEditListener(this.undoManager);
		}
		super.setDocument(document);
		if (this.undoManager != null) {
			document.addUndoableEditListener(this.undoManager);
			this.discardAllEdits();
		}
	}

	/**
	 * Sets the color used for "mark all." This fires a property change of type
	 * {@link #MARK_ALL_COLOR_PROPERTY}.
	 *
	 * @param color
	 *            The color to use for "mark all."
	 * @see #getMarkAllHighlightColor()
	 */
	public void setMarkAllHighlightColor(final Color color) {
		final Color old = (Color) this.markAllHighlightPainter.getPaint();
		if (old != null && !old.equals(color)) {
			this.markAllHighlightPainter.setPaint(color);
			final RTextAreaHighlighter h = (RTextAreaHighlighter) this.getHighlighter();
			if (h.getMarkAllHighlightCount() > 0)
				this.repaint(); // Repaint if words are highlighted.
			this.firePropertyChange(RTextArea.MARK_ALL_COLOR_PROPERTY, old, color);
		}
	}

	/**
	 * Sets whether "mark all" should be enabled when a user does a "find next/find
	 * previous" action via Ctrl+K or Ctrl+Shift+K (the default shortcut keys for
	 * this action). The default value is {@code true}.
	 * <p>
	 * This method fires a property change event of type
	 * {@link #MARK_ALL_ON_OCCURRENCE_SEARCHES_PROPERTY}.
	 *
	 * @param markAll
	 *            Whether "mark all" should be enabled.
	 * @see #getMarkAllOnOccurrenceSearches()
	 */
	public void setMarkAllOnOccurrenceSearches(final boolean markAll) {
		if (markAll != this.markAllOnOccurrenceSearches) {
			this.markAllOnOccurrenceSearches = markAll;
			this.firePropertyChange(RTextArea.MARK_ALL_ON_OCCURRENCE_SEARCHES_PROPERTY, !markAll, markAll);
		}
	}

	/**
	 * Sets the popup menu used by this text area.
	 * <p>
	 *
	 * If you set the popup menu with this method, you'll want to consider also
	 * overriding {@link #configurePopupMenu(JPopupMenu)}, especially if you removed
	 * any of the default menu items.
	 *
	 * @param popupMenu
	 *            The popup menu. If this is <code>null</code>, no popup menu will
	 *            be displayed.
	 * @see #getPopupMenu()
	 * @see #configurePopupMenu(JPopupMenu)
	 */
	public void setPopupMenu(final JPopupMenu popupMenu) {
		this.popupMenu = popupMenu;
		this.popupMenuCreated = true;
	}

	@Override
	public void setRoundedSelectionEdges(final boolean rounded) {
		if (this.getRoundedSelectionEdges() != rounded) {
			this.markAllHighlightPainter.setRoundedEdges(rounded);
			super.setRoundedSelectionEdges(rounded); // Fires event.
		}
	}

	/**
	 * Sets the text mode for this editor pane. If the currently installed caret is
	 * an instance of {@link ConfigurableCaret}, it will be automatically updated to
	 * render itself appropriately for the new text mode.
	 *
	 * @param mode
	 *            Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @see #getTextMode()
	 */
	public void setTextMode(int mode) {

		if (mode != RTextArea.INSERT_MODE && mode != RTextArea.OVERWRITE_MODE)
			mode = RTextArea.INSERT_MODE;

		if (this.textMode != mode) {
			final Caret caret = this.getCaret();
			if (caret instanceof ConfigurableCaret)
				((ConfigurableCaret) caret).setStyle(this.carets[mode]);
			this.textMode = mode;
			// Prevent the caret from blinking while e.g. holding down the
			// Insert key to toggle insert/overwrite modes
			caret.setVisible(false);
			caret.setVisible(true);
		}

	}

	/**
	 * Sets the tool tip supplier.
	 *
	 * @param supplier
	 *            The new tool tip supplier, or <code>null</code> if there is to be
	 *            no supplier.
	 * @see #getToolTipSupplier()
	 */
	public void setToolTipSupplier(final ToolTipSupplier supplier) {
		this.toolTipSupplier = supplier;
	}

	/**
	 * Sets the UI used by this text area. This is overridden so only the
	 * right-click popup menu's UI is updated. The look and feel of an
	 * <code>RTextArea</code> is independent of the Java Look and Feel, and so this
	 * method does not change the text area itself. Subclasses (such as
	 * <code>RSyntaxTextArea</code> can call <code>setRTextAreaUI</code> if they
	 * wish to install a new UI.
	 *
	 * @param ui
	 *            This parameter is ignored.
	 */
	@Override
	public final void setUI(final TextUI ui) {

		// Update the popup menu's ui.
		if (this.popupMenu != null)
			SwingUtilities.updateComponentTreeUI(this.popupMenu);

		// Set things like selection color, selected text color, etc. to
		// laf defaults (if values are null or UIResource instances).
		final RTextAreaUI rtaui = (RTextAreaUI) this.getUI();
		if (rtaui != null)
			rtaui.installDefaults();

	}

	/**
	 * Attempt to undo an "action" done in this text area.
	 *
	 * @see #redoLastAction()
	 */
	public void undoLastAction() {
		// NOTE: that the try/catch block shouldn't be necessary...
		try {
			if (this.undoManager.canUndo())
				this.undoManager.undo();
		} catch (final CannotUndoException cre) {
			cre.printStackTrace();
		}
	}

	/**
	 * Serializes this text area.
	 *
	 * @param s
	 *            The stream to write to.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private void writeObject(final ObjectOutputStream s) throws IOException {

		// UndoManagers cannot be serialized without Exceptions. See
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4275892
		this.getDocument().removeUndoableEditListener(this.undoManager);
		s.defaultWriteObject();
		this.getDocument().addUndoableEditListener(this.undoManager);

	}

}