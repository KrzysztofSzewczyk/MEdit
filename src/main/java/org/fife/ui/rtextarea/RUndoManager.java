/*
 * 12/06/2008
 *
 * RUndoManager.java - Handles undo/redo behavior for RTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * This class manages undos/redos for a particular editor pane. It groups all
 * undos that occur one character position apart together, to avoid Java's
 * horrible "one character at a time" undo behavior. It also recognizes
 * "replace" actions (i.e., text is selected, then the user types), and treats
 * it as a single action, instead of a remove/insert action pair.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RUndoManager extends UndoManager {

	/**
	 * The edit used by {@link RUndoManager}.
	 */
	class RCompoundEdit extends CompoundEdit {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public String getRedoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.redoText");
		}

		@Override
		public String getUndoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.undoText");
		}

		@Override
		public boolean isInProgress() {
			return false;
		}

		@Override
		public void undo() {
			if (RUndoManager.this.compoundEdit != null)
				RUndoManager.this.compoundEdit.end();
			super.undo();
			RUndoManager.this.compoundEdit = null;
		}

	}

	private static final String MSG = "org.fife.ui.rtextarea.RTextArea";
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final String cantRedoText;
	private final String cantUndoText;
	private RCompoundEdit compoundEdit;

	private int internalAtomicEditDepth;

	private int lastOffset;

	private final RTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The parent text area.
	 */
	public RUndoManager(final RTextArea textArea) {
		this.textArea = textArea;
		final ResourceBundle msg = ResourceBundle.getBundle(RUndoManager.MSG);
		this.cantUndoText = msg.getString("Action.CantUndo.Name");
		this.cantRedoText = msg.getString("Action.CantRedo.Name");
	}

	/**
	 * Begins an "atomic" edit. This method is called when RTextArea KNOWS that some
	 * edits should be compound automatically, such as when the user is typing in
	 * overwrite mode (the deletion of the current char + insertion of the new one)
	 * or the playing back of a macro.
	 *
	 * @see #endInternalAtomicEdit()
	 */
	public void beginInternalAtomicEdit() {
		if (++this.internalAtomicEditDepth == 1) {
			if (this.compoundEdit != null)
				this.compoundEdit.end();
			this.compoundEdit = new RCompoundEdit();
		}
	}

	/**
	 * Ends an "atomic" edit.
	 *
	 * @see #beginInternalAtomicEdit()
	 */
	public void endInternalAtomicEdit() {
		if (this.internalAtomicEditDepth > 0 && --this.internalAtomicEditDepth == 0) {
			this.addEdit(this.compoundEdit);
			this.compoundEdit.end();
			this.compoundEdit = null;
			this.updateActions(); // Needed to show the new display name.
		}
	}

	/**
	 * Returns the localized "Can't Redo" string.
	 *
	 * @return The localized "Can't Redo" string.
	 * @see #getCantUndoText()
	 */
	public String getCantRedoText() {
		return this.cantRedoText;
	}

	/**
	 * Returns the localized "Can't Undo" string.
	 *
	 * @return The localized "Can't Undo" string.
	 * @see #getCantRedoText()
	 */
	public String getCantUndoText() {
		return this.cantUndoText;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void redo() {
		super.redo();
		this.updateActions();
	}

	private RCompoundEdit startCompoundEdit(final UndoableEdit edit) {
		this.lastOffset = this.textArea.getCaretPosition();
		this.compoundEdit = new RCompoundEdit();
		this.compoundEdit.addEdit(edit);
		this.addEdit(this.compoundEdit);
		return this.compoundEdit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void undo() {
		super.undo();
		this.updateActions();
	}

	@Override
	public void undoableEditHappened(final UndoableEditEvent e) {

		// This happens when the first undoable edit occurs, and
		// just after an undo. So, we need to update our actions.
		if (this.compoundEdit == null) {
			this.compoundEdit = this.startCompoundEdit(e.getEdit());
			this.updateActions();
			return;
		}

		else if (this.internalAtomicEditDepth > 0) {
			this.compoundEdit.addEdit(e.getEdit());
			return;
		}

		// This happens when there's already an undo that has occurred.
		// Test to see if these undos are on back-to-back characters,
		// and if they are, group them as a single edit. Since an
		// undo has already occurred, there is no need to update our
		// actions here.
		final int diff = this.textArea.getCaretPosition() - this.lastOffset;
		// "<=1" allows contiguous "overwrite mode" key presses to be
		// grouped together.
		if (Math.abs(diff) <= 1) {// ==1) {
			this.compoundEdit.addEdit(e.getEdit());
			this.lastOffset += diff;
			// updateActions();
			return;
		}

		// This happens when this UndoableEdit didn't occur at the
		// character just after the previous undlabeledit. Since an
		// undo has already occurred, there is no need to update our
		// actions here either.
		this.compoundEdit.end();
		this.compoundEdit = this.startCompoundEdit(e.getEdit());
		// updateActions();

	}

	/**
	 * Ensures that undo/redo actions are enabled appropriately and have descriptive
	 * text at all times.
	 */
	public void updateActions() {

		String text;

		Action a = RTextArea.getAction(RTextArea.UNDO_ACTION);
		if (this.canUndo()) {
			a.setEnabled(true);
			text = this.getUndoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		} else if (a.isEnabled()) {
			a.setEnabled(false);
			text = this.cantUndoText;
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}

		a = RTextArea.getAction(RTextArea.REDO_ACTION);
		if (this.canRedo()) {
			a.setEnabled(true);
			text = this.getRedoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		} else if (a.isEnabled()) {
			a.setEnabled(false);
			text = this.cantRedoText;
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}

	}

}