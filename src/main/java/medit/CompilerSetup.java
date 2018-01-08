package medit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class CompilerSetup extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField textField;
	private JTextField textField_1;
	private Compiler[] compilers = new Compiler[8];

	/**
	 * Create the dialog.
	 */
	public CompilerSetup() {
		setResizable(false);
		setIconImage(Toolkit.getDefaultToolkit().getImage(CompilerSetup.class.getResource("/medit/assets/actions/window-new.png")));
		setTitle("Compiler setup");
		setBounds(100, 100, 389, 206);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		contentPanel.add(panel, BorderLayout.NORTH);
		
		JButton button_1 = new JButton("< ");
		panel.add(button_1);
		
		JButton button = new JButton(" >");
		panel.add(button);
		
		JPanel panel_1 = new JPanel();
		contentPanel.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);
		
		JLabel lblPath = new JLabel("Path: ");
		panel_2.add(lblPath);
		
		textField = new JTextField();
		panel_2.add(textField);
		textField.setColumns(20);
		
		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_4 = new JPanel();
		panel_3.add(panel_4, BorderLayout.NORTH);
		
		JLabel lblName = new JLabel("Name: ");
		panel_4.add(lblName);
		
		textField_1 = new JTextField();
		panel_4.add(textField_1);
		textField_1.setColumns(20);
		
		JPanel panel_5 = new JPanel();
		panel_3.add(panel_5, BorderLayout.CENTER);
		panel_5.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_6 = new JPanel();
		panel_5.add(panel_6, BorderLayout.NORTH);
		
		JButton btnAddNew = new JButton("Add New");
		panel_6.add(btnAddNew);
		
		JButton btnRemoveCurrent = new JButton("Remove current");
		panel_6.add(btnRemoveCurrent);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							new File("compilers.txt").delete();
							new File("compilers.txt").createNewFile();
							PrintWriter w = new PrintWriter(new File("compilers.txt"));
							for(int i = 0; i < 8; i++) {
								w.println(compilers[i].name);
								w.println(compilers[i].file.getAbsolutePath());
							}
						} catch (FileNotFoundException e1) {
							Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						} catch (IOException e1) {
							Crash dialog = new Crash(e1);
							dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
							dialog.setVisible(true);
						}
						
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
		if(new File("compilers.txt").exists()) {
			try {
				Scanner s = new Scanner(new File("compilers.txt"));
				int i = s.nextShort(), iterator = 0;
				compilers = new Compiler[i];
				while(s.hasNextLine()) {
					if(iterator>=i) break;
					compilers[iterator].file = new File(s.nextLine());
					compilers[iterator].name = s.nextLine();
					iterator++;
				}
				s.close();
			} catch (FileNotFoundException e1) {
				Crash dialog = new Crash(e1);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
			
		}
	}
}
