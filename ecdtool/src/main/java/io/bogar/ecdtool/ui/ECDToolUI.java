package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ECDToolUI extends JFrame implements ActionListener, UI, WindowListener {

	private static final String WINDOW_TITLE = "eCDTool v0.4";
	private static final String FILE_CUE_OPEN = "Open CUE+FLAC";
	private static final String FILE_CUE_SAVE = "Save CUE+FLAC";
	private static final String FILE_PREFERNCES = "Preferences";
	private static final String FILE_QUIT = "Quit";

	private static final String TRANS_EDIT = "Edit";
	private static final String TRANS_CLEAR = "Clear";
	private static final String TRANS_IMPORT = "Import from XML";
	private static final String TRANS_EXPORT = "Export to XML";

	private JMenuBar wholeMenu;
	private JMenu jMenuFile = new JMenu("File");
	private JMenu jMenuTTable = new JMenu("Translation");
	private JMenuItem saveMenuItem;

	private SimpleAttributeSet colorRed;
	private SimpleAttributeSet colorBlack;

	private JTable table;
	private JTextPane textPane;
	private JScrollPane textPaneScroll;

	// toolbar stuff
	private JToolBar jToolBar;
	private JButton openButton;
	private JButton saveButton;
	private JButton editTTButton;
	private JButton preferencesButton;

	protected JProgressBar jProgress;


	private CueTableModel cueTableModel;
	private BL bl;

	public ECDToolUI() {

		// create business logic
		bl = new BL(this);

		// let's try to set Windows... damn! - Linux look & feel... ;-)
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exc) {
			System.err.println("Error loading L&F: " + exc);
		}

		createMenus();
		setContentPane(createContainer());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		refreshTitle();

		pack();
		setLocation(100, 100);
		addWindowListener(this);
		setVisible(true);
	}

	private void createMenus() {
		// create menu
		jMenuFile.setMnemonic(KeyEvent.VK_F);
		jMenuFile.add(makeMenuItem(FILE_CUE_OPEN, KeyEvent.VK_O, KeyStroke.getKeyStroke(
				KeyEvent.VK_O, Event.CTRL_MASK)));
		saveMenuItem =makeMenuItem(FILE_CUE_SAVE, KeyEvent.VK_S, KeyStroke.getKeyStroke(
				KeyEvent.VK_S, Event.CTRL_MASK));
		jMenuFile.add(saveMenuItem);
		jMenuFile.addSeparator();
		jMenuFile.add(makeMenuItem(FILE_PREFERNCES, KeyEvent.VK_P, null));
		jMenuFile.addSeparator();
		jMenuFile.add(makeMenuItem(FILE_QUIT, KeyEvent.VK_Q, KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, Event.CTRL_MASK)));

		jMenuTTable.setMnemonic(KeyEvent.VK_T);
		jMenuTTable.add(makeMenuItem(TRANS_EDIT, KeyEvent.VK_E, null));
		jMenuTTable.add(makeMenuItem(TRANS_CLEAR, KeyEvent.VK_C, null));
		jMenuTTable.addSeparator();
		jMenuTTable.add(makeMenuItem(TRANS_IMPORT, KeyEvent.VK_I, null));
		jMenuTTable.add(makeMenuItem(TRANS_EXPORT, KeyEvent.VK_X, null));


		// create a menu bar
		wholeMenu = new JMenuBar();
		wholeMenu.add(jMenuFile);
		wholeMenu.add(jMenuTTable);
		setJMenuBar(wholeMenu);
	}

	private JMenuItem makeMenuItem(String name, int mnemonic, KeyStroke k) {
		JMenuItem m = new JMenuItem(name);
		m.addActionListener(this);
		m.setMnemonic(mnemonic);
		m.setAccelerator(k);
		return m;
	}


	private Container createContainer() {
		Container panel = new JPanel();
		panel.setLayout(new BorderLayout());
		createToolbar();
		panel.add(jToolBar, BorderLayout.PAGE_START);

		textPane = new JTextPane();
		textPane.setPreferredSize(new Dimension(1024, 100));
		textPane.setFont(new Font("Serif", Font.PLAIN, 12));
		colorRed = new SimpleAttributeSet();
		StyleConstants.setForeground(colorRed, Color.red);
		StyleConstants.setBold(colorRed, true);
		colorBlack = new SimpleAttributeSet();
		StyleConstants.setForeground(colorBlack, Color.black);
		textPaneScroll = new JScrollPane(textPane);
		textPaneScroll.setAutoscrolls(true);
		panel.add(textPaneScroll, BorderLayout.SOUTH);

		cueTableModel = new CueTableModel(bl);
		table = new JTable(cueTableModel);
		//table.setPreferredScrollableViewportSize(new Dimension(1024, 300));
		table.setCellSelectionEnabled(true);
		initColumnSizes(table);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);

		return panel;
	}

	private Container createToolbar() {
		// install toolbar
		jToolBar = new JToolBar("eCDTool");
		openButton = makeToolBarButton("useiconic.com/cloud-download-3x", FILE_CUE_OPEN, "Open CUE+FLAC", "Open");
		jToolBar.add(openButton);
		saveButton = makeToolBarButton("useiconic.com/cloud-upload-3x", FILE_CUE_SAVE, "Save CUE+FLAC", "Save");
		jToolBar.add(saveButton);
		editTTButton = makeToolBarButton("useiconic.com/list-3x", TRANS_EDIT, "Edit translation", "Edit translation");
		jToolBar.add(new JToolBar.Separator());
		preferencesButton = makeToolBarButton("useiconic.com/check-3x", FILE_PREFERNCES, "Preferences", "Preferences");
		jToolBar.add(preferencesButton);
		jToolBar.add(editTTButton);
		jToolBar.add(new JToolBar.Separator());
		jProgress = new JProgressBar();
		jProgress.setVisible(false);
		jToolBar.add(jProgress);
		return jToolBar;
	}


	private JButton makeToolBarButton(String imageName,
			String actionCommand, String toolTipText, String altText) {
		//Look for the image.
		String imgLocation = "/toolbarButtonGraphics/" + imageName + ".png";
		URL imageURL = ECDToolUI.class.getResource(imgLocation);

		//Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);
		button.addActionListener(this);

		if (imageURL != null) { //image found
			button.setIcon(new ImageIcon(imageURL, altText));
		} else { //no image found
			button.setText(altText);
			System.err.println("Resource not found: " + imgLocation);
		}
		return button;
	}

	private void initColumnSizes(JTable table) {
		CueTableModel model = (CueTableModel) table.getModel();
		TableColumn column = null;
		for (int i = 0; i < model.getColumnCount(); i++) {
			column = table.getColumnModel().getColumn(i);
			int x = model.getColumnWidth(i);
			//System.out.println("Initializing width of column " + i + ". "
			//		+ "Width = " + x);
			column.setPreferredWidth(x);
		}
	}

	protected void errorMessageDialog(String title, String text) {
		JOptionPane.showMessageDialog(this, text,
				title, JOptionPane.ERROR_MESSAGE);
	}

	protected void setMenuAndToolbarEnabled(boolean enabled) {
		for (Component c : jToolBar.getComponents()) {
			c.setEnabled(enabled);
		}
		for (int i=0;i<wholeMenu.getMenuCount();i++) {
			JMenu jm = wholeMenu.getMenu(i);
			jm.setEnabled(enabled);
		}
	}

	private void loadCUE() {
		JFileChooser chooser = new JFileChooser();
		if (bl.getLastOpenDir()!=null) {
			chooser.setCurrentDirectory(new File(bl.getLastOpenDir()));
		}
		ExampleFileFilter filter = new ExampleFileFilter();
		filter.addExtension("cue");
		filter.setDescription("CUE Sheet Files");
		chooser.setFileFilter(filter);
		int result = chooser.showOpenDialog(this);

		if (result == JFileChooser.CANCEL_OPTION)
			return;

		bl.setLastOpenDir(chooser.getCurrentDirectory().getAbsolutePath());
		File selectedFile = chooser.getSelectedFile();
		try {
			bl.loadCUE(selectedFile);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(),
					"CUE/FLAC Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		cueTableModel.fireTableDataChanged();
		refreshTitle();
		logClear();
		logToWindow("\""+selectedFile.getAbsolutePath()+"\" loaded successfully.", false);
		bl.validate();
		if (bl.getNewCharacters().length>0) {
			editTTable(true);
		}
		for (int i = 0; i < bl.getCueSheet().getTotalTracks(); i++) {
			if (bl.getCueSheet().getTrack()[i].getNewFileName().length() > bl
					.getFileNameLength()) {
				logToWindow(String.format(
						"Track %2d file name length exceeds %d", i + 1, bl
								.getFileNameLength()), true);
			}
		}
	}

	private void saveCUE() {

		StartWindow sw = new StartWindow(this, bl);
		sw.setVisible(true);
	}

	private void editTTable(boolean onlyNew) {
		EditTranslationTableWindow ttw = new EditTranslationTableWindow(this, bl, onlyNew);
		ttw.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals(FILE_QUIT)) {
			bl.shutdown();
			System.exit(0);
		} else if (command.equals(FILE_CUE_OPEN)) {
			loadCUE();
		} else if (command.equals(FILE_PREFERNCES)) {
			PreferencesWindow pw = new PreferencesWindow(this, bl);
			pw.setVisible(true);
		} else if (command.equals(TRANS_EDIT)) {
			editTTable(false);
		} else if (command.equals(TRANS_IMPORT)) {
			// CUT BEGIN
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("/home/abogar/temp/CD/ISRC"));
			ExampleFileFilter filter = new ExampleFileFilter();
			filter.addExtension("cue");
			filter.setDescription("CUE Sheet Files");
			chooser.setFileFilter(filter);
			int result = chooser.showOpenDialog(this);

			if (result == JFileChooser.CANCEL_OPTION)
				return;

			File selectedFile = chooser.getSelectedFile();

			FileInputStream is;
			InputStreamReader isr;
			BufferedReader in = null;

			try {
				is = new FileInputStream(selectedFile);
				isr = new InputStreamReader(is, "windows-1250");
				in = new BufferedReader(isr);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			final Pattern pCatalog = Pattern.compile("^CATALOG ([0-9]{13})$");
			final Pattern pTrack = Pattern.compile("^TRACK ([0-9]+) AUDIO$");
			final Pattern pIsrc = Pattern.compile("^ISRC ([A-Z0-9]{12,13})$");

			int track = 0;

			try {
				while (in.ready()) {
					String line = in.readLine().trim();
					Matcher mCatalog = pCatalog.matcher(line);
					Matcher mTrack = pTrack.matcher(line);
					Matcher mIsrc = pIsrc.matcher(line);

					if (mCatalog.matches()) {
						bl.getCueSheet().setCatalog(mCatalog.group(1));
					}

					if (mTrack.matches()) {
						track++;
					}

					if (mIsrc.matches()) {
						bl.getCueSheet().getTrack()[track-1].setIsrc(mIsrc.group(1));
					}

				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			cueTableModel.fireTableDataChanged();
			// CUT END
		} else if (command.equals(FILE_CUE_SAVE)) {
			saveCUE();
		}
	}

	public void refreshTitle() {
		String albumTitle = "";
		if (bl.getCueSheet()!=null) {
			albumTitle = " - " + bl.getCueSheet().getTrack()[0].getComment().getCommentPlus("ALBUM", ", ");
		}

		setTitle( (bl.isChanged()?"* ":"") + WINDOW_TITLE + albumTitle);
	}

	public void logClear() {
		Document d = textPane.getDocument();
		try {
			d.remove(0,d.getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void logToWindow(String s, boolean isBold) {
		Document d = textPane.getDocument();
		try {
			d.insertString(d.getLength(), s+"\n", isBold ? colorRed : colorBlack);
			textPane.setCaretPosition(d.getLength());
		} catch (BadLocationException ble) {
		}
	}

	public void refreshCueTable() {
		cueTableModel.fireTableDataChanged();
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		bl.shutdown();
	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void progressPercent(int percent) {
		jProgress.setValue(percent);
	}
}
