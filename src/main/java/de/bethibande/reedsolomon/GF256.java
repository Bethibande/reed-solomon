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

    public static byte[][] invertMatrix(byte[][] matrix) {
        int size = matrix.length;
        byte[][] augmented = new byte[size][size * 2];

        // Set up augmented matrix [matrix | identity]
        for (int i = 0; i < size; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, size);
            augmented[i][i + size] = 1;
        }

        // Forward elimination
        for (int col = 0; col < size; col++) {
            // Find pivot row
            int pivotRow = -1;
            for (int row = col; row < size; row++) {
                if ((augmented[row][col] & 0xFF) != 0) {
                    pivotRow = row;
                    break;
                }
            }
            if (pivotRow == -1) throw new IllegalArgumentException("Matrix not invertible");

            // Swap rows if needed
            if (pivotRow != col) {
                byte[] temp = augmented[col];
                augmented[col] = augmented[pivotRow];
                augmented[pivotRow] = temp;
            }

            // Scale pivot row to 1
            int pivotVal = augmented[col][col] & 0xFF;
            int invPivot = inverse(pivotVal);
            for (int c = col; c < size * 2; c++) {
                augmented[col][c] = (byte) mul(augmented[col][c] & 0xFF, invPivot);
            }

            // Eliminate other rows
            for (int row = 0; row < size; row++) {
                if (row != col) {
                    int factor = augmented[row][col] & 0xFF;
                    if (factor != 0) {
                        for (int c = col; c < size * 2; c++) {
                            int val = sub(augmented[row][c] & 0xFF,
                                                mul(factor, augmented[col][c] & 0xFF));
                            augmented[row][c] = (byte) val;
                        }
                    }
                }
            }
        }

        // Extract right half => inverse
        byte[][] inverse = new byte[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(augmented[i], size, inverse[i], 0, size);
        }
        return inverse;
    }

}
