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
        final String[] normalizedArgs = normalizeMergedArgs(args, logger);
        final Options cliOptions = buildCliOptions();
        final CommandLine commandLine;
        try {
            commandLine = DefaultParser.builder()
                    .setAllowPartialMatching(false)
                    .get()
                    .parse(cliOptions, normalizedArgs);
        } catch (ParseException e) {
            throw new CopyCliException(ExitCodes.INVALID_INPUT, "invalid arguments: " + e.getMessage());
        }

        final CopyOptions options = new CopyOptions();
        options.showHelp = commandLine.hasOption("h");
        options.showVersion = commandLine.hasOption("v");
        options.dryRun = commandLine.hasOption("dry-run");
        options.replaceExisting = commandLine.hasOption("replace");
        options.skipIfExists = commandLine.hasOption("skip-if-exists");
        if (commandLine.hasOption("out-dir")) {
            options.outputDir = parsePathArg(commandLine.getOptionValue("out-dir"), "--out-dir").toAbsolutePath();
        }
        if (commandLine.hasOption("name-strategy")) {
            options.nameStrategy = parseNameStrategy(commandLine.getOptionValue("name-strategy"));
        }
        final List<String> positionalArgs = commandLine.getArgList();

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

    static void printHelp(Logger logger) throws CopyCliException {
        final String usage = "java -jar copy-1.3.jar [options] <path...>";
        final StringBuilder buffer = new StringBuilder();
        final HelpFormatter helpFormatter = HelpFormatter.builder()
                .setHelpAppendable(new TextHelpAppendable(buffer))
                .get();
        try {
            helpFormatter.printHelp(usage, "", buildCliOptions(), "", true);
        } catch (IOException e) {
            throw new CopyCliException(ExitCodes.IO_ERROR, "failed to render help: " + e.getMessage());
        }
        logger.info(buffer.toString().trim());
    }

    private static Options buildCliOptions() {
        final Options options = new Options();
        options.addOption("h", "help", false, "show usage");
        options.addOption("v", "version", false, "show version");
        options.addOption(null, "dry-run", false, "print planned operations without writing files");
        options.addOption(null, "replace", false, "replace target files if they already exist");
        options.addOption(null, "skip-if-exists", false, "skip files that already exist");
        options.addOption(Option.builder().longOpt("out-dir").hasArg().argName("dir").desc("write output to the specified directory").build());
        options.addOption(Option.builder().longOpt("name-strategy").hasArg().argName("indexed|timestamp|uuid").desc("configure output name generation strategy").build());
        return options;
    }

    private static Path parseInputPath(String inputArg, Logger logger) throws CopyCliException {
        logger.log(Level.INFO, "path: {0}", inputArg);
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
        final String normalizedPathArg = sanitizeQuotedPathToken(rawPathArg);
        if (normalizedPathArg.indexOf('"') >= 0) {
            throw new CopyCliException(
                    ExitCodes.INVALID_INPUT,
                    "invalid " + fieldName + " path: " + rawPathArg + ". For quoted Windows paths, avoid trailing backslash before the closing quote."
            );
        }
        try {
            return Paths.get(normalizedPathArg);
        } catch (InvalidPathException e) {
            throw new CopyCliException(
                    ExitCodes.INVALID_INPUT,
                    "invalid " + fieldName + " path: " + rawPathArg + ". For quoted Windows paths, avoid trailing backslash before the closing quote."
            );
        }
    }

    private static String sanitizeQuotedPathToken(String rawPathArg) {
        String token = rawPathArg.trim();
        if (token.length() >= 2 &&
                ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'")))) {
            token = token.substring(1, token.length() - 1);
        }

        // PowerShell may leave a dangling double quote when a quoted path ending with '\' is merged with next option.
        if (token.endsWith("\"") && token.indexOf('"') == token.length() - 1) {
            token = token.substring(0, token.length() - 1).trim();
        } else if (token.startsWith("\"") && token.lastIndexOf('"') == 0) {
            token = token.substring(1).trim();
        }
        return token;
    }

    private static String[] normalizeMergedArgs(String[] args, Logger logger) {
        final List<String> normalizedArgs = new ArrayList<>();
        for (String arg : args) {
            final int optionStartIndex = findMergedOptionStart(arg);
            if (optionStartIndex > 0) {
                final String pathPart = arg.substring(0, optionStartIndex).trim();
                final String optionPart = arg.substring(optionStartIndex).trim();
                if (!pathPart.isEmpty()) {
                    normalizedArgs.add(pathPart);
                }
                normalizedArgs.addAll(splitOptionPart(optionPart));
            } else {
                normalizedArgs.add(arg);
            }
        }

        final String[] normalizedArray = normalizedArgs.toArray(new String[0]);
        if (!Arrays.equals(args, normalizedArray)) {
            logger.log(Level.INFO, "normalized params: {0}", Arrays.toString(normalizedArray));
        }
        return normalizedArray;
    }

    private static int findMergedOptionStart(String arg) {
        if (arg.trim().startsWith("-")) {
            return -1;
        }
        final String[] optionMarkers = new String[]{
                " --out-dir",
                " --name-strategy",
                " --dry-run",
                " --replace",
                " --skip-if-exists",
                " --help",
                " --version"
        };

        int firstIndex = -1;
        for (String marker : optionMarkers) {
            final int index = arg.indexOf(marker);
            if (index >= 0 && (firstIndex < 0 || index < firstIndex)) {
                firstIndex = index;
            }
        }
        return firstIndex < 0 ? -1 : firstIndex + 1;
    }

    private static List<String> splitOptionPart(String optionPart) {
        final List<String> tokens = new ArrayList<>();
        final StringBuilder currentToken = new StringBuilder();
        char quoteChar = 0;

        for (int i = 0; i < optionPart.length(); i++) {
            final char c = optionPart.charAt(i);
            if (quoteChar == 0) {
                if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken.setLength(0);
                    }
                } else if (c == '"' || c == '\'') {
                    quoteChar = c;
                } else {
                    currentToken.append(c);
                }
            } else {
                if (c == quoteChar) {
                    quoteChar = 0;
                } else {
                    currentToken.append(c);
                }
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        return tokens;
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
