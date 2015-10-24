package io.bogar.ecdtool.bl;

import io.bogar.ecdtool.ui.UI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import org.kc7bfi.jflac.util.ByteData;
import org.kc7bfi.jflac.util.WavWriter;

public class BL {

	private final static String PREF_NODE = "/io/bogar/ecdtool";
	private Preferences prefs = Preferences.userRoot().node(PREF_NODE);

	private final static String PREF_MAP_NODE = PREF_NODE + "/mapping";
	private Preferences prefs_mapping = Preferences.userRoot().node(PREF_MAP_NODE);

	private final static String TRANS_DEL = "";
	private final static String KEY_OFFSET = "offset";
	private final static String KEY_CONCAT = "concatWith";
	private final static String KEY_LAST_SAVE_DIR = "lastSaveDir";
	private final static String KEY_LAST_OPEN_DIR = "lastOpenDir";
	private final static String KEY_FILENAME_LENGTH = "fileNameLength";
	private final static String KEY_EXE_FLAC = "flac";
	private final static String KEY_EXE_METAFLAC = "metaflac";
	private final static String KEY_FILTER_DCP = "dcp";
	private final static String KEY_SAVE_MONOLITHIC = "saveMono";
	private final static String KEY_MAIN_ACTION = "mainaction";

	//private static final Pattern CONSTANT_CHARS = Pattern.compile("[ !#&'()+,\\-.0-9A-Z_a-z]");
	private static final Pattern CONSTANT_CHARS = Pattern.compile("[ '(),\\-.0-9A-Z_a-z]");
	private static final Pattern GENRE_HEAD = Pattern.compile("^GENRE.*$");

	private String lastOpenDir;
	private String lastSaveDir;
	private String concatWith;
	private SortedMap<Character, String> mappingTable = new TreeMap<Character, String>();
	private CueSheet cueSheet;
	private int offsetCorrection;
	private int fileNameLength;
	private String execFLAC;
	private String execMetaFLAC;
	private boolean changed;
	private String flacOptions;
	private Process backgroundProcess;
	private boolean filterDCP;
	private boolean saveMonolithic;
	private MainAction mainAction;

	private UI ui;

	public BL(UI ui) {
		this.ui = ui;
		init();
	}

	public void init() {
		// loading preferences
		lastOpenDir = prefs.get(KEY_LAST_OPEN_DIR, "");
		lastSaveDir = prefs.get(KEY_LAST_SAVE_DIR, "");
		concatWith = prefs.get(KEY_CONCAT, ", ");
		offsetCorrection = prefs.getInt(KEY_OFFSET, 0);
		fileNameLength = prefs.getInt(KEY_FILENAME_LENGTH, 250);
		execFLAC = prefs.get(KEY_EXE_FLAC, null);
		execMetaFLAC = prefs.get(KEY_EXE_METAFLAC, null);
		filterDCP = prefs.getBoolean(KEY_FILTER_DCP, false);
		saveMonolithic= prefs.getBoolean(KEY_SAVE_MONOLITHIC, false);
		mainAction = MainAction.valueOf(prefs.get(KEY_MAIN_ACTION, "RENAME"));
		try {
			for (String key : prefs_mapping.keys()) {
				Character ckey = key.charAt(0);
				mappingTable.put(ckey, prefs_mapping.get(key, ""));
			}
		} catch (BackingStoreException e) {
			ui.logToWindow("BackingStoreException: " + e.getMessage(), true);
		}
	}

	public void shutdown() {
		// saving preferences
		try {
			if (lastOpenDir != null) {
				prefs.put(KEY_LAST_OPEN_DIR, lastOpenDir);
			}
			if (lastSaveDir != null) {
				prefs.put(KEY_LAST_SAVE_DIR, lastSaveDir);
			}
			prefs.put(KEY_CONCAT, concatWith);
			prefs.putInt(KEY_OFFSET, offsetCorrection);
			prefs.putInt(KEY_FILENAME_LENGTH, fileNameLength);
			if (execFLAC!=null) {
				prefs.put(KEY_EXE_FLAC, execFLAC);
			}
			if (execMetaFLAC!=null) {
				prefs.put(KEY_EXE_METAFLAC, execMetaFLAC);
			}
			prefs.putBoolean(KEY_FILTER_DCP, filterDCP);
			prefs.putBoolean(KEY_SAVE_MONOLITHIC, saveMonolithic);
			prefs.put(KEY_MAIN_ACTION, mainAction.toString());
			prefs_mapping.clear();
			for (Character c : mappingTable.keySet()) {
				if (!c.toString().equals(mappingTable.get(c))) {
					prefs_mapping.put(c.toString(), mappingTable.get(c));
				}
			}
		} catch (BackingStoreException e) {
			ui.logToWindow("BackingStoreException: " + e.getMessage(), true);
		}

	}

