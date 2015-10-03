package io.bogar.ecdtool.bl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.kc7bfi.jflac.metadata.VorbisString;

public class CueSheet {

	private List<String> remarkHead;
	private File cueFile;
	private String cueName;
	private String newCueName;
	private TrackSection[] track;
	private boolean edited = false;
	private String catalog;
	private int totalTracks;


	private final static Pattern pRemark = Pattern.compile("^REM (.*)$");
	private final static Pattern pFile = Pattern.compile("^FILE \"([^\"]+).wav\" WAVE$");
	private final static Pattern pTrack = Pattern.compile("^TRACK ([0-9]+) AUDIO$");
	private final static Pattern pPregap = Pattern.compile("^PREGAP ([0-9]+):([0-9]+):([0-9]+)$");
	private final static Pattern pIndex = Pattern.compile("^INDEX ([0-9]+) ([0-9]+):([0-9]+):([0-9]+)$");
	private final static Pattern pPerformer = Pattern.compile("^PERFORMER \"(.*)\"$");
	private final static Pattern pTitle = Pattern.compile("^TITLE \"(.*)\"$");
	private final static Pattern pCatalog = Pattern.compile("^CATALOG ([0-9]{13})$");
	private final static Pattern pIsrc = Pattern.compile("^ISRC ([A-Z0-9]{12,13})$");
	private final static Pattern pDcp = Pattern.compile("^FLAGS DCP$");

	private final static Pattern pFLACComment = Pattern.compile("^([^=]+)=(.*)$");

