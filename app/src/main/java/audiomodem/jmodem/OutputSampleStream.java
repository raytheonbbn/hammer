package audiomodem.jmodem;

import java.io.IOException;

public interface OutputSampleStream {

	public void write(double sample) throws IOException;

}
