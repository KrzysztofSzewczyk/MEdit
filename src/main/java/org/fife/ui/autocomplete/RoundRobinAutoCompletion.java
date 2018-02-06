/*
 * 12/02/2013
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

/**
 * An <code>AutoCompletion</code> that adds the ability to cycle through a set
 * of <code>CompletionProvider</code>s via the trigger key. This allows the
 * application to logically "group together" completions of similar kinds; for
 * example, Java code completions vs. template completions.
 * <p>
 *
 * Usage:
 *
 * <pre>
 * XPathDynamicCompletionProvider dynamicProvider = new XPathDynamicCompletionProvider();
 * RoundRobinAutoCompletion ac = new RoundRobinAutoCompletion(dynamicProvider);
 * XPathCompletionProvider staticProvider = new XPathCompletionProvider();
 * ac.addCompletionProvider(staticProvider);
 * ac.setXXX(..);
 * ...
 * ac.install(textArea);
 * </pre>
 *
 * @author mschlegel
 */
public class RoundRobinAutoCompletion extends AutoCompletion {

	/**
	 * An implementation of the auto-complete action that ensures the proper
	 * <code>CompletionProvider</code> is displayed based on the context in which
	 * the user presses the trigger key.
	 */
	private class CycleAutoCompleteAction extends AutoCompleteAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (RoundRobinAutoCompletion.this.isAutoCompleteEnabled()) {
				if (RoundRobinAutoCompletion.this.isPopupVisible())
					// The popup is already visible, and user pressed the
					// trigger-key. In this case, move to next provider.
					RoundRobinAutoCompletion.this.advanceProvider();
				else
					// Be sure to start with the default provider
					RoundRobinAutoCompletion.this.resetProvider();
				// Check if there are completions from the current provider. If not, advance to
				// the next provider and display that one.
				// A completion provider can force displaying "his" empty completion pop-up by
				// returning an empty BasicCompletion. This is useful when the user is typing
				// backspace and you like to display the first provider always first.
				for (int i = 1; i < RoundRobinAutoCompletion.this.cycle.size(); i++) {
					final List<Completion> completions = RoundRobinAutoCompletion.this.getCompletionProvider()
							.getCompletions(RoundRobinAutoCompletion.this.getTextComponent());
					if (completions.size() > 0)
						// nothing to do, just let the current provider display
						break;
					else
						// search for non-empty completions
						RoundRobinAutoCompletion.this.advanceProvider();
				}
			}
			super.actionPerformed(e);
		}

	}

	/** The List of CompletionProviders to use */
	private final List<CompletionProvider> cycle = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            A single completion provider.
	 * @see #addCompletionProvider(CompletionProvider)
	 */
	public RoundRobinAutoCompletion(final CompletionProvider provider) {

		super(provider);
		this.cycle.add(provider);

		// principal requirement for round-robin
		this.setHideOnCompletionProviderChange(false);
		// this is required since otherwise, on empty list of completions for
		// one of the CompletionProviders, round-robin completion would not
		// work
		this.setHideOnNoText(false);
		// this is required to prevent single choice of 1st provider to choose
		// the completion since the user may want the second provider to be
		// chosen.
		this.setAutoCompleteSingleChoices(false);

	}

	/**
	 * Adds an additional <code>CompletionProvider</code> to the list to cycle
	 * through.
	 *
	 * @param provider
	 *            The new completion provider.
	 */
	public void addCompletionProvider(final CompletionProvider provider) {
		this.cycle.add(provider);
	}

	/**
	 * Moves to the next Provider internally. Needs refresh of the popup window to
	 * display the changes.
	 *
	 * @return true if the next provider was the default one (thus returned to the
	 *         default view). May be used in case you like to hide the popup in this
	 *         case.
	 */
	public boolean advanceProvider() {
		final CompletionProvider currentProvider = this.getCompletionProvider();
		final int i = (this.cycle.indexOf(currentProvider) + 1) % this.cycle.size();
		this.setCompletionProvider(this.cycle.get(i));
		return i == 0;
	}

	/**
	 * Overridden to provide our own implementation of the action.
	 */
	@Override
	protected Action createAutoCompleteAction() {
		return new CycleAutoCompleteAction();
	}

	/**
	 * Resets the cycle to use the default provider on next refresh.
	 */
	public void resetProvider() {
		final CompletionProvider currentProvider = this.getCompletionProvider();
		final CompletionProvider defaultProvider = this.cycle.get(0);
		if (currentProvider != defaultProvider)
			this.setCompletionProvider(defaultProvider);
	}

	// TODO add label "Ctrl-Space for <next provider name>" to the popup window
}