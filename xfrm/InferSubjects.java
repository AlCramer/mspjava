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
public class InferSubjects extends Xfrm {
    public InferSubjects(String name){
        super(name);
    }
    public void doXfrm(){
        // get sequence of verbs + top scope nodes
        PnLst seq = new PnLst();
        Pn e = pg.eS;
        while (e != null) {
            if (e.isVerb() || e.scope == null) {
                seq.append(e);
            }
            e = e.nxt;
        }
        pnRE.declRe("%commaPhr", "[_, _and|_or|_but? _then?]");
        pnRE.declRe("%conjPhr", "_and|_or|_but _then?");
        while (seq.N >= 2) {
            if (pnRE.match(seq, "SubVerb %commaPhr|%conjPhr Mod? VerbNoSub|VerbSub")) {
                e = pnRE.matchResult.a[0].a[0];
                Pn ex = pnRE.matchResult.a[3].a[0];
                pg.reduceHead(pnRE.matchResult.a[1].a[0], ex);
                // ex is given same scope, syntax role, and relations as
                // its peer.
                Pn scope = e.scope;
                ex.scope = scope;
                ex.sr = e.sr;
                if (scope != null) {
                    int relIx = scope.getRel(e);
                    if (relIx != -1) {
                        scope.rel[relIx].append(ex);
                    }
                }
                // ex's subject roles are derived from its peer. "subject"
                // roles are : topic, agent, and experiencer. In testing,
                // order is important: check agent before exper, because
                // of AVE
                int erole = -1;
                if (e.rel[SR.topic].N > 0) {
                    erole = SR.topic;
                }
                if (e.rel[SR.agent].N > 0) {
                    erole = SR.agent;
                }
                if (e.rel[SR.exper].N > 0) {
                    erole = SR.exper;
                }
                if (erole != -1) {
                    if (pnRE.match(new PnLst(ex), "VerbSub")) {
                        // default role assignment classed this as a query:
                        // "have you the time". Slide the roles down the
                        // hierarchy.
                        ex.rel[SR.modifies] = ex.rel[SR.theme];
                        ex.rel[SR.theme] = ex.rel[SR.agent];
                    }
                    // compute role for ex
                    int exrole = SR.agent;
                    if (ex.checkVp(VP.passive|VP.participle) ||
                    ex.testVRoot("be")) {
                        exrole = SR.topic;
                    } else if (ex.testVForm(VP.evt)) {
                        exrole = SR.exper;
                    }
                    exrole = erole;
                    if (ex.testVForm(VP.evt)) {
                        exrole = SR.exper;
                    }
                    ex.rel[exrole] = e.rel[erole];
                }
                // advance
                seq = seq.copy(seq.find(ex), seq.N);
                continue;
            }
            seq = seq.copy(1, seq.N);
        }
    }
}


