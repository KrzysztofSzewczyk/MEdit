/*
 * 09/20/2013
 *
 * CollapsibleSectionPanel - A panel that can show or hide its contents.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.rsta.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A panel that can show or hide contents anchored to its bottom via a shortcut.
 * Those contents "slide" in, since today's applications are all about fancy
 * smancy animations.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CollapsibleSectionPanel extends JPanel {

	/**
	 * Information about a "bottom component."
	 */
	private static class BottomComponentInfo {

		private Dimension _preferredSize;
		private final JComponent component;

		public BottomComponentInfo(final JComponent component) {
			this.component = component;
		}

		public Dimension getRealPreferredSize() {
			if (this._preferredSize == null)
				this._preferredSize = this.component.getPreferredSize();
			return this._preferredSize;
		}

		private void uiUpdated() {
			// Remove explicit size previously set
			this.component.setPreferredSize(null);
		}

	}

	private class HideBottomComponentAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			CollapsibleSectionPanel.this.hideBottomComponent();
		}

	}

	private class ShowBottomComponentAction extends AbstractAction {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final BottomComponentInfo bci;

		public ShowBottomComponentAction(final KeyStroke ks, final BottomComponentInfo bci) {
			this.putValue(Action.ACCELERATOR_KEY, ks);
			this.bci = bci;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			CollapsibleSectionPanel.this.showBottomComponent(this.bci);
		}

	}

	private static final int FRAME_MILLIS = 10;
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final boolean animate;
	private final List<BottomComponentInfo> bottomComponentInfos;
	private BottomComponentInfo currentBci;
	private boolean down;

	private boolean firstTick;

	private int tick;

	private Timer timer;

	private int totalTicks = 10;

	/**
	 * Constructor.
	 */
	public CollapsibleSectionPanel() {
		this(true);
	}

	/**
	 * Constructor.
	 *
	 * @param animate
	 *            Whether the collapsible sections should animate in.
	 */
	public CollapsibleSectionPanel(final boolean animate) {
		super(new BorderLayout());
		this.bottomComponentInfos = new ArrayList<>();
		this.installKeystrokes();
		this.animate = animate;
	}

	/**
	 * Adds a "bottom component." To show this component, you must call
	 * {@link #showBottomComponent(JComponent)} directly. Any previously displayed
	 * bottom component will be hidden.
	 *
	 * @param comp
	 *            The component to add.
	 * @see #addBottomComponent(KeyStroke, JComponent)
	 */
	public void addBottomComponent(final JComponent comp) {
		this.addBottomComponent(null, comp);
	}

	/**
	 * Adds a "bottom component" and binds its display to a key stroke. Whenever
	 * that key stroke is typed in a descendant of this panel, this component will
	 * be displayed. You can also display it programmatically by calling
	 * {@link #showBottomComponent(JComponent)}.
	 *
	 * @param ks
	 *            The key stroke to bind to the display of the component. If this
	 *            parameter is <code>null</code>, this method behaves exactly like
	 *            the {@link #addBottomComponent(JComponent)} overload.
	 * @param comp
	 *            The component to add.
	 * @return An action that displays this component. You can add this action to a
	 *         <code>JMenu</code>, for example, to alert the user of a way to
	 *         display the component.
	 * @see #addBottomComponent(JComponent)
	 */
	public Action addBottomComponent(final KeyStroke ks, final JComponent comp) {

		final BottomComponentInfo bci = new BottomComponentInfo(comp);
		this.bottomComponentInfos.add(bci);

		Action action = null;
		if (ks != null) {
			final InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			im.put(ks, ks);
			action = new ShowBottomComponentAction(ks, bci);
			this.getActionMap().put(ks, action);
		}
		return action;

	}

	private void createTimer() {
		this.timer = new Timer(CollapsibleSectionPanel.FRAME_MILLIS, new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				CollapsibleSectionPanel.this.tick++;
				if (CollapsibleSectionPanel.this.tick == CollapsibleSectionPanel.this.totalTicks) {
					CollapsibleSectionPanel.this.timer.stop();
					CollapsibleSectionPanel.this.timer = null;
					CollapsibleSectionPanel.this.tick = 0;
					final Dimension finalSize = CollapsibleSectionPanel.this.down ? new Dimension(0, 0)
							: CollapsibleSectionPanel.this.currentBci.getRealPreferredSize();
					CollapsibleSectionPanel.this.currentBci.component.setPreferredSize(finalSize);
					if (CollapsibleSectionPanel.this.down) {
						CollapsibleSectionPanel.this.remove(CollapsibleSectionPanel.this.currentBci.component);
						CollapsibleSectionPanel.this.currentBci = null;
					}
				} else {
					if (CollapsibleSectionPanel.this.firstTick) {
						if (CollapsibleSectionPanel.this.down)
							CollapsibleSectionPanel.this.focusMainComponent();
						else
							// We assume here that the component has some
							// focusable child we want to play with
							CollapsibleSectionPanel.this.currentBci.component.requestFocusInWindow();
						CollapsibleSectionPanel.this.firstTick = false;
					}
					final float proportion = !CollapsibleSectionPanel.this.down
							? (float) CollapsibleSectionPanel.this.tick / CollapsibleSectionPanel.this.totalTicks
							: 1f - (float) CollapsibleSectionPanel.this.tick / CollapsibleSectionPanel.this.totalTicks;
					final Dimension size = new Dimension(
							CollapsibleSectionPanel.this.currentBci.getRealPreferredSize());
					size.height = (int) (size.height * proportion);
					CollapsibleSectionPanel.this.currentBci.component.setPreferredSize(size);
				}
				CollapsibleSectionPanel.this.revalidate();
				CollapsibleSectionPanel.this.repaint();
			}
		});
		this.timer.setRepeats(true);
	}

	/**
	 * Attempt to focus the "center" component of this panel.
	 */
	private void focusMainComponent() {
		Component center = ((BorderLayout) this.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		if (center instanceof JScrollPane)
			center = ((JScrollPane) center).getViewport().getView();
		center.requestFocusInWindow();
	}

	/**
	 * Returns the currently displayed bottom component.
	 *
	 * @return The currently displayed bottom component. This will be
	 *         <code>null</code> if no bottom component is displayed.
	 */
	public JComponent getDisplayedBottomComponent() {
		// If a component is animating in or out, we consider it to be "not
		// displayed."
		if (this.currentBci != null && (this.timer == null || !this.timer.isRunning()))
			return this.currentBci.component;
		return null;
	}

	/**
	 * Hides the currently displayed "bottom" component with a slide-out animation.
	 *
	 * @see #showBottomComponent(JComponent)
	 */
	public void hideBottomComponent() {

		if (this.currentBci == null)
			return;
		if (!this.animate) {
			this.remove(this.currentBci.component);
			this.revalidate();
			this.repaint();
			this.currentBci = null;
			this.focusMainComponent();
			return;
		}

		if (this.timer != null) {
			if (this.down)
				return; // Already animating away
			this.timer.stop();
			this.tick = this.totalTicks - this.tick;
		}
		this.down = true;
		this.firstTick = true;

		this.createTimer();
		this.timer.start();

	}

	/**
	 * Installs standard keystrokes for this component.
	 */
	private void installKeystrokes() {

		final InputMap im = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final ActionMap am = this.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "onEscape");
		am.put("onEscape", new HideBottomComponentAction());

	}

	/**
	 * Sets the amount of time, in milliseconds, it should take for a "collapsible
	 * panel" to show or hide. The default is <code>120</code>.
	 *
	 * @param millis
	 *            The amount of time, in milliseconds.
	 */
	public void setAnimationTime(final int millis) {
		if (millis < 0)
			throw new IllegalArgumentException("millis must be >= 0");
		this.totalTicks = Math.max(millis / CollapsibleSectionPanel.FRAME_MILLIS, 1);
	}

	/**
	 * Displays a new "bottom" component. If a component is currently displayed at
	 * the "bottom," it is hidden.
	 *
	 * @param bci
	 *            The new bottom component.
	 * @see #hideBottomComponent()
	 */
	private void showBottomComponent(final BottomComponentInfo bci) {

		if (bci.equals(this.currentBci)) {
			this.currentBci.component.requestFocusInWindow();
			return;
		}

		// Remove currently displayed bottom component
		if (this.currentBci != null)
			this.remove(this.currentBci.component);
		this.currentBci = bci;
		this.add(this.currentBci.component, BorderLayout.SOUTH);
		if (!this.animate) {
			this.currentBci.component.requestFocusInWindow();
			this.revalidate();
			this.repaint();
			return;
		}

		if (this.timer != null)
			this.timer.stop();
		this.tick = 0;
		this.down = false;
		this.firstTick = true;

		// Animate display of new bottom component.
		this.createTimer();
		this.timer.start();

	}

	/**
	 * Displays a previously-registered "bottom component."
	 *
	 * @param comp
	 *            A previously registered component.
	 * @see #addBottomComponent(JComponent)
	 * @see #addBottomComponent(KeyStroke, JComponent)
	 * @see #hideBottomComponent()
	 */
	public void showBottomComponent(final JComponent comp) {

		BottomComponentInfo info = null;
		for (final BottomComponentInfo bci : this.bottomComponentInfos)
			if (bci.component == comp) {
				info = bci;
				break;
			}

		if (info != null)
			this.showBottomComponent(info);

	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (this.bottomComponentInfos != null)
			for (final BottomComponentInfo info : this.bottomComponentInfos) {
				if (!info.component.isDisplayable())
					SwingUtilities.updateComponentTreeUI(info.component);
				info.uiUpdated();
			}
	}

}