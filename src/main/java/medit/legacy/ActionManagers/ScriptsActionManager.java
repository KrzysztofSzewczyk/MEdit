package medit.legacy.ActionManagers;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import medit.NSS.MenuActionListener;
import medit.NSS.NSSEntry;
import medit.NSS.NSSLoader;
import medit.NSS.NSSRunnable;
import medit.legacy.Crash;
import medit.legacy.MainFrame;

/**
 * This class is allowing user to script MEdit using own scripts written in B++.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class ScriptsActionManager {
	/**
	 * MainFrame instance used by this class.
	 */
	private final MainFrame instance;

	/**
	 * Constructor that is assigning its parameter to internal instance variable.
	 *
	 * @param instance
	 */
	public ScriptsActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Setup scripts menu. This function is pretty self-documenting.
	 *
	 * @param mnTools
	 */
	public void SetupScripts(final JMenu mnTools) {
		final NSSLoader tsldr = new NSSLoader();
		try {
			List<NSSEntry> tools = null;
			try {
				tools = tsldr.loadAll(CodeCompletionActionManager.class.getProtectionDomain().getCodeSource()
						.getLocation().toURI().getPath() + File.separator + "scripts.xml");
			} catch (URISyntaxException e1) {
				JOptionPane.showMessageDialog(ScriptsActionManager.this.instance,
						"Could not locate scripts config file.", "Error.", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (tools != null)
				for (final NSSEntry e : tools) {
					new Thread(new NSSRunnable(e.getCodeFN()) {
						@Override
						public void run() {
							final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
							try {
								try {
									engine.eval(new FileReader(CodeCompletionActionManager.class.getProtectionDomain()
											.getCodeSource().getLocation().toURI().getPath() + File.separator
											+ "scripts/start.b++"));
								} catch (URISyntaxException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								engine.eval(new FileReader(this.codefn));
								final Invocable invocable = (Invocable) engine;
								try {
									invocable.invokeFunction("onLoad", ScriptsActionManager.this.instance);
								} catch (final NoSuchMethodException e) {
									JOptionPane.showMessageDialog(ScriptsActionManager.this.instance,
											"Script does not contain start function.", "Error.",
											JOptionPane.ERROR_MESSAGE);
									return;
								}
							} catch (FileNotFoundException | ScriptException e) {
								final Crash dialog = new Crash(e);
								dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
								dialog.setVisible(true);
								return;
							}
						}

					});

					final JMenuItem item = new JMenuItem(e.getName());
					item.addActionListener(new MenuActionListener(e.getCodeFN(), e.getName()) {
						@Override
						public void actionPerformed(final ActionEvent arg0) {
							new Thread(new NSSRunnable(e.getCodeFN()) {
								@Override
								public void run() {
									File codefile = null;
									try {
										codefile = new File(CodeCompletionActionManager.class.getProtectionDomain()
												.getCodeSource().getLocation().toURI().getPath() + File.separator
												+ this.codefn);
									} catch (URISyntaxException e1) {
										JOptionPane
												.showMessageDialog(ScriptsActionManager.this.instance,
														"Script referenced by \"scripts.xml\" file, named \""
																+ this.codefn + "\", does not exist.",
														"Error.", JOptionPane.ERROR_MESSAGE);
									}
									if (!codefile.exists()) {
										JOptionPane
												.showMessageDialog(ScriptsActionManager.this.instance,
														"Script referenced by \"scripts.xml\" file, named \""
																+ this.codefn + "\", does not exist.",
														"Error.", JOptionPane.ERROR_MESSAGE);
										return;
									} else {
										final ScriptEngine engine = new ScriptEngineManager()
												.getEngineByName("nashorn");
										try {
											try {
												engine.eval(new FileReader(
														CodeCompletionActionManager.class.getProtectionDomain()
																.getCodeSource().getLocation().toURI().getPath()
																+ File.separator + "scripts/start.b++"));
											} catch (URISyntaxException e1) {
												JOptionPane.showMessageDialog(ScriptsActionManager.this.instance,
														"Script referenced by \"scripts.xml\" file, named \""
																+ "scripts/start.b++" + "\", does not exist.",
														"Error.", JOptionPane.ERROR_MESSAGE);
											}
											engine.eval(new FileReader(this.codefn));
											final Invocable invocable = (Invocable) engine;
											try {
												invocable.invokeFunction("start", ScriptsActionManager.this.instance);
											} catch (final NoSuchMethodException e) {
												JOptionPane.showMessageDialog(ScriptsActionManager.this.instance,
														"Script does not contain start function.", "Error.",
														JOptionPane.ERROR_MESSAGE);
												return;
											}
										} catch (FileNotFoundException | ScriptException e) {
											final Crash dialog = new Crash(e);
											dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
											dialog.setVisible(true);
											return;
										}
									}
								}
							}).start();
						}
					});
					mnTools.add(item);
				}
		} catch (final ParserConfigurationException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (final SAXException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (final IOException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
	}
}
