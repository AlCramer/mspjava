// Copyright 2014 Al Cramer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package msp.lex;
import java.util.*;
import msp.util.*;

/**
* Lexer for the package. We break the source up into "blocks"
* (convenient chunks for parsing), then turn sequences of words and
* punctuation into sequences of tokens (indices into our vocubulary
* dictionary)
*/
public class Lexer {
    Vcb vcb;
    // the source we're going to lex
    public char[] src;
    // mapping, source index-> line number
    public ILst lnoMap;
    // mapping, source index-> column number
    public ILst colMap;
    public Lexer() {
        vcb = Vcb.vcb;
    }
    // convienence functions
    boolean isalnum(int i) {
        return Character.isLetterOrDigit(src[i]);
    }
    boolean isdigit(int i) {
        return Character.isDigit(src[i]);
    }
    boolean isupper(String txt, int i) {
        return Character.isUpperCase(txt.charAt(i));
    }
    boolean islower(String txt, int i) {
        return Character.isLowerCase(txt.charAt(i));
    }
    
    /** get extract from "src" */
    public String getSrcSubstr(int offset, int N) {
        return new String(src, offset, N);
    }
    
    /**
    * is src[i] a word char? Letters a word chars, as are digits and a
    * few other chars.
    */
    public boolean isWrdChar(int i, int E){
        if (i>E) {
            return false;
        }
        char c = src[i];
        if (isalnum(i) || c == '_' || c == '\'') {
            return true;
        }
        if (c == '-') {
            // is this a hyphen?
            return (i>0) && isalnum(-1) &&
            (i+1 <= E) && isalnum(1);
        }
        return false;
    }
    
    /**
    * helper for "lexWrd": is src[i] a period followed by a single
    * letter/digit?
    */
    public boolean isDotLetterSeq(int i, int E){
        if (i+2 <= E && src[i] == '.' && isalnum(i+1)) {
            return i+2 >= E || !isalnum(i+2);
        }
        return false;
    }
    
    /** lex a word, starting at src[i]: return index of last char */
    public int lexWrd(int i, int E){
        // lex numbers: "1, 200.00". Here we accept periods and commas.
        int S = i;
        if (isdigit(i)) {
            while (i+1<E) {
                if (isdigit(i+1)) {
                    i += 1;
                    continue;
                }
                if ((src[i+1] == '.') || (src[i+1] == ',')) {
                    if (isdigit(i) &&
                    i+2 <= E && isdigit(i+2)) {
                        i += 2;
                        continue;
                    }
                }
                break;
            }
            while (isWrdChar(i+1, E)) {
                i += 1;
            }
            return i;
        }
        // abbreviations like "B.C.", "U.S.A"
        if (isDotLetterSeq(i+1, E)) {
            while (isDotLetterSeq(i+1, E)) {
                i += 2;
            }
            // include trailing "." if present
            if (i+1 <= E && src[i+1] == '.') {
                i += 1;
            }
            return i;
        }
        // default cases: just consume all word chars
        while (isWrdChar(i+1, E)) {
            i += 1;
        }
        // is this "Mr."? May need to bind a trailing period.
        if (i+1 <= E && src[i+1] == '.') {
            String sp = new String(src, S, i-S);
            int tok = vcb.lkup(sp.toLowerCase(), false);
            if (vcb.checkProp(tok, WP.abbrev)) {
                i += 1;
            }
        }
        return i;
    }
    
