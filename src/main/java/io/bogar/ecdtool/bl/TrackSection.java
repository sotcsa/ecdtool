package io.bogar.ecdtool.bl;

import java.util.ArrayList;
import java.util.List;

public class TrackSection {

	// FLAC file's name
	private String fileName;

	// FLAC file's new name will be
	private String newFileName;

	// true of new filename has been edit
	private boolean edited = false;

	// List of the indexes
	private List<CDIndex> indexes = new ArrayList<CDIndex>();

	// This track begins at this position on the CD
	private CDPosition begin = new CDPosition();

	// Size of the file in 2352 byte sectors
	private CDPosition length = new CDPosition();
	
	// pre-gap
	private CDPosition pregap = new CDPosition();

	// Comments loaded from FLAC file
	private NGComment comment = new NGComment();

	private String isrc;
	
	private boolean dcp = false;

	// default constructor
	public TrackSection() {
	}

	public TrackSection(String fileName) {
		this.fileName=fileName;
	}
	
	public CDIndex[] getIndexesToArray() {
		return indexes.toArray(new CDIndex[indexes.size()]);
	}

	public List<CDIndex> getIndexes() {
		return indexes;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public NGComment getComment() {
		return comment;
	}

	public String getNewFileName() {
		return newFileName;
	}

	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	public CDPosition getBegin() {
		return begin;
	}

	public CDPosition getLength() {
		return length;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}

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
	
}
