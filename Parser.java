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
package msp;
import java.util.*;
import java.io.*;
import msp.util.*;
import msp.lex.Vcb;
import msp.lex.Lexer;
import msp.lex.WP;
import msp.lex.ParseBlk;
import msp.graph.*;
import msp.xfrm.*;

/**
* "buildGraph" in module "Pg" constructs our initial parse graph: a
* doubly-linked list of nodes representing individual words. Parsing
* then procedes in 2 phases: reduction, and syntax relations. In
* reduction, we replace short sequences of nodes with a single node, so
* simple phrases like "the girl" and "didn't go" are parsed as units. In
* syntax relations, we walk the top-level nodes of the graph and
* recognize syntax relations between nodes.
*
* Both phases are implemented using "Xfrms". A transform implements
* one or more rules. It walks the graph until it finds one or more
* nodes to which a rule applies. It then passes the nodes (and/or rule)
* over to its "applyRule" method, which makes some modification to the
* graph and returns the node at which the walk is to resume.
*
* After the parse graph is fully defined, we have a full and complete
* representation of the parse. But parse graph nodes ill suited to an
* API, plus they consume a lot of memory. So the final step is to do a
* top down walk of the parse graph, constructing a new and simplified
* version of the parse using nodes of type Nd.
*/
class Parser {
    // version info
    String version = "1.0";
    // Our components
    Vcb vcb;
    Pg pg;
    PnRE pnRE;
    Lexer lexer;
    List<Xfrm> xfrms = new ArrayList();
    Parser(InputStream mspDat) throws IOException {
        // Create our components
        vcb = new msp.lex.Vcb();
        lexer = new Lexer();
        pg = new Pg(lexer);
        pnRE = new PnRE();
        xfrms.add(new ReductXfrm("init"));
        xfrms.add(new LeftReductXfrm("leftinit"));
        xfrms.add(new LeftReductXfrm("queryhead"));
        xfrms.add(new ReductXfrm("vphr"));
        xfrms.add(new ReductXfrm("detphr"));
        xfrms.add(new ReductXfrm("conj"));
        xfrms.add(new BindPreps("bindPreps"));
        xfrms.add(new SrXfrm("sr"));
        xfrms.add(new QueryXfrm("query"));
        xfrms.add(new SvToQXfrm("svToQ"));
        xfrms.add(new InvertQXfrm("invertQ"));
        xfrms.add(new ValidateSpans("validateSpans"));
        xfrms.add(new InferSubjects("inferSubjects"));
        xfrms.add(new ReduceSrClauses("reduceSrClauses"));
        serialize(mspDat, "r");
    }
    
    void serializeVersion(Serialize serializer) throws IOException {
        if (serializer.mode.equals("r")) {
            String[] versions = serializer.decodeStr().split(" ");
            version = versions[0];
            vcb.version = versions[1];
        } else {
            serializer.encodeStr(String.format(
            "%s %s", version, vcb.version));
        }
    }
    
    // test/dev option
    public void setTraceParse(boolean enable){
        Xfrm.traceparse = enable;
    }
    
    /** get xfrm given name */
    public Xfrm getXfrm(String name){
        for (Xfrm x: xfrms) {
            if (x.name.equals(name)) {
                return x;
            }
        }
        return null;
    }
    
    void serialize(Object mspDat, String mode) throws IOException {
        Serialize serializer = new Serialize(mspDat, mode);
        serializeVersion(serializer);
        vcb.serialize(serializer);
        for (Xfrm x : xfrms) {
            x.serialize(serializer);
        }
        serializer.fini();
    }
    
    /** print parser rules */
    public void printme(){
        try {
            PrintStream fp = new PrintStream(new File("msp.lst"));
            for (Xfrm x: xfrms) {
                x.printme(fp);
            }
            fp.close();
        } catch (IOException e) {
        }
    }
    
    /**
    * Parse source -- either a file or a str (but not both).
    * If "delegate" is None, we return a list of parse nodes
    * giving the parse. If delegate is defined, we read and
    * parse the source in sections, passing the parse of each
    * section over to the delegate for processing.
    * This is the main entry function for parsing.
    */
    public List<Nd> parseSrc(Object contentProvider,
    IProcessFile delegate, int maxlines) throws IOException{
        // The parse is a list of parse nodes
        List<Nd>nds = new ArrayList<>();
        // we parse in sections
        Source src = new Source(contentProvider);
        while (src.getSection()) {
            List<ParseBlk> blklst =
            lexer.getParseBlks(src.sectText, src.sectLno);
            PnLst pnlst = parseBlklst(blklst, null);
            nds.addAll(getParseNodes(pnlst, null, -1));
            // If a delegate is defined, pass the node collection
            // over the processing and start over.
            if (delegate != null &&
            src.lno - src.sectLno > maxlines) {
                // process the nodes, then start a new section
                delegate.processParse(nds);
                nds = new ArrayList();
            }
        }
        return nds;
    }
    
