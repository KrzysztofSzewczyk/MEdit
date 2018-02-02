/*
 * 11/25/2008
 *
 * TextEditorPane.java - A syntax highlighting text area that has knowledge of
 * the file it is editing on disk.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;

import org.fife.io.UnicodeReader;
import org.fife.io.UnicodeWriter;
import org.fife.ui.rtextarea.RTextArea;

/**
 * An extension of {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea} that adds
 * information about the file being edited, such as:
 *
 * <ul>
 * <li>Its name and location.
 * <li>Is it dirty?
 * <li>Is it read-only?
 * <li>The last time it was loaded or saved to disk (local files only).
 * <li>The file's encoding on disk.
 * <li>Easy access to the line separator.
 * </ul>
 *
 * Loading and saving is also built into the editor.
 * <p>
 *
 * When saving UTF-8 files, whether or not a BOM is written is controlled by the
 * {@link UnicodeWriter} class. Use
 * {@link UnicodeWriter#setWriteUtf8BOM(boolean)} to toggle writing BOMs for
 * UTF-8 files.
 * <p>
 *
 * Both local and remote files (e.g. ftp) are supported. See the
 * {@link FileLocation} class for more information.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see FileLocation
 */
public class TextEditorPane extends RSyntaxTextArea implements DocumentListener {

	/**
	 * The default name given to files if none is specified in a constructor.
	 */
	private static final String DEFAULT_FILE_NAME = "Untitled.txt";

	/**
	 * Property change event fired when the text area's dirty flag changes.
	 *
	 * @see #setDirty(boolean)
	 */
	public static final String DIRTY_PROPERTY = "TextEditorPane.dirty";

	/**
	 * Property change event fired when the text area's encoding changes.
	 *
	 * @see #setEncoding(String)
	 */
	public static final String ENCODING_PROPERTY = "TextEditorPane.encoding";

	/**
	 * Property change event fired when the file path this text area references is
	 * updated.
	 *
	 * @see #load(FileLocation, String)
	 * @see #saveAs(FileLocation)
	 */
	public static final String FULL_PATH_PROPERTY = "TextEditorPane.fileFullPath";

	/**
	 * The value returned by {@link #getLastSaveOrLoadTime()} for remote files.
	 */
	public static final long LAST_MODIFIED_UNKNOWN = 0;

	/**
	 * Property change event fired when the text area should be treated as
	 * read-only, and previously it should not, or vice-versa.
	 *
	 * @see #setReadOnly(boolean)
	 */
	public static final String READ_ONLY_PROPERTY = "TextEditorPane.readOnly";

	private static final long serialVersionUID = 1L;

	/**
	 * Returns the default encoding for this operating system.
	 *
	 * @return The default encoding.
	 */
	private static String getDefaultEncoding() {
		// NOTE: The "file.encoding" system property is not guaranteed to be
		// set by the spec, so we cannot rely on it.
		String encoding = Charset.defaultCharset().name();
		if (encoding == null)
			try {
				final File f = File.createTempFile("rsta", null);
				final FileWriter w = new FileWriter(f);
				encoding = w.getEncoding();
				w.close();
				f.deleteOnExit();// delete(); Keep FindBugs happy
			} catch (final IOException ioe) {
				encoding = "US-ASCII";
			}
		return encoding;
	}

