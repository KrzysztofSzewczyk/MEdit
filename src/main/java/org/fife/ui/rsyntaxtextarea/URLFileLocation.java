/*
 * 11/13/2008
 *
 * URLFileLocation.java - The location of a file at a (remote) URL.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * The location of a file at a (remote) URL.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class URLFileLocation extends FileLocation {

	/**
	 * A prettied-up full path of the URL (password removed, etc.).
	 */
	private final String fileFullPath;

	/**
	 * A prettied-up filename (leading slash, and possibly "<code>%2F</code>",
	 * removed).
	 */
	private final String fileName;

	/**
	 * URL of the remote file.
	 */
	private final URL url;

	/**
	 * Constructor.
	 *
	 * @param url
	 *            The URL of the file.
	 */
	URLFileLocation(final URL url) {
		this.url = url;
		this.fileFullPath = this.createFileFullPath();
		this.fileName = this.createFileName();
	}

	/**
	 * Creates a "prettied-up" URL to use. This will be stripped of sensitive
	 * information such as passwords.
	 *
	 * @return The full path to use.
	 */
	private String createFileFullPath() {
		String fullPath = this.url.toString();
		fullPath = fullPath.replaceFirst("://([^:]+)(?:.+)@", "://$1@");
		return fullPath;
	}

	/**
	 * Creates the "prettied-up" filename to use.
	 *
	 * @return The base name of the file of this URL.
	 */
	private String createFileName() {
		String fileName = this.url.getPath();
		if (fileName.startsWith("/%2F/"))
			fileName = fileName.substring(4);
		else if (fileName.startsWith("/"))
			fileName = fileName.substring(1);
		return fileName;
	}

	/**
	 * Returns the last time this file was modified, or
	 * {@link TextEditorPane#LAST_MODIFIED_UNKNOWN} if this value cannot be computed
	 * (such as for a remote file).
	 *
	 * @return The last time this file was modified. This will always be
	 *         {@link TextEditorPane#LAST_MODIFIED_UNKNOWN} for URL's.
	 */
	@Override
	protected long getActualLastModified() {
		return TextEditorPane.LAST_MODIFIED_UNKNOWN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileFullPath() {
		return this.fileFullPath;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName() {
		return this.fileName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected InputStream getInputStream() throws IOException {
		return this.url.openStream();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected OutputStream getOutputStream() throws IOException {
		return this.url.openConnection().getOutputStream();
	}

	/**
	 * Returns whether this file location is a local file.
	 *
	 * @return Whether this is a local file.
	 * @see #isLocalAndExists()
	 */
	@Override
	public boolean isLocal() {
		return "file".equalsIgnoreCase(this.url.getProtocol());
	}

	/**
	 * Returns whether this file location is a local file and already exists. This
	 * method always returns <code>false</code> since we cannot check this value
	 * easily.
	 *
	 * @return <code>false</code> always.
	 * @see #isLocal()
	 */
	@Override
	public boolean isLocalAndExists() {
		return false;
	}

}