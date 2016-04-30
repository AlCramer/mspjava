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

/** Bind preps to verbs */
public class BindPreps extends Xfrm {
    public BindPreps(String name){
        super(name);
    }
    public Pn bindPrep(Pn e){
        // e is the prep to be bound
        Pn ep = e;
        int prep = ep.getWrd(0);
        Pn v0 = null;
        int fit0 = -1;
        Pn v1 = null;
        int fit1 = -1;
        Pn ex = ep.prv;
        // Walk left, to a max of 2 verbs; punctuation and
        // conjunctions terminate the walk.
        while (ex != null) {
            if (ex.checkSc(WP.conj|WP.punct)) {
                break;
            }
            if (vcb.isScForVerb(ex.sc)) {
                if (v0 == null) {
                    v0 = ex;
                    fit0 = vcb.getPrepVerbFitness(prep, v0.getVroot());
                } else if (v1 == null) {
                    v1 = ex;
                    fit1 = vcb.getPrepVerbFitness(prep, v1.getVroot());
                    break;
                }
            }
            ex = ex.prv;
        }
        if (fit1 > fit0) {
            ep.sc = vcb.lkupSc("FarPrep");
            v1.setVp(VP.farprep);
            return v1.prv;
        }
        return e.prv;
    }
    public void doXfrm(){
        Pn e = pg.eE;
        while (e != null) {
            if (e.checkSc(WP.prep)) {
                e = bindPrep(e);
            } else {
                e = e.prv;
            }
        }
    }
}


