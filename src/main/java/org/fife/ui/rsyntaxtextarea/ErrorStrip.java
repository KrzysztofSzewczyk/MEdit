/*
 * 08/10/2009
 *
 * ErrorStrip.java - A component that can visually show Parser messages (syntax
 * errors, etc.) in an RSyntaxTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.TaskTagParser.TaskNotice;
import org.fife.ui.rtextarea.RTextArea;

/**
 * A component to sit alongside an {@link RSyntaxTextArea} that displays colored
 * markers for locations of interest (parser errors, marked occurrences, etc.).
 * <p>
 *
 * <code>ErrorStrip</code>s display <code>ParserNotice</code>s from
 * {@link Parser}s. Currently, the only way to get lines flagged in this
 * component is to register a <code>Parser</code> on an RSyntaxTextArea and
 * return <code>ParserNotice</code>s for each line to display an icon for. The
 * severity of each notice must be at least the threshold set by
 * {@link #setLevelThreshold(org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level)}
 * to be displayed in this error strip. The default threshold is
 * {@link org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level#WARNING}.
 * <p>
 *
 * An <code>ErrorStrip</code> can be added to a UI like so:
 *
 * <pre>
 * textArea = createTextArea();
 * textArea.addParser(new MyParser(textArea)); // Identifies lines to display
 * scrollPane = new RTextScrollPane(textArea, true);
 * ErrorStrip es = new ErrorStrip(textArea);
 * JPanel temp = new JPanel(new BorderLayout());
 * temp.add(scrollPane);
 * temp.add(es, BorderLayout.LINE_END);
 * </pre>
 *
 * @author Robert Futrell
 * @version 0.5
 */
// Possible improvements:
// 1. Handle marked occurrence changes & "mark all" changes separately from
// parser changes. For each property change, call a method that removes
// the notices being reloaded from the Markers (removing any Markers that
// are now "empty").
//
public class ErrorStrip extends JPanel {

	/**
	 * The default implementation of the provider of tool tips for markers in an
	 * error strip.
	 *
	 * @author predi
	 */
	private static class DefaultErrorStripMarkerToolTipProvider implements ErrorStripMarkerToolTipProvider {

		@Override
		public String getToolTipText(final List<ParserNotice> notices) {

			String text = null;

			if (notices.size() == 1)
				text = notices.get(0).getMessage();
			else { // > 1
				final StringBuilder sb = new StringBuilder("<html>");
				sb.append(ErrorStrip.MSG.getString("MultipleMarkers"));
				sb.append("<br>");
				for (int i = 0; i < notices.size(); i++) {
					final ParserNotice pn = notices.get(i);
					sb.append("&nbsp;&nbsp;&nbsp;- ");
					sb.append(pn.getMessage());
					sb.append("<br>");
				}
				text = sb.toString();
			}

			return text;

		}

	}

	/**
	 * Returns tool tip text for the markers in an {@link ErrorStrip} that denote
	 * one or more parser notices.
	 *
	 * @author predi
	 */
	public interface ErrorStripMarkerToolTipProvider {

		/**
		 * Returns the tool tip text for a marker in an <code>ErrorStrip</code> that
		 * denotes a given list of parser notices.
		 *
		 * @param notices
		 *            The list of parser notices.
		 * @return The tool tip text. This may be HTML. Returning <code>null</code> will
		 *         result in no tool tip being displayed.
		 */
		String getToolTipText(List<ParserNotice> notices);

	}

	/**
	 * Listens for events in the error strip and its markers.
	 */
	private class Listener extends MouseAdapter implements PropertyChangeListener, CaretListener {

		private final Rectangle visibleRect = new Rectangle();