	/**
	 * Constructor loads from file
	 * @param f
	 */
	public CueSheet(File f) throws Exception {

		cueFile = f;
		remarkHead = new ArrayList<String>();
		track = new TrackSection[99];
		int firstTracksPreGap = 0;


		// removing trailing .cue
		cueName = f.getName();
		cueName = cueName.substring(0, cueName.length()-4);

		FileInputStream is;
		InputStreamReader isr;
		BufferedReader in;

		try {
			is = new FileInputStream(f);
			isr = new InputStreamReader(is, "windows-1250");
			in = new BufferedReader(isr);
		} catch (FileNotFoundException e1) {
			throw new Exception("File not found: " + f);
		} catch (UnsupportedEncodingException e1) {
			throw new Exception("CP1250 not supported");
		}

		int loadedFiles = 0;
		int lineno = 0;

		int currentTrack = 0;
		int lastIndex = -1;

		try {
			while (in.ready()) {

				if (track[currentTrack] == null)
					track[currentTrack] = new TrackSection();

				String line = in.readLine().trim();
				lineno++;

				Matcher matcherRemark = pRemark.matcher(line);
				Matcher matcherFile = pFile.matcher(line);
				Matcher matcherTrack = pTrack.matcher(line);
				Matcher matcherPreGap = pPregap.matcher(line);
				Matcher matcherIndex = pIndex.matcher(line);
				Matcher matcherPerformer = pPerformer.matcher(line);
				Matcher matcherTitle = pTitle.matcher(line);
				Matcher matcherCatalog = pCatalog.matcher(line);
				Matcher matcherIsrc = pIsrc.matcher(line);
				Matcher matcherDcp = pDcp.matcher(line);

				if (matcherRemark.matches() && loadedFiles == 0) {
					remarkHead.add(matcherRemark.group(1));
				} else if (matcherFile.matches()) {
					if (track[loadedFiles]!=null) {
						track[loadedFiles].setFileName(matcherFile.group(1));
					} else {
						track[loadedFiles] = new TrackSection(matcherFile.group(1));
					}
					loadedFiles++;
					// if ((currentTrack != 0 || lastIndex == -1)
					// && !intdata.isEmpty()) {
					// error(lineno, "file was without TRACK/INDEX");
					// }
				} else if (matcherTrack.matches()) {
					int trackno = Integer.parseInt(matcherTrack.group(1), 10);

					if (trackno != ++currentTrack) {
						error(lineno, String.format("TRACK %d expected instead of %d", currentTrack, trackno));
					}
					if (trackno > 1 && lastIndex < 1) {
						error(lineno, "INDEX 01 is missing");
					}
					lastIndex = -1;
				} else if (matcherPreGap.matches()) {
					//if (nofiles == 0 || currentTrack == 0) {
					if (currentTrack!=1) {
 						error(lineno, "PREGAP at wrong position");
					}
					firstTracksPreGap = CDPosition.parse(
											matcherPreGap.group(1),
											matcherPreGap.group(2),
											matcherPreGap.group(3));
				} else if (matcherIndex.matches()) {
					if (currentTrack == 0 || loadedFiles == 0) {
						error(lineno, "INDEX at wrong position");
					}

					int index = Integer.parseInt(matcherIndex.group(1), 10);

					if (lastIndex > 0 && (index != lastIndex + 1)) {
						error(lineno, "wrong INDEX no");
					}

					// recording index position
					track[loadedFiles-1].getIndexes().add(new CDIndex(currentTrack, index,
														matcherIndex.group(2),
														matcherIndex.group(3),
														matcherIndex.group(4)));

					lastIndex = index;
				} else if (matcherPerformer.matches()) {
					// ignoring it silently
				} else if (matcherTitle.matches()) {
					// ignoring it silently
				} else if (matcherCatalog.matches()) {
					// CATALOG number
					if (currentTrack==0) {
						catalog = matcherCatalog.group(1);
					} else {
						error(lineno, "CATALOG at wrong position");
					}
				} else if (matcherIsrc.matches()) {
					if (currentTrack==0) {
						error(lineno, "ISRC at wrong position");
					} else {
						track[currentTrack-1].setIsrc(matcherIsrc.group(1));
					}
				} else if (matcherDcp.matches()) {
					track[currentTrack-1].setDcp(true);
				} else {
					error(lineno, "unexpected: " + line);
				}
			}
		} catch (IOException e) {
			throw new Exception("I/O Exception");
		}
		in.close();
		totalTracks = loadedFiles;

		if (currentTrack == 0) {
			error(lineno, "TRACK 01 is missing");
		}

		if (lastIndex < 1) {
			error(lineno, "last file doesn't contain INDEX 01");
		}

		// LOADING FLAC FILES!
		String dir = f.getParent();

		for (int i = 0; i < totalTracks; i++) {
			String flacFileName = dir + File.separatorChar
					+ track[i].getFileName() + ".flac";
			FileInputStream inputStream = new FileInputStream(new File(flacFileName));
			FLACDecoder dec = new FLACDecoder(inputStream);
			Metadata[] md = dec.readMetadata();
			VorbisComment vc = null;
			StreamInfo si = null;
			for (Metadata m : md) {
				if (m.getClass().equals(StreamInfo.class)) {
					si = (StreamInfo) m;
				} else if (m.getClass().equals(VorbisComment.class)) {
					vc = (VorbisComment) m;
				}
				if (si != null && vc != null) {
					break;
				}
			}

			if (si!=null) {
				long length = si.getTotalSamples() / 588;
				//System.out.println("Tracklength "+i+" = "+length);
				track[i].getLength().setPosition((int)length);
			}

			// at long last comment found!
			for (int iter=0; iter< vc.getNumComments(); iter++) {
				VorbisString vs = vc.getComment(iter);
			  Matcher matcher = pFLACComment.matcher(vs.toString());
				if (!matcher.matches()) {
					System.out.println(vc);
					throw new Exception(flacFileName + ": bad comment: " + vs);
				}
				String name = matcher.group(1);
				String value = matcher.group(2);
				// tracknumber old EAC hotfix
				if (name.toLowerCase().equals("tracknumber")) {
					if (value.length() == 1) {
						value = "0" + value;
					}
				}
				track[i].getComment().addComment(name, value);
			}
			inputStream.close();
		}

		// succesfully loaded, therefore committing changes
		// setting first track's pregap...
		track[0].getBegin().setPosition(firstTracksPreGap);
		// ... and the following track's
		for (int i = 1; i < loadedFiles; i++) {
			int pos = track[i - 1].getBegin().getPosition()
					+ track[i - 1].getLength().getPosition();
			track[i].getBegin().setPosition(pos);
		}

		// calculating the pre-gaps...
		track[0].getPregap().setPosition(firstTracksPreGap);
		for (int i = 1; i < loadedFiles; i++) {
			CDIndex[] indexes = track[i - 1].getIndexesToArray();
			CDIndex lastidx = indexes[indexes.length - 1];
			if (lastidx.getTrack() == i + 1 && lastidx.getNo() == 0) {
				track[i].getPregap().setPosition(
						track[i - 1].getLength().getPosition()
								- lastidx.getPosition().getPosition());
			}
		}

	}

