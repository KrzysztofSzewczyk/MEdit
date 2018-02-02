/*
 * 07/28/2008
 *
 * RtfTransferable.java - Used during drag-and-drop to represent RTF text.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Object used during copy/paste and DnD operations to represent RTF text. It
 * can return the text being moved as either RTF or plain text. This class is
 * basically the same as <code>java.awt.datatransfer.StringSelection</code>,
 * except that it can also return the text as RTF.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RtfTransferable implements Transferable {

	/**
	 * The "flavors" the text can be returned as.
	 */
	private static final DataFlavor[] FLAVORS = { new DataFlavor("text/rtf", "RTF"), DataFlavor.stringFlavor,
			DataFlavor.plainTextFlavor // deprecated
	};

	/**
	 * The RTF data, in bytes (the RTF is 7-bit ascii).
	 */
	private final byte[] data;

	/**
	 * Constructor.
	 *
	 * @param data
	 *            The RTF data.
	 */
	RtfTransferable(final byte[] data) {
		this.data = data;
	}

	@Override
	public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor.equals(RtfTransferable.FLAVORS[0]))
			return new ByteArrayInputStream(this.data == null ? new byte[0] : this.data);
		else if (flavor.equals(RtfTransferable.FLAVORS[1]))
			return this.data == null ? "" : RtfToText.getPlainText(this.data);
		else if (flavor.equals(RtfTransferable.FLAVORS[2])) { // plainTextFlavor (deprecated)
			String text = ""; // Valid if data==null
			if (this.data != null)
				text = RtfToText.getPlainText(this.data);
			return new StringReader(text);
		} else
			throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return RtfTransferable.FLAVORS.clone();
	}

	@Override
	public boolean isDataFlavorSupported(final DataFlavor flavor) {
		for (final DataFlavor element : RtfTransferable.FLAVORS)
			if (flavor.equals(element))
				return true;
		return false;
	}

}