		@Override
		public void caretUpdate(final CaretEvent e) {
			if (ErrorStrip.this.getFollowCaret()) {
				final int line = ErrorStrip.this.textArea.getCaretLineNumber();
				final float percent = line / (float) (ErrorStrip.this.textArea.getLineCount() - 1);
				ErrorStrip.this.textArea.computeVisibleRect(this.visibleRect);
				ErrorStrip.this.caretLineY = (int) (this.visibleRect.height * percent);
				if (ErrorStrip.this.caretLineY != ErrorStrip.this.lastLineY) {
					ErrorStrip.this.repaint(0, ErrorStrip.this.lastLineY, ErrorStrip.this.getWidth(), 2); // Erase old
																											// position
					ErrorStrip.this.repaint(0, ErrorStrip.this.caretLineY, ErrorStrip.this.getWidth(), 2);
					ErrorStrip.this.lastLineY = ErrorStrip.this.caretLineY;
				}
			}
		}

		@Override
		public void mouseClicked(final MouseEvent e) {

			final Component source = (Component) e.getSource();
			if (source instanceof Marker) {
				((Marker) source).mouseClicked(e);
				return;
			}

			final int line = ErrorStrip.this.yToLine(e.getY());
			if (line > -1)
				try {
					final int offs = ErrorStrip.this.textArea.getLineStartOffset(line);
					ErrorStrip.this.textArea.setCaretPosition(offs);
				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(ErrorStrip.this.textArea);
				}

		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {

			final String propName = e.getPropertyName();

			// If they change whether marked occurrences are visible in editor
			if (RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY.equals(propName)) {
				if (ErrorStrip.this.getShowMarkedOccurrences())
					ErrorStrip.this.refreshMarkers();
			}

			// If parser notices changed.
			// TODO: Don't update "mark all/occurrences" markers.
			else if (RSyntaxTextArea.PARSER_NOTICES_PROPERTY.equals(propName))
				ErrorStrip.this.refreshMarkers();
			else if (RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY.equals(propName)) {
				if (ErrorStrip.this.getShowMarkedOccurrences())
					ErrorStrip.this.refreshMarkers();
			}

			// If "mark all" occurrences changed.
			// TODO: Only update "mark all" markers, not all of them.
			else if (RTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY.equals(propName))
				if (ErrorStrip.this.getShowMarkAll())
					ErrorStrip.this.refreshMarkers();

		}

	}

	/**
	 * A notice that wraps a "marked occurrence" instance.
	 */
	private class MarkedOccurrenceNotice implements ParserNotice {

		private final Color color;
		private final DocumentRange range;

		MarkedOccurrenceNotice(final DocumentRange range, final Color color) {
			this.range = range;
			this.color = color;
		}

		@Override
		public int compareTo(final ParserNotice other) {
			return 0; // Value doesn't matter
		}

		@Override
		public boolean containsPosition(final int pos) {
			return pos >= this.range.getStartOffset() && pos < this.range.getEndOffset();
		}

		@Override
		public boolean equals(final Object o) {
			// FindBugs - Define equals() when defining compareTo()
			if (!(o instanceof ParserNotice))
				return false;
			return this.compareTo((ParserNotice) o) == 0;
		}

		@Override
		public Color getColor() {
			return this.color;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean getKnowsOffsetAndLength() {
			return true;
		}

		@Override
		public int getLength() {
			return this.range.getEndOffset() - this.range.getStartOffset();
		}

		@Override
		public Level getLevel() {
			return Level.INFO; // Won't matter
		}

		@Override
		public int getLine() {
			try {
				return ErrorStrip.this.textArea.getLineOfOffset(this.range.getStartOffset()) + 1;
			} catch (final BadLocationException ble) {
				return 0;
			}
		}

		@Override
		public String getMessage() {
			String text = null;
			try {
				final String word = ErrorStrip.this.textArea.getText(this.range.getStartOffset(), this.getLength());
				text = ErrorStrip.MSG.getString("OccurrenceOf");
				text = MessageFormat.format(text, word);
			} catch (final BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(ErrorStrip.this.textArea);
			}
			return text;
		}

		@Override
		public int getOffset() {
			return this.range.getStartOffset();
		}

		@Override
		public Parser getParser() {
			return null;
		}

		@Override
		public boolean getShowInEditor() {
			return false; // Value doesn't matter
		}

		@Override
		public String getToolTipText() {
			return null;
		}

		@Override
		public int hashCode() { // FindBugs, since we override equals()
			return 0; // Value doesn't matter for us.
		}

	}

