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
package msp.graph;
import msp.util.*;
import msp.lex.*;
public class Pn {
    Vcb vcb;
    // "handle" -- for debugging purposes
    public int h = -1;
    // span attributes
    public int S = -1;
    public int E = -1;
    // list structure
    public Pn prv;
    public Pn nxt;
    // subnodes for reductions
    public PnLst sublst = new PnLst();
    // our scope
    public Pn scope;
    // verb qualifiers
    public ILst vqual = new ILst();
    // verbs props
    public int vprops;
    // start and end indices for the verb structure
    public int vS = -1;
    public int vE = -1;
    // syntax class and relation.
    public int sc;
    public int sr = SR.undef;
    // text associated with this node
    public ILst wrds = new ILst();
    // verb roots associated with this node
    public ILst verbs = new ILst();
    // adverbs associated with this node
    public ILst adverbs = new ILst();
    // preposition, etc. which precede this node
    public ILst head = new ILst();
    // syntax relations, verb->word
    public PnLst[] rel = new PnLst[SR.nwordtoverb];
    // "vnxt" is first verb to our right, and "vprv" is
    // first verb to our left.
    public Pn vnxt;
    public Pn vprv;
    // preceeding verb domain
    public Pn vd_left;
    // Conjoined verb expressions: if "vIsoSub" is defined,
    // it gives the verb expression whose subject roles
    // provide the subject roles for this node.
    public Pn vIsoSub;
    // final tree generation: the msnode that corresponds to this
    // parse graph node
    public Object msnode;
    public Pn(int tokV, int S, int E){
        this.S = S;
        this.E = E;
        vcb = Vcb.vcb;
        //for i in range(0, SR.nwordtoverb):
        for (int i=0; i<SR.nwordtoverb; i++) {
            rel[i] = new PnLst();
        }
        if (tokV != -1) {
            wrds.append(tokV);
            sc = computeSynclass(tokV);
            if (vcb.isScForVerb(sc)) {
                // "Is" is defined as "is", which is in turn defined
                // as "be".
                int _def = vcb.getDef(tokV);
                _def = vcb.getDef(_def);
                verbs.append(_def);
                vprops = computeVerbProps(tokV);
            }
        }
    }
    
    public static void connect(Pn lhs, Pn rhs){
        if (lhs != null) {
            lhs.nxt = rhs;
        }
        if (rhs != null) {
            rhs.prv = lhs;
        }
    }
    
    /** compute the "sc" value for a node */
    public int computeSynclass(int tokV){
        String sp = vcb.spell(tokV).toLowerCase();
        char c = sp.charAt(0);
        if (sp.equals("'s")) {
            return vcb.lkupSc("TickS");
        }
        if (sp.equals("and") || sp.equals("or")) {
            return vcb.lkupSc("AndOr");
        }
        if (c == ',') {
            return vcb.lkupSc("Comma");
        }
        if (!(Character.isLetterOrDigit(c) || c == '_' || c == '\'')) {
            return vcb.lkupSc("Punct");
        }
        if (Character.isDigit(c)) {
            // numerals lex as weak-determinants: "I saw 123, 000 people"
            return vcb.lkupSc("Num");
        }
        // a vocabulary word
        return vcb.synclass.a[tokV];
    }
    
    /** get verb props (VP_xxx) for a parse node */
    public int computeVerbProps(int tok){
        int p = 0;
        if (vcb.checkVp(tok, VP.root)) {
            p |= VP.root;
        } else if (vcb.checkVp(tok, VP.negcontraction)) {
            p |= VP.neg;
        }
        if (vcb.checkVp(tok, VP.past|VP.participle)) {
            p |= VP.past;
        } else {
            p |= VP.present;
        }
        if (vcb.checkVp(tok, VP.gerund)) {
            p |= VP.gerund;
        }
        if (vcb.checkVp(tok, VP.adj)) {
            p |= VP.adj;
        }
        if (vcb.checkVp(tok, VP.participle)) {
            p |= VP.participle;
        }
        // remaining verb props are based on the root of the verb. This
        // is given by its definition.
        int tokDef = vcb.getDef(tok);
        if (vcb.checkVp(tokDef, VP.vpq)) {
            p |= VP.prelude;
        }
        return p;
    }
    
    /** get wrd "i" */
    public int getWrd(int i){
        return wrds.a[i];
    }
    
    /**
    * Does node match a word? The test
    * is performed on the node's first word.
    */
    public boolean testWrd(String sp){
        if (wrds.N>0) {
            int _def = vcb.getDef(getWrd(0));
            String spDef = vcb.spell(_def);
            return spDef.equals(sp);
        }
        return false;
    }
    public boolean testWrd(String[] lst){
        for (String sp: lst) {
            if (testWrd(sp)) {
                return true;
            }
        }
        return false;
    }
    
    /** set a prop */
    public void setVp(int v){
        vprops |= v;
    }
    
    /** check verb props */
    public boolean checkVp(int m){
        return (vprops & m) != 0;
    }
    
    /** check word props */
    public boolean checkWrdProp(int m){
        if (wrds.N > 0) {
            return vcb.checkProp(wrds.a[0], m);
        }
        return false;
    }
    
    /** get root form for verb */
    public int getVroot(){
        return verbs.N == 0? 0:verbs.a[0];
    }
    
