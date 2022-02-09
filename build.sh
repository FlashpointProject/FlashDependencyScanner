#!/usr/bin/env bash
# We only need two libraries: ffdec and argparse4j.
javac --class-path "lib/ffdec_lib.jar:lib/argparse4j-0.9.0.jar" -d build/classes src/*.java src/swf/*.java
# Package it in a jar.
jar -c -f build/bin/flashdeps.jar -m manifest.txt -C build/classes src/
# Copy the dependencies to the bin directory.
cp lib/ffdec_lib.jar lib/argparse4j-0.9.0.jar build/bin/
