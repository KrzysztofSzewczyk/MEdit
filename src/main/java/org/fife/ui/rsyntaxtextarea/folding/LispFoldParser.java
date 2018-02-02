/*
 * 08/08/2012
 *
 * LispFoldParser.java - Fold parser for Lisp and related languages.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Fold parser for Lisp and related languages.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LispFoldParser extends CurlyFoldParser {

	@Override
	public boolean isLeftCurly(final Token t) {
		return t.isSingleChar(TokenTypes.SEPARATOR, '(');
	}

	@Override
	public boolean isRightCurly(final Token t) {
		return t.isSingleChar(TokenTypes.SEPARATOR, ')');
	}

}