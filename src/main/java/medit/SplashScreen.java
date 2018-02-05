package medit;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class SplashScreen extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public SplashScreen instance;
	public JLabel lblLogo = new JLabel("");
	public JProgressBar progressBar = new JProgressBar();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					SplashScreen frame = new SplashScreen();
					frame.setVisible(true);
					frame.dispose();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0); // Fixing #4
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public SplashScreen() {
		instance = this;
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent arg0) {
				new MainFrame(instance);
			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 612, 300);
		setUndecorated(true);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		lblLogo.setIcon(new ImageIcon(SplashScreen.class.getResource("/medit/assets/logo/MEdit.png")));
		contentPane.add(lblLogo, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		progressBar.setStringPainted(true);
		
		panel.add(progressBar);
		progressBar.setMaximum(21);
	}

}
