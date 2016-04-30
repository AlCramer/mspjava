// Copyright 2012, 2015 Al Cramer
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
* Reduction transform. We make several passes over the graph,
* performing different kind of reductions. Each pass is implemented
* by a ReductXfrm.
*/
public class ReductXfrm extends Xfrm {
    // actions
    public static int actReduce = 0x1;
    public static int actSetProp = 0x2;
    public static int actSkip = 0x4;
    // finite-state-machine for recognizing node sequences
    FSM fsm;
    // reduction rules are represented by a set of parrallel arrays
    ILst offS;
    ILst offE;
    ILst props;
    ILst sc;
    ILst act;
    // dev/test
    static boolean traceRules;
    
    public ReductXfrm(String name){
        super(name);
        fsm = new FSM(8, true);
    }
    
    public String ruleToStr(int i){
        SLst l = new SLst();
        if (offS.a[i] != 0) {
            l.append(String.format("offS: %d", offS.a[i]));
        }
        if (offE.a[i] != 0) {
            l.append(String.format("offE: %d:", offE.a[i]));
        }
        if (props.a[i] != 0) {
            l.append(String.format("props: %s", VP.tostr(props.a[i], "|")));
        }
        if (sc.a[i] != 0) {
            l.append(String.format("sc: %s", vcb.spellSc(sc.a[i])));
        }
        if (act.a[i] != 0) {
            l.append(String.format("act: %d", act.a[i]));
        }
        return l.join(" ");
    }
    
    /*
    public String seqToStr(ILst seq){
        return vcb.scDct.spell(seq);
    }
    */
    
    public void printme(PrintStream fp){
        fp.printf("Xfrm %s\n", name);
        for (int i=0; i<offS.N; i++) {
            fp.printf("%d. %s\n", i, ruleToStr(i));
        }
    }
    public void serialize(Serialize serializer) throws IOException {
        fsm.serialize(serializer);
        if (serializer.mode.equals("w")) {
            serializer.encodeStrToInt(fsm.seqToV);
            serializer.encodeIntlst(offS, 8);
            serializer.encodeIntlst(offE, 8);
            serializer.encodeIntlst(props, 32);
            serializer.encodeIntlst(sc, 8);
            serializer.encodeIntlst(act, 8);
        } else {
            fsm.seqToV = serializer.decodeStrToInt();
            offS = serializer.decodeIntlst(8);
            offE = serializer.decodeIntlst(8);
            props = serializer.decodeIntlst(32);
            sc = serializer.decodeIntlst(8);
            act = serializer.decodeIntlst(8);
        }
    }
    
    /** can "e" be a verb-qualifier? */
    public boolean isVQual(Pn e){
        String reject[] = new String[] {
            "be", "have", "do", "will", "shall", "use"};
            return e != null &&
            e.isVerb() &&
            !e.testVRoot(reject);
        }
        
