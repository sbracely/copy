import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CopyProcessor {
    private final Logger logger;

    CopyProcessor(Logger logger) {
        this.logger = logger;
    }

    CopyStats copy(Path inputPath, Path outputPath, CopyOptions options) throws IOException {
        if (Files.isDirectory(inputPath)) {
            return copyDirectory(inputPath, outputPath, options);
        } else {
            final CopyStats copyStats = new CopyStats();
            final CopyOutcome copyOutcome = copyFile(inputPath, outputPath, options);
            if (copyOutcome == CopyOutcome.CREATED) {
                copyStats.createdFileCount++;
            } else {
                copyStats.skippedFileCount++;
            }
            return copyStats;
        }
    }

    private CopyStats copyDirectory(Path inputPath, Path outputPath, CopyOptions options) throws IOException {
        final CopyStats copyStats = new CopyStats();

        Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!inputPath.equals(dir) && Files.isSymbolicLink(dir)) {
                    copyStats.skippedSymbolicLinkCount++;
                    logger.warning("skipped symbolic link directory: " + dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                final Path directoryPath = outputPath.resolve(inputPath.relativize(dir));
                if (options.dryRun) {
                    logger.info("dry run create directory: " + directoryPath);
                } else {
                    Files.createDirectories(directoryPath);
                    logger.info("created directory: " + directoryPath);
                }
                copyStats.createdDirectoryCount++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    copyStats.skippedSymbolicLinkCount++;
                    logger.warning("skipped symbolic link file: " + file);
                    return FileVisitResult.CONTINUE;
                }
                final Path outPutPath = outputPath.resolve(inputPath.relativize(file));
                final CopyOutcome copyOutcome = copyFile(file, outPutPath, options);
                if (copyOutcome == CopyOutcome.CREATED) {
                    copyStats.createdFileCount++;
                } else {
                    copyStats.skippedFileCount++;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.log(Level.WARNING, "visit file failed path: " + file, exc);
                copyStats.visitFileFailedCount++;
                return FileVisitResult.CONTINUE;
            }
        });
        return copyStats;
    }

    private CopyOutcome copyFile(Path inputPath, Path outputPath, CopyOptions options) throws IOException {
        if (options.dryRun) {
            logger.info("dry run copy file: " + inputPath + " -> " + outputPath);
            return CopyOutcome.SKIPPED;
        }

        if (Files.exists(outputPath)) {
            if (options.skipIfExists) {
                logger.info("skipped existing file: " + outputPath);
                return CopyOutcome.SKIPPED;
            }
            if (!options.replaceExisting) {
                throw new IOException("target path already exists: " + outputPath);
            }
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(inputPath, outputPath);
        }
        logger.info("created file: " + outputPath);
        return CopyOutcome.CREATED;
    }

    private enum CopyOutcome {
        CREATED,
        SKIPPED
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