    /** test verb-root against spelling */
    public boolean testVRoot(String spTest){
        if (verbs.N > 0) {
            String spRoot = vcb.spell(getVroot());
            if (spRoot.equals(spTest)) {
                return true;
            }
        }
        return false;
    }
    
    /** test verb-root against list of spellings */
    public boolean testVRoot(String[] spRoots){
        if (verbs.N > 0) {
            for (String sp: spRoots) {
                if (testVRoot(sp)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
    * Test form of verb. "form" can be:
    * VP_avgt, VP_evt, VP_ave, VP_vpq
    */
    public boolean testVForm(int form){
        return verbs.N>0 &&
        vcb.checkVp(verbs.a[0], form);
    }
    
    /** is this a verb? */
    public boolean isVerb(){
        return vcb.isScForVerb(sc);
    }
    
    /** quote- and paren- blocks are "container" */
    public boolean isContainer(){
        String scSp = vcb.spellSc(sc);
        return scSp.equals("QuoteBlk") || scSp == "ParenBlk";
    }
    
    /** is this a leaf? (no descendents) */
    public boolean isLeaf(){
        for (PnLst lst: rel) {
            if (lst.N > 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
    * get specified subnodes of this node: relation must be in
    * "srAccept"
    */
    public PnLst getSubnodes(int[] srAccept){
        PnLst nds = new PnLst();
        for (int i=0; i<srAccept.length; i++){
            nds.extend(rel[srAccept[i]]);
        }
        return nds;
    }
    
    /** check sc props */
    public boolean checkSc(int m){
        return vcb.scDct.checkProp(sc, m);
    }
    
    /** find relation of "e" to this node */
    public int getRel(Pn e){
        for (int i=0; i<SR.nwordtoverb; i++) {
            if (rel[i].contains(e)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
    * Unset scope for "e". This erases any existing relations from
    * verbs to e.
    */
    public void unsetScope(){
        if (scope != null) {
            int i = scope.getRel(this);
            if (i != -1) {
                scope.rel[i].remove(this);
            }
        }
        scope = null;
        sr = SR.undef;
    }
    
    /**
    * Set an edge from "v" to "e". "None" is a legal value for "v"
    * -- this just unsets any "i" relations to e
    */
    public void setScope(Pn v, int i){
        // setting scope to self is illegal
        assert this != v;
        // for all our relations, setting v->x erases any existing
        // relations vold->x. If "x" is currently in some relation with
        // "vold", then "vold" is given by "e.scope"
        unsetScope();
        if (v != null) {
            if (v.scope == this) {
                // this call is considered legal: it requires us to unset
                // v's scope.
                v.unsetScope();
            }
            // we order the terms left-to-right by "e.S"
            int ix = -1;
            PnLst rset = v.rel[i];
            //for j in range(0, rset.N):
            for (int j=0; j<rset.N; j++) {
                if (S <= rset.a[j].S) {
                    ix = j;
                    break;
                }
            }
            if (ix == -1) {
                rset.append(this);
            } else {
                rset.insert(ix, this);
            }
            scope = v;
            sr = i;
        }
    }
    
    /** reset a relation */
    public void resetRel(int oldRel, int newRel){
        rel[newRel] = rel[oldRel];
        rel[oldRel] = new PnLst();
        PnLstIterator iter = rel[newRel].getIterator();
        while (iter.hasNext()) {
            iter.next().sr = newRel;
        }
    }
    
    /** return a list of "h" (handles) for a list of nodes */
    public String dumpNdLst(String label, PnLst lst){
        PnLstIterator iter = lst.getIterator();
        SLst l = new SLst();
        while (iter.hasNext()) {
            l.append(Integer.toString(iter.next().h));
        }
        return label + ":" + l.join(", ");
    }
    public String dumpAttr(){
        SLst tmp = new SLst();
        tmp.append(String.format("%d. [%d.%d]", h, S, E));
        if (wrds.N > 0) {
            tmp.append("\"" + vcb.spell(wrds) + "\"");
        }
        if (head.N > 0) {
            tmp.append("head:\"" + vcb.spell(head) + "\"");
        }
        if (vprops != 0) {
            tmp.append("VP:" + VP.tostr(vprops, "|"));
        }
        tmp.append("sc:" + vcb.scTostr(sc));
        if (sr != 0xff) {
            tmp.append("sr:" + SR.ids[sr]);
        }
        if (scope != null) {
            tmp.append("Scp:" + Integer.toString(scope.h));
        }
        if (vIsoSub != null) {
            tmp.append("vIsoSub:" + Integer.toString(vIsoSub.h));
        }
        return tmp.join(" ");
    }
    
    public void printme(){
        System.out.print(dumpAttr());
        for (int i=0; i<SR.nwordtoverb; i++) {
            if (rel[i].N > 0) {
                System.out.print(" "+ dumpNdLst(SR.ids[i], rel[i]));
            }
        }
        System.out.println("");
    }
    /**
    * for dev/test: create sequence of nodes with specified
    * ".h" values
    */
    public static PnLst createNds(int... hseq) {
        PnLst nds = new PnLst();
        for (int i=0; i<hseq.length; i++) {
            Pn nd = new Pn(-1, 0, 0);
            nd.h = hseq[i];
            nds.append(nd);
        }
        return nds;
    }
}


