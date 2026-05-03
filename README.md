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
- `java -jar copy-1.3.jar <path>`

## Output path naming
- output is created in the same parent directory as input
- naming format: `yyyy-MM-dd-HH-mm-ss-SSS-<originalName>`
- example input `D:\foo\bar\demo.txt` -> output `D:\foo\bar\2026-05-03-16-49-15-610-demo.txt`

## Overwrite / conflict policy
- copy uses `Files.copy` without replace mode
- if target path already exists, program exits with non-zero code and prints error to stderr
- invalid arguments/path also return non-zero code

## Examples
### copy
- `java -jar copy-1.3.jar fileName.suffix`
- `java -jar copy-1.3.jar "file name.suffix"`
- `java -jar copy-1.3.jar directoryName`
- `java -jar copy-1.3.jar D:\foo\bar\fileName.suffix`
- `java -jar copy-1.3.jar D:\foo\bar\directoryName`
- `java -jar copy-1.3.jar /home/foo/bar/directoryName`
- `java -jar copy-1.3.jar /home/foo/bar/fileName`
