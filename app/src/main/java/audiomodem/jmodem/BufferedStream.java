package audiomodem.jmodem;

import java.io.EOFException;
import java.io.IOException;

class BufferedStream implements InputSampleStream, OutputSampleStream {

	double[] buffer;
	int offset;

	public BufferedStream(int size) {
		buffer = new double[size];
		reset();
	}

	public BufferedStream(double[] b) {
		buffer = b;
		reset();
	}

	public void reset() {
		offset = 0;
	}

	public void write(double value) throws IOException {
		if (offset >= buffer.length) {
			throw new IOException("no space left");
		}
		buffer[offset] = value;
		offset++;
	}

	public double read() throws IOException {
		if (offset >= buffer.length) {
			throw new EOFException();
		}
		double value = buffer[offset];
		this.offset++;
		return value;
	}

}
