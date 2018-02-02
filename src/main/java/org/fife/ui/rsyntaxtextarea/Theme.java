/*
 * 10/30/2011
 *
 * Theme.java - A color theme for RSyntaxTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.StyleContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.fife.io.UnicodeWriter;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextAreaBase;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A theme is a set of fonts and colors to use to style RSyntaxTextArea and
 * RTextScrollPane. Themes are defined in XML files that are validated against
 * <code>org/fife/ui/rsyntaxtextarea/themes/theme.dtd</code>. This provides
 * applications and other consumers with an easy way to style RSyntaxTextArea
 * without having to use the API.
 * <p>
 *
 * Sample themes are included in the source tree in the
 * <code>org.fife.ui.rsyntaxtextarea.themes</code> package, and can be loaded
 * via <code>getClass().getResourceAsStream(...)</code>.
 * <p>
 *
 * All fields are public to facilitate programmatic manipulation, but typically
 * you won't need to reference any fields directly, rather using the
 * <code>load()</code>, <code>save()</code>, and <code>apply()</code> methods
 * for various tasks.
 * <p>
 *
 * Note that to save a <code>Theme</code> via {@link #save(OutputStream)}, you
 * must currently create a <code>Theme</code> from a text area wrapped in an
 * <code>RTextScrollPane</code>, so that the color information for the gutter
 * can be retrieved.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class Theme {

	/**
	 * Loads a <code>SyntaxScheme</code> from an XML file.
	 */
	private static class XmlHandler extends DefaultHandler {

		public static void load(final Theme theme, final InputStream in) throws IOException {
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setValidating(true);
			try {
				final SAXParser parser = spf.newSAXParser();
				final XMLReader reader = parser.getXMLReader();
				final XmlHandler handler = new XmlHandler();
				handler.theme = theme;
				reader.setEntityResolver(handler);
				reader.setContentHandler(handler);
				reader.setDTDHandler(handler);
				reader.setErrorHandler(handler);
				final InputSource is = new InputSource(in);
				is.setEncoding("UTF-8");
				reader.parse(is);
			} catch (/* SAX|ParserConfiguration */final Exception se) {
				throw new IOException(se.toString());
			}
		}

		private static int parseInt(final Attributes attrs, final String attr, final int def) {
			int value = def;
			final String temp = attrs.getValue(attr);
			if (temp != null)
				try {
					value = Integer.parseInt(temp);
				} catch (final NumberFormatException nfe) {
					nfe.printStackTrace();
				}
			return value;
		}

		private Theme theme;

		@Override
		public void error(final SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void fatalError(final SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public InputSource resolveEntity(final String publicID, final String systemID) throws SAXException {
			return new InputSource(this.getClass().getResourceAsStream("themes/theme.dtd"));
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes attrs) {

			if ("background".equals(qName)) {

				final String color = attrs.getValue("color");
				if (color != null) {
					this.theme.bgColor = Theme.stringToColor(color, Theme.getDefaultBG());
					this.theme.gutterBackgroundColor = this.theme.bgColor;
				} else {
					final String img = attrs.getValue("image");
					if (img != null)
						throw new IllegalArgumentException("Not yet implemented");
				}
			}

			// The base font to use in the editor.
			else if ("baseFont".equals(qName)) {
				int size = this.theme.baseFont.getSize();
				final String sizeStr = attrs.getValue("size");
				if (sizeStr != null)
					size = Integer.parseInt(sizeStr);
				final String family = attrs.getValue("family");
				if (family != null)
					this.theme.baseFont = Theme.getFont(family, Font.PLAIN, size);
				else if (sizeStr != null)
					// No family specified, keep original family
					this.theme.baseFont = this.theme.baseFont.deriveFont(size * 1f);
			}

			else if ("caret".equals(qName)) {
				final String color = attrs.getValue("color");
				this.theme.caretColor = Theme.stringToColor(color);
			}

			else if ("currentLineHighlight".equals(qName)) {
				final String color = attrs.getValue("color");
				this.theme.currentLineHighlight = Theme.stringToColor(color);
				final String fadeStr = attrs.getValue("fade");
				final boolean fade = Boolean.parseBoolean(fadeStr);
				this.theme.fadeCurrentLineHighlight = fade;
			}

			else if ("foldIndicator".equals(qName)) {
				String color = attrs.getValue("fg");
				this.theme.foldIndicatorFG = Theme.stringToColor(color);
				color = attrs.getValue("iconBg");
				this.theme.foldBG = Theme.stringToColor(color);
				color = attrs.getValue("iconArmedBg");
				this.theme.armedFoldBG = Theme.stringToColor(color);
			}

			else if ("gutterBackground".equals(qName)) {
				final String color = attrs.getValue("color");
				if (color != null)
					this.theme.gutterBackgroundColor = Theme.stringToColor(color);
			}

			else if ("gutterBorder".equals(qName)) {
				final String color = attrs.getValue("color");
				this.theme.gutterBorderColor = Theme.stringToColor(color);
			}

			else if ("iconRowHeader".equals(qName)) {
				final String color = attrs.getValue("activeLineRange");
				this.theme.activeLineRangeColor = Theme.stringToColor(color);
				final String inheritBGStr = attrs.getValue("inheritsGutterBG");
				this.theme.iconRowHeaderInheritsGutterBG = inheritBGStr == null ? false : Boolean.valueOf(inheritBGStr);
			}

			else if ("lineNumbers".equals(qName)) {
				final String color = attrs.getValue("fg");
				this.theme.lineNumberColor = Theme.stringToColor(color);
				this.theme.lineNumberFont = attrs.getValue("fontFamily");
				this.theme.lineNumberFontSize = XmlHandler.parseInt(attrs, "fontSize", -1);
			}

			else if ("marginLine".equals(qName)) {
				final String color = attrs.getValue("fg");
				this.theme.marginLineColor = Theme.stringToColor(color);
			}

			else if ("markAllHighlight".equals(qName)) {
				final String color = attrs.getValue("color");
				this.theme.markAllHighlightColor = Theme.stringToColor(color);
			}

			else if ("markOccurrencesHighlight".equals(qName)) {
				final String color = attrs.getValue("color");
				this.theme.markOccurrencesColor = Theme.stringToColor(color);
				final String border = attrs.getValue("border");
				this.theme.markOccurrencesBorder = Boolean.parseBoolean(border);
			}

			else if ("matchedBracket".equals(qName)) {
				final String fg = attrs.getValue("fg");
				this.theme.matchedBracketFG = Theme.stringToColor(fg);
				final String bg = attrs.getValue("bg");
				this.theme.matchedBracketBG = Theme.stringToColor(bg);
				final String highlightBoth = attrs.getValue("highlightBoth");
				this.theme.matchedBracketHighlightBoth = Boolean.parseBoolean(highlightBoth);
				final String animate = attrs.getValue("animate");
				this.theme.matchedBracketAnimate = Boolean.parseBoolean(animate);
			}

			else if ("hyperlinks".equals(qName)) {
				final String fg = attrs.getValue("fg");
				this.theme.hyperlinkFG = Theme.stringToColor(fg);
			}

			else if ("language".equals(qName)) {
				final String indexStr = attrs.getValue("index");
				final int index = Integer.parseInt(indexStr) - 1;
				if (this.theme.secondaryLanguages.length > index) { // Sanity
					final Color bg = Theme.stringToColor(attrs.getValue("bg"));
					this.theme.secondaryLanguages[index] = bg;
				}
			}

			else if ("selection".equals(qName)) {
				final String useStr = attrs.getValue("useFG");
				this.theme.useSelctionFG = Boolean.parseBoolean(useStr);
				String color = attrs.getValue("fg");
				this.theme.selectionFG = Theme.stringToColor(color, Theme.getDefaultSelectionFG());
				// System.out.println(theme.selectionFG);
				color = attrs.getValue("bg");
				this.theme.selectionBG = Theme.stringToColor(color, Theme.getDefaultSelectionBG());
				final String roundedStr = attrs.getValue("roundedEdges");
				this.theme.selectionRoundedEdges = Boolean.parseBoolean(roundedStr);
			}

			// Start of the syntax scheme definition
			else if ("tokenStyles".equals(qName))
				this.theme.scheme = new SyntaxScheme(this.theme.baseFont, false);
			else if ("style".equals(qName)) {

				final String type = attrs.getValue("token");
				Field field = null;
				try {
					field = Token.class.getField(type);
				} catch (final RuntimeException re) {
					throw re; // FindBugs
				} catch (final Exception e) {
					System.err.println("Invalid token type: " + type);
					return;
				}

				if (field.getType() == int.class) {

					int index = 0;
					try {
						index = field.getInt(this.theme.scheme);
					} catch (final IllegalArgumentException e) {
						e.printStackTrace();
						return;
					} catch (final IllegalAccessException e) {
						e.printStackTrace();
						return;
					}

					final String fgStr = attrs.getValue("fg");
					final Color fg = Theme.stringToColor(fgStr);
					this.theme.scheme.getStyle(index).foreground = fg;

					final String bgStr = attrs.getValue("bg");
					final Color bg = Theme.stringToColor(bgStr);
					this.theme.scheme.getStyle(index).background = bg;

					Font font = this.theme.baseFont;
					final String familyName = attrs.getValue("fontFamily");
					if (familyName != null)
						font = Theme.getFont(familyName, font.getStyle(), font.getSize());
					final String sizeStr = attrs.getValue("fontSize");
					if (sizeStr != null)
						try {
							float size = Float.parseFloat(sizeStr);
							size = Math.max(size, 1f);
							font = font.deriveFont(size);
						} catch (final NumberFormatException nfe) {
							nfe.printStackTrace();
						}
					this.theme.scheme.getStyle(index).font = font;

					boolean styleSpecified = false;
					boolean bold = false;
					boolean italic = false;
					final String boldStr = attrs.getValue("bold");
					if (boldStr != null) {
						bold = Boolean.parseBoolean(boldStr);
						styleSpecified = true;
					}
					final String italicStr = attrs.getValue("italic");
					if (italicStr != null) {
						italic = Boolean.parseBoolean(italicStr);
						styleSpecified = true;
					}
					if (styleSpecified) {
						int style = 0;
						if (bold)
							style |= Font.BOLD;
						if (italic)
							style |= Font.ITALIC;
						final Font orig = this.theme.scheme.getStyle(index).font;
						this.theme.scheme.getStyle(index).font = orig.deriveFont(style);
					}

					final String ulineStr = attrs.getValue("underline");
					if (ulineStr != null) {
						final boolean uline = Boolean.parseBoolean(ulineStr);
						this.theme.scheme.getStyle(index).underline = uline;
					}

				}

			}

		}

		@Override
		public void warning(final SAXParseException e) throws SAXException {
			throw e;
		}

	}

	private static String colorToString(final Color c) {
		final int color = c.getRGB() & 0xffffff;
		final StringBuilder stringBuilder = new StringBuilder(Integer.toHexString(color));
		while (stringBuilder.length() < 6)
			stringBuilder.insert(0, "0");
		return stringBuilder.toString();
	}

	/**
	 * Returns the default selection background color to use if "default" is
	 * specified in a theme.
	 *
	 * @return The default selection background to use.
	 * @see #getDefaultSelectionFG()
	 */
	private static Color getDefaultBG() {
		Color c = UIManager.getColor("nimbusLightBackground");
		if (c == null) {
			// Don't search for "text", as Nimbus defines that as the text
			// component "text" color, but Basic LAFs use it for text
			// component backgrounds! Nimbus also defines TextArea.background
			// as too dark, not what it actually uses for text area backgrounds
			c = UIManager.getColor("TextArea.background");
			if (c == null)
				c = new ColorUIResource(SystemColor.text);
		}
		return c;
	}

	/**
	 * Returns the default selection background color to use if "default" is
	 * specified in a theme.
	 *
	 * @return The default selection background to use.
	 * @see #getDefaultSelectionFG()
	 */
	private static Color getDefaultSelectionBG() {
		Color c = UIManager.getColor("TextArea.selectionBackground");
		if (c == null) {
			c = UIManager.getColor("textHighlight");
			if (c == null) {
				c = UIManager.getColor("nimbusSelectionBackground");
				if (c == null)
					c = new ColorUIResource(SystemColor.textHighlight);
			}
		}
		return c;
	}

	/**
	 * Returns the default "selected text" color to use if "default" is specified in
	 * a theme.
	 *
	 * @return The default selection foreground color to use.
	 * @see #getDefaultSelectionBG()
	 */
	private static Color getDefaultSelectionFG() {
		Color c = UIManager.getColor("TextArea.selectionForeground");
		if (c == null) {
			c = UIManager.getColor("textHighlightText");
			if (c == null) {
				c = UIManager.getColor("nimbusSelectedText");
				if (c == null)
					c = new ColorUIResource(SystemColor.textHighlightText);
			}
		}
		return c;
	}

	/**
	 * Returns the specified font.
	 *
	 * @param family
	 *            The font family.
	 * @param style
	 *            The style of font.
	 * @param size
	 *            The size of the font.
	 * @return The font.
	 */
	private static Font getFont(final String family, final int style, final int size) {
		// Use StyleContext to get a composite font for Asian glyphs.
		final StyleContext sc = StyleContext.getDefaultStyleContext();
		return sc.getFont(family, style, size);
	}

	/**
	 * Loads a theme.
	 *
	 * @param in
	 *            The input stream to read from. This will be closed when this
	 *            method returns.
	 * @return The theme.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #save(OutputStream)
	 */
	public static Theme load(final InputStream in) throws IOException {
		return Theme.load(in, null);
	}

	/**
	 * Loads a theme.
	 *
	 * @param in
	 *            The input stream to read from. This will be closed when this
	 *            method returns.
	 * @param baseFont
	 *            The default font to use for any "base font" properties not
	 *            specified in the theme XML. If this is <code>null</code>, a
	 *            default monospaced font will be used.
	 * @return The theme.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #save(OutputStream)
	 */
	public static Theme load(final InputStream in, final Font baseFont) throws IOException {

		final Theme theme = new Theme(baseFont);

		final BufferedInputStream bin = new BufferedInputStream(in);
		try {
			XmlHandler.load(theme, bin);
		} finally {
			bin.close();
		}

		return theme;

	}

	/**
	 * Returns the color represented by a string. The input is expected to be a
	 * 6-digit hex string, optionally prefixed by a '$'. For example, either of the
	 * following:
	 *
	 * <pre>
	 * "$00ff00"
	 * "00ff00"
	 * </pre>
	 *
	 * will return <code>new Color(0, 255, 0)</code>.
	 *
	 * @param s
	 *            The string to evaluate.
	 * @return The color.
	 */
	private static Color stringToColor(final String s) {
		return Theme.stringToColor(s, null);
	}

	/**
	 * Returns the color represented by a string. The input is expected to be a
	 * 6-digit hex string, optionally prefixed by a '$'. For example, either of the
	 * following:
	 *
	 * <pre>
	 * "$00ff00"
	 * "00ff00"
	 * </pre>
	 *
	 * will return <code>new Color(0, 255, 0)</code>.
	 *
	 * @param s
	 *            The string to evaluate.
	 * @param defaultVal
	 *            The color to use if <code>s</code> is "<code>default</code>".
	 * @return The color.
	 */
	private static Color stringToColor(String s, final Color defaultVal) {
		if (s == null || "default".equalsIgnoreCase(s))
			return defaultVal;
		if (s.length() == 6 || s.length() == 7) {
			if (s.charAt(0) == '$')
				s = s.substring(1);
			return new Color(Integer.parseInt(s, 16));
		}
		return null;
	}

	public Color activeLineRangeColor;
	public Color armedFoldBG;
	public Font baseFont;
	public Color bgColor;
	public Color caretColor;
	public Color currentLineHighlight;
	public boolean fadeCurrentLineHighlight;
	public Color foldBG;
	public Color foldIndicatorFG;

	public Color gutterBackgroundColor;

	public Color gutterBorderColor;
	public Color hyperlinkFG;
	public boolean iconRowHeaderInheritsGutterBG;
	public Color lineNumberColor;
	public String lineNumberFont;
	public int lineNumberFontSize;
	public Color marginLineColor;
	public Color markAllHighlightColor;
	public boolean markOccurrencesBorder;
	public Color markOccurrencesColor;

	public boolean matchedBracketAnimate;

	public Color matchedBracketBG;

	public Color matchedBracketFG;

	public boolean matchedBracketHighlightBoth;

	public SyntaxScheme scheme;

	public Color[] secondaryLanguages;

	public Color selectionBG;

	public Color selectionFG;

	public boolean selectionRoundedEdges;

	public boolean useSelctionFG;

	/**
	 * Private constructor, used when loading from a stream.
	 *
	 * @param baseFont
	 *            The default font to use for any "base font" properties not
	 *            specified in the theme XML. If this is <code>null</code>, a
	 *            default monospaced font will be used.
	 */
	private Theme(final Font baseFont) {
		// Optional fields that require a default value.
		this.baseFont = baseFont != null ? baseFont : RTextAreaBase.getDefaultFont();
		this.secondaryLanguages = new Color[3];
		this.activeLineRangeColor = Gutter.DEFAULT_ACTIVE_LINE_RANGE_COLOR;
	}

	/**
	 * Creates a theme from an RSyntaxTextArea. It should be contained in an
	 * <code>RTextScrollPane</code> to get all gutter color information.
	 *
	 * @param textArea
	 *            The text area.
	 */
	public Theme(final RSyntaxTextArea textArea) {

		this.baseFont = textArea.getFont();
		this.bgColor = textArea.getBackground();
		this.caretColor = textArea.getCaretColor();
		this.useSelctionFG = textArea.getUseSelectedTextColor();
		this.selectionFG = textArea.getSelectedTextColor();
		this.selectionBG = textArea.getSelectionColor();
		this.selectionRoundedEdges = textArea.getRoundedSelectionEdges();
		this.currentLineHighlight = textArea.getCurrentLineHighlightColor();
		this.fadeCurrentLineHighlight = textArea.getFadeCurrentLineHighlight();
		this.marginLineColor = textArea.getMarginLineColor();
		this.markAllHighlightColor = textArea.getMarkAllHighlightColor();
		this.markOccurrencesColor = textArea.getMarkOccurrencesColor();
		this.markOccurrencesBorder = textArea.getPaintMarkOccurrencesBorder();
		this.matchedBracketBG = textArea.getMatchedBracketBGColor();
		this.matchedBracketFG = textArea.getMatchedBracketBorderColor();
		this.matchedBracketHighlightBoth = textArea.getPaintMatchedBracketPair();
		this.matchedBracketAnimate = textArea.getAnimateBracketMatching();
		this.hyperlinkFG = textArea.getHyperlinkForeground();

		final int count = textArea.getSecondaryLanguageCount();
		this.secondaryLanguages = new Color[count];
		for (int i = 0; i < count; i++)
			this.secondaryLanguages[i] = textArea.getSecondaryLanguageBackground(i + 1);

		this.scheme = textArea.getSyntaxScheme();

		final Gutter gutter = RSyntaxUtilities.getGutter(textArea);
		if (gutter != null) {
			this.gutterBackgroundColor = gutter.getBackground();
			this.gutterBorderColor = gutter.getBorderColor();
			this.activeLineRangeColor = gutter.getActiveLineRangeColor();
			this.iconRowHeaderInheritsGutterBG = gutter.getIconRowHeaderInheritsGutterBackground();
			this.lineNumberColor = gutter.getLineNumberColor();
			this.lineNumberFont = gutter.getLineNumberFont().getFamily();
			this.lineNumberFontSize = gutter.getLineNumberFont().getSize();
			this.foldIndicatorFG = gutter.getFoldIndicatorForeground();
			this.foldBG = gutter.getFoldBackground();
			this.armedFoldBG = gutter.getArmedFoldBackground();
		}

	}

	/**
	 * Applies this theme to a text area.
	 *
	 * @param textArea
	 *            The text area to apply this theme to.
	 */
	public void apply(final RSyntaxTextArea textArea) {

		textArea.setFont(this.baseFont);
		textArea.setBackground(this.bgColor);
		textArea.setCaretColor(this.caretColor);
		textArea.setUseSelectedTextColor(this.useSelctionFG);
		textArea.setSelectedTextColor(this.selectionFG);
		textArea.setSelectionColor(this.selectionBG);
		textArea.setRoundedSelectionEdges(this.selectionRoundedEdges);
		textArea.setCurrentLineHighlightColor(this.currentLineHighlight);
		textArea.setFadeCurrentLineHighlight(this.fadeCurrentLineHighlight);
		textArea.setMarginLineColor(this.marginLineColor);
		textArea.setMarkAllHighlightColor(this.markAllHighlightColor);
		textArea.setMarkOccurrencesColor(this.markOccurrencesColor);
		textArea.setPaintMarkOccurrencesBorder(this.markOccurrencesBorder);
		textArea.setMatchedBracketBGColor(this.matchedBracketBG);
		textArea.setMatchedBracketBorderColor(this.matchedBracketFG);
		textArea.setPaintMatchedBracketPair(this.matchedBracketHighlightBoth);
		textArea.setAnimateBracketMatching(this.matchedBracketAnimate);
		textArea.setHyperlinkForeground(this.hyperlinkFG);

		final int count = this.secondaryLanguages.length;
		for (int i = 0; i < count; i++)
			textArea.setSecondaryLanguageBackground(i + 1, this.secondaryLanguages[i]);

		textArea.setSyntaxScheme(this.scheme);

		final Gutter gutter = RSyntaxUtilities.getGutter(textArea);
		if (gutter != null) {
			gutter.setBackground(this.gutterBackgroundColor);
			gutter.setBorderColor(this.gutterBorderColor);
			gutter.setActiveLineRangeColor(this.activeLineRangeColor);
			gutter.setIconRowHeaderInheritsGutterBackground(this.iconRowHeaderInheritsGutterBG);
			gutter.setLineNumberColor(this.lineNumberColor);
			final String fontName = this.lineNumberFont != null ? this.lineNumberFont : this.baseFont.getFamily();
			final int fontSize = this.lineNumberFontSize > 0 ? this.lineNumberFontSize : this.baseFont.getSize();
			final Font font = Theme.getFont(fontName, Font.PLAIN, fontSize);
			gutter.setLineNumberFont(font);
			gutter.setFoldIndicatorForeground(this.foldIndicatorFG);
			gutter.setFoldBackground(this.foldBG);
			gutter.setArmedFoldBackground(this.armedFoldBG);
		}

	}

	/**
	 * Saves this theme to an output stream.
	 *
	 * @param out
	 *            The output stream to write to.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #load(InputStream)
	 */
	public void save(final OutputStream out) throws IOException {

		final BufferedOutputStream bout = new BufferedOutputStream(out);
		try {

			final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final DOMImplementation impl = db.getDOMImplementation();

			final Document doc = impl.createDocument(null, "RSyntaxTheme", null);
			final Element root = doc.getDocumentElement();
			root.setAttribute("version", "1.0");

			Element elem = doc.createElement("baseFont");
			if (!this.baseFont.getFamily().equals(RTextAreaBase.getDefaultFont().getFamily()))
				elem.setAttribute("family", this.baseFont.getFamily());
			elem.setAttribute("size", Integer.toString(this.baseFont.getSize()));
			root.appendChild(elem);

			elem = doc.createElement("background");
			elem.setAttribute("color", Theme.colorToString(this.bgColor));
			root.appendChild(elem);

			elem = doc.createElement("caret");
			elem.setAttribute("color", Theme.colorToString(this.caretColor));
			root.appendChild(elem);

			elem = doc.createElement("selection");
			elem.setAttribute("useFG", Boolean.toString(this.useSelctionFG));
			elem.setAttribute("fg", Theme.colorToString(this.selectionFG));
			elem.setAttribute("bg", Theme.colorToString(this.selectionBG));
			elem.setAttribute("roundedEdges", Boolean.toString(this.selectionRoundedEdges));
			root.appendChild(elem);

			elem = doc.createElement("currentLineHighlight");
			elem.setAttribute("color", Theme.colorToString(this.currentLineHighlight));
			elem.setAttribute("fade", Boolean.toString(this.fadeCurrentLineHighlight));
			root.appendChild(elem);

			elem = doc.createElement("marginLine");
			elem.setAttribute("fg", Theme.colorToString(this.marginLineColor));
			root.appendChild(elem);

			elem = doc.createElement("markAllHighlight");
			elem.setAttribute("color", Theme.colorToString(this.markAllHighlightColor));
			root.appendChild(elem);

			elem = doc.createElement("markOccurrencesHighlight");
			elem.setAttribute("color", Theme.colorToString(this.markOccurrencesColor));
			elem.setAttribute("border", Boolean.toString(this.markOccurrencesBorder));
			root.appendChild(elem);

			elem = doc.createElement("matchedBracket");
			elem.setAttribute("fg", Theme.colorToString(this.matchedBracketFG));
			elem.setAttribute("bg", Theme.colorToString(this.matchedBracketBG));
			elem.setAttribute("highlightBoth", Boolean.toString(this.matchedBracketHighlightBoth));
			elem.setAttribute("animate", Boolean.toString(this.matchedBracketAnimate));
			root.appendChild(elem);

			elem = doc.createElement("hyperlinks");
			elem.setAttribute("fg", Theme.colorToString(this.hyperlinkFG));
			root.appendChild(elem);

			elem = doc.createElement("secondaryLanguages");
			for (int i = 0; i < this.secondaryLanguages.length; i++) {
				final Color color = this.secondaryLanguages[i];
				final Element elem2 = doc.createElement("language");
				elem2.setAttribute("index", Integer.toString(i + 1));
				elem2.setAttribute("bg", color == null ? "" : Theme.colorToString(color));
				elem.appendChild(elem2);
			}
			root.appendChild(elem);

			elem = doc.createElement("gutterBackground");
			elem.setAttribute("color", Theme.colorToString(this.gutterBackgroundColor));
			root.appendChild(elem);

			elem = doc.createElement("gutterBorder");
			elem.setAttribute("color", Theme.colorToString(this.gutterBorderColor));
			root.appendChild(elem);

			elem = doc.createElement("lineNumbers");
			elem.setAttribute("fg", Theme.colorToString(this.lineNumberColor));
			if (this.lineNumberFont != null)
				elem.setAttribute("fontFamily", this.lineNumberFont);
			if (this.lineNumberFontSize > 0)
				elem.setAttribute("fontSize", Integer.toString(this.lineNumberFontSize));
			root.appendChild(elem);

			elem = doc.createElement("foldIndicator");
			elem.setAttribute("fg", Theme.colorToString(this.foldIndicatorFG));
			elem.setAttribute("iconBg", Theme.colorToString(this.foldBG));
			elem.setAttribute("iconArmedBg", Theme.colorToString(this.armedFoldBG));
			root.appendChild(elem);

			elem = doc.createElement("iconRowHeader");
			elem.setAttribute("activeLineRange", Theme.colorToString(this.activeLineRangeColor));
			elem.setAttribute("inheritsGutterBG", Boolean.toString(this.iconRowHeaderInheritsGutterBG));
			root.appendChild(elem);

			elem = doc.createElement("tokenStyles");
			final Field[] fields = TokenTypes.class.getFields();
			for (final Field field : fields) {
				final int value = field.getInt(null);
				if (value != TokenTypes.DEFAULT_NUM_TOKEN_TYPES) {
					final Style style = this.scheme.getStyle(value);
					if (style != null) {
						final Element elem2 = doc.createElement("style");
						elem2.setAttribute("token", field.getName());
						final Color fg = style.foreground;
						if (fg != null)
							elem2.setAttribute("fg", Theme.colorToString(fg));
						final Color bg = style.background;
						if (bg != null)
							elem2.setAttribute("bg", Theme.colorToString(bg));
						final Font font = style.font;
						if (font != null) {
							if (!font.getFamily().equals(this.baseFont.getFamily()))
								elem2.setAttribute("fontFamily", font.getFamily());
							if (font.getSize() != this.baseFont.getSize())
								elem2.setAttribute("fontSize", Integer.toString(font.getSize()));
							if (font.isBold())
								elem2.setAttribute("bold", "true");
							if (font.isItalic())
								elem2.setAttribute("italic", "true");
						}
						if (style.underline)
							elem2.setAttribute("underline", "true");
						elem.appendChild(elem2);
					}
				}
			}
			root.appendChild(elem);

			final DOMSource source = new DOMSource(doc);
			// Use a writer instead of OutputStream to allow pretty printing.
			// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337981
			final StreamResult result = new StreamResult(new PrintWriter(new UnicodeWriter(bout, "UTF-8")));
			final TransformerFactory transFac = TransformerFactory.newInstance();
			final Transformer transformer = transFac.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "theme.dtd");
			transformer.transform(source, result);

		} catch (final RuntimeException re) {
			throw re; // FindBugs
		} catch (final Exception e) {
			throw new IOException("Error generating XML: " + e.getMessage(), e);
		} finally {
			bout.close();
		}

	}

}