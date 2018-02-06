/*
 * 12/22/2008
 *
 * FunctionCompletion.java - A completion representing a function.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

/**
 * A completion choice representing a function.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FunctionCompletion extends VariableCompletion implements ParameterizedCompletion {

	/**
	 * Used to improve performance of sorting FunctionCompletions.
	 */
	private String compareString;

	/**
	 * Parameters to the function.
	 */
	private List<Parameter> params;

	/**
	 * A description of the return value of this function.
	 */
	private String returnValDesc;

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The parent provider.
	 * @param name
	 *            The name of this function.
	 * @param returnType
	 *            The return type of this function.
	 */
	public FunctionCompletion(final CompletionProvider provider, final String name, final String returnType) {
		super(provider, name, returnType);
	}

	@Override
	protected void addDefinitionString(final StringBuilder sb) {
		sb.append("<html><b>");
		sb.append(this.getDefinitionString());
		sb.append("</b>");
	}

	/**
	 * Adds HTML describing the parameters to this function to a buffer.
	 *
	 * @param sb
	 *            The buffer to append to.
	 */
	protected void addParameters(final StringBuilder sb) {

		// TODO: Localize me

		final int paramCount = this.getParamCount();
		if (paramCount > 0) {
			sb.append("<b>Parameters:</b><br>");
			sb.append("<center><table width='90%'><tr><td>");
			for (int i = 0; i < paramCount; i++) {
				final Parameter param = this.getParam(i);
				sb.append("<b>");
				sb.append(param.getName() != null ? param.getName() : param.getType());
				sb.append("</b>&nbsp;");
				final String desc = param.getDescription();
				if (desc != null)
					sb.append(desc);
				sb.append("<br>");
			}
			sb.append("</td></tr></table></center><br><br>");
		}

		if (this.returnValDesc != null) {
			sb.append("<b>Returns:</b><br><center><table width='90%'><tr><td>");
			sb.append(this.returnValDesc);
			sb.append("</td></tr></table></center><br><br>");
		}

	}

	/**
	 * Overridden to compare methods by their comparison strings.
	 *
	 * @param c2
	 *            A <code>Completion</code> to compare to.
	 * @return The sort order.
	 */
	@Override
	public int compareTo(final Completion c2) {

		int rc = -1;

		if (c2 == this)
			rc = 0;
		else if (c2 instanceof FunctionCompletion)
			rc = this.getCompareString().compareTo(((FunctionCompletion) c2).getCompareString());
		else
			rc = super.compareTo(c2);

		return rc;

	}

	/**
	 * Returns a string used to compare this method completion to another.
	 *
	 * @return The comparison string.
	 */
	private String getCompareString() {

		/*
		 * This string compares the following parts of methods in this order, to
		 * optimize sort order in completion lists.
		 *
		 * 1. First, by name 2. Next, by number of parameters. 3. Finally, by parameter
		 * type.
		 */

		if (this.compareString == null) {
			final StringBuilder sb = new StringBuilder(this.getName());
			// NOTE: This will fail if a method has > 99 parameters (!)
			final int paramCount = this.getParamCount();
			if (paramCount < 10)
				sb.append('0');
			sb.append(paramCount);
			for (int i = 0; i < paramCount; i++) {
				final String type = this.getParam(i).getType();
				sb.append(type);
				if (i < paramCount - 1)
					sb.append(',');
			}
			this.compareString = sb.toString();
		}

		return this.compareString;

	}

	/**
	 * Returns the "definition string" for this function completion. For example,
	 * for the C "<code>printf</code>" function, this would return
	 * "<code>int printf(const char *, ...)</code>".
	 *
	 * @return The definition string.
	 */
	@Override
	public String getDefinitionString() {

		final StringBuilder sb = new StringBuilder();

		// Add the return type if applicable (C macros like NULL have no type).
		String type = this.getType();
		if (type != null)
			sb.append(type).append(' ');

		// Add the item being described's name.
		sb.append(this.getName());

		// Add parameters for functions.
		final CompletionProvider provider = this.getProvider();
		final char start = provider.getParameterListStart();
		if (start != 0)
			sb.append(start);
		for (int i = 0; i < this.getParamCount(); i++) {
			final Parameter param = this.getParam(i);
			type = param.getType();
			final String name = param.getName();
			if (type != null) {
				sb.append(type);
				if (name != null)
					sb.append(' ');
			}
			if (name != null)
				sb.append(name);
			if (i < this.params.size() - 1)
				sb.append(provider.getParameterListSeparator());
		}
		final char end = provider.getParameterListEnd();
		if (end != 0)
			sb.append(end);

		return sb.toString();

	}

	@Override
	public ParameterizedCompletionInsertionInfo getInsertionInfo(final JTextComponent tc,
			final boolean replaceTabsWithSpaces) {

		final ParameterizedCompletionInsertionInfo info = new ParameterizedCompletionInsertionInfo();

		final StringBuilder sb = new StringBuilder();
		final char paramListStart = this.getProvider().getParameterListStart();
		if (paramListStart != '\0')
			sb.append(paramListStart);
		final int dot = tc.getCaretPosition() + sb.length();
		final int paramCount = this.getParamCount();

		// Get the range in which the caret can move before we hide
		// this tool tip.
		final int minPos = dot;
		Position maxPos = null;
		try {
			maxPos = tc.getDocument().createPosition(dot - sb.length() + 1);
		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}
		info.setCaretRange(minPos, maxPos);
		int firstParamLen = 0;

		// Create the text to insert (keep it one completion for
		// performance and simplicity of undo/redo).
		int start = dot;
		for (int i = 0; i < paramCount; i++) {
			final Parameter param = this.getParam(i);
			final String paramText = this.getParamText(param);
			if (i == 0)
				firstParamLen = paramText.length();
			sb.append(paramText);
			final int end = start + paramText.length();
			info.addReplacementLocation(start, end);
			// Patch for param. list separators with length > 2 -
			// thanks to Matthew Adereth!
			final String sep = this.getProvider().getParameterListSeparator();
			if (i < paramCount - 1 && sep != null) {
				sb.append(sep);
				start = end + sep.length();
			}
		}
		sb.append(this.getProvider().getParameterListEnd());
		int endOffs = dot + sb.length();
		endOffs -= 1;// getProvider().getParameterListStart().length();
		info.addReplacementLocation(endOffs, endOffs); // offset after function
		info.setDefaultEndOffs(endOffs);

		final int selectionEnd = paramCount > 0 ? dot + firstParamLen : dot;
		info.setInitialSelection(dot, selectionEnd);
		info.setTextToInsert(sb.toString());
		return info;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Parameter getParam(final int index) {
		return this.params.get(index);
	}

	/**
	 * Returns the number of parameters to this function.
	 *
	 * @return The number of parameters to this function.
	 * @see #getParam(int)
	 */
	@Override
	public int getParamCount() {
		return this.params == null ? 0 : this.params.size();
	}

	/**
	 * Returns the text to insert for a parameter.
	 *
	 * @param param
	 *            The parameter.
	 * @return The text.
	 */
	private String getParamText(final ParameterizedCompletion.Parameter param) {
		String text = param.getName();
		if (text == null) {
			text = param.getType();
			if (text == null)
				text = "arg";
		}
		return text;
	}

	/**
	 * Returns the description of the return value of this function.
	 *
	 * @return The description, or <code>null</code> if there is none.
	 * @see #setReturnValueDescription(String)
	 */
	public String getReturnValueDescription() {
		return this.returnValDesc;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getShowParameterToolTip() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSummary() {
		final StringBuilder sb = new StringBuilder();
		this.addDefinitionString(sb);
		if (!this.possiblyAddDescription(sb))
			sb.append("<br><br><br>");
		this.addParameters(sb);
		this.possiblyAddDefinedIn(sb);
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getToolTipText() {
		String text = this.getSummary();
		if (text == null)
			text = this.getDefinitionString();
		return text;
	}

	/**
	 * Sets the parameters to this function.
	 *
	 * @param params
	 *            The parameters. This should be a list of
	 *            {@link ParameterizedCompletion.Parameter}s.
	 * @see #getParam(int)
	 * @see #getParamCount()
	 */
	public void setParams(final List<Parameter> params) {
		if (params != null)
			// Deep copy so parsing can re-use its array.
			this.params = new ArrayList<>(params);
	}

	/**
	 * Sets the description of the return value of this function.
	 *
	 * @param desc
	 *            The description.
	 * @see #getReturnValueDescription()
	 */
	public void setReturnValueDescription(final String desc) {
		this.returnValDesc = desc;
	}

}