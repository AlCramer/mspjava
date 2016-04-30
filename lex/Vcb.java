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
import java.io.*;
import msp.util.*;


/**
* Serializable mapping, (int16, int16)->int8. This utility class
* used by the vocabulary to represent the mapping
* (prep, verb)->fitness, where fitness measures the
* strength of the association.
*/
class Int16PairToInt8 {
    // mapping, pair to value
    HashMap<String, Integer> pairToV = new HashMap<String, Integer>();
    Int16PairToInt8() {
        super();
    }
    
    int lkup(int l1, int l2) {
        String key = String.format("%d %d", l1, l2);
        if (pairToV.containsKey(key)) {
            return pairToV.get(key);
        }
        return -1;
    }
    
    void add(int l1, int l2, int v) {
        pairToV.put(String.format("%d %d", l1, l2), v);
    }
    
    void serialize(Serialize serializer) throws IOException {
        if (serializer.mode.equals("w")) {
            ILst l1lst = new ILst();
            ILst l2lst = new ILst();
            ILst rlst = new ILst();
            for (String key : pairToV.keySet()) {
                String[] _key = key.split(" ");
                l1lst.append(Integer.parseInt(_key[0]));
                l2lst.append(Integer.parseInt(_key[1]));
                rlst.append(pairToV.get(key));
            }
            serializer.encodeIntlst(l1lst, 16);
            serializer.encodeIntlst(l2lst, 16);
            serializer.encodeIntlst(rlst, 8);
        } else {
            ILst l1lst = serializer.decodeIntlst(16);
            ILst l2lst = serializer.decodeIntlst(16);
            ILst rlst = serializer.decodeIntlst(8);
            pairToV = new HashMap<String, Integer>();
            for (int i=0; i<l1lst.N; i++) {
                add(l1lst.a[i], l2lst.a[i], rlst.a[i]);
            }
        }
    }
}

/**
* "softly" is a variant of "soft", "looked" is a variant of "look".
* This data structure records the root and props of the variant
*/
class WordVariant{
    int rootKey = 0;
    int props = 0;
    int vprops = 0;
}

/**
* A rewrite rule specifies a lhs ("target"), and a rhs
* ("replacement"). Both are sequences, giving indices into the
* dictionary. We apply a rule by recognizing a lhs in the token
* sequence and replacing it with the rhs.
*/
class RewriteRules{
    // "lhs" and "rhs" are parallel lists of token sequences.
    // lhs[i] and rhs[i] define rule "i": if we see the sequence
    // lhs[i] during tokenization, we replace it with the sequence
    // rhs[i].
    LstILst lhs = new LstILst();
    LstILst rhs = new LstILst();
    // This is a mapping, wrdIx->{ruleIx}. "wrdIx" is the dictionary
    // index for a word. "ruleIx" is the index of a rewrite rule,
    // such that the lhs of the rules starts with that word.
    LstILst index = new LstILst();
    public RewriteRules(){
        super();
    }
    public void serialize(Serialize serializer) throws IOException {
        if (serializer.mode.equals("w")) {
            serializer.encodeLstlst(lhs, 16);
            serializer.encodeLstlst(rhs, 16);
            serializer.encodeLstlst(index, 16);
        } else {
            lhs = serializer.decodeLstlst(16);
            rhs = serializer.decodeLstlst(16);
            index = serializer.decodeLstlst(16);
        }
    }
}

/**
* This module contains our vocabulary. "dict" is a dictionary, defining
* the mappings word->index, index->word, and index->properties.
* Additional data structures provide more information about entries.
* These are:
* 1. "vprops" -- verb props for the entry,
* 2. "_def" -- key of some other entry in the lexicon, which
* is the "definition" of this word. Sometimes a word is defined to
* itself.
* 3. "synClass" -- syntax-class for the word.
* 4. Prep<->Verbassociations -- is a prep associated with a verb? The
* association maybe an indirect object phrase ("I gave the apple
* TO the girl"); or it might be a common modifier clause
* ("I walked TO the store").
* 5. Rewrite Rules -- rules for replacing one set of words with
* another during tokenization.
*/
public class Vcb {
    public static Vcb vcb;
    // Our dictionary
    Dict dct = new Dict();
    public int getN(){
        return dct.getN();
    }
    // verb properties
    ILst vprops = new ILst();
    // definitions for entries.
    ILst _def = new ILst();
    // syntax class for entries.
    public ILst synclass = new ILst();
    // rewrite rules A rewrite rule specifies a lhs ("target"), and a rhs
    // ("replacement"). Both are sequences, giving indices into the
    // dictionary. We apply a rule by recognizing a lhs in the token
    // sequence and replacing it with the rhs.
    RewriteRules rwrules = new RewriteRules();
    // mapping, (prep, verb)->fitness.
    // prep and verb are dictionary indices. fitness is an int value
    // giving the strength of the association between the prep and
    // verb. 0 means means no association.
    Int16PairToInt8 prepVerbFitness = new Int16PairToInt8();
    // The syntax-class dictionary
    public Dict scDct = new Dict();
    // sc singeltons
    SLst scSingletons = new SLst();
    // version info: readin from "lexicon.txt"
    public String version = "?";
    
