package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;
import io.bogar.ecdtool.bl.MainAction;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class StartWindow extends JDialog implements ActionListener {

	private BL bl;
	private ECDToolUI ui;
	
	private static final String S_SEPARATE = "Separate files per track";
	private static final String S_MONOLITHIC = "One file per album";
	private static final String S_RENAME = "Just rename files";
	private static final String S_RECOMPRESS = "[Re]encode to FLAC";
	private static final String S_UNCOMPRESS = "Save to WAVE";
	
	private JButton okButton;
	private JButton cancelButton;
	private JButton buttonOutDir;
	private JTextField textOutDir;
	
	JRadioButton jrSeparate;
	JRadioButton jrMonolithic;
	JRadioButton jrRename;
	JRadioButton jrReCompress;
	JRadioButton jrUnCompress;

	private File currentDirectory = null;

	public StartWindow(ECDToolUI ui, BL bl) {
		super(ui, true);
		this.bl = bl;
		this.ui = ui;
		currentDirectory = new File(bl.getLastSaveDir());
		init();
	}

	private void init() {
		setTitle("Save with offset " + bl.getOffsetCorrection());
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		// offset correction
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;
		
		JPanel panelMS = new JPanel();
		panelMS.setLayout(new GridLayout(2,1));
		TitledBorder titledbms = BorderFactory.createTitledBorder("Mode");
		panelMS.setBorder(titledbms);
		
		boolean mono = bl.isSaveMonolithic() && (bl.getOffsetCorrection()<=0);
		
		jrSeparate = new JRadioButton(S_SEPARATE);
		jrSeparate.setActionCommand(S_SEPARATE);
		jrSeparate.setSelected(!mono);
		jrSeparate.addActionListener(this);
		jrSeparate.setEnabled(bl.getOffsetCorrection()<=0);
		jrMonolithic = new JRadioButton(S_MONOLITHIC);
		jrMonolithic.setActionCommand(S_MONOLITHIC);
		jrMonolithic.setSelected(mono);
		jrMonolithic.addActionListener(this);
		jrMonolithic.setEnabled(bl.getOffsetCorrection()<=0);
		ButtonGroup monosep = new ButtonGroup();
		monosep.add(jrSeparate);
		monosep.add(jrMonolithic);
		
		panelMS.add(jrSeparate);
		panelMS.add(jrMonolithic);
		
		panel.add(panelMS, constraints);

		JPanel panelAction = new JPanel();
		panelAction.setLayout(new GridLayout(3,1));
		TitledBorder titledbAction = BorderFactory.createTitledBorder("Action");
		panelAction.setBorder(titledbAction);
		
		jrRename=new JRadioButton(S_RENAME);
		jrRename.setEnabled(bl.getOffsetCorrection()==0);
		jrRename.setSelected(jrRename.isEnabled() && bl.getMainAction().equals(MainAction.RENAME));
		jrRename.addActionListener(this);
		panelAction.add(jrRename);
		
		if (jrRename.isSelected()) {
			jrSeparate.setEnabled(false);
			jrMonolithic.setEnabled(false);
		}
		
		jrReCompress = new JRadioButton(S_RECOMPRESS);
		jrReCompress.setSelected(!jrRename.isSelected() && bl.getMainAction().equals(MainAction.RECOMPRESS));
		jrReCompress.addActionListener(this);
		panelAction.add(jrReCompress);
		
		jrUnCompress = new JRadioButton(S_UNCOMPRESS);
		jrUnCompress.setSelected(!jrRename.isSelected() && bl.getMainAction().equals(MainAction.UNCOMPRESS));
		jrUnCompress.addActionListener(this);
		panelAction.add(jrUnCompress);
		
		constraints.gridx=1;
		panel.add(panelAction, constraints);
		constraints.gridx=0;
		constraints.gridy++;
		
		ButtonGroup what = new ButtonGroup();
		what.add(jrRename);
		what.add(jrReCompress);
		what.add(jrUnCompress);
		
		JPanel panelOutDir = new JPanel();
		TitledBorder titledbDir = BorderFactory.createTitledBorder("Output Directory");
		panelOutDir.setBorder(titledbDir);
		buttonOutDir = new JButton("Set");
		buttonOutDir.addActionListener(this);
		buttonOutDir.setEnabled(!bl.getMainAction().equals(MainAction.RENAME));
		panelOutDir.add(buttonOutDir);
		String outDir = bl.getMainAction().equals(MainAction.RENAME) ? bl.getLastOpenDir() : bl.getLastSaveDir();  
		textOutDir = new JTextField(outDir);
		textOutDir.setColumns(64);
		textOutDir.setEditable(false);
		panelOutDir.add(textOutDir);
		
		constraints.gridwidth=2;
		panel.add(panelOutDir, constraints);
		c.add(panel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		okButton = new JButton("OK");
		bottomPanel.add(okButton);
		cancelButton = new JButton("Cancel");
		bottomPanel.add(cancelButton);

		c.add(bottomPanel, BorderLayout.SOUTH);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		pack();
		setLocation(300,300);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source.equals(okButton)) {
			bl.setSaveMonolithic(jrMonolithic.isSelected());
			if (jrRename.isSelected()) {
				bl.setMainAction(MainAction.RENAME);
			} else if (jrReCompress.isSelected()) {
				bl.setMainAction(MainAction.RECOMPRESS);
			} else if (jrUnCompress.isSelected()) {
				bl.setMainAction(MainAction.UNCOMPRESS);
			}
			bl.setLastSaveDir(currentDirectory.getAbsolutePath());
			Thread t = new DoTheDirtyJob(ui, bl);
			t.start();
			dispose();			
		} else if (source.equals(cancelButton)) {
			dispose();
		} else if (source.equals(buttonOutDir)) {
			JFileChooser chooser = new JFileChooser();
			if (bl.getLastSaveDir() != null) {
				chooser.setCurrentDirectory(new File(bl.getLastSaveDir()));
			}
			//chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			chooser.setDialogTitle("Please select the output directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = chooser.showSaveDialog(this);

			if (result == JFileChooser.CANCEL_OPTION)
				return;
			
			currentDirectory = chooser.getSelectedFile();
			textOutDir.setText(currentDirectory.getAbsolutePath());
		} else if (source.equals(jrRename)) {
			buttonOutDir.setEnabled(false);
			textOutDir.setText(bl.getLastOpenDir());
			jrSeparate.setEnabled(false);
			jrMonolithic.setEnabled(false);
		} else if (source.equals(jrReCompress) || source.equals(jrUnCompress)) {
			buttonOutDir.setEnabled(true);
			textOutDir.setText(currentDirectory.getAbsolutePath());
			jrSeparate.setEnabled(true);
			jrMonolithic.setEnabled(true);
		}
		
	}

}
