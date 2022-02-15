# FlashDependencyScanner
Scans flash files to check and see the possibility of external dependencies

Requires Java 8.  
Build it with `./gradlew build` or `.\gradlew.bat build`, depending on your platform.  
That will create some archives with the binaries at `flashdeps/build/distributions`. Extract whichever achive you like, and run it with the appropriate script or batchfile, depending on your platform.

This program is licensed under the [MIT License](licenses/FlashDependencyScanner/LICENSE).

This program uses the following libraries:
 - [JPEXS Free Flash Decompiler](https://github.com/jindrapetrik/jpexs-decompiler)
 - [Argparse4j](https://github.com/argparse4j/argparse4j)
 - [JSON in Java](https://github.com/stleary/JSON-java)

We extend our utmost thanks to the authors of those libraries.
