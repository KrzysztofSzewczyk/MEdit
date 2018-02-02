/*
 * 07/29/2004
 *
 * RTATextTransferHandler.java - Handles the transfer of data to/from an
 * RTextArea via drag-and-drop.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.im.InputContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

/**
 * Handles the transfer of data to/from an <code>RTextArea</code> via
 * drag-and-drop. This class is pretty much ripped off from a subclass of
 * <code>BasicTextUI</code>. In the future, it will include the ability to
 * drag-and-drop files into <code>RTextArea</code>s (i.e., the text will be
 * inserted into the text area).
 * <p>
 *
 * The main reason this class is kept around is so we can subclass it.
 *
 * @author Robert Futrell
 * @version 0.1
 */
@SuppressWarnings("deprecation")
public class RTATextTransferHandler extends TransferHandler {

	/**
	 * A possible implementation of the Transferable interface for RTextAreas.
	 */
	static class TextTransferable implements Transferable {

		private static DataFlavor[] plainFlavors;
		private static DataFlavor[] stringFlavors;
		// Initialization of supported flavors.
		static {
			try {

				TextTransferable.plainFlavors = new DataFlavor[3];
				TextTransferable.plainFlavors[0] = new DataFlavor("text/plain;class=java.lang.String");
				TextTransferable.plainFlavors[1] = new DataFlavor("text/plain;class=java.io.Reader");
				TextTransferable.plainFlavors[2] = new DataFlavor(
						"text/plain;charset=unicode;class=java.io.InputStream");

				TextTransferable.stringFlavors = new DataFlavor[2];
				TextTransferable.stringFlavors[0] = new DataFlavor(
						DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.String");
				TextTransferable.stringFlavors[1] = DataFlavor.stringFlavor;

			} catch (final ClassNotFoundException cle) {
				System.err.println("Error initializing org.fife.ui.RTATextTransferHandler");
			}
		}

		private final JTextComponent c;

		private Position p0;
		private Position p1;

		protected String plainData;

		TextTransferable(final JTextComponent c, final int start, final int end) {
			this.c = c;
			final Document doc = c.getDocument();
			try {
				this.p0 = doc.createPosition(start);
				this.p1 = doc.createPosition(end);
				this.plainData = c.getSelectedText();
			} catch (final BadLocationException ble) {
			}
		}

		/**
		 * Fetch the data in a text/plain format.
		 */
		protected String getPlainData() {
			return this.plainData;
		}

		/**
		 * Returns an object which represents the data to be transferred. The class of
		 * the object returned is defined by the representation class of the flavor.
		 *
		 * @param flavor
		 *            the requested flavor for the data
		 * @see DataFlavor#getRepresentationClass
		 * @exception IOException
		 *                if the data is no longer available in the requested flavor.
		 * @exception UnsupportedFlavorException
		 *                if the requested data flavor is not supported.
		 */
		@Override
		public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (this.isPlainFlavor(flavor)) {
				String data = this.getPlainData();
				data = data == null ? "" : data;
				if (String.class.equals(flavor.getRepresentationClass()))
					return data;
				else if (Reader.class.equals(flavor.getRepresentationClass()))
					return new StringReader(data);
				else if (InputStream.class.equals(flavor.getRepresentationClass()))
					return new StringBufferInputStream(data);
			} else if (this.isStringFlavor(flavor)) {
				String data = this.getPlainData();
				data = data == null ? "" : data;
				return data;
			}
			throw new UnsupportedFlavorException(flavor);
		}

		/**
		 * Returns an array of DataFlavor objects indicating the flavors the data can be
		 * provided in. The array should be ordered according to preference for
		 * providing the data (from most richly descriptive to least descriptive).
		 *
		 * @return an array of data flavors in which this data can be transferred
		 */
		@Override
		public DataFlavor[] getTransferDataFlavors() {

			final int plainCount = this.isPlainSupported() ? TextTransferable.plainFlavors.length : 0;
			final int stringCount = this.isPlainSupported() ? TextTransferable.stringFlavors.length : 0;
			final int totalCount = plainCount + stringCount;
			final DataFlavor[] flavors = new DataFlavor[totalCount];

			// fill in the array
			int pos = 0;
			if (plainCount > 0) {
				System.arraycopy(TextTransferable.plainFlavors, 0, flavors, pos, plainCount);
				pos += plainCount;
			}
			if (stringCount > 0)
				System.arraycopy(TextTransferable.stringFlavors, 0, flavors, pos, stringCount);
			// pos += stringCount;

			return flavors;

		}

