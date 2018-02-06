/*
 * 02/06/2010
 *
 * CompletionXMLParser.java - Parses XML representing code completion for a
 * C-like language.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for an XML file describing a procedural language such as C. XML files
 * will be validated against the <code>CompletionXml.dtd</code> DTD found in
 * this package.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CompletionXMLParser extends DefaultHandler {

	/**
	 * The class loader to use to load custom completion classes, such as the one
	 * defined by {@link #funcCompletionType}. If this is <code>null</code>, then a
	 * default class loader is used. This field will usually be <code>null</code>.
	 */
	private static ClassLoader DEFAULT_COMPLETION_CLASS_LOADER;

	private static final char getSingleChar(final String str) {
		return str.length() == 1 ? str.charAt(0) : 0;
	}

	/**
	 * Sets the class loader to use when loading custom classes to use for various
	 * {@link Completion} types, such as {@link FunctionCompletion}s, from XML.
	 * <p>
	 *
	 * Users should very rarely have a need to use this method.
	 *
	 * @param cl
	 *            The class loader to use. If this is <code>null</code>, then a
	 *            default is used.
	 */
	public static void setDefaultCompletionClassLoader(final ClassLoader cl) {
		CompletionXMLParser.DEFAULT_COMPLETION_CLASS_LOADER = cl;
	}

	/**
	 * The completion provider to use when loading classes, such as custom
	 * {@link FunctionCompletion}s.
	 */
	private ClassLoader completionCL;
	/**
	 * The completions found after parsing the XML.
	 */
	private final List<Completion> completions;
	private String definedIn;
	private final StringBuilder desc;
	private boolean doingKeywords;
	/**
	 * If specified in the XML, this class will be used instead of
	 * {@link FunctionCompletion} when appropriate. This class should extend
	 * <tt>FunctionCompletion</tt>, or stuff will break.
	 */
	private String funcCompletionType;
	private boolean gettingDesc;
	private boolean gettingParamDesc;
	private boolean gettingParams;
	private boolean gettingReturnValDesc;
	private boolean inCompletionTypes;
	private boolean inKeyword;
	private boolean inParam;
	private String name;
	private final StringBuilder paramDesc;
	private char paramEndChar;
	private String paramName;
	private final List<ParameterizedCompletion.Parameter> params;
	private String paramSeparator;
	private char paramStartChar;
	private String paramType;

	/**
	 * The provider we're getting completions for.
	 */
	private CompletionProvider provider;

	private String returnType;

	private final StringBuilder returnValDesc;

	private String type;

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The provider to get completions for.
	 * @see #reset(CompletionProvider)
	 */
	public CompletionXMLParser(final CompletionProvider provider) {
		this(provider, null);
	}

	/**
	 * Constructor.
	 *
	 * @param provider
	 *            The provider to get completions for.
	 * @param cl
	 *            The class loader to use, if necessary, when loading classes from
	 *            the XML (custom {@link FunctionCompletion}s, for example). This
	 *            may be <code>null</code> if the default is to be used, or if the
	 *            XML does not define specific classes for completion types.
	 * @see #reset(CompletionProvider)
	 */
	public CompletionXMLParser(final CompletionProvider provider, final ClassLoader cl) {
		this.provider = provider;
		this.completionCL = cl;
		if (this.completionCL == null)
			// May also be null, but that's okay.
			this.completionCL = CompletionXMLParser.DEFAULT_COMPLETION_CLASS_LOADER;
		this.completions = new ArrayList<>();
		this.params = new ArrayList<>(1);
		this.desc = new StringBuilder();
		this.paramDesc = new StringBuilder();
		this.returnValDesc = new StringBuilder();
		this.paramStartChar = this.paramEndChar = 0;
		this.paramSeparator = null;
	}

	/**
	 * Called when character data inside an element is found.
	 */
	@Override
	public void characters(final char[] ch, final int start, final int length) {
		if (this.gettingDesc)
			this.desc.append(ch, start, length);
		else if (this.gettingParamDesc)
			this.paramDesc.append(ch, start, length);
		else if (this.gettingReturnValDesc)
			this.returnValDesc.append(ch, start, length);
	}

	private FunctionCompletion createFunctionCompletion() {

		FunctionCompletion fc = null;
		if (this.funcCompletionType != null)
			try {
				Class<?> clazz = null;
				if (this.completionCL != null)
					clazz = Class.forName(this.funcCompletionType, true, this.completionCL);
				else
					clazz = Class.forName(this.funcCompletionType);
				final Constructor<?> c = clazz.getDeclaredConstructor(CompletionProvider.class, String.class,
						String.class);
				fc = (FunctionCompletion) c.newInstance(this.provider, this.name, this.returnType);
			} catch (final RuntimeException re) { // FindBugs
				throw re;
			} catch (final Exception e) {
				e.printStackTrace();
			}

		if (fc == null)
			fc = new FunctionCompletion(this.provider, this.name, this.returnType);

		if (this.desc.length() > 0) {
			fc.setShortDescription(this.desc.toString());
			this.desc.setLength(0);
		}
		fc.setParams(this.params);
		fc.setDefinedIn(this.definedIn);
		if (this.returnValDesc.length() > 0) {
			fc.setReturnValueDescription(this.returnValDesc.toString());
			this.returnValDesc.setLength(0);
		}

		return fc;

	}

	private MarkupTagCompletion createMarkupTagCompletion() {
		final MarkupTagCompletion mc = new MarkupTagCompletion(this.provider, this.name);
		if (this.desc.length() > 0) {
			mc.setDescription(this.desc.toString());
			this.desc.setLength(0);
		}
		mc.setAttributes(this.params);
		mc.setDefinedIn(this.definedIn);
		return mc;
	}

	private BasicCompletion createOtherCompletion() {
		final BasicCompletion bc = new BasicCompletion(this.provider, this.name);
		if (this.desc.length() > 0) {
			bc.setSummary(this.desc.toString());
			this.desc.setLength(0);
		}
		return bc;
	}

	private VariableCompletion createVariableCompletion() {
		final VariableCompletion vc = new VariableCompletion(this.provider, this.name, this.returnType);
		if (this.desc.length() > 0) {
			vc.setShortDescription(this.desc.toString());
			this.desc.setLength(0);
		}
		vc.setDefinedIn(this.definedIn);
		return vc;
	}

	/**
	 * Called when an element is closed.
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qName) {

		if ("keywords".equals(qName))
			this.doingKeywords = false;
		else if (this.doingKeywords) {

			if ("keyword".equals(qName)) {
				Completion c = null;
				if ("function".equals(this.type))
					c = this.createFunctionCompletion();
				else if ("constant".equals(this.type))
					c = this.createVariableCompletion();
				else if ("tag".equals(this.type))
					c = this.createMarkupTagCompletion();
				else if ("other".equals(this.type))
					c = this.createOtherCompletion();
				else
					throw new InternalError("Unexpected type: " + this.type);
				this.completions.add(c);
				this.inKeyword = false;
			} else if (this.inKeyword)
				if ("returnValDesc".equals(qName))
					this.gettingReturnValDesc = false;
				else if (this.gettingParams) {
					if ("params".equals(qName))
						this.gettingParams = false;
					else if ("param".equals(qName)) {
						final FunctionCompletion.Parameter param = new FunctionCompletion.Parameter(this.paramType,
								this.paramName);
						if (this.paramDesc.length() > 0) {
							param.setDescription(this.paramDesc.toString());
							this.paramDesc.setLength(0);
						}
						this.params.add(param);
						this.inParam = false;
					} else if (this.inParam)
						if ("desc".equals(qName))
							this.gettingParamDesc = false;
				} else if ("desc".equals(qName))
					this.gettingDesc = false;

		}

		else if (this.inCompletionTypes)
			if ("completionTypes".equals(qName))
				this.inCompletionTypes = false;

	}

	@Override
	public void error(final SAXParseException e) throws SAXException {
		throw e;
	}

	/**
	 * Returns the completions found after parsing the XML.
	 *
	 * @return The completions.
	 */
	public List<Completion> getCompletions() {
		return this.completions;
	}

	/**
	 * Returns the parameter end character specified.
	 *
	 * @return The character, or 0 if none was specified.
	 */
	public char getParamEndChar() {
		return this.paramEndChar;
	}

	/**
	 * Returns the parameter end string specified.
	 *
	 * @return The string, or <code>null</code> if none was specified.
	 */
	public String getParamSeparator() {
		return this.paramSeparator;
	}

	/**
	 * Returns the parameter start character specified.
	 *
	 * @return The character, or 0 if none was specified.
	 */
	public char getParamStartChar() {
		return this.paramStartChar;
	}

	/**
	 * Resets this parser to grab more completions.
	 *
	 * @param provider
	 *            The new provider to get completions for.
	 */
	public void reset(final CompletionProvider provider) {
		this.provider = provider;
		this.completions.clear();
		this.doingKeywords = this.inKeyword = this.gettingDesc = this.gettingParams = this.inParam = this.gettingParamDesc = false;
		this.paramStartChar = this.paramEndChar = 0;
		this.paramSeparator = null;
	}

	@Override
	public InputSource resolveEntity(final String publicID, final String systemID) throws SAXException {
		return new InputSource(this.getClass().getResourceAsStream("CompletionXml.dtd"));
	}

	/**
	 * Called when an element starts.
	 */
	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attrs) {
		if ("keywords".equals(qName))
			this.doingKeywords = true;
		else if (this.doingKeywords) {
			if ("keyword".equals(qName)) {
				this.name = attrs.getValue("name");
				this.type = attrs.getValue("type");
				this.returnType = attrs.getValue("returnType");
				this.params.clear();
				this.definedIn = attrs.getValue("definedIn");
				this.inKeyword = true;
			} else if (this.inKeyword)
				if ("returnValDesc".equals(qName))
					this.gettingReturnValDesc = true;
				else if ("params".equals(qName))
					this.gettingParams = true;
				else if (this.gettingParams) {
					if ("param".equals(qName)) {
						this.paramName = attrs.getValue("name");
						this.paramType = attrs.getValue("type");
						this.inParam = true;
					}
					if (this.inParam)
						if ("desc".equals(qName))
							this.gettingParamDesc = true;
				} else if ("desc".equals(qName))
					this.gettingDesc = true;
		} else if ("environment".equals(qName)) {
			this.paramStartChar = CompletionXMLParser.getSingleChar(attrs.getValue("paramStartChar"));
			this.paramEndChar = CompletionXMLParser.getSingleChar(attrs.getValue("paramEndChar"));
			this.paramSeparator = attrs.getValue("paramSeparator");
			// paramTerminal = attrs.getValua("terminal");
		} else if ("completionTypes".equals(qName))
			this.inCompletionTypes = true;
		else if (this.inCompletionTypes)
			if ("functionCompletionType".equals(qName))
				this.funcCompletionType = attrs.getValue("type");
	}

	@Override
	public void warning(final SAXParseException e) throws SAXException {
		throw e;
	}

}