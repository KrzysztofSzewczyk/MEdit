package medit.API;

import javax.swing.JLabel;

/**
 * This is BottombarLabel class, that is kind of API interface for MainFrame
 * bottombar label.
 * 
 * @author Krzysztof Szewczyk
 *
 */

public class BottombarLabel {

	JLabel bbLabel;

	public BottombarLabel(JLabel instance) {
		bbLabel = instance;
	}

	public String GetText() {
		return bbLabel.getText();
	}

	public void Show() {
		bbLabel.setVisible(true);
	}

	public void Hide() {
		bbLabel.setVisible(false);
	}

}
