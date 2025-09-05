package de.bethibande.reedsolomon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public record Metadata(
        int shardSize,
        long actualFileSize
) {

    public static final Path METADATA_FILE = EncodeDemo.DATA_PATH.resolve("file.metadata");

    public static void writeFileMetadata(final Metadata metadata) throws IOException {
        Files.writeString(METADATA_FILE,
                          "%d:%d".formatted(metadata.shardSize, metadata.actualFileSize),
                          StandardOpenOption.CREATE,
                          StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static Metadata readFileMetadata() throws IOException {
        final String str = Files.readString(METADATA_FILE);
        final String[] parts = str.split(":");
        return new Metadata(Integer.parseInt(parts[0]), Long.parseLong(parts[1]));
    }

}
