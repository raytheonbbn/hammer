package audiomodem.jmodem;

import java.io.IOException;

class Equalizer {

	private final int order;
	private final int lookahead;

	public Equalizer(int order, int lookahead) {
		this.order = order;
		this.lookahead = lookahead;
	}

	public Filter run(InputSampleStream s) throws IOException {
		int size = Config.symbolLength * Config.trainingLength;
		double[] signal = Utils.take(s, size);

		double[] expected = Utils.zeros(size);
		BufferedStream stream = new BufferedStream(expected);
		Modulator m = new Modulator(stream);
		m.writeTraining();

		Filter filt = train(signal, expected);

		double[] filtered = Utils.zeros(signal.length + lookahead);
		for (int i = 0; i < signal.length; i++) {
			filtered[i] = filt.process(signal[i]);
		}
		for (int i = 0; i < lookahead; i++) {
			filtered[i + signal.length] = filt.process(s.read());
		}
		Demodulator d = new Demodulator(new BufferedStream(filtered), null);
		for (int i = 0; i < Config.trainingSymbols; i++) {
			d.getSymbol();
			// TODO: verify training sequence
		}
		return filt;
	}

	public Filter train(double[] signal, double[] expected) {
		double[] padding = Utils.zeros(lookahead);
		Vector x = Vector.concat(signal, padding);
		Vector y = Vector.concat(padding, expected);
		final int L = x.size;

		final int N = order + lookahead;
		double[] Rxx = Utils.zeros(N);
		double[] Rxy = Utils.zeros(N);

		for (int i = 0; i < N; i++) {
			Vector x_ = x.slice(0, L - i);
			Rxx[i] = x.slice(i, L).dot(x_);
			Rxy[i] = y.slice(i, L).dot(x_);
		}

		return new Filter(Levinson.solver(Rxx, Rxy));
	}

}
