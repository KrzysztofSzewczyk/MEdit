/*
 * 08/29/2014
 *
 * ClipboardHistory.java - A history of text added to the clipboard.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listens for cuts and copies from instances of {@link RTextArea}. This is used
 * for the "clipboard history" shortcut (Ctrl+Shift+V by default).
 * <p>
 *
 * Note that this class does not listen for all events on the system clipboard,
 * because that functionality is pretty fragile. See <a href=
 * "http://stackoverflow.com/questions/5484927/listen-to-clipboard-changes-check-ownership">
 * http://stackoverflow.com/questions/5484927/listen-to-clipboard-changes-check-ownership</a>
 * for more information.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public final class ClipboardHistory {

	private static final int DEFAULT_MAX_SIZE = 12;

	private static ClipboardHistory instance;

	/**
	 * Returns the singleton instance of this class, lazily creating it if
	 * necessary.
	 * <p>
	 *
	 * This method should only be called on the EDT.
	 *
	 * @return The singleton instance of this class.
	 */
	public static ClipboardHistory get() {
		if (ClipboardHistory.instance == null)
			ClipboardHistory.instance = new ClipboardHistory();
		return ClipboardHistory.instance;
	}

	private final List<String> history;

	private int maxSize;

	private ClipboardHistory() {
		this.history = new ArrayList<>();
		this.maxSize = ClipboardHistory.DEFAULT_MAX_SIZE;
	}

	/**
	 * Adds an entry to the clipboard history.
	 *
	 * @param str
	 *            The text to add.
	 * @see #getHistory()
	 */
	public void add(final String str) {
		final int size = this.history.size();
		if (size == 0)
			this.history.add(str);
		else {
			final int index = this.history.indexOf(str);
			if (index != size - 1) {
				if (index > -1)
					this.history.remove(index);
				this.history.add(str);
			}
			this.trim();
		}
	}

	/**
	 * Returns the clipboard history, in most-recently-used order.
	 *
	 * @return The clipboard history.
	 */
	public List<String> getHistory() {
		final List<String> copy = new ArrayList<>(this.history);
		Collections.reverse(copy);
		return copy;
	}

	/**
	 * Returns the maximum number of clipboard values remembered.
	 *
	 * @return The maximum number of clipboard values remembered.
	 * @see #setMaxSize(int)
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	/**
	 * Sets the maximum number of clipboard values remembered.
	 *
	 * @param maxSize
	 *            The maximum number of clipboard values to remember.
	 * @throws IllegalArgumentException
	 *             If <code>maxSize</code> is not greater than zero.
	 * @see #getMaxSize()
	 */
	public void setMaxSize(final int maxSize) {
		if (maxSize <= 0)
			throw new IllegalArgumentException("Maximum size must be >= 0");
		this.maxSize = maxSize;
		this.trim();
	}

	/**
	 * Ensures the remembered set of strings is not larger than the maximum allowed
	 * size.
	 */
	private void trim() {
		while (this.history.size() > this.maxSize)
			this.history.remove(0);
	}

}