import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCopySingleFile() throws IOException {
        Path sourceFile = tempDir.resolve("demo.txt");
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));

        RunResult runResult = run(sourceFile);

        assertEquals(0, runResult.exitCode);

        List<Path> copiedFiles = Files.list(tempDir)
                .filter(path -> !path.equals(sourceFile))
                .filter(path -> path.getFileName().toString().endsWith("-demo.txt"))
                .collect(Collectors.toList());
        assertEquals(1, copiedFiles.size());
        assertEquals("hello", new String(Files.readAllBytes(copiedFiles.get(0)), StandardCharsets.UTF_8));
    }

    @Test
    void shouldCopyDirectory() throws IOException {
        Path sourceDir = tempDir.resolve("src");
        Path nestedDir = sourceDir.resolve("nested");
        Files.createDirectories(nestedDir);
        Path sourceFile = nestedDir.resolve("a.txt");
        Files.write(sourceFile, "abc".getBytes(StandardCharsets.UTF_8));

        RunResult runResult = run(sourceDir);

        assertEquals(0, runResult.exitCode);

        List<Path> copiedDirs = Files.list(tempDir)
                .filter(Files::isDirectory)
                .filter(path -> !path.equals(sourceDir))
                .filter(path -> path.getFileName().toString().endsWith("-src"))
                .collect(Collectors.toList());
        assertEquals(1, copiedDirs.size());

        Path copiedFile = copiedDirs.get(0).resolve("nested").resolve("a.txt");
        assertTrue(Files.exists(copiedFile));
        assertEquals("abc", new String(Files.readAllBytes(copiedFile), StandardCharsets.UTF_8));
    }

    @Test
    void shouldReturnErrorWhenInputPathDoesNotExist() throws IOException {
        Path missingFile = tempDir.resolve("missing.txt");

        RunResult runResult = run(missingFile);

        assertEquals(2, runResult.exitCode);
    }

    private RunResult run(Path inputPath) throws IOException {
        int exitCode = Main.run(new String[]{inputPath.toString()});
        return new RunResult(exitCode);
    }

    private static class RunResult {
        private final int exitCode;

        private RunResult(int exitCode) {
            this.exitCode = exitCode;
        }
    }
}
