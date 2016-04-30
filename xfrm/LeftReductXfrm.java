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
/**
* Subclass of ReductXfrm: these reductions require that we be in
* the left (start) context of a syntax-relations region.
*/
public class LeftReductXfrm extends ReductXfrm {
    public LeftReductXfrm(String name){
        super(name);
    }
    /**
    * Get next region of graph, starting at "e", in which we
    * reduce
    */
    public PgRegion getRegion(Pn e){
        while (e != null) {
            // skip punctuation and conjunctions
            if (e.checkSc(WP.punct|WP.conj)) {
                e = e.nxt;
                continue;
            }
            Pn S = e;
            Pn E = e;
            // extend to next punctuation (conjunctions are allowed)
            while (e.nxt != null) {
                if (e.nxt.checkSc(WP.punct)) {
                    break;
                }
                E = e.nxt;
                e = e.nxt;
            }
            return new PgRegion(S, E);
        }
        return null;
    }
    
    /** Do left (start) context reductions */
    public void doXfrm(){
        PgRegion region = getRegion(pg.eS);
        while (region != null) {
            PnLstVPair rule = findRule(region.S);
            if (rule != null) {
                applyRule(region.S, rule);
            }
            region = getRegion(region.E.nxt);
        }
    }
}