    /** append token(s) for word "sp", expanding contractions */
    void appendContract(int S, String sp, LexRec lr) {
        ILst toks = lr.toks;
        ILst tokLoc = lr.tokLoc;
        // is there a rewrite rule for this word?
        int ix = vcb.lkup(sp.toLowerCase(), false);
        if (ix != 0) {
            ILst test = new ILst();
            test.append(ix);
            int rix = vcb.findRewrite(test.a, 0);
            if (rix != -1) {
                ILst rhs = vcb.getRhsRewrite(rix, isupper(sp, 0));
                for (int i=0; i<rhs.N; i++) {
                    toks.append(rhs.a[i]);
                    tokLoc.append(S);
                }
            }
            return;
        }
        // split on ticks
        String[] terms = sp.split("'");
        if (terms.length == 2) {
            // some canonical cases: exceptions are handled by rewrite
            // rules
            String t0 = terms[0];
            String t1 = terms[1];
            String t0lc = t0.toLowerCase();
            String t1lc = t1.toLowerCase();
            int l0 = t0.length();
            if ((l0 > 2) && t0lc.endsWith("n") && t1lc.equals("t")) {
                // "wouldn't"
                toks.append(vcb.getVocab(t0.substring(0, l0-1)));
                toks.append(vcb.getVocab("not"));
                tokLoc.append(S);
                tokLoc.append(S);
                return;
            }
            if ((l0 >= 1) && t1lc.equals("re")) {
                // "we're"
                toks.append(vcb.getVocab(t0));
                toks.append(vcb.getVocab("are"));
                tokLoc.append(S);
                tokLoc.append(S);
                return;
            }
            if ((l0 >= 1) && t1lc.equals("ll")) {
                // "we'll"
                toks.append(vcb.getVocab(t0));
                toks.append(vcb.getVocab("will"));
                tokLoc.append(S);
                tokLoc.append(S);
                return;
            }
            if (l0 >= 1 && t1lc.equals("ve")) {
                // "we've"
                toks.append(vcb.getVocab(t0));
                toks.append(vcb.getVocab("have"));
                tokLoc.append(S);
                tokLoc.append(S);
                return;
            }
            // "'s" and "'d" are context dependant and are resolved
            // during the parse
            if (t1lc.equals("s") || t1lc.equals("d")) {
                toks.append(vcb.getVocab(t0));
                toks.append(vcb.getVocab("'" + t1));
                tokLoc.append(S);
                tokLoc.append(S);
                return;
            }
        }
        // default is to accept construct as a single word
        toks.append(vcb.getVocab(sp));
        tokLoc.append(S);
    }
    
    /** rewrite token sequence, applying rewrite rules */
    public void applyRewriteRules(LexRec lr){
        ILst _toks = lr.toks;
        ILst _tokLoc = lr.tokLoc;
        ILst toks = lr.toks = new ILst();
        ILst tokLoc = lr.tokLoc = new ILst();
        int i = 0;
        while (i<_toks.N) {
            int rix = vcb.findRewrite(_toks.a, i);
            if (rix != -1) {
                // For token-location, we have to approximate. All terms in
                // the rewrite are assigned location of first term of lhs,
                // except for last term in the rewrite; that gets location
                // of last term in lhs.
                RewriteRules rules = vcb.rwrules;
                int nLhs = rules.lhs.a[rix].N;
                int SfirstTerm = _tokLoc.a[i];
                int SlastTerm = _tokLoc.a[i+nLhs-1];
                String sp = vcb.spell(_toks.a[i]);
                ILst terms = vcb.getRhsRewrite(rix, isupper(sp, 0));
                for (int j=0; j<terms.N; j++) {
                    toks.append(terms.a[j]);
                    tokLoc.append((j == terms.N -1) ? SlastTerm : SfirstTerm);
                }
                i += nLhs;
            } else {
                toks.append(_toks.a[i]);
                tokLoc.append(_tokLoc.a[i]);
                i += 1;
            }
        }
    }
    
    
    public boolean canbeProperName(int i, ILst toks){
        if (i >= toks.N) {
            return false;
        }
        String sp = vcb.spell(toks.a[i]);
        if (sp.length()>1 && isupper(sp, 0) && islower(sp, 1)) {
            // Camel case. Are we at the start of a sentence?
            boolean atStart = false;
            if (i == 0) {
                atStart = true;
            } else {
                String spPrv = vcb.spell(toks.a[i-1]);
                if (!Character.isLetterOrDigit(spPrv.charAt(0))) {
                    atStart = true;
                }
            }
            if (atStart) {
                // If this word is known to our vocabulary, we in
                // general reject it; exception is for words marked as names.
                int props = vcb.getProps(toks.a[i]);
                if ((props & WP.n) != 0) {
                    return true;
                }
                return props == 0;
            }
            // Capitalized word, preceded by non-cap: accept
            return true;
        }
        return false;
    }
    