    /** parse a list of blocks. Returns a list of Pn's. */
    public PnLst parseBlklst(List<ParseBlk> blklst, Pn parent){
        PnLst pnlst = new PnLst();
        for (ParseBlk blk: blklst) {
            if (blk.sublst.size() > 0) {
                // A quote or parenthesized text. Create appropriate
                // container node
                int sc = vcb.lkupSc("QuoteBlk");
                if (blk.bracket.equals("(")) {
                    sc = vcb.lkupSc("ParenBlk");
                }
                Pn pn = new Pn(-1, blk.S, blk.E);
                pn.sc = sc;
                pnlst.append(pn);
                pn.sublst = parseBlklst(blk.sublst, pn);
            } else {
                // parse and add nodes to "pnds".
                pnlst.extend(parseBlk(blk));
            }
        }
        // rewrite "pnLst" to get attributions
        pnlst = Attributions.set(pnRE, pnlst);
        return pnlst;
    }
    
    /** parse a block */
    public PnLst parseBlk(ParseBlk blk) {
        pg.buildGraph(blk);
        if (Xfrm.traceparse) {
            pg.printme("initial graph");
        }
        for (Xfrm x : xfrms) {
            x.doXfrm();
            if (Xfrm.traceparse) {
                pg.printme(String.format("Post %s:", x.name));
            }
        }
        return pg.getRootNodes();
    }
    
    /** get the "kind" attribute for a parse node */
    public int getNdKind(Pn e, int form){
        String scSp = vcb.spellSc(e.sc);
        if (scSp.equals("QuoteBlk")) {
            return NdKind.quote;
        }
        if (scSp.equals("ParenBlk")) {
            return NdKind.paren;
        }
        if (e.checkSc(WP.punct)) {
            return NdKind.punct;
        }
        if (form == NdForm.queryclause ||
        form == NdForm.queryphrase ||
        form == NdForm.queryword) {
            return NdKind.query;
        }
        if (e.isVerb()) {
            PnLst sub = e.getSubnodes(
            new int[]{SR.agent, SR.topic, SR.exper});
            if (sub.N > 0) {
                // something is in a subject role.
                if (sub.a[0].checkSc(WP.query)) {
                    return NdKind.query;
                }
                if (e.rel[SR.vadj].N > 0 &&
                e.rel[SR.vadj].a[0].testVRoot("let")) {
                    return NdKind.imper;
                }
                if (!e.checkVp(msp.lex.VP.gerund)) {
                    return NdKind.assertion;
                }
                } else if (e.vIsoSub != null &&
                e.vIsoSub.msnode != null) {
                    // pick it up from our peer
                    Nd peer = (Nd)e.vIsoSub.msnode;
                    return peer.kind;
                } else if (e.checkVp(msp.lex.VP.root)) {
                    return NdKind.imper;
                    } else if (e.checkVp(msp.lex.VP.passive) &&
                    e.rel[SR.theme].N > 0) {
                        return NdKind.assertion;
                    }
                }
                // return default
                return NdKind.x;
            }
            
            /** get the "form" attribute for a parse node */
            public int getNdForm(Pn e, String text){
                if (e.isContainer()) {
                    return NdForm.x;
                }
                if (e.checkSc(WP.punct)) {
                    if (text.equals(".") ||
                    text.equals("?") ||
                    text.equals("!") ||
                    text.equals(":") ||
                    text.equals(";")) {
                        return NdForm.terminator;
                    } else if (text.equals(", ")) {
                        return NdForm.comma;
                    }
                    return NdForm.x;
                }
                if (e.isVerb()) {
                    // "sub": set of terms in subject clause
                    PnLst sub = e.getSubnodes(
                    new int[]{SR.agent, SR.topic, SR.exper});
                    if (e.vIsoSub != null) {
                        // this is subject-verb
                        return NdForm.verbclause;
                    } else if (e.checkVp(msp.lex.VP.query)) {
                        // explicitly marked as query
                        return NdForm.queryclause;
                    } else if (sub.N == 0) {
                        if (e.checkVp(
                        msp.lex.VP.gerund|
                        msp.lex.VP.inf|
                        msp.lex.VP.root)) {
                            return NdForm.action;
                        }
                    }
                    // default is "verb-clause"
                    return NdForm.verbclause;
                }
                if (e.wrds.N == 1) {
                    // a word. Default is "X", but look for useful cases.
                    int wrd = e.getWrd(0);
                    if (vcb.checkProp(wrd, WP.query)) {
                        return NdForm.queryword;
                    }
                    if (vcb.checkProp(wrd, WP.n|WP.detw)) {
                        return NdForm.n;
                    }
                    if (vcb.checkProp(wrd, WP.conj)) {
                        return NdForm.conjword;
                    }
                    if (vcb.checkProp(wrd, WP.mod)) {
                        return NdForm.mod;
                    }
                    // use default
                    return NdForm.x;
                }
                // a phrase. possessive? ("John's cat")
                if (e.wrds.contains(vcb.lkup("'s", false))) {
                    return NdForm.n;
                }
                // compound modifier? ("very happy", "sad and miserable")
                boolean isMod = true;
                ILstIterator iter = e.wrds.getIterator();
                while (iter.hasNext()) {
                    if (!vcb.checkProp(iter.next(), WP.mod|WP.conj)) {
                        isMod = false;
                        break;
                    }
                }
                if (isMod) {
                    return NdForm.mod;
                }
                // conjunction phrase? ("boys and girls")
                iter = e.wrds.getIterator();
                while (iter.hasNext()) {
                    if (vcb.checkProp(iter.next(), WP.conj)) {
                        return NdForm.conjphrase;
                    }
                }
                // remaining tests based on first word
                int wrd = e.getWrd(0);
                if (vcb.checkProp(wrd, WP.query)) {
                    // "how many", "what time"
                    return NdForm.queryphrase;
                }
                if (vcb.checkProp(wrd, WP.dets|WP.detw)) {
                    return NdForm.n;
                }
                // default
                return NdForm.x;
            }
            
