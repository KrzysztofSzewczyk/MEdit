package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

import medit.Crash;
import medit.MainFrame;
import medit.NSS.MenuActionListener;
import medit.NSS.NSSEntry;
import medit.NSS.NSSLoader;
import medit.NSS.NSSRunnable;

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
			final List<NSSEntry> tools = tsldr.loadAll("scripts.xml");
			if (tools != null)
				for (final NSSEntry e : tools) {
					final JMenuItem item = new JMenuItem(e.getName());
					item.addActionListener(new MenuActionListener(e.getCodeFN(), e.getName()) {
						@Override
						public void actionPerformed(final ActionEvent arg0) {
							new Thread(new NSSRunnable(e.getCodeFN()) {
								@Override
								public void run() {
									if (ScriptsActionManager.this.instance.currentFile == null) {
										JOptionPane.showMessageDialog(ScriptsActionManager.this.instance,
												"Please save your work in order to execute any tool.", "Error.",
												JOptionPane.ERROR_MESSAGE);
										return;
									}
									File codefile = new File(this.codefn);
									if (!codefile.exists()) {
										JOptionPane
												.showMessageDialog(ScriptsActionManager.this.instance,
														"Script referenced by \"scripts.xml\" file, named \""
																+ this.codefn + "\", does not exist.",
														"Error.", JOptionPane.ERROR_MESSAGE);
										return;
									} else {
										ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
										try {
											engine.eval(new FileReader(this.codefn));
											Invocable invocable = (Invocable) engine;
											try {
												invocable.invokeFunction("start", instance);
											} catch (NoSuchMethodException e) {
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
