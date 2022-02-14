package src;

import java.io.*;
import src.swf.*;

public class DependencyChecker {
    // Because it's final, it can be used like a C macro.
    // The java compiler will remove if (DEBUG) when DEBUG is false.
    public static final boolean DEBUG = false;

    public static void main(String[] args) {
        // Parse the commandline
        SWFConfig c = SWFConfig.ParseCLI(args);

        // Prevent the library from using System.err.println();
        PrintStream dummy = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
        System.setErr(dummy);

        // Start scanning the files.
        SWFScanner swfc = new SWFScanner(c);
        // Scan the files, and wait for exit.
        swfc.scan();
        // Close the files, etc.
        c.cleanUp();
    }
}