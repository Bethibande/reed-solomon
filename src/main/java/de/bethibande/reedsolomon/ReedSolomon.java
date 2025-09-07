package de.bethibande.reedsolomon;

import java.util.ArrayList;
import java.util.List;

public class ReedSolomon {

    private final int dataShards;
    private final int parityShards;
    private final int totalShards;

    private final Matrix matrix;

    public ReedSolomon(final int dataShards, final int parityShards) {
        this.dataShards = dataShards;
        this.parityShards = parityShards;
        this.totalShards = dataShards + parityShards;
        this.matrix = Matrix.vandermonde(parityShards, dataShards);
    }

    private Matrix eliminateDeadRows(final Matrix matrix,
                                     final List<Integer> presentRows) {
        final Matrix output = new Matrix(presentRows.size(), matrix.getColumns());
        for (int i = 0; i < presentRows.size(); i++) {
            output.setRow(i, matrix.getRow(presentRows.get(i)));
        }

        return output;
    }

    public void decode(final byte[][] dataShards,
                       final byte[][] parityShards,
                       final int shardSize) {
        final Matrix totalDecodeMatrix = new Matrix(this.totalShards, this.dataShards);
        totalDecodeMatrix.copyFrom(Matrix.identity(this.dataShards), 0, this.dataShards, 0, this.dataShards, 0, 0);
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

        final Matrix allMatrix = new Matrix(allVector.length, shardSize, allVector);
        final Matrix reducedAllMatrix = eliminateDeadRows(allMatrix, presentRows);

        final Matrix reducedMatrix = eliminateDeadRows(totalDecodeMatrix, presentRows);
        final Matrix inverseMatrix = reducedMatrix.inverse();

        final Matrix result = inverseMatrix.multiply(reducedAllMatrix);

        for(int i = 0; i < dataShards.length; i++) {
            dataShards[i] = result.getRow(i);
        }
    }

    public void encode(final byte[][] dataShards, final byte[][] parityShards, final int shardSize) {
        final Matrix dataMatrix = new Matrix(dataShards.length, shardSize, dataShards);
        final Matrix parityMatrix = matrix.multiply(dataMatrix);

        for(int i = 0; i < parityShards.length; i++) {
            if (parityShards[i] == null) parityShards[i] = new byte[shardSize]; // Initialize missing parity shards
            System.arraycopy(parityMatrix.getRow(i), 0, parityShards[i], 0, shardSize);
        }
    }

    public boolean canRecover(final int missingDataShards, final int missingParityShards) {
        return (missingDataShards + missingParityShards) <= parityShards;
    }

}
