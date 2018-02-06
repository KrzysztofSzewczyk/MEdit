package medit.NSS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.WindowConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import medit.Crash;

/**
 * This class is loading scripts for new script system.
 *
 * @author Krzysztof Szewczyk
 *
 */

public class NSSLoader {

	private final List<NSSEntry> tools = new ArrayList<>();

	/**
	 * This function is loading every scripts from selected file.
	 *
	 * @param string
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */

	public List<NSSEntry> loadAll(final String string) throws ParserConfigurationException, SAXException, IOException {
		if (!new File(string).exists())
			return null;
		final File inputFile = new File(string);
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		if (doc.getDocumentElement().getNodeName() != "medit") {
			final Crash dialog = new Crash(
					new Exception("Parent element in script config file has to be equal to \"medit\"!"));
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		final NodeList nList = doc.getElementsByTagName("script");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			final Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				final Element eElement = (Element) nNode;
				final String name = eElement.getElementsByTagName("name").item(0).getTextContent();
				final String script = eElement.getElementsByTagName("scriptfile").item(0).getTextContent();
				this.tools.add(new NSSEntry(name, script) {
					@Override
					public String getCodeFN() {
						return this.codefn;
					}

					@Override
					public String getName() {
						return this.name;
					}
				});
			}
		}
		return this.tools;
	}

}
