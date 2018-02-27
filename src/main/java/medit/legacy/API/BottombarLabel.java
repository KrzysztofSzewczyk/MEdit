package medit.legacy.API;

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

	public BottombarLabel(final JLabel instance) {
		this.bbLabel = instance;
	}

	public String GetText() {
		return this.bbLabel.getText();
	}

	public void Hide() {
		this.bbLabel.setVisible(false);
	}

	public void Show() {
		this.bbLabel.setVisible(true);
	}

}