    public boolean canbeMI(int i, ILst toks){
        if (i+1 >= toks.N) {
            return false;
        }
        String sp = vcb.spell(toks.a[i]);
        String spnxt = vcb.spell(toks.a[i+1]);
        return sp.length() == 1 && isupper(sp, 0) && spnxt.equals(".");
    }
    
    /** rewrite token sequence, so "John F.Kennedy" becomes a single token */
    public void rewriteProperNames(LexRec lr){
        ILst _toks = lr.toks;
        ILst _tokLoc = lr.tokLoc;
        ILst toks = lr.toks = new ILst();
        ILst tokLoc = lr.tokLoc = new ILst();
        int i = 0;
        while (i<_toks.N) {
            if (canbeProperName(i, _toks)) {
                int S = i;
                int E = i;
                SLst spSeq = new SLst();
                spSeq.append(vcb.spell(_toks.a[S]));
                while (true) {
                    if (canbeProperName(E+1, _toks)) {
                        spSeq.append(vcb.spell(_toks.a[E+1]));
                        E += 1;
                        continue;
                    }
                    if (canbeMI(E+1, _toks)) {
                        spSeq.append(vcb.spell(_toks.a[E+1]) + '.');
                        E += 2;
                        continue;
                    }
                    break;
                }
                if (E > S) {
                    String spAll = spSeq.join(" ");
                    toks.append(vcb.getVocab(spAll));
                    tokLoc.append(_tokLoc.a[i]);
                    i = E + 1;
                    continue;
                }
            }
            toks.append(_toks.a[i]);
            tokLoc.append(_tokLoc.a[i]);
            i += 1;
        }
    }
    
    
    /**
    * tokenize source text. Returns
    * (toks, tokLoc). "toks" is a list of tokens (indices into the
    * vocabulary's dictionary. "tokLoc[i]" gives the index in the source
    * text for the first character of the i_th token.
    */
    public LexRec lex(){
        LexRec lr = new LexRec();
        ILst toks = lr.toks;
        ILst tokLoc = lr.tokLoc;
        int S = 0;
        int E = src.length-1;
        if ((src == null) || (E<S)) {
            return lr;
        }
        int i = S;
        while (i <= E) {
            // Consume white space.
            char c = src[i];
            if ((c == ' ') || (c == '\t') ||
            (c == '\r') || (c == '\n')) {
                i += 1;
                continue;
            }
            // start index for this token
            S = i;
            if (src[i] == '-') {
                // multiple dashes lex as a single token
                while (i <= E && src[i] == '-') {
                    i += 1;
                }
                String sp = new String(src, S, i-S);
                toks.append(vcb.getVocab(sp));
                tokLoc.append(S);
                continue;
            }
            if (src[i] == '$' && isWrdChar(i+1, E)) {
                // $ binds to the word that follows: advance i and fall
                // thru to code below.
                i += 1;
            }
            if (isWrdChar(i, E)) {
                // a word
                int ixE = lexWrd(i, E);
                String sp = new String(src, S, ixE-S+1);
                int count = sp.length() - sp.replace("'", "").length();
                if (count == 0) {
                    toks.append(vcb.getVocab(sp));
                    tokLoc.append(i);
                } else {
                    appendContract(i, sp, lr);
                }
                i = ixE + 1;
                continue;
            }
            // everything else lexes as a single token.
            toks.append(vcb.getVocab(new String(src, i, 1)));
            tokLoc.append(S);
            i += 1;
        }
        // rewrite as per the rules defined in "vcb.txt"
        applyRewriteRules(lr);
        // collapse "John F. Kennedy" into a single token
        rewriteProperNames(lr);
        return lr;
    }
    
    /** does "tok" open a nested scope? */
    public boolean isOpener(int t){
        String sp = vcb.spell(t);
        return sp.equals("(") ||
        sp.equals("{") ||
        sp.equals("[") ||
        sp.equals("'") ||
        sp.equals("\"");
    }
    
