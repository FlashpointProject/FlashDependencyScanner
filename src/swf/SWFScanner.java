package src.swf;

import java.util.*;
import java.util.concurrent.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import src.*;
import src.sockets.*;

public class SWFScanner {
    private SWFConfig _config;
    private List<SWFFile> _files;
    private int _ignoreCount;
    private Set<String> _ignoreList;
    private BlockingQueue<SWFFile> _queue;
    private FileLogger _fileLogger;
    private FileLogger _processedLogger;
    //private SWFProcessHost _ph;
    //private SWFProcessClient _pc;
    public static ExecutorService _pool;

    public SWFScanner(SWFConfig c) {
        this._config = c;
        this._fileLogger = new FileLogger(_config, c.getOutputFilePath());
        this._files = new ArrayList<SWFFile>();
        this._pool = Executors.newFixedThreadPool(c.getThreadCount());
        this._ignoreCount = 0;
        this._fileLogger = new FileLogger(_config, c.getOutputFilePath());
        this._ignoreList = new HashSet<String>();

        this._processedLogger = null;
        if(c.getProcessedListPath() != "") {
            this._processedLogger = new FileLogger(_config, c.getProcessedListPath());
        }
    }

    public void scan() {
        // Check: are we a subprocess? (inverted, because !.)
        if(!_config.getIsSubProcess()) {
            // No, do parent process things.
            loadIgnoreList();
            loadFiles();

            scanFiles();
        } else {
            // Yes, do child process things.
            scanFiles();
        }
    }

    /**
     * Read the the ignore list file into the ignore list variable.
     * Does nothing if it's a subprocess.
     * @author krum110487
     */
    private void loadIgnoreList() {
        // Are we a subprocess?
        if(!_config.getIsSubProcess()) {
            // Only do this if the this is NOT the sub-process
            try {
                // Open the ignore list.
                Scanner s = new Scanner(new File(this._config.getIgnoreListPath()));
                // Read the ignore list into our ignore list variable.
                while (s.hasNextLine()){
                    _ignoreList.add(s.nextLine());
                }
                s.close();
            } catch (FileNotFoundException fnf) {
                // Whoops, the file wasn't found.
                fnf.printStackTrace();
            }
        }
    }

    /**
     * If we're a main process, index the directories for a list of files.
     * If we're not, do... nothing?
     * @author krum110487
     */
    private void loadFiles() {
        //System.out.println("Loading Files...");
        // If we're not a subprocess,
        if (!_config.getIsSubProcess()) {
            // Main Process logic, load like normal.
            // If the file-list path is not empty,
            if (!_config.getFileListPath().equals("")) {
                // Do nothing.
                //TODO: implement loading list from a file.
            } else if (!_config.getSourcePath().equals("")) {
                // TODO: figure out what ssf is.
                if(_config.getSSF()) {
                    // Get swf files without a maximum depth.
                    recurseForFiles(new File(_config.getSourcePath()), "swf", 0, 0, -1);
                } else {
                    // Get swf files with a maximum depth of zero: don't enter any directories.
                    recurseForFiles(new File(_config.getSourcePath()), "swf", 0, 0, 0);
                }
            }
            else {
                //throw new Exception("No file supplied, you must include at least --fileList OR a file as the last paramater.");
            }
        } else {
            // Sub Process logic, load from sockets.
            // The output is discarded at the end of this method. Why are we calling these?
            int offset = _config.getSubProcessOffset();
            int limit = _config.getScanLimit();
        }

        // Print out a status message.
        System.out.println(_files.size() + " files loaded successfully.");
        System.out.println(this._ignoreCount + " files ignored.\n");
    }

