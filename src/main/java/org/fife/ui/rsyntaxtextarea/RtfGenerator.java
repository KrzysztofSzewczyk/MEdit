/*
 * 07/28/2008
 *
 * RtfGenerator.java - Generates RTF via a simple Java API.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import org.fife.ui.rtextarea.RTextAreaBase;

/**
 * Generates RTF text via a simple Java API.
 * <p>
 *
 * The following RTF features are supported:
 * <ul>
 * <li>Fonts
 * <li>Font sizes
 * <li>Foreground and background colors
 * <li>Bold, italic, and underline
 * </ul>
 *
 * The RTF generated isn't really "optimized," but it will do, especially for
 * small amounts of text, such as what's common when copy-and-pasting. It tries
 * to be sufficient for the use case of copying syntax highlighted code:
 * <ul>
 * <li>It assumes that tokens changing foreground color often is fairly common.
 * <li>It assumes that background highlighting is fairly uncommon.
 * </ul>
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RtfGenerator {

	/**
	 * The default font size for RTF. This is point size, in half points.
	 */
	private static final int DEFAULT_FONT_SIZE = 12;// 24;

	/**
	 * Returns the index of the specified item in a list. If the item is not in the
	 * list, it is added, and its new index is returned.
	 *
	 * @param list
	 *            The list (possibly) containing the item.
	 * @param item
	 *            The item to get the index of.
	 * @return The index of the item.
	 */
	private static int getColorIndex(final List<Color> list, final Color item) {
		int pos = list.indexOf(item);
		if (pos == -1) {
			list.add(item);
			pos = list.size() - 1;
		}
		return pos;
	}

	/**
	 * Returns the index of the specified font in a list of fonts. This method only
	 * checks for a font by its family name; its attributes such as bold and italic
	 * are ignored.
	 * <p>
	 *
	 * If the font is not in the list, it is added, and its new index is returned.
	 *
	 * @param list
	 *            The list (possibly) containing the font.
	 * @param font
	 *            The font to get the index of.
	 * @return The index of the font.
	 */
	private static int getFontIndex(final List<Font> list, final Font font) {
		final String fontName = font.getFamily();
		for (int i = 0; i < list.size(); i++) {
			final Font font2 = list.get(i);
			if (font2.getFamily().equals(fontName))
				return i;
		}
		list.add(font);
		return list.size() - 1;
	}

	/**
	 * Returns a good "default" monospaced font to use when Java's logical font
	 * "Monospaced" is found.
	 *
	 * @return The monospaced font family to use.
	 */
	private static String getMonospacedFontFamily() {
		String family = RTextAreaBase.getDefaultFont().getFamily();
		if ("Monospaced".equals(family))
			family = "Courier";
		return family;
	}

	private final List<Color> colorList;
	private final StringBuilder document;
	private final List<Font> fontList;
	private boolean lastBold;
	private int lastFGIndex;

	private int lastFontIndex;

	private int lastFontSize;

	private boolean lastItalic;

	private boolean lastWasControlWord;

	/**
	 * Java2D assumes a 72 dpi screen resolution, but on Windows the screen
	 * resolution is either 96 dpi or 120 dpi, depending on your font display
	 * settings. This is an attempt to make the RTF generated match the size of
	 * what's displayed in the RSyntaxTextArea.
	 */
	private int screenRes;

	/**
	 * Constructor.
	 */
	public RtfGenerator() {
		this.fontList = new ArrayList<>(1); // Usually only 1.
		this.colorList = new ArrayList<>(1); // Usually only 1.
		this.document = new StringBuilder();
		this.reset();
	}

	/**
	 * Adds a newline to the RTF document.
	 *
	 * @see #appendToDoc(String, Font, Color, Color)
	 */
	public void appendNewline() {
		this.document.append("\\par");
		this.document.append('\n'); // Just for ease of reading RTF.
		this.lastWasControlWord = false;
	}

	/**
	 * Appends styled text to the RTF document being generated.
	 *
	 * @param text
	 *            The text to append.
	 * @param f
	 *            The font of the text. If this is <code>null</code>, the default
	 *            font is used.
	 * @param fg
	 *            The foreground of the text. If this is <code>null</code>, the
	 *            default foreground color is used.
	 * @param bg
	 *            The background color of the text. If this is <code>null</code>,
	 *            the default background color is used.
	 * @see #appendNewline()
	 */
	public void appendToDoc(final String text, final Font f, final Color fg, final Color bg) {
		this.appendToDoc(text, f, fg, bg, false);
	}

	/**
	 * Appends styled text to the RTF document being generated.
	 *
	 * @param text
	 *            The text to append.
	 * @param f
	 *            The font of the text. If this is <code>null</code>, the default
	 *            font is used.
	 * @param fg
	 *            The foreground of the text. If this is <code>null</code>, the
	 *            default foreground color is used.
	 * @param bg
	 *            The background color of the text. If this is <code>null</code>,
	 *            the default background color is used.
	 * @param underline
	 *            Whether the text should be underlined.
	 * @see #appendNewline()
	 */
	public void appendToDoc(final String text, final Font f, final Color fg, final Color bg, final boolean underline) {
		this.appendToDoc(text, f, fg, bg, underline, true);
	}

	/**
	 * Appends styled text to the RTF document being generated.
	 *
	 * @param text
	 *            The text to append.
	 * @param f
	 *            The font of the text. If this is <code>null</code>, the default
	 *            font is used.
	 * @param fg
	 *            The foreground of the text. If this is <code>null</code>, the
	 *            default foreground color is used.
	 * @param bg
	 *            The background color of the text. If this is <code>null</code>,
	 *            the default background color is used.
	 * @param underline
	 *            Whether the text should be underlined.
	 * @param setFG
	 *            Whether the foreground specified by <code>fg</code> should be
	 *            honored (if it is non-<code>null</code>).
	 * @see #appendNewline()
	 */
	public void appendToDoc(final String text, final Font f, final Color fg, final Color bg, final boolean underline,
			final boolean setFG) {

		if (text != null) {

			// Set font to use, if different from last addition.
			final int fontIndex = f == null ? 0 : RtfGenerator.getFontIndex(this.fontList, f) + 1;
			if (fontIndex != this.lastFontIndex) {
				this.document.append("\\f").append(fontIndex);
				this.lastFontIndex = fontIndex;
				this.lastWasControlWord = true;
			}

			// Set styles to use.
			if (f != null) {
				final int fontSize = this.fixFontSize(f.getSize2D() * 2); // Half points!
				if (fontSize != this.lastFontSize) {
					this.document.append("\\fs").append(fontSize);
					this.lastFontSize = fontSize;
					this.lastWasControlWord = true;
				}
				if (f.isBold() != this.lastBold) {
					this.document.append(this.lastBold ? "\\b0" : "\\b");
					this.lastBold = !this.lastBold;
					this.lastWasControlWord = true;
				}
				if (f.isItalic() != this.lastItalic) {
					this.document.append(this.lastItalic ? "\\i0" : "\\i");
					this.lastItalic = !this.lastItalic;
					this.lastWasControlWord = true;
				}
			} else { // No font specified - assume neither bold nor italic.
				if (this.lastFontSize != RtfGenerator.DEFAULT_FONT_SIZE) {
					this.document.append("\\fs").append(RtfGenerator.DEFAULT_FONT_SIZE);
					this.lastFontSize = RtfGenerator.DEFAULT_FONT_SIZE;
					this.lastWasControlWord = true;
				}
				if (this.lastBold) {
					this.document.append("\\b0");
					this.lastBold = false;
					this.lastWasControlWord = true;
				}
				if (this.lastItalic) {
					this.document.append("\\i0");
					this.lastItalic = false;
					this.lastWasControlWord = true;
				}
			}
			if (underline) {
				this.document.append("\\ul");
				this.lastWasControlWord = true;
			}

			// Set the foreground color.
			if (setFG) {
				int fgIndex = 0;
				if (fg != null)
					fgIndex = RtfGenerator.getColorIndex(this.colorList, fg) + 1;
				if (fgIndex != this.lastFGIndex) {
					this.document.append("\\cf").append(fgIndex);
					this.lastFGIndex = fgIndex;
					this.lastWasControlWord = true;
				}
			}

			// Set the background color.
			if (bg != null) {
				final int pos = RtfGenerator.getColorIndex(this.colorList, bg);
				this.document.append("\\highlight").append(pos + 1);
				this.lastWasControlWord = true;
			}

			if (this.lastWasControlWord) {
				this.document.append(' '); // Delimiter
				this.lastWasControlWord = false;
			}
			this.escapeAndAdd(this.document, text);

			// Reset everything that was set for this text fragment.
			if (bg != null) {
				this.document.append("\\highlight0");
				this.lastWasControlWord = true;
			}
			if (underline) {
				this.document.append("\\ul0");
				this.lastWasControlWord = true;
			}

		}

	}

	/**
	 * Appends styled text to the RTF document being generated.
	 *
	 * @param text
	 *            The text to append.
	 * @param f
	 *            The font of the text. If this is <code>null</code>, the default
	 *            font is used.
	 * @param bg
	 *            The background color of the text. If this is <code>null</code>,
	 *            the default background color is used.
	 * @param underline
	 *            Whether the text should be underlined.
	 * @see #appendNewline()
	 */
	public void appendToDocNoFG(final String text, final Font f, final Color bg, final boolean underline) {
		this.appendToDoc(text, f, null, bg, underline, false);
	}

	/**
	 * Appends some text to a buffer, with special care taken for special characters
	 * as defined by the RTF spec.
	 *
	 * <ul>
	 * <li>All tab characters are replaced with the string "<code>\tab</code>"
	 * <li>'\', '{' and '}' are changed to "\\", "\{" and "\}"
	 * </ul>
	 *
	 * @param text
	 *            The text to append (with tab chars substituted).
	 * @param sb
	 *            The buffer to append to.
	 */
	private void escapeAndAdd(final StringBuilder sb, final String text) {
		final int count = text.length();
		for (int i = 0; i < count; i++) {
			final char ch = text.charAt(i);
			switch (ch) {
			case '\t':
				// Micro-optimization: for syntax highlighting with
				// tab indentation, there are often multiple tabs
				// back-to-back at the start of lines, so don't put
				// spaces between each "\tab".
				sb.append("\\tab");
				while (++i < count && text.charAt(i) == '\t')
					sb.append("\\tab");
				sb.append(' ');
				i--; // We read one too far.
				break;
			case '\\':
			case '{':
			case '}':
				sb.append('\\').append(ch);
				break;
			default:
				if (ch <= 127)
					sb.append(ch);
				else
					sb.append("\\u").append((int) ch);
				break;
			}
		}
	}

	/**
	 * Returns a font point size, adjusted for the current screen resolution.
	 * <p>
	 *
	 * Java2D assumes 72 dpi. On systems with larger dpi (Windows, GTK, etc.), font
	 * rendering will appear too small if we simply return a Java "Font" object's
	 * getSize() value. We need to adjust it for the screen resolution.
	 *
	 * @param pointSize
	 *            A Java Font's point size, as returned from
	 *            <code>getSize2D()</code>.
	 * @return The font point size, adjusted for the current screen resolution. This
	 *         will allow other applications to render fonts the same size as they
	 *         appear in the Java application.
	 */
	private int fixFontSize(float pointSize) {
		if (this.screenRes != 72)
			pointSize = Math.round(pointSize * 72f / this.screenRes);
		return (int) pointSize;
	}

	private String getColorTableRtf() {

		// Example:
		// "{\\colortbl ;\\red255\\green0\\blue0;\\red0\\green0\\blue255; }"

		final StringBuilder sb = new StringBuilder();

		sb.append("{\\colortbl ;");
		for (final Color c : this.colorList) {
			sb.append("\\red").append(c.getRed());
			sb.append("\\green").append(c.getGreen());
			sb.append("\\blue").append(c.getBlue());
			sb.append(';');
		}
		sb.append("}");

		return sb.toString();

	}

	private String getFontTableRtf() {

		// Example:
		// "{\\fonttbl{\\f0\\fmodern\\fcharset0 Courier;}}"

		final StringBuilder sb = new StringBuilder();

		// Workaround for text areas using the Java logical font "Monospaced"
		// by default. There's no way to know what it's mapped to, so we
		// just search for a monospaced font on the system.
		final String monoFamilyName = RtfGenerator.getMonospacedFontFamily();

		sb.append("{\\fonttbl{\\f0\\fnil\\fcharset0 " + monoFamilyName + ";}");
		for (int i = 0; i < this.fontList.size(); i++) {
			final Font f = this.fontList.get(i);
			String familyName = f.getFamily();
			if (familyName.equals("Monospaced"))
				familyName = monoFamilyName;
			sb.append("{\\f").append(i + 1).append("\\fnil\\fcharset0 ");
			sb.append(familyName).append(";}");
		}
		sb.append('}');

		return sb.toString();

	}

	/**
	 * Returns the RTF document created by this generator.
	 *
	 * @return The RTF document, as a <code>String</code>.
	 */
	public String getRtf() {

		final StringBuilder sb = new StringBuilder();
		sb.append("{");

		// Header
		sb.append("\\rtf1\\ansi\\ansicpg1252");
		sb.append("\\deff0"); // First font in font table is the default
		sb.append("\\deflang1033");
		sb.append("\\viewkind4"); // "Normal" view
		sb.append("\\uc\\pard\\f0");
		sb.append("\\fs20"); // Font size in half-points (default 24)
		sb.append(this.getFontTableRtf()).append('\n');
		sb.append(this.getColorTableRtf()).append('\n');

		// Content
		sb.append(this.document);

		sb.append("}");

		// System.err.println("*** " + sb.length());
		return sb.toString();

	}

	/**
	 * Resets this generator. All document information and content is cleared.
	 */
	public void reset() {
		this.fontList.clear();
		this.colorList.clear();
		this.document.setLength(0);
		this.lastWasControlWord = false;
		this.lastFontIndex = 0;
		this.lastFGIndex = 0;
		this.lastBold = false;
		this.lastItalic = false;
		this.lastFontSize = RtfGenerator.DEFAULT_FONT_SIZE;
		this.screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
	}

}