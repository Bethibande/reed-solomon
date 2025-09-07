package de.bethibande.reedsolomon;

public class GF256 {

    public static final int FIELD_SIZE = 256;
    public static final int GENERATOR = 0x11d;

    private static final byte[] exp = new byte[FIELD_SIZE * 2];
    private static final int[] log = new int[FIELD_SIZE];
    private static final byte[] mul = new byte[FIELD_SIZE * FIELD_SIZE];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            exp[i] = (byte) x;
            log[x] = i;
            x <<= 1;
            if (x >= 256) {
                x ^= GENERATOR;
            }
        }
        exp[255] = exp[0];

        log[0] = -1;

        for (int a = 0; a < FIELD_SIZE; a++) {
            for (int b = 0; b < FIELD_SIZE; b++) {
                if (a == 0 || b == 0) {
                    mul[(a << 8) | b] = 0;
                    continue;
                }

                mul[(a << 8) | b] = exp[log[a] + log[b]];
            }
        }
    }

    public static int add(int a, int b) {
        return a ^ b;
    }

    public static byte mul(int a, int b) {
        return mul[(a << 8) | b];
    }

    public static byte pow(int a, int power) {
        if (power == 0) return 1;
        if (a == 0) return 0;
        int logResult = (log[a] * power) % (FIELD_SIZE - 1);
        if (logResult < 0) logResult += (FIELD_SIZE - 1);
        return exp[logResult];
    }

    public static byte inverse(int a) {
        if (a == 0) throw new ArithmeticException("inverse of 0");
        return exp[FIELD_SIZE - 1 - log[a]];
    }

}
