package audiomodem.jmodem;

import java.io.IOException;

class Sampler implements InputSampleStream {

	public final int width = 128;
	public final int resolution = 1024;

	private final int N = width * resolution;
	private final double[][] filt;
	private final int coeffs_len;
	private double time;
	private double freq;
	private final double[] buff;
	private int index;
	private final InputSampleStream src;
	private final int last_pos;

	public Sampler(InputSampleStream source, double frequency) {
		src = source;
		double[] h = new double[2 * N];
		for (int i = -N; i < N; i++) {
			double u = i;
			double cosine = Math.cos(0.5 * Math.PI * u / N);
			double window = cosine * cosine;
			h[N + i] = sinc(Math.PI * u / resolution) * window;
		}
		coeffs_len = 2 * width;
		filt = new double[resolution][coeffs_len];
		for (int i = 0; i < resolution; i++) {
			for (int j = 0; j < coeffs_len; j++) {
				filt[i][j] = h[i + (coeffs_len - 1 - j) * resolution];
			}
		}

		buff = new double[coeffs_len]; // zeroes (by default)
		index = width;
		time = width + 1;
		freq = frequency;
		last_pos = buff.length - 1;
	}

	private double sinc(double d) {
		return d != 0 ? Math.sin(d) / d : 1.0;
	}

	public double read() throws IOException {
		int k = (int) time;
		int j = (int) ((time - k) * resolution);
		double[] coeffs = filt[j];
		for (int end = k + width; index < end; index++) {
			System.arraycopy(buff, 1, buff, 0, buff.length - 1);
			buff[last_pos] = src.read(); // push to buff's end
		}
		time += freq;
		double result = 0;
		for (int i = 0; i < buff.length; i++) {
			result += (coeffs[i] * buff[i]);
		}
		return result;
	}

	public void updateTime(double dt) {
		time += dt;
	}

	public void updateFreq(double df) {
		freq += df;
	}

}
