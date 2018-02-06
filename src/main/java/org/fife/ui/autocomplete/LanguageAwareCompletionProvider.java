/*
 * 01/03/2009
 *
 * LanguageAwareCompletionProvider.java - A completion provider that is aware
 * of the language it is working with.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

/**
 * A completion provider for the C programming language (and other languages
 * with similar syntax). This provider simply delegates to another provider,
 * depending on whether the caret is in:
 *
 * <ul>
 * <li>Code (plain text)</li>
 * <li>A string</li>
 * <li>A comment</li>
 * <li>A documentation comment</li>
 * </ul>
 *
 * This allows for different completion choices in comments than in code, for
 * example.
 * <p>
 *
 * This provider also implements the
 * <tt>org.fife.ui.rtextarea.ToolTipSupplier</tt> interface, which allows it to
 * display tooltips for completion choices. Thus the standard
 * {@link VariableCompletion} and {@link FunctionCompletion} completions should
 * be able to display tooltips with the variable declaration or function
 * definition (provided the <tt>RSyntaxTextArea</tt> was registered with the
 * <tt>javax.swing.ToolTipManager</tt>).
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LanguageAwareCompletionProvider extends CompletionProviderBase implements ToolTipSupplier {

	/**
	 * The provider to use when completing in a comment.
	 */
	private CompletionProvider commentCompletionProvider;

	/**
	 * The provider to use when no provider is assigned to a particular token type.
	 */
	private CompletionProvider defaultProvider;

	/**
	 * The provider to use while in documentation comments.
	 */
	private CompletionProvider docCommentCompletionProvider;

	/**
	 * The provider to use when completing in a string.
	 */
	private CompletionProvider stringCompletionProvider;

	/**
	 * Constructor subclasses can use when they don't have their default provider
	 * created at construction time. They should call
	 * {@link #setDefaultCompletionProvider(CompletionProvider)} in this
	 * constructor.
	 */
	protected LanguageAwareCompletionProvider() {
	}

	/**
	 * Constructor.
	 *
	 * @param defaultProvider
	 *            The provider to use when no provider is assigned to a particular
	 *            token type. This cannot be <code>null</code>.
	 */
	public LanguageAwareCompletionProvider(final CompletionProvider defaultProvider) {
		this.setDefaultCompletionProvider(defaultProvider);
	}

	/**
	 * Calling this method will result in an {@link UnsupportedOperationException}
	 * being thrown. To set the parameter completion parameters, do so on the
	 * provider returned by {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException
	 *             Always.
	 * @see #setParameterizedCompletionParams(char, String, char)
	 */
	@Override
	public void clearParameterizedCompletionParams() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAlreadyEnteredText(final JTextComponent comp) {
		if (!(comp instanceof RSyntaxTextArea))
			return CompletionProviderBase.EMPTY_STRING;
		final CompletionProvider provider = this.getProviderFor(comp);
		return provider != null ? provider.getAlreadyEnteredText(comp) : null;
	}

	/**
	 * Returns the completion provider to use for comments.
	 *
	 * @return The completion provider to use.
	 * @see #setCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getCommentCompletionProvider() {
		return this.commentCompletionProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Completion> getCompletionsAt(final JTextComponent tc, final Point p) {
		return this.defaultProvider == null ? null : this.defaultProvider.getCompletionsAt(tc, p);
	}

	/**
	 * Does the dirty work of creating a list of completions.
	 *
	 * @param comp
	 *            The text component to look in.
	 * @return The list of possible completions, or an empty list if there are none.
	 */
	@Override
	protected List<Completion> getCompletionsImpl(final JTextComponent comp) {
		if (comp instanceof RSyntaxTextArea) {
			final CompletionProvider provider = this.getProviderFor(comp);
			if (provider != null)
				return provider.getCompletions(comp);
		}
		return Collections.emptyList();
	}

	/**
	 * Returns the completion provider used when one isn't defined for a particular
	 * token type.
	 *
	 * @return The completion provider to use.
	 * @see #setDefaultCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDefaultCompletionProvider() {
		return this.defaultProvider;
	}

	/**
	 * Returns the completion provider to use for documentation comments.
	 *
	 * @return The completion provider to use.
	 * @see #setDocCommentCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getDocCommentCompletionProvider() {
		return this.docCommentCompletionProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(final JTextComponent tc) {
		// Parameterized completions can only come from the "code" completion
		// provider. We do not do function/method completions while editing
		// strings or comments.
		final CompletionProvider provider = this.getProviderFor(tc);
		return provider == this.defaultProvider ? provider.getParameterizedCompletions(tc) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListEnd() {
		return this.defaultProvider.getParameterListEnd();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getParameterListSeparator() {
		return this.defaultProvider.getParameterListSeparator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char getParameterListStart() {
		return this.defaultProvider.getParameterListStart();
	}

	/**
	 * Returns the completion provider to use at the current caret position in a
	 * text component.
	 *
	 * @param comp
	 *            The text component to check.
	 * @return The completion provider to use.
	 */
	private CompletionProvider getProviderFor(final JTextComponent comp) {

		final RSyntaxTextArea rsta = (RSyntaxTextArea) comp;
		final RSyntaxDocument doc = (RSyntaxDocument) rsta.getDocument();
		final int line = rsta.getCaretLineNumber();
		final Token t = doc.getTokenListForLine(line);
		if (t == null)
			return this.getDefaultCompletionProvider();

		final int dot = rsta.getCaretPosition();
		final Token curToken = RSyntaxUtilities.getTokenAtOffset(t, dot);

		if (curToken == null) { // At end of the line

			int type = doc.getLastTokenTypeOnLine(line);
			if (type == TokenTypes.NULL) {
				final Token temp = t.getLastPaintableToken();
				if (temp == null)
					return this.getDefaultCompletionProvider();
				type = temp.getType();
			}

			// TokenMakers can use types < 0 for "internal types." This
			// gives them a chance to map their internal types back to "real"
			// types to get completion providers.
			else if (type < 0)
				type = doc.getClosestStandardTokenTypeForInternalType(type);

			switch (type) {
			case TokenTypes.ERROR_STRING_DOUBLE:
				return this.getStringCompletionProvider();
			case TokenTypes.COMMENT_EOL:
			case TokenTypes.COMMENT_MULTILINE:
				return this.getCommentCompletionProvider();
			case TokenTypes.COMMENT_DOCUMENTATION:
				return this.getDocCommentCompletionProvider();
			default:
				return this.getDefaultCompletionProvider();
			}

		}

		// FIXME: This isn't always a safe assumption.
		if (dot == curToken.getOffset())
			// Need to check previous token for its type before deciding.
			// Previous token may also be on previous line!
			return this.getDefaultCompletionProvider();

		switch (curToken.getType()) {
		case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
		case TokenTypes.ERROR_STRING_DOUBLE:
			return this.getStringCompletionProvider();
		case TokenTypes.COMMENT_EOL:
		case TokenTypes.COMMENT_MULTILINE:
			return this.getCommentCompletionProvider();
		case TokenTypes.COMMENT_DOCUMENTATION:
			return this.getDocCommentCompletionProvider();
		case TokenTypes.NULL:
		case TokenTypes.WHITESPACE:
		case TokenTypes.IDENTIFIER:
		case TokenTypes.VARIABLE:
		case TokenTypes.PREPROCESSOR:
		case TokenTypes.DATA_TYPE:
		case TokenTypes.FUNCTION:
		case TokenTypes.OPERATOR:
			return this.getDefaultCompletionProvider();
		}

		return null; // In a token type we can't auto-complete from.

	}

	/**
	 * Returns the completion provider to use for strings.
	 *
	 * @return The completion provider to use.
	 * @see #setStringCompletionProvider(CompletionProvider)
	 */
	public CompletionProvider getStringCompletionProvider() {
		return this.stringCompletionProvider;
	}

	/**
	 * Returns the tool tip to display for a mouse event.
	 * <p>
	 *
	 * For this method to be called, the <tt>RSyntaxTextArea</tt> must be registered
	 * with the <tt>javax.swing.ToolTipManager</tt> like so:
	 *
	 * <pre>
	 * ToolTipManager.sharedInstance().registerComponent(textArea);
	 * </pre>
	 *
	 * @param textArea
	 *            The text area.
	 * @param e
	 *            The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 */
	@Override
	public String getToolTipText(final RTextArea textArea, final MouseEvent e) {

		String tip = null;

		final List<Completion> completions = this.getCompletionsAt(textArea, e.getPoint());
		if (completions != null && completions.size() > 0) {
			// Only ever 1 match for us in C...
			final Completion c = completions.get(0);
			tip = c.getToolTipText();
		}

		return tip;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoActivateOkay(final JTextComponent tc) {
		final CompletionProvider provider = this.getProviderFor(tc);
		return provider != null ? provider.isAutoActivateOkay(tc) : false;
	}

	/**
	 * Sets the comment completion provider.
	 *
	 * @param provider
	 *            The provider to use in comments.
	 * @see #getCommentCompletionProvider()
	 */
	public void setCommentCompletionProvider(final CompletionProvider provider) {
		this.commentCompletionProvider = provider;
	}

	/**
	 * Sets the default completion provider.
	 *
	 * @param provider
	 *            The provider to use when no provider is assigned to a particular
	 *            token type. This cannot be <code>null</code>.
	 * @see #getDefaultCompletionProvider()
	 */
	public void setDefaultCompletionProvider(final CompletionProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException("provider cannot be null");
		this.defaultProvider = provider;
	}

	/**
	 * Sets the documentation comment completion provider.
	 *
	 * @param provider
	 *            The provider to use in comments.
	 * @see #getDocCommentCompletionProvider()
	 */
	public void setDocCommentCompletionProvider(final CompletionProvider provider) {
		this.docCommentCompletionProvider = provider;
	}

	/**
	 * Calling this method will result in an {@link UnsupportedOperationException}
	 * being thrown. To set the parameter completion parameters, do so on the
	 * provider returned by {@link #getDefaultCompletionProvider()}.
	 *
	 * @throws UnsupportedOperationException
	 *             Always.
	 * @see #clearParameterizedCompletionParams()
	 */
	@Override
	public void setParameterizedCompletionParams(final char listStart, final String separator, final char listEnd) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the completion provider to use while in a string.
	 *
	 * @param provider
	 *            The provider to use.
	 * @see #getStringCompletionProvider()
	 */
	public void setStringCompletionProvider(final CompletionProvider provider) {
		this.stringCompletionProvider = provider;
	}

}