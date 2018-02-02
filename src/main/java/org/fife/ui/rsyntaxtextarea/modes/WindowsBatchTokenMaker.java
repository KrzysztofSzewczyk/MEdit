/*
 * 03/07/2004
 *
 * WindowsBatchTokenMaker.java - Scanner for Windows batch files.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.modes;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * A token maker that turns text into a linked list of <code>Token</code>s for
 * syntax highlighting Microsoft Windows batch files.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class WindowsBatchTokenMaker extends AbstractTokenMaker {

	private enum VariableType {
		BRACKET_VAR, DOUBLE_PERCENT_VAR, NORMAL_VAR, TILDE_VAR; // Escaped '%' var, special highlighting rules?
	}

	private int currentTokenStart;
	private int currentTokenType;

	protected final String operators = "@:*<>=?";

	private VariableType varType;

	/**
	 * Constructor.
	 */
	public WindowsBatchTokenMaker() {
		super(); // Initializes tokensToHighlight.
	}

	/**
	 * Checks the token to give it the exact ID it deserves before being passed up
	 * to the super method.
	 *
	 * @param segment
	 *            <code>Segment</code> to get text from.
	 * @param start
	 *            Start offset in <code>segment</code> of token.
	 * @param end
	 *            End offset in <code>segment</code> of token.
	 * @param tokenType
	 *            The token's type.
	 * @param startOffset
	 *            The offset in the document at which the token occurs.
	 */
	@Override
	public void addToken(final Segment segment, final int start, final int end, int tokenType, final int startOffset) {

		switch (tokenType) {
		// Since reserved words, functions, and data types are all passed
		// into here as "identifiers," we have to see what the token
		// really is...
		case TokenTypes.IDENTIFIER:
			final int value = this.wordsToHighlight.get(segment, start, end);
			if (value != -1)
				tokenType = value;
			break;
		}

		super.addToken(segment, start, end, tokenType, startOffset);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getLineCommentStartAndEnd(final int languageIndex) {
		return new String[] { "rem ", null };
	}

	/**
	 * Returns whether tokens of the specified type should have "mark occurrences"
	 * enabled for the current programming language.
	 *
	 * @param type
	 *            The token type.
	 * @return Whether tokens of this type should have "mark occurrences" enabled.
	 */
	@Override
	public boolean getMarkOccurrencesOfTokenType(final int type) {
		return type == TokenTypes.IDENTIFIER || type == TokenTypes.VARIABLE;
	}

	/**
	 * Returns a list of tokens representing the given text.
	 *
	 * @param text
	 *            The text to break into tokens.
	 * @param startTokenType
	 *            The token with which to start tokenizing.
	 * @param startOffset
	 *            The offset at which the line of tokens begins.
	 * @return A linked list of tokens representing <code>text</code>.
	 */
	@Override
	public Token getTokenList(final Segment text, final int startTokenType, final int startOffset) {

		this.resetTokenList();

		final char[] array = text.array;
		final int offset = text.offset;
		final int count = text.count;
		final int end = offset + count;

		// See, when we find a token, its starting position is always of the form:
		// 'startOffset + (currentTokenStart-offset)'; but since startOffset and
		// offset are constant, tokens' starting positions become:
		// 'newStartOffset+currentTokenStart' for one less subtraction operation.
		final int newStartOffset = startOffset - offset;

		this.currentTokenStart = offset;
		this.currentTokenType = startTokenType;

		// beginning:
		for (int i = offset; i < end; i++) {

			final char c = array[i];

			switch (this.currentTokenType) {

			case TokenTypes.NULL:

				this.currentTokenStart = i; // Starting a new token here.

				switch (c) {

				case ' ':
				case '\t':
					this.currentTokenType = TokenTypes.WHITESPACE;
					break;

				case '"':
					this.currentTokenType = TokenTypes.ERROR_STRING_DOUBLE;
					break;

				case '%':
					this.currentTokenType = TokenTypes.VARIABLE;
					break;

				// The "separators".
				case '(':
				case ')':
					this.addToken(text, this.currentTokenStart, i, TokenTypes.SEPARATOR,
							newStartOffset + this.currentTokenStart);
					this.currentTokenType = TokenTypes.NULL;
					break;

				// The "separators2".
				case ',':
				case ';':
					this.addToken(text, this.currentTokenStart, i, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.currentTokenType = TokenTypes.NULL;
					break;

				// Newer version of EOL comments, or a label
				case ':':
					// If this will be the first token added, it is
					// a new-style comment or a label
					if (this.firstToken == null) {
						if (i < end - 1 && array[i + 1] == ':')
							this.currentTokenType = TokenTypes.COMMENT_EOL;
						else
							this.currentTokenType = TokenTypes.PREPROCESSOR;
					} else
						this.currentTokenType = TokenTypes.IDENTIFIER;
					break;

				default:

					// Just to speed things up a tad, as this will usually be the case (if spaces
					// above failed).
					if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\') {
						this.currentTokenType = TokenTypes.IDENTIFIER;
						break;
					}

					final int indexOf = this.operators.indexOf(c, 0);
					if (indexOf > -1) {
						this.addToken(text, this.currentTokenStart, i, TokenTypes.OPERATOR,
								newStartOffset + this.currentTokenStart);
						this.currentTokenType = TokenTypes.NULL;
						break;
					} else {
						this.currentTokenType = TokenTypes.IDENTIFIER;
						break;
					}

				} // End of switch (c).

				break;

			case TokenTypes.WHITESPACE:

				switch (c) {

				case ' ':
				case '\t':
					break; // Still whitespace.

				case '"':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					this.currentTokenType = TokenTypes.ERROR_STRING_DOUBLE;
					break;

				case '%':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					this.currentTokenType = TokenTypes.VARIABLE;
					break;

				// The "separators".
				case '(':
				case ')':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.addToken(text, i, i, TokenTypes.SEPARATOR, newStartOffset + i);
					this.currentTokenType = TokenTypes.NULL;
					break;

				// The "separators2".
				case ',':
				case ';':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.addToken(text, i, i, TokenTypes.IDENTIFIER, newStartOffset + i);
					this.currentTokenType = TokenTypes.NULL;
					break;

				// Newer version of EOL comments, or a label
				case ':':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					// If the previous (whitespace) token was the first token
					// added, this is a new-style comment or a label
					if (this.firstToken.getNextToken() == null) {
						if (i < end - 1 && array[i + 1] == ':')
							this.currentTokenType = TokenTypes.COMMENT_EOL;
						else
							this.currentTokenType = TokenTypes.PREPROCESSOR;
					} else
						this.currentTokenType = TokenTypes.IDENTIFIER;
					break;

				default: // Add the whitespace token and start anew.

					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.WHITESPACE,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;

					// Just to speed things up a tad, as this will usually be the case (if spaces
					// above failed).
					if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\') {
						this.currentTokenType = TokenTypes.IDENTIFIER;
						break;
					}

					final int indexOf = this.operators.indexOf(c, 0);
					if (indexOf > -1) {
						this.addToken(text, this.currentTokenStart, i, TokenTypes.OPERATOR,
								newStartOffset + this.currentTokenStart);
						this.currentTokenType = TokenTypes.NULL;
						break;
					} else
						this.currentTokenType = TokenTypes.IDENTIFIER;

				} // End of switch (c).

				break;

			default: // Should never happen
			case TokenTypes.IDENTIFIER:

				switch (c) {

				case ' ':
				case '\t':
					// Check for REM comments.
					if (i - this.currentTokenStart == 3 && (array[i - 3] == 'r' || array[i - 3] == 'R')
							&& (array[i - 2] == 'e' || array[i - 2] == 'E')
							&& (array[i - 1] == 'm' || array[i - 1] == 'M')) {
						this.currentTokenType = TokenTypes.COMMENT_EOL;
						break;
					}
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					this.currentTokenType = TokenTypes.WHITESPACE;
					break;

				case '"':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					this.currentTokenType = TokenTypes.ERROR_STRING_DOUBLE;
					break;

				case '%':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i;
					this.currentTokenType = TokenTypes.VARIABLE;
					break;

				// Should be part of identifiers, but not at end of "REM".
				case '\\':
					// Check for REM comments.
					if (i - this.currentTokenStart == 3 && (array[i - 3] == 'r' || array[i - 3] == 'R')
							&& (array[i - 2] == 'e' || array[i - 2] == 'E')
							&& (array[i - 1] == 'm' || array[i - 1] == 'M'))
						this.currentTokenType = TokenTypes.COMMENT_EOL;
					break;

				case '.':
				case '_':
					break; // Characters good for identifiers.

				// The "separators".
				case '(':
				case ')':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.addToken(text, i, i, TokenTypes.SEPARATOR, newStartOffset + i);
					this.currentTokenType = TokenTypes.NULL;
					break;

				// The "separators2".
				case ',':
				case ';':
					this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
							newStartOffset + this.currentTokenStart);
					this.addToken(text, i, i, TokenTypes.IDENTIFIER, newStartOffset + i);
					this.currentTokenType = TokenTypes.NULL;
					break;

				default:

					// Just to speed things up a tad, as this will usually be the case.
					if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\')
						break;

					final int indexOf = this.operators.indexOf(c);
					if (indexOf > -1) {
						this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.IDENTIFIER,
								newStartOffset + this.currentTokenStart);
						this.addToken(text, i, i, TokenTypes.OPERATOR, newStartOffset + i);
						this.currentTokenType = TokenTypes.NULL;
						break;
					}

					// Otherwise, fall through and assume we're still okay as an IDENTIFIER...

				} // End of switch (c).

				break;

			case TokenTypes.COMMENT_EOL:
				i = end - 1;
				this.addToken(text, this.currentTokenStart, i, TokenTypes.COMMENT_EOL,
						newStartOffset + this.currentTokenStart);
				// We need to set token type to null so at the bottom we don't add one more
				// token.
				this.currentTokenType = TokenTypes.NULL;
				break;

			case TokenTypes.PREPROCESSOR: // Used for labels
				i = end - 1;
				this.addToken(text, this.currentTokenStart, i, TokenTypes.PREPROCESSOR,
						newStartOffset + this.currentTokenStart);
				// We need to set token type to null so at the bottom we don't add one more
				// token.
				this.currentTokenType = TokenTypes.NULL;
				break;

			case TokenTypes.ERROR_STRING_DOUBLE:

				if (c == '"') {
					this.addToken(text, this.currentTokenStart, i, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE,
							newStartOffset + this.currentTokenStart);
					this.currentTokenStart = i + 1;
					this.currentTokenType = TokenTypes.NULL;
				}
				// Otherwise, we're still an unclosed string...

				break;

			case TokenTypes.VARIABLE:

				if (i == this.currentTokenStart + 1) { // first character after '%'.
					this.varType = VariableType.NORMAL_VAR;
					switch (c) {
					case '{':
						this.varType = VariableType.BRACKET_VAR;
						break;
					case '~':
						this.varType = VariableType.TILDE_VAR;
						break;
					case '%':
						this.varType = VariableType.DOUBLE_PERCENT_VAR;
						break;
					default:
						if (RSyntaxUtilities.isLetter(c) || c == '_' || c == ' ')
							// okay in variable names.
							break;
						else if (RSyntaxUtilities.isDigit(c)) { // Single-digit command-line argument ("%1").
							this.addToken(text, this.currentTokenStart, i, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart);
							this.currentTokenType = TokenTypes.NULL;
							break;
						} else { // Anything else, ???.
							this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart); // ???
							i--;
							this.currentTokenType = TokenTypes.NULL;
							break;
						}
					} // End of switch (c).
				} else
					switch (this.varType) {
					case BRACKET_VAR:
						if (c == '}') {
							this.addToken(text, this.currentTokenStart, i, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart);
							this.currentTokenType = TokenTypes.NULL;
						}
						break;
					case TILDE_VAR:
						if (!RSyntaxUtilities.isLetterOrDigit(c)) {
							this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart);
							i--;
							this.currentTokenType = TokenTypes.NULL;
						}
						break;
					case DOUBLE_PERCENT_VAR:
						// Can be terminated with "%%", or (essentially) a space.
						// substring chars are valid
						if (c == '%') {
							if (i < end - 1 && array[i + 1] == '%') {
								i++;
								this.addToken(text, this.currentTokenStart, i, TokenTypes.VARIABLE,
										newStartOffset + this.currentTokenStart);
								this.currentTokenType = TokenTypes.NULL;
							}
						} else if (!RSyntaxUtilities.isLetterOrDigit(c) && c != ':' && c != '~' && c != ','
								&& c != '-') {
							this.addToken(text, this.currentTokenStart, i - 1, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart);
							this.currentTokenType = TokenTypes.NULL;
							i--;
						}
						break;
					default:
						if (c == '%') {
							this.addToken(text, this.currentTokenStart, i, TokenTypes.VARIABLE,
									newStartOffset + this.currentTokenStart);
							this.currentTokenType = TokenTypes.NULL;
						}
						break;
					}
				break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		// Deal with the (possibly there) last token.
		if (this.currentTokenType != TokenTypes.NULL) {

			// Check for REM comments.
			if (end - this.currentTokenStart == 3 && (array[end - 3] == 'r' || array[end - 3] == 'R')
					&& (array[end - 2] == 'e' || array[end - 2] == 'E')
					&& (array[end - 1] == 'm' || array[end - 1] == 'M'))
				this.currentTokenType = TokenTypes.COMMENT_EOL;

			this.addToken(text, this.currentTokenStart, end - 1, this.currentTokenType,
					newStartOffset + this.currentTokenStart);
		}

		this.addNullToken();

		// Return the first token in our linked list.
		return this.firstToken;

	}

	/**
	 * Returns the words to highlight for Windows batch files.
	 *
	 * @return A <code>TokenMap</code> containing the words to highlight for Windows
	 *         batch files.
	 * @see org.fife.ui.rsyntaxtextarea.AbstractTokenMaker#getWordsToHighlight
	 */
	@Override
	public TokenMap getWordsToHighlight() {

		final TokenMap tokenMap = new TokenMap(true); // Ignore case.
		final int reservedWord = TokenTypes.RESERVED_WORD;

		// Batch-file specific stuff (?)
		tokenMap.put("goto", reservedWord);
		tokenMap.put("if", reservedWord);
		tokenMap.put("shift", reservedWord);
		tokenMap.put("start", reservedWord);

		// General command line stuff
		tokenMap.put("ansi.sys", reservedWord);
		tokenMap.put("append", reservedWord);
		tokenMap.put("arp", reservedWord);
		tokenMap.put("assign", reservedWord);
		tokenMap.put("assoc", reservedWord);
		tokenMap.put("at", reservedWord);
		tokenMap.put("attrib", reservedWord);
		tokenMap.put("break", reservedWord);
		tokenMap.put("cacls", reservedWord);
		tokenMap.put("call", reservedWord);
		tokenMap.put("cd", reservedWord);
		tokenMap.put("chcp", reservedWord);
		tokenMap.put("chdir", reservedWord);
		tokenMap.put("chkdsk", reservedWord);
		tokenMap.put("chknfts", reservedWord);
		tokenMap.put("choice", reservedWord);
		tokenMap.put("cls", reservedWord);
		tokenMap.put("cmd", reservedWord);
		tokenMap.put("color", reservedWord);
		tokenMap.put("comp", reservedWord);
		tokenMap.put("compact", reservedWord);
		tokenMap.put("control", reservedWord);
		tokenMap.put("convert", reservedWord);
		tokenMap.put("copy", reservedWord);
		tokenMap.put("ctty", reservedWord);
		tokenMap.put("date", reservedWord);
		tokenMap.put("debug", reservedWord);
		tokenMap.put("defrag", reservedWord);
		tokenMap.put("del", reservedWord);
		tokenMap.put("deltree", reservedWord);
		tokenMap.put("dir", reservedWord);
		tokenMap.put("diskcomp", reservedWord);
		tokenMap.put("diskcopy", reservedWord);
		tokenMap.put("do", reservedWord);
		tokenMap.put("doskey", reservedWord);
		tokenMap.put("dosshell", reservedWord);
		tokenMap.put("drivparm", reservedWord);
		tokenMap.put("echo", reservedWord);
		tokenMap.put("edit", reservedWord);
		tokenMap.put("edlin", reservedWord);
		tokenMap.put("emm386", reservedWord);
		tokenMap.put("erase", reservedWord);
		tokenMap.put("exist", reservedWord);
		tokenMap.put("exit", reservedWord);
		tokenMap.put("expand", reservedWord);
		tokenMap.put("extract", reservedWord);
		tokenMap.put("fasthelp", reservedWord);
		tokenMap.put("fc", reservedWord);
		tokenMap.put("fdisk", reservedWord);
		tokenMap.put("find", reservedWord);
		tokenMap.put("for", reservedWord);
		tokenMap.put("format", reservedWord);
		tokenMap.put("ftp", reservedWord);
		tokenMap.put("graftabl", reservedWord);
		tokenMap.put("help", reservedWord);
		tokenMap.put("ifshlp.sys", reservedWord);
		tokenMap.put("in", reservedWord);
		tokenMap.put("ipconfig", reservedWord);
		tokenMap.put("keyb", reservedWord);
		tokenMap.put("kill", reservedWord);
		tokenMap.put("label", reservedWord);
		tokenMap.put("lh", reservedWord);
		tokenMap.put("loadfix", reservedWord);
		tokenMap.put("loadhigh", reservedWord);
		tokenMap.put("lock", reservedWord);
		tokenMap.put("md", reservedWord);
		tokenMap.put("mem", reservedWord);
		tokenMap.put("mkdir", reservedWord);
		tokenMap.put("mklink", reservedWord);
		tokenMap.put("mode", reservedWord);
		tokenMap.put("more", reservedWord);
		tokenMap.put("move", reservedWord);
		tokenMap.put("msav", reservedWord);
		tokenMap.put("msd", reservedWord);
		tokenMap.put("mscdex", reservedWord);
		tokenMap.put("nbtstat", reservedWord);
		tokenMap.put("net", reservedWord);
		tokenMap.put("netstat", reservedWord);
		tokenMap.put("nlsfunc", reservedWord);
		tokenMap.put("not", reservedWord);
		tokenMap.put("nslookup", reservedWord);
		tokenMap.put("path", reservedWord);
		tokenMap.put("pathping", reservedWord);
		tokenMap.put("pause", reservedWord);
		tokenMap.put("ping", reservedWord);
		tokenMap.put("power", reservedWord);
		tokenMap.put("print", reservedWord);
		tokenMap.put("prompt", reservedWord);
		tokenMap.put("pushd", reservedWord);
		tokenMap.put("popd", reservedWord);
		tokenMap.put("qbasic", reservedWord);
		tokenMap.put("rd", reservedWord);
		tokenMap.put("ren", reservedWord);
		tokenMap.put("rename", reservedWord);
		tokenMap.put("rmdir", reservedWord);
		tokenMap.put("route", reservedWord);
		tokenMap.put("sc", reservedWord);
		tokenMap.put("scandisk", reservedWord);
		tokenMap.put("scandreg", reservedWord);
		tokenMap.put("set", reservedWord);
		tokenMap.put("setx", reservedWord);
		tokenMap.put("setver", reservedWord);
		tokenMap.put("share", reservedWord);
		tokenMap.put("shutdown", reservedWord);
		tokenMap.put("smartdrv", reservedWord);
		tokenMap.put("sort", reservedWord);
		tokenMap.put("subset", reservedWord);
		tokenMap.put("switches", reservedWord);
		tokenMap.put("sys", reservedWord);
		tokenMap.put("time", reservedWord);
		tokenMap.put("tracert", reservedWord);
		tokenMap.put("tree", reservedWord);
		tokenMap.put("type", reservedWord);
		tokenMap.put("undelete", reservedWord);
		tokenMap.put("unformat", reservedWord);
		tokenMap.put("unlock", reservedWord);
		tokenMap.put("ver", reservedWord);
		tokenMap.put("verify", reservedWord);
		tokenMap.put("vol", reservedWord);
		tokenMap.put("xcopy", reservedWord);

		return tokenMap;

	}

}