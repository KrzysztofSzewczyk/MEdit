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

	private List<NTSEntry> tools = new ArrayList<NTSEntry>();

	public List<NTSEntry> loadAll(String string) throws ParserConfigurationException, SAXException, IOException {
		if (!new File(string).exists())
			return null;
		File inputFile = new File(string);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		if (doc.getDocumentElement().getNodeName() != "medit") {
			final Crash dialog = new Crash(
					new Exception("Parent element in tool config file has to be equal to \"medit\"!"));
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		NodeList nList = doc.getElementsByTagName("tool");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String name = eElement.getElementsByTagName("name").item(0).getTextContent();
				String script = eElement.getElementsByTagName("script").item(0).getTextContent();
				String exeName = eElement.getElementsByTagName("exe").item(0).getTextContent();
				tools.add(new NTSEntry(name, script, exeName) {
					@Override
					public String getName() {
						return this.name;
					}
					@Override
					public String getCode() {
						return this.code;
					}
					@Override
					public String getExeName() {
						return this.exeName;
					}
				});
			}
		}
		return tools;
	}

}
