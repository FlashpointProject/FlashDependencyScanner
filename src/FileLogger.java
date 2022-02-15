package src;

import java.io.*;
import java.util.*;
import java.text.*;
import src.swf.*;

public class FileLogger {
    private String _fileName;
    private File _file;
    private SWFConfig _config;

    public FileLogger(SWFConfig config, String fileName) {
        this._fileName = resolveName(fileName);
        this._file = new File(fileName);
        this._config = config;
    }

    private String resolveName(String name) {
        //TODO: update to take the bracketed date and use it below.
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm");
        Date date = new Date();
        return name.replace("<yyyy-MM-dd_HH_mm>", dateFormat.format(date));
    }

    public String getFileName() {
        return this._fileName;
    }

    public void createFile(Boolean overwrite) {
        if(overwrite && _file.exists()) {
            delFile();
            this._file = new File(this._fileName);
        }

        if(!_file.exists()){
            this._file = new File(this._fileName);
        }
    }

    public void delFile() {
        try
        {
            File file = new File(this._fileName);
            if(file.exists()){
                file.delete();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void logFile(String path, Integer rank, String details) {
        PrintWriter csvWriter;
        try
        {
            createFile(false);
            csvWriter = new  PrintWriter(new FileWriter(this._file, true));
            SWFConfig.OutputDetail dl = this._config.getOutputDetailLevel();

            if(dl == SWFConfig.OutputDetail.FULL_DUMP) {
                csvWriter.println("\"" + path + "\"," + rank + ",\"" + details + "\"");
            }
            else if(dl == SWFConfig.OutputDetail.PATH_AND_RANK) {
                csvWriter.println("\"" + path+"\"," + rank);
            } else {
                csvWriter.println("\"" + path+"\"");
            }
            csvWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        PrintWriter csvWriter;
        try
        {
            createFile(false);
            csvWriter = new  PrintWriter(new FileWriter(this._file, true));

            csvWriter.println(message);
            csvWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}