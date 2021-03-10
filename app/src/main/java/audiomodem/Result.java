package audiomodem;

public class Result {
    public final String out;
    public final String err;
    public final int crc32;

    public Result(String o, String e, int c) {
        out = o;
        err = e;
        crc32 = c;
    }
}
