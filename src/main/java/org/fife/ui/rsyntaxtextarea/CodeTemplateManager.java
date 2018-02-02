/*
 * 02/21/2005
 *
 * CodeTemplateManager.java - manages code templates.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;

/**
 * Manages "code templates."
 * <p>
 *
 * All methods in this class are synchronized for thread safety, but as a best
 * practice, you should probably only modify the templates known to a
 * <code>CodeTemplateManager</code> on the EDT. Modifying a
 * <code>CodeTemplate</code> retrieved from a <code>CodeTemplateManager</code>
 * while <em>not</em> on the EDT could cause problems.
 * <p>
 *
 * For more flexible boilerplate code insertion, consider using the <a href=
 * "http://javadoc.fifesoft.com/autocomplete/org/fife/ui/autocomplete/TemplateCompletion.html">
 * TemplateCompletion class</a> in the
 * <a href="https://github.com/bobbylight/AutoComplete">AutoComplete add-on
 * library</a>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CodeTemplateManager {

	/**
	 * A comparator that takes a <code>CodeTemplate</code> as its first parameter
	 * and a <code>Segment</code> as its second, and knows to compare the template's
	 * ID to the segment's text.
	 */
	@SuppressWarnings("rawtypes")
	private static class TemplateComparator implements Comparator, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Object template, final Object segment) {

			// Get template start index (0) and length.
			final CodeTemplate t = (CodeTemplate) template;
			final char[] templateArray = t.getID().toCharArray();
			int i = 0;
			final int len1 = templateArray.length;

			// Find "token" part of segment and get its offset and length.
			final Segment s = (Segment) segment;
			final char[] segArray = s.array;
			int len2 = s.count;
			int j = s.offset + len2 - 1;
			while (j >= s.offset && CodeTemplateManager.isValidChar(segArray[j]))
				j--;
			j++;
			final int segShift = j - s.offset;
			len2 -= segShift;

			int n = Math.min(len1, len2);
			while (n-- != 0) {
				final char c1 = templateArray[i++];
				final char c2 = segArray[j++];
				if (c1 != c2)
					return c1 - c2;
			}
			return len1 - len2;

		}

	}

	/**
	 * A file filter that accepts only XML files.
	 */
	private static class XMLFileFilter implements FileFilter {
		@Override
		public boolean accept(final File f) {
			return f.getName().toLowerCase().endsWith(".xml");
		}
	}

	/**
	 * Returns whether the specified character is a valid character for a
	 * <code>CodeTemplate</code> id.
	 *
	 * @param ch
	 *            The character to check.
	 * @return Whether the character is a valid template character.
	 */
	public static final boolean isValidChar(final char ch) {
		return RSyntaxUtilities.isLetterOrDigit(ch) || ch == '_';
	}

	private final TemplateComparator comparator;
	private File directory;

	private int maxTemplateIDLength;

	private final Segment s;

	private List<CodeTemplate> templates;

	/**
	 * Constructor.
	 */
	public CodeTemplateManager() {
		this.s = new Segment();
		this.comparator = new TemplateComparator();
		this.templates = new ArrayList<>();
	}

	/**
	 * Registers the specified template with this template manager.
	 *
	 * @param template
	 *            The template to register.
	 * @throws IllegalArgumentException
	 *             If <code>template</code> is <code>null</code>.
	 * @see #removeTemplate(CodeTemplate)
	 * @see #removeTemplate(String)
	 */
	public synchronized void addTemplate(final CodeTemplate template) {
		if (template == null)
			throw new IllegalArgumentException("template cannot be null");
		this.templates.add(template);
		this.sortTemplates();
	}

	/**
	 * Returns the template that should be inserted at the current caret position,
	 * assuming the trigger character was pressed.
	 *
	 * @param textArea
	 *            The text area that's getting text inserted into it.
	 * @return A template that should be inserted, if appropriate, or
	 *         <code>null</code> if no template should be inserted.
	 */
	public synchronized CodeTemplate getTemplate(final RSyntaxTextArea textArea) {
		final int caretPos = textArea.getCaretPosition();
		final int charsToGet = Math.min(caretPos, this.maxTemplateIDLength);
		try {
			final Document doc = textArea.getDocument();
			doc.getText(caretPos - charsToGet, charsToGet, this.s);
			@SuppressWarnings("unchecked")
			final int index = Collections.binarySearch(this.templates, this.s, this.comparator);
			return index >= 0 ? (CodeTemplate) this.templates.get(index) : null;
		} catch (final BadLocationException ble) {
			ble.printStackTrace();
			throw new InternalError("Error in CodeTemplateManager");
		}
	}

	/**
	 * Returns the number of templates this manager knows about.
	 *
	 * @return The template count.
	 */
	public synchronized int getTemplateCount() {
		return this.templates.size();
	}

	/**
	 * Returns the templates currently available.
	 *
	 * @return The templates available.
	 */
	public synchronized CodeTemplate[] getTemplates() {
		final CodeTemplate[] temp = new CodeTemplate[this.templates.size()];
		return this.templates.toArray(temp);
	}

	/**
	 * Returns the specified code template.
	 *
	 * @param template
	 *            The template to remove.
	 * @return <code>true</code> if the template was removed, <code>false</code> if
	 *         the template was not in this template manager.
	 * @throws IllegalArgumentException
	 *             If <code>template</code> is <code>null</code>.
	 * @see #removeTemplate(String)
	 * @see #addTemplate(CodeTemplate)
	 */
	public synchronized boolean removeTemplate(final CodeTemplate template) {

		if (template == null)
			throw new IllegalArgumentException("template cannot be null");

		// TODO: Do a binary search
		return this.templates.remove(template);

	}

	/**
	 * Returns the code template with the specified id.
	 *
	 * @param id
	 *            The id to check for.
	 * @return The code template that was removed, or <code>null</code> if there was
	 *         no template with the specified ID.
	 * @throws IllegalArgumentException
	 *             If <code>id</code> is <code>null</code>.
	 * @see #removeTemplate(CodeTemplate)
	 * @see #addTemplate(CodeTemplate)
	 */
	public synchronized CodeTemplate removeTemplate(final String id) {

		if (id == null)
			throw new IllegalArgumentException("id cannot be null");

		// TODO: Do a binary search
		for (final Iterator<CodeTemplate> i = this.templates.iterator(); i.hasNext();) {
			final CodeTemplate template = i.next();
			if (id.equals(template.getID())) {
				i.remove();
				return template;
			}
		}

		return null;

	}

	/**
	 * Replaces the current set of available templates with the ones specified.
	 *
	 * @param newTemplates
	 *            The new set of templates. Note that we will be taking a shallow
	 *            copy of these and sorting them.
	 */
	public synchronized void replaceTemplates(final CodeTemplate[] newTemplates) {
		this.templates.clear();
		if (newTemplates != null)
			for (final CodeTemplate newTemplate : newTemplates)
				this.templates.add(newTemplate);
		this.sortTemplates(); // Also recomputes maxTemplateIDLength.
	}

	/**
	 * Saves all templates as XML files in the current template directory.
	 *
	 * @return Whether or not the save was successful.
	 */
	public synchronized boolean saveTemplates() {

		if (this.templates == null)
			return true;
		if (this.directory == null || !this.directory.isDirectory())
			return false;

		// Blow away all old XML files to start anew, as some might be from
		// templates we're removed from the template manager.
		final File[] oldXMLFiles = this.directory.listFiles(new XMLFileFilter());
		if (oldXMLFiles == null)
			return false; // Either an IOException or it isn't a directory.
		final int count = oldXMLFiles.length;
		for (int i = 0; i < count; i++)
			/* boolean deleted = */oldXMLFiles[i].delete();

		// Save all current templates as XML.
		boolean wasSuccessful = true;
		for (final CodeTemplate template : this.templates) {
			final File xmlFile = new File(this.directory, template.getID() + ".xml");
			try {
				final XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(xmlFile)));
				e.writeObject(template);
				e.close();
			} catch (final IOException ioe) {
				ioe.printStackTrace();
				wasSuccessful = false;
			}
		}

		return wasSuccessful;

	}

	/**
	 * Sets the directory in which to look for templates. Calling this method adds
	 * any new templates found in the specified directory to the templates already
	 * registered.
	 *
	 * @param dir
	 *            The new directory in which to look for templates.
	 * @return The new number of templates in this template manager, or
	 *         <code>-1</code> if the specified directory does not exist.
	 */
	public synchronized int setTemplateDirectory(final File dir) {

		if (dir != null && dir.isDirectory()) {

			this.directory = dir;

			final File[] files = dir.listFiles(new XMLFileFilter());
			final int newCount = files == null ? 0 : files.length;
			final int oldCount = this.templates.size();

			final List<CodeTemplate> temp = new ArrayList<>(oldCount + newCount);
			temp.addAll(this.templates);

			for (int i = 0; i < newCount; i++)
				try {
					final XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(files[i])));
					final Object obj = d.readObject();
					if (!(obj instanceof CodeTemplate)) {
						d.close();
						throw new IOException("Not a CodeTemplate: " + files[i].getAbsolutePath());
					}
					temp.add((CodeTemplate) obj);
					d.close();
				} catch (/* IO, NoSuchElement */final Exception e) {
					// NoSuchElementException can be thrown when reading
					// an XML file not in the format expected by XMLDecoder.
					// (e.g. CodeTemplates in an old format).
					e.printStackTrace();
				}
			this.templates = temp;
			this.sortTemplates();

			return this.getTemplateCount();

		}

		return -1;

	}

	/**
	 * Removes any null entries in the current set of templates (if any), sorts the
	 * remaining templates, and computes the new maximum template ID length.
	 */
	private synchronized void sortTemplates() {

		// Get the maximum length of a template ID.
		this.maxTemplateIDLength = 0;

		// Remove any null entries (should only happen because of
		// IOExceptions, etc. when loading from files), and sort
		// the remaining list.
		for (final Iterator<CodeTemplate> i = this.templates.iterator(); i.hasNext();) {
			final CodeTemplate temp = i.next();
			if (temp == null || temp.getID() == null)
				i.remove();
			else
				this.maxTemplateIDLength = Math.max(this.maxTemplateIDLength, temp.getID().length());
		}

		Collections.sort(this.templates);

	}

}