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

package msp.xfrm;
import java.util.*;
import java.io.*;
import msp.util.*;
import msp.lex.*;
import msp.graph.*;



/**
* This code establishes syntax relations. The parse graph consists
* of a linked list of parse nodes (class "Pn"), representing words
* and punctuation. Each node has a "sc"(syntax class) attribute:
* this is our generalization of part-of-speach. At this point the
* graph is a simple linear sequence of nodes. We now
* create the tree structure by assigning each node an "sr" (syntax
* relation) attribute. This is an int value encoding the pair
* (syntax-relation, parent-spec). "syntax-relation"
* is an enumerator drawn from the SR_xxx constants. "parent-spec"
* specifies the node to which this relation obtains.
*
* Here's an overview of the key concepts.
*
* SrRegion, SrMap, scseq and srseq:
* An SrRegion is a sequence of nodes (in the parse graph)
* that may be syntactically related. Each node
* has an sc attribute, so the node sequence defines a sequence
* of sc-values. This is called an "scseq". Given an scseq, our task
* is to find a parallel sequence of "sr-values". An sr-value specifies
* a node's parent in the parse tree, and what relation
* obtains between that node and its parent. A sequence of sr-values
* is called an "srseq".
*
* We treat this task as a mapping: given some arbitrary scseq,
* find the best corresponding srseq. To emphasize that this is a
* mapping relation, we use "x_i" to designate some specific scseq,
* and "y_i" to designate some specific srseq. "X" means a set
* of x values (that is, a set of scseq's); "Y" means a set of
* y values (that is, a set of srseq's). So we're concerned with
* a set of mappings, from X to Y. This set of mappings is called
* an "SrMap". In our parse model, these mappings are many-to-1.
* So a given x maps to exactly one y, and multiple x's can map
* to the same y.
*
* Parse Model:
* Our parse model breaks the nodes of the parse graph up into
* 3 regions, designated 0, 1, and 2. Each has an associated SrMap.
* SrMap1 describes the region [subject+rootVerb]. SrMap2 describes
* the region [rootVerb+object]. SrMap0 describes the optional
* prelude that can appear before the subject in an SVO structure
* ("[On Monday] we saw mermaids"; "[When did] she leave?"). Note
* how the 3 regions compose (fit together): region1 immediately
* follows region0, but region1 and region2 overlap by one node
* (the root node of the tree). This kind of composition
* defines the algebra of our syntax.
*
* For some given SrRegion, there will in general be multiple ways
* to decompose it into x's. Suppose there are 2 possible compositions.
* We must choose between them. The rule is:
* 1. If one composition is longer than the other, choose that one.
* 2. If they are equal in length, choose the one with greater composit
* weight. Each mapping x->y has an associated weight, which gives the
* frequency with which that mapping was observed over a large corpus.
* Since weights are probabilities, the weight assigned to the
* composition (x1_i, x2_j) is weight(x1_i) * weight(x2_j).
*/

/** Mapping, seq -> index */
class SeqDct {
    String name;
    HashMap<String, Integer> dct = new HashMap<>();
    LstILst sequences = new LstILst();
    public SeqDct(String name){
        this.name = name;
        // index 0 is reserved for the null (empty) sequence
        sequences.append(null);
        dct.put("_null_", 0);
    }
    public void serialize(Serialize serializer) throws IOException {
        if (serializer.mode.equals("w")) {
            serializer.encodeLstlst(sequences, 16);
        } else {
            sequences = serializer.decodeLstlst(16);
            sequences.a[0] = new ILst();
            //for i in range(1, sequences.N):
            for (int i=1; i<sequences.N; i++) {
                ILst seq = sequences.a[i];
                SLst l = new SLst();
                for (int j=0; j<seq.N; j++) {
                    l.append(Integer.toString(seq.a[j]));
                }
                String key = l.join(" ");
                dct.put(key, i);
            }
        }
    }
    public int getN(){
        // return number of sequences. This does NOT include the
        // null (empty) sequence that is always included in the
        // in the dictionary as entry0.
        return sequences.N-1;
    }
    public String seqToStr(ILst seq) {
        // create a dump for the sequence: overwritten in
        // child classes. Here we just write out the elements
        // as int's.
        return seq.toString();
    }
    public void printme(){
        System.out.printf("\n--> %s\n", name);
        for (int i =0; i<sequences.N; i++) {
            ILst seq = sequences.a[i];
            System.out.printf("%d. %s %s\n",
            i, seq.toString(), seqToStr(seq));
        }
    }
    public ILst getSeq(int i){
        return sequences.a[i];
    }
    public int getLenSeq(int i){
        return sequences.a[i].N;
    }
    
}