    public Vcb() {
        super();
        Vcb.vcb = this;
    }
    
    public void serialize(Serialize serializer) throws IOException {
        dct.serialize(serializer);
        if (serializer.mode.equals("w")) {
            serializer.encodeIntlst(vprops, 32);
            serializer.encodeIntlst(_def, 32);
            serializer.encodeIntlst(synclass, 32);
            serializer.encodeStrlst(scSingletons);
        } else {
            vprops = serializer.decodeIntlst(32);
            _def = serializer.decodeIntlst(32);
            synclass = serializer.decodeIntlst(32);
            scSingletons = serializer.decodeStrlst();
        }
        scDct.serialize(serializer);
        rwrules.serialize(serializer);
        prepVerbFitness.serialize(serializer);
    }
    
    /** lookup "sp", returning the key for its entry */
    public int lkup(String sp, boolean createIfMissing){
        int ix = dct.lkup(sp, false);
        if (ix != 0) {
            return ix;
        }
        if (!createIfMissing) {
            return 0;
        }
        ix = dct.lkup(sp, true);
        vprops.append(0);
        _def.append(0);
        synclass.append(0);
        rwrules.index.append(null);
        return ix;
    }
    
    /** define an entry */
    public int define(String sp, int props, int vprops, int _def){
        int ix = lkup(sp, true);
        setProp(ix, props);
        setVp(ix, vprops);
        if (_def != 0) {
            // this def overrides any previous def
            setDef(ix, _def);
        } else {
            // if this entry has no definition, define to self
            if (getDef(ix) == 0) {
                setDef(ix, ix);
            }
        }
        return ix;
    }
    
    /** get spelling */
    public String spell(int ix){
        return dct.spell(ix);
    }
    
