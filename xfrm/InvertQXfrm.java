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
* Invert Q expressions. Given "the girl you saw", [the girl] gets
* the (sr, scope) attributes of the verb, and the verb becomes a
* modifier of [the girl].
*/
public class InvertQXfrm extends Xfrm {
    public InvertQXfrm(String name){
        super(name);
    }
    public Pn invertQ(Pn q){
        // q is the node to be qualified
        Pn v = q.scope;
        q.scope = v.scope;
        q.sr = v.sr;
        if (v.checkVp(VP.inf) && v.rel[SR.agent].N > 0) {
            // "a cake good enough to eat"
            // The pattern is [Nexpr Adj Inf] and the tree is:
            // Adj modifies Nexpr; Inf modifies Adj
            Pn sub = v.rel[SR.agent].a[0];
            sub.setScope(q, SR.modifies);
            v.setScope(sub, SR.modifies);
        } else {
            v.sr = SR.modifies;
            v.scope = q;
        }
        return q.nxt;
    }
    public void doXfrm(){
        Pn e = pg.eS;
        while (e != null) {
            if (e.sr == SR.isqby) {
                e = invertQ(e);
            } else {
                e = e.nxt;
            }
        }
        pg.validateRel();
    }
}


