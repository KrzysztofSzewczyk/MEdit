package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import medit.GoToLine;
import medit.MainFrame;

/**
 * This class contains code, that is embedding items into Text Operations menu.
 * It's created on MEdit ActionManager template.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class TextOPActionManager {

	/**
	 * MainFrame instance used in this class.
	 */
	private final MainFrame instance;

	/**
	 * Creating TextOPActionManager, by passing MainFrame instance.
	 *
	 * @param instance
	 */
	public TextOPActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Creating menu items for Text Operations menu.
	 *
	 * @param mnTextOperations
	 */

	public void SetupTextOP(final JMenu mnTextOperations) {
		final JMenu mnCase = new JMenu("Case");
		mnTextOperations.add(mnCase);
		final JMenuItem mntmThisWay = new JMenuItem("THIS WAY");
		/**
		 * Creating action listener for uppercasing menu item, which is creating new
		 * thread to uppercase selected text.
		 */
		mntmThisWay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							TextOPActionManager.this.instance.textPane.replaceSelection(
									TextOPActionManager.this.instance.textPane.getSelectedText().toUpperCase());
						} catch (final Exception e2) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay);
		final JMenuItem mntmThisWay_1 = new JMenuItem("this way");
		/**
		 * Creating action listener for lowercasing menu item, which is creating new
		 * thread to lowercasing selected text.
		 */
		mntmThisWay_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							TextOPActionManager.this.instance.textPane.replaceSelection(
									TextOPActionManager.this.instance.textPane.getSelectedText().toLowerCase());
						} catch (final Exception e2) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay_1);
		final JMenuItem mntmThisWay_2 = new JMenuItem("This Way");
		/**
		 * Creating action listener for uppercasing first letter of each word menu item,
		 * which is creating new thread to uppercase first letter of each word of
		 * selected text.
		 */
		mntmThisWay_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final String text = TextOPActionManager.this.instance.textPane.getSelectedText();
							TextOPActionManager.this.instance.textPane
									.replaceSelection(toTitleCase(text));
						} catch (final Exception e3) {
						}
					}
				}).start();
			}

			public String toTitleCase(final String givenString) {
				final String[] arr = givenString.split(" ");
				final StringBuffer sb = new StringBuffer();

				for (final String element : arr)
					sb.append(Character.toUpperCase(element.charAt(0))).append(element.substring(1)).append(" ");
				return sb.toString().trim();
			}
		});
		mnCase.add(mntmThisWay_2);
		final JMenuItem mntmThisWay_4 = new JMenuItem("ThIs WaY");
		/**
		 * Creating action listener for uppercasing letter, which index mod 2 = 0 menu
		 * item, which is creating new thread to uppercase letter, which index mod 2 = 0
		 * of selected text.
		 */
		mntmThisWay_4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final char[] text = TextOPActionManager.this.instance.textPane.getSelectedText()
									.toCharArray();
							for (int i = 0; i < text.length; i++)
								if (i == text.length % 2)
									text[i] = Character.toUpperCase(text[i]);
								else
									text[i] = Character.toLowerCase(text[i]);
							TextOPActionManager.this.instance.textPane.replaceSelection(new String(text));
						} catch (final Exception e3) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay_4);
		final JMenuItem mntmRandom = new JMenuItem("RanDOm");
		/**
		 * Creating action listener for uppercasing and lowercasing random letters in
		 * menu item, which is creating new thread to uppercase and lowercase random
		 * letters in selected text.
		 */
		mntmRandom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final char[] text = TextOPActionManager.this.instance.textPane.getSelectedText()
									.toCharArray();
							for (int i = 0; i < text.length; i++)
								if (new Random().nextInt(3) == 1)
									text[i] = Character.toUpperCase(text[i]);
								else
									text[i] = Character.toLowerCase(text[i]);
							TextOPActionManager.this.instance.textPane.replaceSelection(new String(text));
						} catch (final Exception e3) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmRandom);
		final JMenu mnRow = new JMenu("Row");
		mnTextOperations.add(mnRow);
		final JMenuItem mntmGoToRow = new JMenuItem("Go to row ...");
		/**
		 * This is actionlistener which is creating 'Go to row ...' dialog.
		 */
		mntmGoToRow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GoToLine gtlDlg = new GoToLine(TextOPActionManager.this.instance.instance);
				gtlDlg.setVisible(true);
				gtlDlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
		});
		mnRow.add(mntmGoToRow);
	}

}
