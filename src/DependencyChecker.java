package src;

import java.io.PrintStream;
import java.io.OutputStream;
import src.swf.SWFConfig;
import src.swf.SWFScanner;
import static src.Macros.DEBUG_MAIN;;

public class DependencyChecker {

    public static void main(String[] args) {
        if (DEBUG_MAIN) {
            synchronized (System.out) {
                System.out.println("Startup, parsing args.");
            }
        }
        // Parse the commandline
        SWFConfig c = SWFConfig.ParseCLI(args);

        // Prevent the library from using System.err.println();
        PrintStream dummy = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
        System.setErr(dummy);

        if (DEBUG_MAIN) {
            synchronized (System.out) {
                System.out.println("Starting scanning.");
            }
        }

        // Start scanning the files.
        SWFScanner swfc = new SWFScanner(c);
        // Scan the files, and wait for exit.
        swfc.scan();
        if (DEBUG_MAIN) {
            synchronized (System.out) {
                System.out.println("Scanning done, starting cleanup.");
            }
        }
        // Close the files, etc.
        c.cleanUp();
        if (DEBUG_MAIN) {
            synchronized (System.out) {
                System.out.println("Exiting.");
            }
        }
        // Um... looks like the program is hanging here? I don't understand why.
        // Let's tell the user that it's safe to interrupt us now.
        synchronized (System.out) {
            System.out.println("Scanning complete. It is now safe to interrupt.");
        }
    }
}