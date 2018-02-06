/*
 * 07/29/2009
 *
 * FocusableTip.java - A focusable tool tip, like those in Eclipse.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.focusabletip;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MouseInputAdapter;

import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;

/**
 * A focusable tool tip, similar to those found in Eclipse. The user can click
 * in the tip and it becomes a "real," resizable window.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FocusableTip {

	/**
	 * Listens for events in a text area.
	 */
	private class TextAreaListener extends MouseInputAdapter
			implements CaretListener, ComponentListener, FocusListener, KeyListener {

		@Override
		public void caretUpdate(final CaretEvent e) {
			final Object source = e.getSource();
			if (source == FocusableTip.this.textArea)
				FocusableTip.this.possiblyDisposeOfTipWindow();
		}

		@Override
		public void componentHidden(final ComponentEvent e) {
			this.handleComponentEvent(e);
		}

		@Override
		public void componentMoved(final ComponentEvent e) {
			this.handleComponentEvent(e);
		}

		@Override
		public void componentResized(final ComponentEvent e) {
			this.handleComponentEvent(e);
		}

		@Override
		public void componentShown(final ComponentEvent e) {
			this.handleComponentEvent(e);
		}

		@Override
		public void focusGained(final FocusEvent e) {
		}

		@Override
		public void focusLost(final FocusEvent e) {
			// Only dispose of tip if it wasn't the TipWindow that was clicked
			// "c" can be null, at least on OS X, so we must check that before
			// calling SwingUtilities.getWindowAncestor() to guard against an
			// NPE.
			final Component c = e.getOppositeComponent();
			final boolean tipClicked = c instanceof TipWindow
					|| c != null && SwingUtilities.getWindowAncestor(c) instanceof TipWindow;
			if (!tipClicked)
				FocusableTip.this.possiblyDisposeOfTipWindow();
		}

		private void handleComponentEvent(final ComponentEvent e) {
			FocusableTip.this.possiblyDisposeOfTipWindow();
		}

		public void install(final JTextArea textArea) {
			textArea.addCaretListener(this);
			textArea.addComponentListener(this);
			textArea.addFocusListener(this);
			textArea.addKeyListener(this);
			textArea.addMouseListener(this);
			textArea.addMouseMotionListener(this);
		}

		@Override
		public void keyPressed(final KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				FocusableTip.this.possiblyDisposeOfTipWindow();
			else if (e.getKeyCode() == KeyEvent.VK_F2)
				if (FocusableTip.this.tipWindow != null && !FocusableTip.this.tipWindow.getFocusableWindowState()) {
					FocusableTip.this.tipWindow.actionPerformed(null);
					e.consume(); // Don't do bookmarking stuff
				}
		}

		@Override
		public void keyReleased(final KeyEvent e) {
		}

		@Override
		public void keyTyped(final KeyEvent e) {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			// possiblyDisposeOfTipWindow();
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			if (FocusableTip.this.tipVisibleBounds == null
					|| !FocusableTip.this.tipVisibleBounds.contains(e.getPoint()))
				FocusableTip.this.possiblyDisposeOfTipWindow();
		}

		public void uninstall() {
			FocusableTip.this.textArea.removeCaretListener(this);
			FocusableTip.this.textArea.removeComponentListener(this);
			FocusableTip.this.textArea.removeFocusListener(this);
			FocusableTip.this.textArea.removeKeyListener(this);
			FocusableTip.this.textArea.removeMouseListener(this);
			FocusableTip.this.textArea.removeMouseMotionListener(this);
		}

	}

	private static final ResourceBundle MSG = ResourceBundle
			.getBundle("org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip");
	/**
	 * Margin from mouse cursor at which to draw focusable tip.
	 */
	private static final int X_MARGIN = 18;
	/**
	 * Margin from mouse cursor at which to draw focusable tip.
	 */
	private static final int Y_MARGIN = 12;

	/**
	 * Returns localized text for the given key.
	 *
	 * @param key
	 *            The key into the resource bundle.
	 * @return The localized text.
	 */
	static String getString(final String key) {
		return FocusableTip.MSG.getString(key);
	}

	private final HyperlinkListener hyperlinkListener;

	private URL imageBase;

	private String lastText;

	private Dimension maxSize; // null to automatic.

	private JTextArea textArea;

	private final TextAreaListener textAreaListener;

	/**
	 * The screen bounds in which the mouse has to stay for the currently displayed
	 * tip to stay visible.
	 */
	private final Rectangle tipVisibleBounds;

	private TipWindow tipWindow;

	public FocusableTip(final JTextArea textArea, final HyperlinkListener listener) {
		this.setTextArea(textArea);
		this.hyperlinkListener = listener;
		this.textAreaListener = new TextAreaListener();
		this.tipVisibleBounds = new Rectangle();
	}

	/**
	 * Compute the bounds in which the user can move the mouse without the tip
	 * window disappearing.
	 */
	private void computeTipVisibleBounds() {
		// Compute area that the mouse can move in without hiding the
		// tip window. Note that Java 1.4 can only detect mouse events
		// in Java windows, not globally.
		final Rectangle r = this.tipWindow.getBounds();
		final Point p = r.getLocation();
		SwingUtilities.convertPointFromScreen(p, this.textArea);
		r.setLocation(p);
		this.tipVisibleBounds.setBounds(r.x, r.y - 15, r.width, r.height + 15 * 2);
	}

	private void createAndShowTipWindow(final MouseEvent e, final String text) {

		final Window owner = SwingUtilities.getWindowAncestor(this.textArea);
		this.tipWindow = new TipWindow(owner, this, text);
		this.tipWindow.setHyperlinkListener(this.hyperlinkListener);

		// Give apps a chance to decorate us with drop shadows, etc.
		final PopupWindowDecorator decorator = PopupWindowDecorator.get();
		if (decorator != null)
			decorator.decorate(this.tipWindow);

		// TODO: Position tip window better (handle RTL, edges of screen, etc.).
		// Wrap in an invokeLater() to work around a JEditorPane issue where it
		// doesn't return its proper preferred size until after it is displayed.
		// See http://forums.sun.com/thread.jspa?forumID=57&threadID=574810
		// for a discussion.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				// If a new FocusableTip is requested while another one is
				// *focused* and visible, the focused tip (i.e. "tipWindow")
				// will be disposed of. If this Runnable is run after the
				// dispose(), tipWindow will be null. All of this is done on
				// the EDT so no synchronization should be necessary.
				if (FocusableTip.this.tipWindow == null)
					return;

				FocusableTip.this.tipWindow.fixSize();
				final ComponentOrientation o = FocusableTip.this.textArea.getComponentOrientation();

				final Point p = e.getPoint();
				SwingUtilities.convertPointToScreen(p, FocusableTip.this.textArea);

				// Ensure tool tip is in the window bounds.
				// Multi-monitor support - make sure the completion window (and
				// description window, if applicable) both fit in the same
				// window in a multi-monitor environment. To do this, we decide
				// which monitor the rectangle "p" is in, and use that one.
				final Rectangle sb = TipUtil.getScreenBoundsForPoint(p.x, p.y);
				// Dimension ss = tipWindow.getToolkit().getScreenSize();

				// Try putting our stuff "below" the mouse first.
				int y = p.y + FocusableTip.Y_MARGIN;
				if (y + FocusableTip.this.tipWindow.getHeight() >= sb.y + sb.height)
					y = p.y - FocusableTip.Y_MARGIN - FocusableTip.this.tipWindow.getHeight();

				// Get x-coordinate of completions. Try to align left edge
				// with the mouse first (with a slight margin).
				int x = p.x - FocusableTip.X_MARGIN; // ltr
				if (!o.isLeftToRight())
					x = p.x - FocusableTip.this.tipWindow.getWidth() + FocusableTip.X_MARGIN;
				if (x < sb.x)
					x = sb.x;
				else if (x + FocusableTip.this.tipWindow.getWidth() > sb.x + sb.width)
					x = sb.x + sb.width - FocusableTip.this.tipWindow.getWidth();

				FocusableTip.this.tipWindow.setLocation(x, y);
				FocusableTip.this.tipWindow.setVisible(true);

				FocusableTip.this.computeTipVisibleBounds(); // Do after tip is visible
				FocusableTip.this.textAreaListener.install(FocusableTip.this.textArea);
				FocusableTip.this.lastText = text;

			}
		});

	}

	/**
	 * Returns the base URL to use when loading images in this focusable tip.
	 *
	 * @return The base URL to use.
	 * @see #setImageBase(URL)
	 */
	public URL getImageBase() {
		return this.imageBase;
	}

	/**
	 * The maximum size for unfocused tool tips.
	 *
	 * @return The maximum size for unfocused tool tips. A value of
	 *         <code>null</code> will use a default size.
	 * @see #setMaxSize(Dimension)
	 */
	public Dimension getMaxSize() {
		return this.maxSize;
	}

	/**
	 * Disposes of the focusable tip currently displayed, if any.
	 */
	public void possiblyDisposeOfTipWindow() {
		if (this.tipWindow != null) {
			this.tipWindow.dispose();
			this.tipWindow = null;
			this.textAreaListener.uninstall();
			this.tipVisibleBounds.setBounds(-1, -1, 0, 0);
			this.lastText = null;
			this.textArea.requestFocus();
		}
	}

	void removeListeners() {
		// System.out.println("DEBUG: Removing text area listeners");
		this.textAreaListener.uninstall();
	}

	/**
	 * Sets the base URL to use when loading images in this focusable tip.
	 *
	 * @param url
	 *            The base URL to use.
	 * @see #getImageBase()
	 */
	public void setImageBase(final URL url) {
		this.imageBase = url;
	}

	/**
	 * Sets the maximum size for unfocused tool tips.
	 *
	 * @param maxSize
	 *            The new maximum size. A value of <code>null</code> will cause a
	 *            default size to be used.
	 * @see #getMaxSize()
	 */
	public void setMaxSize(final Dimension maxSize) {
		this.maxSize = maxSize;
	}

	private void setTextArea(final JTextArea textArea) {
		this.textArea = textArea;
		// Is okay to do multiple times.
		ToolTipManager.sharedInstance().registerComponent(textArea);
	}

	public void toolTipRequested(final MouseEvent e, final String text) {

		if (text == null || text.length() == 0) {
			this.possiblyDisposeOfTipWindow();
			this.lastText = text;
			return;
		}

		if (this.lastText == null || text.length() != this.lastText.length() || !text.equals(this.lastText)) {
			this.possiblyDisposeOfTipWindow();
			this.createAndShowTipWindow(e, text);
		}

	}

}