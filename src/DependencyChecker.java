package src;

import java.util.*;
import java.io.*;
import src.swf.*;
import java.util.concurrent.*;

public class DependencyChecker {
    public static ExecutorService pool;

    public static void main(String[] args) {
        //Prevent the library from using System.err.println();
        PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });
        System.setErr(dummy);

        //Parse the commandline
        SWFConfig c = SWFConfig.ParseCLI(args);
        //TODO: update the term data to use an actual path.
        c.setTermData("");

        //DEBUG: Print out the commandline
        System.out.println(c);

        //Start scanning the files.
        SWFScanner swfc = new SWFScanner(c);
        swfc.scan();
    }
}