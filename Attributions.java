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
package msp;
import msp.util.*;
import msp.lex.*;
import msp.graph.*;
import msp.xfrm.*;

/**
* This module implements attribution ("Drop dead, " he said";
* "Why?", she asked")
*/
public class Attributions {
    public Attributions() {
        super();
    }
    // helper for "setAttributions"
    public static void setAttribution(Pn quote, Pn attr){
        // re tree structure: two quotes can share the same attribution.
        // We choose first quote as parent.
        Pn scope = attr.scope;
        if (scope != null &&
        scope.getRel(attr) != SR.attribution) {
            attr.unsetScope();
        }
        if (scope == null) {
            attr.setScope(quote, SR.attribution);
        } else {
            quote.rel[SR.attribution].append(attr);
        }
        // convert "said he" to "he said"
        if (attr.rel[SR.agent].N == 0 &&
        attr.rel[SR.theme].N > 0) {
            attr.rel[SR.agent] = attr.rel[SR.theme];
            attr.rel[SR.theme] = new PnLst();
        }
    }
    
    /** Rewrite node list, setting attributions */
    public static PnLst set(PnRE pnRE, PnLst nds){
        PnLst _nds = new PnLst();
        int i = 0;
        while (i < nds.N) {
            if (pnRE.match(nds,
            "QuoteBlk AgentSaid Comma|Terminator agentSaid QuoteBlk", i)) {
                Pn q1 = pnRE.mr(0);
                setAttribution(q1, pnRE.mr(1));
                Pn q2 = nds.a[i+4];
                setAttribution(q2, pnRE.mr(3));
                _nds.append(q1);
                _nds.append(q2);
                i += 5;
                continue;
            }
            if (pnRE.match(nds,
            "QuoteBlk AgentSaid Comma|Terminator QuoteBlk", i)) {
                Pn q1 = pnRE.mr(0);
                setAttribution(q1, pnRE.mr(1));
                Pn q2 = pnRE.mr(3);
                setAttribution(q2, pnRE.mr(1));
                _nds.append(q1);
                _nds.append(q2);
                i += 4;
                continue;
            }
            if (pnRE.match(nds,
            "AgentSaid Comma QuoteBlk", i)) {
                Pn q = pnRE.mr(2);
                setAttribution(q, pnRE.mr(0));
                _nds.append(q);
                i += 3;
                continue;
            }
            if (pnRE.match(nds,
            "QuoteBlk Comma? AgentSaid", i)) {
                Pn q = pnRE.mr(0);
                setAttribution(q, pnRE.mr(2));
                _nds.append(q);
                i += 3;
                continue;
            }
            _nds.append(nds.a[i]);
            i += 1;
        }
        return _nds;
    }
}