/** Mapping, scseq -> index */
class ScSeqDct extends SeqDct{
    ScSeqDct(String name) {
        super(name);
    }
    public String seqToStr(ILst seq) {
        return Vcb.vcb.spellSc(seq);
    }
}

/** Mapping, srseq -> index */
class SrSeqDct extends SeqDct{
    SrSeqDct(String name) {
        super(name);
    }
    public String seqToStr(ILst seq) {
        return SR.srEncTostr(seq);
    }
}

/** FSM for SrMap. We override the "print" methods to get better listings */
class SrFSM extends FSM {
    SrMap srmap;
    SrFSM(int nbitsSeqTerm, boolean leftToRight, SrMap srmap){
        super(nbitsSeqTerm, leftToRight);
        this.srmap = srmap;
    }
    public void printMatch(PnLstVPair m) {
        ILst scseq = new ILst();
        PnLstIterator iter = m.pnLst.getIterator();
        while (iter.hasNext()) {
            scseq.append(iter.next().sc);
        }
        String scseqSp = Vcb.vcb.spellSc(scseq);
        ILst srseq = srmap.ydct.sequences.a[ srmap.xToY.a[m.v] ];
        String srseqSp = SR.srEncTostr(srseq);
        System.out.printf("%s -> %s\n", scseqSp, srseqSp);
    }
    public void printme(PrintStream fp){
        if (fp == null) {
            fp = System.out;
        }
        for (int i=0; i<states.N; i++) {
            HashSet<Integer> iset = (HashSet<Integer>)states.a[i];
            if (iset.size() == 0) {
                continue;
            }
            ILst tmp = new ILst();
            for (int v : iset) {
                tmp.append(v);
            }
            tmp.sort();
            fp.printf("state %d. ", i);
            fp.printf("inputs: %s\n", tmp.toString() );
            fp.printf(" %s\n", Vcb.vcb.spellSc(tmp) );
        }
        // seq->v
        fp.print("mappings. scseq->srseq:\n");
        List<String> lst = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : seqToV.entrySet()) {
            String key = entry.getKey();
            if (key.equals("_null_")) {
                continue;
            }
            int v = entry.getValue();
            ILst scseq = new ILst();
            for (String term : key.split(" ")) {
                scseq.append(Integer.parseInt(term));
            }
            String scseqSp = Vcb.vcb.spellSc(scseq);
            int y = srmap.xToY.a[v];
            if (y == 0) {
                // skip: sequence not used in this map
                continue;
            }
            ILst srseq = srmap.ydct.sequences.a[y];
            String srseqSp = SR.srEncTostr(srseq);
            String hd = String.format("%s -> %s\n", scseqSp, srseqSp);
            lst.add(hd +
            String.format(" %s -> %s\n", key, srseq.toString()));
        }
        java.util.Collections.sort(lst);
        for (String s: lst) {
            fp.print(s);
        }
    }
}

