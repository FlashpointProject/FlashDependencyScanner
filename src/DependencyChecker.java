package src;

import java.io.*;
import src.swf.*;

public class DependencyChecker {

    public static void main(String[] args) {
        // Parse the commandline
        SWFConfig c = SWFConfig.ParseCLI(args);

        // Prevent the library from using System.err.println();
        PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });
        System.setErr(dummy);
        
        // Start scanning the files.
        SWFScanner swfc = new SWFScanner(c);
        // Scan the files, and wait for exit.
        swfc.scan();
        // Close the files, etc.
        c.cleanUp();
    }
}