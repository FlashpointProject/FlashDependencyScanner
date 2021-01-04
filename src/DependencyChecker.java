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

    public static List<String> recurseForFiles(File dir) {
        //Add the files to the queue
        List<String> lst = new ArrayList<String>();
        try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					lst.addAll(recurseForFiles(file));
				} else {
					if(getFileExtension(file).equals("swf")) {
                        File f = file;
                        lst.add(f.getCanonicalPath());
                    }
				}
            }
		} catch (Exception e) {
			e.printStackTrace();
        }

        return lst;
    }
    
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") > 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }

    private static void createShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                if(pool != null) if(!pool.isShutdown()) pool.shutdownNow();
            }
        }));
    }
}