/*
 * 01/06/2009
 *
 * MarkOccurrencesSupport.java - Handles marking all occurrences of the
 * currently selected identifier in a text area.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Caret;

import org.fife.ui.rtextarea.SmartHighlightPainter;

/**
 * Marks all occurrences of the token at the current caret position, if it is an
 * identifier.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see OccurrenceMarker
 */
class MarkOccurrencesSupport implements CaretListener, ActionListener {

	/**
	 * The default color used to mark occurrences.
	 */
	static final Color DEFAULT_COLOR = new Color(224, 224, 224);
	/**
	 * The default delay.
	 */
	static final int DEFAULT_DELAY_MS = 1000;
	private final SmartHighlightPainter p;

	private RSyntaxTextArea textArea;

	private final Timer timer;

	/**
	 * Constructor. Creates a listener with a 1 second delay.
	 */
	MarkOccurrencesSupport() {
		this(MarkOccurrencesSupport.DEFAULT_DELAY_MS);
	}

	/**
	 * Constructor.
	 *
	 * @param delay
	 *            The delay between when the caret last moves and when the text
	 *            should be scanned for matching occurrences. This should be in
	 *            milliseconds.
	 */
	MarkOccurrencesSupport(final int delay) {
		this(delay, MarkOccurrencesSupport.DEFAULT_COLOR);
	}

	/**
	 * Constructor.
	 *
	 * @param delay
	 *            The delay between when the caret last moves and when the text
	 *            should be scanned for matching occurrences. This should be in
	 *            milliseconds.
	 * @param color
	 *            The color to use to mark the occurrences. This cannot be
	 *            <code>null</code>.
	 */
	MarkOccurrencesSupport(final int delay, final Color color) {
		this.timer = new Timer(delay, this);
		this.timer.setRepeats(false);
		this.p = new SmartHighlightPainter();
		this.setColor(color);
	}

	/**
	 * Called after the caret has been moved and a fixed time delay has elapsed.
	 * This locates and highlights all occurrences of the identifier at the caret
	 * position, if any.
	 * <p>
	 *
	 * Callers should not call this method directly, but should rather prefer
	 * {@link #doMarkOccurrences()} to mark occurrences.
	 *
	 * @param e
	 *            The event.
	 * @see #doMarkOccurrences()
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		// Don't do anything if they are selecting text.
		final Caret c = this.textArea.getCaret();
		if (c.getDot() != c.getMark())
			return;

		final RSyntaxDocument doc = (RSyntaxDocument) this.textArea.getDocument();
		final OccurrenceMarker occurrenceMarker = doc.getOccurrenceMarker();
		boolean occurrencesChanged = false;

		if (occurrenceMarker != null) {

			doc.readLock();
			try {

				final Token t = occurrenceMarker.getTokenToMark(this.textArea);

				if (t != null && occurrenceMarker.isValidType(this.textArea, t) && !RSyntaxUtilities.isNonWordChar(t)) {
					this.clear();
					final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
					occurrenceMarker.markOccurrences(doc, t, h, this.p);
					// textArea.repaint();
					// TODO: Do a textArea.repaint() instead of repainting each
					// marker as it's added if count is huge
					occurrencesChanged = true;
				}

			} finally {
				doc.readUnlock();
				// time = System.currentTimeMillis() - time;
				// System.out.println("MarkOccurrencesSupport took: " + time + " ms");
			}

		}

		if (occurrencesChanged)
			this.textArea.fireMarkedOccurrencesChanged();

	}

	/**
	 * Called when the caret moves in the text area.
	 *
	 * @param e
	 *            The event.
	 */
	@Override
	public void caretUpdate(final CaretEvent e) {
		this.timer.restart();
	}

	/**
	 * Removes all highlights added to the text area by this listener.
	 */
	void clear() {
		if (this.textArea != null) {
			final RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter) this.textArea.getHighlighter();
			h.clearMarkOccurrencesHighlights();
		}
	}

	/**
	 * Immediately marks all occurrences of the token at the current caret position.
	 */
	public void doMarkOccurrences() {
		this.timer.stop();
		this.actionPerformed(null);
	}

	/**
	 * Returns the color being used to mark occurrences.
	 *
	 * @return The color being used.
	 * @see #setColor(Color)
	 */
	public Color getColor() {
		return (Color) this.p.getPaint();
	}

	/**
	 * Returns the delay, in milliseconds.
	 *
	 * @return The delay.
	 * @see #setDelay(int)
	 */
	public int getDelay() {
		return this.timer.getDelay();
	}

	/**
	 * Returns whether a border is painted around marked occurrences.
	 *
	 * @return Whether a border is painted.
	 * @see #setPaintBorder(boolean)
	 * @see #getColor()
	 */
	public boolean getPaintBorder() {
		return this.p.getPaintBorder();
	}

	/**
	 * Installs this listener on a text area. If it is already installed on another
	 * text area, it is uninstalled first.
	 *
	 * @param textArea
	 *            The text area to install on.
	 */
	public void install(final RSyntaxTextArea textArea) {
		if (this.textArea != null)
			this.uninstall();
		this.textArea = textArea;
		textArea.addCaretListener(this);
		if (textArea.getMarkOccurrencesColor() != null)
			this.setColor(textArea.getMarkOccurrencesColor());
	}

	/**
	 * Sets the color to use when marking occurrences.
	 *
	 * @param color
	 *            The color to use.
	 * @see #getColor()
	 * @see #setPaintBorder(boolean)
	 */
	public void setColor(final Color color) {
		this.p.setPaint(color);
		if (this.textArea != null) {
			this.clear();
			this.caretUpdate(null); // Force a highlight repaint.
		}
	}

	/**
	 * Sets the delay between the last caret position change and when the text is
	 * scanned for matching identifiers. A delay is needed to prevent repeated
	 * scanning while the user is typing.
	 *
	 * @param delay
	 *            The new delay.
	 * @see #getDelay()
	 */
	public void setDelay(final int delay) {
		this.timer.setInitialDelay(delay);
	}

	/**
	 * Toggles whether a border is painted around marked highlights.
	 *
	 * @param paint
	 *            Whether to paint a border.
	 * @see #getPaintBorder()
	 * @see #setColor(Color)
	 */
	public void setPaintBorder(final boolean paint) {
		if (paint != this.p.getPaintBorder()) {
			this.p.setPaintBorder(paint);
			if (this.textArea != null)
				this.textArea.repaint();
		}
	}

	/**
	 * Uninstalls this listener from the current text area. Does nothing if it not
	 * currently installed on any text area.
	 *
	 * @see #install(RSyntaxTextArea)
	 */
	public void uninstall() {
		if (this.textArea != null) {
			this.clear();
			this.textArea.removeCaretListener(this);
		}
	}

}