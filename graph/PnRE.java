// Copyright 2012 Al Cramer
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
import msp.lex.*;

/**
* Regular expression machinary for parser: match list of Pn
* (parse nodes) against a regular expression.
*/
public class PnRE extends ReMatch{
    public static PnRE pnRE;
    public PnRE(){
        super();
        PnRE.pnRE = this;
        declRe("%qualObjTerm", "X Prep X");
        declRe("%immedObjTerm", ".a[%qualObjTerm|X]");
    }
    
    // Convenience method: match, starting at first node in list
    public boolean match(PnLst src, String re) {
        return match(src, re, 0);
    }
    
    // Convenience method: get first node in match term "i"
    public Pn mr(int i) {
        return matchResult.a[i].a[0];
    }
    
    /**
    * helper for matchTerm: get first term in the (grammatical)
    * subject for v
    */
    public Pn getGrammaticalSub(Pn e){
        if (e.isVerb()) {
            if (e.rel[SR.topic].N > 0) {
                return e.rel[SR.topic].a[0];
            }
            if (e.rel[SR.agent].N > 0) {
                return e.rel[SR.agent].a[0];
            }
            if (e.rel[SR.exper].N > 0) {
                return e.rel[SR.exper].a[0];
            }
        }
        return null;
    }
    public PnLst matchTerm(int state, String reTerm){
        // "state" is an index into "self.src" (a list of Pn's)
        if (state >= src.N) {
            return null;
        }
        Pn term = src.a[state];
        if (reTerm.equals(".")) {
            // match any
            return new PnLst(term);
        }
        if (reTerm.startsWith("_")) {
            // a literal
            if (reTerm.substring(1).equals(
            vcb.spell(term.wrds.a[0]))) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("Prep")) {
            // any kind of prep
            if (vcb.checkScProp(term.sc, WP.prep)) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("Mod")) {
            // any kind of Mod
            if (vcb.checkScProp(term.sc, WP.mod)) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("VAdj")) {
            // verb-adjunct
            // MUSTDO: change to test on sc
            if (term.checkVp(VP.adj)) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("X")) {
            // a noun or modifier
            if (vcb.spell(term.sc).equals("X")) {
                return new PnLst(term);
            }
            return null;
        }
        // specific verbs for verb-phrases
        if (reTerm.equals("Be")) {
            return term.testVRoot("be")? new PnLst(term) : null;
        }
        if (reTerm.equals("Have")) {
            return term.testVRoot("have")? new PnLst(term) : null;
        }
        if (reTerm.equals("Do")) {
            return term.testVRoot("do")? new PnLst(term) : null;
        }
        if (reTerm.equals("Get")) {
            return term.testVRoot("get")? new PnLst(term) : null;
        }
        // "TickS" is "'s": can be an abbrev for "is" (or marker
        // for possession).
        if (reTerm.equals("TickS")) {
            return vcb.spell(term.sc).equals("'s")?
            new PnLst(term) : null;
        }
        // any old verb
        if (reTerm.equals("V")) {
            // a verb
            if (term.isVerb()) {
                return new PnLst(term);
            }
            return null;
        }
        // small verb constructs
        if (reTerm.equals("SubVerb")) {
            // a verb with a (grammatical) subject
            Pn sub = getGrammaticalSub(term);
            if (sub != null) {
                if (sub.E < term.vS) {
                    return new PnLst(term);
                }
            }
            return null;
        }
        if (reTerm.equals("VerbNoSub")) {
            // a verb with no subject
            if (term.isVerb() &&
            matchTerm(state, "SubVerb") == null) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("VerbSub")) {
            // verb-subject-optional object:
            // appears in aquery contexts
            Pn sub = getGrammaticalSub(term);
            if (sub != null) {
                if (sub.S > term.vE) {
                    return new PnLst(term);
                }
            }
            return null;
        }
        
        // attributions
        if (reTerm.equals("QuoteBlk")) {
            if (term.sc == vcb.lkupSc("QuoteBlk")) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("Comma")) {
            if (term.sc == vcb.lkupSc("Comma")) {
                return new PnLst(term);
            }
            return null;
        }
        if (reTerm.equals("Terminator")) {
            if (term.sc == vcb.lkupSc("Punct")) {
                String text = vcb.spell(term.wrds);
                if (text.equals(".") ||
                text.equals("?") ||
                text.equals("!") ||
                text.equals(":") ||
                text.equals(";")) {
                    return new PnLst(term);
                }
            }
            return null;
        }
        if (reTerm.equals("AgentSaid")) {
            if (term.isVerb()) {
                if (vcb.checkProp(term.verbs.a[0], WP.attribution)) {
                    return new PnLst(term);
                }
            }
            return null;
        }
        // internal error
        throw new RuntimeException(
        String.format("PnRE.matchTerm unknown term: %s", reTerm));
    }
}


