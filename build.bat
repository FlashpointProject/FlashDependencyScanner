md build\classes
md build\bin
javac -cp lib\ffdec_lib.jar;lib\argparse4j-0.9.0.jar -d build\classes src\*.java src\swf\*.java
jar cfm build\bin\flashdeps.jar manifest.txt -C build\classes src\
