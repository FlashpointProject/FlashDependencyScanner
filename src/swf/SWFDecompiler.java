package src.swf;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.ConvertData;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.exporters.modes.ScriptExportMode;
import com.jpexs.decompiler.flash.helpers.HighlightedTextWriter;
import com.jpexs.decompiler.flash.helpers.CodeFormatting;
import com.jpexs.decompiler.flash.tags.ABCContainerTag;
import com.jpexs.decompiler.flash.tags.base.ASMSource;

import src.swf.SWFConfig.OutputDetail;
import static src.DependencyChecker.DEBUG;

public class SWFDecompiler {
    // DeobfuscationLevel level = DeobfuscationLevel.getByLevel(1);
    // swf.deobfuscate(level);

    // Get rid of crazy invalid identifiers
    // swf.deobfuscateIdentifiers(RenameType.RANDOMWORD);
    // swf.assignClassesToSymbols();
    private SWF swf;
    private SWFTerms terms;
    private Map<String, Integer> term_freq;
    private OutputDetail loglevel;
    private String filepath;
    private Integer score = 0;

    public SWFDecompiler(File infile, OutputDetail detail, SWFTerms inputTerms) {
        this.terms = inputTerms;
        this.term_freq = new HashMap<String, Integer>();
        this.loglevel = detail;
        try {
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("Decompiling: " + infile.getPath());
                }
            }
            // Use jpexs to decompile the swf.
            BufferedInputStream swf_stream = new BufferedInputStream(new FileInputStream(infile.getPath()));
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("opened stream.");
                }
            }
            this.swf = new SWF(swf_stream, false);
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("Created SWF object.");
                }
            }
            swf_stream.close();
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("closed stream.");
                }
            }
            this.filepath = infile.getCanonicalPath();
            if (DEBUG) {
                synchronized (System.out) {
                    System.out.println("set filepath");
                }
            }

        } catch (com.jpexs.decompiler.flash.EndOfStreamException eofse) {
            synchronized (System.out) {
                System.out.println("EndOfStreamException Error: " + infile.getPath());
            }
        } catch (InterruptedException ie) {
            synchronized (System.out) {
                System.out.println("Error - interrupted: " + ie.toString());
            }
        } catch (IOException ioe) {
            synchronized (System.out) {
                System.out.println("Error - IO: " + ioe.toString());
            }
        } catch (Exception e) {
            synchronized (System.out) {
                System.out.println("Decompile Error: " + e.toString());
            }
        }
    }

    /**
     * Get the string that should be printed to output. This will depend on the
     * loglevel.
     * 
     * @param found Whether or not something was actually found.
     * @return The correct string to output for this loglevel.
     */
    public String GetOutputString(boolean found) {
        if (loglevel == OutputDetail.PATH_VAL) {
            // Btw, using the ternary operator here to convert bool->int.
            return this.filepath + ',' + (found ? 1 : 0) + System.lineSeparator();
        } else {
            // If we're logging terms, include those.
            return this.filepath + ',' + (found ? 1 : 0) + ',' + joinTermMap() + System.lineSeparator();
        }
    }

    /**
     * Get the list of terms and their frequencies, formatted for log output.
     * Remember, term_freq will only have one element if we're on PATH_VAL_HIT mode.
     * 
     * @return A string with the following format: each term is represented as
     *         "term:count", and different terms are separated by semicolons.
     */
    public String joinTermMap() {
        // Start off with an empty string.
        StringBuilder out = new StringBuilder("");
        // For each entry,
        for (Map.Entry<String, Integer> entry : term_freq.entrySet()) {
            // Add it to the string.
            out.append(entry.getKey() + ':' + entry.getValue() + ';');
        }
        // Return the now-built string.
        return out.toString();
    }

    /**
     * Return if this object's SWF uses ActionScript 3.
     * 
     * @return See above.
     */
    public Boolean isAS3() {
        return swf.isAS3();
    }

    /**
     * Scans a file for external imports.
     * 
     * @param getPcode If true, decompile to pcode instead of actionscript.
     * @return True if the game is multi-asset, false if the game is single-asset.
     * @throws Exception If an error occured.
     */
    public boolean scanFile(Boolean getPcode) throws Exception {
        if (DEBUG) {
            synchronized (System.out) {
                System.out.println("SWFDecompiler.scanFile called");
            }
        }
        // Set the export mode...
        ScriptExportMode sem;
        if (getPcode) {
            sem = ScriptExportMode.PCODE;
        } else {
            sem = ScriptExportMode.AS;
        }
        // By default, we found nothing.
        boolean found = false;

        // If it is AS3 then do this:
        if (this.swf.isAS3()) {
            // TODO: what's the point of this? It only works until the first ABC.
            boolean dotest = false;

            // Get a list of all ABC's
            List<ABC> allAbcs = new ArrayList<>();
            for (ABCContainerTag ct : this.swf.getAbcList()) {
                allAbcs.add(ct.getABC());
            }

            // For each ABC we get the source with the Highlight Writer
            for (ABC abc : allAbcs) {
                for (int s = 0; s < abc.script_info.size(); s++) {
                    String startAfter = null;
                    HighlightedTextWriter htw = new HighlightedTextWriter(new CodeFormatting(), false);
                    ScriptPack en = abc.script_info.get(s).getPacks(abc, s, null, allAbcs).get(0);
                    String classPathString = en.getClassPath().toString();

                    if (startAfter == null || classPathString.equals(startAfter)) {
                        dotest = true;
                    }
                    if (!dotest) {
                        synchronized (System.out) {
                            System.out.println("Skipped:" + classPathString);
                        }
                    }

                    en.toSource(htw, abc.script_info.get(s).traits.traits, new ConvertData(), sem, false);
                    // found gets or-ed with the result. If we found something and we're not looking
                    // for more,
                    if ((found |= scanScript(htw.toString()))
                            && loglevel != OutputDetail.PATH_VAL_ALLHITS
                            && loglevel != OutputDetail.PATH_VAL_ALLHITS_SCORE) {
                        // Bail out.
                        return true;
                    }
                }
            }
            // Else non-as3...
        } else {
            Map<String, ASMSource> asms = this.swf.getASMs(false);
            for (ASMSource asm : asms.values()) {
                HighlightedTextWriter writer = new HighlightedTextWriter(new CodeFormatting(), false);
                if (!getPcode) {
                    asm.getActionScriptSource(writer, null);
                } else {
                    asm.getASMSource(sem, writer, null);
                }
                String as = writer.toString();
                // If we found something and we're not looking for more,
                if ((found |= scanScript(as))
                        && loglevel != OutputDetail.PATH_VAL_ALLHITS
                        && loglevel != OutputDetail.PATH_VAL_ALLHITS_SCORE) {
                    // Bail out.
                    return true;
                }
            }
        }
        return found;
    }

    // scan the script for all of the values.
    // Returns true if it found anything within that script.
    public Boolean scanScript(String script) throws Exception {
        // Check: were there any import statements at all?
        if (!this.checkScriptForReq(script)) {
            // No, none. Don't waste time looking for the terms.
            return false;
        }

        // Check the loglevel - are we looking for terms?
        if (loglevel == OutputDetail.PATH_VAL) {
            // No, we're not. Return true.
            return true;
        }

        // If we're looking for terms, continue.
        for (String term : terms.getTermRateList().keySet()) {
            if (checkTermData(term, script)) {
                // If we're only looking for the first hit, don't continue.
                if (loglevel == OutputDetail.PATH_VAL_HIT) {
                    break;
                }
            }
        }

        return true;
    }

    // Once a requirement is found, return true and stop.
    // This should only be used to quickly do a pre-scan for functions and not to
    // invalidate a script
    // This is really used to invalidate a SWF file by looking at every script and
    // not waste
    // our time by looking for extensions when the required functions don't even
    // exist.
    private Boolean checkScriptForReq(String script) {
        // Convert the script to lower case.
        String lcase = script.toLowerCase();
        // By default, we assume that there are no external files.
        Boolean meetsReqs = false;
        if (!this.swf.isAS3()) {
            if (lcase.indexOf("new xml(") > -1 && lcase.indexOf(".load(") > -1) {
                // Need to have new XML and .load to know it is getting an external file
                meetsReqs = true;
                return true;
            }
        }

        // AS3 and AS2 see if the terms exist.
        for (String term : terms.getTermRequiredList()) {
            if (lcase.indexOf(term.toLowerCase()) > -1) {
                meetsReqs = true;
                break;
            }
        }

        return meetsReqs;
    }

    /**
     * Search for a given word in the script.
     * 
     * @param word   The word to search for.
     * @param script The script to search.
     * @return True if we found something, false otherwise.
     */
    public Boolean checkTermData(String word, String script) {
        // Convert the script and word to lowercase.
        String lcScript = script.toLowerCase();
        String lcWord = word.toLowerCase();

        int index = 0;
        Boolean foundSomething = false;
        while (index != -1) {
            index = lcScript.indexOf(lcWord, index);

            // Add the term to the list on the first run only to prevent duplicates
            if (index > -1) {
                // We found something!
                foundSomething = true;
                // If we're looking for all the hits,
                if (loglevel == OutputDetail.PATH_VAL_ALLHITS
                        || loglevel == OutputDetail.PATH_VAL_ALLHITS_SCORE) {
                    // Keep searching, so that we get them all
                    // Increment so that we don't double-count this one.
                    index++;
                    // Increment the count for this term by one.
                    addTerm(word);
                } else {
                    // We're not looking for all the terms, only one.
                    // Break, so that we don't waste time looking for more.
                    break;
                }
            }
        }
        return foundSomething;
    }

    /**
     * Increment by one the number of times that a term was found.
     * 
     * @param term The term to increment the count for.
     */
    public void addTerm(String term) {
        // Get the current count.
        Integer cnt = this.term_freq.get(term);
        // If it's null, the term wasn't found.
        if (cnt == null) {
            // Note the first instance of the term.
            this.term_freq.put(term, 1);
        } else {
            // Increment the count by one.
            this.term_freq.put(term, ++cnt);
        }
        // If we're tracking the score,
        if (loglevel == OutputDetail.PATH_VAL_ALLHITS_SCORE) {
            // Increment the score by the proper weight.
            score += this.terms.getTermRateList().get(term);
        }
    }
}