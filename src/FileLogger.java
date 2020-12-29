package src;

import java.io.*;
import java.util.*;
import java.text.*;

public class FileLogger {
    private String _fileName;
    private File _file;

    public FileLogger(String fileName) {
        this._fileName = resolveName(fileName);
        this._file = new File(fileName);
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

    public void logFile(String path, Integer rank) {
        PrintWriter csvWriter;
        try
        {
            createFile(false);
            csvWriter = new  PrintWriter(new FileWriter(this._file, true));

            csvWriter.println("\"" + path+"\"," + rank);
            csvWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}