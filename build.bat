md build\classes
md build\bin
javac -cp lib\ffdec_lib.jar;lib\metaas-0.8.jar;lib\LZMA.jar;lib\cmykjpeg.jar;lib\gson-2.4.jar -d build\classes src\*.java src\sockets\*.java src\swf\*.java
jar cfm build\bin\test.jar manifest.txt -C build\classes src\