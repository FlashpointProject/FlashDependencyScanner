package src.swf;

import java.util.*;
import java.nio.file.*;
import com.google.gson.*;

public class SWFFile {
    private String _path = "";
    private Integer _number = -1;
    private String _logResults = "";
    private Map<String,Integer> _terms = new HashMap<String,Integer>();
    private boolean _decompErrorFlag = false;

    public SWFFile(String path, Integer number) {
        this._path = path;
        this._number = number;
    }

    //////////////////////////////////////////////////////////
    //----------- Getter and Setter Functions --------------//
    //////////////////////////////////////////////////////////
    public String getPath() { return this._path; }
    public void setPath(String path) throws Exception {
        Path p = Paths.get(path);
        if(Files.exists(p)) {
            this._path = path;
        } else {
            if(Files.isRegularFile(p)) {
                throw new Exception("The file path \"" + path + "\" is either locked, or could not be found");
            } else {
                throw new Exception("The directory \"" + path + "\" cannot be found.");
            }
        }
    }

    @Override
    public String toString() {
        String co = "File " + String.format ("%06d",this.getNumber()) + "\t";
        co += this.getPath();
        return co;
    }

    public String toJSON() {
        String json = "";
        try {
            SWFJson s = new SWFJson();
            s.path = this.getPath();
            s.number = this.getNumber();

            Gson gson = new Gson();
            json = gson.toJson(s);
        } catch (Exception e) {
            System.out.println(e);
        }
        return json;
    }

    public Map<String, Integer> getTerm() { return this._terms; }
    public Integer getTermCount(String term) { 
        Integer cnt = this._terms.get(term);
        if(cnt == null) cnt = 0;
        return cnt;
    }
    public void addTerm(String term) { 
        Integer cnt = this._terms.get(term);
        if(cnt == null) {
            this._terms.put(term, 1);
        } else {
            this._terms.put(term, ++cnt);
        }
    }

    public Integer getNumber()         { return this._number; }
    public void setNumber(Integer num) { this._number = num; }

    public void setFoundRequiredFunc() {
        this._terms.put("foundReqFunc", 1);
    }

    public Integer getTotalRank(SWFTerms termRnk) { 
        Integer rank = 0;
        for(String term : _terms.keySet()) {
            if(term == "foundReqFunc") {
                rank++;
            } else {
                rank += getTermCount(term) * termRnk.getTermRating(term);
            }
        }
        return rank;
    }

    public String getCountsByTerm() {
        String results = "";
        for(String term : _terms.keySet()) {
            if(term == "foundReqFunc") {
                //Show nothing??
            } else {
                if(term.startsWith(".")) {
                    String newTerm = term.replace(".", "").replace("\"", "").replace("'", "").replace("?", "");
                    results += newTerm + "=" + getTermCount(term) + ";";
                }
            }
        }

        return results;
    }

    public String getLogResults()             { return this._logResults;    }
    public void setLogResults(String results) { this._logResults = results; }

    public boolean getDecompErrorFlag()          { return this._decompErrorFlag; }
    public void setDecompErrorFlag(boolean flag) { this._decompErrorFlag = flag; }

    //////////////////////////////////////////////////////////
    //------------ Private Helper Functions ----------------//
    //////////////////////////////////////////////////////////
    private void parseAndLoadConfig() {
        //Use this._configPath and parse it accordingly...
        //Set all of the values based upon it.
    }
}