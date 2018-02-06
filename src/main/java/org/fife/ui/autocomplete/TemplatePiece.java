/*
 * 06/17/2012
 *
 * TemplatePiece.java - A logical piece of a template completion.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

/**
 * A piece of a <code>TemplateCompletion</code>. You add instances of this class
 * to template completions to define them.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see TemplateCompletion
 */
interface TemplatePiece {

	public class Param implements TemplatePiece {

		String text;

		public Param(final String text) {
			this.text = text;
		}

		@Override
		public String getText() {
			return this.text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.Param: param=" + this.text + "]";
		}

	}

	public class ParamCopy implements TemplatePiece {

		private final String text;

		public ParamCopy(final String text) {
			this.text = text;
		}

		@Override
		public String getText() {
			return this.text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.ParamCopy: param=" + this.text + "]";
		}

	}

	public class Text implements TemplatePiece {

		private final String text;

		public Text(final String text) {
			this.text = text;
		}

		@Override
		public String getText() {
			return this.text;
		}

		@Override
		public String toString() {
			return "[TemplatePiece.Text: text=" + this.text + "]";
		}

	}

	String getText();

}