package de.bethibande.reedsolomon;

public class ByteMatrix {

    public static ByteMatrix identity(final int size) {
        final ByteMatrix matrix = new ByteMatrix(size, size);
        for (int i = 0; i < size; i++) {
            matrix.set(i, i, (byte) 1);
        }
        return matrix;
    }

    private final int rows, columns;
    private final byte[][] values;

    public ByteMatrix(final int rows, final int columns) {
        this.rows = rows;
        this.columns = columns;
        this.values = new byte[rows][columns];
    }

    public ByteMatrix(final int row, final int columns, final byte[][] values) {
        this.rows = row;
        this.columns = columns;
        this.values = values;
    }

    public int getColumns() {
        return columns;
    }

    public byte get(final int row, final int column) {
        return this.values[row][column];
    }

    public void setRow(final int row, final byte[] data) {
        System.arraycopy(data, 0, this.values[row], 0, data.length);
    }

    public void set(final int row, final int column, final byte value) {
        this.values[row][column] = value;
    }

    public byte[] getRow(final int row) {
        return this.values[row];
    }

    public void copyFrom(final ByteMatrix other,
                         final int rowStart,
                         final int rowEnd,
                         final int colStart,
                         final int colEnd,
                         final int toRow,
                         final int toCol) {
        for (int row = rowStart; row < rowEnd; row++) {
            for (int col = colStart; col < colEnd; col++) {
                this.set(toRow + row - rowStart, toCol + col - colStart, other.get(row, col));
            }
        }
    }

    public ByteMatrix multiply(final ByteMatrix other) {
        if (this.columns != other.rows) {
            throw new IllegalArgumentException("Matrix dimension mismatch");
        }

        final ByteMatrix result = new ByteMatrix(this.rows, other.columns);
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < other.columns; col++) {
                byte value = 0;
                for (int i = 0; i < this.columns; i++) {
                    value ^= (byte) GF256.mul(get(row, i) & 0xFF, other.get(i, col) & 0xFF);
                }
                result.set(row, col, value);
            }
        }
        return result;
    }

    private void gaussianElimination() {
        int n = rows; // square matrix size
        int totalCols = columns; // augmented matrix cols = 2 * n

        for (int pivotCol = 0; pivotCol < n; pivotCol++) {
            // Find pivot row
            int pivotRow = -1;
            for (int r = pivotCol; r < n; r++) {
                if (values[r][pivotCol] != 0) {
                    pivotRow = r;
                    break;
                }
            }
            if (pivotRow == -1) {
                throw new RuntimeException("Matrix is singular");
            }

            // Swap rows if pivotRow != pivotCol
            if (pivotRow != pivotCol) {
                byte[] tmp = values[pivotCol];
                values[pivotCol] = values[pivotRow];
                values[pivotRow] = tmp;
            }

            // Scale pivot row to make pivot == 1
            byte pivotVal = values[pivotCol][pivotCol];
            byte pivotInv = GF256.inverse(pivotVal & 0xFF);
            for (int c = 0; c < totalCols; c++) {
                values[pivotCol][c] = GF256.mul(values[pivotCol][c] & 0xFF, pivotInv & 0xFF);
            }

            // Eliminate pivot column in all other rows
            for (int r = 0; r < n; r++) {
                if (r != pivotCol && values[r][pivotCol] != 0) {
                    byte factor = values[r][pivotCol];
                    for (int c = 0; c < totalCols; c++) {
                        byte product = GF256.mul(factor & 0xFF, values[pivotCol][c] & 0xFF);
                        values[r][c] ^= product;
                    }
                }
            }
        }
    }

    private ByteMatrix slice(final int rowStart,
                             final int rowEnd,
                             final int colStart,
                             final int colEnd) {
        final ByteMatrix result = new ByteMatrix(rowEnd - rowStart, colEnd - colStart);
        for (int row = rowStart; row < rowEnd; row++) {
            System.arraycopy(this.values[row], colStart, result.values[row - rowStart], 0, colEnd - colStart);
        }
        return result;
    }

    public ByteMatrix augment(final ByteMatrix identity) {
        final ByteMatrix augmented = new ByteMatrix(this.rows, this.columns + identity.columns);
        augmented.copyFrom(this, 0, this.rows, 0, this.columns, 0, 0);
        augmented.copyFrom(identity, 0, identity.rows, 0, identity.columns, 0, this.columns);
        return augmented;
    }

    public ByteMatrix inverse() {
        final ByteMatrix augmented = augment(identity(this.rows));
        augmented.gaussianElimination();

        return augmented.slice(0, rows, columns, columns * 2);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final int maxDigits = (int) Math.ceil(Math.log10(rows));
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                sb.append(String.format("%" + maxDigits + "d ", get(row, col) & 0xFF));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
