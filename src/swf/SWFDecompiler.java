package src.swf;

import java.io.*;
import java.util.*;

import com.jpexs.decompiler.graph.TranslateException;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.ConvertData;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.exporters.modes.ScriptExportMode;
import com.jpexs.decompiler.flash.helpers.HighlightedTextWriter;
import com.jpexs.decompiler.flash.helpers.CodeFormatting;
import com.jpexs.decompiler.flash.tags.ABCContainerTag;
import com.jpexs.decompiler.flash.tags.base.ASMSource;

public class SWFDecompiler {
    // DeobfuscationLevel level = DeobfuscationLevel.getByLevel(1);
    // swf.deobfuscate(level);

    // Get rid of crazy invalid identifiers
    // swf.deobfuscateIdentifiers(RenameType.RANDOMWORD);
    // swf.assignClassesToSymbols();
    private SWF swf;
    private SWFFile _file;
    private SWFTerms _terms;

    public SWFDecompiler(SWFFile file) {
        this._file = file;
        this._terms = new SWFTerms();
        try {
            System.out.println("Decompiling: " + file.getPath());
            // Use jpexs to decompile the swf.
            // TODO: close the stream?
            this.swf = new SWF(new BufferedInputStream(new FileInputStream(file.getPath())), false);
        } catch (com.jpexs.decompiler.flash.EndOfStreamException eofse) {
            System.out.println("EndOfStreamException Error: " + file.getPath());
        } catch (InterruptedException ie) {
            // TODO:Throw Error
        } catch (IOException ioe) {
            // TODO:Throw Error
        } catch (Exception e) {
            System.out.println("TODO: needs to be a custom error when the thing doesn't decompile...");
        }
    }

    public Boolean isAS3() {
        return swf.isAS3();
    }

    /**
     * Scans a file for external imports.
     * @param getPcode If true, decompile to pcode instead of actionscript.
     * @return True if the game is multi-asset, false if the game is single-asset.
     * @throws Exception If an error occured.
     */
    public boolean scanFile(Boolean getPcode) throws Exception {
        // Set the export mode...
        ScriptExportMode sem;
        if (getPcode) {
            sem = ScriptExportMode.PCODE;
        } else {
            sem = ScriptExportMode.AS;
        }

        // If it is AS3 then do this:
        if (this.swf.isAS3()) {
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
                        System.out.println("Skipped:" + classPathString);
                    }

                    en.toSource(htw, abc.script_info.get(s).traits.traits, new ConvertData(), sem, false);
                    return scanScript(htw.toString());
                }
            }
            // Else non-as3...
        } else {
            Map<String, ASMSource> asms = this.swf.getASMs(false);
            for (ASMSource asm : asms.values()) {
                try {
                    HighlightedTextWriter writer = new HighlightedTextWriter(new CodeFormatting(), false);
                    if (!getPcode) {
                        asm.getActionScriptSource(writer, null);
                    } else {
                        asm.getASMSource(sem, writer, null);
                    }
                    String as = writer.toString();

                    return scanScript(as);
                }
            }
        }

    }

    // scan the script for all of the values.
    // Returns true if it found anything within that script.
    public Boolean scanScript(String script) throws Exception {
        if (!this.checkScriptForReq(script)) {
            return false;
        }

        Boolean scanPassed = false;
        // Umm... point of this? we're gonna return true anyway...
        for (String term : _terms.getTermRateList().keySet()) {
            if (checkTermData(term, script)) {
                scanPassed = true;
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
        String lcase = script.toLowerCase();
        Boolean meetsReqs = false;
        if (!this.swf.isAS3()) {
            if (lcase.indexOf("new xml(") > -1 && lcase.indexOf(".load(") > -1) {
                // Need to have new XML and .load to know it is getting an external file
                meetsReqs = true;
                this._file.setFoundRequiredFunc();
                return true;
            }
        }

        // AS3 and AS2 see if the terms exist.
        for (String term : _terms.getTermRequiredList()) {
            if (lcase.indexOf(term.toLowerCase()) > -1) {
                meetsReqs = true;
                this._file.setFoundRequiredFunc();
                break;
            }
        }
        return meetsReqs;
    }

    public Boolean checkTermData(String word, String script) throws Exception {
        String lcScript = script.toLowerCase();
        int scriptLen = script.length();
        String lcWord = word.toLowerCase();

        int index = 0;
        int termCnt = 0;
        int termRankCnt = 0;
        Boolean foundSomething = false;
        while (index != -1) {
            foundSomething = true;
            index = lcScript.indexOf(lcWord, index);

            // Add the term to the list on the first run only to prevent duplicates
            if (index > -1) {
                _file.addTerm(word);
                index++;
            }
        }
        return foundSomething;
    }
}