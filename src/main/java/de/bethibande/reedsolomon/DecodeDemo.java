package de.bethibande.reedsolomon;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecodeDemo {

    public static final Path OUTPUT_PATH = Path.of("./decoded.png");

    public static void main(String[] args) throws IOException {
        final Map<String, Path> dataShardFiles = listShards(EncodeDemo.DATA_FILE_EXTENSION);
        final Map<String, Path> parityShardFiles = listShards(EncodeDemo.PARITY_FILE_EXTENSION);

        final byte[][] dataShards = new byte[EncodeDemo.DATA_SHARDS][];
        final byte[][] parityShards = new byte[EncodeDemo.PARITY_SHARDS][];

        final int missingDataShards = readShards(EncodeDemo.DATA_SHARDS, EncodeDemo.DATA_FILE_EXTENSION, dataShardFiles, dataShards);
        final int missingParityShards = readShards(EncodeDemo.PARITY_SHARDS, EncodeDemo.PARITY_FILE_EXTENSION, parityShardFiles, parityShards);

        final ReedSolomon rs = new ReedSolomon(EncodeDemo.DATA_SHARDS, EncodeDemo.PARITY_SHARDS);
        if (!rs.canRecover(missingDataShards, missingParityShards)) {
            throw new IllegalStateException("Cannot recover shards too many missing shards (%d data, %d parity)".formatted(missingDataShards, missingParityShards));
        }

        final Metadata metadata = Metadata.readFileMetadata();

        if (missingDataShards > 0) {
            rs.decode(dataShards, parityShards, metadata.shardSize());
            System.out.println("Restored %d data shards".formatted(missingDataShards));

            for (int i = 0; i < dataShards.length; i++) {
                EncodeDemo.writeShard(i, dataShards[i], EncodeDemo.DATA_FILE_EXTENSION);
            }
        }

        if (missingParityShards > 0) {
            rs.encode(dataShards, parityShards, metadata.shardSize());
            System.out.println("Re-Encoded %d parity shards".formatted(missingParityShards));

            for (int i = 0; i < parityShards.length; i++) {
                EncodeDemo.writeShard(i, parityShards[i], EncodeDemo.PARITY_FILE_EXTENSION);
            }
        }

        if (Files.exists(OUTPUT_PATH)) Files.delete(OUTPUT_PATH);

        try (final OutputStream out = Files.newOutputStream(OUTPUT_PATH, StandardOpenOption.CREATE)) {
            for (int i = 0; i < dataShards.length; i++) {
                final long remainingBytes = metadata.actualFileSize() - ((long) i * metadata.shardSize());
                out.write(dataShards[i], 0, Math.min(dataShards[i].length, (int) remainingBytes));
            }
        }
    }

    private static Map<String, Path> listShards(final String suffix) throws IOException {
        try (final Stream<Path> files = Files.list(EncodeDemo.DATA_PATH)) {
            return files.filter(path -> path.toString().endsWith("." + suffix))
                    .collect(Collectors.toMap(path -> path.getFileName().toString(), path -> path));
        }
    }

    private static int readShards(final int shardCount,
                                   final String suffix,
                                   final Map<String, Path> shardFiles,
                                   final byte[][] shards) throws IOException {
        int missing = 0;
        for (int i = 0; i < shardCount; i++) {
            final String fileName = "shard-" + i + "." + suffix;
            final Path path = shardFiles.get(fileName);

            if (path != null) {
                shards[i] = Files.readAllBytes(path);
            } else {
                missing++;
            }
        }

        return missing;
    }

}
