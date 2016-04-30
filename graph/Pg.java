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
package msp.graph;
import java.util.*;
import msp.util.*;
import msp.lex.*;

/**
* The parse graph ("pg") is the main data structure used in parsing.
* After reading in the source text, we break it up into a sequence of
* lexemes, each corresponding to a word or punctuation mark. Each lexeme
* then becomes a node in the graph. This is a doubly linked list of "Pn"
* nodes. Initially the graph is a 1-dimensional structure.
*
* Parsing then becomes a matter of deciding: what short sequences of
* words ("the girl", "didn't go") can be combined into a single node, to
* form a single parse unit? And then: what are the syntax relations
* between these parsemes? These tasks are handled by the module
* "parser".
*/
public class Pg {
    public static Pg pg;
    Vcb vcb;
    Lexer lexer;
    // first phr in sequence
    public Pn eS;
    // last phr in sequence
    public Pn eE;
    
    public Pg(Lexer lexer) {
        super();
        vcb = Vcb.vcb;
        Pg.pg = this;
        this.lexer = lexer;
    }
    
    // parse node factory
    int pnEnum = 0;
    /** create phrase with given props */
    Pn pnFactory(int tokV, int S, int E){
        Pn e = new Pn(tokV, S, E);
        e.h = pnEnum++;
        return e;
    }
    
    /** reset span of graph, returning restore info */
    public PnLst resetSpan(Pn S, Pn E){
        PnLst rinfo = new PnLst();
        rinfo.append(S.prv);
        rinfo.append(E.nxt);
        rinfo.append(eS);
        rinfo.append(eE);
        eS = S;
        eE = E;
        eS.prv = null;
        eE.nxt = null;
        return rinfo;
    }
    
    /** restore span of graph, using info from "rinfo" */
    public void restoreSpan(PnLst rinfo){
        eS.prv = rinfo.a[0];
        eE.nxt = rinfo.a[1];
        eS = rinfo.a[2];
        eE = rinfo.a[3];
    }
    
    /** print the graph */
    public void printme(String title){
        if (title != null) {
            System.out.println(title);
        }
        Pn e = eS;
        while (e != null) {
            e.printme();
            e = e.nxt;
        }
        System.out.println("");
    }
    
    /**
    * build parse graph for source text in the region specified by
    * "parseblk"
    */
    public void buildGraph(ParseBlk parseblk){
        ILst toks = parseblk.toks;
        ILst tokLoc = parseblk.tokLoc;
        pnEnum = 0;
        eS = eE = null;
        //for i in range(0, toks.N):
        for (int i=0; i<toks.N; i++) {
            // The span of a node gives start and end index of the region
            // in the source text spanned by e.
            int ixS = tokLoc.a[i];
            String sp = vcb.spell(toks.a[i]);
            Pn e = pnFactory(toks.a[i], ixS, ixS+sp.length()-1);
            // linked-list bookkeeping
            if (eS == null) {
                eS = eE = e;
            } else {
                Pn.connect(eE, e);
                eE = e;
            }
        }
    }
    
    /** remove a node from the graph */
    public void removeNode(Pn e){
        if (e == eS && e == eE) {
            eS = eE = null;
        } else if (e == eS) {
            eS = e.nxt;
        } else if (e == eE) {
            eE = e.prv;
        }
        Pn.connect(e.prv, e.nxt);
    }
    
    /**
    * replace nodes S..E with a single node, "R". S..E become the
    * sublist of R. R's "wrds" attribute is the concatenation of the
    * words for S..E. if R is a verb expression, its "verbs" attribute
    * is derived likewise from S..E
    */
    public Pn reduceTerms(Pn S, Pn E, int vprops, int sc){
        Pn R = pnFactory(-1, S.S, E.E);
        R.vprops = vprops;
        R.sc = sc;
        // words for the reduction is the concatenation of the words for
        // eS..eE
        Pn e = S;
        while (true) {
            R.sublst.append(e);
            R.wrds.extend(e.wrds);
            R.verbs.extend(e.verbs);
            if (e == E) {
                break;
            }
            e = e.nxt;
        }
        if (!vcb.isScForVerb(sc)) {
            // kill the verbs
            R.verbs = new ILst();
        }
        // insert R into the region S..E
        Pn left = S.prv;
        Pn right = E.nxt;
        Pn.connect(left, R);
        Pn.connect(R, right);
        if (R.prv == null) {
            eS = R;
        }
        if (R.nxt == null) {
            eE = R;
        }
        return R;
    }
    
    /**
    * The head reduction: terms from S up to (but not including) E
    * are removed the graph; the text content is appended to the
    * "head" attribute of E.
    */
    public void reduceHead(Pn S, Pn E){
        Pn e = S;
        while (e != E) {
            E.head.extend(e.wrds);
            Pn nxt = e.nxt;
            removeNode(e);
            e = nxt;
        }
    }
    
    /**
    * Walk the graph and get all "root" nodes: these are nodes with null
    * scope.
    */
    public PnLst getRootNodes(){
        PnLst rootNds = new PnLst(64);
        Pn e = eS;
        while (e != null) {
            if (e.scope == null) {
                rootNds.append(e);
            }
            e = e.nxt;
        }
        return rootNds;
    }
    
    /**
    * Clear the "rel" attributes of nodes, then recompute using scope
    * and sr attributes.
    */
    public void validateRel(){
        // clear any currently defined relations
        Pn e = eS;
        while (e != null) {
            //for lst in e.rel:
            for (PnLst lst : e.rel) {
                lst.N = 0;
            }
            e = e.nxt;
        }
        // rebuild using scope and sr attributes
        e = eS;
        while (e != null) {
            if (e.scope != null && e.sr < SR.nwordtoverb) {
                e.scope.rel[e.sr].append(e);
            }
            e = e.nxt;
        }
    }
    
    /**
    * Validate the "span" attribute of nodes: if "e" is in the scope of
    * "ex", increase ex's span as needed to include e.
    */
    public void validateSpan(){
        Pn e = eS;
        while (e != null) {
            Pn ex = e.scope;
            // Walk up the scope tree.
            while (ex != null) {
                if (ex.isVerb()) {
                    if (e.S < ex.S) {
                        ex.S = e.S;
                    }
                    if (e.E > ex.E) {
                        ex.E = e.E;
                    }
                }
                ex = ex.scope;
            }
            e = e.nxt;
        }
    }
} // end class Pg



