/*
 * 05/26/2012
 *
 * TemplateCompletion.java - A completion used to insert boilerplate code
 * snippets that have arbitrary sections the user will want to change, such as
 * for-loops.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;

import org.fife.ui.autocomplete.TemplatePiece.Param;
import org.fife.ui.autocomplete.TemplatePiece.ParamCopy;
import org.fife.ui.autocomplete.TemplatePiece.Text;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;

/**
 * A completion made up of a template with arbitrary parameters that the user
 * can tab through and fill in. This completion type is useful for inserting
 * common boilerplate code, such as for-loops.
 * <p>
 *
 * The format of a template is similar to those in Eclipse. The following
 * example would be the format for a for-loop template:
 *
 * <pre>
 * for (int ${i} = 0; ${i} &lt; ${array}.length; ${i}++) {
 *    ${cursor}
 * }
 * </pre>
 *
 * In the above example, the first <code>${i}</code> is a parameter for the user
 * to type into; all the other <code>${i}</code> instances are automatically
 * changed to what the user types in the first one. The parameter named
 * <code>${cursor}</code> is the "ending position" of the template. It's where
 * the caret moves after it cycles through all other parameters. If the user
 * types into it, template mode terminates. If more than one
 * <code>${cursor}</code> parameter is specified, behavior is undefined.
 * <p>
 *
 * Two dollar signs in a row ("<code>$$</code>") will be evaluated as a single
 * dollar sign. Otherwise, the template parsing is pretty straightforward and
 * fault-tolerant.
 * <p>
 *
 * Leading whitespace is automatically added to lines if the template spans more
 * than one line, and if used with a text component using a
 * <code>PlainDocument</code>, tabs will be converted to spaces if requested.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class TemplateCompletion extends AbstractCompletion implements ParameterizedCompletion {

	private final String definitionString;

	private final String inputText;

	/**
	 * The template's parameters.
	 */
	private final List<Parameter> params;

	private final List<TemplatePiece> pieces;

	private String shortDescription;

	private final String summary;

	public TemplateCompletion(final CompletionProvider provider, final String inputText, final String definitionString,
			final String template) {
		this(provider, inputText, definitionString, template, null, null);
	}

	public TemplateCompletion(final CompletionProvider provider, final String inputText, final String definitionString,
			final String template, final String shortDescription, final String summary) {
		super(provider);
		this.inputText = inputText;
		this.definitionString = definitionString;
		this.shortDescription = shortDescription;
		this.summary = summary;
		this.pieces = new ArrayList<>(3);
		this.params = new ArrayList<>(3);
		this.parse(template);
	}

	private void addTemplatePiece(final TemplatePiece piece) {
		this.pieces.add(piece);
		if (piece instanceof Param && !"cursor".equals(piece.getText())) {
			final String type = null; // TODO
			final Parameter param = new Parameter(type, piece.getText());
			this.params.add(param);
		}
	}

	@Override
	public String getDefinitionString() {
		return this.definitionString;
	}

	@Override
	public String getInputText() {
		return this.inputText;
	}

	@Override
	public ParameterizedCompletionInsertionInfo getInsertionInfo(final JTextComponent tc,
			final boolean replaceTabsWithSpaces) {

		final ParameterizedCompletionInsertionInfo info = new ParameterizedCompletionInsertionInfo();

		final StringBuilder sb = new StringBuilder();
		final int dot = tc.getCaretPosition();

		// Get the range in which the caret can move before we hide
		// this tool tip.
		final int minPos = dot;
		Position maxPos = null;
		int defaultEndOffs = -1;
		try {
			maxPos = tc.getDocument().createPosition(dot);
		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}
		info.setCaretRange(minPos, maxPos);
		int selStart = dot; // Default value
		int selEnd = selStart;

		final Document doc = tc.getDocument();
		String leadingWS = null;
		try {
			leadingWS = RSyntaxUtilities.getLeadingWhitespace(doc, dot);
		} catch (final BadLocationException ble) { // Never happens
			ble.printStackTrace();
			leadingWS = "";
		}

		// Create the text to insert (keep it one completion for
		// performance and simplicity of undo/redo).
		int start = dot;
		for (int i = 0; i < this.pieces.size(); i++) {
			final TemplatePiece piece = this.pieces.get(i);
			final String text = this.getPieceText(i, leadingWS);
			if (piece instanceof Text) {
				if (replaceTabsWithSpaces)
					start = this.possiblyReplaceTabsWithSpaces(sb, text, tc, start);
				else {
					sb.append(text);
					start += text.length();
				}
			} else if (piece instanceof Param && "cursor".equals(text))
				defaultEndOffs = start;
			else {
				final int end = start + text.length();
				sb.append(text);
				if (piece instanceof Param) {
					info.addReplacementLocation(start, end);
					if (selStart == dot) {
						selStart = start;
						selEnd = selStart + text.length();
					}
				} else if (piece instanceof ParamCopy)
					info.addReplacementCopy(piece.getText(), start, end);
				start = end;
			}
		}

		// Highlight the first parameter. If no params were specified, move
		// the caret to the ${cursor} location, if specified
		if (selStart == minPos && selStart == selEnd && this.getParamCount() == 0)
			if (defaultEndOffs > -1)
				selStart = selEnd = defaultEndOffs;
		info.setInitialSelection(selStart, selEnd);

		if (defaultEndOffs > -1)
			// Keep this location "after" all others when tabbing
			info.addReplacementLocation(defaultEndOffs, defaultEndOffs);
		info.setDefaultEndOffs(defaultEndOffs);
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
	 * {@inheritDoc}
	 */
	@Override
	public int getParamCount() {
		return this.params == null ? 0 : this.params.size();
	}

	private String getPieceText(final int index, final String leadingWS) {
		final TemplatePiece piece = this.pieces.get(index);
		String text = piece.getText();
		if (text.indexOf('\n') > -1)
			text = text.replaceAll("\n", "\n" + leadingWS);
		return text;
	}

	/**
	 * Returns <code>null</code>; template completions insert all of their text via
	 * <code>getInsertionInfo()</code>.
	 *
	 * @return <code>null</code> always.
	 */
	@Override
	public String getReplacementText() {
		return null;
	}

	public String getShortDescription() {
		return this.shortDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getShowParameterToolTip() {
		return false;
	}

	@Override
	public String getSummary() {
		return this.summary;
	}

	/**
	 * Returns whether a parameter is already defined with a specific name.
	 *
	 * @param name
	 *            The name.
	 * @return Whether a parameter is defined with that name.
	 */
	private boolean isParamDefined(final String name) {
		for (int i = 0; i < this.getParamCount(); i++) {
			final Parameter param = this.getParam(i);
			if (name.equals(param.getName()))
				return true;
		}
		return false;
	}

	/**
	 * Parses a template string into logical pieces used by this class.
	 *
	 * @param template
	 *            The template to parse.
	 */
	private void parse(final String template) {

		int offs = 0;
		int lastOffs = 0;

		while ((offs = template.indexOf('$', lastOffs)) > -1 && offs < template.length() - 1) {

			final char next = template.charAt(offs + 1);
			switch (next) {
			case '$': // "$$" => escaped single dollar sign
				this.addTemplatePiece(new TemplatePiece.Text(template.substring(lastOffs, offs + 1)));
				lastOffs = offs + 2;
				break;
			case '{': // "${...}" => variable
				final int closingCurly = template.indexOf('}', offs + 2);
				if (closingCurly > -1) {
					this.addTemplatePiece(new TemplatePiece.Text(template.substring(lastOffs, offs)));
					final String varName = template.substring(offs + 2, closingCurly);
					if (!"cursor".equals(varName) && this.isParamDefined(varName))
						this.addTemplatePiece(new TemplatePiece.ParamCopy(varName));
					else
						this.addTemplatePiece(new TemplatePiece.Param(varName));
					lastOffs = closingCurly + 1;
				}
				break;
			}

		}

		if (lastOffs < template.length()) {
			final String text = template.substring(lastOffs);
			this.addTemplatePiece(new TemplatePiece.Text(text));
		}

	}

	private int possiblyReplaceTabsWithSpaces(final StringBuilder sb, final String text, final JTextComponent tc,
			int start) {

		int tab = text.indexOf('\t');
		if (tab > -1) {

			final int startLen = sb.length();

			int size = 4;
			final Document doc = tc.getDocument();
			if (doc != null) {
				final Integer i = (Integer) doc.getProperty(PlainDocument.tabSizeAttribute);
				if (i != null)
					size = i.intValue();
			}
			String tabStr = "";
			for (int i = 0; i < size; i++)
				tabStr += " ";

			int lastOffs = 0;
			do {
				sb.append(text.substring(lastOffs, tab));
				sb.append(tabStr);
				lastOffs = tab + 1;
			} while ((tab = text.indexOf('\t', lastOffs)) > -1);
			sb.append(text.substring(lastOffs));

			start += sb.length() - startLen;

		} else {
			sb.append(text);
			start += text.length();
		}

		return start;

	}

	/**
	 * Sets the short description of this template completion.
	 *
	 * @param shortDesc
	 *            The new short description.
	 * @see #getShortDescription()
	 */
	public void setShortDescription(final String shortDesc) {
		this.shortDescription = shortDesc;
	}

	@Override
	public String toString() {
		return this.getDefinitionString();
	}

}