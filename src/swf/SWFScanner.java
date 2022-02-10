package src.swf;

import java.util.concurrent.*;
import java.io.*;

import static src.DependencyChecker.DEBUG;

public class SWFScanner {
    private SWFConfig config;
    // private SWFProcessHost _ph;
    // private SWFProcessClient _pc;
    public ThreadPoolExecutor fileTaskPool;
    private String ext = "swf";
    private int maxDepth;
    // The number of tasks to keep around per thread in the queue.
    public static final int maxQueueFactor = 20;
    // The amount of time, in hours, to wait for fileTaskPool to exit.
    public static final int poolWaitTime = 1000;

    public SWFScanner(SWFConfig c) {
        this.config = c;
        this.maxDepth = c.getMaxDepth();
        // Create the threadpool.
        fileTaskPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(c.getThreadCount());
    }

    /**
     * Start scanning and wait for it to complete.
     */
    public void scan() {
        // We start with zero files scanned.
        Integer totalScanned = 0;
        // For each file that we were supposed to scan,
        for (String filename : this.config.getFileList()) {
            // Scan it, and record the number of files scanned.
            totalScanned = recurseForFiles(new File(filename), totalScanned, 0);
        }
        if (DEBUG) {
            synchronized (System.out) {
                System.out.println("Done recursing.");
            }
        }
        // Shut down the pool. Note that this prevents more tasks from being added,
        // but doesn't discard the tasks that we already have queued. It also doesn't
        // block.
        fileTaskPool.shutdown();
        // This shouldn't be very long: we have taken special care to ensure that the
        // queue doesn't grow very large.
        if (DEBUG) {
            synchronized (System.out) {
                System.out.println("waiting for tasks to complete.");
            }
        }
        try {
            fileTaskPool.awaitTermination(poolWaitTime, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            synchronized (System.out) {
                System.out.println("awaitTermination interrupted, exiting!");
            }
            System.exit(3);
        }
    }

    /**
     * Submit an swf-scanning task to the pool, blocking until enough space is
     * available.
     * 
     * @param swf The file to scan.
     */
    private void StageFileForScanning(File swf) {
        // While there are too many tasks in the pool...
        while (fileTaskPool.getQueue().size() > this.config.getThreadCount() * maxQueueFactor) {
            try {
                // Sleep for 100 ms, and check again.
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // If we're interrupted, log that and exit.
                synchronized (System.out) {
                    System.out.println("Interrupted, exiting.");
                }
                System.exit(3);
            }
        }
        // Submit the task, when there is enough capacity.
        fileTaskPool.submit(() -> {
            ScanFile(swf);
        });
    }

    /**
     * Scans a single file and logs the results appropriately.
     * 
     * @param swf The file to scan.
     */
    private void ScanFile(File swf) {
        try {
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("SWFScanner.ScanFile called");
                }
            }
            // Create a new decompiler for it.
            SWFDecompiler dec = new SWFDecompiler(swf, this.config.getOutputDetailLevel());
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("created dec.");
                }
            }
            // Decompile and search the file for terms.
            boolean found = dec.scanFile(this.config.getPcode());
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("Done, found = " + (found ? 1 : 0));
                }
            }
            // Mark the file as processed (and possible ignore it for future runs).
            config.markAsProcessed(swf.getCanonicalPath());
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("marked as processed.");
                }
            }
            // Write a relevant message to the log. The exact message will be determined by
            // config.getOuptuDetailLevel().
            config.writeLog(dec.GetOutputString(found));
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("wrote to log.");
                }
            }
        } catch (Exception e) {
            synchronized (System.out) {
                System.out.println("Error scanning file: " + e.toString());
            }
        }

    }

    /**
     * Recursively index non-ignored filenames that end with a given extension in
     * startDir, up to a maximum depth.
     * 
     * @param startDir The directory to search in.
     * @param ext      The extension that the files should have.
     * @param num      The number of files already indexed by previous tasks.
     * @param depth    Our current depth in the filetree.
     * @param maxDepth The maximum allowed depth in the filetree.
     * @return The total number of files indexed.
     */
    // TODO: make a prettier non-recursive wrapper for this.
    private Integer recurseForFiles(File startFile, Integer num, Integer depth) {
        // The number of files we have picked up so far.
        Integer n = num;
        // The depth that we're currently at.
        Integer d = depth;
        try {
            if (startFile.isDirectory()) {
                // List the files in the starting dir.
                File[] files = startFile.listFiles();
                // For each file in startDir,
                for (File file : files) {
                    // If we either lack a recursion limit or we haven't reached it yet and we're
                    // scanning subdirs
                    // and we're below the scanlimit,
                    if ((maxDepth < 0 || maxDepth >= d + 1) && this.config.getSSF() && n < this.config.getScanLimit()) {
                        // Recurse on that directory, adding one to the depth.
                        // Set the output of that to n.
                        n = recurseForFiles(file, n, d + 1);
                    }
                }
            } else {
                // If the file extension matches the extension that we're searching for
                // and we're below the scanlimit.
                if (getFileExtension(startFile).equals(ext) && n < this.config.getScanLimit()) {
                    // Get the file's absolute, symlink-resolved path.
                    String fileStr = startFile.getCanonicalPath();
                    // By default, don't ignore the file.
                    Boolean fileIgnore = false;
                    try {
                        // Try to check if the filename is in the list of ignored ones.
                        fileIgnore = this.config.getIgnoreList().contains(fileStr);
                    } catch (Exception e) {
                        // Do nothing, list was empty.
                    }

                    // Are we ignoring the file?
                    if (!fileIgnore) {
                        // No, we open it and include it. TODO: can we make the actual opening be lazy?
                        // Also, increment the file counter. Give the file this as its number.
                        StageFileForScanning(startFile);
                        // System.out.println("File " + n + ": " + fileStr);
                    }
                }
            }
        } catch (Exception e) {
            // Whoops, we hit an error.
            synchronized (System.out) {
                System.out.println("Recurse error " + e);
            }
            e.printStackTrace();
        }

        // Return the number of files that we got.
        return n;
    }

    /**
     * Gets the file extension from a filepath.
     * 
     * @param file The file that is of interest.
     * @return The file's extension.
     */
    private String getFileExtension(File file) {
        // Get the file's name, including the extension.
        String fileName = file.getName();
        // If the filename contains '.',
        if (fileName.lastIndexOf(".") > 0) {
            // Everything from the last dot until the end is the extension.
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        // The file has no extension.
        else
            return "";
    }
}