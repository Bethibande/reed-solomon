package de.bethibande.reedsolomon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class EncodeDemo {

    private static final Path FILE = Path.of("./example.png");

    public static final Path DATA_PATH = Path.of("./data/");

    public static final int DATA_SHARDS = 4;
    public static final int PARITY_SHARDS = 2;

    public static void main(String[] args) throws IOException {
        final byte[] data = Files.readAllBytes(FILE);

        final int shardSize = Math.ceilDiv(data.length, DATA_SHARDS);
        final byte[][] dataShards = new byte[DATA_SHARDS][shardSize];
        final byte[][] parityShards = new byte[PARITY_SHARDS][shardSize];

        for(int i = 0; i < dataShards.length; i++) {
            System.arraycopy(data, i * shardSize, dataShards[i], 0, Math.min(shardSize, data.length - i * shardSize));
            if (data.length - i * shardSize < shardSize) {
                for(int j = data.length - i * shardSize; j < shardSize; j++) {
                    dataShards[i][j] = 0;
                }
            }
        }

        final ReedSolomon rs = new ReedSolomon(DATA_SHARDS, PARITY_SHARDS);
        rs.encode(dataShards, parityShards, shardSize);

        cleanDataDirectoryIfPresent();

        int i = 0;
        for (final byte[] dataShard : dataShards) {
            writeShard(i++, dataShard, "data");
        }
        i = 0;
        for (final byte[] parityShard : parityShards) {
            writeShard(i++, parityShard, "parity");
        }

        Metadata.writeFileMetadata(new Metadata(shardSize, Files.size(FILE)));
    }

    public static void writeShard(final int index, final byte[] shard, final String suffix) throws IOException {
        final Path path = DATA_PATH.resolve("shard-" + index + "." + suffix);
        Files.write(path, shard, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void cleanDataDirectoryIfPresent() throws IOException {
        if (Files.exists(DATA_PATH)) {
            try (final Stream<Path> files = Files.list(DATA_PATH)) {
                files.forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        if (!Files.exists(DATA_PATH)) Files.createDirectory(DATA_PATH);
    }

}
