package audiomodem.jmodem;

class Vector {

	private final double[] data;
	private final int offset;
	public final int size;

	public Vector(double[] data, int offset, int size) {
		this.data = data;
		this.offset = offset;
		this.size = size;
	}

	public Vector(double[] data) {
		this.data = data;
		this.offset = 0;
		this.size = data.length;
	}

	public Vector slice(int begin, int end) {
		return new Vector(data, begin, end - begin);
	}

	public static Vector concat(double[] ...vs) {
		int total = 0;
		for (double[] v : vs) {
			total += v.length;
		}
		double[] data = new double[total];
		int i = 0;
		for (double[] v : vs) {
			for (double e : v) {
				data[i] = e;
				i++;
			}
		}
		return new Vector(data);
	}

	double dot(Vector v) {
		assert(v.size == this.size);
		double res = 0;
		for (int i = 0; i < size; i++) {
			res += (this.data[this.offset + i] * v.data[v.offset + i]);
		}
		return res;
	}

	double power() {
		double res = 0;
		for (int i = 0; i < size; i++) {
			double s = this.data[this.offset + i];
			res += (s * s);
		}
		return res;
	}

	double norm() {
		return Math.sqrt(power());
	}

}
