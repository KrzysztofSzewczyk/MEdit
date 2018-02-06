package medit;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class SplashScreen extends JFrame {

	private static final long serialVersionUID = 1L;

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					// System.setProperty("sun.java2d.ddscale", "true");
					// UIManager.setLookAndFeel("com.birosoft.liquid.LiquidLookAndFeel");
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					final SplashScreen frame = new SplashScreen();
					frame.setVisible(true);
					frame.dispose();
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(0); // Fixing #4
				}
			}
		});
	}

	private final JPanel contentPane;
	public SplashScreen instance;
	public JLabel lblLogo = new JLabel("");

	public JProgressBar progressBar = new JProgressBar();

	/**
	 * Create the frame.
	 */
	public SplashScreen() {
		this.instance = this;
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent arg0) {
				new MainFrame(SplashScreen.this.instance);
			}
		});
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(100, 100, 612, 300);
		this.setUndecorated(true);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.setContentPane(this.contentPane);

		this.lblLogo.setIcon(new ImageIcon(SplashScreen.class.getResource("/medit/assets/logo/MEdit.png")));
		this.contentPane.add(this.lblLogo, BorderLayout.CENTER);

		final JPanel panel = new JPanel();
		this.contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		this.progressBar.setStringPainted(true);

		panel.add(this.progressBar);
		this.progressBar.setMaximum(21);
	}

}
