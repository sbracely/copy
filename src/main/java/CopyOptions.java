import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CopyOptions {
    final List<Path> inputPaths = new ArrayList<>();
    Path outputDir;
    boolean showHelp;
}
