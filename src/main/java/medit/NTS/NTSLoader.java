package medit.NTS;

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

public class NTSLoader {

	private final List<NTSEntry> tools = new ArrayList<>();

	public List<NTSEntry> loadAll(final String string) throws ParserConfigurationException, SAXException, IOException {
		if (!new File(string).exists())
			return null;
		final File inputFile = new File(string);
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		if (doc.getDocumentElement().getNodeName() != "medit") {
			final Crash dialog = new Crash(
					new Exception("Parent element in tool config file has to be equal to \"medit\"!"));
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		final NodeList nList = doc.getElementsByTagName("tool");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			final Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				final Element eElement = (Element) nNode;
				final String name = eElement.getElementsByTagName("name").item(0).getTextContent();
				final String script = eElement.getElementsByTagName("script").item(0).getTextContent();
				final String exeName = eElement.getElementsByTagName("exe").item(0).getTextContent();
				this.tools.add(new NTSEntry(name, script, exeName) {
					@Override
					public String getCode() {
						return this.code;
					}

					@Override
					public String getExeName() {
						return this.exeName;
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
