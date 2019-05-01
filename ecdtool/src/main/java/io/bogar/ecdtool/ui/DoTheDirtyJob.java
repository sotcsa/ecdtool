package io.bogar.ecdtool.ui;

import io.bogar.ecdtool.bl.BL;
import io.bogar.ecdtool.bl.MainAction;

public class DoTheDirtyJob extends Thread {
	
	ECDToolUI ui;
	BL bl;

	public DoTheDirtyJob(ECDToolUI ui, BL bl) {
		this.ui = ui;
		this.bl = bl;
	}

	public void run() {
		ui.setMenuAndToolbarEnabled(false);
		ui.jProgress.setMinimum(0);
		int maxFactor = bl.getMainAction().equals(MainAction.RECOMPRESS) ? 2 : 1;
		ui.jProgress.setMaximum(bl.getCueSheet().getTotalTracks() * maxFactor - 1);
		ui.jProgress.setVisible(true);
		try {
			bl.saveCUE();
			bl.closeCUE();
			ui.refreshTitle();
		} catch (Exception e) {
			ui.errorMessageDialog("Save failed", e.getClass() + ": " + e.getMessage());
		} finally {
			ui.setMenuAndToolbarEnabled(true);
			ui.jProgress.setVisible(false);
		}
	}

}
