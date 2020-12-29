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
    private Set<String> _ignoreList;
    private BlockingQueue<SWFFile> _queue;
    private FileLogger _fileLogger;
    //private SWFProcessHost _ph;
    //private SWFProcessClient _pc;
    public static ExecutorService _pool;

    public SWFScanner(SWFConfig c) {
        this._config = c;
        this._fileLogger = new FileLogger(c.getOutputFilePath());
        this._files = new ArrayList<SWFFile>();
        this._pool = Executors.newFixedThreadPool(c.getThreadCount());
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
        //load the ignore list file into a string.
        //_config.getIgnoreListPath()
        //load the hashes into memory for checking
        if(!_config.getIsSubProcess()) {
            //Only do this if the this is NOT the sub-process
        }
    }

    private void loadFiles() {
        System.out.println("Loading Files...");
        if(!_config.getIsSubProcess()) {
            //Main Process logic, load like normal.
            if(!_config.getFileListPath().equals("")) {
                //Ignore SSF
                //Load the file and build the list.
            }
            else if(!_config.getSourcePath().equals("")) {
                if(_config.getSSF()) {
                    recurseForFiles(new File(_config.getSourcePath()), "swf", 0);
                } else {
                    _files.add(new SWFFile(_config.getSourcePath(),0));
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

        System.out.println("Files loaded with " + _files.size() + " files.");
    }

    private void scanFiles() {
        //For each of the files found, do stuff.
        System.out.println(_config);
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
            System.out.println("Server Started on port " + _config.getMultiProcessPort());
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

                new SocketServer(socket, _files, this._fileLogger.getFileName()).startThread();
            }
        }
    }

    private void loadParentThreads() {
        //Request file
        Client c = new Client();
        boolean conn = c.startConnection("localhost", _config.getMultiProcessPort());

        String logFile = c.getLogFile();
        this._fileLogger = new FileLogger(logFile);
        
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
                    System.out.println(newFile.getTotalRank());
                } else {
                    //TODO: seperate file... Scan completed, but nothing was found
                }
            } catch (Exception e) {
                //catch exception
            }
        }
        c.stopConnection();
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

    private Integer recurseForFiles(File startDir, String ext, Integer num) {
        //Add the files to the queue
        Integer n = num;
        try {
            File[] files = startDir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
                    n = recurseForFiles(file, ext, n);
				} else {
					if(getFileExtension(file).equals(ext)) {
                        _files.add(new SWFFile(file.getCanonicalPath(), ++n));
                        System.out.println("File " + n + ": " + file.getCanonicalPath());
                    }
				}
            }
		} catch (Exception e) {
            System.out.println(e);
			e.printStackTrace();
        }

        return n;
    }

    /*
    private void scanFiles() {
        //loop through each of the files, load them and check the hash to see if it matches the ignorelist.
        for(int x = 0; x < _files.size()-1; x++) {
            BufferedInputStream f = new BufferedInputStream(new FileInputStream(_files[x]));
            if(!_ignoreList.contains(getHash(f))) {
                //DO the decompilation
            } else {
                //Ignore the file, o
            }
        }
    }
    */

    /*
    private String getHash(BufferedInputStream bis) {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();
    
        byte[] hash = digest.digest();
        return Base64.getEncoder.encodeToString(hash);
    }
    */

    private String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") > 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }
}