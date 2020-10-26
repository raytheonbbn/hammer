package audiomodem.jmodem;

import java.io.IOException;

class Utils {

	public static double[] cos(int n) {
		double[] res = zeros(n);
		for (int i = 0; i < n; i++) {
			res[i] = Math.cos(0.5 * Math.PI * i);
		}
		return res;
	}
	public static double[] sin(int n) {
		double[] res = zeros(n);
		for (int i = 0; i < n; i++) {
			res[i] = Math.sin(0.5 * Math.PI * i);
		}
		return res;
	}
	public static double[] zeros(int n) {
		return new double[n];
	}
	public static int argmax(double[] xs) {
		int res = 0;
		for (int i = 1; i < xs.length; i++) {
			if (xs[i] > xs[res]) {
				res = i;
			}
		}
		return res;
	}
	public static int argmin(double[] xs) {
		int res = 0;
		for (int i = 1; i < xs.length; i++) {
			if (xs[i] < xs[res]) {
				res = i;
			}
		}
		return res;
	}
	public static double mean(double[] x) {
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum += x[i];
		}
		return sum / x.length;
	}
	public static double[] take(InputSampleStream s, int n) throws IOException {
		double[] signal = zeros(n);
		for (int i = 0; i < n; i++) {
			signal[i] = s.read();
		}
		return signal;
	}
	public static void log(String s) {
		System.err.println(s);
	}
	public static void writeSilence(Sender.OutputStreamWrapper dst, double seconds) throws IOException {
		final double dt = 1.0 / Config.sampleRate;
		for (double t = 0; t < seconds; t += dt) {
			dst.write(0.0);
		}	
	}

}
