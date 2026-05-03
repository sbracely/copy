import java.nio.file.Path;

final class CopyOptions {
    Path inputPath;
    Path outputDir;
    NameStrategy nameStrategy = NameStrategy.INDEXED;
    boolean dryRun;
    boolean replaceExisting;
    boolean skipIfExists;
    boolean showHelp;
    boolean showVersion;
}
