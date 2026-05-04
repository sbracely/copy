import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
        LOGGER.log(Level.INFO, "params: {0}", Arrays.toString(args));
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
        options.showHelp = commandLine.hasOption("h");
        if (commandLine.hasOption("out-dir")) {
            options.outputDir = parsePathArg(commandLine.getOptionValue("out-dir"), "--out-dir").toAbsolutePath();
        }
        final List<String> positionalArgs = commandLine.getArgList();

        if (options.outputDir != null && Files.exists(options.outputDir) && !Files.isDirectory(options.outputDir)) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "--out-dir must be a directory path");
        }

        if (options.showHelp) {
            return options;
        }

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
        final Options options = new Options();
        options.addOption("h", "help", false, "show usage");
        options.addOption(Option.builder().longOpt("out-dir").hasArg().argName("dir").desc("write output to the specified directory").get());
        return options;
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