class SrMap {
    // For listings and traces
    String name;
    // finite state machine for recognizing scseq's
    SrFSM fsm;
    // mapping, scseq -> best (most likely)srseq
    ILst xToY = new ILst();
    // weight assigned to this mapping
    ILst w = new ILst();
    // sequence dictionaries
    SeqDct ydct;
    SeqDct xdct;
    SrMap(String name, boolean fsmLeftToRight, SeqDct xdct, SeqDct ydct){
        this.name = name;
        fsm = new SrFSM(8, fsmLeftToRight, this);
        this.xdct = xdct;
        this.ydct = ydct;
    }
    public void serialize(Serialize serializer) throws IOException{
        fsm.serialize(serializer);
        if (serializer.mode.equals("w")) {
            serializer.encodeIntlst(xToY, 16);
            serializer.encodeIntlst(w, 16);
        } else {
            xToY = serializer.decodeIntlst(16);
            w = serializer.decodeIntlst(16);
        }
    }
    public void printme(PrintStream fp){
        if (fp == null) {
            fp = System.out;
        }
        fp.printf("%s:\n", name);
        for (int i=0; i<w.N; i++) {
            if (w.a[i] == 0) {
                continue;
            }
            ILst seq = xdct.sequences.a[i];
            fp.printf("x%d. %s %s\n",
            i, seq.toString(), Vcb.vcb.spellSc(seq));
            seq = ydct.sequences.a[ xToY.a[i] ];
            fp.printf("y%d. %s %s\n",
            i, seq.toString(), SR.srEncTostr(seq));
            fp.printf("Weight: %d\n\n", w.a[i]);
        }
        // enable this code to print the FSM
        if (true) {
            fp.printf("begin %s FSM\n", name);
            fsm.printme(fp);
            fp.printf("end %s FSM\n", name);
        }
    }
    
    public int getN(){
        int N = 0;
        for (int i=0; i<w.N; i++) {
            if (w.a[i] > 0) {
                N += 1;
            }
        }
        return N;
    }
    public double getW(int x){
        return ((double)w.a[x])/((double)(0xffff));
    }
}

// An "X" value, plus its mapping to Y
class ParseTerm{
    int x;
    SrMap srmap;
    ParseTerm(int x, SrMap srmap){
        this.x = x;
        this.srmap = srmap;
    }
}

class ParseRec{
    // Sequence of syntax-class values: the source for the
    // parse.
    ILst scseq;
    // index of root
    int ixroot;
    // The parse conists of a left and right side, meeting at the
    // root. These sides are represented as lists of ParseTerms.
    List<ParseTerm> left = new ArrayList<ParseTerm>();
    List<ParseTerm> right = new ArrayList<ParseTerm>();
    // Length of the parse (number of terms, in scseq, covered
    // by this parse).
    int _len = 0;
    // weight assigned to this value (0 .. 1.0)
    double w = 0.0;
    ParseRec(ILst scseq, int ixroot) {
        this.scseq = scseq;
        this.ixroot = ixroot;
    }
}

/**
* This code established syntax relations. The parse graph
* consists of a linked list of parse nodes (class "Pn"),
* representing words and punctuation. Each node has a "sc"
* (syntax class) attribute: this is our generalization of
* part-of-speach. In this phase of the parse, we assign an
* "sr" (syntax relation) to each node.
*/

public class SrXfrm extends Xfrm {
    // debug toggles
    boolean trace = false;
    boolean traceBest = false;
    // mappings, x and y sequences -> index
    SeqDct xdct = new ScSeqDct("srxfrm xdct");
    SeqDct ydct = new SrSeqDct("srxfrm ydct");
    // The parse maps
    SrMap srmap[] = new SrMap[5];
    public SrXfrm(String name){
        super(name);
        srmap[0] = new SrMap("prelude", false, xdct, ydct);
        srmap[1] = new SrMap("chain", false, xdct, ydct);
        srmap[2] = new SrMap("subv", false, xdct, ydct);
        srmap[3] = new SrMap("vobj", true, xdct, ydct);
        srmap[4] = new SrMap("postlude", true, xdct, ydct);
    }
    
