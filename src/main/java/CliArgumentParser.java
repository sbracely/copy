import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class CliArgumentParser {
    private static final Logger LOGGER = Logger.getLogger(CliArgumentParser.class.getName());

    private CliArgumentParser() {
    }

    static CopyOptions parseArguments(String[] args) throws CopyCliException {
        LOGGER.log(Level.INFO, "arguments: {0}", Arrays.toString(args));
        final Options cliOptions = buildCliOptions();
        final CommandLine commandLine;
        try {
            commandLine = DefaultParser.builder()
                    .setAllowPartialMatching(false)
                    .get()
                    .parse(cliOptions, args);
        } catch (ParseException e) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "invalid arguments: " + e.getMessage());
        }

        final CopyOptions options = new CopyOptions();
        options.showHelp = commandLine.hasOption("help");
        if (options.showHelp) {
            return options;
        }

        if (commandLine.hasOption("out-dir")) {
            options.outputDir = parsePathArg(commandLine.getOptionValue("out-dir"), "--out-dir").toAbsolutePath();
        }

        if (options.outputDir != null && Files.exists(options.outputDir) && !Files.isDirectory(options.outputDir)) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "--out-dir must be a directory path");
        }

        final List<String> positionalArgs = commandLine.getArgList();

        if (positionalArgs.isEmpty()) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "require at least ONE path");
        }
        for (String positionalArg : positionalArgs) {
            options.inputPaths.add(parseInputPath(positionalArg));
        }
        return options;
    }

    static void printHelp() throws CopyCliException {
        final String usage = "java -jar copy-1.4.jar [options] <path...>";
        final StringBuilder buffer = new StringBuilder();
        final HelpFormatter helpFormatter = HelpFormatter.builder()
                .setHelpAppendable(new TextHelpAppendable(buffer))
                .get();
        try {
            helpFormatter.printHelp(usage, "", buildCliOptions(), "", true);
        } catch (IOException e) {
            throw new CopyCliException(ExitCodes.IO_ERROR, "failed to render help: " + e.getMessage());
        }
        LOGGER.info(buffer.toString().trim());
    }

    private static Options buildCliOptions() {
        return new Options()
                .addOption("h", "help", false, "show usage")
                .addOption(
                        Option.builder("o").longOpt("out-dir").hasArg().argName("dir")
                                .desc("write output to the specified directory").get()
                );
    }

    private static Path parseInputPath(String inputArg) throws CopyCliException {
        LOGGER.log(Level.INFO, "path: {0}", inputArg);
        final Path inputPath = parsePathArg(inputArg, "input").toAbsolutePath();
        if (!Files.exists(inputPath)) {
            throw new CopyCliException(
                    ExitCodes.INVALID_INPUT,
                    "input path does not exist: " + inputPath + " (arg: " + inputArg + ")"
            );
        }
        return inputPath;
    }

    private static Path parsePathArg(String rawPathArg, String fieldName) throws CopyCliException {
        final String normalizedPathArg = rawPathArg.trim();
        try {
            return Paths.get(normalizedPathArg);
        } catch (InvalidPathException e) {
            throw new CopyCliException(
                    ExitCodes.INVALID_INPUT,
                    "invalid " + fieldName + " path: " + rawPathArg
            );
        }
    }

}