	/**
	 * A "marker" in this error strip, representing one or more notices.
	 */
	private class Marker extends JComponent {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final List<ParserNotice> notices;

		Marker(final ParserNotice notice) {
			this.notices = new ArrayList<>(1); // Usually just 1
			this.addNotice(notice);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			this.setSize(this.getPreferredSize());
			ToolTipManager.sharedInstance().registerComponent(this);
		}

		public void addNotice(final ParserNotice notice) {
			this.notices.add(notice);
		}

		public boolean containsMarkedOccurence() {
			boolean result = false;
			for (int i = 0; i < this.notices.size(); i++)
				if (this.notices.get(i) instanceof MarkedOccurrenceNotice) {
					result = true;
					break;
				}
			return result;
		}

		public Color getColor() {
			// Return the color for the highest-level parser.
			Color c = null;
			int lowestLevel = Integer.MAX_VALUE; // ERROR is 0
			for (final ParserNotice notice : this.notices)
				if (notice.getLevel().getNumericValue() < lowestLevel) {
					lowestLevel = notice.getLevel().getNumericValue();
					c = notice.getColor();
				}
			return c;
		}

		@Override
		public Dimension getPreferredSize() {
			final int w = ErrorStrip.PREFERRED_WIDTH - 4; // 2-pixel empty border
			return new Dimension(w, 5);
		}

		@Override
		public String getToolTipText() {
			return ErrorStrip.this.markerToolTipProvider.getToolTipText(Collections.unmodifiableList(this.notices));
		}

		protected void mouseClicked(final MouseEvent e) {
			final ParserNotice pn = this.notices.get(0);
			int offs = pn.getOffset();
			final int len = pn.getLength();
			if (offs > -1 && len > -1) { // These values are optional
				final DocumentRange range = new DocumentRange(offs, offs + len);
				RSyntaxUtilities.selectAndPossiblyCenter(ErrorStrip.this.textArea, range, true);
			} else {
				final int line = pn.getLine();
				try {
					offs = ErrorStrip.this.textArea.getLineStartOffset(line);
					ErrorStrip.this.textArea.getFoldManager().ensureOffsetNotInClosedFold(offs);
					ErrorStrip.this.textArea.setCaretPosition(offs);
				} catch (final BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(ErrorStrip.this.textArea);
				}
			}
		}

		@Override
		protected void paintComponent(final Graphics g) {

			// TODO: Give "priorities" and always pick color of a notice with
			// highest priority (e.g. parsing errors will usually be red).

			Color borderColor = this.getColor();
			if (borderColor == null)
				borderColor = Color.DARK_GRAY;
			final Color fillColor = ErrorStrip.this.getBrighterColor(borderColor);

			final int w = this.getWidth();
			final int h = this.getHeight();

			g.setColor(fillColor);
			g.fillRect(0, 0, w, h);

			g.setColor(borderColor);
			g.drawRect(0, 0, w - 1, h - 1);

		}

		@Override
		public void removeNotify() {
			super.removeNotify();
			ToolTipManager.sharedInstance().unregisterComponent(this);
			this.removeMouseListener(ErrorStrip.this.listener);
		}

		public void updateLocation() {
			final int line = this.notices.get(0).getLine();
			final int y = ErrorStrip.this.lineToY(line);
			this.setLocation(2, y);
		}

	}

	private static final ResourceBundle MSG = ResourceBundle.getBundle("org.fife.ui.rsyntaxtextarea.ErrorStrip");

	/**
	 * The preferred width of this component.
	 */
	private static final int PREFERRED_WIDTH = 14;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Returns a possibly brighter component for a color.
	 *
	 * @param i
	 *            An RGB component for a color (0-255).
	 * @return A possibly brighter value for the component.
	 */
	private static int possiblyBrighter(int i) {
		if (i < 255)
			i += (int) ((255 - i) * 0.8f);
		return i;
	}

	/**
	 * Mapping of colors to brighter colors. This is kept to prevent unnecessary
	 * creation of the same Colors over and over.
	 */
	private Map<Color, Color> brighterColors;

	/**
	 * Where we paint the caret marker.
	 */
	private int caretLineY;

