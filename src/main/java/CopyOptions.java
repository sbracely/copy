import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CopyOptions {
    final List<Path> inputPaths = new ArrayList<>();
    Path outputDir;
    NameStrategy nameStrategy = NameStrategy.INDEXED;
    boolean dryRun;
    boolean replaceExisting;
    boolean skipIfExists;
    boolean showHelp;
    boolean showVersion;
}
