import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

final class OutputPathResolver {
    private OutputPathResolver() {
    }

    static Path buildOutputPath(Path inputPath, Path outputDir, Set<Path> reservedOutputPaths) throws CopyCliException {
        final Path basePath = outputDir != null ? outputDir : inputPath.getParent();
        if (basePath == null) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "input path must have a parent directory");
        }

        final String originalName = inputPath.getFileName().toString();
        return resolveIndexedPath(basePath, originalName, reservedOutputPaths);
    }

    private static Path resolveIndexedPath(Path basePath, String originalName, Set<Path> reservedOutputPaths) {
        final Path candidatePath = basePath.resolve(originalName);
        if (!Files.exists(candidatePath) && !reservedOutputPaths.contains(candidatePath.toAbsolutePath().normalize())) {
            reservedOutputPaths.add(candidatePath.toAbsolutePath().normalize());
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
            final Path normalizedNextPath = nextPath.toAbsolutePath().normalize();
            if (!Files.exists(nextPath) && !reservedOutputPaths.contains(normalizedNextPath)) {
                reservedOutputPaths.add(normalizedNextPath);
                return nextPath;
            }
        }
        throw new IllegalStateException("cannot allocate output file name for: " + originalName);
    }
}