		/**
		 * Returns whether or not the specified data flavor is supported for this
		 * object.
		 *
		 * @param flavor
		 *            the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		@Override
		public boolean isDataFlavorSupported(final DataFlavor flavor) {
			final DataFlavor[] flavors = this.getTransferDataFlavors();
			for (final DataFlavor flavor2 : flavors)
				if (flavor2.equals(flavor))
					return true;
			return false;
		}

		/**
		 * Returns whether or not the specified data flavor is an plain flavor that is
		 * supported.
		 *
		 * @param flavor
		 *            the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		protected boolean isPlainFlavor(final DataFlavor flavor) {
			final DataFlavor[] flavors = TextTransferable.plainFlavors;
			for (final DataFlavor flavor2 : flavors)
				if (flavor2.equals(flavor))
					return true;
			return false;
		}

		/**
		 * Should the plain text flavors be offered? If so, the method getPlainData
		 * should be implemented to provide something reasonable.
		 */
		protected boolean isPlainSupported() {
			return this.plainData != null;
		}

		/**
		 * Returns whether or not the specified data flavor is a String flavor that is
		 * supported.
		 *
		 * @param flavor
		 *            the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		protected boolean isStringFlavor(final DataFlavor flavor) {
			final DataFlavor[] flavors = TextTransferable.stringFlavors;
			for (final DataFlavor flavor2 : flavors)
				if (flavor2.equals(flavor))
					return true;
			return false;
		}

		void removeText() {
			if (this.p0 != null && this.p1 != null && this.p0.getOffset() != this.p1.getOffset())
				try {
					final Document doc = this.c.getDocument();
					doc.remove(this.p0.getOffset(), this.p1.getOffset() - this.p0.getOffset());
				} catch (final BadLocationException e) {
				}
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private JTextComponent exportComp;
	private int p0;
	private int p1;
	private boolean shouldRemove;

	private boolean withinSameComponent;

	/**
	 * This method indicates if a component would accept an import of the given set
	 * of data flavors prior to actually attempting to import it.
	 *
	 * @param comp
	 *            The component to receive the transfer. This argument is provided
	 *            to enable sharing of TransferHandlers by multiple components.
	 * @param flavors
	 *            The data formats available.
	 * @return <code>true</code> iff the data can be inserted.
	 */
	@Override
	public boolean canImport(final JComponent comp, final DataFlavor[] flavors) {
		final JTextComponent c = (JTextComponent) comp;
		if (!(c.isEditable() && c.isEnabled()))
			return false;
		return this.getImportFlavor(flavors, c) != null;
	}

	/**
	 * Create a Transferable to use as the source for a data transfer.
	 *
	 * @param comp
	 *            The component holding the data to be transfered. This argument is
	 *            provided to enable sharing of TransferHandlers by multiple
	 *            components.
	 * @return The representation of the data to be transfered.
	 *
	 */
	@Override
	protected Transferable createTransferable(final JComponent comp) {
		this.exportComp = (JTextComponent) comp;
		this.shouldRemove = true;
		this.p0 = this.exportComp.getSelectionStart();
		this.p1 = this.exportComp.getSelectionEnd();
		return this.p0 != this.p1 ? new TextTransferable(this.exportComp, this.p0, this.p1) : null;
	}

	/**
	 * This method is called after data has been exported. This method should remove
	 * the data that was transfered if the action was MOVE.
	 *
	 * @param source
	 *            The component that was the source of the data.
	 * @param data
	 *            The data that was transferred or possibly null if the action is
	 *            <code>NONE</code>.
	 * @param action
	 *            The actual action that was performed.
	 */
	@Override
	protected void exportDone(final JComponent source, final Transferable data, final int action) {
		// only remove the text if shouldRemove has not been set to
		// false by importData and only if the action is a move
		if (this.shouldRemove && action == TransferHandler.MOVE) {
			final TextTransferable t = (TextTransferable) data;
			t.removeText();
			if (this.withinSameComponent) {
				((RTextArea) source).endAtomicEdit();
				this.withinSameComponent = false;
			}
		}
		this.exportComp = null;
		if (data instanceof TextTransferable)
			ClipboardHistory.get().add(((TextTransferable) data).getPlainData());
	}

