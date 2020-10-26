package audiomodem.jmodem;

class Filter {

	final double[] coeffs;
	final int length;
	final int end;
	final double[] buffer;

	public Filter(double[] c) {
		coeffs = c;
		length = c.length;
		buffer = new double[length];
		end = length - 1;
	}

	public double process(double value) {
		System.arraycopy(buffer, 1, buffer, 0, length - 1);
		buffer[end] = value;

		double result = 0;
		for (int i = 0; i < length; i++) {
			result += (buffer[i] * coeffs[end - i]);
		}
		return result;
	}
}