	public static void main(final String[] args) throws Exception {
		try {
			final TextEditorPane textArea = new TextEditorPane();
			textArea.load(FileLocation.create("d:/temp/test.txt"), "UTF-8");
			final JPanel cp = new JPanel();
			cp.setPreferredSize(new java.awt.Dimension(300, 300));
			cp.setLayout(new java.awt.BorderLayout());
			cp.add(new JScrollPane(textArea));
			final JFrame frame = new JFrame();
			frame.setContentPane(cp);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The charset to use when reading or writing this file.
	 */
	private String charSet;

	/**
	 * Whether the file is dirty.
	 */
	private boolean dirty;

	/**
	 * The last time this file was modified on disk, for local files. For remote
	 * files, this value should always be {@link #LAST_MODIFIED_UNKNOWN}.
	 */
	private long lastSaveOrLoadTime;

	/**
	 * The location of the file being edited.
	 */
	private FileLocation loc;

	/**
	 * Whether the file should be treated as read-only.
	 */
	private boolean readOnly;

	/**
	 * Constructor. The file will be given a default name.
	 */
	public TextEditorPane() {
		this(RTextArea.INSERT_MODE);
	}

	/**
	 * Constructor. The file will be given a default name.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 */
	public TextEditorPane(final int textMode) {
		this(textMode, false);
	}

	/**
	 * Creates a new <code>TextEditorPane</code>. The file will be given a default
	 * name.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled
	 *            Whether or not to use word wrap in this pane.
	 */
	public TextEditorPane(final int textMode, final boolean wordWrapEnabled) {
		super(textMode);
		this.setLineWrap(wordWrapEnabled);
		try {
			this.init(null, null);
		} catch (final IOException ioe) { // Never happens
			ioe.printStackTrace();
		}
	}

	/**
	 * Creates a new <code>TextEditorPane</code>.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled
	 *            Whether or not to use word wrap in this pane.
	 * @param loc
	 *            The location of the text file being edited. If this value is
	 *            <code>null</code>, a file named "Untitled.txt" in the current
	 *            directory is used.
	 * @throws IOException
	 *             If an IO error occurs reading the file at <code>loc</code>. This
	 *             of course won't happen if <code>loc</code> is <code>null</code>.
	 */
	public TextEditorPane(final int textMode, final boolean wordWrapEnabled, final FileLocation loc)
			throws IOException {
		this(textMode, wordWrapEnabled, loc, null);
	}

	/**
	 * Creates a new <code>TextEditorPane</code>.
	 *
	 * @param textMode
	 *            Either <code>INSERT_MODE</code> or <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled
	 *            Whether or not to use word wrap in this pane.
	 * @param loc
	 *            The location of the text file being edited. If this value is
	 *            <code>null</code>, a file named "Untitled.txt" in the current
	 *            directory is used. This file is displayed as empty even if it
	 *            actually exists.
	 * @param defaultEnc
	 *            The default encoding to use when opening the file, if the file is
	 *            not Unicode. If this value is <code>null</code>, a system default
	 *            value is used.
	 * @throws IOException
	 *             If an IO error occurs reading the file at <code>loc</code>. This
	 *             of course won't happen if <code>loc</code> is <code>null</code>.
	 */
	public TextEditorPane(final int textMode, final boolean wordWrapEnabled, final FileLocation loc,
			final String defaultEnc) throws IOException {
		super(textMode);
		this.setLineWrap(wordWrapEnabled);
		this.init(loc, defaultEnc);
	}

	/**
	 * Callback for when styles in the current document change. This method is never
	 * called.
	 *
	 * @param e
	 *            The document event.
	 */
	@Override
	public void changedUpdate(final DocumentEvent e) {
	}

	/**
	 * Returns the encoding to use when reading or writing this file.
	 *
	 * @return The encoding.
	 * @see #setEncoding(String)
	 */
	public String getEncoding() {
		return this.charSet;
	}

	/**
	 * Returns the full path to this document.
	 *
	 * @return The full path to the document.
	 */
	public String getFileFullPath() {
		return this.loc == null ? null : this.loc.getFileFullPath();
	}

	/**
	 * Returns the file name of this document.
	 *
	 * @return The file name.
	 */
	public String getFileName() {
		return this.loc == null ? null : this.loc.getFileName();
	}

	/**
	 * Returns the timestamp for when this file was last loaded or saved <em>by this
	 * editor pane</em>. If the file has been modified on disk by another process
	 * after it was loaded into this editor pane, this method will not return the
	 * actual file's last modified time.
	 * <p>
	 *
	 * For remote files, this method will always return
	 * {@link #LAST_MODIFIED_UNKNOWN}.
	 *
	 * @return The timestamp when this file was last loaded or saved by this editor
	 *         pane, if it is a local file, or {@link #LAST_MODIFIED_UNKNOWN} if it
	 *         is a remote file.
	 * @see #isModifiedOutsideEditor()
	 */
	public long getLastSaveOrLoadTime() {
		return this.lastSaveOrLoadTime;
	}

	/**
	 * Returns the line separator used when writing this file (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>", or "<code>\r</code>").
	 * <p>
	 *
	 * Note that this value is an <code>Object</code> and not a <code>String</code>
	 * as that is the way the {@link Document} interface defines its property
	 * values. If you always use {@link #setLineSeparator(String)} to modify this
	 * value, then the value returned from this method will always be a
	 * <code>String</code>.
	 *
	 * @return The line separator. If this value is <code>null</code>, then the
	 *         system default line separator is used (usually the value of
	 *         <code>System.getProperty("line.separator")</code>).
	 * @see #setLineSeparator(String)
	 * @see #setLineSeparator(String, boolean)
	 */
	public Object getLineSeparator() {
		return this.getDocument().getProperty(DefaultEditorKit.EndOfLineStringProperty);
	}

	/**
	 * Initializes this editor with the specified file location.
	 *
	 * @param loc
	 *            The file location. If this is <code>null</code>, a default
	 *            location is used and an empty file is displayed.
	 * @param defaultEnc
	 *            The default encoding to use when opening the file, if the file is
	 *            not Unicode. If this value is <code>null</code>, a system default
	 *            value is used.
	 * @throws IOException
	 *             If an IO error occurs reading from <code>loc</code>. If
	 *             <code>loc</code> is <code>null</code>, this cannot happen.
	 */
	private void init(final FileLocation loc, final String defaultEnc) throws IOException {

		if (loc == null) {
			// Don't call load() just in case Untitled.txt actually exists,
			// just to ensure there is no chance of an IOException being thrown
			// in the default case.
			this.loc = FileLocation.create(TextEditorPane.DEFAULT_FILE_NAME);
			this.charSet = defaultEnc == null ? TextEditorPane.getDefaultEncoding() : defaultEnc;
			// Ensure that line separator always has a value, even if the file
			// does not exist (or is the "default" file). This makes life
			// easier for host applications that want to display this value.
			this.setLineSeparator(System.getProperty("line.separator"));
		} else
			this.load(loc, defaultEnc); // Sets this.loc

		if (this.loc.isLocalAndExists()) {
			final File file = new File(this.loc.getFileFullPath());
			this.lastSaveOrLoadTime = file.lastModified();
			this.setReadOnly(!file.canWrite());
		} else {
			this.lastSaveOrLoadTime = TextEditorPane.LAST_MODIFIED_UNKNOWN;
			this.setReadOnly(false);
		}

		this.setDirty(false);

	}

	/**
	 * Callback for when text is inserted into the document.
	 *
	 * @param e
	 *            Information on the insertion.
	 */
	@Override
	public void insertUpdate(final DocumentEvent e) {
		if (!this.dirty)
			this.setDirty(true);
	}

	/**
	 * Returns whether or not the text in this editor has unsaved changes.
	 *
	 * @return Whether or not the text has unsaved changes.
	 * @see #setDirty(boolean)
	 */
	public boolean isDirty() {
		return this.dirty;
	}

	/**
	 * Returns whether this file is a local file.
	 *
	 * @return Whether this is a local file.
	 */
	public boolean isLocal() {
		return this.loc.isLocal();
	}

	/**
	 * Returns whether this is a local file that already exists.
	 *
	 * @return Whether this is a local file that already exists.
	 */
	public boolean isLocalAndExists() {
		return this.loc.isLocalAndExists();
	}

	/**
	 * Returns whether the text file has been modified outside of this editor since
	 * the last load or save operation. Note that if this is a remote file, this
	 * method will always return <code>false</code>.
	 * <p>
	 *
	 * This method may be used by applications to implement a reloading feature,
	 * where the user is prompted to reload a file if it has been modified since
	 * their last open or save.
	 *
	 * @return Whether the text file has been modified outside of this editor.
	 * @see #getLastSaveOrLoadTime()
	 */
	public boolean isModifiedOutsideEditor() {
		return this.loc.getActualLastModified() > this.getLastSaveOrLoadTime();
	}

	/**
	 * Returns whether or not the text area should be treated as read-only.
	 *
	 * @return Whether or not the text area should be treated as read-only.
	 * @see #setReadOnly(boolean)
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * Loads the specified file in this editor. This method fires a property change
	 * event of type {@link #FULL_PATH_PROPERTY}.
	 *
	 * @param loc
	 *            The location of the file to load. This cannot be
	 *            <code>null</code>.
	 * @param defaultEnc
	 *            The encoding to use when loading/saving the file. This encoding
	 *            will only be used if the file is not Unicode. If this value is
	 *            <code>null</code>, the system default encoding is used.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #save()
	 * @see #saveAs(FileLocation)
	 */
	public void load(final FileLocation loc, final String defaultEnc) throws IOException {

		// For new local files, just go with it.
		if (loc.isLocal() && !loc.isLocalAndExists()) {
			this.charSet = defaultEnc != null ? defaultEnc : TextEditorPane.getDefaultEncoding();
			this.loc = loc;
			this.setText(null);
			this.discardAllEdits();
			this.setDirty(false);
			return;
		}

		// Old local files and remote files, load 'em up. UnicodeReader will
		// check for BOMs and handle them correctly in all cases, then pass
		// rest of stream down to InputStreamReader.
		final UnicodeReader ur = new UnicodeReader(loc.getInputStream(), defaultEnc);

		// Remove listener so dirty flag doesn't get set when loading a file.
		final Document doc = this.getDocument();
		doc.removeDocumentListener(this);
		final BufferedReader r = new BufferedReader(ur);
		try {
			this.read(r, null);
		} finally {
			doc.addDocumentListener(this);
			r.close();
		}

		// No IOException thrown, so we can finally change the location.
		this.charSet = ur.getEncoding();
		final String old = this.getFileFullPath();
		this.loc = loc;
		this.setDirty(false);
		this.setCaretPosition(0);
		this.discardAllEdits();
		this.firePropertyChange(TextEditorPane.FULL_PATH_PROPERTY, old, this.getFileFullPath());

	}

	/**
	 * Reloads this file from disk. The file must exist for this operation to not
	 * throw an exception.
	 * <p>
	 *
	 * The file's "dirty" state will be set to <code>false</code> after this
	 * operation. If this is a local file, its "last modified" time is updated to
	 * reflect that of the actual file.
	 * <p>
	 *
	 * Note that if the file has been modified on disk, and is now a Unicode
	 * encoding when before it wasn't (or if it is a different Unicode now), this
	 * will cause this {@link TextEditorPane}'s encoding to change. Otherwise, the
	 * file's encoding will stay the same.
	 *
	 * @throws IOException
	 *             If the file does not exist, or if an IO error occurs reading the
	 *             file.
	 * @see #isLocalAndExists()
	 */
	public void reload() throws IOException {
		final String oldEncoding = this.getEncoding();
		final UnicodeReader ur = new UnicodeReader(this.loc.getInputStream(), oldEncoding);
		final String encoding = ur.getEncoding();
		final BufferedReader r = new BufferedReader(ur);
		try {
			this.read(r, null); // Dumps old contents.
		} finally {
			r.close();
		}
		this.setEncoding(encoding);
		this.setDirty(false);
		this.syncLastSaveOrLoadTimeToActualFile();
		this.discardAllEdits(); // Prevent user from being able to undo the reload
	}

	/**
	 * Called whenever text is removed from this editor.
	 *
	 * @param e
	 *            The document event.
	 */
	@Override
	public void removeUpdate(final DocumentEvent e) {
		if (!this.dirty)
			this.setDirty(true);
	}

	/**
	 * Saves the file in its current encoding.
	 * <p>
	 *
	 * The text area's "dirty" state is set to <code>false</code>, and if this is a
	 * local file, its "last modified" time is updated.
	 *
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #saveAs(FileLocation)
	 * @see #load(FileLocation, String)
	 */
	public void save() throws IOException {
		this.saveImpl(this.loc);
		this.setDirty(false);
		this.syncLastSaveOrLoadTimeToActualFile();
	}

	/**
	 * Saves this file in a new local location. This method fires a property change
	 * event of type {@link #FULL_PATH_PROPERTY}.
	 *
	 * @param loc
	 *            The location to save to.
	 * @throws IOException
	 *             If an IO error occurs.
	 * @see #save()
	 * @see #load(FileLocation, String)
	 */
	public void saveAs(final FileLocation loc) throws IOException {
		this.saveImpl(loc);
		// No exception thrown - we can "rename" the file.
		final String old = this.getFileFullPath();
		this.loc = loc;
		this.setDirty(false);
		this.lastSaveOrLoadTime = loc.getActualLastModified();
		this.firePropertyChange(TextEditorPane.FULL_PATH_PROPERTY, old, this.getFileFullPath());
	}

	/**
	 * Saves the text in this editor to the specified location.
	 *
	 * @param loc
	 *            The location to save to.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private void saveImpl(final FileLocation loc) throws IOException {
		final OutputStream out = loc.getOutputStream();
		final BufferedWriter w = new BufferedWriter(new UnicodeWriter(out, this.getEncoding()));
		try {
			this.write(w);
		} finally {
			w.close();
		}
	}

	/**
	 * Sets whether or not this text in this editor has unsaved changes. This fires
	 * a property change event of type {@link #DIRTY_PROPERTY}.
	 * <p>
	 *
	 * Applications will usually have no need to call this method directly; the only
	 * time you might have a need to call this method directly is if you have to
	 * initialize an instance of TextEditorPane with content that does not come from
	 * a file. <code>TextEditorPane</code> automatically sets its own dirty flag
	 * when its content is edited, when its encoding is changed, or when its line
	 * ending property is changed. It is cleared whenever <code>load()</code>,
	 * <code>reload()</code>, <code>save()</code>, or <code>saveAs()</code> are
	 * called.
	 *
	 * @param dirty
	 *            Whether or not the text has been modified.
	 * @see #isDirty()
	 */
	public void setDirty(final boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			this.firePropertyChange(TextEditorPane.DIRTY_PROPERTY, !dirty, dirty);
		}
	}

	/**
	 * Sets the document for this editor.
	 *
	 * @param doc
	 *            The new document.
	 */
	@Override
	public void setDocument(final Document doc) {
		final Document old = this.getDocument();
		if (old != null)
			old.removeDocumentListener(this);
		super.setDocument(doc);
		doc.addDocumentListener(this);
	}

	/**
	 * Sets the encoding to use when reading or writing this file. This method sets
	 * the editor's dirty flag when the encoding is changed, and fires a property
	 * change event of type {@link #ENCODING_PROPERTY}.
	 *
	 * @param encoding
	 *            The new encoding.
	 * @throws UnsupportedCharsetException
	 *             If the encoding is not supported.
	 * @throws NullPointerException
	 *             If <code>encoding</code> is <code>null</code>.
	 * @see #getEncoding()
	 */
	public void setEncoding(final String encoding) {
		if (encoding == null)
			throw new NullPointerException("encoding cannot be null");
		else if (!Charset.isSupported(encoding))
			throw new UnsupportedCharsetException(encoding);
		if (this.charSet == null || !this.charSet.equals(encoding)) {
			final String oldEncoding = this.charSet;
			this.charSet = encoding;
			this.firePropertyChange(TextEditorPane.ENCODING_PROPERTY, oldEncoding, this.charSet);
			this.setDirty(true);
		}
	}

	/**
	 * Sets the line separator sequence to use when this file is saved (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>").
	 *
	 * Besides parameter checking, this method is preferred over
	 * <code>getDocument().putProperty()</code> because it sets the editor's dirty
	 * flag when the line separator is changed.
	 *
	 * @param separator
	 *            The new line separator.
	 * @throws NullPointerException
	 *             If <code>separator</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             If <code>separator</code> is not one of "<code>\n</code>",
	 *             "<code>\r\n</code>" or "<code>\r</code>".
	 * @see #getLineSeparator()
	 */
	public void setLineSeparator(final String separator) {
		this.setLineSeparator(separator, true);
	}

	/**
	 * Sets the line separator sequence to use when this file is saved (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>").
	 *
	 * Besides parameter checking, this method is preferred over
	 * <code>getDocument().putProperty()</code> because can set the editor's dirty
	 * flag when the line separator is changed.
	 *
	 * @param separator
	 *            The new line separator.
	 * @param setDirty
	 *            Whether the dirty flag should be set if the line separator is
	 *            changed.
	 * @throws NullPointerException
	 *             If <code>separator</code> is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             If <code>separator</code> is not one of "<code>\n</code>",
	 *             "<code>\r\n</code>" or "<code>\r</code>".
	 * @see #getLineSeparator()
	 */
	public void setLineSeparator(final String separator, final boolean setDirty) {
		if (separator == null)
			throw new NullPointerException("terminator cannot be null");
		if (!"\r\n".equals(separator) && !"\n".equals(separator) && !"\r".equals(separator))
			throw new IllegalArgumentException("Invalid line terminator");
		final Document doc = this.getDocument();
		final Object old = doc.getProperty(DefaultEditorKit.EndOfLineStringProperty);
		if (!separator.equals(old)) {
			doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, separator);
			if (setDirty)
				this.setDirty(true);
		}
	}

	/**
	 * Sets whether or not this text area should be treated as read-only. This fires
	 * a property change event of type {@link #READ_ONLY_PROPERTY}.
	 *
	 * @param readOnly
	 *            Whether or not the document is read-only.
	 * @see #isReadOnly()
	 */
	public void setReadOnly(final boolean readOnly) {
		if (this.readOnly != readOnly) {
			this.readOnly = readOnly;
			this.firePropertyChange(TextEditorPane.READ_ONLY_PROPERTY, !readOnly, readOnly);
		}
	}

	/**
	 * Syncs this text area's "last saved or loaded" time to that of the file being
	 * edited, if that file is local and exists. If the file is remote or is local
	 * but does not yet exist, nothing happens.
	 * <p>
	 *
	 * You normally do not have to call this method, as the "last saved or loaded"
	 * time for {@link TextEditorPane}s is kept up-to-date internally during such
	 * operations as {@link #save()}, {@link #reload()}, etc.
	 *
	 * @see #getLastSaveOrLoadTime()
	 * @see #isModifiedOutsideEditor()
	 */
	public void syncLastSaveOrLoadTimeToActualFile() {
		if (this.loc.isLocalAndExists())
			this.lastSaveOrLoadTime = this.loc.getActualLastModified();
	}
}