	/**
	 * Try to find a flavor that can be used to import a Transferable to a specified
	 * text component. The set of usable flavors are tried in the following order:
	 * <ol>
	 * <li>First, an attempt is made to find a flavor matching the content tyep of
	 * the EditorKit for the component.
	 * <li>Second, an attempt to find a text/plain flavor is made.
	 * <li>Third, an attempt to find a flavor representing a String reference in the
	 * same VM is made.
	 * <li>Lastly, DataFlavor.stringFlavor is searched for.
	 * </ol>
	 *
	 * @param flavors
	 *            The flavors to check if c will accept them.
	 * @param c
	 *            The text component to see whether it will accept any of the
	 *            specified data flavors as input.
	 */
	protected DataFlavor getImportFlavor(final DataFlavor[] flavors, final JTextComponent c) {

		DataFlavor refFlavor = null;
		DataFlavor stringFlavor = null;

		for (final DataFlavor flavor : flavors) {

			final String mime = flavor.getMimeType();
			if (mime.startsWith("text/plain"))
				return flavor;
			else if (refFlavor == null && mime.startsWith("application/x-java-jvm-local-objectref")
					&& flavor.getRepresentationClass() == String.class)
				refFlavor = flavor;
			else if (stringFlavor == null && flavor.equals(DataFlavor.stringFlavor))
				stringFlavor = flavor;

		}

		if (refFlavor != null)
			return refFlavor;
		else if (stringFlavor != null)
			return stringFlavor;

		return null;

	}

	/**
	 * This is the type of transfer actions supported by the source. Some models are
	 * not mutable, so a transfer operation of COPY only should be advertised in
	 * that case.
	 *
	 * @param c
	 *            The component holding the data to be transfered. This argument is
	 *            provided to enable sharing of TransferHandlers by multiple
	 *            components.
	 * @return If the text component is editable, COPY_OR_MOVE is returned,
	 *         otherwise just COPY is allowed.
	 */
	@Override
	public int getSourceActions(final JComponent c) {
		if (((JTextComponent) c).isEditable())
			return TransferHandler.COPY_OR_MOVE;
		else
			return TransferHandler.COPY;
	}

	/**
	 * Import the given stream data into the text component.
	 */
	protected void handleReaderImport(final Reader in, final JTextComponent c) throws IOException {

		final char[] buff = new char[1024];
		int nch;
		boolean lastWasCR = false;
		int last;
		StringBuilder sbuff = null;

		// Read in a block at a time, mapping \r\n to \n, as well as single
		// \r to \n.
		while ((nch = in.read(buff, 0, buff.length)) != -1) {

			if (sbuff == null)
				sbuff = new StringBuilder(nch);
			last = 0;

			for (int counter = 0; counter < nch; counter++)
				switch (buff[counter]) {
				case '\r':
					if (lastWasCR) {
						if (counter == 0)
							sbuff.append('\n');
						else
							buff[counter - 1] = '\n';
					} else
						lastWasCR = true;
					break;
				case '\n':
					if (lastWasCR) {
						if (counter > last + 1)
							sbuff.append(buff, last, counter - last - 1);
						// else nothing to do, can skip \r, next write will
						// write \n
						lastWasCR = false;
						last = counter;
					}
					break;
				default:
					if (lastWasCR) {
						if (counter == 0)
							sbuff.append('\n');
						else
							buff[counter - 1] = '\n';
						lastWasCR = false;
					}
					break;

				} // End fo switch (buff[counter]).

			if (last < nch)
				if (lastWasCR) {
					if (last < nch - 1)
						sbuff.append(buff, last, nch - last - 1);
				} else
					sbuff.append(buff, last, nch - last);

		} // End of while ((nch = in.read(buff, 0, buff.length)) != -1).

		if (this.withinSameComponent)
			((RTextArea) c).beginAtomicEdit();

		if (lastWasCR)
			sbuff.append('\n');
		c.replaceSelection(sbuff != null ? sbuff.toString() : "");

	}

	/**
	 * This method causes a transfer to a component from a clipboard or a DND drop
	 * operation. The Transferable represents the data to be imported into the
	 * component.
	 *
	 * @param comp
	 *            The component to receive the transfer. This argument is provided
	 *            to enable sharing of TransferHandlers by multiple components.
	 * @param t
	 *            The data to import
	 * @return <code>true</code> iff the data was inserted into the component.
	 */
	@Override
	public boolean importData(final JComponent comp, final Transferable t) {

		final JTextComponent c = (JTextComponent) comp;
		this.withinSameComponent = c == this.exportComp;

		// if we are importing to the same component that we exported from
		// then don't actually do anything if the drop location is inside
		// the drag location and set shouldRemove to false so that exportDone
		// knows not to remove any data
		if (this.withinSameComponent && c.getCaretPosition() >= this.p0 && c.getCaretPosition() <= this.p1) {
			this.shouldRemove = false;
			return true;
		}

		boolean imported = false;
		final DataFlavor importFlavor = this.getImportFlavor(t.getTransferDataFlavors(), c);
		if (importFlavor != null)
			try {
				final InputContext ic = c.getInputContext();
				if (ic != null)
					ic.endComposition();
				final Reader r = importFlavor.getReaderForText(t);
				this.handleReaderImport(r, c);
				imported = true;
			} catch (final UnsupportedFlavorException ufe) {
				ufe.printStackTrace();
			} catch (final IOException ioe) {
				ioe.printStackTrace();
			}

		return imported;

	}

}