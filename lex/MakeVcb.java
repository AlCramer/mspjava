// Copyright 2011 Al Cramer
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

/**
* Initialize vocabulary from "lexicon.txt", an ascii file containing
* information about words (lists of verbs, etc.).
*/
package msp.lex;
import java.util.*;
import java.io.*;
import msp.util.*;
/**
* Initialize vocabulary from "lexicon.txt", an ascii file containing
* information about words (lists of verbs, etc.).
*/
public class MakeVcb {
    Vcb vcb;
    // rewrite rules. We collect rules of the form
    // "<lhs terms> : <rhs terms>"
    // as we process the file "lexicon.txt", then create
    // vcb's "rwrules" attribute when the collection is complete.
    List<Object> rwrulesRaw = new ArrayList<Object>();
    public void defineRewriteRules(RewriteRules rules){
        // ensure all terms are defined in the vocab
        for (Object e: rwrulesRaw) {
            String[] rule = (String[])e;
            for (String t: rule) {
                if (t != ":") {
                    vcb.define(t, 0, 0, 0);
                }
            }
        }
        // initialize the index for the rules collection
        for (int i=0; i<vcb.getN(); i++) {
            rules.index.append(null);
        }
        // define the lhs & rhs side of each rule
        for (Object e: rwrulesRaw) {
            String[] rule = (String[])e;
            ILst lhs = new ILst();
            ILst rhs = new ILst();
            ILst targ = lhs;
            //for t in rule:
            for (String t: rule) {
                if (t.equals(":")) {
                    targ = rhs;
                } else {
                    targ.append(vcb.lkup(t, false));
                }
            }
            // "rix": index of new rule.
            int rix = rules.lhs.N;
            rules.lhs.append(lhs);
            rules.rhs.append(rhs);
            // "key": first token, lhs. Add rix to rules.index[key]
            int key = lhs.a[0];
            ILst lst = rules.index.a[key];
            if (lst == null) {
                lst = rules.index.a[key] = new ILst();
                lst.append(rix);
            } else {
                // index entries are sorted, longest-lhs first
                int ixInsert = -1;
                for (int i=0; i<lst.N; i++) {
                    int _rix = lst.a[i];
                    if (lhs.N >= rules.lhs.a[_rix].N) {
                        ixInsert = i;
                        break;
                    }
                }
                if (ixInsert != -1) {
                    lst.insert(ixInsert, rix);
                } else {
                    lst.append(rix);
                }
            }
        }
    }
    
    /** add a prep->{verb} mapping */
    public void addPrepVerbFitness(String[] terms){
        // get prep
        int pkey = vcb.define(terms[0], WP.prep, 0, 0);
        // collect verbs
        int i = 2; // skip ":" after prep;
        while (i<terms.length) {
            String verb = terms[i];
            int cnt = 1;
            if (verb.contains("=")) {
                String[] _term = verb.split("=");
                verb = _term[0];
                cnt = Integer.parseInt(_term[1]);
            }
            int vkey = vcb.lkup(verb, false);
            if ((vkey != 0) && vcb.checkVp(vkey, VP.root)) {
                vcb.prepVerbFitness.add(pkey, vkey, cnt);
            }
            i += 1;
        }
    }
    
    /**
    * create entry for word and set props. If word is already defined,
    * just add the props to the existing entry
    */
    public int define(String sp, int props, int vprops){
        int key = vcb.lkup(sp, false);
        if (key != 0) {
            vcb.setProp(key, props);
            vcb.setVp(key, vprops);
            return key;
        }
        key = vcb.define(sp, props, vprops, 0);
        int _def = vcb.getDef(key);
        if (_def == key) {
            // is this word a variant of some other entry?
            WordVariant v = new WordVariant();
            if (vcb.isWordVariant(sp, v)) {
                vcb.setDef(key, v.rootKey);
                vcb.setProp(key, v.props);
                vcb.setVp(key, v.vprops);
            }
        }
        return key;
    }
    
    /**
    * define a group of words: props and root apply to all
    * members in list
    */
    public void defineWords(int props, int vprops, int root, String lst){
        for (String sp : lst.split("\\s+")) {
            vcb.define(sp, props, vprops, root);
        }
    }
    