    public void serialize(Serialize serializer) throws IOException{
        xdct.serialize(serializer);
        ydct.serialize(serializer);
        for (SrMap p: srmap) {
            p.serialize(serializer);
            if (serializer.mode.equals("r")) {
                p.fsm.seqToV = xdct.dct;
            }
        }
    }
    public void printstats(PrintStream fp, String title){
        if (fp == null) {
            fp = System.out;
        }
        if (title != null) {
            fp.printf("\n** %s **\n" , title);
        }
        int nX = xdct.getN();
        fp.printf("N X-elements (total): %d\n" , nX);
        //for srm in srmap:
        for (SrMap srm: srmap) {
            fp.printf("N X %s: %d\n" , srm.name, srm.getN());
        }
        fp.printf("N Y-elements: %d\n" , ydct.getN());
    }
    public void printme(PrintStream fp){
        if (fp == null) {
            fp = System.out;
        }
        fp.printf("Xfrm %s\n" , name);
        for (SrMap srm : srmap) {
            srm.printme(fp);
        }
    }
    public int sumPathLen(List<ParseTerm> path){
        int s = 0;
        //for e in path:
        for (ParseTerm e : path) {
            if (e.x != 0) {
                s += xdct.getLenSeq(e.x);
            }
        }
        return s;
    }
    public double getPathW(List<ParseTerm> path){
        double w = 1.0;
        //for i in range(0, path.N):
        for (ParseTerm e: path) {
            if (e.x != 0) {
                w *= e.srmap.getW(e.x);
            }
        }
        return w;
    }
    public List<ParseTerm> getBestPath(List<Object> paths){
        List best = (List<ParseTerm>)paths.get(0);
        int l = sumPathLen(best);
        double w = getPathW(best);
        //for i in range(1, paths.N):
        for (int i=1; i<paths.size(); i++) {
            List p = (List<ParseTerm>)paths.get(i);
            int L = sumPathLen(p);
            double W = getPathW(p);
            if ((L > l) ||
            ((L == l) && (W > w))) {
                best = p;
                l = L;
                w = W;
            }
        }
        return best;
    }
    public void printPath(List<ParseTerm> path){
        double w = getPathW(path);
        w *= 0xffff;
        SLst tmp = new SLst();
        tmp.append(String.format("w: %.1f", w));
        //for e in path:
        for (ParseTerm e: path) {
            ILst scseq = xdct.sequences.a[e.x];
            String xtostr = vcb.spellSc(scseq);
            int y = e.srmap.xToY.a[e.x];
            String ytostr = SR.srEncTostr( ydct.sequences.a[y] );
            w = e.srmap.getW(e.x);
            w*= 0xffff;
            tmp.append(String.format(
            "[(%s)%d. w=%.1f: %s -> %d. %s]",
            e.srmap.name, e.x, w, xtostr, y, ytostr));
        }
        System.out.println(tmp.join("\n"));
    }
    public void printPathset(List<Object> pathset, String title){
        if (title != null) {
            System.out.println(title);
        }
        if (pathset.size() == 0) {
            System.out.println("empty pathset");
            return;
        }
        for (int i=0; i<pathset.size(); i++) {
            System.out.println("-----");
            printPath((List<ParseTerm>)pathset.get(i));
            if (i == pathset.size()-1) {
                System.out.println("-----");
            }
        }
    }
    public void printParseRec(ParseRec pr, String title){
        if (title != null) {
            System.out.println(title);
        }
        System.out.println(String.format(
        "ixroot: %d", pr.ixroot ));
        System.out.println(String.format("scseq: %s", pr.scseq.toString()));
        System.out.println(String.format(" : %s", vcb.spellSc(pr.scseq) ));
        System.out.println("left:");
        printPath(pr.left);
        System.out.println("right:");
        printPath(pr.right);
        System.out.println(String.format("len: %d", pr._len ));
        System.out.println(String.format("w X 0xffff: %f", pr.w ));
    }
    public boolean pathsetContains(List<Object> pathset, List<ParseTerm> p){
        //for px in pathset:
        for (Object e: pathset) {
            List<ParseTerm> px = (List<ParseTerm>)e;
            if (px.size() != p.size()) {
                continue;
            }
            boolean equals = true;
            //for i in range(0, p.N):
            for (int i=0; i<p.size(); i++) {
                if (p.get(i).x != px.get(i).x) {
                    equals = false;
                    break;
                }
            }
            if (equals) {
                return true;
            }
        }
        return false;
    }
    public void extendLeft(List<Object> paths,
    ILst scseq, int ixroot, SrMap srmap){
        if (trace) {
            System.out.println(String.format(
            "extend left %s.", srmap.name));
            printPathset(paths, "pre-extend");
        }
        //for p in paths:
        List<Object> delta = new ArrayList<Object>();
        for (Object e: paths) {
            List<ParseTerm> p = (List<ParseTerm>)e;
            int l = sumPathLen(p);
            ILst xSet = srmap.fsm.getSequences(scseq, ixroot-l);
            for (int i=0; i<xSet.N; i++) {
                int x = xSet.a[i];
                if (srmap.getW(x) == 0.0) {
                    continue;
                }
                List<ParseTerm> newPath = new ArrayList<ParseTerm>();
                newPath.add(new ParseTerm(x, srmap));
                newPath.addAll(p);
                if (!pathsetContains(paths, newPath)) {
                    delta.add(newPath);
                }
            }
        }
        paths.addAll(delta);
        if (trace) {
            printPathset(paths, "post-extend");
            System.out.println("");
        }
    }
    public void extendRight(List<Object> paths,
    ILst scseq, int ixroot, SrMap srmap){
        if (trace) {
            System.out.println(String.format(
            "extend right %s.", srmap.name ));
            printPathset(paths, "pre-extend");
        }
        List<Object> delta = new ArrayList<Object>();
        for (Object e: paths) {
            List<ParseTerm> p = (List<ParseTerm>)e;
            int l = sumPathLen(p);
            ILst xSet = srmap.fsm.getSequences(scseq, ixroot+l);
            for (int i=0; i<xSet.N; i++) {
                int x = xSet.a[i];
                if (srmap.getW(x) == 0.0) {
                    continue;
                }
                List<ParseTerm> newPath = new ArrayList<ParseTerm>();
                newPath.addAll(p);
                newPath.add(new ParseTerm(x, srmap));
                delta.add(newPath);
            }
        }
        paths.addAll(delta);
        if (trace) {
            printPathset(paths, "post-extend");
            System.out.println("");
        }
    }
    public ParseRec _getSrseq(ILst scseq, int ixroot){
        // X-domaine id's
        int XRidPrelude = 0;
        int XRidChain = 1;
        int XRidSubv = 2;
        int XRidVobj = 3;
        int XRidPostlude = 4;
        ParseRec best = new ParseRec(scseq, ixroot);
        ILst subvSet = srmap[XRidSubv].fsm.getSequences(scseq, ixroot);
        ILst vobjSet = srmap[XRidVobj].fsm.getSequences(scseq, ixroot);
        if ((subvSet.N) == 0 || (vobjSet.N == 0)) {
            return best;
        }
        // parse left from scseq[ixroot]
        List<Object> paths = new ArrayList<Object>();
        for (int i=0; i<subvSet.N; i++) {
            int x = subvSet.a[i];
            List p = new ArrayList<ParseTerm>();
            p.add(new ParseTerm(x, srmap[XRidSubv]));
            paths.add(p);
        }
        // make 2 extensions for the chain domaine
        extendLeft(paths, scseq, ixroot, srmap[XRidChain]);
        extendLeft(paths, scseq, ixroot, srmap[XRidChain]);
        extendLeft(paths, scseq, ixroot, srmap[XRidPrelude]);
        best.left = getBestPath(paths);
        // parse right from scseq[ixroot]
        paths = new ArrayList<Object>();
        for (int i=0; i<vobjSet.N; i++) {
            int x = vobjSet.a[i];
            List p = new ArrayList<ParseTerm>();
            p.add(new ParseTerm(x, srmap[XRidVobj]));
            paths.add(p);
        }
        extendRight(paths, scseq, ixroot, srmap[XRidPostlude]);
        best.right = getBestPath(paths);
        // set length and weight of the parse
        best._len = sumPathLen(best.left)+
        sumPathLen(best.right) - 1;
        best.w = getPathW(best.left) *
        getPathW(best.right);
        return best;
    }
    public ILst getSrseq(ILst scseq){
        ParseRec best = new ParseRec(scseq, -1);
        for (int ixroot=0; ixroot < scseq.N; ixroot++) {
            if (!vcb.isScForVerb(scseq.a[ixroot])) {
                continue;
            }
            ParseRec _best = _getSrseq(scseq, ixroot);
            if (_best._len > best._len ||
            (_best._len == best._len && _best.w > best.w)) {
                best = _best;
                if (traceBest) {
                    printParseRec(best, "***\nSet best:");
                }
            }
        }
        ILst srseq = new ILst(scseq.N, 0xff);
        if (best.w == 0.0) {
            // the parse failed
            return srseq;
        }
        // define left region of the parse
        int i = best.ixroot;
        //for ixPath in range(best.left.N-1, -1, -1):
        for (int ixPath = best.left.size()-1; ixPath >= 0; ixPath--) {
            ParseTerm e = best.left.get(ixPath);
            int y = e.srmap.xToY.a[e.x];
            ILst srseqDelta = ydct.getSeq(y);
            //for j in range(srseqDelta.N - 1, -1, -1):
            for (int j = srseqDelta.N-1; j >= 0; j--) {
                srseq.a[i] = srseqDelta.a[j];
                i -= 1;
            }
        }
        // define right region of the parse
        i = best.ixroot;
        //for ixPath in range(0, best.right.N):
        for (int ixPath = 0; ixPath < best.right.size(); ixPath++) {
            ParseTerm e = best.right.get(ixPath);
            int y = e.srmap.xToY.a[e.x];
            ILst srseqDelta = ydct.getSeq(y);
            //for j in range(0, srseqDelta.N):
            for (int j = 0; j < srseqDelta.N; j++) {
                srseq.a[i] = srseqDelta.a[j];
                i += 1;
            }
        }
        // parse is defined over the sub-sequence S..E. Indices
        // follow python convention: S inclusive, E exclusive.
        int S = best.ixroot - sumPathLen(best.left) + 1;
        int E = best.ixroot + sumPathLen(best.right);
        validateScopes(scseq, srseq, S, E);
        return srseq;
    }
    
