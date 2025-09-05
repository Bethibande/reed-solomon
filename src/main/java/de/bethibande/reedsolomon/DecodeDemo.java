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

    public static void main(String[] args) throws IOException {
        final Map<String, Path> dataShardFiles = listShards("data");
        final Map<String, Path> parityShardFiles = listShards("parity");

        final byte[][] dataShards = new byte[EncodeDemo.DATA_SHARDS][];
        final byte[][] parityShards = new byte[EncodeDemo.PARITY_SHARDS][];

        final int missingDataShards = readShards(EncodeDemo.DATA_SHARDS, "data", dataShardFiles, dataShards);
        final int missingParityShards = readShards(EncodeDemo.PARITY_SHARDS, "parity", parityShardFiles, parityShards);

        final ReedSolomon rs = new ReedSolomon(EncodeDemo.DATA_SHARDS, EncodeDemo.PARITY_SHARDS);
        if (!rs.canRecover(missingDataShards, missingParityShards)) {
            throw new IllegalStateException("Cannot recover shards too many missing shards (%d data, %d parity)".formatted(missingDataShards, missingParityShards));
        }

        final Metadata metadata = Metadata.readFileMetadata();

        if (missingDataShards > 0) {
            rs.decode(dataShards, parityShards, metadata.shardSize());
            System.out.println("Restored %d data shards".formatted(missingDataShards));

            for (int i = 0; i < dataShards.length; i++) {
                EncodeDemo.writeShard(i, dataShards[i], "data");
            }
        }

        if (missingParityShards > 0) {
            rs.encode(dataShards, parityShards, metadata.shardSize());
            System.out.println("Re-Encoded %d parity shards".formatted(missingParityShards));

            for (int i = 0; i < parityShards.length; i++) {
                EncodeDemo.writeShard(i, parityShards[i], "parity");
            }
        }

        final Path output = Path.of("./decoded.png");
        if (Files.exists(output)) Files.delete(output);

        try (final OutputStream out = Files.newOutputStream(output, StandardOpenOption.CREATE)) {
            for (int i = 0; i < dataShards.length; i++) {
                out.write(dataShards[i], 0, Math.min(dataShards[i].length, (int) (metadata.actualFileSize() - (i * metadata.shardSize()))));
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
