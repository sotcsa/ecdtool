package io.bogar.ecdtool.bl;

public class CueTrack {

	private String isrc;
	private boolean dcp = false;
	private CDPosition pregap;

	public String getIsrc() {
		return isrc;
	}
	public void setIsrc(String isrc) {
		this.isrc = isrc;
	}
	public boolean isDcp() {
		return dcp;
	}
	public void setDcp(boolean dcp) {
		this.dcp = dcp;
	}
	public CDPosition getPregap() {
		return pregap;
	}
	public void setPregap(CDPosition pregap) {
		this.pregap = pregap;
	}

}
