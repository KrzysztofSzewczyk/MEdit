package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import org.fife.ui.rsyntaxtextarea.Theme;

import medit.Crash;
import medit.MainFrame;

public class ThemesActionManager {

	private MainFrame instance;
	
	public ThemesActionManager(MainFrame instance) {
		this.instance = instance;
	}
	
	public void RegisterThemes(JMenu parent) {
		JMenuItem mntmDark = new JMenuItem("Dark");
		mntmDark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		
		JMenuItem mntmExtraDefault = new JMenuItem("Extra Default");
		mntmExtraDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default-alt.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmExtraDefault);
		parent.add(mntmDark);
		
		JMenuItem mntmDefault = new JMenuItem("Default");
		mntmDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmDefault);
		
		JMenuItem mntmEclipse = new JMenuItem("Eclipse");
		mntmEclipse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmEclipse);
		
		JMenuItem mntmIntellijIdea = new JMenuItem("IntelliJ IDEA");
		mntmIntellijIdea.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmIntellijIdea);
		
		JMenuItem mntmMonokai = new JMenuItem("Monokai");
		mntmMonokai.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmMonokai);
		
		JMenuItem mntmVisualStudio = new JMenuItem("Visual Studio");
		mntmVisualStudio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
					theme.apply(instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmVisualStudio);
	}
	
}