        /** reduce a phrase, S..E. */
        public Pn reduceTerms(Pn S, Pn E, int vprops, int sc){
            // If this is not a verb phrase reduction, just call graph's
            // reduction method
            if (!vcb.isScForVerb(sc)) {
                return pg.reduceTerms(S, E, vprops, sc);
            }
            // get list of verb terms S..E (skip modfiers). Catch negations
            // ("have not seen")
            vprops = 0;
            boolean isNeg = false;
            PnLst terms = new PnLst();
            ILst adverbs = new ILst();
            Pn e = S;
            while (e != null) {
                if (vcb.checkScProp(e.sc, WP.verb)) {
                    terms.append(e);
                } else if (e.wrds.N > 0) {
                    String sp = vcb.spell(e.wrds.a[0]).toLowerCase();
                    if (sp.equals("not") || sp == "never") {
                        isNeg = true;
                    } else if (sp.equals("to")) {
                        // include this in "terms"
                        terms.append(e);
                    } else if (e.checkSc(WP.adv)) {
                        adverbs.extend(e.wrds);
                    }
                }
                if (e == E) {
                    break;
                }
                e = e.nxt;
            }
            // Initial analysis: get first cut at props for the verb phrase.
            // "be" forms
            Pn v = null;
            if (pnRE.match(terms, "VAdj? Have|TickS _been|_being V")) {
                // "has been struck" -> passive case
                // "has been going" -> perfect case
                v = (Pn)pnRE.matchResult.a[3].a[0];
                vprops = VP.passive;
                if (v.checkVp(VP.gerund)) {
                    vprops = VP.perfect;
                }
            } else if (pnRE.match(terms, "Be|TickS _being|_been V")) {
                // "he's been killed" -> passive case
                // "I am being killed" -> passive case
                // "he's been walking" -> perfect case
                v = (Pn)pnRE.matchResult.a[2].a[0];
                vprops = VP.passive;
                if (v.checkVp(VP.gerund)) {
                    vprops = VP.perfect;
                }
            } else if (pnRE.match(terms, "VAdj? Be|TickS V")) {
                // "will be struck" -> passive case
                // "will be going" -> future tense (caught later)
                v = (Pn)pnRE.matchResult.a[2].a[0];
                vprops = VP.passive;
                if (v.checkVp(VP.gerund)) {
                    vprops = 0;
                }
            } else if (pnRE.match(terms, "_being V")) {
                // "being choosen was a surprise"
                // This is passive case with no primary theme. Class
                // this as a gerund: it will then be parsed as an action.
                vprops = VP.gerund;
                
                // "have" forms
            } else if (pnRE.match(terms, "VAdj? _to? Have V")) {
                // "may have seen" -> perfect case
                // "ought to have loved" -> perfect case
                vprops = VP.perfect;
                
                // do forms
            } else if (pnRE.match(terms, "Do V")) {
                // a no-op
                
                // "to" forms.
            } else if (pnRE.match(terms, "VAdj? _to Be|Get V")) {
                // "to be" and "to get" are equivalent. Cases include:
                // "to get very tired" (a passive form)
                // "to get going" (an action form)
                // "ought to have left"
                // "to be eating" is translated as "to eat", an
                // an infinitive form. "to be eaten" is translated
                // as passive construct
                vprops = VP.passive;
                v = (Pn)pnRE.matchResult.a[3].a[0];
                if (v.checkVp(VP.gerund)) {
                    vprops = VP.inf;
                }
            } else if (pnRE.match(terms, "Be _to V")) {
                // past-future construct: "how she was to get out".
                // Class this as subjunctive
                vprops = VP.subjunctive;
            } else if (pnRE.match(terms, "_used _to V")) {
                // "used to go"
                vprops = VP.past;
            } else if (pnRE.match(terms, "VAdj? _to V")) {
                // standard infinitive: "to not go", "to see"
                vprops = VP.inf;
            } else if (pnRE.match(terms, "VAdj V")) {
                // "would go"
                // "pass" here, else we'll match subsequent cases
                
                // "she will"
            } else if (pnRE.match(terms, "VAdj")) {
                vprops = VP.adj;
            }
            
            if (isNeg) {
                vprops |= VP.neg;
            }
            
            // Get additional props (tense, etc.)
            // "vS" and "vE": first/last verb in list
            int N = terms.N;
            Pn vS = (Pn)terms.a[0];
            Pn vE = (Pn)terms.a[N-1];
            // tense is derived from first term. Semantic props are inherited
            // from the last verb.
            vprops |= (vS.vprops & VP.tensemask);
            vprops |= (vE.vprops & VP.semanticmask);
            // If this is the reduction of an atomic verb phrase, get
            // additional props from vS.
            String scSp = vcb.spellSc(sc);
            if (terms.N == 1) {
                int mask = VP.gerund|VP.participle|VP.root|VP.semanticmask;
                vprops |= (vS.vprops & mask);
                if (scSp.equals("BeQuery") ||
                scSp.equals("VAdjQuery")) {
                    vprops |= VP.query;
                }
            }
            // If input syntax is "V", we extend it using "vprops" and facts
            // about the main verb.
            if (scSp.equals("V")) {
                if ((vprops & VP.inf) != 0) {
                    scSp = "Inf";
                } else if ((vprops & VP.gerund) != 0) {
                    scSp = "Ger";
                } else if ((vprops & VP.participle) != 0) {
                    scSp = "Part";
                } else if ((vprops & VP.passive) != 0) {
                    scSp = "Pas";
                }
                sc = vcb.lkupSc(scSp);
            }
            // call the graph's reduction method
            Pn R = pg.reduceTerms(S, E, vprops, sc);
            // last term gives the root verbs(s)
            R.verbs = vE.verbs.clone();
            // save any adverbs
            R.adverbs = adverbs;
            // vS and vE gives indices for start and end of verb construct
            R.vS = S.S;
            R.vE = E.E;
            // some complex forms ("have gone") are purely syntactic; others
            // ("might go") are considered to represent a qualified form for a
            // verb, and we save the qualifier.
            //for i in range(0, terms.N):
            for (int i=0; i<terms.N; i++) {
                Pn ex = (Pn)terms.a[i];
                if (ex.vqual.N > 0) {
                    R.vqual.extend(ex.vqual);
                }
                if (ex != vE && isVQual(ex)) {
                    R.vqual.append(ex.verbs.a[0]);
                }
            }
            // Reduce "[was beginning][to understand]
            Pn left = R.prv;
            if (left != null &&
            left.isVerb() &&
            left.testVForm(VP.vpq)) {
                vprops = R.vprops & VP.semanticmask;
                R = reduceTerms(left, R, vprops, vcb.lkupSc("V"));
            }
            return R;
        }
        
