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
        this._fileLogger = new FileLogger(c.getOutputFilePath());
        this._files = new ArrayList<SWFFile>();
        this._pool = Executors.newFixedThreadPool(c.getThreadCount());
        this._ignoreCount = 0;
        this._fileLogger = new FileLogger(c.getOutputFilePath());
        this._ignoreList = new HashSet<String>();

        this._processedLogger = null;
        if(c.getProcessedListPath() != "") {
            this._processedLogger = new FileLogger(c.getProcessedListPath());
        }
    }

    public void scan() {
        if(!_config.getIsSubProcess()) {
            loadIgnoreList();
            loadFiles();
            scanFiles();
        } else {
            scanFiles();
        }
    }

    private void loadIgnoreList() {
        if(!_config.getIsSubProcess()) {
            //Only do this if the this is NOT the sub-process
            try {
                Scanner s = new Scanner(new File(this._config.getIgnoreListPath()));
                while (s.hasNextLine()){
                    _ignoreList.add(s.nextLine());
                }
                s.close();
            } catch (FileNotFoundException fnf) {
                fnf.printStackTrace();
            }
        }
    }

    private void loadFiles() {
        //System.out.println("Loading Files...");
        if(!_config.getIsSubProcess()) {
            //Main Process logic, load like normal.
            if(!_config.getFileListPath().equals("")) {
                //TODO: implement loading list from a file.
            }
            else if(!_config.getSourcePath().equals("")) {
                if(_config.getSSF()) {
                    recurseForFiles(new File(_config.getSourcePath()), "swf", 0, 0, -1);
                } else {
                    recurseForFiles(new File(_config.getSourcePath()), "swf", 0, 0, 0);
                }
            }
            else {
                //throw new Exception("No file supplied, you must include at least --fileList OR a file as the last paramater.");
            }
        } else {
            //Sub Process logic, load from sockets.
            int offset = _config.getSubProcessOffset();
            int limit = _config.getScanLimit();
        }

        System.out.println(_files.size() + " files loaded successfully.");
        System.out.println(this._ignoreCount + " files ignored.\n");
    }

    private void scanFiles() {
        //For each of the files found, do stuff.
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
        if(conn) {
            this._processedLogger = new FileLogger(c.getProcessedLogFile());
            this._fileLogger = new FileLogger(c.getLogFile());
        }

        int cntr = 0;
        while(conn && cntr < _config.getScanLimit()) {
            cntr++;
            SWFFile f = c.getFile();
            if(f == null) break;
            try {
                //scan the file...
                SWFDecompiler sd = new SWFDecompiler(f);
                SWFFile newFile = sd.scanFile(_config.getPcode());

                //Append the output file...
                if(newFile.getTotalRank() > 0) {
                    this._fileLogger.logFile(newFile.getPath(), newFile.getTotalRank());
                    System.out.println("Rank = " + newFile.getTotalRank());
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
                System.out.println("Could not decompile");
                //TODO: log failure here...

                if(this._processedLogger != null) {
                    this._processedLogger.log(f.getPath());
                    System.out.println("");
                }
            }
        }
        //TODO: open a new command widnow here if there are still files left to get
        //TODO: implement a socket command to get the count left.

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
        return new SWFThread(c, num) {
            public void doWork(SWFConfig c) {
                SWFFile f = null;
                Integer count = 0;
                System.out.println("Started thread: " + num);
                while(count < _config.getScanLimit() && (f = _queue.poll()) != null) {
                    count++;
                    try {
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

    private Integer recurseForFiles(File startDir, String ext, Integer num, Integer depth, Integer maxDepth) {
        //Add the files to the queue
        Integer n = num;
        Integer d = depth;
        try {
            File[] files = startDir.listFiles();
			for (File file : files) {
				if (file.isDirectory() && (maxDepth < 0 || maxDepth >= d+1)) {
                    n = recurseForFiles(file, ext, n, d+1, maxDepth);
				} else {
					if(getFileExtension(file).equals(ext)) {
                        String fileStr = file.getCanonicalPath();
                        Boolean fileIgnore = false;
                        try {
                            fileIgnore = _ignoreList.contains(fileStr);
                        } catch (Exception e) {
                            //Do nothing, list was empty.
                        }

                        if(!fileIgnore) {
                            _files.add(new SWFFile(fileStr, ++n));
                            //System.out.println("File " + n + ": " + fileStr);
                        } else {
                            this._ignoreCount++;
                            //System.out.println("File Ignored: " + fileStr);
                        }
                    }
				}
            }
		} catch (Exception e) {
            System.out.println("Recurse error " + e);
			e.printStackTrace();
        }

        return n;
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") > 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }
}