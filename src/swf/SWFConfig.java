package src.swf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.impl.Arguments;

public class SWFConfig {
    public static enum OutputDetail {
        PATH_VAL, // Only the path and the output value. This is the bare minimum functionality.
        PATH_VAL_HIT, // The path, the output value, and (if applicable) which function was hit.
        PATH_VAL_ALLHITS // The path, the output value, and (if applicable) all the hits found.
    }

    private BufferedWriter outputFile;
    private ReentrantLock outputFile_lock = new ReentrantLock();
    private int threadCount;
    private int scanLimit;
    private boolean pcode;
    private boolean ssf;
    private int maxDepth;
    private OutputDetail outputDetailLevel;
    private BufferedWriter processedFile;
    private ReentrantLock processedFile_lock = new ReentrantLock();
    private List<String> ignoreList;
    private List<String> files;

    // The constructor does nothing.
    public SWFConfig() {}

    // All the parsing is in this function.
    public static SWFConfig ParseCLI(String[] args) {
        //parse CLI to get everything...
        SWFConfig c = new SWFConfig();
        
        // Use a real parser to do this. Hand-rolled stuff is great until it breaks.
        ArgumentParser parser = ArgumentParsers.newFor("SWFDepChecker").build().defaultHelp(true).description("Checks if one or more SWFs is multi-asset.");
        MutuallyExclusiveGroup pcode_ascript = parser.addMutuallyExclusiveGroup();
        pcode_ascript.addArgument("--pcode").help("Decompile to P-Code, instead of ActionScript.").action(Arguments.storeConst())
                     .dest("pcode").setConst(true).setDefault(true).type(boolean.class);
        pcode_ascript.addArgument("--ascript").help("Decompile to ActionScript, instead of P-Code.").action(Arguments.storeConst())
                     .dest("pcode").setConst(false).setDefault(true).type(boolean.class);
        parser.addArgument("--ssf", "--search-subfolders").help("Search subfolders for SWFs too.").action(Arguments.storeConst())
              .dest("ssf").setConst(true).setDefault(false).type(boolean.class);
        parser.addArgument("--max-depth").help("Set a maximum subfolder depth to search. Implies --ssf.").action(Arguments.store())
              .dest("maxdepth").setDefault(-1).type(int.class);
        parser.addArgument("--scanlimit").help("A total limit on the number of SWFs scanned.").action(Arguments.store())
              .dest("scanlimit").setDefault(-1).type(int.class);
        // A format for the current date and time.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        // Set the default output filename to "SWFScan_yyyy-MM-dd_HH-mm-ss.csv".
        parser.addArgument("-o", "--output").help("Set the file to write output to.").action(Arguments.store())
              .dest("output").setDefault("SWFScan_"+dtf.format(LocalDateTime.now())+".csv").type(String.class);
        // TODO: replace with -v, -vv, etc. verbosity spec?
        parser.addArgument("-d", "--detail").help("Set the output detail level, 1-5.").action(Arguments.store())
              .dest("detail").setDefault(1).type(int.class);
        parser.addArgument("-t", "--threads").help("Set the number of threads to use.").action(Arguments.store())
        .dest("threads").setDefault(1).type(int.class);
        parser.addArgument("--ignore-list").help("Set a file containing a list of absolute paths to ignore.").action(Arguments.store())
              .dest("ignorelist").setDefault("processedfiles.csv").type(String.class);
        parser.addArgument("--processed-log").help("Set the file to log a list of already-processed files.").action(Arguments.store())
              .dest("processedlist").setDefault("processedfiles.csv").type(String.class);
        // Exactly one of the two file-input methods must be used.
        MutuallyExclusiveGroup file_list = parser.addMutuallyExclusiveGroup().required(true);
        // Note that the type is different for these two. This makes it easy to determine which one was used.
        file_list.addArgument("--file-list").help("A file containing a list of files or directories (if ssf is set) to scan.")
                 .dest("filelist").action(Arguments.store()).type(String.class);
        file_list.addArgument("file").nargs("*").help("The files or directories (if ssf is set) to scan.").dest("filelist")
                 .action(Arguments.store()).type(List.class);
        // The results of the parsing. Init to null.
        Namespace results = null;
        try {
            results = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        // Now to actually set all of these things.
        c.setPcode(results.getBoolean("pcode")); // Implemented
        c.setSSF(results.getBoolean("ssf")); // Implemented.
        c.setMaxDepth(results.getInt("maxdepth")); // Implemented.
        c.setScanLimit(results.getInt("scanlimit"));
        c.setOutputFilePath(results.getString("output")); // Implemented.
        c.setOutputDetailLevel(results.getInt("detail")); // Implemented.
        c.setThreadCount(results.getInt("threads")); // Implemented.
        // Note: read the ignore before opening the processed list.
        c.setIgnoreListPath(results.getString("ignorelist")); // Implemented
        c.setProcessedListFile(results.getString("processedlist")); // Implemented.
        if (results.get("filelist").getClass().equals(List.class)) {
            c.setFileList(results.getList("filelist")); // Implemented.
        } else {
            c.setFileList(results.getString("filelist")); // Implemented.
        }
        return c;
    }

    /**
     * Does EOL tasks for this object - closing files, etc.
     */
    public void cleanUp() {
        outputFile_lock.lock();
        try {
            // Try to close the file.
            outputFile.close();
            // If it's already closed, we get an IOException. Just carry on.
        } catch (IOException e) {}
        outputFile_lock.unlock();
        // Now for the processed file.
        processedFile_lock.lock();
        try {
            // Try to close the file.
            processedFile.close();
            // If it's already closed, we get an IOException. Just carry on.
        } catch (IOException e) {}
        processedFile_lock.unlock();
        // All the rest of the stuff will be cleaned up by the GC.
    }


    //////////////////////////////////////////////////////////
    //----------- Getter and Setter Functions --------------//
    //////////////////////////////////////////////////////////
    /**
     * Get the current list of SWFs and directories.
     * @return The current list.
     */
    public List<String> getFileList() {
        return this.files;
    }
    /**
     * Sets the list of SWFs and directories.
     * @param newList The list to set it to.
     */
    public void setFileList(List<String> newList) {
        this.files = newList;
    }
    /**
     * Reads the list of SWFs and directories from a file. Exits if the reading is unsuccessful.
     * @param listPath The path for the list of SWFs and directories.
     */
    public void setFileList(String listPath) {
        // Initialize this to an empty list.
        this.files = new ArrayList<String>();
        try {
            // Read in all lines from the file list.
            BufferedReader reader = new BufferedReader(new FileReader(listPath));
            String line;
            while ((line = reader.readLine()) != null) {
                this.files.add(line);
            }
            reader.close();
        } catch (IOException e) {
            // If there was an error, yell at the user.
            System.out.println("Error while reading file list: "+e.toString());
            System.exit(2);
        }
    }

    // No more javadoc for you. You know what these do.
    public BufferedWriter getOutputFilePath() { return this.outputFile; }
    public void setOutputFilePath(String outputFilePath) {
        try {
            this.outputFile_lock.lock();
            // If it's non-null, close it to prevent leaks.
            if (this.outputFile != null) {this.outputFile.close();}
            // Open the processed list file. TODO: remember to close this at the end.
            // Note: the second argument to FileWriter opens it append-only.
            this.outputFile = new BufferedWriter(new FileWriter(outputFilePath, true));
            this.outputFile_lock.unlock();
        } catch (IOException e) {
            this.outputFile_lock.unlock();
            // If there was an error, yell at the user.
            System.out.println("Error while opening the output file: "+e.toString());
            System.exit(2);
        }
    }
    /**
     * Writes a string to the log, with the appropriate locking.
     * @param logstr The string to write. Should have its own newlines, etc.
     */
    public void writeLog(String logstr) {
        // Lock the writer.
        this.outputFile_lock.lock();
        try {
            // TODO: flush this at the end.
            // Write the string.
            this.outputFile.write(logstr);
            // Unlock.
            this.outputFile_lock.unlock();
        } catch (IOException e) {
            // We failed, somehow. Unlock.
            this.outputFile_lock.unlock();
        }
    }

    public int getScanLimit()           { return this.scanLimit;  }
    public void setScanLimit(int count) { this.scanLimit = count; }


    public int getThreadCount() { return this.threadCount; }
    public void setThreadCount(int count) {
        // Thread count must be greater than zero.
        if (count > 0) {
            // Cap thread count at 50.
            if(count > 50) { count = 50; }
            this.threadCount = count;
        } else {
            // Minimum thread count: 1.
            this.threadCount = 1;
        }
    }

    public BufferedWriter getProcessedListFile() {
        return this.processedFile;
    }
    public void setProcessedListFile(String processedListPath) {
        try {
            this.processedFile_lock.lock();
            // If it's non-null, close it to prevent leaks.
            if (this.processedFile != null) {this.processedFile.close();}
            // Open the processed list file. TODO: remember to close this at the end.
            // Note: the second argument to FileWriter opens it append-only.
            this.processedFile = new BufferedWriter(new FileWriter(processedListPath, true));
            this.processedFile_lock.unlock();
        } catch (IOException e) {
            this.processedFile_lock.unlock();
            // If there was an error, yell at the user.
            System.out.println("Error while opening the processed file list: "+e.toString());
            System.exit(2);
        }
    }
    /**
     * Marks a single file as processed in the file, with the appropriate locking.
     * @param filename The file to mark as processed.
     * @return True if successful, false on an error.
     */
    public boolean markAsProcessed(String filename) {
        // Lock the writer.
        this.processedFile_lock.lock();
        try {
            // TODO: flush this at the end.
            // Write the filename, and add a newline.
            this.processedFile.write(filename);
            this.processedFile.newLine();
            // Unlock and return true.
            this.processedFile_lock.unlock();
            return true;
        } catch (IOException e) {
            // We failed, somehow. Unlock and return false.
            this.processedFile_lock.unlock();
            return false;
        }
    }

    public List<String> getIgnoreList() { return this.ignoreList; }
    public void setIgnoreListPath(String ignoreListPath) {
        // Initialize this to an empty list.
        this.ignoreList = new ArrayList<String>();
        try {
            // Read in all lines from the file list.
            BufferedReader reader = new BufferedReader(new FileReader(ignoreListPath));
            String line;
            while ((line = reader.readLine()) != null) {
                this.ignoreList.add(line);
            }
            reader.close();
        } catch (IOException e) {
            // If there was an error, yell at the user.
            System.out.println("Error while reading ignore list: "+e.toString());
            System.exit(2);
        }
    }

    public boolean getPcode() { return this.pcode;     }
    public void setPcode(boolean usePcode) { 
        this.pcode = usePcode; 
    }

    public boolean getSSF()         { return this.ssf; }
    public void setSSF(boolean ssf) { this.ssf = ssf;  }

    public int getMaxDepth() { return this.maxDepth; }
    public void setMaxDepth(int maxdepth) {
        // If we're getting a non-default value,
        if (maxdepth != -1) {
            // Set ssf to true.
            this.setSSF(true);            
        }
        // Set the value.
        this.maxDepth =  maxdepth;
    }
    public OutputDetail getOutputDetailLevel()        { return this.outputDetailLevel; }
    public void setOutputDetailLevel(OutputDetail od) { this.outputDetailLevel = od;   }
    public void setOutputDetailLevel(int od) { 
        switch(od) {
            case 1:
                setOutputDetailLevel(OutputDetail.PATH_VAL);
                break;
            case 2:
                setOutputDetailLevel(OutputDetail.PATH_VAL_HIT);
                break;
            case 3:
                setOutputDetailLevel(OutputDetail.PATH_VAL_ALLHITS);
                break;
            default:
                // Should never happen: the parser should give us a nice value.
                setOutputDetailLevel(OutputDetail.PATH_VAL);
                break;
        }
    }
}