    /** Get lines from the ASCII file of lexical info */
    static SLst readLexicon(String fn) throws IOException {
        SLst lines = new SLst(1024);
        File file = new File(fn);
        BufferedReader br = new BufferedReader(new FileReader(file));
        boolean lastLineContinues = false;
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            int l = line.length();
            if ((l == 0) || (line.charAt(0) == '/')) {
                // skip empty line or comment
                continue;
            }
            boolean thisLineContinues = false;
            if (line.charAt(l - 1) == '+') {
                thisLineContinues = true;
                line = line.substring(0, l - 1);
            }
            if (lastLineContinues) {
                String curLine = lines.a[lines.N - 1];
                lines.a[lines.N - 1] = curLine + " " + line;
            } else {
                lines.append(line);
            }
            lastLineContinues = thisLineContinues;
        }
        br.close();
        return lines;
    }
    
    /**
    * Add a verb. First word is the root. If verb is irregular,
    * remaining terms are:
    * 3rdPersonPresent past-simple past-perfect gerund. Ex:
    * "go goes went gone going".
    */
    public void addVerb(String[] terms){
        int rootKey =
        vcb.define(terms[0], WP.verb, VP.root|VP.present, 0);
        int i = 1;
        if (i<terms.length && !terms[i].equals(":")) {
            // forms for verbs "goes"
            vcb.define(terms[i], WP.verb, VP.present, rootKey);
            i += 1;
            // "went"
            vcb.define(terms[i], WP.verb, VP.past, rootKey);
            i += 1;
            // "gone"
            vcb.define(terms[i], WP.verb, VP.past, rootKey);
            i += 1;
            // "going"
            vcb.define(terms[i], WP.verb, VP.gerund, rootKey);
            i += 1;
        }
        if (i<terms.length && terms[i].equals(":")) {
            // get syntax form
            i += 1;
            if (terms[i].equals("AVE")) {
                vcb.setVp(rootKey, VP.ave);
            } else if (terms[i].equals("EVT")) {
                vcb.setVp(rootKey, VP.evt);
            } else if (terms[i].equals("AVGT")) {
                vcb.setVp(rootKey, VP.avgt);
            } else if (terms[i].equals("VPQ")) {
                vcb.setVp(rootKey, VP.vpq);
            } else {
                throw new RuntimeException(
                "MakeVcb:: bad \">>Verbs\" entry:" + terms.toString());
            }
        }
    }
    
    /** assign a syntax class to each word in the vocabulary */
    public void assignSynclasses(){
        vcb.synclass = new ILst();
        //for i in range(1, vcb.getN()):
        for (int i = 1; i < vcb.getN(); i++) {
            String sp = vcb.spell(i);
            int scIx = vcb.scDct.lkup(sp, false);
            if (scIx == 0) {
                String scDesc = vcb.getScDesc(i);
                scIx = vcb.scDct.lkup(scDesc, false);
                if (scIx == 0) {
                    /*
                    System.out.println(String.format(
                    "Warning: could not assign syntax class to \"%s\"",
                    sp ));
                    */
                    scIx = vcb.scDct.lkup("X", false);
                }
            }
            vcb.synclass.append(scIx);
        }
    }
    
    /** initialize vocabulary from an ASCII file of lexical info */
    public Vcb createVcb(String fnlexicon) throws IOException {
        vcb = new Vcb();
        // By convention an index of "0" means "no entry" on a lookup. Make
        // a dummy entry for 0, so any subsequent entries will have key > 0
        vcb.define("_NULL_", 0, 0, 0);
        // create entries for various forms of "be": be being am are is was
        // were been
        int rootKey = vcb.define("be", WP.verb, VP.root|VP.present, 0);
        vcb.define("being", WP.verb, VP.gerund, rootKey);
        // present tense forms. "'s" is a contraction for "is"
        defineWords(WP.verb, VP.present, rootKey, "am are is 's");
        // past tense forms
        defineWords(WP.verb, VP.past, rootKey, "was were been");
        // create entry for "'d" as a verb adjunct
        vcb.define("'d", WP.verb, VP.adj, 0);
        // create entries for "and" and "or"
        defineWords(WP.conj, 0, 0, "and or");
        // create entries for verb-phrase-adjuncts
        defineWords(WP.verb, VP.adj, 0,
        "will shall would should may might ought");
        vcb.define("can", WP.verb, VP.adj|VP.present, 0);
        vcb.define("could", WP.verb, VP.adj|VP.past, 0);
        // define entries for pronouns. Note "her" is a special case:
        // it's a weak determinant ("I saw her", "I saw her mother").
        defineWords(WP.n, 0, 0,
        "i you we he she they it");
        defineWords(WP.n, 0, 0,
        "me you him them us it");
        vcb.define("her", WP.detw, 0, 0);
        // create entries for words mapping to distinct synclasses
        //for sp in vcb.scSingletons:
        SLstIterator iter = vcb.scSingletons.getIterator();
        while (iter.hasNext()) {
            vcb.define(iter.next(), 0, 0, 0);
        }
        // read additional lexical info from file.
        String state = "";
        int props = 0;
        SLst lines = readLexicon(fnlexicon);
        iter = lines.getIterator();
        while (iter.hasNext()) {
            String line = iter.next().trim();
            if (line.startsWith("/")) {
                continue;
            }
            if (line.startsWith(">>Version")) {
                vcb.version = line.substring(">>Version".length()).trim();
                continue;
            }
            String[] terms = line.split(" ");
            if (terms.length == 0) {
                continue;
            }
            String w0 = terms[0];
            if (w0.startsWith(">")) {
                if (w0.equals(">>Rewrite")) {
                    state = w0;
                } else if (w0.equals(">>Verbs")) {
                    state = w0;
                } else if (w0.equals(">>Contractions")) {
                    state = w0;
                } else if (w0.equals(">>PrepVerbs")) {
                    state = w0;
                } else {
                    // everything else sets a prop for a word.
                    state = "props";
                    if (w0.equals(">>Nouns")) {
                        props = WP.noun;
                    } else if (w0.equals(">>Conjunctions")) {
                        props = WP.conj;
                    } else if (w0.equals(">>DetStrong")) {
                        props = WP.dets;
                    } else if (w0.equals(">>DetWeak")) {
                        props = WP.detw;
                    } else if (w0.equals(">>Names")) {
                        props = WP.n;
                    } else if (w0.equals(">>Abbrev")) {
                        props = WP.abbrev;
                    } else if (w0.equals(">>Adjectives")) {
                        props = WP.adj;
                    } else if (w0.equals(">>Prepositions")) {
                        props = WP.prep;
                    } else if (w0.equals(">>ClausePreps")) {
                        props = WP.clprep;
                    } else if (w0.equals(">>QualPreps")) {
                        props = WP.qualprep;
                    } else if (w0.equals(">>Query")) {
                        props = WP.query;
                    } else if (w0.equals(">>ClauseModifiers")) {
                        props = WP.clmod;
                    } else if (w0.equals(">>Adverbs")) {
                        props = WP.adv;
                    } else if (w0.equals(">>Attribution")) {
                        props = WP.attribution;
                    } else {
                        throw new RuntimeException(
                        "MakeVcb. malformed lexicon line: " + line);
                    }
                }
                continue;
            }
            if (state.equals("props")) {
                if (terms.length > 1 && props != WP.abbrev) {
                    // create respell rule mapping multiple tokens to a
                    // single token, then set the props for the single
                    // token
                    SLst slTerms = new SLst(terms);
                    String rhs = slTerms.join(" ");
                    slTerms.append(":");
                    slTerms.append(rhs);
                    rwrulesRaw.add(slTerms.toArray());
                    w0 = rhs;
                }
                define(w0, props, 0);
            } else if (state.equals(">>Verbs")) {
                addVerb(terms);
            } else if (state.equals(">>Contractions") || state.equals(">>Rewrite")) {
                // For the contraction case, mark the word as a
                // contraction.
                if (state.equals(">>Contractions")) {
                    rootKey = vcb.define(w0, WP.contraction, 0, 0);
                }
                rwrulesRaw.add(terms);
            } else if (state.equals(">>PrepVerbs")) {
                addPrepVerbFitness(terms);
            }
        } // end loop over lines
        // define rewrite rules
        defineRewriteRules(vcb.rwrules);
        // assign synlasses
        assignSynclasses();
        return vcb;
    }
} // end class MakeVcb





