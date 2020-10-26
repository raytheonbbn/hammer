package audiomodem.jmodem;

import java.io.IOException;

public interface InputSampleStream {

	public double read() throws IOException;

}