    /**
    * Correct srseq[S..E] so scope encoding are correct.
    * TODO: document
    */
    public void validateScopes(ILst scseq, ILst srseq, int S, int E){
        // get "ixroot": index of parse tree root. This is the
        // lefttmost unscoped verb.
        int ixroot = -1;
        //for i in range(S, E):
        for (int i=S; i<E; i++) {
            if (vcb.isScForVerb(scseq.a[i]) && srseq.a[i] == 0xff) {
                ixroot = i;
                break;
            }
        }
        if (ixroot == -1) {
            // no action
            return;
        }
        //for i in range(S, E):
        for (int i=S; i<E; i++) {
            // scope encoded: low 4 bits of srseq[i]. We want
            // scope undefined (by convention, 0xf)
            int scopeEnc = srseq.a[i] & 0xf;
            if ((i == ixroot) || (scopeEnc != 0xf)) {
                continue;
            }
            // resolve scope for term "i"
            int nV = 0;
            int sign = 0;
            if (i < ixroot) {
                // scope node is to the right of "i"
                int j = i+1;
                while (j < ixroot) {
                    if (vcb.isScForVerb(scseq.a[j])) {
                        nV += 1;
                    }
                    j += 1;
                }
            } else {
                // scope node is to the left of "i"
                sign = 1;
                int j = i-1;
                while (j > ixroot) {
                    if (vcb.isScForVerb(scseq.a[j])) {
                        nV += 1;
                    }
                    j -= 1;
                }
            }
            // rel encoded is hi 4 bits of srseq[i]. If this is
            // undefined (0xf), we change it to SR_theme. This is
            // the convention for scope chains.
            int rel = 0xf & (srseq.a[i] >> 4);
            if (rel == 0xf) {
                rel = SR.theme;
            }
            srseq.a[i] = (rel << 4) | ((sign<<3) | nV);
        }
    }
    
