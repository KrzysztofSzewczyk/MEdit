/*
 * 12/21/2008
 *
 * AbstractCompletionProvider.java - Base class for completion providers.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.text.JTextComponent;

/**
 * A base class for completion providers. {@link Completion}s are kept in a
 * sorted list. To get the list of completions that match a given input, a
 * binary search is done to find the first matching completion, then all
 * succeeding completions that also match are also returned.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractCompletionProvider extends CompletionProviderBase {

	/**
	 * A comparator that compares the input text of a {@link Completion} against a
	 * String lexicographically, ignoring case.
	 */
	@SuppressWarnings("rawtypes")
	public static class CaseInsensitiveComparator implements Comparator, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 8184690981211418885L;

		@Override
		public int compare(final Object o1, final Object o2) {
			final String s1 = o1 instanceof String ? (String) o1 : ((Completion) o1).getInputText();
			final String s2 = o2 instanceof String ? (String) o2 : ((Completion) o2).getInputText();
			return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
		}

	}

	/**
	 * Compares a {@link Completion} against a String.
	 */
	protected CaseInsensitiveComparator comparator;

	/**
	 * The completions this provider is aware of. Subclasses should ensure that this
	 * list is sorted alphabetically (case-insensitively).
	 */
	protected List<Completion> completions;

	/**
	 * Constructor.
	 */
	public AbstractCompletionProvider() {
		this.comparator = new CaseInsensitiveComparator();
		this.clearParameterizedCompletionParams();
		this.completions = new ArrayList<>();
	}

	/**
	 * Adds a single completion to this provider. If you are adding multiple
	 * completions to this provider, for efficiency reasons please consider using
	 * {@link #addCompletions(List)} instead.
	 *
	 * @param c
	 *            The completion to add.
	 * @throws IllegalArgumentException
	 *             If the completion's provider isn't this
	 *             <tt>CompletionProvider</tt>.
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletion(final Completion c) {
		this.checkProviderAndAdd(c);
		Collections.sort(this.completions);
	}

	/**
	 * Adds {@link Completion}s to this provider.
	 *
	 * @param completions
	 *            The completions to add. This cannot be <code>null</code>.
	 * @throws IllegalArgumentException
	 *             If a completion's provider isn't this
	 *             <tt>CompletionProvider</tt>.
	 * @see #addCompletion(Completion)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletions(final List<Completion> completions) {
		// this.completions.addAll(completions);
		for (final Completion c : completions)
			this.checkProviderAndAdd(c);
		Collections.sort(this.completions);
	}

	/**
	 * Adds simple completions for a list of words.
	 *
	 * @param words
	 *            The words.
	 * @see BasicCompletion
	 */
	protected void addWordCompletions(final String[] words) {
		final int count = words == null ? 0 : words.length;
		for (int i = 0; i < count; i++)
			this.completions.add(new BasicCompletion(this, words[i]));
		Collections.sort(this.completions);
	}

	protected void checkProviderAndAdd(final Completion c) {
		if (c.getProvider() != this)
			throw new IllegalArgumentException("Invalid CompletionProvider");
		this.completions.add(c);
	}

	/**
	 * Removes all completions from this provider. This does not affect the parent
	 * <tt>CompletionProvider</tt>, if there is one.
	 *
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 */
	public void clear() {
		this.completions.clear();
	}

	/**
	 * Returns a list of <tt>Completion</tt>s in this provider with the specified
	 * input text.
	 *
	 * @param inputText
	 *            The input text to search for.
	 * @return A list of {@link Completion}s, or <code>null</code> if there are no
	 *         matching <tt>Completion</tt>s.
	 */
	@SuppressWarnings("unchecked")
	public List<Completion> getCompletionByInputText(final String inputText) {

		// Find any entry that matches this input text (there may be > 1).
		int end = Collections.binarySearch(this.completions, inputText, this.comparator);
		if (end < 0)
			return null;

		// There might be multiple entries with the same input text.
		int start = end;
		while (start > 0 && this.comparator.compare(this.completions.get(start - 1), inputText) == 0)
			start--;
		final int count = this.completions.size();
		while (++end < count && this.comparator.compare(this.completions.get(end), inputText) == 0)
			;

		return this.completions.subList(start, end); // (inclusive, exclusive)

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<Completion> getCompletionsImpl(final JTextComponent comp) {

		final List<Completion> retVal = new ArrayList<>();
		final String text = this.getAlreadyEnteredText(comp);

		if (text != null) {

			int index = Collections.binarySearch(this.completions, text, this.comparator);
			if (index < 0)
				index = -index - 1;
			else {
				// If there are several overloads for the function being
				// completed, Collections.binarySearch() will return the index
				// of one of those overloads, but we must return all of them,
				// so search backward until we find the first one.
				int pos = index - 1;
				while (pos > 0 && this.comparator.compare(this.completions.get(pos), text) == 0) {
					retVal.add(this.completions.get(pos));
					pos--;
				}
			}

			while (index < this.completions.size()) {
				final Completion c = this.completions.get(index);
				if (Util.startsWithIgnoreCase(c.getInputText(), text)) {
					retVal.add(c);
					index++;
				} else
					break;
			}

		}

		return retVal;

	}

	/**
	 * Removes the specified completion from this provider. This method will not
	 * remove completions from the parent provider, if there is one.
	 *
	 * @param c
	 *            The completion to remove.
	 * @return <code>true</code> if this provider contained the specified
	 *         completion.
	 * @see #clear()
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 */
	public boolean removeCompletion(final Completion c) {
		// Don't just call completions.remove(c) as it'll be a linear search.
		final int index = Collections.binarySearch(this.completions, c);
		if (index < 0)
			return false;
		this.completions.remove(index);
		return true;
	}

}