	/**
	 * The color to use for the caret marker.
	 */
	private Color caretMarkerColor;

	/**
	 * Whether the caret marker's location should be rendered.
	 */
	private boolean followCaret;

	/**
	 * The last location of the caret marker.
	 */
	private int lastLineY;

	/**
	 * Only notices of this severity (or worse) will be displayed in this error
	 * strip.
	 */
	private ParserNotice.Level levelThreshold;

	/**
	 * Listens for events in this component.
	 */
	private transient Listener listener;

	/**
	 * Generates the tool tips for markers in this error strip.
	 */
	private transient ErrorStripMarkerToolTipProvider markerToolTipProvider;

	/**
	 * Whether markers for "mark all" highlights should be shown in this error
	 * strip.
	 */
	private boolean showMarkAll;

	/**
	 * Whether "marked occurrences" in the text area should be shown in this error
	 * strip.
	 */
	private boolean showMarkedOccurrences;

	/**
	 * The text area.
	 */
	private final RSyntaxTextArea textArea;

	/**
	 * Constructor.
	 *
	 * @param textArea
	 *            The text area we are examining.
	 */
	public ErrorStrip(final RSyntaxTextArea textArea) {
		this.textArea = textArea;
		this.listener = new Listener();
		ToolTipManager.sharedInstance().registerComponent(this);
		this.setLayout(null); // Manually layout Markers as they can overlap
		this.addMouseListener(this.listener);
		this.setShowMarkedOccurrences(true);
		this.setShowMarkAll(true);
		this.setLevelThreshold(ParserNotice.Level.WARNING);
		this.setFollowCaret(true);
		this.setCaretMarkerColor(Color.BLACK);
		this.setMarkerToolTipProvider(null); // Install default
	}

	/**
	 * Adds markers for a list of ranges in the document.
	 *
	 * @param ranges
	 *            The list of ranges in the document.
	 * @param markerMap
	 *            A mapping from line number to <code>Marker</code>.
	 * @param color
	 *            The color to use for the markers.
	 */
	private void addMarkersForRanges(final List<DocumentRange> ranges, final Map<Integer, Marker> markerMap,
			final Color color) {
		for (final DocumentRange range : ranges) {
			int line = 0;
			try {
				line = this.textArea.getLineOfOffset(range.getStartOffset());
			} catch (final BadLocationException ble) { // Never happens
				continue;
			}
			final ParserNotice notice = new MarkedOccurrenceNotice(range, color);
			final Integer key = Integer.valueOf(line);
			Marker m = markerMap.get(key);
			if (m == null) {
				m = new Marker(notice);
				m.addMouseListener(this.listener);
				markerMap.put(key, m);
				this.add(m);
			} else if (!m.containsMarkedOccurence())
				m.addNotice(notice);
		}
	}

	/**
	 * Overridden so we only start listening for parser notices when this component
	 * (and presumably the text area) are visible.
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		this.textArea.addCaretListener(this.listener);
		this.textArea.addPropertyChangeListener(RSyntaxTextArea.PARSER_NOTICES_PROPERTY, this.listener);
		this.textArea.addPropertyChangeListener(RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, this.listener);
		this.textArea.addPropertyChangeListener(RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY, this.listener);
		this.textArea.addPropertyChangeListener(RTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, this.listener);
		this.refreshMarkers();
	}

	/**
	 * Manually manages layout since this component uses no layout manager.
	 */
	@Override
	public void doLayout() {
		for (int i = 0; i < this.getComponentCount(); i++) {
			final Marker m = (Marker) this.getComponent(i);
			m.updateLocation();
		}
		this.listener.caretUpdate(null); // Force recalculation of caret line pos
	}

	/**
	 * Returns a "brighter" color.
	 *
	 * @param c
	 *            The color.
	 * @return A brighter color.
	 */
	private Color getBrighterColor(final Color c) {
		if (this.brighterColors == null)
			this.brighterColors = new HashMap<>(5); // Usually small
		Color brighter = this.brighterColors.get(c);
		if (brighter == null) {
			// Don't use c.brighter() as it doesn't work well for blue, and
			// also doesn't return something brighter "enough."
			final int r = ErrorStrip.possiblyBrighter(c.getRed());
			final int g = ErrorStrip.possiblyBrighter(c.getGreen());
			final int b = ErrorStrip.possiblyBrighter(c.getBlue());
			brighter = new Color(r, g, b);
			this.brighterColors.put(c, brighter);
		}
		return brighter;
	}

