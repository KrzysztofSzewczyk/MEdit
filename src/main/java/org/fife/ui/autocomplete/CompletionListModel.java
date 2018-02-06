/*
 * 12/22/2008
 *
 * CompletionListModel.java - A model that allows bulk addition of elements.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * A list model implementation that allows the bulk addition of elements. This
 * is the only feature missing from <code>DefaultListModel</code> that we need.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class CompletionListModel extends AbstractListModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Container for items in this model.
	 */
	private final List<Completion> delegate;

	/**
	 * Constructor.
	 */
	public CompletionListModel() {
		this.delegate = new ArrayList<>();
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after this
	 * call returns (unless it throws an exception).
	 *
	 * @see #setContents(Collection)
	 */
	public void clear() {
		final int end = this.delegate.size() - 1;
		this.delegate.clear();
		if (end >= 0)
			this.fireIntervalRemoved(this, 0, end);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getElementAt(final int index) {
		return this.delegate.get(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() {
		return this.delegate.size();
	}

	/**
	 * Sets the contents of this model. All previous contents are removed.
	 *
	 * @param contents
	 *            The new contents of this model.
	 */
	public void setContents(final Collection<Completion> contents) {
		this.clear();
		final int count = contents.size();
		if (count > 0) {
			this.delegate.addAll(contents);
			this.fireIntervalAdded(this, 0, count - 1); // endpoints included (!)
		}
	}

}