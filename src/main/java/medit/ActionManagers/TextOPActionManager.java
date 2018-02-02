package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import medit.GoToLine;
import medit.MainFrame;

public class TextOPActionManager {

	private final MainFrame instance;

	public TextOPActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	public void SetupTextOP(final JMenu mnTextOperations) {
		final JMenu mnCase = new JMenu("Case");
		mnTextOperations.add(mnCase);
		final JMenuItem mntmThisWay = new JMenuItem("THIS WAY");
		mntmThisWay.addActionListener(e -> new Thread(() -> {
			try {
				instance.textPane.replaceSelection(instance.textPane.getSelectedText().toUpperCase());
			} catch (final Exception e2) {
			}
		}).start());
		mnCase.add(mntmThisWay);
		final JMenuItem mntmThisWay_1 = new JMenuItem("this way");
		mntmThisWay_1.addActionListener(e -> new Thread(() -> {
			try {
				instance.textPane.replaceSelection(instance.textPane.getSelectedText().toLowerCase());
			} catch (final Exception e2) {
			}
		}).start());
		mnCase.add(mntmThisWay_1);
		final JMenuItem mntmThisWay_2 = new JMenuItem("This Way");
		mntmThisWay_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				new Thread(() -> {
					try {
						final String text = instance.textPane.getSelectedText();
						instance.textPane.replaceSelection(this.toTitleCase(text));
					} catch (final Exception e3) {
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
		mntmThisWay_4.addActionListener(e -> new Thread(() -> {
			try {
				final char[] text = instance.textPane.getSelectedText().toCharArray();
				for (int i = 0; i < text.length; i++)
					if (i == text.length % 2)
						text[i] = Character.toUpperCase(text[i]);
					else
						text[i] = Character.toLowerCase(text[i]);
				instance.textPane.replaceSelection(new String(text));
			} catch (final Exception e3) {
			}
		}).start());
		mnCase.add(mntmThisWay_4);
		final JMenuItem mntmRandom = new JMenuItem("RanDOm");
		mntmRandom.addActionListener(e -> new Thread(() -> {
			try {
				final char[] text = instance.textPane.getSelectedText().toCharArray();
				for (int i = 0; i < text.length; i++)
					if (new Random().nextInt(3) == 1)
						text[i] = Character.toUpperCase(text[i]);
					else
						text[i] = Character.toLowerCase(text[i]);
				instance.textPane.replaceSelection(new String(text));
			} catch (final Exception e3) {
			}
		}).start());
		mnCase.add(mntmRandom);
		final JMenu mnRow = new JMenu("Row");
		mnTextOperations.add(mnRow);
		final JMenuItem mntmGoToRow = new JMenuItem("Go to row ...");
		mntmGoToRow.addActionListener(e -> {
			final GoToLine gtlDlg = new GoToLine(instance.instance);
			gtlDlg.setVisible(true);
			gtlDlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		});
		mnRow.add(mntmGoToRow);
	}

}