    private void scanFiles() {
        //For each of the files found, do stuff.
        // Are we running in single-process mode?
        if(_config.getMultiProcessCount() <= 1 && !_config.getIsSubProcess()) {
            //single process logic
            System.out.println("Scan started...");
            loadLocalThreads();
        } else if(_config.getIsSubProcess()) {
            //sub-process logic, this logic isn't really different here
            System.out.println("Scan started...");
            loadParentThreads();
        } else {
            //parent process server logic
            System.out.println("Server Started on port " + _config.getMultiProcessPort() + "...");
            ServerSocket serverSocket = null;
            Socket socket = null;

            try {
                serverSocket = new ServerSocket( _config.getMultiProcessPort() );
            } catch (IOException e) {
                e.printStackTrace();
    
            }

            // Eternally restart the immortal SocketServer.
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println("I/O error: " + e);
                }

                _config.setOutputFilePath(this._fileLogger.getFileName());
                new SocketServer(socket, _files, _config).startThread();
            }
        }
    }

    private void loadParentThreads() {
        //Request file
        Client c = new Client();
        boolean conn = c.startConnection("localhost", _config.getMultiProcessPort());
        boolean morefiles = true;
        if(conn) {
            this._processedLogger = new FileLogger(_config, c.getProcessedLogFile());
            this._fileLogger = new FileLogger(_config, c.getLogFile());
        } else {
            morefiles = false;
        }

        int cntr = 0;
        while(conn && cntr < _config.getScanLimit()) {
            cntr++;
            SWFFile f = c.getFile();
            if(f == null) {
                f = c.getFile();
                morefiles = false;
                break;
            }
            try {
                //scan the file...
                SWFDecompiler sd = new SWFDecompiler(f);
                SWFFile newFile = sd.scanFile(_config.getPcode());

                //Append the output file...
                int totalRank = newFile.getTotalRank(_config.getTermData());
                if(totalRank > 0) {
                    this._fileLogger.logFile(newFile.getPath(), totalRank, newFile.getCountsByTerm());
                    System.out.println("Rank  = " + totalRank);
                    System.out.println("Terms = " + newFile.getCountsByTerm());
                } else {
                    //Optionally output single asset games.
                }

                //System.out.println(f.getPath());
                if(this._processedLogger != null) {
                    this._processedLogger.log(f.getPath());
                    System.out.println("");
                }

            } catch (Exception e) {
                //catch exception
                System.out.println("Could not decompile due to " + e);
                //TODO: log failure here...

                if(this._processedLogger != null) {
                    this._processedLogger.log(f.getPath());
                    System.out.println("");
                }
            }
        }

        System.out.println("moreFiles=" + morefiles);
        if(morefiles) {
            // TODO: make this not windows-specific.
            //TODO: create a _config.generateCMDFlags() to do the work below.
            try {
                System.out.println("Kicking off next console...");
                String cmd = "/c start cmd /K run.bat -subprocess -lpf -port " + _config.getMultiProcessPort() + " -scanLimit " + _config.getScanLimit();
                System.out.println(cmd);
                Runtime.getRuntime().exec(new String[] {"cmd", cmd}); 
            } catch (IOException ioe) {
                System.out.println("CMD error: " + ioe);
            }
        }

        c.stopConnection();
        System.exit(1);
    }

    private void loadLocalThreads() {
        loadThreads();
    }

    private class SWFTaskListener implements TaskListener {
        private SWFConfig _config;
    
        public SWFTaskListener(SWFConfig c) {
            _config = c;
        }
    
        public void threadComplete( SWFThread runner) {
            SWFTaskListener stl = new SWFTaskListener(_config);
            System.out.println("Thread Died: " + runner.getNumber());
            if(_queue.size() > 0) {
                System.out.println("Restarting Thread: " + runner.getNumber());
                SWFThread st = getRunnable(_config, runner.getNumber());
                st.addListener(stl);
                _pool.execute(st);
            } 
        }
    }

    private void loadThreads() {
        SWFTaskListener stl = new SWFTaskListener(_config);
        _queue = new ArrayBlockingQueue<SWFFile>(_files.size());
        _queue.addAll(_files);

        for(int i = 1; i <= _config.getThreadCount(); i++) {
            System.out.println("Start Thread 1");
            SWFThread st = getRunnable(_config, i);
            st.addListener(stl);
            _pool.execute(st);
        }
    }

    private SWFThread getRunnable(SWFConfig c, Integer num) {
        // Make a new SWFThread instance that has the doWork function filled in.
        return new SWFThread(c, num) {
            // A filled-in doWork function.
            public void doWork(SWFConfig c) {
                // Declare and initialize the SWFFile.
                SWFFile f = null;
                // Count tracks the number of files scanned.
                Integer count = 0;
                System.out.println("Started thread: " + num);
                // While we are below the scan limit and there are still more to scan,
                // TODO: thread safety?
                while(count < _config.getScanLimit() && (f = _queue.poll()) != null) {
                    // Increment the count.
                    count++;
                    try {
                        // Decompile and scan the file.
                        SWFDecompiler d = new SWFDecompiler(f);
                        d.scanFile(c.getPcode());
                    } catch (Exception e) {
                        //TODO: exception stuffs
                    }

                    System.out.println(f);
                }
                return;
            }
        };
    }

    /**
     * Recursively index non-ignored filenames that end with a given extension in startDir, up to a maximum depth.
     * @param startDir The directory to search in.
     * @param ext The extension that the files should have.
     * @param num The number of files already indexed by previous tasks.
     * @param depth Our current depth in the filetree.
     * @param maxDepth The maximum allowed depth in the filetree.
     * @return The total number of files indexed.
     * @author krum110487
     */
    // TODO: make a prettier non-recursive wrapper for this.
    private Integer recurseForFiles(File startDir, String ext, Integer num, Integer depth, Integer maxDepth) {
        // The number of files we have picked up so far.
        Integer n = num;
        // The depth that we're currently at.
        Integer d = depth;
        try {
            // List the files in the starting dir.
            File[] files = startDir.listFiles();
            // For each file in startDir,
			for (File file : files) {
                // If the file is a directory and we either lack a recursion limit or we haven't reached it yet,
				if (file.isDirectory() && (maxDepth < 0 || maxDepth >= d+1)) {
                    // Recurse on that directory, adding one to the depth.
                    // Set the output of that to n.
                    // TODO: can we make this async, and await it at the end?
                    n = recurseForFiles(file, ext, n, d+1, maxDepth);
				} else {
                    // If the file extension matches the extension that we're searching for,
					if(getFileExtension(file).equals(ext)) {
                        // Get the file's absolute, symlink-resolved path.
                        String fileStr = file.getCanonicalPath();
                        // By default, don't ignore the file.
                        Boolean fileIgnore = false;
                        try {
                            // Try to check if the filename is in the list of ignored ones.
                            fileIgnore = _ignoreList.contains(fileStr);
                        } catch (Exception e) {
                            // Do nothing, list was empty.
                        }

                        // Are we ignoring the file?
                        if(!fileIgnore) {
                            // No, we open it and include it. TODO: can we make the actual opening be lazy?
                            // Also, increment the file counter. Give the file this as its number.
                            _files.add(new SWFFile(fileStr, ++n));
                            //System.out.println("File " + n + ": " + fileStr);
                        } else {
                            // Yes, we're ignoring it. Increment the relevant counter.
                            this._ignoreCount++;
                            //System.out.println("File Ignored: " + fileStr);
                        }
                    }
				}
            }
		} catch (Exception e) {
            // Whoops, we hit an error.
            System.out.println("Recurse error " + e);
			e.printStackTrace();
        }

        // Return the number of files that we got.
        return n;
    }

    /**
     * Gets the file extension from a filepath.
     * @param file The file that is of interest.
     * @return The file's extension.
     * @author krum110487
     */
    private String getFileExtension(File file) {
        // Get the file's name, including the extension.
        String fileName = file.getName();
        // If the filename contains '.',
        if(fileName.lastIndexOf(".") > 0) {
            // Everything from the last dot until the end is the extension.
            return fileName.substring(fileName.lastIndexOf(".")+1);
        }
        // The file has no extension.
        else return "";
    }
}