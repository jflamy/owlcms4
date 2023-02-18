/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

/*
 * Only change to this file is the package name.  We need to specify the
 * WaveFileReader explicitly because we are reading files from inside a jar,
 * and the generic javax.sound class resets when trying to determine the format.
 * Since we know the format, we pass the proper reader.
 * Except that the proper reader is in a hidden package.  Since the source
 * is open, we make a copy, to avoid the ugliness of Java 11 --add-imports.
 */
package app.owlcms.sound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

/**
 * WAVE file format class.
 *
 * @author Jan Borgersen
 */

final class WaveFileFormat extends AudioFileFormat {

	static final int DATA_MAGIC = 0x64617461; // "data"

	static final int FMT_MAGIC = 0x666d7420; // "fmt "

	// magic numbers
	static final int RIFF_MAGIC = 1380533830;

	static final int WAVE_FORMAT_ADPCM = 0x0002;
	static final int WAVE_FORMAT_ALAW = 0x0006;
	static final int WAVE_FORMAT_DIGIFIX = 0x0016;
	static final int WAVE_FORMAT_DIGISTD = 0x0015;

	static final int WAVE_FORMAT_DVI_ADPCM = 0x0011;
	static final int WAVE_FORMAT_MULAW = 0x0007;
	static final int WAVE_FORMAT_OKI_ADPCM = 0x0010;
	static final int WAVE_FORMAT_PCM = 0x0001;
	static final int WAVE_FORMAT_SX7383 = 0x1C07;
	// encodings
	static final int WAVE_FORMAT_UNKNOWN = 0x0000;
	static final int WAVE_IBM_FORMAT_ADPCM = 0x0103;
	static final int WAVE_IBM_FORMAT_ALAW = 0x0102;
	static final int WAVE_IBM_FORMAT_MULAW = 0x0101;
	static final int WAVE_MAGIC = 1463899717;
	// $$fb 2002-04-16: Fix for 4636355: RIFF audio headers could be _more_ spec
	// compliant
	/**
	 * fmt_ chunk size in bytes
	 */
	private static final int STANDARD_FMT_CHUNK_SIZE = 16;
	// $$fb 2001-07-13: added management of header size in this class
	// $$fb 2002-04-16: Fix for 4636355: RIFF audio headers could be _more_ spec
	// compliant
	private static final int STANDARD_HEADER_SIZE = 28;

	static int getFmtChunkSize(int waveType) {
		// $$fb 2002-04-16: Fix for 4636355: RIFF audio headers could be _more_ spec
		// compliant
		// add 2 bytes for "codec specific data length" for non-PCM codecs
		int result = STANDARD_FMT_CHUNK_SIZE;
		if (waveType != WAVE_FORMAT_PCM) {
			result += 2; // WORD for "codec specific data length"
		}
		return result;
	}

	static int getHeaderSize(int waveType) {
		// $$fb 2002-04-16: Fix for 4636355: RIFF audio headers could be _more_ spec
		// compliant
		// use dynamic format chunk size
		return STANDARD_HEADER_SIZE + getFmtChunkSize(waveType);
	}

	/**
	 * Wave format type.
	 */
	private final int waveType;

	WaveFileFormat(AudioFileFormat aff) {

		this(aff.getType(), aff.getByteLength(), aff.getFormat(), aff.getFrameLength());
	}

	WaveFileFormat(AudioFileFormat.Type type, int lengthInBytes, AudioFormat format, int lengthInFrames) {

		super(type, lengthInBytes, format, lengthInFrames);

		AudioFormat.Encoding encoding = format.getEncoding();

		if (encoding.equals(AudioFormat.Encoding.ALAW)) {
			waveType = WAVE_FORMAT_ALAW;
		} else if (encoding.equals(AudioFormat.Encoding.ULAW)) {
			waveType = WAVE_FORMAT_MULAW;
		} else if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED) ||
		        encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
			waveType = WAVE_FORMAT_PCM;
		} else {
			waveType = WAVE_FORMAT_UNKNOWN;
		}
	}

	int getHeaderSize() {
		return getHeaderSize(getWaveType());
	}

	int getWaveType() {

		return waveType;
	}
}
