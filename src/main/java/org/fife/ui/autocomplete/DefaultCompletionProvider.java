/*
 * 12/21/2008
 *
 * DefaultCompletionProvider.java - A basic completion provider implementation.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

/**
 * A basic completion provider implementation. This provider has no
 * understanding of language semantics. It simply checks the text entered up to
 * the caret position for a match against known completions. This is all that is
 * needed in the majority of cases.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DefaultCompletionProvider extends AbstractCompletionProvider {

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)}.
	 */
	private String lastCompletionsAtText;

	/**
	 * Used to speed up {@link #getCompletionsAt(JTextComponent, Point)}, since this
	 * may be called multiple times in succession (this is usually called by
	 * <tt>JTextComponent.getToolTipText()</tt>, and if the user wiggles the mouse
	 * while a tool tip is displayed, this method gets repeatedly called. It can be
	 * costly so we try to speed it up a tad).
	 */
	private List<Completion> lastParameterizedCompletionsAt;

	protected Segment seg;

	/**
	 * Constructor. The returned provider will not be aware of any completions.
	 *
	 * @see #addCompletion(Completion)
	 */
	public DefaultCompletionProvider() {
		this.init();
	}

	/**
	 * Creates a completion provider that provides completion for a simple list of
	 * words.
	 *
	 * @param words
	 *            The words to offer as completion suggestions. If this is
	 *            <code>null</code>, no completions will be known.
	 * @see BasicCompletion
	 */
	public DefaultCompletionProvider(final String[] words) {
		this.init();
		this.addWordCompletions(words);
	}

	/**
	 * Returns the text just before the current caret position that could be the
	 * start of something auto-completable.
	 * <p>
	 *
	 * This method returns all characters before the caret that are matched by
	 * {@link #isValidChar(char)}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getAlreadyEnteredText(final JTextComponent comp) {

		final Document doc = comp.getDocument();

		final int dot = comp.getCaretPosition();
		final Element root = doc.getDefaultRootElement();
		final int index = root.getElementIndex(dot);
		final Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot - start;
		try {
			doc.getText(start, len, this.seg);
		} catch (final BadLocationException ble) {
			ble.printStackTrace();
			return CompletionProviderBase.EMPTY_STRING;
		}

		final int segEnd = this.seg.offset + len;
		start = segEnd - 1;
		while (start >= this.seg.offset && this.isValidChar(this.seg.array[start]))
			start--;
		start++;

		len = segEnd - start;
		return len == 0 ? CompletionProviderBase.EMPTY_STRING : new String(this.seg.array, start, len);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Completion> getCompletionsAt(final JTextComponent tc, final Point p) {

		final int offset = tc.viewToModel(p);
		if (offset < 0 || offset >= tc.getDocument().getLength()) {
			this.lastCompletionsAtText = null;
			return this.lastParameterizedCompletionsAt = null;
		}

		final Segment s = new Segment();
		final Document doc = tc.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int line = root.getElementIndex(offset);
		final Element elem = root.getElement(line);
		final int start = elem.getStartOffset();
		final int end = elem.getEndOffset() - 1;

		try {

			doc.getText(start, end - start, s);

			// Get the valid chars before the specified offset.
			int startOffs = s.offset + offset - start - 1;
			while (startOffs >= s.offset && this.isValidChar(s.array[startOffs]))
				startOffs--;

			// Get the valid chars at and after the specified offset.
			int endOffs = s.offset + offset - start;
			while (endOffs < s.offset + s.count && this.isValidChar(s.array[endOffs]))
				endOffs++;

			final int len = endOffs - startOffs - 1;
			if (len <= 0)
				return this.lastParameterizedCompletionsAt = null;
			final String text = new String(s.array, startOffs + 1, len);

			if (text.equals(this.lastCompletionsAtText))
				return this.lastParameterizedCompletionsAt;

			// Get a list of all Completions matching the text.
			final List<Completion> list = this.getCompletionByInputText(text);
			this.lastCompletionsAtText = text;
			return this.lastParameterizedCompletionsAt = list;

		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		this.lastCompletionsAtText = null;
		return this.lastParameterizedCompletionsAt = null;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(final JTextComponent tc) {

		List<ParameterizedCompletion> list = null;

		// If this provider doesn't support parameterized completions,
		// bail out now.
		final char paramListStart = this.getParameterListStart();
		if (paramListStart == 0)
			return list; // null

		final int dot = tc.getCaretPosition();
		final Segment s = new Segment();
		final Document doc = tc.getDocument();
		final Element root = doc.getDefaultRootElement();
		final int line = root.getElementIndex(dot);
		final Element elem = root.getElement(line);
		int offs = elem.getStartOffset();
		final int len = dot - offs - 1/* paramListStart.length() */;
		if (len <= 0)
			return list; // null

		try {

			doc.getText(offs, len, s);

			// Get the identifier preceding the '(', ignoring any whitespace
			// between them.
			offs = s.offset + len - 1;
			while (offs >= s.offset && Character.isWhitespace(s.array[offs]))
				offs--;
			final int end = offs;
			while (offs >= s.offset && this.isValidChar(s.array[offs]))
				offs--;

			final String text = new String(s.array, offs + 1, end - offs);

			// Get a list of all Completions matching the text, but then
			// narrow it down to just the ParameterizedCompletions.
			final List<Completion> l = this.getCompletionByInputText(text);
			if (l != null && !l.isEmpty())
				for (int i = 0; i < l.size(); i++) {
					final Object o = l.get(i);
					if (o instanceof ParameterizedCompletion) {
						if (list == null)
							list = new ArrayList<>(1);
						list.add((ParameterizedCompletion) o);
					}
				}

		} catch (final BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return list;

	}

	/**
	 * Initializes this completion provider.
	 */
	protected void init() {
		this.seg = new Segment();
	}

	/**
	 * Returns whether the specified character is valid in an auto-completion. The
	 * default implementation is equivalent to
	 * "<code>Character.isLetterOrDigit(ch) || ch=='_'</code>". Subclasses can
	 * override this method to change what characters are matched.
	 *
	 * @param ch
	 *            The character.
	 * @return Whether the character is valid.
	 */
	protected boolean isValidChar(final char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_';
	}

	/**
	 * Loads completions from an XML file. The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param file
	 *            An XML file to load from.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public void loadFromXML(final File file) throws IOException {
		final BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
		try {
			this.loadFromXML(bin);
		} finally {
			bin.close();
		}
	}

	/**
	 * Loads completions from an XML input stream. The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param in
	 *            The input stream to read from.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public void loadFromXML(final InputStream in) throws IOException {
		this.loadFromXML(in, null);
	}

	/**
	 * Loads completions from an XML input stream. The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param in
	 *            The input stream to read from.
	 * @param cl
	 *            The class loader to use when loading any extra classes defined in
	 *            the XML, such as custom {@link FunctionCompletion}s. This may be
	 *            <code>null</code> if the default is to be used, or if no custom
	 *            completions are defined in the XML.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public void loadFromXML(final InputStream in, final ClassLoader cl) throws IOException {

		// long start = System.currentTimeMillis();

		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(true);
		final CompletionXMLParser handler = new CompletionXMLParser(this, cl);
		final BufferedInputStream bin = new BufferedInputStream(in);
		try {
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(bin, handler);
			final List<Completion> completions = handler.getCompletions();
			this.addCompletions(completions);
			final char startChar = handler.getParamStartChar();
			if (startChar != 0) {
				final char endChar = handler.getParamEndChar();
				final String sep = handler.getParamSeparator();
				if (endChar != 0 && sep != null && sep.length() > 0)
					this.setParameterizedCompletionParams(startChar, sep, endChar);
			}
		} catch (final SAXException se) {
			throw new IOException(se.toString());
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce.toString());
		} finally {
			// long time = System.currentTimeMillis() - start;
			// System.out.println("XML loaded in: " + time + "ms");
			bin.close();
		}

	}

	/**
	 * Loads completions from an XML file. The XML should validate against
	 * <code>CompletionXml.dtd</code>.
	 *
	 * @param resource
	 *            A resource the current ClassLoader can get to.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public void loadFromXML(final String resource) throws IOException {
		final ClassLoader cl = this.getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream(resource);
		if (in == null) {
			final File file = new File(resource);
			if (file.isFile())
				in = new FileInputStream(file);
			else
				throw new IOException("No such resource: " + resource);
		}
		final BufferedInputStream bin = new BufferedInputStream(in);
		try {
			this.loadFromXML(bin);
		} finally {
			bin.close();
		}
	}

}