	public void loadCUE(File cueFile) throws Exception {
		CueSheet cueSheet = new CueSheet(cueFile);
		translateFilenames(cueSheet);
		this.cueSheet = cueSheet;
		deriveChangeStatus();
	}

	public void saveCD() throws Exception {
		int progressValue = 0;
		ui.progressPercent(progressValue);
		if (offsetCorrection>0 && saveMonolithic) {
			throw new Exception("Monolithic not supported with offset>0");
		}
		File cueFile = new File(lastSaveDir + File.separatorChar
				+ cueSheet.getNewCueName() + ".cue");
		ui.logToWindow("Writing cuesheet " + cueFile.getName(), false);
		cueSheet.dump(cueFile, filterDCP, saveMonolithic);

		// create shift data if offsetting
		ByteData shift = null;
		if (offsetCorrection!=0) {
			byte nullval = 0;
			shift = new ByteData(Math.abs(offsetCorrection) * 4);
			for (int i = 0; i < Math.abs(offsetCorrection) * 4; i++) {
				shift.append(nullval);
			}
		}

		File registerTempFLAC = null;
		File registerComment = null;
		File registerNFN = null;

		int i = offsetCorrection > 0 ? cueSheet.getTotalTracks() - 1 : 0;
		int increment = offsetCorrection > 0 ? -1 : 1;
		int iend = offsetCorrection > 0 ? -1 : cueSheet.getTotalTracks();
		backgroundProcess = null;

		File wavFile = null;
		BufferedOutputStream bos = null;
		WavWriter wavWriter = null;

		if (saveMonolithic) {
			wavFile = new File(lastSaveDir + File.separatorChar
					+ cueSheet.getNewCueName() + ".wav");
			ui.logToWindow("Writing " + wavFile.getName(), false);
	        FileOutputStream os = new FileOutputStream(wavFile);
	        bos = new BufferedOutputStream(os);

	        TrackSection xx = cueSheet.getTrack()[cueSheet.getTotalTracks()-1];
	        long totalSamples = 588*(xx.getBegin().getPosition()+xx.getLength().getPosition());
					wavWriter = new WavWriter(bos, new Long(totalSamples), new Integer(2), new Integer(16), new Integer(44100));
	        wavWriter.writeHeader();

	        // handle pregap
	        int pregap = cueSheet.getTrack()[0].getBegin().getPosition();
	        ByteData bd = null;
	        if (pregap>0) {
	        	bd = new ByteData(2352);
	        	for (int idx=0;idx<2352;idx++)
	        		bd.append((byte)0);
	        }
	        while (pregap-- > 0) {
				wavWriter.writePCM(bd);
			}

		}

		while (i != iend) {
			TrackSection fsection = cueSheet.getTrack()[i];
			File flacSource = new File(lastOpenDir + File.separatorChar
					+ fsection.getFileName() + ".flac");
			if (!saveMonolithic) {
				wavFile = new File(lastSaveDir + File.separatorChar
						+ fsection.getNewFileName() + ".wav");
				ui.logToWindow("Writing " + wavFile.getName(), false);
		        FileOutputStream os = new FileOutputStream(wavFile);
		        bos = new BufferedOutputStream(os);
		        wavWriter = new WavWriter(bos);
			}

			// running deflac
			FLACDumper fd = new FLACDumper(flacSource, wavWriter,
					offsetCorrection, shift, saveMonolithic);
			shift = fd.doIt();
			ui.progressPercent(++progressValue);

			if (!saveMonolithic) {
				bos.close();
			}

			File cmtFile = new File(lastSaveDir + File.separatorChar
					+ fsection.getNewFileName() + ".txt");

			NGComment cmt = fsection.getComment();
			if (offsetCorrection != 0) {
				cmt.delComment("replaygain_album_gain");
				cmt.delComment("replaygain_album_peak");
				cmt.delComment("replaygain_track_gain");
				cmt.delComment("replaygain_track_peak");
			}
			cmt.dump(cmtFile);


			if (mainAction.equals(MainAction.RECOMPRESS) && !saveMonolithic) {
				if (backgroundProcess != null) {
					// we have to wait the previous encoder
					encodingPostProcess(registerTempFLAC, registerComment,
							registerNFN);
					ui.progressPercent(++progressValue);
				}
				File flacTemp = File.createTempFile("eCD-", ".flac", wavFile
						.getParentFile());

				// starting encoder
				ui.logToWindow("Starting encoder...", false);
				ProcessBuilder pb = new ProcessBuilder(execFLAC,
						"--totally-silent", "--best", "-V", "-f",
						"--delete-input-file", "-o",
						flacTemp.getAbsolutePath(), wavFile.getAbsolutePath());
				backgroundProcess = pb.start();
				registerTempFLAC = flacTemp;
				registerComment = cmtFile;
				registerNFN = new File(lastSaveDir + File.separatorChar
						+ fsection.getNewFileName() + ".flac");
			}

			i += increment;
		}
		if (saveMonolithic) {
			bos.close();
		}
		if (mainAction.equals(MainAction.RECOMPRESS) && !saveMonolithic) {
			encodingPostProcess(registerTempFLAC, registerComment, registerNFN);
		}

	}

