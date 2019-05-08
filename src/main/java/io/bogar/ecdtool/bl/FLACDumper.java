package io.bogar.ecdtool.bl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jflac.FLACDecoder;
import org.jflac.PCMProcessor;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;
import org.jflac.util.WavWriter;

public class FLACDumper  {
	
	private File flacSource;
	private int offset;
	private WavWriter wavWriter;
	private long counterFLACPosition = 0;
	private long length;
	private ByteData shiftIn;
	private ByteData shiftOut;
	boolean monolithic;
	
	private PCMProcessor pcmNegative = new PCMProcessor() {
		
		public void processPCM(ByteData pcm) {
			try {
				if (counterFLACPosition == 0) {
					// first frame
					wavWriter.writePCM(shiftIn);
					wavWriter.writePCM(pcm);
				} else if (counterFLACPosition >= length + offset) {
					for (int i = 0; i < pcm.getLen(); i++) {
						shiftOut.append(pcm.getData(i));
					}
				} else if (counterFLACPosition + (pcm.getLen() / 4) > length
						+ offset) {
					int towav = (int) (length + offset - counterFLACPosition);
					ByteData tempbd = new ByteData(towav * 4);
					int i = 0;
					while (i < towav * 4) {
						tempbd.append(pcm.getData(i));
						i++;
					}
					wavWriter.writePCM(tempbd);
					while (i < pcm.getLen()) {
						shiftOut.append(pcm.getData(i));
						i++;
					}
				} else {
					wavWriter.writePCM(pcm);
				}
				counterFLACPosition += pcm.getLen() / 4;

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void processStreamInfo(StreamInfo info) {
			processGeneralStreamInfo(info);
		}

	};

	private PCMProcessor pcmPositive = new PCMProcessor() {

		public void processPCM(ByteData pcm) {
			try {
				if (counterFLACPosition < offset) {
					int i = 0;
					while (i<(offset-counterFLACPosition)*4 && i<pcm.getLen()) {
						shiftOut.append(pcm.getData(i++));
					}
					int rest = pcm.getLen()-i;
					if (rest>0) {
						ByteData restpcm = new ByteData(rest);
						while (i<pcm.getLen()) {
							restpcm.append(pcm.getData(i++));
						}
						wavWriter.writePCM(restpcm);
					}
				} else {
					wavWriter.writePCM(pcm);
				}
				counterFLACPosition += pcm.getLen() / 4;
				
				// last frame?
				if (counterFLACPosition==length) {
					wavWriter.writePCM(shiftIn);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void processStreamInfo(StreamInfo info) {
			processGeneralStreamInfo(info);
		}

	};

	private PCMProcessor pcmZero = new PCMProcessor() {

		public void processPCM(ByteData pcm) {
			try {
				wavWriter.writePCM(pcm);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void processStreamInfo(StreamInfo info) {
			processGeneralStreamInfo(info);
		}

	};
	
	private void processGeneralStreamInfo(StreamInfo info) {
		length = info.getTotalSamples();
		if (!monolithic) {
			try {
				wavWriter.writeHeader(info);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public FLACDumper(File flacSource, WavWriter wavWriter, int offset,
			ByteData shiftIn, boolean monolithic) {
		this.flacSource = flacSource;
		this.wavWriter = wavWriter;
		this.offset = offset;
		this.shiftIn = shiftIn;
		this.monolithic = monolithic;
		this.shiftOut = new ByteData(Math.abs(offset) * 4);
	}
	
	public ByteData doIt() throws Exception {
        FileInputStream is = new FileInputStream(flacSource);
        FLACDecoder decoder = new FLACDecoder(is);
        if (offset<0) {
        	decoder.addPCMProcessor(pcmNegative);
        } else if (offset>0) {
        	decoder.addPCMProcessor(pcmPositive);
        } else {
        	// offset == 0
        	decoder.addPCMProcessor(pcmZero);
        }
        decoder.decode();
        is.close();
		return shiftOut;
	}
	
}
