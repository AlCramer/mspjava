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
import msp.util.*;
import msp.lex.*;
import msp.graph.*;

/** Conjoin words in the same sr context to form phrases */
public class ReduceSrClauses extends Xfrm{
    public ReduceSrClauses(String name){
        super(name);
    }
    public void reduceClauses(PnLst lst){
        if (lst.N == 0) {
            return;
        }
        // recurse thru child clauses
        for (int i=0; i<lst.N; i++) {
            Pn e = lst.a[i];
            for (int j=0; j<e.rel.length; j++) {
                reduceClauses(e.rel[j]);
            }
        }
        // merge sequences of prep's
        int prepMask = WP.prep|WP.qualprep|WP.clprep;
        PnLst l1 = new PnLst(lst.a[0]);
        for (int i=1; i<lst.N; i++) {
            Pn e = lst.a[i];
            Pn last = l1.a[l1.N-1];
            if (last.checkSc(prepMask) &&
            e.checkSc(prepMask) &&
            e.isLeaf()) {
                last.wrds.extend(e.wrds);
                last.E = e.E;
                pg.removeNode(e);
                continue;
            }
            l1.append(e);
        }
        // rewrite l1 to "lst", merging word sequences
        lst.N = 0;
        int i = 0;
        while (i < l1.N) {
            Pn e = l1.a[i];
            i += 1;
            if (e.checkSc(WP.punct)) {
                lst.append(e);
                continue;
            }
            // "e" is a word. It starts a phrase (which may consist solely
            // of this word).
            Pn S = e;
            if (S.checkSc(prepMask|WP.conj)) {
                // bind this to the word that follows (if there is a word)
                if (i<l1.N && !l1.a[i].checkSc(WP.punct)) {
                    l1.a[i].head.extend(S.wrds);
                    pg.removeNode(S);
                    S = l1.a[i];
                    i += 1;
                }
            }
            // "i" is at term that follows S. If S is a leaf, merge any
            // leaves that follow.
            if (S.isLeaf()) {
                while (i<l1.N) {
                    if (l1.a[i].checkSc(WP.punct|WP.verb) ||
                    !l1.a[i].isLeaf()) {
                        break;
                    }
                    S.wrds.extend(l1.a[i].wrds);
                    pg.removeNode(l1.a[i]);
                    i += 1;
                }
            }
            // add S to the lst
            lst.append(S);
        }
    }
    public void doXfrm(){
        reduceClauses(pg.getRootNodes());
    }
}


