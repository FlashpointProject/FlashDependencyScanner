package src.swf;

import java.util.*;
import java.util.regex.*;

public class SWFTerms {
    private Map<String, Integer> _termRate;
    private List<String> _termRequired;

    //TODO: make this use regex for a "fast vs more accurate vs most accurate"
    public SWFTerms() {
        _termRequired = new ArrayList<String>();
        _termRate = new HashMap<String, Integer>();

        _termRequired.add("URLRequest");
        _termRequired.add("URLLoader");
        _termRequired.add("LoadMovie");
        _termRequired.add("LoadMovieNum");
        _termRequired.add("URLStream");
        _termRequired.add("LoadVariables");
        _termRequired.add("LoadVariablesNum");
        _termRequired.add("AVURLLoader");
        _termRequired.add("NetStream");

        _termRate.put("URLRequest", 0);
        _termRate.put("URLLoader", 0);
        _termRate.put("LoadMovie", 0);
        _termRate.put("LoadMovieNum", 0);
        _termRate.put("URLStream", 0);
        _termRate.put("LoadVariables", 0);
        _termRate.put("LoadVariablesNum", 0);
        _termRate.put("AVURLLoader", 0);
        _termRate.put("NetStream", 0);
        addTermRateExt(".png", 100);
        addTermRateExt(".xml", 90);
        addTermRateExt(".jpg", 90);
        addTermRateExt(".swf", 80);
        addTermRateExt(".txt", 70);
        addTermRateExt(".mp3", 40);
        addTermRateExt(".bin", 30);
        addTermRateExt(".gif", 30);
        addTermRateExt(".ogg", 20);
        addTermRateExt(".json", 20);
        addTermRateExt(".mtl", 20);
        addTermRateExt(".obj", 20);
        addTermRateExt(".flv", 20);
        addTermRateExt(".zlib", 20);
        addTermRateExt(".aas", 20);
        addTermRateExt(".au", 20);
        addTermRateExt(".settings", 20);
        addTermRateExt(".rpgmvp", 20);
        addTermRateExt(".m4a", 20);
        addTermRateExt(".w3d", 20);
        addTermRateExt(".cct", 20);
        addTermRateExt(".wav", 20);
        addTermRateExt(".webp", 10);
        addTermRateExt(".cxml", 10);
        addTermRateExt(".rpgmvo", 10);
        addTermRateExt(".MOD", 10);
        addTermRateExt(".lua", 10);
        addTermRateExt(".dat", 10);
        addTermRateExt(".resource", 10);
        addTermRateExt(".lvl", 10);
        addTermRateExt(".webm", 10);
        addTermRateExt(".bmp", 10);
        addTermRateExt(".f4v", 10);
        addTermRateExt(".mp4", 10);
        addTermRateExt(".fnt", 10);
        addTermRateExt(".ts", 9);
        addTermRateExt(".dae", 8);
        addTermRateExt(".csv", 7);
        addTermRateExt(".vmo", 6);
        addTermRateExt(".asasm", 5);
        addTermRateExt(".php", 5);
        addTermRateExt(".htm", 5);
        addTermRateExt(".html", 5);
        addTermRateExt(".asp", 5);
        addTermRateExt(".ttf", 4);
        addTermRateExt(".hx", 3);;
        addTermRateExt(".properties", 2);
        addTermRateExt(".db", 1);
    }

    public void addTermRateExt(String term, Integer rank) {
        _termRate.put(term + "\"", rank);
        _termRate.put(term + "'", rank);
        _termRate.put(term + "?", rank);
    }

    public int getTermRating(String term) {
        if(_termRate.get(term) == null) { return 0; }
        return _termRate.get(term);
    }

    public boolean isTermRequired(String term) {
        return _termRequired.contains(term);
    }

    public Map<String, Integer> getTermRateList() {
        return _termRate;
    }

    public List<String> getTermRequiredList() {
        return _termRequired;
    }
}