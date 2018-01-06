package medit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

import javax.swing.ImageIcon;
import java.awt.Toolkit;
import javax.swing.JScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main frame for MEdit project.
 * @author Krzysztof Szewczyk
 */

public class MainFrame extends JFrame {

	/**
	 * Serial version UID required by Eclipse
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private static int instances = 1;

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				if(instances==0) System.exit(0);
			}
		});
		setIconImage(Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource("/medit/assets/apps/accessories-text-editor.png")));
		setTitle("MEdit");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 467, 460);
		this.setMinimumSize(new Dimension(460,460));
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							MainFrame frame = new MainFrame();
							frame.setVisible(true);
							instances++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		mnFile.add(mntmNew);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mnFile.add(mntmSave);
		
		JMenuItem mntmSaveAs = new JMenuItem("Save As...");
		mnFile.add(mntmSaveAs);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmCut = new JMenuItem("Cut");
		mnEdit.add(mntmCut);
		
		JMenuItem mntmCopy = new JMenuItem("Copy");
		mnEdit.add(mntmCopy);
		
		JMenuItem mntmPaste = new JMenuItem("Paste");
		mnEdit.add(mntmPaste);
		
		JMenuItem mntmDelete = new JMenuItem("Delete");
		mnEdit.add(mntmDelete);
		
		JSeparator separator_1 = new JSeparator();
		mnEdit.add(separator_1);
		
		JMenuItem mntmSearch = new JMenuItem("Search");
		mnEdit.add(mntmSearch);
		
		JMenuItem mntmSearchAndReplace = new JMenuItem("Search and replace");
		mnEdit.add(mntmSearchAndReplace);
		
		JMenuItem mntmCountOccurences = new JMenuItem("Count occurences...");
		mnEdit.add(mntmCountOccurences);
		
		JSeparator separator_4 = new JSeparator();
		mnEdit.add(separator_4);
		
		JMenuItem mntmUndo = new JMenuItem("Undo");
		mnEdit.add(mntmUndo);
		
		JMenuItem mntmRedo = new JMenuItem("Redo");
		mnEdit.add(mntmRedo);
		
		JSeparator separator_2 = new JSeparator();
		mnEdit.add(separator_2);
		
		JMenuItem mntmFont = new JMenuItem("Font");
		mnEdit.add(mntmFont);
		
		JMenu mnWorkspace = new JMenu("Workspace");
		menuBar.add(mnWorkspace);
		
		JMenuItem mntmDocumentediting = new JMenuItem("Document editing");
		mnWorkspace.add(mntmDocumentediting);
		
		JMenuItem mntmProgramming = new JMenuItem("Programming");
		mnWorkspace.add(mntmProgramming);
		
		JMenu mnLanguage = new JMenu("Language");
		menuBar.add(mnLanguage);
		
		JMenuItem mntmPolish = new JMenuItem("Polish");
		mnLanguage.add(mntmPolish);
		
		JMenuItem mntmEnglish = new JMenuItem("English");
		mnLanguage.add(mntmEnglish);
		
		JMenu mnAbout = new JMenu("About");
		menuBar.add(mnAbout);
		
		JMenuItem mntmPreferences = new JMenuItem("Preferences");
		mnAbout.add(mntmPreferences);
		
		JMenuItem mntmAbout = new JMenuItem("About MEdit");
		mntmAbout.addActionListener(new ActionListener() {
			/**
			 * MEdit About Box action listener.
			 */
			public void actionPerformed(ActionEvent arg0) {
				AboutBox dialog = new AboutBox();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		mnAbout.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		contentPane.add(toolBar, BorderLayout.NORTH);
		
		JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							MainFrame frame = new MainFrame();
							frame.setVisible(true);
							instances++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
		btnNewButton.setToolTipText("Create new file");
		btnNewButton.setFocusPainted(false);
		btnNewButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-new.png")));
		toolBar.add(btnNewButton);
		
		JButton btnOpenButton = new JButton("");
		btnOpenButton.setToolTipText("Open existing file");
		btnOpenButton.setFocusPainted(false);
		btnOpenButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-open.png")));
		toolBar.add(btnOpenButton);
		
		JButton btnSaveButton = new JButton("");
		btnSaveButton.setToolTipText("Save file");
		btnSaveButton.setFocusPainted(false);
		btnSaveButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/document-save.png")));
		toolBar.add(btnSaveButton);
		
		JButton btnCloseButton = new JButton("");
		btnCloseButton.setToolTipText("Close file");
		btnCloseButton.setFocusPainted(false);
		btnCloseButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/status/image-missing.png")));
		toolBar.add(btnCloseButton);
		
		JButton btnCutButton = new JButton("");
		btnCutButton.setToolTipText("Cut");
		btnCutButton.setFocusPainted(false);
		btnCutButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-cut.png")));
		toolBar.add(btnCutButton);
		
		JButton btnCopyButton = new JButton("");
		btnCopyButton.setToolTipText("Copy");
		btnCopyButton.setFocusPainted(false);
		btnCopyButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-copy.png")));
		toolBar.add(btnCopyButton);
		
		JButton btnPasteButton = new JButton("");
		btnPasteButton.setToolTipText("Paste");
		btnPasteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-paste.png")));
		toolBar.add(btnPasteButton);
		
		JButton btnDeleteButton = new JButton("");
		btnDeleteButton.setToolTipText("Delete");
		btnDeleteButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-delete.png")));
		toolBar.add(btnDeleteButton);
		
		JButton btnUndoButton = new JButton("");
		btnUndoButton.setToolTipText("Undo");
		btnUndoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-undo.png")));
		toolBar.add(btnUndoButton);
		
		JButton btnRedoButton = new JButton("");
		btnRedoButton.setToolTipText("Redo");
		btnRedoButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/actions/edit-redo.png")));
		toolBar.add(btnRedoButton);
		
		JButton btnPreferencesButton = new JButton("");
		btnPreferencesButton.setToolTipText("Preferences");
		btnPreferencesButton.setIcon(new ImageIcon(MainFrame.class.getResource("/medit/assets/categories/applications-system.png")));
		toolBar.add(btnPreferencesButton);
		
		JToolBar toolBar_1 = new JToolBar();
		toolBar_1.setFloatable(false);
		contentPane.add(toolBar_1, BorderLayout.SOUTH);
		
		JLabel lblReady = new JLabel("Ready | Length: 0 | Filename: \"Unnamed\" | Maximum size: 0KB | INS | LCK | SCR");
		toolBar_1.add(lblReady);
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		JTextPane textPane = new JTextPane();
		textPane.setFont(new Font("Serif", Font.PLAIN, 16));
		scrollPane.setViewportView(textPane);
	}

}
