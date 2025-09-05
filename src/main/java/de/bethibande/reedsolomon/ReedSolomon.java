package de.bethibande.reedsolomon;

import java.util.ArrayList;
import java.util.List;

public class ReedSolomon {

    private final int dataShards;
    private final int parityShards;
    private final int totalShards;

    private final ByteMatrix matrix;

    public ReedSolomon(final int dataShards, final int parityShards) {
        this.dataShards = dataShards;
        this.parityShards = parityShards;
        this.totalShards = dataShards + parityShards;
        this.matrix = buildVandermondeMatrix(parityShards, dataShards);
    }

    private ByteMatrix buildVandermondeMatrix(final int rows, final int cols) {
        final ByteMatrix encodeMatrix = new ByteMatrix(rows, cols);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                encodeMatrix.set(row, col, GF256.pow(col + 1, row));
            }
        }
        return encodeMatrix;
    }

    private ByteMatrix eliminateDeadRows(final ByteMatrix matrix,
                                         final List<Integer> presentRows) {
        final ByteMatrix output = new ByteMatrix(presentRows.size(), matrix.getColumns());
        for (int i = 0; i < presentRows.size(); i++) {
            output.setRow(i, matrix.getRow(presentRows.get(i)));
        }

        return output;
    }

    public void decode(final byte[][] dataShards,
                       final byte[][] parityShards,
                       final int shardSize) {
        final ByteMatrix totalDecodeMatrix = new ByteMatrix(this.totalShards, this.dataShards);
        totalDecodeMatrix.copyFrom(ByteMatrix.identity(this.dataShards), 0, this.dataShards, 0, this.dataShards, 0, 0);
        totalDecodeMatrix.copyFrom(this.matrix, 0, this.parityShards, 0, this.dataShards, this.dataShards, 0);

        final byte[][] allVector = new byte[this.totalShards][shardSize];
        System.arraycopy(dataShards, 0, allVector, 0, dataShards.length);
        System.arraycopy(parityShards, 0, allVector, dataShards.length, parityShards.length);

        final List<Integer> presentRows = new ArrayList<>();
        for (int i = 0; i < allVector.length; i++) {
            if (allVector[i] != null) {
                presentRows.add(i);
            }
            if (presentRows.size() == dataShards.length) break;
        }

        final ByteMatrix allMatrix = new ByteMatrix(allVector.length, shardSize, allVector);
        final ByteMatrix reducedAllMatrix = eliminateDeadRows(allMatrix, presentRows);

        final ByteMatrix reducedMatrix = eliminateDeadRows(totalDecodeMatrix, presentRows);
        final ByteMatrix inverseMatrix = reducedMatrix.inverse();

        final ByteMatrix result = inverseMatrix.multiply(reducedAllMatrix);

        for(int i = 0; i < dataShards.length; i++) {
            dataShards[i] = result.getRow(i);
        }
    }

    public void encode(final byte[][] dataShards, final byte[][] parityShards, final int shardSize) {
        final ByteMatrix dataMatrix = new ByteMatrix(dataShards.length, shardSize, dataShards);
        final ByteMatrix parityMatrix = matrix.multiply(dataMatrix);

        for(int i = 0; i < parityShards.length; i++) {
            if (parityShards[i] == null) parityShards[i] = new byte[shardSize]; // Initialize missing parity shards
            System.arraycopy(parityMatrix.getRow(i), 0, parityShards[i], 0, shardSize);
        }
    }

    public boolean canRecover(final int missingDataShards, final int missingParityShards) {
        return (missingDataShards + missingParityShards) <= parityShards;
    }

}
