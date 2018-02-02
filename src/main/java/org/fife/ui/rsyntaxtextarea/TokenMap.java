/*
 * 08/26/2004
 *
 * TokenMap.java - Similar to a Map in Java, only designed specifically for
 * org.fife.ui.rsyntaxtextarea.Tokens.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.Segment;

/**
 * A hash table for reserved words, etc. defined by a {@link TokenMaker}. This
 * class is designed for the quick lookup of tokens, as it can compare
 * <code>Segment</code>s without the need to allocate a new string.
 * <p>
 *
 * The <code>org.fife.ui.rsyntaxtextarea</code> package uses this class to help
 * identify reserved words in programming languages. An instance of
 * {@link TokenMaker} will create and initialize an instance of this class
 * containing all reserved words, data types, and all other words that need to
 * be syntax-highlighted for that particular language. When the token maker
 * parses a line and identifies an individual token, it is looked up in the
 * <code>TokenMap</code> to see if it should be syntax-highlighted.
 *
 * @author Robert Futrell
 * @version 0.6
 */
public class TokenMap {

	/**
	 * The "token" used by a token map. Note that this isn't the same thing as the
	 * {@link Token} class, but it's basically a 1-1 correspondence for reserved
	 * words, etc.
	 */
	private static final class TokenMapToken {

		private final int length;
		private TokenMapToken nextToken;
		private final int offset;
		private final char[] text;
		private final int tokenType;

		private TokenMapToken(final char[] text, final int tokenType) {
			this.text = text;
			this.offset = 0;
			this.length = text.length;
			this.tokenType = tokenType;
		}

		@Override
		public String toString() {
			return "[TokenMapToken: " + new String(this.text, this.offset, this.length) + "]";
		}

	}

	private static final int DEFAULT_TOKEN_MAP_SIZE = 52;
	private final boolean ignoreCase;

	private final int size;

	private final TokenMapToken[] tokenMap;

	/**
	 * Constructs a new token map that is case-sensitive.
	 */
	public TokenMap() {
		this(TokenMap.DEFAULT_TOKEN_MAP_SIZE);
	}

	/**
	 * Constructs a new token map.
	 *
	 * @param ignoreCase
	 *            Whether or not this token map should ignore case when comparing
	 *            tokens.
	 */
	public TokenMap(final boolean ignoreCase) {
		this(TokenMap.DEFAULT_TOKEN_MAP_SIZE, ignoreCase);
	}

	/**
	 * Constructs a new token map that is case-sensitive.
	 *
	 * @param size
	 *            The size of the token map.
	 */
	public TokenMap(final int size) {
		this(size, false);
	}

	/**
	 * Constructs a new token map.
	 *
	 * @param size
	 *            The size of the token map.
	 * @param ignoreCase
	 *            Whether or not this token map should ignore case when comparing
	 *            tokens.
	 */
	public TokenMap(final int size, final boolean ignoreCase) {
		this.size = size;
		this.tokenMap = new TokenMapToken[size];
		this.ignoreCase = ignoreCase;
	}

	/**
	 * Adds a token to a specified bucket in the token map.
	 *
	 * @param bucket
	 *            The bucket in which to add the token.
	 * @param token
	 *            The token to add.
	 */
	private void addTokenToBucket(final int bucket, final TokenMapToken token) {
		final TokenMapToken old = this.tokenMap[bucket];
		token.nextToken = old;
		this.tokenMap[bucket] = token;
	}

