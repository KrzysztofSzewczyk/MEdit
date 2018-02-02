/*
 * 08/06/2004
 *
 * Style.java - A set of traits for a particular token type to use while
 * painting.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JPanel;

/**
 * The color and style information for a token type. Each token type in an
 * <code>RSyntaxTextArea</code> has a corresponding <code>Style</code>; this
 * <code>Style</code> tells us the following things:
 *
 * <ul>
 * <li>What foreground color to use for tokens of this type.</li>
 * <li>What background color to use.</li>
 * <li>The font to use.</li>
 * <li>Whether the token should be underlined.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 0.6
 */
@SuppressWarnings({ "checkstyle:visibilitymodifier" })
public class Style implements Cloneable {

	public static final Color DEFAULT_BACKGROUND = null;
	public static final Font DEFAULT_FONT = null;
	public static final Color DEFAULT_FOREGROUND = Color.BLACK;

	public Color background;
	public Font font;
	public FontMetrics fontMetrics;
	public Color foreground;

	public boolean underline;

	/**
	 * Creates a new style defaulting to black foreground, no background, and no
	 * styling.
	 */
	public Style() {
		this(Style.DEFAULT_FOREGROUND);
	}

	/**
	 * Creates a new style with the specified foreground and no styling.
	 *
	 * @param fg
	 *            The foreground color to use.
	 */
	public Style(final Color fg) {
		this(fg, Style.DEFAULT_BACKGROUND);
	}

	/**
	 * Creates a new style with the specified colors and no styling.
	 *
	 * @param fg
	 *            The foreground color to use.
	 * @param bg
	 *            The background color to use.
	 */
	public Style(final Color fg, final Color bg) {
		this(fg, bg, Style.DEFAULT_FONT);
	}

	/**
	 * Creates a new style.
	 *
	 * @param fg
	 *            The foreground color to use.
	 * @param bg
	 *            The background color to use.
	 * @param font
	 *            The font for this syntax scheme.
	 */
	public Style(final Color fg, final Color bg, final Font font) {
		this(fg, bg, font, false);
	}

	/**
	 * Creates a new style.
	 *
	 * @param fg
	 *            The foreground color to use.
	 * @param bg
	 *            The background color to use.
	 * @param font
	 *            The font for this syntax scheme.
	 * @param underline
	 *            Whether or not to underline tokens with this style.
	 */
	public Style(final Color fg, final Color bg, final Font font, final boolean underline) {
		this.foreground = fg;
		this.background = bg;
		this.font = font;
		this.underline = underline;
		this.fontMetrics = font == null ? null : new JPanel().getFontMetrics(font); // Default, no rendering hints!
	}

	/**
	 * Returns whether or not two (possibly <code>null</code>) objects are equal.
	 */
	private boolean areEqual(final Object o1, final Object o2) {
		return o1 == null && o2 == null || o1 != null && o1.equals(o2);
	}

	/**
	 * Returns a deep copy of this object.
	 *
	 * @return The copy.
	 */
	@Override
	public Object clone() {
		Style clone = null;
		try {
			clone = (Style) super.clone();
		} catch (final CloneNotSupportedException cnse) { // Never happens
			cnse.printStackTrace();
			return null;
		}
		clone.foreground = this.foreground;
		clone.background = this.background;
		clone.font = this.font;
		clone.underline = this.underline;
		clone.fontMetrics = this.fontMetrics;
		return clone;
	}

	/**
	 * Returns whether or not two syntax schemes are equal.
	 *
	 * @param o2
	 *            The object with which to compare this syntax scheme.
	 * @return Whether or not these two syntax schemes represent the same scheme.
	 */
	@Override
	public boolean equals(final Object o2) {
		if (o2 instanceof Style) {
			final Style ss2 = (Style) o2;
			if (this.underline == ss2.underline && this.areEqual(this.foreground, ss2.foreground)
					&& this.areEqual(this.background, ss2.background) && this.areEqual(this.font, ss2.font)
					&& this.areEqual(this.fontMetrics, ss2.fontMetrics))
				return true;
		}
		return false;
	}

	/**
	 * Computes the hash code to use when adding this syntax scheme to hash tables.
	 * <p>
	 *
	 * This method is implemented, since {@link #equals(Object)} is implemented, to
	 * keep FindBugs happy.
	 *
	 * @return The hash code.
	 */
	@Override
	public int hashCode() {
		int hashCode = this.underline ? 1 : 0;
		if (this.foreground != null)
			hashCode ^= this.foreground.hashCode();
		if (this.background != null)
			hashCode ^= this.background.hashCode();
		return hashCode;
	}

	/**
	 * Returns a string representation of this style.
	 *
	 * @return A string representation of this style.
	 */
	@Override
	public String toString() {
		return "[Style: foreground: " + this.foreground + ", background: " + this.background + ", underline: "
				+ this.underline + ", font: " + this.font + "]";
	}

}