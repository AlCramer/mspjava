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
* Verb props -- tense and negation.
* In MSP, constructs like "would not have gone" are recognized,
* analyzed, and represented by a single node in the parse tree.
* Nodes representing verb constructs have a "vprops" attribute
* giving information about the construct. We use bit-masks
* to represent props; use the node's "checkVP" method to
* interrogate its props.
*/
public class VP{
    public final static int neg = 0x1;
    public final static int past = 0x2;
    public final static int present = 0x4;
    public final static int future = 0x8;
    public final static int subjunctive = 0x10;
    public final static int perfect = 0x20;
    
    /** dump a bitset of verb props */
    public static String tostr(int m){
        SLst tmp = new SLst();
        if ((m & VP.neg) != 0) {
            tmp.append("neg");
        }
        if ((m & VP.past) != 0) {
            tmp.append("past");
        }
        if ((m & VP.present) != 0) {
            tmp.append("present");
        }
        if ((m & VP.future) != 0) {
            tmp.append("future");
        }
        if ((m & VP.subjunctive) != 0) {
            tmp.append("subjunctive");
        }
        if ((m & VP.perfect) != 0) {
            tmp.append("perfect");
        }
        return tmp.join(" ");
    }
}