	/**
	 * returns the color to use when painting the caret marker.
	 *
	 * @return The caret marker color.
	 * @see #setCaretMarkerColor(Color)
	 */
	public Color getCaretMarkerColor() {
		return this.caretMarkerColor;
	}

	/**
	 * Returns whether the caret's position should be drawn.
	 *
	 * @return Whether the caret's position should be drawn.
	 * @see #setFollowCaret(boolean)
	 */
	public boolean getFollowCaret() {
		return this.followCaret;
	}

	/**
	 * Returns the minimum severity a parser notice must be for it to be displayed
	 * in this error strip. This will be one of the constants defined in the
	 * <code>ParserNotice</code> class.
	 *
	 * @return The minimum severity.
	 * @see #setLevelThreshold(org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level)
	 */
	public ParserNotice.Level getLevelThreshold() {
		return this.levelThreshold;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Dimension getPreferredSize() {
		final int height = this.textArea.getPreferredScrollableViewportSize().height;
		return new Dimension(ErrorStrip.PREFERRED_WIDTH, height);
	}

	/**
	 * Returns whether "mark all" highlights are shown in this error strip.
	 *
	 * @return Whether markers are shown for "mark all" highlights.
	 * @see #setShowMarkAll(boolean)
	 */
	public boolean getShowMarkAll() {
		return this.showMarkAll;
	}

	/**
	 * Returns whether marked occurrences are shown in this error strip.
	 *
	 * @return Whether marked occurrences are shown.
	 * @see #setShowMarkedOccurrences(boolean)
	 */
	public boolean getShowMarkedOccurrences() {
		return this.showMarkedOccurrences;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getToolTipText(final MouseEvent e) {
		String text = null;
		final int line = this.yToLine(e.getY());
		if (line > -1) {
			text = ErrorStrip.MSG.getString("Line");
			text = MessageFormat.format(text, Integer.valueOf(line + 1));
		}
		return text;
	}

	/**
	 * Returns the y-offset in this component corresponding to a line in the text
	 * component.
	 *
	 * @param line
	 *            The line.
	 * @return The y-offset.
	 * @see #yToLine(int)
	 */
	private int lineToY(final int line) {
		final int h = this.textArea.getVisibleRect().height;
		final float lineCount = this.textArea.getLineCount();
		return (int) ((line - 1) / (lineCount - 1) * (h - 2));
	}

	/**
	 * Overridden to (possibly) draw the caret's position.
	 *
	 * @param g
	 *            The graphics context.
	 */
	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);
		if (this.caretLineY > -1) {
			g.setColor(this.getCaretMarkerColor());
			g.fillRect(0, this.caretLineY, this.getWidth(), 2);
		}
	}

	/**
	 * Refreshes the markers displayed in this error strip.
	 */
	private void refreshMarkers() {

		this.removeAll(); // listener is removed in Marker.removeNotify()
		final Map<Integer, Marker> markerMap = new HashMap<>();

		final List<ParserNotice> notices = this.textArea.getParserNotices();
		for (final ParserNotice notice : notices)
			if (notice.getLevel().isEqualToOrWorseThan(this.levelThreshold) || notice instanceof TaskNotice) {
				final Integer key = Integer.valueOf(notice.getLine());
				Marker m = markerMap.get(key);
				if (m == null) {
					m = new Marker(notice);
					m.addMouseListener(this.listener);
					markerMap.put(key, m);
					this.add(m);
				} else
					m.addNotice(notice);
			}

		if (this.getShowMarkedOccurrences() && this.textArea.getMarkOccurrences()) {
			final List<DocumentRange> occurrences = this.textArea.getMarkedOccurrences();
			this.addMarkersForRanges(occurrences, markerMap, this.textArea.getMarkOccurrencesColor());
		}

		if (this.getShowMarkAll() /* && textArea.getMarkAll() */) {
			final Color markAllColor = this.textArea.getMarkAllHighlightColor();
			final List<DocumentRange> ranges = this.textArea.getMarkAllHighlightRanges();
			this.addMarkersForRanges(ranges, markerMap, markAllColor);
		}

		this.revalidate();
		this.repaint();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		this.textArea.removeCaretListener(this.listener);
		this.textArea.removePropertyChangeListener(RSyntaxTextArea.PARSER_NOTICES_PROPERTY, this.listener);
		this.textArea.removePropertyChangeListener(RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, this.listener);
		this.textArea.removePropertyChangeListener(RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY, this.listener);
		this.textArea.removePropertyChangeListener(RTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, this.listener);
	}

