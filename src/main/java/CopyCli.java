import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

            if (options.inputPath == null) {
                throw new CopyCliException(ExitCodes.INVALID_INPUT, "require ONE path");
            }

            if (options.outputDir != null && !options.dryRun) {
                Files.createDirectories(options.outputDir);
            }

            final Path outputPath = OutputPathResolver.buildOutputPath(
                    options.inputPath,
                    options.outputDir,
                    options.nameStrategy
            );
            new CopyProcessor(LOGGER).copy(options.inputPath, outputPath, options);
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
}
