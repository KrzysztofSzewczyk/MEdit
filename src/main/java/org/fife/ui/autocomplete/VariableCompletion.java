/*
 * 12/22/2008
 *
 * VariableCompletion.java - A completion for a variable.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import javax.swing.text.JTextComponent;

/**
 * A completion for a variable (or constant) in a programming language.
 * <p>
 *
 * This completion type uses its <tt>shortDescription</tt> property as part of
 * its summary returned by {@link #getSummary()}; for this reason, it may be a
 * little longer (even much longer), if desired, than what is recommended for
 * <tt>BasicCompletion</tt>s (where the <tt>shortDescription</tt> is used in
 * {@link #toString()} for <tt>ListCellRenderers</tt>).
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class VariableCompletion extends BasicCompletion {

	/**
	 * What library (for example) this variable is defined in.
	 */
	private String definedIn;

	/**
	 * The variable's type.
	 */
	private final String type;

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The parent provider.
	 * @param name
	 *            The name of this variable.
	 * @param type
	 *            The type of this variable (e.g. "<code>int</code>",
	 *            "<code>String</code>", etc.).
	 */
	public VariableCompletion(final CompletionProvider provider, final String name, final String type) {
		super(provider, name);
		this.type = type;
	}

	protected void addDefinitionString(final StringBuilder sb) {
		sb.append("<html><b>").append(this.getDefinitionString()).append("</b>");
	}

	/**
	 * Returns where this variable is defined.
	 *
	 * @return Where this variable is defined.
	 * @see #setDefinedIn(String)
	 */
	public String getDefinedIn() {
		return this.definedIn;
	}

	public String getDefinitionString() {

		final StringBuilder sb = new StringBuilder();

		// Add the return type if applicable (C macros like NULL have no type).
		if (this.type != null)
			sb.append(this.type).append(' ');

		// Add the item being described's name.
		sb.append(this.getName());

		return sb.toString();

	}

	/**
	 * Returns the name of this variable.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.getReplacementText();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSummary() {
		final StringBuilder sb = new StringBuilder();
		this.addDefinitionString(sb);
		this.possiblyAddDescription(sb);
		this.possiblyAddDefinedIn(sb);
		return sb.toString();
	}

	/**
	 * Returns the tool tip text to display for mouse hovers over this completion.
	 * <p>
	 *
	 * Note that for this functionality to be enabled, a <tt>JTextComponent</tt>
	 * must be registered with the <tt>ToolTipManager</tt>, and the text component
	 * must know to search for this value. In the case of an
	 * <a href="http://fifesoft.com/rsyntaxtextarea">RSyntaxTextArea</a>, this can
	 * be done with a <tt>org.fife.ui.rtextarea.ToolTipSupplier</tt> that calls into
	 * {@link CompletionProvider#getCompletionsAt(JTextComponent, java.awt.Point)}.
	 *
	 * @return The tool tip text for this completion, or <code>null</code> if none.
	 */
	@Override
	public String getToolTipText() {
		return this.getDefinitionString();
	}

	/**
	 * Returns the type of this variable.
	 *
	 * @return The type.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Adds some HTML describing where this variable is defined, if this information
	 * is known.
	 *
	 * @param sb
	 *            The buffer to append to.
	 */
	protected void possiblyAddDefinedIn(final StringBuilder sb) {
		if (this.definedIn != null) {
			sb.append("<hr>Defined in:"); // TODO: Localize me
			sb.append(" <em>").append(this.definedIn).append("</em>");
		}
	}

	/**
	 * Adds the description text as HTML to a buffer, if a description is defined.
	 *
	 * @param sb
	 *            The buffer to append to.
	 * @return Whether there was a description to add.
	 */
	protected boolean possiblyAddDescription(final StringBuilder sb) {
		if (this.getShortDescription() != null) {
			sb.append("<hr><br>");
			sb.append(this.getShortDescription());
			sb.append("<br><br><br>");
			return true;
		}
		return false;
	}

	/**
	 * Sets where this variable is defined.
	 *
	 * @param definedIn
	 *            Where this variable is defined.
	 * @see #getDefinedIn()
	 */
	public void setDefinedIn(final String definedIn) {
		this.definedIn = definedIn;
	}

	/**
	 * Overridden to return the name of the variable being completed.
	 *
	 * @return A string representation of this completion.
	 */
	@Override
	public String toString() {
		return this.getName();
	}

}