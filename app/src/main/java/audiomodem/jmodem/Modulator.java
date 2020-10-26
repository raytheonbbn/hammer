package audiomodem.jmodem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

class Modulator {
	final private static double[] cos = Utils.cos(Config.symbolLength);
	final private static double[] sin = Utils.sin(Config.symbolLength);

	private final OutputSampleStream output;

	public Modulator(OutputSampleStream o) {
		output = o;
	}

	void writeSymbols(double real, double imag, int count) throws IOException {
		for (int c = 0; c < count; c++) {
			for (int i = 0; i < Config.symbolLength; i++) {
				output.write(real * cos[i] + imag * sin[i]);
			}
		}
	}

	public void writeByte(byte b) throws IOException {
		for (int i = 0; i < 8; i++) {
			int k = (b >> i) & 1;
			writeSymbols(0f, 1f - (2f * k), 1);
		}
	}

	public void writeSilence(int n) throws IOException {
		writeSymbols(0f, 0f, n);
	}

	public void writePrefix() throws IOException {
		writeSilence(Config.prefixSilence);
		writeSymbols(0f, -1f, Config.prefixSymbols);
		writeSilence(Config.prefixSilence);
	}

	public void writeTraining() throws IOException {
		writeSilence(Config.trainingSilence);
		for (int register = 0x0001, i = 0; i < Config.trainingSymbols; i++) {
			int k = (i < Config.trainingConstant) ? 0 : (register & 3);
			writeSymbols(cos[k], -sin[k], 1);
			register = register << 1;
			if (register >> 16 != 0) {
				register = register ^ 0x1100b;
			}
		}
		writeSilence(Config.trainingSilence);
	}

	public void writeData(byte[] data, int length) throws IOException {

		for (int i = 0; i < length; i++) {
			if (i % Config.frameSize == 0) {
				int size = Math.min(Config.frameSize, length - i);
				writeChecksum(data, i, size);
			}
			writeByte(data[i]);
		}
	}

	public void writeChecksum(byte[] data, int offset, int size)
			throws IOException {
		writeByte((byte) (size + Config.checksumSize));

		CRC32 crc = new CRC32();
		if (data != null) {
			crc.update(data, offset, size);
		}

		ByteBuffer checksum = ByteBuffer.allocate(Config.checksumSize);
		checksum.putInt((int) crc.getValue());
		for (byte b : checksum.array()) {
			writeByte(b);
		}
	}

	public void writeEOF() throws IOException {
		writeChecksum(null, 0, 0);
		writeSilence(Config.postfixSilence);
	}

}
