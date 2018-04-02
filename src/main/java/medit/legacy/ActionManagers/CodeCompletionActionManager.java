package medit.legacy.ActionManagers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.WindowConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import medit.legacy.Crash;
import medit.legacy.MainFrame;

/**
 * This very important class is setting up code completion for MEdit
 *
 * @author Krzysztof Szewczyk
 *
 */

public class CodeCompletionActionManager {

	/**
	 * MainFrame instance used by this class to reference bottombar.
	 */

	private final MainFrame instance;

	/**
	 * This field is storing old autocompletion to get it removed later.
	 */

	private AutoCompletion oldAC;

	/**
	 * This is constructor that we pass MainFrame instance to.
	 *
	 * @param instance
	 */

	public CodeCompletionActionManager(final MainFrame instance) {
		this.instance = instance;
	}

	/**
	 * Get code completion entries
	 */
	private CompletionProvider createCompletionProvider(final String language) {
		final DefaultCompletionProvider provider = new DefaultCompletionProvider();
		try {
			if (new File(CodeCompletionActionManager.class.getProtectionDomain().getCodeSource().getLocation().toURI()
					.getPath() + File.separator + "completion.xml").exists()) {
				final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = null;
				try {
					dBuilder = dbFactory.newDocumentBuilder();
				} catch (final ParserConfigurationException e1) {
					final Crash dialog2 = new Crash(e1);
					dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog2.setVisible(true);
				}
				Document doc = null;
				try {
					doc = dBuilder.parse(new File("completion.xml"));
				} catch (SAXException | IOException e) {
					final Crash dialog2 = new Crash(e);
					dialog2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog2.setVisible(true);
				}
				doc.getDocumentElement().normalize();
				if (doc.getDocumentElement().getNodeName() != "medit") {
					final Crash dialog = new Crash(new Exception(
							"Parent element in code completion config file has to be equal to \"medit\"!"));
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				}
				final NodeList nList = doc.getElementsByTagName(language);
				for (int temp = 0; temp < nList.getLength(); temp++) {
					final Node nNode = nList.item(temp);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						final String completion = ((Element) nNode).getElementsByTagName("completion").item(0)
								.getTextContent();
						provider.addCompletion(new BasicCompletion(provider, completion));
					}
				}
			}
		} catch (DOMException | URISyntaxException e) {
			final Crash dialog = new Crash(new Exception("Error: Could not find config file."));
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}

		return provider;

	}

	/**
	 * Setup code completion
	 */

	public void SetUpCodeCompletion(String language) {
		language = language.substring(5);
		if (this.oldAC != null)
			this.oldAC.uninstall();
		final CompletionProvider provider = this.createCompletionProvider(language);
		final AutoCompletion ac = new AutoCompletion(provider);
		ac.setAutoActivationDelay(100);
		ac.install(this.instance.textPane);
		this.oldAC = ac;
	}
}