            /** Helper for getParseNodes */
            public int remapSr(int sr){
                if (sr == SR.agent) {
                    return NdKind.agent;
                }
                if (sr == SR.topic) {
                    return NdKind.topic;
                }
                if (sr == SR.exper) {
                    return NdKind.exper;
                }
                if (sr == SR.theme) {
                    return NdKind.theme;
                }
                if (sr == SR.auxtheme) {
                    return NdKind.auxtheme;
                }
                if ((sr == SR.modifies) || (sr == SR.ladj)){
                    return NdKind.qual;
                }
                if (sr == SR.attribution) {
                    return NdKind.attribution;
                }
                return -1;
            }
            
            /** Helper for getParseNodes */
            public int remapVp(int v){
                int msv = 0;
                if ((v & msp.lex.VP.neg) != 0) {
                    msv |= msp.VP.neg;
                }
                if ((v & msp.lex.VP.past) != 0) {
                    msv |= msp.VP.past;
                }
                if ((v & msp.lex.VP.present) != 0) {
                    msv |= msp.VP.present;
                }
                if ((v & msp.lex.VP.future) != 0) {
                    msv |= msp.VP.future;
                }
                if ((v & msp.lex.VP.subjunctive) != 0) {
                    msv |= msp.VP.subjunctive;
                }
                if ((v & msp.lex.VP.perfect) != 0) {
                    msv |= msp.VP.perfect;
                }
                return msv;
            }
            
            /**
            * This method accepts a list of graph nodes, and returns a
            * corresponding list of parse nodes.
            */
            public List<Nd> getParseNodes(PnLst lst, Nd parent, int sr){
                List<Nd> nds = new ArrayList();
                //for e in lst:
                PnLstIterator iter = lst.getIterator();
                while (iter.hasNext()) {
                    Pn e = iter.next();
                    if (e.msnode != null) {
                        nds.add((Nd)(e.msnode));
                        continue;
                    }
                    // create a parse node and add to "nds".
                    String text = e.isVerb()?
                    lexer.getSrcSubstr(e.S, e.E - e.S + 1): vcb.spell(e.wrds);
                    int form = getNdForm(e, text);
                    int kind;
                    if (sr != -1) {
                        assert parent != null;
                        kind = remapSr(sr);
                        assert kind != -1;
                    } else {
                        kind = getNdKind(e, form);
                    }
                    Nd nd = new Nd(kind, form, text, parent);
                    e.msnode = nd;
                    nds.add(nd);
                    // get content for containder nodes (quotes and parens)
                    if (e.isContainer()) {
                        nd.subnodes.addAll(getParseNodes(e.sublst, nd, -1));
                    }
                    // get subnodes
                    for (int i=0; i<SR.nwordtoverb; i++) {
                        if (e.rel[i].N > 0 && remapSr(i) != -1) {
                            nd.subnodes.addAll(getParseNodes(e.rel[i], nd, i));
                        }
                    }
                    nd.head = vcb.spell(e.head);
                    nd.vroots = vcb.spell(e.verbs);
                    nd.vqual = vcb.spell(e.vqual);
                    nd.adverbs = vcb.spell(e.adverbs);
                    if (e.vprops != 0) {
                        if (form != NdForm.action) {
                            nd.vprops = remapVp(e.vprops);
                        }
                    }
                    nd.lineS = lexer.lnoMap.a[e.S];
                    nd.colS = lexer.colMap.a[e.S];
                    nd.lineE = lexer.lnoMap.a[e.E];
                    nd.colE = lexer.colMap.a[e.E];
                }
                return nds;
            }
            
            // test/dev: print out the parse tables. Expects "msp.dat" to reside in same
            // dir as contains this class file.
            public static void main(String[] args) {
                try {
                    String path = Parser.class.getResource("Parser.class").getPath();
                    String fn = path.replace("Parser.class", "msp.dat");
                    InputStream mspDat = new FileInputStream(fn);
                    Parser p = new Parser(mspDat);
                    p.printme();
                    mspDat.close();
                } catch (IOException e) {
                    throw new RuntimeException("UtMsp failure", e);
                }
            }
            
        }
        
        
        
        