    /**
    * for i_th term in list of terms, find it scope node
    * (a verb) as encoded in "srseq[i]"
    */
    public Pn findV(int i, PnLst terms, ILst srseq){
        int sr = srseq.a[i];
        // sign/mag encoding.
        int scopeSign = (0x8 & sr) >> 3;
        int scopeMag = 0x7 & sr;
        int nV = 0;
        if (scopeSign == 0) {
            // search right
            int j = i+1;
            while (j < terms.N) {
                Pn ex = terms.a[j];
                if (ex.isVerb()) {
                    nV += 1;
                    if (nV > scopeMag) {
                        return ex;
                    }
                }
                j += 1;
            }
        } else {
            // search left
            int j = i-1;
            while (j >= 0) {
                Pn ex = terms.a[j];
                if (ex.isVerb()) {
                    nV += 1;
                    if (nV > scopeMag) {
                        return ex;
                    }
                }
                j -= 1;
            }
        }
        return null;
    }
    
    /** get extended sc for node */
    public int getExtSc(Pn e){
        if (e.isVerb()) {
            // query heads are retained as is
            if (e.checkSc(WP.qhead|WP.beqhead)) {
                return e.sc;
            }
            // root form...
            String spSc = "V";
            if (e.testVRoot("be")) {
                spSc = "be";
            } else if (e.checkVp(VP.inf)) {
                spSc = "Inf";
            } else if (e.checkVp(VP.gerund)) {
                spSc = "Ger";
            } else if (e.checkVp(VP.participle)) {
                spSc = "Part";
            } else if (e.checkVp(VP.passive)) {
                spSc = "Pas";
            }
            // extension
            if (e.testVForm(VP.avgt)) {
                spSc += "AVGT";
            } else if (e.testVForm(VP.ave)) {
                spSc += "AVE";
            } else if (e.testVForm(VP.evt)) {
                spSc += "EVT";
            }
            int scExt = vcb.lkupSc(spSc);
            return scExt;
        }
        int scProps = vcb.scDct.getProps(e.sc);
        if (scProps == WP.n || scProps == WP.noun) {
            return vcb.lkupSc("X");
        }
        if (vcb.spellSc(e.sc).equals("her")) {
            return vcb.lkupSc("X");
        }
        return e.sc;
    }
    