	/**
	 * Sets the color to use when painting the caret marker.
	 *
	 * @param color
	 *            The new caret marker color.
	 * @see #getCaretMarkerColor()
	 */
	public void setCaretMarkerColor(final Color color) {
		if (color != null) {
			this.caretMarkerColor = color;
			this.listener.caretUpdate(null); // Force repaint
		}
	}

	/**
	 * Toggles whether the caret's current location should be drawn.
	 *
	 * @param follow
	 *            Whether the caret's current location should be followed.
	 * @see #getFollowCaret()
	 */
	public void setFollowCaret(final boolean follow) {
		if (this.followCaret != follow) {
			if (this.followCaret)
				this.repaint(0, this.caretLineY, this.getWidth(), 2); // Erase
			this.caretLineY = -1;
			this.lastLineY = -1;
			this.followCaret = follow;
			this.listener.caretUpdate(null); // Possibly repaint
		}
	}

	/**
	 * Sets the minimum severity a parser notice must be for it to be displayed in
	 * this error strip. This should be one of the constants defined in the
	 * <code>ParserNotice</code> class. The default value is
	 * {@link org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level#WARNING}.
	 *
	 * @param level
	 *            The new severity threshold.
	 * @see #getLevelThreshold()
	 * @see ParserNotice
	 */
	public void setLevelThreshold(final ParserNotice.Level level) {
		this.levelThreshold = level;
		if (this.isDisplayable())
			this.refreshMarkers();
	}

	/**
	 * Sets the provider of tool tips for markers in this error strip. Applications
	 * can use this method to control the content and format of the tool tip
	 * descriptions of line markers.
	 *
	 * @param provider
	 *            The provider. If this is <code>null</code>, a default
	 *            implementation will be used.
	 */
	public void setMarkerToolTipProvider(final ErrorStripMarkerToolTipProvider provider) {
		this.markerToolTipProvider = provider != null ? provider : new DefaultErrorStripMarkerToolTipProvider();
	}

	/**
	 * Sets whether "mark all" highlights are shown in this error strip.
	 *
	 * @param show
	 *            Whether to show markers for "mark all" highlights.
	 * @see #getShowMarkAll()
	 */
	public void setShowMarkAll(final boolean show) {
		if (show != this.showMarkAll) {
			this.showMarkAll = show;
			if (this.isDisplayable())
				this.refreshMarkers();
		}
	}

	/**
	 * Sets whether marked occurrences are shown in this error strip.
	 *
	 * @param show
	 *            Whether to show marked occurrences.
	 * @see #getShowMarkedOccurrences()
	 */
	public void setShowMarkedOccurrences(final boolean show) {
		if (show != this.showMarkedOccurrences) {
			this.showMarkedOccurrences = show;
			if (this.isDisplayable())
				this.refreshMarkers();
		}
	}

	/**
	 * Returns the line in the text area corresponding to a y-offset in this
	 * component.
	 *
	 * @param y
	 *            The y-offset.
	 * @return The line.
	 * @see #lineToY(int)
	 */
	private int yToLine(final int y) {
		int line = -1;
		final int h = this.textArea.getVisibleRect().height;
		if (y < h) {
			final float at = y / (float) h;
			line = Math.round((this.textArea.getLineCount() - 1) * at);
		}
		return line;
	}

}