	/**
	 * Saves cue+wav/flac
	 * @throws Exception
	 */
	public void saveCUE() throws Exception {
		cueSheet.commitFileName(fileNameLength);
		if (mainAction.equals(MainAction.RENAME)) {
			File cueBak = new File(lastOpenDir + File.separatorChar + cueSheet.getCueName() + ".bak");
			cueSheet.getCueFile().renameTo(cueBak);

			File cueFile = new File(lastOpenDir + File.separatorChar
					+ cueSheet.getNewCueName() + ".cue");
			ui.logToWindow("Writing cuesheet " + cueFile.getName(), false);
			cueSheet.dump(cueFile, filterDCP, false);
			for (int i=0; i<cueSheet.getTotalTracks(); i++) {
				TrackSection ts = cueSheet.getTrack()[i];
				File flacFileOld = new File(lastOpenDir + File.separatorChar + ts.getFileName() + ".flac");
				File flacFileNew = new File(lastOpenDir + File.separatorChar + ts.getNewFileName() + ".flac");
				if (!flacFileOld.equals(flacFileNew)) {
					ui.logToWindow("Renaming: " + ts.getFileName() + " -> " + ts.getNewFileName(), false);
					flacFileOld.renameTo(flacFileNew);
				}
			}
		} else {
			saveCD();
		}
		ui.logToWindow("Done.", false);
	}

	private void encodingPostProcess(File flac, File comment, File nfn) throws Exception {
		int result = backgroundProcess.waitFor();
		if (result!=0) {
			throw new Exception("FLAC exit code = " + result);
		}
		ui.logToWindow("Encoder finished.", false);
		ProcessBuilder pb = new ProcessBuilder(execMetaFLAC,
				"--no-utf8-convert", "--import-tags-from="
						+ comment.getAbsolutePath(), flac.getAbsolutePath());
		backgroundProcess = pb.start();
		result = backgroundProcess.waitFor();
		if (result!=0) {
			throw new Exception("METAFLAC exit code = " + result);
		}
		comment.delete();
		flac.renameTo(nfn);
	}

	public void closeCUE() {
		cueSheet = null;
		ui.refreshCueTable();
	}

	public void translateFilenames() {
		translateFilenames(cueSheet);
		ui.refreshCueTable();
	}

	private void translateFilenames(CueSheet cueSheet) {

		if (cueSheet==null)
			return;
		// translating cue's name itself
		// get album name from first track
		StringBuilder sb = new StringBuilder();
		NGComment ngc = cueSheet.getTrack()[0].getComment();
		String s = ngc.getCommentPlus("album", concatWith);
		if (s!=null) {
			sb.append(s);
		}
		s = ngc.getCommentPlus("disctotal", concatWith);
		if (s!=null) {
			int disctotal=Integer.parseInt(s,10);
			int discnumber=0;
			if (disctotal != 1) {
				s = ngc.getCommentPlus("discnumber", concatWith);
				if (s!=null) {
					discnumber=Integer.parseInt(s,10);
				}
				sb.append(" - CD");
				String format = "%d";
				if (disctotal>99) {
					format = "%03d";
				} else if (disctotal>9){
					format = "%02d";
				}
				sb.append(String.format(format, discnumber));
			}
		}

		String toTrans = cueSheet.isEdited() ? cueSheet.getNewCueName() : sb.toString();
		cueSheet.setNewCueName(translate(toTrans));

		// calculate proposed filenames
		for (int i = 0; i < cueSheet.getTotalTracks(); i++) {

			if (cueSheet.getTrack()[i].isEdited()) {
				toTrans = cueSheet.getTrack()[i].getNewFileName();
			} else {
				ngc = cueSheet.getTrack()[i].getComment();
				sb = new StringBuilder();

				s = ngc.getCommentPlus("tracknumber", concatWith);
				if (s != null) {
					sb.append(s);
				}

				s = ngc.getCommentPlus("title", concatWith);
				if (s != null) {
					sb.append(" ");
					sb.append(s);
				}

				s = ngc.getCommentPlus("opus", concatWith);
				if (s != null) {
					sb.append(", ");
					sb.append(s);
				}

				s = ngc.getCommentPlus("part", concatWith);
				if (s != null) {
					sb.append(" - ");
					sb.append(s);
				}
				toTrans = sb.toString();
			}

			cueSheet.getTrack()[i].setNewFileName(translate(toTrans));
		}
	}

