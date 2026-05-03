final class CopyCliException extends Exception {
    private final int exitCode;

    CopyCliException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    int getExitCode() {
        return exitCode;
    }
}