    public String spell(ILst wrds) {
        if (wrds.N == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(spell(wrds.a[0]));
        int i = 1;
        while (i < wrds.N) {
            String sp = spell(wrds.a[i]);
            i += 1;
            char clast = sb.charAt(sb.length() - 1);
            if (Character.isLetterOrDigit(clast)) {
                if (Character.isLetterOrDigit(sp.charAt(0)) ||
                (sp.charAt(0) == '$')) {
                    sb.append(" ");
                }
            }
            sb.append(sp);
        }
        // reformat into second buffer
        StringBuffer sb1 = new StringBuffer();
        for (i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            char cnxt = i < sb.length() - 1 ? sb.charAt(i + 1) : '\0';
            sb1.append(c);
            if ((c == '.') || (c == '?') || (c == '!') || (c == ';')
            || (c == ':') || (c == '-')) {
                if (Character.isLetterOrDigit(cnxt)) {
                    sb1.append(" ");
                }
            }
        }
        return sb1.toString();
    }
    
    /** set prop */
    public void setVp(int ix, int v){
        vprops.a[ix] |= v;
    }
    
    /** check prop */
    public boolean checkVp(int ix, int v){
        return (ix != 0) && ((vprops.a[ix] & v) != 0);
    }
    
    /** get props */
    public int getVprops(int ix){
        return vprops.a[ix];
    }
    
    /** get def for ix */
    public int getDef(int ix){
        return _def.a[ix];
    }
    
    /** set def for ix */
    public void setDef(int ix, int v){
        _def.a[ix] = v;
    }
    
    /** get props */
    public int getProps(int ix){
        return dct.props.a[ix];
    }
    
    /** set prop */
    public void setProp(int ix, int v){
        dct.setProp(ix, v);
    }
    
    /** check prop */
    public boolean checkProp(int ix, int v){
        return dct.checkProp(ix, v);
    }
    
    /** get strength of association between prep and verb */
    public int getPrepVerbFitness(int prep, int verb){
        return prepVerbFitness.lkup(prep, verb);
    }
    
    /**
    * is an unknown word a variant of a known verb? We expect the
    * lower-case spelling of the unknown word.
    */
    public boolean isVerbVariant(String wrd, WordVariant v){
        int l = wrd.length();
        String test;
        String root;
        int key, vKey;
        // if this the not-contraction for a verb? ("isn't", "didn't")
        if ((l >= 5) && wrd.endsWith("n't")) {
            test = wrd.substring(0, l-3);
            // some cases are irregular...
            vKey = lkup(test, false);
            if (vKey != 0) {
                v.props |= WP.verb;
                v.vprops = VP.negcontraction | getVprops(vKey);
                v.vprops &= ~VP.root;
                v.rootKey = getDef(vKey);
                return true;
            }
        }
        // "...ing"
        if ((l >= 5) && wrd.endsWith("ing")) {
            root = wrd.substring(0, l-3);
            // "wanting"
            key = lkup(root, false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.gerund;
                return true;
            }
            // "hating"
            test = root + "e";
            key = lkup(test, false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.gerund;
                return true;
            }
            // "shipping"
            int lroot =root.length();
            if (root.charAt(lroot - 1) == root.charAt(lroot - 2)) {
                test = root.substring(0, lroot);
                key = lkup(test, false);
                if (checkVp(key, VP.root)) {
                    v.props |= WP.verb;
                    v.rootKey = key;
                    v.vprops |= VP.gerund;
                    return true;
                }
            }
        }
        // "...ed"
        if ((l >= 4) && wrd.endsWith("ed")) {
            root = wrd.substring(0, l-2);
            int lroot = root.length();
            // "wanted"
            key = lkup(root, false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.participle|VP.past;
                return true;
            }
            // "hated"
            key = lkup(root + "e", false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.participle|VP.past;
                return true;
            }
            // "shipped"
            if (root.charAt(lroot - 1) == root.charAt(lroot - 2)) {
                test = root.substring(0, lroot);
                key = lkup(test, false);
                if (checkVp(key, VP.root)) {
                    v.props |= WP.verb;
                    v.rootKey = key;
                    v.vprops |= VP.participle|VP.past;
                    return true;
                }
            }
        }
        // "...es"
        if ((l >= 4) && wrd.endsWith("es")) {
            // "watches"
            test = wrd.substring(0, l-2);
            if (test == "be") {
                // "bees"
                return false;
            }
            key = lkup(test, false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.present;
                return true;
            }
        }
        // "eats"
        if ((l >= 3) && wrd.endsWith("s")) {
            test = wrd.substring(0, l-1);
            key = lkup(test, false);
            if (checkVp(key, VP.root)) {
                v.props |= WP.verb;
                v.rootKey = key;
                v.vprops |= VP.present;
                return true;
            }
        }
        return false;
    }
    
    /**
    * is an unknown word a variant of a known word? We expect the
    * lower-case spelling of the unknown word.
    */
    public boolean isWordVariant(String wrd, WordVariant v){
        // check for verb variants
        boolean isVerbVar = isVerbVariant(wrd, v);
        // check non-verb forms.
        int l = wrd.length();
        String test;
        int rootKey;
        // is word an adverb form of a known modifier?
        if ((l >= 5) && wrd.endsWith("ly")) {
            test = wrd.substring(0 , l-2);
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.mod)) {
                v.props |= WP.adv;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
        }
        // a simple plural of a noun (cat->cats) ?
        if ((l >= 4) && wrd.endsWith("s")) {
            test = wrd.substring(0, l-1);
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.noun)) {
                v.props |= WP.noun;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
        }
        // mod variants: (strong->strongest), (strange->strangest)
        if ((l >= 6) && wrd.endsWith("est")) {
            test = wrd.substring(0, l-3);
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.mod)) {
                v.props |= WP.adj;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
            test += "e";
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.mod)) {
                v.props |= WP.adj;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
        }
        // mod variants: (strong->stronger), (strange->stranger)
        if ((l >= 6) && wrd.endsWith("er")) {
            test = wrd.substring(0, l-2);
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.mod)) {
                v.props |= WP.adj;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
            test += "e";
            rootKey = lkup(test, false);
            if (checkProp(rootKey, WP.mod)) {
                v.props |= WP.adj;
                if (v.rootKey == 0) {
                    v.rootKey = rootKey;
                }
                return true;
            }
        }
        return isVerbVar;
    }
    
    /** get synclass spelling for entry "i" */
    public String getScDesc(int i){
        // most tests based on word/verb props, but some require that we look at
        // the spelling
        String sp = spell(i);
        if (checkProp(i, WP.dets)) {
            return "DetS";
        }
        // conjunctions
        if (sp.equals("and") || sp == "or") {
            return "AndOr";
        }
        if (checkProp(i, WP.conj)) {
            return "Conj";
        }
        if (checkProp(i, WP.query)) {
            return "Query";
        }
        if (checkVp(i, VP.gerund)) {
            return "Ger";
        }
        if (checkVp(i, VP.participle)) {
            return "Part";
        }
        // collect classes for this entry (there may be > 1)
        SLst l = new SLst();
        // determinants
        if (checkProp(i, WP.detw)) {
            l.append("DetW");
        }
        // preps
        if (checkProp(i, WP.clprep)) {
            l.append("ClPrep");
        } else if (checkProp(i, WP.qualprep)) {
            l.append("QualPrep");
        } else if (checkProp(i, WP.prep)) {
            l.append("Prep");
        }
        // nouns
        if (checkProp(i, WP.noun)) {
            l.append("Noun");
        }
        // names
        if (checkProp(i, WP.n)) {
            l.append("N");
        }
        // mods
        if (checkProp(i, WP.adj)) {
            l.append("Adj");
        }
        if (checkProp(i, WP.adv)) {
            l.append("Adv");
        }
        if (checkProp(i, WP.clmod)) {
            l.append("ClMod");
        }
        // verb-adjuncts and verbs
        if (checkProp(i, WP.verb)) {
            if (checkVp(i, VP.adj)) {
                l.append("VAdj");
            } else {
                l.append("V");
            }
        }
        if (l.N == 0) {
            l.append("X");
        }
        return l.join("|");
    }
    
    /** get entry for word "sp", create if needed */
    public int getVocab(String sp){
        int ix = lkup(sp, false);
        if (ix != 0) {
            return ix;
        }
        ix = lkup(sp, true);
        // need a def for this word. Does the lower case version exist?
        String spLc = sp.toLowerCase();
        if (!spLc.equals(sp)) {
            int ixLc = lkup(spLc, false);
            if (ixLc != 0) {
                // this is our def. Set and transfer props
                setDef(ix, ixLc);
                setProp(ix, getProps(ixLc));
                setVp(ix, getVprops(ixLc));
                synclass.a[ix] = synclass.a[ixLc];
                return ix;
            }
        }
        // is this word a variant of a known word?
        WordVariant wv = new WordVariant();
        if (isWordVariant(spLc, wv)) {
            setDef(ix, wv.rootKey);
            setProp(ix, wv.props);
            setVp(ix, wv.vprops);
            synclass.a[ix] = scDct.lkup(getScDesc(ix), false);
            assert synclass.a[ix] != 0;
            return ix;
        }
        // define to self
        setDef(ix, ix);
        synclass.a[ix] = scDct.lkup("X", false);
        return ix;
    }
    
    /** get spelling for syntax class */
    public String spellSc(int ix){
        return scDct.spell(ix);
    }
    public String spellSc(ILst lst){
        return scDct.spell(lst);
    }
    
    /** get index for sc, given its spelling */
    public int lkupSc(String scSp){
        return scDct.lkup(scSp, false);
    }
    
    /** is "sc" a synclass for a verb? */
    public boolean isScForVerb(int i){
        return scDct.checkProp(i, WP.verb);
    }
    
    /** check props (WP_xxx) for sc's */
    public boolean checkScProp(int scIx, int m){
        return scDct.checkProp(scIx, m);
    }
    
    /** return spelling+props for sc */
    public String scTostr(int i){
        int scProps = scDct.props.a[i];
        return String.format(
        "%s(%s)", scDct.spell(i), WP.tostr(scProps));
    }
    
    /** is sc a singleton? */
    public boolean isScSingleton(int i){
        return scSingletons.contains(scDct.spell(i));
    }
    
    /**
    * does rewrite rule "rix" apply to tok sequence "toks"
    * starting at element "i"?
    */
    public boolean testRewrite(int rix, int[] toks, int i){
        int nLhs = rwrules.lhs.a[rix].N;
        if (i + nLhs > toks.length) {
            return false;
        }
        //for j in range(0, nLhs):
        for (int j=0; j<nLhs; j++) {
            if (rwrules.lhs.a[rix].a[j] != getDef(toks[i+j])) {
                return false;
            }
        }
        return true;
    }
    
    /** find rewrite rule that applies to toks[i] */
    public int findRewrite(int[] toks, int i){
        ILst rules = rwrules.index.a[getDef(toks[i])];
        if (rules != null) {
            //for rix in rules:
            ILstIterator iter = rules.getIterator();
            while (iter.hasNext()) {
                int rix = iter.next();
                if (testRewrite(rix, toks, i)) {
                    return rix;
                }
            }
        }
        return -1;
    }
    
    /** get rhs tokens for rewrite rule */
    public ILst getRhsRewrite(int rix, boolean wantUpper){
        ILst rhs = rwrules.rhs.a[rix].clone();
        if (wantUpper) {
            // want upper-case start for rhs[0]
            String spx = spell(rhs.a[0]);
            char c0 = Character.toUpperCase(spx.charAt(0));
            StringBuffer sb = new StringBuffer(spx);
            sb.setCharAt(0, c0);
            rhs.a[0] = getVocab(sb.toString());
        }
        return rhs;
    }
    
    /** print a rewrite rules */
    public void printRewriteRule(int i){
        System.out.println(String.format(
        "rule%d. %s : %s",
        i, spell(rwrules.lhs.a[i]), spell(rwrules.rhs.a[i]) ));
    }
    
    /** print the rewrite rules */
    public void printRewriteRules(){
        System.out.println("lexicon rewrite rules:");
        //for i in range(0, rwrules.lhs.N):
        for (int i=0; i<rwrules.lhs.N; i++) {
            printRewriteRule(i);
        }
        System.out.println("index");
        //for i in range(0, rwrules.index.N):
        for (int i=0; i<rwrules.index.N; i++) {
            if (rwrules.index.a[i] != null) {
                String myrules = rwrules.index.a[i].toString();
                System.out.println(String.format(
                "%d. %s -> {%s}",
                i, spell(i), myrules ));
            }
        }
    }
    
    /** print (prep, verb)->fitness mapping. */
    public void printPrepVerbFitness(){
        System.out.print("Preps-for-verbs:");
        ArrayList<String> tmp = new ArrayList<String>();
        for (String key : prepVerbFitness.pairToV.keySet()) {
            String[] _key = key.split(" ");
            String spPrep = spell(Integer.parseInt(_key[0]));
            String spVerb = spell(Integer.parseInt(_key[1]));
            tmp.add(String.format("%s %s: %d",
            spPrep, spVerb, prepVerbFitness.pairToV.get(key)));
        }
        Collections.sort(tmp);
        for (String e : tmp) {
            System.out.println(e);
        }
    }
    
    /** print (prep, fitness) for verb */
    public void printPrepsForVerb(int ixv){
        String keyv = Integer.toString(ixv);
        for (String key : prepVerbFitness.pairToV.keySet()) {
            String[] _key = key.split(" ");
            if (!keyv.equals(_key[1])) {
                continue;
            }
            String spPrep = spell(Integer.parseInt(_key[0]));
            String spVerb = spell(Integer.parseInt(_key[1]));
            System.out.println(String.format("%s %s: %d",
            spPrep, spVerb, prepVerbFitness.pairToV.get(key)));
        }
    }
    
    
    /** print info about a word */
    public void printWrdInfo(String sp){
        int i = getVocab(sp);
        System.out.print(String.format(
        "ix:%d " , i ));
        System.out.print(String.format(
        "def:%d " , getDef(i) ));
        System.out.print(String.format(
        "spDef:%s " , spell(getDef(i)) ));
        System.out.println(String.format(
        "props:%s " , WP.tostr(getProps(i)) ));
        int scIx = synclass.a[i];
        System.out.print(String.format(
        "sc:%s " , spellSc(scIx) ));
        int scProps = scDct.props.a[scIx];
        System.out.println(String.format(
        "scProps:%s ", WP.tostr(scProps) ));
        if (rwrules.index.a[i] != null) {
            System.out.println(String.format(
            "rewrite rules:" ));
            //for rix in rwrules.index.a[i]:
            ILstIterator iter = rwrules.index.a[i].getIterator();
            while (iter.hasNext()) {
                printRewriteRule(iter.next());
            }
        }
        System.out.println("Preps:");
        printPrepsForVerb(i);
        System.out.println("");
    }
    
    /** unit-test: print info about various words */
    public void ut() {
        String[] wrds = { "i", "was", "can't", "saw", "take" };
        for (int i = 0; i < wrds.length; i++) {
            System.out.println(wrds[i] + ":");
            printWrdInfo(wrds[i]);
        }
    }
    
}