	protected String translate(String s) {
		char[] chars = s.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<chars.length;i++) {
			Character c = chars[i];
			if (CONSTANT_CHARS.matcher(c.toString()).matches()) {
				sb.append(c);
				continue;
			}
			if (mappingTable.containsKey(chars[i])) {
				String value = mappingTable.get(c);
				if (value.equals(TRANS_DEL)) {
					continue;
				}
				sb.append(value);
			} else {
				// new character, adding to mappingtable
				mappingTable.put(c, c.toString());
				sb.append(c);
				ui.logToWindow("New character found: " + c, false);
			}
		}
		// handle multiple dots and last dot
		String tmps = sb.toString();
		String rets = tmps.replaceAll("[.]{3}", "").replaceAll("[.]+", ".").replaceAll("[.]$", "");
		// drop ? and !
		tmps = rets;
		rets = tmps.replaceAll("[?]+", "").replaceAll("[!]+", "");
		return rets;
	}

	public Character[] getNewCharacters() {
		List<Character> cList = new ArrayList<Character>();
		for (Character key : mappingTable.keySet()) {
			if (key.toString().equals(mappingTable.get(key))) {
				cList.add(key);
			}
		}
		return cList.toArray(new Character[cList.size()]);
	}

	public void validate() {
		if (cueSheet==null) {
			return;
		}
		// check if genre is present
		for (String s : cueSheet.getRemarkHead()) {
			if (GENRE_HEAD.matcher(s).matches()) {
				ui.logToWindow("Genre present.", true);
				break;
			}
		}

	}

	public void deriveChangeStatus() {
		boolean n = cueSheet==null ? false : offsetCorrection!=0 || cueSheet.isModified();
		if (changed != n) {
			changed = n;
			ui.refreshTitle();
		}
	}

	// getters & setters
	public SortedMap<Character, String> getMappingTable() {
		return mappingTable;
	}

	public String getLastOpenDir() {
		return lastOpenDir;
	}

	public void setLastOpenDir(String lastOpenDir) {
		this.lastOpenDir = lastOpenDir;
	}

	public String getLastSaveDir() {
		return lastSaveDir;
	}

	public void setLastSaveDir(String lastSaveDir) {
		this.lastSaveDir = lastSaveDir;
	}

	public CueSheet getCueSheet() {
		return cueSheet;
	}

	public int getOffsetCorrection() {
		return offsetCorrection;
	}

	public void setOffsetCorrection(int offsetCorrection) {
		this.offsetCorrection = offsetCorrection;
	}

	public int getFileNameLength() {
		return fileNameLength;
	}

	public void setFileNameLength(int fileNameLength) {
		this.fileNameLength = fileNameLength;
	}

	public String getExecFLAC() {
		return execFLAC;
	}

	public void setExecFLAC(String execFLAC) {
		this.execFLAC = execFLAC;
	}

	public String getExecMetaFLAC() {
		return execMetaFLAC;
	}

	public void setExecMetaFLAC(String execMetaFLAC) {
		this.execMetaFLAC = execMetaFLAC;
	}

	public boolean isChanged() {
		return changed;
	}

	public String getFlacOptions() {
		return flacOptions;
	}

	public void setFlacOptions(String flacOptions) {
		this.flacOptions = flacOptions;
	}

	public boolean isFilterDCP() {
		return filterDCP;
	}

	public void setFilterDCP(boolean filterDCP) {
		this.filterDCP = filterDCP;
	}

	public boolean isSaveMonolithic() {
		return saveMonolithic;
	}

	public void setSaveMonolithic(boolean saveMonolithic) {
		this.saveMonolithic = saveMonolithic;
	}

	public MainAction getMainAction() {
		return mainAction;
	}

	public void setMainAction(MainAction mainAction) {
		this.mainAction = mainAction;
	}

}
