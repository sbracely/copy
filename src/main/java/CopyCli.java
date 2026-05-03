import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CopyCli {
    private static final Logger LOGGER = Logger.getLogger(CopyCli.class.getName());
    private static final String VERSION = "1.3";

    public static void main(String[] args) {
        final int exitCode = run(args);
        if (exitCode != ExitCodes.OK) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        try {
            final CopyOptions options = CliArgumentParser.parseArguments(args, LOGGER);
            if (options.showHelp) {
                CliArgumentParser.printHelp(LOGGER);
                return ExitCodes.OK;
            }
            if (options.showVersion) {
                LOGGER.info("version: " + VERSION);
                return ExitCodes.OK;
            }

            if (options.inputPaths.isEmpty()) {
                throw new CopyCliException(ExitCodes.INVALID_INPUT, "require at least ONE path");
            }

            if (options.outputDir != null && !options.dryRun) {
                Files.createDirectories(options.outputDir);
            }

            final CopyProcessor copyProcessor = new CopyProcessor(LOGGER);
            final Set<Path> reservedOutputPaths = new HashSet<>();
            final CopyProcessor.CopyStats totalCopyStats = new CopyProcessor.CopyStats();
            for (Path inputPath : options.inputPaths) {
                final Path outputPath = OutputPathResolver.buildOutputPath(
                        inputPath,
                        options.outputDir,
                        options.nameStrategy,
                        reservedOutputPaths
                );
                CopyProcessor.CopyStats copyStats = copyProcessor.copy(inputPath, outputPath, options);
                totalCopyStats.merge(copyStats);
            }
            logCopySummary(totalCopyStats);
            LOGGER.info("finish");
            return ExitCodes.OK;
        } catch (CopyCliException e) {
            LOGGER.severe("error: " + e.getMessage());
            return e.getExitCode();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error: I/O failure: " + e.getMessage(), e);
            return ExitCodes.IO_ERROR;
        }
    }

    private static void logCopySummary(CopyProcessor.CopyStats copyStats) {
        LOGGER.info("created directory count: " + copyStats.createdDirectoryCount);
        LOGGER.info("created file count: " + copyStats.createdFileCount);
        LOGGER.info("skipped file count: " + copyStats.skippedFileCount);
        LOGGER.info("skipped symbolic link count: " + copyStats.skippedSymbolicLinkCount);
        LOGGER.info("visit file failed count: " + copyStats.visitFileFailedCount);
    }
}
