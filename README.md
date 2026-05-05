# Copy
- copy file
- copy directory

## Develop environments
- JDK 8+
- Maven 3.x

## Logging
- use JDK built-in `java.util.logging` (JUL)

## Usage
- download or build release copy-1.5.jar
- install JRE 8+
- `java -jar copy-1.5.jar [options] <path...>`

## Options
- `-h`, `--help`: show usage
- `-o`, `--out-dir <dir>`: write output to the specified directory

## Output path naming
- output is created in the same parent directory as input
- default strategy: keep original name first; if exists, append English-parentheses suffix like ` (1)`, ` (2)`, ...
- examples:
  - `D:\foo\bar\demo.txt` -> `D:\foo\bar\demo (1).txt`

## Security / safety defaults
- symbolic link directories/files are skipped during directory copy
- directory walk does not follow symbolic links
- invalid arguments/path return non-zero exit code

## Examples
### copy
- `java -jar copy-1.5.jar fileName.suffix`
- `java -jar copy-1.5.jar "file name.suffix"`
- `java -jar copy-1.5.jar fileA.txt fileB.txt`
- `java -jar copy-1.5.jar directoryName`
- `java -jar copy-1.5.jar directoryA directoryB`
- `java -jar copy-1.5.jar D:\foo\bar\fileName.suffix`
- `java -jar copy-1.5.jar D:\foo\bar\directoryName`
- `java -jar copy-1.5.jar /home/foo/bar/directoryName`
- `java -jar copy-1.5.jar /home/foo/bar/fileName`