    /**
    * Get next region of graph, starting at "e", over which we
    * establish syntax relations.
    */
    public PnLst getSrRegion(Pn e){
        // conjunctions and punctuation delimit the regions
        int delim = WP.punct|WP.conj;
        while (e != null) {
            // conjunctions and puctuation delimit the regions
            if (e.checkSc(delim)) {
                e = e.nxt;
                continue;
            }
            PnLst terms = new PnLst();
            terms.append(e);
            // extend to next delimitor
            while (e.nxt != null) {
                if (e.nxt.checkSc(delim)) {
                    break;
                }
                terms.append(e.nxt);
                e = e.nxt;
            }
            return terms;
        }
        return null;
    }
    
    /** Establish syntax relations, node->verb */
    public void doXfrm(){
        PnLst terms = getSrRegion(pg.eS);
        while (terms != null) {
            ILst scseq = new ILst();
            PnLstIterator iter = terms.getIterator();
            while (iter.hasNext()) {
                scseq.append(getExtSc(iter.next()));
            }
            ILst srseq = getSrseq(scseq);
            //for i in range(0, terms.N):
            for (int i=0; i<terms.N; i++) {
                int sr = srseq.a[i];
                if (sr == 0xff) {
                    continue;
                }
                Pn v = findV(i, terms, srseq);
                if (v != null) {
                    // relation is hi 4 bits of "sr"
                    int rel = 0xf & (sr>>4);
                    terms.a[i].setScope(v, rel);
                }
            }
            terms = getSrRegion(terms.a[terms.N-1].nxt);
        }
    }
}




