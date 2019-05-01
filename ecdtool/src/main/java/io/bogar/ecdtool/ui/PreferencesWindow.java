package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PreferencesWindow extends JDialog implements ActionListener {

	// JFormattedTextField.
	// JSpinner

	private BL bl;

	private JButton okButton;
	private JButton cancelButton;
	private JFormattedTextField textOffset;
	private JFormattedTextField textLength;
	private JTextField textFLAC;
	private JTextField textMetaFLAC;
	private JTextField textOptions;
	private JCheckBox checkBoxFilterDCP;
	private JButton buttonFLAC;
	private JButton buttonMetaFLAC;
	private File currentDirectory = null;
	
	public PreferencesWindow(Frame f, BL bl) {
		super(f, true);
		this.bl = bl;
		init();
	}

	private void init() {
		setTitle("Properties");
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		// offset correction
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel("Offset correction:"), constraints);
		textOffset = new JFormattedTextField(new Integer(bl.getOffsetCorrection()));
		textOffset.setColumns(6);
		textOffset.setToolTipText("Post offset correction of the album");
		textOffset.setHorizontalAlignment(JFormattedTextField.RIGHT);
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.WEST;
		panel.add(textOffset, constraints);
		// filename length
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel("File name's length:"), constraints);
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.WEST;
		textLength = new JFormattedTextField(new Integer(bl.getFileNameLength()));
		textLength.setColumns(6);
		textLength.setToolTipText("Core file size of the files + .flac");
		textLength.setHorizontalAlignment(JFormattedTextField.RIGHT);
		panel.add(textLength, constraints);
		// FLAC
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel("FLAC:"), constraints);
		constraints.gridx = 1;
		constraints.ipadx = 3;
		constraints.anchor = GridBagConstraints.WEST;
		textFLAC = new JTextField(bl.getExecFLAC());
		textFLAC.setColumns(32);
		panel.add(textFLAC, constraints);
		constraints.gridx = 4;
		constraints.anchor = GridBagConstraints.CENTER;
		buttonFLAC = new JButton("Search");
		buttonFLAC.addActionListener(this);
		panel.add(buttonFLAC, constraints);
		// MetaFLAC
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel("MetaFLAC:"), constraints);
		constraints.gridx = 1;
		//constraints.ipadx = 3;
		constraints.anchor = GridBagConstraints.WEST;
		textMetaFLAC = new JTextField(bl.getExecMetaFLAC());
		textMetaFLAC.setColumns(32);
		panel.add(textMetaFLAC, constraints);
		constraints.gridx = 4;
		constraints.anchor = GridBagConstraints.CENTER;
		buttonMetaFLAC = new JButton("Search");
		buttonMetaFLAC.addActionListener(this);
		panel.add(buttonMetaFLAC, constraints);
		// FLAC Options
		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(new JLabel("FLAC Options:"), constraints);
		textOptions = new JTextField(bl.getFlacOptions());
		textOptions.setColumns(32);
		constraints.gridx = 1;
		panel.add(textOptions, constraints);
		// Filter DCP
		constraints.gridx = 1;
		constraints.gridy = 5;
		constraints.anchor = GridBagConstraints.WEST;
		checkBoxFilterDCP = new JCheckBox("Filter DCP", bl.isFilterDCP());
		panel.add(checkBoxFilterDCP, constraints);
		
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
			// offset stuff
			int oldOffset = bl.getOffsetCorrection();
			int newOffset = (Integer) textOffset.getValue();
			if (oldOffset!=newOffset) {
				bl.setOffsetCorrection(newOffset);
				bl.deriveChangeStatus();
			}
			// file name length stuff
			int oldLength = bl.getFileNameLength();
			int newLength = (Integer) textLength.getValue();
			if (oldLength!=newLength) {
				bl.setFileNameLength(newLength);
				bl.translateFilenames();
			}
			// flac options stuff
			bl.setFlacOptions(textOptions.getText());
			bl.setFilterDCP(checkBoxFilterDCP.isSelected());
			dispose();
		} else if (source.equals(cancelButton)) {
			dispose();
		} else if (source.equals(buttonFLAC)) {
			JFileChooser chooser1 = new JFileChooser();
			chooser1.setDialogTitle("Open FLAC executable...");
			if (bl.getExecFLAC()!=null) {
				chooser1.setCurrentDirectory(new File(new File(bl.getExecFLAC()).getPath()));
			} else if (currentDirectory!=null) {
				chooser1.setCurrentDirectory(currentDirectory);
			}
			int result = chooser1.showOpenDialog(this);
			if (result == JFileChooser.CANCEL_OPTION)
				return;
			currentDirectory = chooser1.getCurrentDirectory();
			bl.setExecFLAC(chooser1.getSelectedFile().getAbsolutePath());
			textFLAC.setText(bl.getExecFLAC());
		} else if (source.equals(buttonMetaFLAC)) {
			JFileChooser chooser2 = new JFileChooser();
			chooser2.setDialogTitle("Open MetaFLAC executable...");
			if (bl.getExecMetaFLAC()!=null) {
				chooser2.setCurrentDirectory(new File(new File(bl.getExecMetaFLAC()).getPath()));
			} else if (currentDirectory!=null) {
				chooser2.setCurrentDirectory(currentDirectory);
			}
			int result = chooser2.showOpenDialog(this);
			if (result == JFileChooser.CANCEL_OPTION)
				return;
			currentDirectory = chooser2.getCurrentDirectory();
			bl.setExecMetaFLAC(chooser2.getSelectedFile().getAbsolutePath());
			textMetaFLAC.setText(bl.getExecMetaFLAC());
		}
		
	}

}
