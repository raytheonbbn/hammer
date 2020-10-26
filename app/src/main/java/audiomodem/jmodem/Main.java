package audiomodem.jmodem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class Main {

	public static void receive(InputSampleStream src, OutputStream dst) throws IOException {
		Receiver.run(src,  dst);
	}

	public static void send(InputStream src, OutputSampleStream dst) throws IOException {
		Sender.run(src,  dst);
	}
	
	public static void main(String[] args) throws IOException {
		String cmd = args[0];
		switch (cmd) {
		case "send":
			Sender.main(args);
			return;
		case "recv":
			Receiver.main(args);
			return;
		default:
			Logger.getLogger("Main").info("Invalid command: " + cmd);
		}
	}

}
