import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CopyProcessor {
    private static final Logger LOGGER = Logger.getLogger(CopyProcessor.class.getName());

    CopyStats copy(Path inputPath, Path outputPath) throws IOException {
        if (Files.isDirectory(inputPath)) {
            return copyDirectory(inputPath, outputPath);
        } else {
            final CopyStats copyStats = new CopyStats();
            copyFile(inputPath, outputPath);
            copyStats.createdFileCount++;
            return copyStats;
        }
    }

    private CopyStats copyDirectory(Path inputPath, Path outputPath) throws IOException {
        final CopyStats copyStats = new CopyStats();

        Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!inputPath.equals(dir) && Files.isSymbolicLink(dir)) {
                    copyStats.skippedSymbolicLinkCount++;
                    LOGGER.warning("skipped symbolic link directory: " + dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                final Path directoryPath = outputPath.resolve(inputPath.relativize(dir));
                Files.createDirectories(directoryPath);
                LOGGER.info("created directory: " + directoryPath);
                copyStats.createdDirectoryCount++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    copyStats.skippedSymbolicLinkCount++;
                    LOGGER.warning("skipped symbolic link file: " + file);
                    return FileVisitResult.CONTINUE;
                }
                final Path outPutPath = outputPath.resolve(inputPath.relativize(file));
                copyFile(file, outPutPath);
                copyStats.createdFileCount++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.log(Level.WARNING, "visit file failed path: " + file, exc);
                copyStats.visitFileFailedCount++;
                return FileVisitResult.CONTINUE;
            }
        });
        return copyStats;
    }

    private void copyFile(Path inputPath, Path outputPath) throws IOException {
        Files.copy(inputPath, outputPath);
        LOGGER.info("created file: " + outputPath);
    }

    static class CopyStats {
        int createdDirectoryCount;
        int createdFileCount;
        int skippedFileCount;
        int skippedSymbolicLinkCount;
        int visitFileFailedCount;

        void merge(CopyStats other) {
            this.createdDirectoryCount += other.createdDirectoryCount;
            this.createdFileCount += other.createdFileCount;
            this.skippedFileCount += other.skippedFileCount;
            this.skippedSymbolicLinkCount += other.skippedSymbolicLinkCount;
            this.visitFileFailedCount += other.visitFileFailedCount;
        }
    }
}
