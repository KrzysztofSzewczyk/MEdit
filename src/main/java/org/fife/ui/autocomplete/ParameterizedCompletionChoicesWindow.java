/*
 * 12/11/2010
 *
 * ParameterizedCompletionChoicesWindow.java - A list of likely choices for a
 * parameter.
 *
 * This library is distributed under a modified BSD license.  See the included
 * AutoComplete.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.PopupWindowDecorator;

/**
 * A small popup window offering a list of likely choices for a parameter when
 * the user has code-completed a parameterized completion. For example, if they
 * have just code-completed the C function "<code>fprintf</code>", when entering
 * the file name, this popup might display all local variables of type
 * "<code>char *</code>".
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class ParameterizedCompletionChoicesWindow extends JWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Comparator used to sort completions by their relevance before sorting them
	 * lexicographically.
	 */
	private static final Comparator<Completion> sortByRelevanceComparator = new SortByRelevanceComparator();

	/**
	 * The parent AutoCompletion instance.
	 */
	private final AutoCompletion ac;

	/**
	 * A list of lists of choices for each parameter.
	 */
	private List<List<Completion>> choicesListList;

	/**
	 * The list of completion choices.
	 */
	private final JList list;

	/**
	 * The currently displayed completion choices.
	 */
	private final DefaultListModel model;

	/**
	 * The scroll pane containing the list.
	 */
	private final JScrollPane sp;

	/**
	 * Constructor.
	 *
	 * @param parent
	 *            The parent window (hosting the text component).
	 * @param ac
	 *            The auto-completion instance.
	 * @param context
	 *            The completion context.
	 */
	public ParameterizedCompletionChoicesWindow(final Window parent, final AutoCompletion ac,
			final ParameterizedCompletionContext context) {

		super(parent);
		this.ac = ac;
		final ComponentOrientation o = ac.getTextComponentOrientation();

		this.model = new DefaultListModel();
		this.list = new JList(this.model);
		if (ac.getParamChoicesRenderer() != null)
			this.list.setCellRenderer(ac.getParamChoicesRenderer());
		this.list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2)
					context.insertSelectedChoice();
			}
		});
		this.sp = new JScrollPane(this.list);

		this.setContentPane(this.sp);
		this.applyComponentOrientation(o);
		this.setFocusableWindowState(false);

		// Give apps a chance to decorate us with drop shadows, etc.
		final PopupWindowDecorator decorator = PopupWindowDecorator.get();
		if (decorator != null)
			decorator.decorate(this);

	}

	/**
	 * Returns the selected value.
	 *
	 * @return The selected value, or <code>null</code> if nothing is selected.
	 */
	public String getSelectedChoice() {
		final Completion c = (Completion) this.list.getSelectedValue();
		return c == null ? null : c.toString();
	}

	/**
	 * Changes the selected index.
	 *
	 * @param amount
	 *            The amount by which to change the selected index.
	 */
	public void incSelection(final int amount) {
		int selection = this.list.getSelectedIndex();
		selection += amount;
		if (selection < 0)
			// Account for nothing selected yet
			selection = this.model.getSize() - 1;// += model.getSize();
		else
			selection %= this.model.getSize();
		this.list.setSelectedIndex(selection);
		this.list.ensureIndexIsVisible(selection);
	}

	/**
	 * Initializes this window to offer suggestions for the parameters of a specific
	 * completion.
	 *
	 * @param pc
	 *            The completion whose parameters we should offer suggestions for.
	 */
	public void initialize(final ParameterizedCompletion pc) {

		final CompletionProvider provider = pc.getProvider();
		final ParameterChoicesProvider pcp = provider.getParameterChoicesProvider();
		if (pcp == null) {
			this.choicesListList = null;
			return;
		}

		final int paramCount = pc.getParamCount();
		this.choicesListList = new ArrayList<>(paramCount);
		final JTextComponent tc = this.ac.getTextComponent();

		for (int i = 0; i < paramCount; i++) {
			final ParameterizedCompletion.Parameter param = pc.getParam(i);
			final List<Completion> choices = pcp.getParameterChoices(tc, param);
			this.choicesListList.add(choices);
		}

	}

	/**
	 * Sets the location of this window relative to the given rectangle.
	 *
	 * @param r
	 *            The visual position of the caret (in screen coordinates).
	 */
	public void setLocationRelativeTo(final Rectangle r) {

		// Multi-monitor support - make sure the completion window (and
		// description window, if applicable) both fit in the same window in
		// a multi-monitor environment. To do this, we decide which monitor
		// the rectangle "r" is in, and use that one (just pick top-left corner
		// as the defining point).
		final Rectangle screenBounds = Util.getScreenBoundsForPoint(r.x, r.y);
		// Dimension screenSize = tooltip.getToolkit().getScreenSize();

		// Try putting our stuff "below" the caret first.
		final int y = r.y + r.height + 5;

		// Get x-coordinate of completions. Try to align left edge with the
		// caret first.
		int x = r.x;
		if (x < screenBounds.x)
			x = screenBounds.x;
		else if (x + this.getWidth() > screenBounds.x + screenBounds.width)
			x = screenBounds.x + screenBounds.width - this.getWidth();

		this.setLocation(x, y);

	}

	/**
	 * Displays the choices for the specified parameter matching the given text.
	 * This will display or hide this popup window as necessary.
	 *
	 * @param param
	 *            The index of the parameter the caret is currently in. This may be
	 *            <code>-1</code> if not in a parameter (i.e., on the comma between
	 *            parameters).
	 * @param prefix
	 *            Text in the parameter before the dot. This may be
	 *            <code>null</code> to represent the empty string.
	 */
	public void setParameter(final int param, final String prefix) {

		this.model.clear();
		final List<Completion> temp = new ArrayList<>();

		if (this.choicesListList != null && param >= 0 && param < this.choicesListList.size()) {

			final List<Completion> choices = this.choicesListList.get(param);
			if (choices != null)
				for (final Completion c : choices) {
					final String choice = c.getReplacementText();
					if (prefix == null || Util.startsWithIgnoreCase(choice, prefix))
						temp.add(c);
				}

			// Sort completions appropriately.
			Comparator<Completion> c = null;
			if (/* sortByRelevance */true)
				c = ParameterizedCompletionChoicesWindow.sortByRelevanceComparator;
			Collections.sort(temp, c);
			for (int i = 0; i < temp.size(); i++)
				this.model.addElement(temp.get(i));

			final int visibleRowCount = Math.min(this.model.size(), 10);
			this.list.setVisibleRowCount(visibleRowCount);

			// Toggle visibility, if necessary.
			if (visibleRowCount == 0 && this.isVisible())
				this.setVisible(false);
			else if (visibleRowCount > 0) {
				Dimension size = this.getPreferredSize();
				if (size.width < 150)
					this.setSize(150, size.height);
				else
					this.pack();
				// Make sure nothing is ever obscured by vertical scroll bar.
				if (this.sp.getVerticalScrollBar() != null && this.sp.getVerticalScrollBar().isVisible()) {
					size = this.getSize();
					final int w = size.width + this.sp.getVerticalScrollBar().getWidth() + 5;
					this.setSize(w, size.height);
				}
				this.list.setSelectedIndex(0);
				this.list.ensureIndexIsVisible(0);
				if (!this.isVisible())
					this.setVisible(true);
			}

		} else
			this.setVisible(false);

	}

	/**
	 * Toggles the visibility of this popup window.
	 *
	 * @param visible
	 *            Whether this window should be visible.
	 */
	@Override
	public void setVisible(final boolean visible) {
		if (visible != this.isVisible()) {
			// i.e. if no possibilities matched what's been typed
			if (visible && this.model.size() == 0)
				return;
			super.setVisible(visible);
		}
	}

	/**
	 * Updates the <tt>LookAndFeel</tt> of this window.
	 */
	public void updateUI() {
		SwingUtilities.updateComponentTreeUI(this);
	}

}