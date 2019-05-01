package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;


public class EditTranslationTableWindow extends JDialog implements ActionListener, ItemListener {
	
	private TranslationTableModel model;
	private JButton okButton;
	private JButton cancelButton;
	private JCheckBox onCheckBox;
	private BL bl;


	public EditTranslationTableWindow(Frame f, BL bl, boolean onlyNew) {
		super(f, true);
		this.bl = bl;
		setTitle("Translation");
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		model = new TranslationTableModel(bl, onlyNew);
		JTable table = new JTable(model);
		table.setPreferredScrollableViewportSize(new Dimension(400,300));
		table.setCellSelectionEnabled(true);
		c.add(new JScrollPane(table), BorderLayout.NORTH);
		
		onCheckBox = new JCheckBox("Only new");
		onCheckBox.setMnemonic(KeyEvent.VK_N);
		onCheckBox.setSelected(onlyNew);
		onCheckBox.addItemListener(this);
		c.add(onCheckBox, BorderLayout.EAST);

		JPanel bottomPanel = new JPanel();
		okButton = new JButton("OK");
		bottomPanel.add(okButton);
		cancelButton = new JButton("Cancel");
		bottomPanel.add(cancelButton);
		c.add(bottomPanel, BorderLayout.SOUTH);
		
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		setLocation(300, 300);
		pack();
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source.equals(okButton)) {
			model.commit();
			bl.translateFilenames();
			bl.deriveChangeStatus();
			dispose();
		} else if (source.equals(cancelButton)) {
			dispose();
		} 
	}
	
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source.equals(onCheckBox)) {
			boolean onlyNew = e.getStateChange() == ItemEvent.SELECTED;
			model.setOnlyNew(onlyNew);
		}
	}

}
