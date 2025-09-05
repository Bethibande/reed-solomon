package de.bethibande.reedsolomon;

public class GF256 {

    public static final int FIELD_SIZE = 256;
    public static final int GENERATOR = 0x11d;

    private static final byte[] exp = new byte[FIELD_SIZE * 2];
    private static final int[] log = new int[FIELD_SIZE];

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
    }

    public static int add(int a, int b) {
        return a ^ b;
    }

    public static int sub(int a, int b) {
        return a ^ b;
    }

    public static byte mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return exp[log[a] + log[b]];
    }

    public static byte div(int a, int b) {
        if (b == 0) throw new ArithmeticException("Division by zero");
        if (a == 0) return 0;
        int diff = log[a & 0xFF] - log[b & 0xFF];
        return exp[diff];
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
