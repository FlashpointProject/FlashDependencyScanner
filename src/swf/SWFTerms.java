package src.swf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import static src.DependencyChecker.DEBUG;

public class SWFTerms {
    private HashMap<String, Integer> _termRate;
    private List<String> _termRequired;

    // TODO: make this use regex for a "fast vs more accurate vs most accurate"
    public SWFTerms(String jsonString) {
        // Init the arrays to empty.
        _termRequired = new ArrayList<String>();
        _termRate = new HashMap<String, Integer>();

        // Convert the jsonString to a JSONObject.
        JSONObject terms = new JSONObject(jsonString);

        // Get the three relevant arrays.
        terms.getJSONArray("requiredTerms").forEach((Object element) -> {
            // Cast to string and add the element.
            _termRequired.add((String) element);
            // If we're debugging, log a message.
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("Adding required term: " + (String) element);
                }
            }
        });

        terms.getJSONArray("hitTerms").forEach((Object element) -> {
            // Cast to a string, and add it with zero weight.
            _termRate.put((String) element, 0);
            // If we're debugging, log a message.
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("Adding include function: " + (String) element);
                }
            }
        });

        terms.getJSONArray("hitExtensions").forEach((Object element) -> {
            // Add the extension to the map.
            addTermRateExt(((JSONObject) element).getString("ext"), ((JSONObject) element).getInt("weight"));
        });
    }

    public void addTermRateExt(String term, Integer weight) {
        // If we're debugging, log a message.
        if (DEBUG) {
            synchronized (System.out) {
                System.out.println("Adding extension: \"" + term + "\" with weight: " + weight);
            }
        }
        _termRate.put(term + "\"", weight);
        _termRate.put(term + "'", weight);
        _termRate.put(term + "?", weight);
    }

    public boolean isTermRequired(String term) {
        return _termRequired.contains(term);
    }

    public HashMap<String, Integer> getTermRateList() {
        return _termRate;
    }

    public List<String> getTermRequiredList() {
        return _termRequired;
    }
}