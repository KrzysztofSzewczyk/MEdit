package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import medit.GoToLine;
import medit.MainFrame;

public class TextActionManager {

	private MainFrame instance;

	public TextActionManager(MainFrame instance) {
		this.instance = instance;
	}

	public void SetupText(JMenu mnTextOperations) {
		JMenu mnCase = new JMenu("Case");
		mnTextOperations.add(mnCase);
		JMenuItem mntmThisWay = new JMenuItem("THIS WAY");
		mntmThisWay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							instance.textPane.replaceSelection(instance.textPane.getSelectedText().toUpperCase());
						} catch (Exception e2) {
						}
					}
				}).start();

			}
		});
		mnCase.add(mntmThisWay);
		JMenuItem mntmThisWay_1 = new JMenuItem("this way");
		mntmThisWay_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							instance.textPane.replaceSelection(instance.textPane.getSelectedText().toLowerCase());
						} catch (Exception e2) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay_1);
		JMenuItem mntmThisWay_2 = new JMenuItem("This Way");
		mntmThisWay_2.addActionListener(new ActionListener() {
			public String toTitleCase(String givenString) {
				String[] arr = givenString.split(" ");
				StringBuffer sb = new StringBuffer();

				for (int i = 0; i < arr.length; i++) {
					sb.append(Character.toUpperCase(arr[i].charAt(0))).append(arr[i].substring(1)).append(" ");
				}
				return sb.toString().trim();
			}

			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							String text = instance.textPane.getSelectedText();
							instance.textPane.replaceSelection(toTitleCase(text));
						} catch (Exception e3) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay_2);
		JMenuItem mntmThisWay_4 = new JMenuItem("ThIs WaY");
		mntmThisWay_4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							char[] text = instance.textPane.getSelectedText().toCharArray();
							for (int i = 0; i < text.length; i++) {
								if (i == text.length % 2)
									text[i] = Character.toUpperCase(text[i]);
								else
									text[i] = Character.toLowerCase(text[i]);
							}
							instance.textPane.replaceSelection(new String(text));
						} catch (Exception e3) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmThisWay_4);
		JMenuItem mntmRandom = new JMenuItem("RanDOm");
		mntmRandom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							char[] text = instance.textPane.getSelectedText().toCharArray();
							for (int i = 0; i < text.length; i++) {
								if (new Random().nextInt(3) == 1)
									text[i] = Character.toUpperCase(text[i]);
								else
									text[i] = Character.toLowerCase(text[i]);
							}
							instance.textPane.replaceSelection(new String(text));
						} catch (Exception e3) {
						}
					}
				}).start();
			}
		});
		mnCase.add(mntmRandom);
		JMenu mnRow = new JMenu("Row");
		mnTextOperations.add(mnRow);
		JMenuItem mntmGoToRow = new JMenuItem("Go to row ...");
		mntmGoToRow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GoToLine gtlDlg = new GoToLine(instance);
				gtlDlg.setVisible(true);
				gtlDlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
		});
		mnRow.add(mntmGoToRow);
	}
}