	private void error(int lineno, String msg) throws Exception {
		String error = String.format("Error on line %d: %s", lineno, msg);
		throw new Exception(error);

	}

	public boolean isModified() {
		if (!cueName.equals(newCueName))
			return true;
		for (int i=0; i<totalTracks; i++) {
			if (!track[i].getFileName().equals(track[i].getNewFileName()))
				return true;
		}
		return false;
	}

	public void dump(File fileCUE, boolean filterDCP, boolean monolithic) throws Exception {
		if (fileCUE.exists()) {
			fileCUE.delete();
		}
		if (!fileCUE.createNewFile()) {
			throw new Exception("Can't create file");
		}
		FileOutputStream os = new FileOutputStream(fileCUE);
		OutputStreamWriter osw = new OutputStreamWriter(os, "windows-1250");
		BufferedWriter bw = new BufferedWriter(osw);

		for (String s : remarkHead) {
			bw.write("REM "+s+"\r\n");
		}

		if (catalog!=null) {
			bw.write("CATALOG "+catalog+"\r\n");
		}

		if (monolithic) {
			bw.write("FILE \"" + newCueName +".wav\" WAVE\r\n");
		}

		boolean wasFile = false;
		for (int i = 0; i < totalTracks; i++) {
			if (!monolithic) {
				bw.write("FILE \"" + track[i].getNewFileName() + ".wav\" WAVE\r\n");
			}
			if (!wasFile) {
				bw.write(String.format("  TRACK %02d AUDIO\r\n", i + 1));
				if (track[i].isDcp() && !filterDCP) {
					bw.write("    FLAGS DCP\r\n");
				}
				if (track[i].getIsrc()!=null) {
					bw.write("    ISRC "+track[i].getIsrc()+"\r\n");
				}
			}
			wasFile = false;
			CDPosition pregap;
			if (i == 0 && (pregap = track[0].getBegin()).getPosition() != 0) {
				if (!monolithic) {
					bw.write("    PREGAP " + pregap.toIndexString() + "\r\n");
				} else {
					CDPosition zeropos = new CDPosition(0);
					bw.write(String.format("    INDEX %02d %s\r\n", 0, zeropos.toIndexString()));
				}
			}
			for (CDIndex index : track[i].getIndexes()) {
				if (index.getTrack() != i + 1) {
					wasFile = true;
					bw.write(String.format("  TRACK %02d AUDIO\r\n", index
							.getTrack()));
					if (track[i+1].isDcp() && !filterDCP) {
						bw.write("    FLAGS DCP\r\n");
					}
					if (track[i+1].getIsrc()!=null) {
						bw.write("    ISRC "+track[i+1].getIsrc()+"\r\n");
					}
				}
				CDPosition ipos = index.getPosition();
				if (monolithic) {
					ipos.setPosition(ipos.getPosition()+track[i].getBegin().getPosition());
				}
				bw.write(String.format("    INDEX %02d %s\r\n", index.getNo(),
						ipos.toIndexString()));
			}
		}
		bw.close();

	}

	public void commitFileName(int max) {
		for (int i=0;i<totalTracks;i++) {
			String nfn = track[i].getNewFileName();
			nfn = nfn.substring(0, max > nfn.length() ? nfn.length() : max);
			track[i].setNewFileName(nfn);
		}
	}

	public TrackSection[] getTrack() {
		return track;
	}

	public String getNewCueName() {
		return newCueName;
	}

	public void setNewCueName(String newCueName) {
		this.newCueName = newCueName;
	}

	public List<String> getRemarkHead() {
		return remarkHead;
	}

	public String getCueName() {
		return cueName;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}

	public String getCatalog() {
		return catalog;
	}

	public int getTotalTracks() {
		return totalTracks;
	}

	public File getCueFile() {
		return cueFile;
	}

	// CUT
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

}
