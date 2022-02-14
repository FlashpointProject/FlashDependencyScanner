md build\classes
md build\bin
javac -cp lib\ffdec_lib.jar;lib\argparse4j-0.9.0.jar;lib\json-20211205.jar -d build\classes src\*.java src\swf\*.java
jar cfm build\bin\flashdeps.jar manifest.txt -C build\classes src\
copy lib\ffdec_lib.jar build\bin
copy lib\argparse4j-0.9.0.jar build\bin
copy lib\json-20211205.jar build\bin
rem Little hack on the next line appends nothing to processedfiles.csv.
rem I don't know why it works, but these people said it would: https://www.dostips.com/forum/viewtopic.php?t=3476
rem. >> processedfiles.csv
