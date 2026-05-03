import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

final class OutputPathResolver {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");

    private OutputPathResolver() {
    }

    static Path buildOutputPath(Path inputPath, Path outputDir, NameStrategy nameStrategy) throws CopyCliException {
        final Path basePath = outputDir != null ? outputDir : inputPath.getParent();
        if (basePath == null) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "input path must have a parent directory");
        }

        final String originalName = inputPath.getFileName().toString();
        if (nameStrategy == NameStrategy.TIMESTAMP) {
            final String timestampPrefix = LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "-";
            return resolveIndexedPath(basePath, timestampPrefix + originalName);
        }
        if (nameStrategy == NameStrategy.UUID) {
            final String uuidPrefix = UUID.randomUUID().toString() + "-";
            return resolveIndexedPath(basePath, uuidPrefix + originalName);
        }
        return resolveIndexedPath(basePath, originalName);
    }

    private static Path resolveIndexedPath(Path basePath, String originalName) {
        final Path candidatePath = basePath.resolve(originalName);
        if (!Files.exists(candidatePath)) {
            return candidatePath;
        }

        final int dotIndex = originalName.lastIndexOf('.');
        final String baseName;
        final String extension;
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        } else {
            baseName = originalName;
            extension = "";
        }

        for (int index = 1; index < Integer.MAX_VALUE; index++) {
            final String nextName = baseName + " (" + index + ")" + extension;
            final Path nextPath = basePath.resolve(nextName);
            if (!Files.exists(nextPath)) {
                return nextPath;
            }
        }
        throw new IllegalStateException("cannot allocate output file name for: " + originalName);
    }
}
