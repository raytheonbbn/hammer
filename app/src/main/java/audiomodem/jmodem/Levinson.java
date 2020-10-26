package audiomodem.jmodem;

class Levinson {

	public static double[] solver(double[] t, double[] y) {
		final int N = t.length;
		assert N == y.length; 
		
		double[] v = Utils.zeros(N);
		v[0] = 1 / t[0];
		
		double[][] f = new double[N][N];
		double[][] b = new double[N][N];
		f[0] = v;
		b[0] = v;
		
		for (int n = 1; n < N; n++) {
			double[] prev_f = f[n-1];
			double[] prev_b = b[n-1];
			
			double ef = 0;
			double eb = 0;
			for (int i = 0; i < n; i++) {
				ef += (t[n-i] * prev_f[i]);
				eb += (t[i+1] * prev_b[i]);				
			}
			double[] f_ = Utils.zeros(N);
			double[] b_ = Utils.zeros(N);
			System.arraycopy(prev_f, 0, f_, 0, n);
			System.arraycopy(prev_b, 0, b_, 1, n);
			
			double det = 1 - ef * eb;
			double[] f_current = f[n];
			double[] b_current = b[n];
			for (int i = 0; i <= n; i++) {
				f_current[i] = (f_[i] - ef * b_[i]) / det;
				b_current[i] = (b_[i] - eb * f_[i]) / det;
			}
		}
		
		double[] x = Utils.zeros(N);
		for (int n = 0; n < N; n++) {
			double ef = 0;
			for (int i = 0; i < n; i++) {
				ef += (t[n-i] * x[i]);
			}
			double err = (y[n] - ef);
			double[] b_ = b[n];
			for (int i = 0; i <= n; i++) {
				x[i] += err * b_[i];
			}
		}		
		return x;
	}

}
