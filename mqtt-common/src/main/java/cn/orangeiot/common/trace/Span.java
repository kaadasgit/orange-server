package cn.orangeiot.common.trace;

import org.apache.commons.lang3.RandomUtils;

import java.util.Random;
import java.util.RandomAccess;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-08-29
 */
public final class Span {

    static final char[] HEX_DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String getTraceId() {
        return idToHex(new Random().nextLong());
    }

    private static String idToHex(long id) {
        char[] data = new char[16];
        writeHexLong(data, 0, id);
        return new String(data);
    }


    public static byte[] LongToBytes(long values) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    private static void writeHexLong(char[] data, int pos, long v) {
        LongToBytes(v);

        writeHexByte(data, pos + 0, (byte) ((v >>> 56L) & 0xff));
        writeHexByte(data, pos + 2, (byte) ((v >>> 48L) & 0xff));
        writeHexByte(data, pos + 4, (byte) ((v >>> 40L) & 0xff));
        writeHexByte(data, pos + 6, (byte) ((v >>> 32L) & 0xff));
        writeHexByte(data, pos + 8, (byte) ((v >>> 24L) & 0xff));
        writeHexByte(data, pos + 10, (byte) ((v >>> 16L) & 0xff));
        writeHexByte(data, pos + 12, (byte) ((v >>> 8L) & 0xff));
        writeHexByte(data, pos + 14, (byte) (v & 0xff));
    }

    private static void writeHexByte(char[] data, int pos, byte b) {
        data[pos + 0] = HEX_DIGITS[(b >> 4) & 0xf];
        data[pos + 1] = HEX_DIGITS[b & 0xf];
    }


    public static void main(String[] args) {
        System.out.println(Span.getTraceId());
    }
}
