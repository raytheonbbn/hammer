package audiomodem.jmodem;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

class Receiver {
	
	private static final Logger log = Logger.getLogger("Receiver");

	public static void run(InputSampleStream src, OutputStream dst) throws IOException {
		log.info("detecting");
		Detector d = new Detector(src);
		d.run();
		double drift = d.frequencyDrift();
		log.info("drift: " + drift * 1e6 + " ppm");
		src = new Sampler(src, 1.0 / (1 + drift));

		log.info("training");
		Equalizer eq = new Equalizer(9, 8);
		Filter filt = eq.run(src);

		log.info("demodulating");
		Demodulator r = new Demodulator(src, filt);
		r.run(dst);

		log.info("done");
	}

	static class InputStreamWrapper implements InputSampleStream {

		DataInputStream input;
		byte[] blob = new byte[Config.fileBufferSize];
		ByteBuffer buf;

		public InputStreamWrapper(InputStream i) {
			input = new DataInputStream(i);
			buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
		}

		public double read() throws IOException {
			if (buf.hasRemaining() == false) {
				buf.clear();
				input.readFully(blob);
			}
			return buf.getShort() / Config.scalingFactor;
		}
	}

	static void main(String[] args) throws IOException {
		Logger root = Logger.getGlobal();
		root.setLevel(Level.ALL);

		InputSampleStream src = new InputStreamWrapper(System.in);
		run(src, System.out);
	}

}