	/**
	 * Returns the token type associated with the given text, if the given text is
	 * in this token map. If it isn't, <code>-1</code> is returned.
	 *
	 * @param array1
	 *            An array of characters containing the text.
	 * @param start
	 *            The starting index in the array of the text.
	 * @param end
	 *            The ending index in the array of the text.
	 * @return The token type associated with the given text, or <code>-1</code> if
	 *         this token was not specified in this map.
	 */
	public int get(final char[] array1, final int start, final int end) {

		final int length1 = end - start + 1;

		final int hash = this.getHashCode(array1, start, length1);
		TokenMapToken token = this.tokenMap[hash];

		char[] array2;
		int offset2;
		int offset1;
		int length;

		/*
		 * We check whether or not to ignore case before doing any looping to minimize
		 * the number of extraneous comparisons we do. This makes for slightly redundant
		 * code, but it'll be a little more efficient.
		 */

		// If matches are case-sensitive (C, C++, Java, etc.)...
		if (!this.ignoreCase)
			mainLoop: while (token != null) {
				if (token.length == length1) {
					array2 = token.text;
					offset2 = token.offset;
					offset1 = start;
					length = length1;
					while (length-- > 0)
						if (array1[offset1++] != array2[offset2++]) {
							token = token.nextToken;
							continue mainLoop;
						}
					return token.tokenType;
				}
				token = token.nextToken;
			}
		else
			mainLoop2: while (token != null) {
				if (token.length == length1) {
					array2 = token.text;
					offset2 = token.offset;
					offset1 = start;
					length = length1;
					while (length-- > 0)
						if (RSyntaxUtilities.toLowerCase(array1[offset1++]) != array2[offset2++]) {
							token = token.nextToken;
							continue mainLoop2;
						}
					return token.tokenType;
				}
				token = token.nextToken;
			}

		// Didn't match any of the tokens in the bucket.
		return -1;

	}

	/**
	 * Returns the token type associated with the given text, if the given text is
	 * in this token map. If it isn't, <code>-1</code> is returned.
	 *
	 * @param text
	 *            The segment from which to get the text to compare.
	 * @param start
	 *            The starting index in the segment of the text.
	 * @param end
	 *            The ending index in the segment of the text.
	 * @return The token type associated with the given text, or <code>-1</code> if
	 *         this token was not specified in this map.
	 */
	public int get(final Segment text, final int start, final int end) {
		return this.get(text.array, start, end);
	}

	/**
	 * Returns the hash code for a given string.
	 *
	 * @param text
	 *            The text to hash.
	 * @param offset
	 *            The offset into the text at which to start hashing.
	 * @param length
	 *            The last character in the text to hash.
	 * @return The hash code.
	 */
	private int getHashCode(final char[] text, final int offset, final int length) {
		return (RSyntaxUtilities.toLowerCase(text[offset]) + RSyntaxUtilities.toLowerCase(text[offset + length - 1]))
				% this.size;
	}

	/**
	 * Returns whether this token map ignores case when checking for tokens. This
	 * property is set in the constructor and cannot be changed, as this is an
	 * intrinsic property of a particular programming language.
	 *
	 * @return Whether or not this token maker is ignoring case.
	 */
	protected boolean isIgnoringCase() {
		return this.ignoreCase;
	}

	/**
	 * Adds a string to this token map. The char array passed-in will be used as the
	 * actual data for the token, so it may well be modified (such as lower-casing
	 * it if <code>ignoreCase</code> is <code>true</code>). This shouldn't be an
	 * issue though as this method is only called from the public <code>put</code>
	 * method, which allocates a new char array.
	 *
	 * @param string
	 *            The string to add.
	 * @param tokenType
	 *            The type of token the string is.
	 */
	private void put(final char[] string, final int tokenType) {
		final int hashCode = this.getHashCode(string, 0, string.length);
		this.addTokenToBucket(hashCode, new TokenMapToken(string, tokenType));
	}

	/**
	 * Adds a string to this token map.
	 *
	 * @param string
	 *            The string to add.
	 * @param tokenType
	 *            The type of token the string is.
	 */
	public void put(final String string, final int tokenType) {
		if (this.isIgnoringCase())
			this.put(string.toLowerCase().toCharArray(), tokenType);
		else
			this.put(string.toCharArray(), tokenType);
	}

}