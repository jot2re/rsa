package dk.jot2re.mult.gilboa.util;

public class Fiddling {

    public static int ceil(int numerator, int denominator) {
        int res = numerator/denominator;
        if (numerator % denominator != 0) {
            return res+1;
        } else {
            return res+0;
        }
    }

    public static byte[] longToBytes(long data) {
        return new byte[]{
                (byte) ((data >> 56) & 0xff),
                (byte) ((data >> 48) & 0xff),
                (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff),
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff),
        };
    }
}
