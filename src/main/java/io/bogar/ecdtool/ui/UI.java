package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.CueSheet;

public interface UI {

	public void logToWindow(String s, boolean isBold);

	public void logClear();
	
	public void refreshCueTable();
	
	public void refreshTitle();
	
	public void progressPercent(int percent);
	
}