    /** find close for nested scope */
    public int findCloser(ILst toks, int i){
        String sp_opener = vcb.spell(toks.a[i]);
        // this initialization corrct for single and double quotes
        int closer = toks.a[i];
        i += 1;
        if (sp_opener.equals("{")) {
            closer = vcb.lkup("}", true);
        } else if (sp_opener.equals("[")) {
            closer = vcb.lkup("]", true);
        } else if (sp_opener.equals("(")) {
            closer = vcb.lkup(")", true);
        }
        i += 1;
        while (i < toks.N) {
            if (toks.a[i] == closer) {
                return i;
            }
            if (isOpener(toks.a[i])) {
                int E = findCloser(toks, i);
                if (E == -1) {
                    i += 1;
                } else {
                    i = E + 1;
                }
                continue;
            }
            i += 1;
        }
        return -1;
    }
    
    /**
    * Recursively break a region of "src" into a sequence of blocks for
    * parsing.
    */
    public List<ParseBlk> _getParseBlks(ILst toks, ILst tokLoc){
        List<ParseBlk> lst = new ArrayList<ParseBlk>();
        int i = 0;
        while (i < toks.N) {
            int E;
            if (isOpener(toks.a[i])) {
                E = findCloser(toks, i);
                if (E == -1) {
                    // malformed: skip this character and continue
                    i += 1;
                    continue;
                }
                // A quote or parenthesized text.Get content
                List<ParseBlk> content = _getParseBlks(
                toks.slice(i+1, E),
                tokLoc.slice(i+1, E));
                if (content.size() > 0) {
                    ParseBlk blk = new ParseBlk(null, null);
                    blk.setSp(i+1, E-1);
                    blk.bracket = vcb.spell(toks.a[i]);
                    blk.sublst = content;
                    lst.add(blk);
                }
            } else {
                E = i;
                while (E+1 < toks.N) {
                    if (isOpener(toks.a[E+1])) {
                        break;
                    }
                    E += 1;
                }
                ParseBlk blk = new ParseBlk(
                toks.slice(i, E+1),
                tokLoc.slice(i, E+1));
                blk.setSp(i, E);
                lst.add(blk);
            }
            i = E + 1;
        }
        return lst;
    }
    
    /**
    * Break source into a sequence of blocks for parsing. "srcText"
    * is a chunk taken from some larger text. "lno" gives the line
    * number at which this chunk starts.
    */
    public List<ParseBlk> getParseBlks(String srcTxt, int lno){
        // create the line and column mappings.
        lnoMap = new ILst();
        colMap = new ILst();
        int col = 1;
        for (int i=0; i<srcTxt.length(); i++) {
            lnoMap.append(lno);
            colMap.append(col);
            col += 1;
            if (srcTxt.charAt(i) == '\n') {
                lno += 1;
                col = 1;
            }
        }
        // Some texts use single ticks as quote marks, creating confusion
        // between quote marks and contraction ticks. So we change
        // single-tick quote marks to double-tick marks. First create a version
        // of the source in which contraction ticks are encoded to '~'.
        srcTxt = srcTxt.replaceAll("(\\w+)'(\\w+)", "$1~$2");
        srcTxt = srcTxt.replaceAll("''(\\w+)", "'~$1");
        srcTxt = srcTxt.replaceAll("(\\w+)''", "$1~'");
        // some irregular forms
        srcTxt = srcTxt.replaceAll("'em", "~em");
        srcTxt = srcTxt.replaceAll("'tis", "~tis");
        srcTxt = srcTxt.replaceAll("'twas", "~twas");
        srcTxt = srcTxt.replaceAll("'twill", "~twill");
        // any remaining single ticks are treated as quotes: convert
        // to standard double-quote mark convention
        srcTxt = srcTxt.replaceAll("'", "\"");
        // change '~' back to single tick
        srcTxt = srcTxt.replaceAll("~", "'");
        // convert srcTxt to array-of-chars
        src = srcTxt.toCharArray();
        // lex the source
        LexRec lr = lex();
        // create the parse blocks.
        return _getParseBlks(lr.toks, lr.tokLoc);
    }
}







