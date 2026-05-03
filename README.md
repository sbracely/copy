# Copy
- copy file
- copy directory

## Develop environments
- JDK 8+
- Maven 3.x

## Logging
- use JDK built-in `java.util.logging` (JUL)

## Usage
- download or build release copy-1.3.jar
- install JRE 8+
- `java -jar copy-1.3.jar [options] <path...>`

## Options
- `-h`, `--help`: show usage
- `-v`, `--version`: show version
- `--dry-run`: print planned operations without writing files
- `--out-dir <dir>`: write output to the specified directory
- `--replace`: replace target files if they already exist
- `--skip-if-exists`: skip files that already exist
- `--name-strategy <indexed|timestamp|uuid>`: configure output name generation strategy (default: `indexed`)

## Output path naming
- output is created in the same parent directory as input
- `indexed` strategy (default): keep original name first; if exists, append English-parentheses suffix like ` (1)`, ` (2)`, ...
- `timestamp` strategy: prepend timestamp like `yyyy-MM-dd-HH-mm-ss-SSS-`
- `uuid` strategy: prepend random UUID like `550e8400-e29b-41d4-a716-446655440000-`
- examples:
  - indexed: `D:\foo\bar\demo.txt` -> `D:\foo\bar\demo (1).txt`
  - timestamp: `D:\foo\bar\demo.txt` -> `D:\foo\bar\2026-05-03-18-23-01-123-demo.txt`
  - uuid: `D:\foo\bar\demo.txt` -> `D:\foo\bar\550e8400-e29b-41d4-a716-446655440000-demo.txt`

## Overwrite / conflict policy
- default: if target path exists, copy fails with non-zero exit code
- `--replace`: overwrite existing target files
- `--skip-if-exists`: keep existing files and skip copy
- `--replace` and `--skip-if-exists` cannot be used together

## Security / safety defaults
- symbolic link directories/files are skipped during directory copy
- directory walk does not follow symbolic links
- invalid arguments/path return non-zero exit code

## Examples
### copy
- `java -jar copy-1.3.jar fileName.suffix`
- `java -jar copy-1.3.jar "file name.suffix"`
- `java -jar copy-1.3.jar fileA.txt fileB.txt`
- `java -jar copy-1.3.jar directoryName`
- `java -jar copy-1.3.jar directoryA directoryB`
- `java -jar copy-1.3.jar D:\foo\bar\fileName.suffix`
- `java -jar copy-1.3.jar D:\foo\bar\directoryName`
- `java -jar copy-1.3.jar /home/foo/bar/directoryName`
- `java -jar copy-1.3.jar /home/foo/bar/fileName`
