package src.swf;

import java.util.*;

public class SWFConfig {
    public static enum OutputDetail {
        PATH_ONLY,
        PATH_AND_RANK,
        FULL_DUMP
    }

    private String _configPath = "";
    private String _outputFilePath = "AtRiskFiles_<yyyy-MM-dd_HH_mm>.csv";
    private int _threadCount = 1;
    private int _processCount = 1;
    private int _processPort = 11235;
    private boolean _parallelSpeedUp = false;
    private int _scanLimit = -1;
    private boolean _pcode = true;
    private boolean _pcodeFlag = false;
    private boolean _ascriptFlag = false;
    private boolean _ssf = false;
    private OutputDetail _outputDetailLevel = OutputDetail.FULL_DUMP;
    private boolean _logProcessedFiles = true;
    private String _fileList = "";
    private String _processedList = "processedFiles.csv";
    private String _ignoreList = "processedFiles.csv";
    private int _rankMin = 1;
    private boolean _cliMessaging = true;
    private String _sourcePath = "";

    //Sub process only variables
    private boolean _isSubProcess = false;
    private int _subProcessOffset = 0;
    private SWFTerms _termData;

    public SWFConfig() {
        //Convert the default pattern, in case one is not sent in.
        this.setOutputFilePath(this._outputFilePath);
    }

    @Override
    public String toString() {
        String co = "Config Settings:\n";

        co += "\t--config = "            + this.getConfigPath()         + "\n";
        co += "\t--searchSubFolders = "  + this.getSSF()                + "\n";
        co += "\t--threads = "           + this.getThreadCount()        + "\n";
        co += "\t--multi-processes = "   + this.getMultiProcessCount()  + "\n";
        co += "\t--parallelSpeedUp = "   + this.getParallelSpeedUp()    + "\n";
        co += "\t--scanLimit = "         + this.getScanLimit()          + "\n";
        co += "\t--pcode = "             + this.getPcode()              + "\n";
        co += "\t--ascript = "           + !this.getPcode()             + "\n";
        co += "\t--detail = "            + this.getOutputDetailLevel()  + "\n";
        co += "\t--logProcessedFiles = " + this.getLogProcessedFiles()  + "\n";
        co += "\t--fileList = "          + this.getFileListPath()       + "\n";
        co += "\t--processedListPath = " + this.getIgnoreListPath()     + "\n";
        co += "\t--ignoreList = "        + this.getIgnoreListPath()     + "\n";
        co += "\t--rankMin = "           + this.getRankMin()            + "\n";
        co += "\t--CLIMessaging = "      + this.getCLIMessagingFlag()   + "\n";
        co += "\t--outputFile = "        + this.getOutputFilePath()     + "\n";
        co += "\t--subProcess = "        + this.getIsSubProcess()       + "\n";
        co += "\t--spOffset = "          + this.getSubProcessOffset()   + "\n";
        co += "\tsourcePath = "          + this.getSourcePath()         + "\n";

        return co;
    }

    public static SWFConfig ParseCLI(String[] args) {
        //parse CLI to get everything...
        SWFConfig c = new SWFConfig();

        int argLen = 0;
        while(argLen <= args.length-1) {
            //The last argument is the path to use.            
            switch(args[argLen].toLowerCase()) {
                case "help":
                case "-help":
                case "--help":
                case "?":
                case "-?":
                case "--?":
                    displayHelp();
                    break;
                case "generatehashlist":
                    //generateHashList(args[++argLen]);
                    //argLen = args.length-1;
                    break;
                case "-config": 
                case "--config":
                    c.setConfigPath(args[++argLen]);
                    c.parseAndLoadConfig();
                    argLen = args.length-1;
                    break;
                case "-searchsubfolders":
                case "--searchsubfolders":
                case "-ssf":
                case "--ssf":
                    c.setSSF(true);
                    break;
                case "-scanlimit":
                case "--scanlimit":
                case "-sl":
                case "--sl":
                    c.setScanLimit(args[++argLen]);
                    break;
                case "-threads":
                case "--threads":
                case "-t":
                case "--t":
                    c.setThreadCount(args[++argLen]);
                    break;
                case "-multi-process":
                case "--multi-process":
                case "-mp":
                case "--mp":
                    c.setMultiProcessCount(args[++argLen]);
                    c.setMultiProcessPort(args[++argLen]);
                    break;
                case "-port":
                case "--port":
                    c.setMultiProcessPort(args[++argLen]);
                case "-parallelspeedup":
                case "--parallelspeedup":
                case "-psu":
                case "--psu":
                    c.setParallelSpeedUp(true);
                    break;
                case "-pcode":
                case "--pcode":
                    c._pcodeFlag = true;
                    c.setPcode(true);
                    break;
                case "-ascript":
                case "--ascript":
                    c._ascriptFlag = true;
                    c.setPcode(false);
                    break;
                case "-detail":
                case "--detail":
                case "-d":
                case "--d":
                    c.setOutputDetailLevel(args[++argLen]);
                    break;
                case "-logprocessedfiles":
                case "--logprocessedfiles":
                case "-lpf":
                case "--lpf":
                    c.setLogProcessedFiles(true);
                    break;
                case "-filelist":
                case "--filelist":
                case "-fl":
                case "--fl":
                    c.setFileListPath(args[++argLen]);
                    break;
                case "-processedlistpath":
                case "--processedlistpath":
                case "-plp":
                case "--plp":
                    c.setProcessedListPath(args[++argLen]);
                    break;
                case "-ignorelist":
                case "--ignorelist":
                case "-il":
                case "--il":
                    c.setIgnoreListPath(args[++argLen]);
                    break;
                case "-rankmin":
                case "--rankmin":
                case "-rmin":
                case "--rmin":
                    c.setRankMin(args[++argLen]);
                    break;
                case "-climessaging":
                case "--climessaging":
                case "-clim":
                case "--clim":
                    c.setCLIMessagingFlag(true);
                    break;
                case "-outputfile":
                case "--outputfile":
                case "-of":
                case "--of":
                    c.setOutputFilePath(args[++argLen]);
                    break;
                //unlisted commands for sub-process setup.
                case "-subprocess":
                case "--subprocess":
                    c.setIsSubProcess(true);
                case "-spoffset":
                case "--spoffset":
                    c.setSubProcessOffset(args[argLen]);
                default:
                    c.setSourcePath(args[argLen]); 
                    break;    
            }

            argLen++;
        }

        return c;
    }

