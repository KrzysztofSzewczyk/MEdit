/*
 * 07/29/2009
 *
 * TipWindow.java - The actual window component representing the tool tip.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.focusabletip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;

/**
 * The actual tool tip component.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class TipWindow extends JWindow implements ActionListener {

	/**
	 * Listens for events in this window.
	 */
	private final class TipListener extends MouseAdapter {

		private TipListener() {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			// Since we registered this listener on the child components of
			// the JWindow, not the JWindow iteself, we have to be careful.
			final Component source = (Component) e.getSource();
			final Point p = e.getPoint();
			SwingUtilities.convertPointToScreen(p, source);
			if (!TipWindow.this.getBounds().contains(p))
				TipWindow.this.ft.possiblyDisposeOfTipWindow();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			TipWindow.this.actionPerformed(null); // Manually create "real" window
		}

	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static TipWindow visibleInstance;
	private final FocusableTip ft;
	private final String text;
	private final JEditorPane textArea;

	private transient TipListener tipListener;

	private transient HyperlinkListener userHyperlinkListener;

	/**
	 * Constructor.
	 *
	 * @param owner
	 *            The parent window.
	 * @param msg
	 *            The text of the tool tip. This can be HTML.
	 */
	TipWindow(final Window owner, final FocusableTip ft, String msg) {

		super(owner);
		this.ft = ft;
		// Render plain text tool tips correctly.
		if (msg != null && msg.length() >= 6 && !msg.substring(0, 6).toLowerCase().equals("<html>"))
			msg = "<html>" + RSyntaxUtilities.escapeForHtml(msg, "<br>", false);
		this.text = msg;
		this.tipListener = new TipListener();

		final JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(TipUtil.getToolTipBorder());
		cp.setBackground(TipUtil.getToolTipBackground());
		this.textArea = new JEditorPane("text/html", this.text);
		TipUtil.tweakTipEditorPane(this.textArea);
		if (ft.getImageBase() != null)
			((HTMLDocument) this.textArea.getDocument()).setBase(ft.getImageBase());
		this.textArea.addMouseListener(this.tipListener);
		this.textArea.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				TipWindow.this.ft.possiblyDisposeOfTipWindow();
		});
		cp.add(this.textArea);

		this.setFocusableWindowState(false);
		this.setContentPane(cp);
		this.setBottomPanel(); // Must do after setContentPane()
		this.pack();

		// InputMap/ActionMap combo doesn't work for JWindows (even when
		// using the JWindow's JRootPane), so we'll resort to KeyListener
		final KeyAdapter ka = new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					TipWindow.this.ft.possiblyDisposeOfTipWindow();
			}
		};
		this.addKeyListener(ka);
		this.textArea.addKeyListener(ka);

		// Ensure only 1 TipWindow is ever visible. If the caller does what
		// they're supposed to and only creates these on the EDT, the
		// synchronization isn't necessary, but we'll be extra safe.
		synchronized (TipWindow.class) {
			if (TipWindow.visibleInstance != null)
				TipWindow.visibleInstance.dispose();
			TipWindow.visibleInstance = this;
		}

	}

	@Override
	public void actionPerformed(final ActionEvent e) {

		if (!this.getFocusableWindowState()) {
			this.setFocusableWindowState(true);
			this.setBottomPanel();
			this.textArea.removeMouseListener(this.tipListener);
			this.pack();
			this.addWindowFocusListener(new WindowAdapter() {
				@Override
				public void windowLostFocus(final WindowEvent e) {
					TipWindow.this.ft.possiblyDisposeOfTipWindow();
				}
			});
			this.ft.removeListeners();
			if (e == null)
				this.requestFocus();
		}

	}

	/**
	 * Disposes of this window.
	 */
	@Override
	public void dispose() {
		// System.out.println("[DEBUG]: Disposing...");
		final Container cp = this.getContentPane();
		for (int i = 0; i < cp.getComponentCount(); i++)
			// Okay if listener is already removed
			cp.getComponent(i).removeMouseListener(this.tipListener);
		this.ft.removeListeners();
		super.dispose();
	}

	/**
	 * Workaround for JEditorPane not returning its proper preferred size when
	 * rendering HTML until after layout already done. See
	 * http://forums.sun.com/thread.jspa?forumID=57&threadID=574810 for a
	 * discussion.
	 */
	void fixSize() {

		Dimension d = this.textArea.getPreferredSize();
		Rectangle r = null;
		try {

			// modelToView call is required for this hack, never remove!
			r = this.textArea.modelToView(this.textArea.getDocument().getLength() - 1);

			// Ensure the text area doesn't start out too tall or wide.
			d = this.textArea.getPreferredSize();
			d.width += 25; // Just a little extra space
			final int maxWindowW = this.ft.getMaxSize() != null ? this.ft.getMaxSize().width : 600;
			final int maxWindowH = this.ft.getMaxSize() != null ? this.ft.getMaxSize().height : 400;
			d.width = Math.min(d.width, maxWindowW);
			d.height = Math.min(d.height, maxWindowH);

			// Both needed for modelToView() calculation below...
			this.textArea.setPreferredSize(d);
			this.textArea.setSize(d);

			// if the new textArea width causes our text to wrap, we must
			// compute a new preferred size to get all our physical lines.
			r = this.textArea.modelToView(this.textArea.getDocument().getLength() - 1);
			if (r.y + r.height > d.height) {
				d.height = r.y + r.height + 5;
				if (this.ft.getMaxSize() != null)
					d.height = Math.min(d.height, maxWindowH);
				this.textArea.setPreferredSize(d);
			}

		} catch (final BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}

		this.pack(); // Must re-pack to calculate proper size.

	}

	public String getText() {
		return this.text;
	}

	private void setBottomPanel() {

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JSeparator(), BorderLayout.NORTH);

		final boolean focusable = this.getFocusableWindowState();
		if (focusable) {
			final SizeGrip sg = new SizeGrip();
			sg.applyComponentOrientation(sg.getComponentOrientation()); // Workaround
			panel.add(sg, BorderLayout.LINE_END);
			final MouseInputAdapter adapter = new MouseInputAdapter() {
				private Point lastPoint;

				@Override
				public void mouseDragged(final MouseEvent e) {
					final Point p = e.getPoint();
					SwingUtilities.convertPointToScreen(p, panel);
					if (this.lastPoint == null)
						this.lastPoint = p;
					else {
						final int dx = p.x - this.lastPoint.x;
						final int dy = p.y - this.lastPoint.y;
						TipWindow.this.setLocation(TipWindow.this.getX() + dx, TipWindow.this.getY() + dy);
						this.lastPoint = p;
					}
				}

				@Override
				public void mousePressed(final MouseEvent e) {
					this.lastPoint = e.getPoint();
					SwingUtilities.convertPointToScreen(this.lastPoint, panel);
				}
			};
			panel.addMouseListener(adapter);
			panel.addMouseMotionListener(adapter);
			// Don't add tipListener to the panel or SizeGrip
		} else {
			panel.setOpaque(false);
			final JLabel label = new JLabel(FocusableTip.getString("FocusHotkey"));
			Color fg = UIManager.getColor("Label.disabledForeground");
			Font font = this.textArea.getFont();
			font = font.deriveFont(font.getSize2D() - 1.0f);
			label.setFont(font);
			if (fg == null)
				fg = Color.GRAY;
			label.setOpaque(true);
			final Color bg = TipUtil.getToolTipBackground();
			label.setBackground(bg);
			label.setForeground(fg);
			label.setHorizontalAlignment(SwingConstants.TRAILING);
			label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			panel.add(label);
			panel.addMouseListener(this.tipListener);
		}

		// Replace the previous SOUTH Component with the new one.
		final Container cp = this.getContentPane();
		if (cp.getComponentCount() == 2) { // Skip first time through
			final Component comp = cp.getComponent(0);
			cp.remove(0);
			final JScrollPane sp = new JScrollPane(comp);
			final Border emptyBorder = BorderFactory.createEmptyBorder();
			sp.setBorder(emptyBorder);
			sp.setViewportBorder(emptyBorder);
			sp.setBackground(this.textArea.getBackground());
			sp.getViewport().setBackground(this.textArea.getBackground());
			cp.add(sp);
			// What was component 1 is now 0.
			cp.getComponent(0).removeMouseListener(this.tipListener);
			cp.remove(0);
		}

		cp.add(panel, BorderLayout.SOUTH);

	}

	/**
	 * Sets the listener for hyperlink events in this tip window.
	 *
	 * @param listener
	 *            The new listener. The old listener (if any) is removed. A value of
	 *            <code>null</code> means "no listener."
	 */
	public void setHyperlinkListener(final HyperlinkListener listener) {
		// We've added a separate listener, so remove only the user's.
		if (this.userHyperlinkListener != null)
			this.textArea.removeHyperlinkListener(this.userHyperlinkListener);
		this.userHyperlinkListener = listener;
		if (this.userHyperlinkListener != null)
			this.textArea.addHyperlinkListener(this.userHyperlinkListener);
	}

}