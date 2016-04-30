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
* Context dependent transform of subject-verb to qualified
* expression.
*/
public class SvToQXfrm extends Xfrm{
    public SvToQXfrm(String name){
        super(name);
    }
    /** Can "e" be in a subject role? */
    public boolean inSubRole(Pn e){
        return e.sr == SR.agent ||
        e.sr == SR.exper ||
        e.sr == SR.topic;
    }
    
    /** is "e" an object term in an avgt expression? */
    public boolean isAvgtObjTerm(Pn e){
        if (e.sr == SR.theme || e.sr == SR.auxtheme) {
            if (e.scope.rel[SR.theme].N > 0 &&
            e.scope.rel[SR.auxtheme].N > 0) {
                // We're an object term in AGVT context: "I gave
                // the guy sitting there an apple".
                return true;
            }
        }
        return false;
    }
    
    /**
    * Returns verb node to be transformed: we're currently
    * at "e" in a right-to-left traversal of the graph
    */
    public Pn findVerb(Pn e){
        if (e.checkVp(VP.gerund)) {
            if (inSubRole(e) || isAvgtObjTerm(e)) {
                // "the girl sitting there" in subject or avgt object role
                return e;
            }
        } else if (e.checkVp(VP.participle)) {
            if (e.scope != null) {
                if (inSubRole(e) || isAvgtObjTerm(e)) {
                    // "the strongest wind ever recorded" in subject or
                    // avgt object role
                    return e;
                }
            }
            } else if (vcb.checkScProp(e.sc, WP.query) &&
            inSubRole(e) &&
            e.scope.rel[SR.isqby].N == 0) {
                // "who ate the cake".
                return e.scope;
            }
            return null;
        }
        
        public void doXfrm(){
            Pn e = pg.eS;
            while (e != null) {
                Pn v = findVerb(e);
                if (v != null) {
                    if (v.rel[SR.agent].N > 0) {
                        v.resetRel(SR.agent, SR.isqby);
                    } else if (v.rel[SR.exper].N > 0) {
                        v.resetRel(SR.exper, SR.isqby);
                    } else if (v.rel[SR.topic].N > 0) {
                        v.resetRel(SR.topic, SR.isqby);
                    }
                    e = v.nxt;
                } else {
                    e = e.nxt;
                }
            }
            pg.validateRel();
        }
    }
    
    
