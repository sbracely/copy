import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int EXIT_OK = 0;
    private static final int EXIT_INVALID_INPUT = 2;
    private static final int EXIT_IO_ERROR = 3;

    public static void main(String[] args) {
        final int exitCode = run(args);
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        try {
            final Path inputPath = parseInputPath(args);
            final Path outputPath = buildOutputPath(inputPath);

            if (Files.isDirectory(inputPath)) {
                copyDirectory(inputPath, outputPath);
            } else {
                copyFile(inputPath, outputPath);
            }

            LOGGER.info("finish");
            return EXIT_OK;
        } catch (AppException e) {
            LOGGER.severe("error: " + e.getMessage());
            return e.exitCode;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error: I/O failure: " + e.getMessage(), e);
            return EXIT_IO_ERROR;
        }
    }

    private static Path parseInputPath(String[] args) throws AppException {
        LOGGER.log(Level.INFO, "params = {0}", Arrays.toString(args));

        if (args.length != 1) {
            throw new AppException(EXIT_INVALID_INPUT, "require ONE path");
        }

        LOGGER.log(Level.INFO, "path = {0}", args[0]);
        final Path inputPath = Paths.get(args[0]).toAbsolutePath();
        if (!Files.exists(inputPath)) {
            throw new AppException(EXIT_INVALID_INPUT, "input path does not exist");
        }
        return inputPath;
    }

    private static Path buildOutputPath(Path inputPath) throws AppException {
        final Path parentPath = inputPath.getParent();
        if (parentPath == null) {
            throw new AppException(EXIT_INVALID_INPUT, "input path must have a parent directory");
        }
        return parentPath.resolve(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS-")) + inputPath.getFileName());
    }

    private static void copyDirectory(Path inputPath, Path outputPath) throws IOException {
        final List<Path> createDirectoryList = new LinkedList<>();
        final List<Path> createFileList = new LinkedList<>();
        final List<Path> visitFileFailedList = new LinkedList<>();

        final Pattern pattern = Pattern.compile(inputPath.toString(), Pattern.LITERAL);
        final String quoteReplacement = Matcher.quoteReplacement(outputPath.toString());

        Files.walkFileTree(inputPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                final Path directoryPath = getTargetPath(dir);
                Files.createDirectories(directoryPath);
                createDirectoryList.add(directoryPath);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                final Path outPutPath = getTargetPath(file);
                copyFile(file, outPutPath);
                createFileList.add(outPutPath);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.log(Level.WARNING, "visit file failed path = " + file, exc);
                visitFileFailedList.add(file);
                return FileVisitResult.CONTINUE;
            }

            /**
             * build a path to the target
             * @param path
             *        the path to the file to copy
             * @return the path to the target
             */
            private Path getTargetPath(Path path) {
                return Paths.get(pattern.matcher(path.toString()).replaceFirst(quoteReplacement));
            }
        });

        LOGGER.info("create directory count = " + createDirectoryList.size());
        createDirectoryList.forEach(path -> LOGGER.info(path.toString()));

        LOGGER.info("create file count = " + createFileList.size());
        createFileList.forEach(path -> LOGGER.info(path.toString()));

        LOGGER.info("visit file failed count = " + visitFileFailedList.size());
        visitFileFailedList.forEach(path -> LOGGER.warning(path.toString()));

    }

    private static void copyFile(Path inputPath, Path outputPath) throws IOException {
        Files.copy(inputPath, outputPath);
        LOGGER.info("create file : " + outputPath);
    }

    private static class AppException extends Exception {
        private final int exitCode;

        private AppException(int exitCode, String message) {
            super(message);
            this.exitCode = exitCode;
        }
    }

}
