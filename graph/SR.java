// Copyright 2011, 2015 Al Cramer
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
import msp.util.*;

/** Syntax-relations */
public class SR {
    public final static int agent = 0;
    public final static int topic = 1;
    public final static int exper = 2;
    public final static int theme = 3;
    public final static int auxtheme = 4;
    public final static int modifies = 5;
    public final static int isqby = 6;
    public final static int attribution = 7;
    public final static int ladj = 8;
    public final static int vadj = 9;
    public final static int prelude = 10;
    // This used for a word that is in the scope of a verb, but its
    // relation is undefined.
    public final static int undef = 11;
    // Total number of relations, word->verb
    public final static int nwordtoverb = 12;
    // these are computational (not part of the syntax model proper)
    public final static int sub = 12;
    public final static int obj = 13;
    // names for roles
    public static String[] ids = {
        "agent", "topic", "exper", "theme", "auxTheme",
        "qual", "isqby", "attribution", "ladj", "vadj",
        "prelude", "undef", "sub", "obj"};
        /**
        * We encode (relation, scope) pairs into a single 8 bit value.
        * This function decodes a pair and writes to string; it's used
        * for dev/test` */
        public static String srEncTostr(int t){
            if (t == 0xff) {
                return "0xff";
            }
            // hi 4 bits gives rel, low 4 bits give scope spec.
            int scopeSpec = 0xf & t;
            int scopeSign = scopeSpec >> 3;
            int scopeMag = 0x7 & scopeSpec;
            String scopeSp = "";
            if (scopeSign == 0) {
                scopeSp = String.format("%d", scopeMag);
            } else {
                scopeSp = String.format("-%d", scopeMag);
            }
            String srSp = SR.ids[0xf & (t>>4)];
            return String.format("%s:%s", srSp, scopeSp);
        }
        
        public static String srEncTostr(ILst srseq){
            SLst l = new SLst();
            ILstIterator iter = srseq.getIterator();
            while (iter.hasNext()) {
                l.append(srEncTostr(iter.next()));
            }
            String res = l.join(" ");
            // shorten the listings...
            res = res.replace("auxTheme", "auxTh");
            res = res.replace("theme", "th");
            res = res.replace("agent", "ag");
            return res;
            
        }
    }
    
    
    
