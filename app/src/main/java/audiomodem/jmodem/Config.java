package audiomodem.jmodem;

class Config {

	final static double scalingFactor = 30e3;
	final static int sampleRate = 8000;

	final static int symbolLength = 8;
	final static int carrierFreq = 2000;
	final static int baudRate = sampleRate / symbolLength;

	final static int checksumSize = 4;
	final static int frameSize = 250;

	final static int prefixSymbols = 200;
	final static int prefixSilence = 50;
	final static int prefixLength = prefixSymbols + prefixSilence;

	final static int trainingSilence = 50;
	final static int trainingSymbols = 200;
	final static int trainingConstant = 16;

	final static int trainingLength = trainingSilence * 2 + trainingSymbols;

	final static int fileBufferSize = 4096;
	final static int postfixSilence = 100;
	public static Complex[] constellation = {
		new Complex(0, +1), 
		new Complex(0, -1)
	};

}
