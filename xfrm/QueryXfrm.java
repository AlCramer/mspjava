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
public class QueryXfrm extends Xfrm {
    public QueryXfrm(String name){
        super(name);
    }
    public void doXfrm(){
        Pn e = pg.eS;
        while (e != null) {
            // is "e" a verb-adjunct ("did she leave", "why did she leave")
            if (e.sr == SR.vadj) {
                // transfer relevant attributes to scope verb
                Pn v = e.scope;
                v.unsetScope();
                v.vprops = e.vprops & VP.tensemask;
                String[] exclude = new String[] {
                    "be", "have", "do", "will", "shall"};
                    if (!e.testVRoot(exclude)) {
                        v.vqual.append(e.getWrd(0));
                    }
                    // is e "why did..."?
                    if (e.rel[SR.isqby].N > 0) {
                        Pn qwrd = e.rel[SR.isqby].a[0];
                        qwrd.setScope(v, SR.isqby);
                    }
                    // mark main verb as a query
                    v.setVp(VP.query);
                } else if (e.checkSc(WP.qhead|WP.beqhead)) {
                    // mark "e" as query
                    e.setVp(VP.query);
                }
                e = e.nxt;
            }
        }
    }
    
    
