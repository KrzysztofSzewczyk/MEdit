package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.fife.ui.rsyntaxtextarea.Theme;

import com.bulenkov.darcula.DarculaLaf;

import medit.Crash;
import medit.MainFrame;

/**
 * This class is creating menu items for each of themes available in MEdit.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class ThemesActionManager {

	/**
	 * This is instance of MainFrame used by this class, to reference textPane
	 * variable.
	 */

	private final MainFrame instance;

	/**
	 * Constructor passing MainFrame instance.
	 * 
	 * @param instance
	 */

	public ThemesActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Function registering themes for 'parent' JMenu.
	 * 
	 * @param parent
	 */

	public void RegisterThemes(final JMenu parent) {
		final JMenuItem mntmDark = new JMenuItem("Dark");
		mntmDark.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});

		final JMenuItem mntmExtraDefault = new JMenuItem("Alternative MEdit");
		mntmExtraDefault.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default-alt.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmExtraDefault);
		parent.add(mntmDark);

		final JMenuItem mntmDefault = new JMenuItem("MEdit");
		mntmDefault.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmDefault);

		final JMenuItem mntmEclipse = new JMenuItem("Eclipse");
		mntmEclipse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmEclipse);

		final JMenuItem mntmIntellijIdea = new JMenuItem("IntelliJ IDEA");
		mntmIntellijIdea.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmIntellijIdea);

		final JMenuItem mntmMonokai = new JMenuItem("Monokai");
		mntmMonokai.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme.load(
							this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmMonokai);

		final JMenuItem mntmVisualStudio = new JMenuItem("Visual Studio");
		mntmVisualStudio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					final Theme theme = Theme
							.load(this.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
					theme.apply(ThemesActionManager.this.instance.textPane);
				} catch (final IOException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmVisualStudio);
		
		final JSeparator separator = new JSeparator();
		parent.add(separator);
		
		final JMenuItem mntmClassicLF = new JMenuItem("Classic L&F");
		mntmClassicLF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					//instance.setVisible(false);
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					SwingUtilities.updateComponentTreeUI(instance);
					instance.pack();
					//instance.setVisible(true);
				} catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmClassicLF);
		
		final JMenuItem mntmDarculaLF = new JMenuItem("Darcula L&F");
		mntmDarculaLF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try { 
					//instance.setVisible(false);
					UIManager.setLookAndFeel(new DarculaLaf());
					SwingUtilities.updateComponentTreeUI(instance);
					instance.pack();
					//instance.setVisible(true);
				} catch (final UnsupportedLookAndFeelException ioe) { // Never happens
					final Crash dialog = new Crash(ioe);
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
			}
		});
		parent.add(mntmDarculaLF);
	}

}
