/*
 * 12/17/2010
 *
 * SortByRelevanceComparator.java - Sorts two Completions by relevance before
 * sorting them lexicographically.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.Comparator;

/**
 * Compares two <code>Completion</code>s by their relevance before sorting them
 * lexicographically.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class SortByRelevanceComparator implements Comparator<Completion> {

	@Override
	public int compare(final Completion c1, final Completion c2) {
		final int rel1 = c1.getRelevance();
		final int rel2 = c2.getRelevance();
		final int diff = rel2 - rel1;// rel1 - rel2;
		return diff == 0 ? c1.compareTo(c2) : diff;
	}

}