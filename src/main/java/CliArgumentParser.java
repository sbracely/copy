import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CliArgumentParser {
    private CliArgumentParser() {
    }

    static CopyOptions parseArguments(String[] args, Logger logger) throws CopyCliException {
        logger.log(Level.INFO, "params: {0}", Arrays.toString(args));
        final CopyOptions options = new CopyOptions();
        final List<String> positionalArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                options.showHelp = true;
            } else if ("-v".equals(arg) || "--version".equals(arg)) {
                options.showVersion = true;
            } else if ("--dry-run".equals(arg)) {
                options.dryRun = true;
            } else if ("--replace".equals(arg)) {
                options.replaceExisting = true;
            } else if ("--skip-if-exists".equals(arg)) {
                options.skipIfExists = true;
            } else if ("--out-dir".equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new CopyCliException(ExitCodes.INVALID_INPUT, "require output directory after --out-dir");
                }
                options.outputDir = Paths.get(args[++i]).toAbsolutePath();
            } else if ("--name-strategy".equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new CopyCliException(ExitCodes.INVALID_INPUT, "require strategy after --name-strategy");
                }
                options.nameStrategy = parseNameStrategy(args[++i]);
            } else if (arg.startsWith("-")) {
                throw new CopyCliException(ExitCodes.INVALID_INPUT, "unknown option: " + arg);
            } else {
                positionalArgs.add(arg);
            }
        }

        if (options.replaceExisting && options.skipIfExists) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "--replace and --skip-if-exists cannot be used together");
        }

        if (options.outputDir != null && Files.exists(options.outputDir) && !Files.isDirectory(options.outputDir)) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "--out-dir must be a directory path");
        }

        if (options.showHelp || options.showVersion) {
            return options;
        }

        if (positionalArgs.isEmpty()) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "require at least ONE path");
        }
        for (String positionalArg : positionalArgs) {
            options.inputPaths.add(parseInputPath(positionalArg, logger));
        }
        return options;
    }

    static void printHelp(Logger logger) {
        logger.info("usage: java -jar copy-1.3.jar [options] <path...>");
        logger.info("options: -h,--help  -v,--version  --dry-run  --replace  --skip-if-exists  --out-dir <dir>  --name-strategy <indexed|timestamp|uuid>");
    }

    private static Path parseInputPath(String inputArg, Logger logger) throws CopyCliException {
        logger.log(Level.INFO, "path: {0}", inputArg);
        final Path inputPath = Paths.get(inputArg).toAbsolutePath();
        if (!Files.exists(inputPath)) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "input path does not exist");
        }
        return inputPath;
    }

    private static NameStrategy parseNameStrategy(String strategyValue) throws CopyCliException {
        if ("indexed".equalsIgnoreCase(strategyValue)) {
            return NameStrategy.INDEXED;
        }
        if ("timestamp".equalsIgnoreCase(strategyValue)) {
            return NameStrategy.TIMESTAMP;
        }
        if ("uuid".equalsIgnoreCase(strategyValue)) {
            return NameStrategy.UUID;
        }
        throw new CopyCliException(ExitCodes.INVALID_INPUT, "unsupported name strategy: " + strategyValue);
    }
}
