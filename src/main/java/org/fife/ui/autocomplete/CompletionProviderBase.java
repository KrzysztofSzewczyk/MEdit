/*
 * 02/06/2010
 *
 * CompletionProviderBase.java - Base completion provider implementation.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ListCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;

/**
 * A base class for all standard completion providers. This class implements
 * functionality that should be sharable across all <tt>CompletionProvider</tt>
 * implementations.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see AbstractCompletionProvider
 */
public abstract class CompletionProviderBase implements CompletionProvider {

	protected static final String EMPTY_STRING = "";

	/**
	 * Comparator used to sort completions by their relevance before sorting them
	 * lexicographically.
	 */
	private static final Comparator<Completion> sortByRelevanceComparator = new SortByRelevanceComparator();

	/**
	 * Whether auto-activation should occur after letters.
	 */
	private boolean autoActivateAfterLetters;

	/**
	 * Non-letter chars that should cause auto-activation to occur.
	 */
	private String autoActivateChars;

	/**
	 * The renderer to use for completions from this provider. If this is
	 * <code>null</code>, a default renderer is used.
	 */
	private ListCellRenderer listCellRenderer;

	/**
	 * Provides completion choices for a parameterized completion's parameters.
	 */
	private ParameterChoicesProvider paramChoicesProvider;

	/**
	 * Text that marks the end of a parameter list, for example, ')'.
	 */
	private char paramListEnd;

	/**
	 * Text that separates items in a parameter list, for example, ", ".
	 */
	private String paramListSeparator;

	/**
	 * Text that marks the beginning of a parameter list, for example, '('.
	 */
	private char paramListStart;

	/**
	 * The parent completion provider.
	 */
	private CompletionProvider parent;

	/**
	 * A segment to use for fast char access.
	 */
	private final Segment s = new Segment();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearParameterizedCompletionParams() {
		this.paramListEnd = this.paramListStart = 0;
		this.paramListSeparator = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Completion> getCompletions(final JTextComponent comp) {

		final List<Completion> completions = this.getCompletionsImpl(comp);
		if (this.parent != null) {
			final List<Completion> parentCompletions = this.parent.getCompletions(comp);
			if (parentCompletions != null) {
				completions.addAll(parentCompletions);
				Collections.sort(completions);
			}
		}

		// NOTE: We can't sort by relevance prior to this; we need to have
		// things alphabetical so we can easily narrow down completions to
		// those starting with what was already typed.
		if (/* sortByRelevance */true)
			Collections.sort(completions, CompletionProviderBase.sortByRelevanceComparator);

		return completions;

	}

	/**
	 * Does the dirty work of creating a list of completions.
	 *
	 * @param comp
	 *            The text component to look in.
	 * @return The list of possible completions, or an empty list if there are none.
	 */
	protected abstract List<Completion> getCompletionsImpl(JTextComponent comp);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListCellRenderer getListCellRenderer() {
		return this.listCellRenderer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ParameterChoicesProvider getParameterChoicesProvider() {
		return this.paramChoicesProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListEnd() {
		return this.paramListEnd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getParameterListSeparator() {
		return this.paramListSeparator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListStart() {
		return this.paramListStart;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CompletionProvider getParent() {
		return this.parent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoActivateOkay(final JTextComponent tc) {
		final Document doc = tc.getDocument();
		char ch = 0;
		try {
			doc.getText(tc.getCaretPosition(), 1, this.s);
			ch = this.s.first();
		} catch (final BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}
		return this.autoActivateAfterLetters && Character.isLetter(ch)
				|| this.autoActivateChars != null && this.autoActivateChars.indexOf(ch) > -1;
	}

	/**
	 * Sets the characters that auto-activation should occur after. A Java
	 * completion provider, for example, might want to set <code>others</code> to
	 * "<code>.</code>", to allow auto-activation for members of an object.
	 *
	 * @param letters
	 *            Whether auto-activation should occur after any letter.
	 * @param others
	 *            A string of (non-letter) chars that auto-activation should occur
	 *            after. This may be <code>null</code>.
	 */
	public void setAutoActivationRules(final boolean letters, final String others) {
		this.autoActivateAfterLetters = letters;
		this.autoActivateChars = others;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setListCellRenderer(final ListCellRenderer r) {
		this.listCellRenderer = r;
	}

	/**
	 * Sets the param choices provider. This is used when a user code-completes a
	 * parameterized completion, such as a function or method. For any parameter to
	 * the function/method, this object can return possible completions.
	 *
	 * @param pcp
	 *            The parameter choices provider, or <code>null</code> for none.
	 * @see #getParameterChoicesProvider()
	 */
	public void setParameterChoicesProvider(final ParameterChoicesProvider pcp) {
		this.paramChoicesProvider = pcp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setParameterizedCompletionParams(final char listStart, final String separator, final char listEnd) {
		if (listStart < 0x20 || listStart == 0x7F)
			throw new IllegalArgumentException("Invalid listStart");
		if (listEnd < 0x20 || listEnd == 0x7F)
			throw new IllegalArgumentException("Invalid listEnd");
		if (separator == null || separator.length() == 0)
			throw new IllegalArgumentException("Invalid separator");
		this.paramListStart = listStart;
		this.paramListSeparator = separator;
		this.paramListEnd = listEnd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setParent(final CompletionProvider parent) {
		this.parent = parent;
	}

}