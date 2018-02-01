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

public class ToolActionManager {
	private MainFrame instance;

	public ToolActionManager(MainFrame instance) {
		this.instance = instance;
	}

	public void SetupTools(JMenu mnTools) {
		NTSLoader tsldr = new NTSLoader();
		try {
			List<NTSEntry> tools = tsldr.loadAll("tools.xml");
			if (tools != null) {
				for (NTSEntry e : tools) {
					JMenuItem item = new JMenuItem(e.getName());
					item.addActionListener(new MenuActionListener(e.getCode(), e.getName(), e.getExeName()) {

						@Override
						public void actionPerformed(ActionEvent arg0) {
							new Thread(new NTSRunnable(e.getExeName(), e.getCode()) {
								public String getProcessOutput(ProcessBuilder processBuilder)
										throws IOException, InterruptedException {
									processBuilder.redirectErrorStream(true);

									Process process = processBuilder.start();
									StringBuilder processOutput = new StringBuilder();

									try (BufferedReader processOutputReader = new BufferedReader(
											new InputStreamReader(process.getInputStream()));) {
										String readLine;

										while ((readLine = processOutputReader.readLine()) != null) {
											processOutput.append(readLine + System.lineSeparator());
										}

										process.waitFor();
									}

									return processOutput.toString().trim();
								}

								@Override
								public void run() {
									if (instance.currentFile == null) {
										JOptionPane.showMessageDialog(instance,
												"Please save your work in order to execute any tool.", "Error.",
												JOptionPane.ERROR_MESSAGE);
										return;
									}
									String currentCode = this.code;
									currentCode = currentCode.replaceAll("\\$\\(FILE\\)",
											instance.currentFile.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\")); // Windows
																													// bug
																													// workaround
									ProcessBuilder builder = new ProcessBuilder(this.exeName, currentCode);
									builder.directory(instance.currentFile.getParentFile());

									CommandOutputDialog dialog;
									try {
										dialog = new CommandOutputDialog(getProcessOutput(builder));
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
			}
		} catch (ParserConfigurationException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (SAXException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (IOException e) {
			final Crash dialog = new Crash(e);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
	}
}
