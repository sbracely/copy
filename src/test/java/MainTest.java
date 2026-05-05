import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        List<Path> copiedFiles;
        try (Stream<Path> paths = Files.list(tempDir)) {
            copiedFiles = paths
                    .filter(path -> !path.equals(sourceFile))
                    .filter(path -> path.getFileName().toString().equals("demo (1).txt"))
                    .collect(Collectors.toList());
        }
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

        List<Path> copiedDirs;
        try (Stream<Path> paths = Files.list(tempDir)) {
            copiedDirs = paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(sourceDir))
                    .filter(path -> path.getFileName().toString().equals("src (1)"))
                    .collect(Collectors.toList());
        }
        assertEquals(1, copiedDirs.size());

        Path copiedFile = copiedDirs.get(0).resolve("nested").resolve("a.txt");
        assertTrue(Files.exists(copiedFile));
        assertEquals("abc", new String(Files.readAllBytes(copiedFile), StandardCharsets.UTF_8));
    }

    @Test
    void shouldReturnErrorWhenInputPathDoesNotExist() {
        Path missingFile = tempDir.resolve("missing.txt");

        RunResult runResult = run(missingFile);

        assertEquals(2, runResult.exitCode);
    }

    @Test
    void shouldReturnOkForHelp() {
        RunResult runResult = run("--help");
        assertEquals(0, runResult.exitCode);
    }

    @Test
    void shouldOutputToSpecifiedDirectory() throws IOException {
        Path sourceFile = tempDir.resolve("withOutDir.txt");
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));
        Path outDir = tempDir.resolve("output");

        RunResult runResult = run("--out-dir", outDir.toString(), sourceFile.toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(outDir));
        long generatedFileCount;
        try (Stream<Path> paths = Files.list(outDir)) {
            generatedFileCount = paths
                    .filter(path -> path.getFileName().toString().equals("withOutDir.txt"))
                    .count();
        }
        assertEquals(1, generatedFileCount);
    }

    @Test
    void shouldSupportShortOutDirOption() throws IOException {
        Path sourceFile = tempDir.resolve("shortOption.txt");
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));
        Path outDir = tempDir.resolve("short-output");

        RunResult runResult = run("-o", outDir.toString(), sourceFile.toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(outDir.resolve("shortOption.txt")));
    }

    @Test
    void shouldCopyUsingAbsoluteInputPath() throws IOException {
        Path sourceFile = tempDir.resolve("absolute-input.txt").toAbsolutePath();
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));

        RunResult runResult = run(sourceFile.toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(sourceFile.getParent().resolve("absolute-input (1).txt")));
    }

    @Test
    void shouldOutputToAbsoluteOutDir() throws IOException {
        Path sourceFile = tempDir.resolve("absolute-out-source.txt");
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));
        Path outDir = tempDir.resolve("absolute-output").toAbsolutePath();

        RunResult runResult = run("--out-dir", outDir.toString(), sourceFile.toAbsolutePath().toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(outDir.resolve("absolute-out-source.txt")));
    }

    @Test
    void shouldAppendIndexWhenOutputFileNameAlreadyExists() throws IOException {
        Path sourceFile = tempDir.resolve("name.txt");
        Files.write(sourceFile, "hello".getBytes(StandardCharsets.UTF_8));
        Path outDir = tempDir.resolve("output");
        Files.createDirectories(outDir);
        Files.write(outDir.resolve("name.txt"), "existing".getBytes(StandardCharsets.UTF_8));

        RunResult runResult = run("--out-dir", outDir.toString(), sourceFile.toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(outDir.resolve("name.txt")));
        assertTrue(Files.exists(outDir.resolve("name (1).txt")));
    }

    @Test
    void shouldCopyMultipleInputsInOneCommand() throws IOException {
        Path file1 = tempDir.resolve("multi-a.txt");
        Path file2 = tempDir.resolve("multi-b.txt");
        Files.write(file1, "a".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "b".getBytes(StandardCharsets.UTF_8));
        Path outDir = tempDir.resolve("multi-output");

        RunResult runResult = run("--out-dir", outDir.toString(), file1.toString(), file2.toString());

        assertEquals(0, runResult.exitCode);
        assertTrue(Files.exists(outDir.resolve("multi-a.txt")));
        assertTrue(Files.exists(outDir.resolve("multi-b.txt")));
    }

    @Test
    void shouldReturnErrorForMalformedQuotedPathToken() {
        RunResult runResult = run("bad\"path");
        assertEquals(2, runResult.exitCode);
    }

    private RunResult run(Path inputPath) {
        int exitCode = CopyCli.run(new String[]{inputPath.toString()});
        return new RunResult(exitCode);
    }

    private RunResult run(String... args) {
        int exitCode = CopyCli.run(args);
        return new RunResult(exitCode);
    }

    private static class RunResult {
        private final int exitCode;

        private RunResult(int exitCode) {
            this.exitCode = exitCode;
        }
    }
}
