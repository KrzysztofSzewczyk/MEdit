/*
 * 01/06/2009
 *
 * MarkupTagComletion.java - A completion representing a tag in markup, such
 * as HTML or XML.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;

/**
 * A completion representing a tag in markup, such as HTML or XML.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class MarkupTagCompletion extends AbstractCompletion {

	/**
	 * Attributes of the tag.
	 */
	private List<Parameter> attrs;
	private String definedIn;
	private String desc;

	private final String name;

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The parent provider instance.
	 * @param name
	 *            The name of the tag.
	 */
	public MarkupTagCompletion(final CompletionProvider provider, final String name) {
		super(provider);
		this.name = name;
	}

	/**
	 * Adds HTML describing the attributes of this tag to a buffer.
	 *
	 * @param sb
	 *            The buffer to append to.
	 */
	protected void addAttributes(final StringBuilder sb) {

		// TODO: Localize me.

		final int attrCount = this.getAttributeCount();
		if (attrCount > 0) {
			sb.append("<b>Attributes:</b><br>");
			sb.append("<center><table width='90%'><tr><td>");
			for (int i = 0; i < attrCount; i++) {
				final Parameter attr = this.getAttribute(i);
				sb.append("&nbsp;&nbsp;&nbsp;<b>");
				sb.append(attr.getName() != null ? attr.getName() : attr.getType());
				sb.append("</b>&nbsp;");
				final String desc = attr.getDescription();
				if (desc != null)
					sb.append(desc);
				sb.append("<br>");
			}
			sb.append("</td></tr></table></center><br><br>");
		}

	}

	protected void addDefinitionString(final StringBuilder sb) {
		sb.append("<html><b>").append(this.name).append("</b>");
	}

	/**
	 * Returns the specified {@link ParameterizedCompletion.Parameter}.
	 *
	 * @param index
	 *            The index of the attribute to retrieve.
	 * @return The attribute.
	 * @see #getAttributeCount()
	 */
	public Parameter getAttribute(final int index) {
		return this.attrs.get(index);
	}

	/**
	 * Returns the number of attributes of this tag.
	 *
	 * @return The number of attributes of this tag.
	 * @see #getAttribute(int)
	 */
	public int getAttributeCount() {
		return this.attrs == null ? 0 : this.attrs.size();
	}

	/**
	 * Returns all attributes of this tag.
	 *
	 * @return A list of {@link ParameterizedCompletion.Parameter}s.
	 * @see #getAttribute(int)
	 * @see #getAttributeCount()
	 */
	public List<Parameter> getAttributes() {
		return this.attrs;
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

	/**
	 * Returns a short description of this variable. This should be an HTML snippet.
	 *
	 * @return A short description of this variable. This may be <code>null</code>.
	 * @see #setDescription(String)
	 */
	public String getDescription() {
		return this.desc;
	}

	/**
	 * Returns the name of this tag.
	 *
	 * @return The name of this tag.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getReplacementText() {
		return this.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSummary() {
		final StringBuilder sb = new StringBuilder();
		this.addDefinitionString(sb);
		this.possiblyAddDescription(sb);
		this.addAttributes(sb);
		this.possiblyAddDefinedIn(sb);
		return sb.toString();
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
	 */
	protected void possiblyAddDescription(final StringBuilder sb) {
		if (this.desc != null) {
			sb.append("<hr><br>");
			sb.append(this.desc);
			sb.append("<br><br><br>");
		}
	}

	/**
	 * Sets the attributes of this tag.
	 *
	 * @param attrs
	 *            The attributes.
	 * @see #getAttribute(int)
	 * @see #getAttributeCount()
	 */
	public void setAttributes(final List<? extends Parameter> attrs) {
		// Deep copy so parsing can re-use its array.
		this.attrs = new ArrayList<>(attrs);
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
	 * Sets the short description of this tag. This should be an HTML snippet.
	 *
	 * @param desc
	 *            A short description of this tag. This may be <code>null</code>.
	 * @see #getDescription()
	 */
	public void setDescription(final String desc) {
		this.desc = desc;
	}

}