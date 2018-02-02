package medit.ActionManagers;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import medit.CommandOutputDialog;
import medit.Crash;
import medit.MainFrame;
import medit.NTS.MenuActionListener;
import medit.NTS.NTSEntry;
import medit.NTS.NTSLoader;
import medit.NTS.NTSRunnable;

/**
 * This class is nearly copy+paste of ScriptActionManager.
 * It's aim is to create elements in tools menu based on
 * tools.xml file.
 * @author Krzysztof Szewczyk
 *
 */

public class ToolActionManager {
	/**
	 * MainFrame instance used by this class
	 */
	
	private final MainFrame instance;

	/**
	 * Constructor assigning passed instance to internal instance.
	 * @param instance
	 */
	
	public ToolActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * This function is setting up tools menu.
	 * @param mnTools
	 */
	
	public void SetupTools(final JMenu mnTools) {
		final NTSLoader tsldr = new NTSLoader();
		try {
			final List<NTSEntry> tools = tsldr.loadAll("tools.xml");
			if (tools != null)
				for (final NTSEntry e : tools) {
					final JMenuItem item = new JMenuItem(e.getName());
					item.addActionListener(new MenuActionListener(e.getCode(), e.getName(), e.getExeName()) {

						@Override
						public void actionPerformed(final ActionEvent arg0) {
							new Thread(new NTSRunnable(e.getExeName(), e.getCode()) {
								public String getProcessOutput(final ProcessBuilder processBuilder)
										throws IOException, InterruptedException {
									processBuilder.redirectErrorStream(true);

									final Process process = processBuilder.start();
									final StringBuilder processOutput = new StringBuilder();

									try (BufferedReader processOutputReader = new BufferedReader(
											new InputStreamReader(process.getInputStream()));) {
										String readLine;

										while ((readLine = processOutputReader.readLine()) != null)
											processOutput.append(readLine + System.lineSeparator());

										process.waitFor();
									}

									return processOutput.toString().trim();
								}

								@Override
								public void run() {
									if (ToolActionManager.this.instance.currentFile == null) {
										JOptionPane.showMessageDialog(ToolActionManager.this.instance,
												"Please save your work in order to execute any tool.", "Error.",
												JOptionPane.ERROR_MESSAGE);
										return;
									}
									String currentCode = this.code;
									currentCode = currentCode.replaceAll("\\$\\(FILE\\)",
											ToolActionManager.this.instance.currentFile.getAbsolutePath()
													.replaceAll("\\\\", "\\\\\\\\")); // Windows
									// bug
									// workaround
									final ProcessBuilder builder = new ProcessBuilder(this.exeName, currentCode);
									builder.directory(ToolActionManager.this.instance.currentFile.getParentFile());

									CommandOutputDialog dialog;
									try {
										dialog = new CommandOutputDialog(this.getProcessOutput(builder));
										dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
										dialog.setVisible(true);
									} catch (IOException | InterruptedException e) {
										final Crash dlg = new Crash(e);
										dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
										dlg.setVisible(true);
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
