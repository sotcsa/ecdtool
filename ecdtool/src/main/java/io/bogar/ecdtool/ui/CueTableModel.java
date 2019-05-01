package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;
import io.bogar.ecdtool.bl.CueSheet;

import javax.swing.table.AbstractTableModel;

public class CueTableModel extends AbstractTableModel {
	
	private final static String[] headers = { "#", "Source", "Target", "Start",
			"Length", "Pre-Gap", "Start sector", "End Sector", "DCP", "ISRC" };
	private final static int[] columnWidth = { 4, 80, 400, 20, 
			20, 20, 12, 12, 4, 60 };
	
	private BL bl;

	public CueTableModel(BL bl) {
		this.bl = bl;
	}
	
	public String getColumnName(int col) {
		return headers[col];
	}

	public int getRowCount() {
		return bl.getCueSheet() == null ? 0 : bl.getCueSheet().getTotalTracks()+1; 
	}
	
	public int getColumnCount() {
		return 10; 
	}
	
	public Object getValueAt( int row, int col) {
		
		Object ret = "";

		switch (col) {
		case 0:
			ret = row == 0 ? "**" : String.format("%02d", row);
			break;
		case 1:
			ret = row == 0 ? bl.getCueSheet().getCueName() : bl.getCueSheet().getTrack()[row - 1]
					.getFileName();
			break;
		case 2:
			ret = row == 0 ? bl.getCueSheet().getNewCueName() : bl.getCueSheet().getTrack()[row - 1]
					.getNewFileName();
			int max = bl.getFileNameLength();
			if (((String)ret).length() > max) {
				ret = ((String)ret).substring(0, max).concat("|").concat(((String)ret).substring(max, ((String)ret).length()));
			}
			//ret = ((String)ret).substring(0, max > ((String)ret).length() ? ((String)ret).length() : max);
			break;
		case 3:
			if (row > 0) {
				ret = bl.getCueSheet().getTrack()[row - 1].getBegin().toString();
			}
			break;
		case 4:
			if (row > 0) {
				ret = bl.getCueSheet().getTrack()[row - 1].getLength().toString();
			}
			break;
		case 5:
			if (row > 0) {
				ret = bl.getCueSheet().getTrack()[row - 1].getPregap().toString();
			}
			break;
		case 6:
			if (row > 0) {
				int begin = bl.getCueSheet().getTrack()[row - 1].getBegin().getPosition();
				ret = new Integer(begin);
			}
			break;
		case 7:
			if (row > 0) {
				int end = bl.getCueSheet().getTrack()[row - 1].getBegin().getPosition()
						+ bl.getCueSheet().getTrack()[row - 1].getLength().getPosition()-1;
				ret = new Integer(end);
			}
			break;
		case 8:
			ret = (row==0) ? Boolean.FALSE : new Boolean(bl.getCueSheet().getTrack()[row-1].isDcp());   
			break;
		case 9:
			ret = (row==0) ? bl.getCueSheet().getCatalog() : bl.getCueSheet().getTrack()[row-1].getIsrc();
		}

		return ret;
	}
	
    public boolean isCellEditable(int row, int col) {
    	return col==2;
    }
    
    public void setValueAt(Object value, int row, int col) {
    	if (col == 2) {
    		String s = (String)value;
			int max = bl.getFileNameLength();
			s = s.replaceAll("\\|", "");
			//s = s.substring(0, max > s.length() ? s.length() : max);
			if (row == 0) {
				bl.getCueSheet().setNewCueName(s);
				bl.getCueSheet().setEdited(true);
			} else {
				bl.getCueSheet().getTrack()[row - 1].setNewFileName(s);
				bl.getCueSheet().getTrack()[row - 1].setEdited(true);
			}
	        fireTableCellUpdated(row, col);
	        bl.deriveChangeStatus();
		}
    }

    
	public Class<?> getColumnClass(int columnIndex) {
		switch(columnIndex) {
		case 6: 
		case 7: return Integer.class;
		case 8: return Boolean.class;
		default: return String.class;
		}
	}
	
	public int getColumnWidth(int i) {
		return columnWidth[i];
	}
	
}
