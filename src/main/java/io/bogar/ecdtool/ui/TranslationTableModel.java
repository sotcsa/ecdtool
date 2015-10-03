package io.bogar.ecdtool.ui;

import java.util.Arrays;
import java.util.SortedMap;

import io.bogar.ecdtool.bl.BL;

import javax.swing.table.AbstractTableModel;

public class TranslationTableModel extends AbstractTableModel {
	
	private BL bl;
	private boolean onlyNew;
	private char[] keys;
	private String[] values;
	private int boundary;
	private final static String[] headers = { "Source", "Target" };

	
	public TranslationTableModel(BL bl, boolean onlyNew) {
		this.bl = bl;
		this.onlyNew = onlyNew;
		SortedMap<Character, String> mappingTable = bl.getMappingTable();
		int keySize = mappingTable.size();
		keys = new char[keySize];
		values = new String[keySize];
		boundary = 0;
		int fromEnd = keySize-1;
		for (Character c : mappingTable.keySet()) {
			String s = mappingTable.get(c);
			if (c.toString().equals(s)) {
				keys[boundary++] = c;
			} else {
				keys[fromEnd--] = c;
			}
		}
		if (boundary>0) {
			Arrays.sort(keys,0,boundary);
		}
		if (fromEnd<keySize-1) {
			Arrays.sort(keys,fromEnd+1,keys.length);
		}
		for (int i=0;i<keys.length;i++) {
			values[i] = mappingTable.get(keys[i]);
		}
	}

	public String getColumnName(int col) {
		return headers[col];
	}
	
	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return onlyNew ? boundary : keys.length;
	}

	public Object getValueAt(int row, int col) {
		// TODO Auto-generated method stub
		switch (col) {
		case 0:	return keys[row];
		case 1:	return values[row];
		default: return null;
		}
	}
	
    public boolean isCellEditable(int row, int col) {
    	return col==1;
    }
	
    public void setValueAt(Object value, int row, int col) {
    	if (col==1) {
    		values[row] = (String) value;
    	}
    }
	
	public void setOnlyNew(boolean onlyNew) {
		this.onlyNew = onlyNew;
		fireTableDataChanged();
	}
	
	public void commit() {
		for (int i=0;i<keys.length;i++) {
			if (!bl.getMappingTable().get(keys[i]).equals(values[i])) {
				bl.getMappingTable().put(keys[i], values[i]);
			}
		}
	}

}
