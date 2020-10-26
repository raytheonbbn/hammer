package audiomodem.jmodem;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class Sender {

	public static void run(InputStream src, OutputSampleStream dst) throws IOException {
		Modulator m = new Modulator(dst);
		m.writePrefix();
		m.writeTraining();
		byte[] buf = new byte[Config.fileBufferSize];
		while (true) {
			int read = src.read(buf);
			if (read == -1) {
				break;
			}
			m.writeData(buf, read);
		}
		m.writeEOF();
	}
	
	static class OutputStreamWrapper implements OutputSampleStream {

		DataOutputStream output;
		byte[] blob = new byte[Config.fileBufferSize];
		ByteBuffer buf;

		public OutputStreamWrapper(OutputStream o) {
			output = new DataOutputStream(o);
			buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
		}

		public void write(double value) throws IOException {
			if (buf.remaining() == 0) {
				flush();
			}
			short sample = (short) (Config.scalingFactor * value);
			buf.putShort(sample);
		}

		void flush() throws IOException {
			output.write(blob, 0, buf.position());
			buf.rewind();
		}
	}
	
	public static void orig_main(String[] args) throws IOException {
		OutputStreamWrapper dst = new OutputStreamWrapper(System.out);
		Utils.writeSilence(dst, 1.0);
		run(System.in, dst);
		Utils.writeSilence(dst, 1.0);
		
	}
	
	public static void main(String[] args) throws IOException {
		OutputStreamWrapper dst = new OutputStreamWrapper(new FileOutputStream("/home/nsoule/file.out"));
		Utils.writeSilence(dst, 1.0);
		run(new FileInputStream(args[0]), dst);
		Utils.writeSilence(dst, 1.0);
		
	}
}
