/*
 * 12/08/2004
 *
 * TextFilePropertiesDialog.java - Dialog allowing you to view/edit a
 * text file's properties.
 * This library is distributed under a modified BSD license.  See the included
 * RSTAUI.License.txt file for details.
 */
package org.fife.rsta.ui;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

/**
 * A dialog that displays the properties of an individual text file being edited
 * by a {@link org.fife.ui.rsyntaxtextarea.TextEditorPane}. Some properties can
 * be modified directly from this dialog.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class TextFilePropertiesDialog extends EscapableDialog implements ActionListener {

	private static class DocumentCharIterator implements CharacterIterator {

		private final Document doc;
		private int index;
		private final Segment s;

		public DocumentCharIterator(final Document doc) {
			this.doc = doc;
			this.index = 0;
			this.s = new Segment();
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (final CloneNotSupportedException cnse) { // Never happens
				throw new InternalError("Clone not supported???");
			}
		}

		@Override
		public char current() {
			if (this.index >= this.getEndIndex())
				return CharacterIterator.DONE;
			try {
				this.doc.getText(this.index, 1, this.s);
				return this.s.first();
			} catch (final BadLocationException ble) {
				return CharacterIterator.DONE;
			}
		}

		@Override
		public char first() {
			this.index = this.getBeginIndex();
			return this.current();
		}

		@Override
		public int getBeginIndex() {
			return 0;
		}

		@Override
		public int getEndIndex() {
			return this.doc.getLength();
		}

		@Override
		public int getIndex() {
			return this.index;
		}

		@Override
		public char last() {
			this.index = Math.max(0, this.getEndIndex() - 1);
			return this.current();
		}

		@Override
		public char next() {
			this.index = Math.min(this.index + 1, this.getEndIndex());
			return this.current();
		}

		@Override
		public char previous() {
			this.index = Math.max(this.index - 1, this.getBeginIndex());
			return this.current();
		}

		@Override
		public char setIndex(final int pos) {
			if (pos < this.getBeginIndex() || pos > this.getEndIndex())
				throw new IllegalArgumentException("Illegal index: " + this.index);
			this.index = pos;
			return this.current();
		}

	}

	private static final String[] LINE_TERMINATOR_LABELS = { TextFilePropertiesDialog.msg.getString("SysDef"),
			TextFilePropertiesDialog.msg.getString("CR"), TextFilePropertiesDialog.msg.getString("LF"),
			TextFilePropertiesDialog.msg.getString("CRLF"), };
	private static final String[] LINE_TERMINATORS = { System.getProperty("line.separator"), "\r", "\n", "\r\n" };
	private static final ResourceBundle msg = ResourceBundle.getBundle("org.fife.rsta.ui.TextFilePropertiesDialog");
	private static final long serialVersionUID = 1L;

	/**
	 * Returns a string representation of a file size, such as "842 bytes", "1.73
	 * KB" or "3.4 MB".
	 *
	 * @param file
	 *            The file to get the size of.
	 * @return The string.
	 */
	private static final String getFileSizeStringFor(final File file) {

		int count = 0;
		double tempSize = file.length();
		double prevSize = tempSize;

		// Keep dividing by 1024 until you get the largest unit that goes
		// into this file's size.
		while (count < 4 && (tempSize = prevSize / 1024f) >= 1) {
			prevSize = tempSize;
			count++;
		}

		String suffix = null;
		switch (count) {
		case 0:
			suffix = "bytes";
			break;
		case 1:
			suffix = "KB";
			break;
		case 2:
			suffix = "MB";
			break;
		case 3:
			suffix = "GB";
			break;
		case 4:
			suffix = "TB";
			break;
		}

		final NumberFormat fileSizeFormat = NumberFormat.getNumberInstance();
		fileSizeFormat.setGroupingUsed(true);
		fileSizeFormat.setMinimumFractionDigits(0);
		fileSizeFormat.setMaximumFractionDigits(1);
		return fileSizeFormat.format(prevSize) + " " + suffix;

	}

	private JComboBox encodingCombo;

	private JTextField filePathField;

	private JButton okButton;

	private JComboBox terminatorCombo;

	private TextEditorPane textArea;

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            The main application dialog.
	 * @param textArea
	 *            The text area on which to report.
	 */
	public TextFilePropertiesDialog(final Dialog parent, final TextEditorPane textArea) {
		super(parent);
		this.init(textArea);
	}

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            The main application window.
	 * @param textArea
	 *            The text area on which to report.
	 */
	public TextFilePropertiesDialog(final Frame parent, final TextEditorPane textArea) {
		super(parent);
		this.init(textArea);
	}

	/**
	 * Listens for actions in this dialog.
	 *
	 * @param e
	 *            The action event.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {

		final String command = e.getActionCommand();

		if ("TerminatorComboBox".equals(command))
			this.okButton.setEnabled(true);
		else if ("encodingCombo".equals(command))
			this.okButton.setEnabled(true);
		else if ("OKButton".equals(command)) {
			final String terminator = this.getSelectedLineTerminator();
			if (terminator != null) {
				final String old = (String) this.textArea.getLineSeparator();
				if (!terminator.equals(old))
					this.textArea.setLineSeparator(terminator);
			}
			final String encoding = (String) this.encodingCombo.getSelectedItem();
			if (encoding != null)
				this.textArea.setEncoding(encoding);
			this.setVisible(false);
		}

		else if ("CancelButton".equals(command))
			this.escapePressed();

	}

	private int calculateWordCount(final TextEditorPane textArea) {

		int wordCount = 0;
		final RSyntaxDocument doc = (RSyntaxDocument) textArea.getDocument();

		final BreakIterator bi = BreakIterator.getWordInstance();
		bi.setText(new DocumentCharIterator(textArea.getDocument()));
		for (int nextBoundary = bi.first(); nextBoundary != BreakIterator.DONE; nextBoundary = bi.next())
			// getWordInstance() returns boundaries for both words and
			// non-words (whitespace, punctuation, etc.)
			try {
				final char ch = doc.charAt(nextBoundary);
				if (Character.isLetterOrDigit(ch))
					wordCount++;
			} catch (final BadLocationException ble) {
				ble.printStackTrace();
			}

		return wordCount;

	}

	/**
	 * Creates a "footer" component containing the OK and Cancel buttons.
	 *
	 * @param ok
	 *            The OK button.
	 * @param cancel
	 *            The Cancel button.
	 * @return The footer component for the dialog.
	 */
	protected Container createButtonFooter(final JButton ok, final JButton cancel) {

		final JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		buttonPanel.add(ok);
		buttonPanel.add(cancel);

		final JPanel panel = new JPanel(new BorderLayout());
		final ComponentOrientation o = this.getComponentOrientation();
		final int PADDING = 8;
		final int left = o.isLeftToRight() ? 0 : PADDING;
		final int right = o.isLeftToRight() ? PADDING : 0;
		panel.setBorder(BorderFactory.createEmptyBorder(10, left, 0, right));
		panel.add(buttonPanel, BorderLayout.LINE_END);
		return panel;

	}

	/**
	 * Returns the title to use for this dialog.
	 *
	 * @param fileName
	 *            The name of the file whose properties are being shown.
	 * @return The title for this dialog.
	 */
	protected String createTitle(final String fileName) {
		return MessageFormat.format(TextFilePropertiesDialog.msg.getString("Title"), this.textArea.getFileName());
	}

	private String getSelectedLineTerminator() {
		return TextFilePropertiesDialog.LINE_TERMINATORS[this.terminatorCombo.getSelectedIndex()];
	}

	private void init(final TextEditorPane textArea) {

		this.textArea = textArea;
		this.setTitle(this.createTitle(textArea.getFileName()));

		final ComponentOrientation o = ComponentOrientation.getOrientation(this.getLocale());

		final JPanel contentPane = new ResizableFrameContentPane(new BorderLayout());
		contentPane.setBorder(UIUtil.getEmpty5Border());

		// Where we actually add our content.
		final JPanel content2 = new JPanel();
		content2.setLayout(new SpringLayout());
		contentPane.add(content2, BorderLayout.NORTH);

		this.filePathField = new JTextField(40);
		this.filePathField.setText(textArea.getFileFullPath());
		this.filePathField.setEditable(false);
		final JLabel filePathLabel = UIUtil.newLabel(TextFilePropertiesDialog.msg, "Path", this.filePathField);

		final JLabel linesLabel = new JLabel(TextFilePropertiesDialog.msg.getString("Lines"));
		final JLabel linesCountLabel = new JLabel(Integer.toString(textArea.getLineCount()));

		final JLabel charsLabel = new JLabel(TextFilePropertiesDialog.msg.getString("Characters"));
		final JLabel charsCountLabel = new JLabel(Integer.toString(textArea.getDocument().getLength()));

		final JLabel wordsLabel = new JLabel(TextFilePropertiesDialog.msg.getString("Words"));
		final JLabel wordsCountLabel = new JLabel(Integer.toString(this.calculateWordCount(textArea)));

		this.terminatorCombo = new JComboBox(TextFilePropertiesDialog.LINE_TERMINATOR_LABELS);
		if (textArea.isReadOnly())
			this.terminatorCombo.setEnabled(false);
		UIUtil.fixComboOrientation(this.terminatorCombo);
		this.setSelectedLineTerminator((String) textArea.getLineSeparator());
		this.terminatorCombo.setActionCommand("TerminatorComboBox");
		this.terminatorCombo.addActionListener(this);
		final JLabel terminatorLabel = UIUtil.newLabel(TextFilePropertiesDialog.msg, "LineTerminator",
				this.terminatorCombo);

		this.encodingCombo = new JComboBox();
		if (textArea.isReadOnly())
			this.encodingCombo.setEnabled(false);
		UIUtil.fixComboOrientation(this.encodingCombo);

		// Populate the combo box with all available encodings.
		final Map<String, Charset> availcs = Charset.availableCharsets();
		final Set<String> charsetNames = availcs.keySet();
		for (final String charsetName : charsetNames)
			this.encodingCombo.addItem(charsetName);
		this.setEncoding(textArea.getEncoding());
		this.encodingCombo.setActionCommand("encodingCombo");
		this.encodingCombo.addActionListener(this);
		final JLabel encodingLabel = UIUtil.newLabel(TextFilePropertiesDialog.msg, "Encoding", this.encodingCombo);

		final JLabel sizeLabel = new JLabel(TextFilePropertiesDialog.msg.getString("FileSize"));
		final File file = new File(textArea.getFileFullPath());
		String size = "";
		if (file.exists() && !file.isDirectory())
			size = TextFilePropertiesDialog.getFileSizeStringFor(file);
		final JLabel sizeLabel2 = new JLabel(size);

		final long temp = textArea.getLastSaveOrLoadTime();
		String modifiedString;
		if (temp <= 0)
			modifiedString = "";
		else {
			final Date modifiedDate = new Date(temp);
			final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a  EEE, MMM d, yyyy");
			modifiedString = sdf.format(modifiedDate);
		}
		final JLabel modifiedLabel = new JLabel(TextFilePropertiesDialog.msg.getString("LastModified"));
		final JLabel modified = new JLabel(modifiedString);

		if (o.isLeftToRight()) {
			content2.add(filePathLabel);
			content2.add(this.filePathField);
			content2.add(linesLabel);
			content2.add(linesCountLabel);
			content2.add(charsLabel);
			content2.add(charsCountLabel);
			content2.add(wordsLabel);
			content2.add(wordsCountLabel);
			content2.add(terminatorLabel);
			content2.add(this.terminatorCombo);
			content2.add(encodingLabel);
			content2.add(this.encodingCombo);
			content2.add(sizeLabel);
			content2.add(sizeLabel2);
			content2.add(modifiedLabel);
			content2.add(modified);
		} else {
			content2.add(this.filePathField);
			content2.add(filePathLabel);
			content2.add(linesCountLabel);
			content2.add(linesLabel);
			content2.add(charsCountLabel);
			content2.add(charsLabel);
			content2.add(wordsCountLabel);
			content2.add(wordsLabel);
			content2.add(this.terminatorCombo);
			content2.add(terminatorLabel);
			content2.add(this.encodingCombo);
			content2.add(encodingLabel);
			content2.add(sizeLabel2);
			content2.add(sizeLabel);
			content2.add(modified);
			content2.add(modifiedLabel);
		}

		UIUtil.makeSpringCompactGrid(content2, 8, 2, 0, 0, 5, 5);

		// Make a panel for OK and cancel buttons.
		this.okButton = UIUtil.newButton(TextFilePropertiesDialog.msg, "OK");
		this.okButton.setActionCommand("OKButton");
		this.okButton.addActionListener(this);
		this.okButton.setEnabled(false);
		final JButton cancelButton = UIUtil.newButton(TextFilePropertiesDialog.msg, "Cancel");
		cancelButton.setActionCommand("CancelButton");
		cancelButton.addActionListener(this);
		final Container buttons = this.createButtonFooter(this.okButton, cancelButton);
		contentPane.add(buttons, BorderLayout.SOUTH);

		this.setContentPane(contentPane);
		this.setModal(true);
		this.applyComponentOrientation(o);
		this.pack();
		this.setLocationRelativeTo(this.getParent());

	}

	/**
	 * Sets the encoding selected by this dialog.
	 *
	 * @param encoding
	 *            The desired encoding. If this value is invalid or not supported by
	 *            this OS, <code>US-ASCII</code> is used.
	 */
	private void setEncoding(final String encoding) {

		Charset cs1 = Charset.forName(encoding);

		final int count = this.encodingCombo.getItemCount();
		for (int i = 0; i < count; i++) {
			final String item = (String) this.encodingCombo.getItemAt(i);
			final Charset cs2 = Charset.forName(item);
			if (cs1.equals(cs2)) {
				this.encodingCombo.setSelectedIndex(i);
				return;
			}
		}

		// Encoding not found: select default.
		cs1 = Charset.forName("US-ASCII");
		for (int i = 0; i < count; i++) {
			final String item = (String) this.encodingCombo.getItemAt(i);
			final Charset cs2 = Charset.forName(item);
			if (cs1.equals(cs2)) {
				this.encodingCombo.setSelectedIndex(i);
				return;
			}
		}

	}

	private void setSelectedLineTerminator(final String terminator) {
		for (int i = 0; i < TextFilePropertiesDialog.LINE_TERMINATORS.length; i++)
			if (TextFilePropertiesDialog.LINE_TERMINATORS[i].equals(terminator)) {
				this.terminatorCombo.setSelectedIndex(i);
				break;
			}
	}

	/**
	 * Overridden to focus the file path text field and select its contents when
	 * this dialog is made visible.
	 *
	 * @param visible
	 *            Whether this dialog should be made visible.
	 */
	@Override
	public void setVisible(final boolean visible) {
		if (visible)
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					TextFilePropertiesDialog.this.filePathField.requestFocusInWindow();
					TextFilePropertiesDialog.this.filePathField.selectAll();
				}
			});
		super.setVisible(visible);
	}

}