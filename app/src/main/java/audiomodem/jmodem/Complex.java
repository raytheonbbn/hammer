package audiomodem.jmodem;

class Complex {
	public Complex(double realPart, double imagPart) {
		this.real = realPart;
		this.imag = imagPart;
	}

	@Override
	public String toString() {
		return "<" + real + ", " + imag + ">";
	}

	public double real;
	public double imag;

	public int nearest(Complex[] z) {
		double[] d = new double[z.length];
		for (int i = 0; i < d.length; i++) {
			double dx = real - z[i].real;
			double dy = imag - z[i].imag;
			d[i] = dx * dx + dy * dy;
		}
		return Utils.argmin(d);
	}
}