    static SWFConfig getConfig(String configPath) {
        SWFConfig c = new SWFConfig();
        c.setConfigPath(configPath);

        //Parse file using this._configPath;
        c.parseAndLoadConfig();

        return c;
    }

    private static void displayHelp() {
        //TODO: show the CLI help...

        System.exit(0);
    }

    //////////////////////////////////////////////////////////
    //----------- Getter and Setter Functions --------------//
    //////////////////////////////////////////////////////////
    public String getConfigPath() { return this._configPath; }
    public void setConfigPath(String path) {
        if(isFile(path)) {
            this._configPath = path;
        } else {
            //TODO: Throw error
        }
    }

    public String getSourcePath() { return this._sourcePath; }
    public void setSourcePath(String path) {
        if(isFolderOrFile(path)) {
            this._sourcePath = path;
        } else {
            //TODO: Throw Error
        }
    }

    public String getOutputFilePath() { return this._outputFilePath; }
    public void setOutputFilePath(String filePath) {
        //TODO check if path given is valid folder, and file is valid to be created.
        //Find <>, take the pattern and convert current date to that format and replace everything
        //between < and > with the result.
        this._outputFilePath = filePath;
    }

    public int getMultiProcessCount() { return this._processCount; }
    public void setMultiProcessCount(int count) {
        if(count > 0) {
            if(count > 50) { count = 50; }
            this._processCount = count;
        }
    }
    public void setMultiProcessCount(String count) {
        try {
            int val = Integer.parseInt(count);
            this.setMultiProcessCount(val);
        } catch (NumberFormatException nfe) {
            System.out.println("The flag \"multi-processor\" (-mp | -multi-processor) was used, but the value used for it was not a number.");
            System.out.println("The correct usage is \"-mp <NUMBER>\" e.g. \"-mp 50 11235\"");
            System.out.println("This flag will set the number of processes to be triggered for the process to use, default is 1 and 11235");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMultiProcessPort() { return this._processPort; }
    public void setMultiProcessPort(int port) {
        if(port <= 255) {
            this._processPort = 11235;
        } else {
            this._processPort = port;
        }
    }
    public void setMultiProcessPort(String port) {
        try {
            int val = Integer.parseInt(port);
            this.setMultiProcessPort(val);
        } catch (NumberFormatException nfe) {
            System.out.println("The flag \"multi-processor\" (-mp | -multi-processor) was used, but the value used for the port was not a number.");
            System.out.println("The correct usage is \"-mp <NUMBER> <PORT>\" e.g. \"-mp 50 11235\"");
            System.out.println("This flag will set the number of processes to be triggered for the process to use, default is 1 and 11235");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getScanLimit()           { return this._scanLimit;  }
    public void setScanLimit(int count) { this._scanLimit = count; }
    public void setScanLimit(String count) {
        try {
            int val = Integer.parseInt(count);
            this.setScanLimit(val);
        } catch (NumberFormatException nfe) {
            System.out.println("The flag \"scanLimit\" (-sl | -scanLimit) was used, but the value used for it was not a number.");
            System.out.println("The correct usage is \"-pl <NUMBER>\" e.g. \"-pl 50\"");
            System.out.println("This flag will set the scanLimit will determine the amount of items a process can use at a time before it closes and a new process spawns, default is -1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getSubProcessOffset()           { return this._subProcessOffset;  }
    public void setSubProcessOffset(int count) { this._subProcessOffset = count; }
    public void setSubProcessOffset(String count) {
        try {
            int val = Integer.parseInt(count);
            this.setSubProcessOffset(val);
        } catch (NumberFormatException nfe) {
            System.out.println("setSubProcessOffset is not a number, fix it.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int getThreadCount() { return this._threadCount; }
    public void setThreadCount(int count) {
        if(count > 0) {
            if(count > 50) { count = 50; }
            this._threadCount = count;
        }
    }
    public void setThreadCount(String count) {
        try {
            int val = Integer.parseInt(count);
            this.setThreadCount(val);
        } catch (NumberFormatException nfe) {
            System.out.println("The flag \"threads\" (-t | -threads) was used, but the value used for it was not a number.");
            System.out.println("The correct usage is \"-t <NUMBER>\" e.g. \"-t 50\"");
            System.out.println("This flag will set the number of threads for the process to use, default is 1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getParallelSpeedUp() { return this._parallelSpeedUp; }
    public void setParallelSpeedUp(boolean flag) { this._parallelSpeedUp = flag; }

    public String getFileListPath() { return this._fileList; }
    public void setFileListPath(String fileListPath) {
        if(isFile(fileListPath)) {
            this._fileList = fileListPath;
        } else {
            //TODO: Throw error
        }
    }

    public String getProcessedListPath() { return this._processedList; }
    public void setProcessedListPath(String processedListPath) {
        if(isFile(processedListPath)) {
            this._processedList = processedListPath;
        } else {
            //TODO: Throw error.
        }
    }

    public String getIgnoreListPath() { return this._ignoreList; }
    public void setIgnoreListPath(String ignoreListPath) {
        if(isFile(ignoreListPath)) {
            this._ignoreList = ignoreListPath;
        } else {
            //TODO: Throw error.
        }
    }

    public boolean getPcode() { return this._pcode;     }
    public void setPcode(boolean usePcode) { 
        if(this._pcodeFlag && this._ascriptFlag) { 
            System.out.println("Warning: using both -pcode and -ascript will result in the last flag in the command to be used.");
        }
        this._pcode = usePcode; 
    }

    public boolean getSSF()         { return this._ssf; }
    public void setSSF(boolean ssf) { this._ssf = ssf;  }

    public OutputDetail getOutputDetailLevel()        { return this._outputDetailLevel; }
    public void setOutputDetailLevel(OutputDetail od) { this._outputDetailLevel = od;   }
    public void setOutputDetailLevel(String od) { 
        switch(od.toLowerCase()) {
            case "pathonly":
                setOutputDetailLevel(OutputDetail.PATH_ONLY);
                break;
            case "pathandrank":
                setOutputDetailLevel(OutputDetail.PATH_AND_RANK);
                break;
            default:
                setOutputDetailLevel(OutputDetail.FULL_DUMP);
                break;
        }
    }

    public int getRankMin()             { return this._rankMin;                                     }
    public void setRankMin(int minRank) { if(minRank < 0) { minRank = 0; } this._rankMin = minRank; }
    public void setRankMin(String minRank) { 
        try {
            int val = Integer.parseInt(minRank);
            this.setRankMin(val);
        } catch (NumberFormatException nfe) {
            System.out.println("The flag \"rankMin\" (-rmin | -rankMin) was used, but the value used for it was not a number.");
            System.out.println("The correct usage is \"-rmin <NUMBER>\" e.g. \"-rmin 104\"");
            System.out.println("This flag will set the number of threads for the process to use, default is 1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getLogProcessedFiles()          { return this._logProcessedFiles; }
    public void setLogProcessedFiles(boolean flag) { this._logProcessedFiles = flag; }

    public boolean getCLIMessagingFlag()          { return this._cliMessaging; }
    public void setCLIMessagingFlag(boolean flag) { this._cliMessaging = flag; }

    public boolean getIsSubProcess()          { return this._isSubProcess; }
    public void setIsSubProcess(boolean flag) { this._isSubProcess = flag; }

    public SWFTerms getTermData()          { return this._termData; }
    public void setTermData(String path) {
        //TODO: path is unused as the file does not exist yet. 
        this._termData = new SWFTerms();
    }

    //////////////////////////////////////////////////////////
    //------------ Private Helper Functions ----------------//
    //////////////////////////////////////////////////////////
    private void parseAndLoadConfig() {
        //Use this._configPath and parse it accordingly...
        //Set all of the values based upon it.
    }

    private boolean isFolder(String path) {
        //TODO: setup logic to check for a valid folder.
        return true;
    }

    private boolean isFile(String path) {
        //TODO: setup logic to check for a valid file.
        return true;
    }

    private boolean isFolderOrFile(String path) {
        //TODO: setup logic to check for a valid folder/file.
        return true;
    }
}