        public PnLstVPair findRule(Pn e){
            List<PnLstVPair> matches = fsm.getMatches(e, true);
            if (matches.size() > 0) {
                // want the longest match: the last element in the match
                // set.
                return matches.get(matches.size()-1);
            }
            return null;
        }
        
        public Pn applyRule(Pn e, PnLstVPair rule){
            PnLst seq = rule.pnLst;
            int vix = rule.v;
            Pn S = seq.a[0];
            Pn E = seq.a[seq.N-1];
            if (traceRules) {
                ILst l = new ILst();
                PnLstIterator iter = seq.getIterator();
                while (iter.hasNext()) {
                    l.append(iter.next().sc);
                }
                System.out.printf( "%s. reduce %s by %s",
                name, vcb.scDct.spell(l), ruleToStr(vix) );
            }
            //for i in range(0, offS.a[vix]):
            for (int i=0; i<offS.a[vix]; i++) {
                S = S.nxt;
            }
            //for i in range(0, offE.a[vix]):
            for (int i=0; i<offE.a[vix]; i++) {
                E = E.prv;
            }
            if (act.a[vix] == ReductXfrm.actSkip) {
                // a no-op
                return E.nxt;
            }
            if (act.a[vix] == ReductXfrm.actReduce) {
                Pn R = reduceTerms(S, E, props.a[vix], sc.a[vix]);
                return R.nxt;
            }
            if (act.a[vix] == ReductXfrm.actSetProp) {
                Pn ex = S;
                while (true) {
                    ex.setVp(props.a[vix]);
                    if (ex == E) {
                        break;
                    }
                    ex = ex.nxt;
                }
                return seq.a[seq.N-1].nxt;
            }
            throw new RuntimeException("ReductXfrm: unsupported action");
        }
        
        public void doXfrm(){
            Pn e = pg.eS;
            while (e != null) {
                PnLstVPair rule = findRule(e);
                if (rule != null) {
                    e = applyRule(e, rule);
                } else {
                    e = e.nxt;
                }
            }
        }
    }
    
    
    
    
    
