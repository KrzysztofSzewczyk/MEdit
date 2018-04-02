package medit.legacy;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import javax.swing.JScrollPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

public class TextPaneFactory {
	public static JScrollPane createNewScrollPane() {
		RSyntaxTextArea textPane = new RSyntaxTextArea();
		RTextScrollPane scrollPane = new RTextScrollPane();
		Theme theme = null;
		try {
			theme = Theme
					.load(new Object().getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		textPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
		scrollPane.setViewportView(textPane);

		textPane.clearParsers();
		textPane.setParserDelay(1);
		textPane.setAnimateBracketMatching(true);
		textPane.setAutoIndentEnabled(true);
		textPane.setAntiAliasingEnabled(true);
		textPane.setBracketMatchingEnabled(true);
		textPane.setCloseCurlyBraces(true);
		textPane.setCloseMarkupTags(true);
		textPane.setCodeFoldingEnabled(true);
		textPane.setHyperlinkForeground(Color.pink);
		textPane.setHyperlinksEnabled(true);
		textPane.setPaintMatchedBracketPair(true);
		textPane.setPaintTabLines(true);

		scrollPane.setIconRowHeaderEnabled(true);
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);

		theme.apply(textPane);
		return scrollPane;
	}
}
