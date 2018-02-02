/*
 * 09/16/2004
 *
 * Macro.java - A macro as recorded/played back by an RTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.fife.io.UnicodeReader;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A macro as recorded/played back by an {@link RTextArea}.
 * <p>
 *
 * <code>Macro</code>s are static; when a Macro is loaded, it can be run by any
 * instance of <code>RTextArea</code> in the application. To activate and play
 * back a macro, use the following methods:
 *
 * <ul>
 * <li>{@link RTextArea#loadMacro(Macro)}
 * <li>{@link RTextArea#playbackLastMacro()}
 * </ul>
 *
 * To record and save a new macro, you'd use the following methods:
 *
 * <ul>
 * <li>{@link RTextArea#beginRecordingMacro()} (this discards the previous
 * "current" macro, if any)
 * <li>{@link RTextArea#endRecordingMacro()} (at this point, you could call
 * <code>playbackLastMacro()</code> to play this macro immediately if desired)
 * <li>{@link RTextArea#getCurrentMacro()}.{@link #saveToFile(File)}
 * </ul>
 *
 * As <code>Macro</code>s save themselves as XML files, a common technique is to
 * save all macros in files named "<code>{@link #getName()}.xml</code>", and
 * place them all in a common directory.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class Macro {

	/**
	 * A "record" of a macro is a single action in the macro (corresponding to a key
	 * type and some action in the editor, such as a letter inserted into the
	 * document, scrolling one page down, selecting the current line, etc.).
	 */
	static class MacroRecord {

		String actionCommand;
		String id;

		MacroRecord() {
			this(null, null);
		}

		MacroRecord(final String id, final String actionCommand) {
			this.id = id;
			this.actionCommand = actionCommand;
		}

	}

	private static final String ACTION = "action";

	private static final String FILE_ENCODING = "UTF-8";
	private static final String ID = "id";
	private static final String MACRO_NAME = "macroName";
	private static final String ROOT_ELEMENT = "macro";

	private static final String UNTITLED_MACRO_NAME = "<Untitled>";

	private ArrayList<MacroRecord> macroRecords;

	private String name;

	/**
	 * Constructor.
	 */
	public Macro() {
		this(Macro.UNTITLED_MACRO_NAME);
	}

	/**
	 * Loads a macro from a file on disk.
	 *
	 * @param file
	 *            The file from which to load the macro.
	 * @throws IOException
	 *             If the file does not exist or an I/O exception occurs while
	 *             reading the file.
	 * @see #saveToFile(String)
	 * @see #saveToFile(File)
	 */
	public Macro(final File file) throws IOException {

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			// InputSource is = new InputSource(new FileReader(file));
			final InputSource is = new InputSource(new UnicodeReader(new FileInputStream(file), Macro.FILE_ENCODING));
			is.setEncoding(Macro.FILE_ENCODING);
			doc = db.parse(is);// db.parse(file);
		} catch (final Exception e) {
			e.printStackTrace();
			String desc = e.getMessage();
			if (desc == null)
				desc = e.toString();
			throw new IOException("Error parsing XML: " + desc);
		}

		this.macroRecords = new ArrayList<>();

		// Traverse the XML tree.
		final boolean parsedOK = this.initializeFromXMLFile(doc.getDocumentElement());
		if (!parsedOK) {
			this.name = null;
			this.macroRecords.clear();
			this.macroRecords = null;
			throw new IOException("Error parsing XML!");
		}

	}

	/**
	 * Constructor.
	 *
	 * @param name
	 *            The name of the macro.
	 */
	public Macro(final String name) {
		this(name, null);
	}

	/**
	 * Constructor.
	 *
	 * @param name
	 *            The name of the macro.
	 * @param records
	 *            The initial records of the macro.
	 */
	public Macro(final String name, final List<MacroRecord> records) {

		this.name = name;

		if (records != null) {
			this.macroRecords = new ArrayList<>(records.size());
			for (final MacroRecord record : records)
				this.macroRecords.add(record);
		} else
			this.macroRecords = new ArrayList<>(10);

	}

	/**
	 * Adds a macro record to this macro.
	 *
	 * @param record
	 *            The record to add. If <code>null</code>, nothing happens.
	 * @see #getMacroRecords
	 */
	public void addMacroRecord(final MacroRecord record) {
		if (record != null)
			this.macroRecords.add(record);
	}

	/**
	 * Returns the macro records that make up this macro.
	 *
	 * @return The macro records.
	 * @see #addMacroRecord
	 */
	public List<MacroRecord> getMacroRecords() {
		return this.macroRecords;
	}

	/**
	 * Returns the name of this macro. A macro's name is simply something to
	 * identify it with in a UI; it has nothing to do with the name of the file to
	 * save the macro to.
	 *
	 * @return The macro's name.
	 * @see #setName(String)
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Used in parsing an XML document containing a macro. This method initializes
	 * this macro with the data contained in the passed-in node.
	 *
	 * @param node
	 *            The root node of the parsed XML document.
	 * @return <code>true</code> if the macro initialization went okay;
	 *         <code>false</code> if an error occurred.
	 */
	private boolean initializeFromXMLFile(final Element root) {

		/*
		 * This method expects the XML document to be in the following format:
		 *
		 * <?xml version="1.0" encoding="UTF-8" ?> <macro> <macroName>test</macroName>
		 * <action id="default-typed">abcdefg</action> [<action id=...>...</action>] ...
		 * </macro>
		 *
		 */

		final NodeList childNodes = root.getChildNodes();
		final int count = childNodes.getLength();

		for (int i = 0; i < count; i++) {

			Node node = childNodes.item(i);
			final int type = node.getNodeType();
			switch (type) {

			// Handle element nodes.
			case Node.ELEMENT_NODE:

				final String nodeName = node.getNodeName();

				if (nodeName.equals(Macro.MACRO_NAME)) {
					final NodeList childNodes2 = node.getChildNodes();
					this.name = Macro.UNTITLED_MACRO_NAME;
					if (childNodes2.getLength() > 0) {
						node = childNodes2.item(0);
						final int type2 = node.getNodeType();
						if (type2 != Node.CDATA_SECTION_NODE && type2 != Node.TEXT_NODE)
							return false;
						this.name = node.getNodeValue().trim();
					}
					// System.err.println("Macro name==" + name);
				}

				else if (nodeName.equals(Macro.ACTION)) {
					final NamedNodeMap attributes = node.getAttributes();
					if (attributes == null || attributes.getLength() != 1)
						return false;
					final Node node2 = attributes.item(0);
					final MacroRecord macroRecord = new MacroRecord();
					if (!node2.getNodeName().equals(Macro.ID))
						return false;
					macroRecord.id = node2.getNodeValue();
					final NodeList childNodes2 = node.getChildNodes();
					final int length = childNodes2.getLength();
					if (length == 0) { // Could be empty "" command.
						// System.err.println("... empty actionCommand");
						macroRecord.actionCommand = "";
						// System.err.println("... adding action: " + macroRecord);
						this.macroRecords.add(macroRecord);
						break;
					} else {
						node = childNodes2.item(0);
						final int type2 = node.getNodeType();
						if (type2 != Node.CDATA_SECTION_NODE && type2 != Node.TEXT_NODE)
							return false;
						macroRecord.actionCommand = node.getNodeValue();
						this.macroRecords.add(macroRecord);
					}

				}
				break;

			default:
				break; // Skip whitespace nodes, etc.

			}

		}

		// Everything went okay.
		return true;

	}

	/**
	 * Saves this macro to an XML file. This file can later be read in by the
	 * constructor taking a <code>File</code> parameter; this is the mechanism for
	 * saving macros.
	 *
	 * @param file
	 *            The file in which to save the macro.
	 * @throws IOException
	 *             If an error occurs while generating the XML for the output file.
	 * @see #saveToFile(String)
	 */
	public void saveToFile(final File file) throws IOException {
		this.saveToFile(file.getAbsolutePath());
	}

	/**
	 * Saves this macro to a file. This file can later be read in by the constructor
	 * taking a <code>File</code> parameter; this is the mechanism for saving
	 * macros.
	 *
	 * @param fileName
	 *            The name of the file in which to save the macro.
	 * @throws IOException
	 *             If an error occurs while generating the XML for the output file.
	 * @see #saveToFile(File)
	 */
	public void saveToFile(final String fileName) throws IOException {

		/*
		 * This method writes the XML document in the following format:
		 *
		 * <?xml version="1.0" encoding="UTF-8" ?> <macro> <macroName>test</macroName>
		 * <action id="default-typed">abcdefg</action> [<action id=...>...</action>] ...
		 * </macro>
		 *
		 */

		try {

			final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final DOMImplementation impl = db.getDOMImplementation();

			final Document doc = impl.createDocument(null, Macro.ROOT_ELEMENT, null);
			final Element rootElement = doc.getDocumentElement();

			// Write the name of the macro.
			final Element nameElement = doc.createElement(Macro.MACRO_NAME);
			nameElement.appendChild(doc.createCDATASection(this.name));
			rootElement.appendChild(nameElement);

			// Write all actions (the meat) in the macro.
			for (final MacroRecord record : this.macroRecords) {
				final Element actionElement = doc.createElement(Macro.ACTION);
				actionElement.setAttribute(Macro.ID, record.id);
				if (record.actionCommand != null && record.actionCommand.length() > 0) {
					// Remove illegal characters. I'm no XML expert, but
					// I'm not sure what I'm doing wrong. If we don't
					// strip out chars with Unicode value < 32, our
					// generator will insert '&#<value>', which will cause
					// our parser to barf when reading the macro back in
					// (it says "Invalid XML character"). But why doesn't
					// our generator tell us the character is invalid too?
					String command = record.actionCommand;
					for (int j = 0; j < command.length(); j++)
						if (command.charAt(j) < 32) {
							command = command.substring(0, j);
							if (j < command.length() - 1)
								command += command.substring(j + 1);
						}
					final Node n = doc.createCDATASection(command);
					actionElement.appendChild(n);
				}
				rootElement.appendChild(actionElement);
			}

			// Dump the XML out to the file.
			final StreamResult result = new StreamResult(new File(fileName));
			final DOMSource source = new DOMSource(doc);
			final TransformerFactory transFac = TransformerFactory.newInstance();
			final Transformer transformer = transFac.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, Macro.FILE_ENCODING);
			transformer.transform(source, result);

		} catch (final RuntimeException re) {
			throw re; // Keep FindBugs happy.
		} catch (final Exception e) {
			throw new IOException("Error generating XML!");
		}

	}

	/**
	 * Sets the name of this macro. A macro's name is simply something to identify
	 * it with in a UI; it has nothing to do with the name of the file to save the
	 * macro to.
	 *
	 * @param name
	 *            The new name for the macro.
	 * @see #getName()
	 */
	public void setName(final String name) {
		this.name = name;
	}

}