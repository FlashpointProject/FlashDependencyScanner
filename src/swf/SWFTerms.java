package src.swf;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class SWFTerms {
    private List<String> _termRate;
    private List<String> _termRequired;

    //TODO: make this use regex for a "fast vs more accurate vs most accurate"
    public SWFTerms(String jsonString) {
        // Init the arrays to empty.
        _termRequired = new ArrayList<String>();
        _termRate = new ArrayList<String>();
        
        // Convert the jsonString to a JSONObject.
        JSONObject terms = new JSONObject(jsonString);
        // Get the three relevant arrays.
        /*JSONArray requiredTerms = terms.getJSONArray("requiredTerms");
        for (int i = 0; i < requiredTerms.length(); i++) {
            _termRequired.add(requiredTerms.getString(i));
        }*/
        for (Object element : terms.getJSONArray("requiredTerms").toList()) {
            _termRequired.add((String)element);
        }
        for (Object element : terms.getJSONArray("hitTerms").toList()) {
            _termRate.add((String)element);
        }
        for (Object element : terms.getJSONArray("hitExtensions").toList()) {
            addTermRateExt((String)element);
        }
        /*
        _termRequired.add("URLRequest");
        _termRequired.add("URLLoader");
        _termRequired.add("LoadMovie");
        _termRequired.add("LoadMovieNum");
        _termRequired.add("URLStream");
        _termRequired.add("LoadVariables");
        _termRequired.add("LoadVariablesNum");
        _termRequired.add("AVURLLoader");
        _termRequired.add("NetStream");
        _termRequired.add("loadSound");

        _termRate.add("URLRequest");
        _termRate.add("URLLoader");
        _termRate.add("LoadMovie");
        _termRate.add("LoadMovieNum");
        _termRate.add("URLStream");
        _termRate.add("LoadVariables");
        _termRate.add("LoadVariablesNum");
        _termRate.add("AVURLLoader");
        _termRate.add("NetStream");
        _termRate.add("loadSound");
        addTermRateExt(".png");
        addTermRateExt(".xml");
        addTermRateExt(".jpg");
        addTermRateExt(".swf");
        addTermRateExt(".txt");
        addTermRateExt(".mp3");
        addTermRateExt(".bin");
        addTermRateExt(".gif");
        addTermRateExt(".ogg");
        addTermRateExt(".json");
        addTermRateExt(".mtl");
        addTermRateExt(".obj");
        addTermRateExt(".flv");
        addTermRateExt(".zlib");
        addTermRateExt(".aas");
        addTermRateExt(".au");
        addTermRateExt(".settings");
        addTermRateExt(".rpgmvp");
        addTermRateExt(".m4a");
        addTermRateExt(".w3d");
        addTermRateExt(".cct");
        addTermRateExt(".wav");
        addTermRateExt(".webp");
        addTermRateExt(".cxml");
        addTermRateExt(".rpgmvo");
        addTermRateExt(".MOD");
        addTermRateExt(".lua");
        addTermRateExt(".dat");
        addTermRateExt(".resource");
        addTermRateExt(".lvl");
        addTermRateExt(".webm");
        addTermRateExt(".bmp");
        addTermRateExt(".f4v");
        addTermRateExt(".mp4");
        addTermRateExt(".fnt");
        addTermRateExt(".ts");
        addTermRateExt(".dae");
        addTermRateExt(".csv");
        addTermRateExt(".vmo");
        addTermRateExt(".asasm");
        addTermRateExt(".php");
        addTermRateExt(".htm");
        addTermRateExt(".html");
        addTermRateExt(".asp");
        addTermRateExt(".ttf");
        addTermRateExt(".hx");;
        addTermRateExt(".properties");
        addTermRateExt(".db");
        */
    }

    public void addTermRateExt(String term) {
        _termRate.add(term + "\"");
        _termRate.add(term + "'");
        _termRate.add(term + "?");
    }

    public boolean isTermRequired(String term) {
        return _termRequired.contains(term);
    }

    public List<String> getTermRateList() {
        return _termRate;
    }

    public List<String> getTermRequiredList() {
        return _termRequired;
    }
}