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
package msp.lex;
import msp.util.*;

/** Word properties */
public class WP {
    // parts-of-speach
    public final static int conj = 0x1;
    public final static int clprep = 0x2;
    public final static int qualprep = 0x4;
    public final static int prep = 0x8;
    public final static int n = 0x10;
    public final static int noun = 0x20;
    public final static int adj = 0x40;
    public final static int sub = 0x80;
    public final static int x = 0x100;
    public final static int verb = 0x200;
    public final static int adv = 0x400;
    // "mr", "mrs", etc.
    public final static int abbrev = 0x800;
    // "can't"
    public final static int contraction = 0x1000;
    // who/what/why/when/where/how
    public final static int query = 0x2000;
    // strong ("a") and weak ("that") determinants
    public final static int dets = 0x4000;
    public final static int detw = 0x8000;
    // punctuation
    public final static int punct = 0x10000;
    // clause modifiers
    public final static int clmod = 0x20000;
    // "(why) did .."; and "(why) is..."
    public final static int qhead = 0x80000;
    public final static int beqhead = 0x100000;
    // A number: "1821"
    public final static int num = 0x200000;
    // Attribution verb for quotation: "he said"
    public final static int attribution = 0x400000;
    // "Modifier" -- either adjective or adverb
    public final static int mod = adv|adj;
    
    /** Dump word props */
    public static String tostr(int m){
        SLst s = new SLst();
        if ((m & WP.conj) != 0) s.append("CONJ");
        if ((m & WP.clprep) != 0) s.append("CLPREP");
        if ((m & WP.qualprep) != 0) s.append("QUALPREP");
        if ((m & WP.prep) != 0) s.append("PREP");
        if ((m & WP.n) != 0) s.append("N");
        if ((m & WP.noun) != 0) s.append("NOUN");
        if ((m & WP.adj) != 0) s.append("ADJ");
        if ((m & WP.sub) != 0) s.append("SUB");
        if ((m & WP.x) != 0) s.append("X");
        if ((m & WP.verb) != 0) s.append("VERB");
        if ((m & WP.abbrev) != 0) s.append("ABBREV");
        if ((m & WP.contraction) != 0) s.append("CONTRACTION");
        if ((m & WP.query) != 0) s.append("QUERY");
        if ((m & WP.dets) != 0) s.append("DETS");
        if ((m & WP.detw) != 0) s.append("DETW");
        if ((m & WP.punct) != 0) s.append("PUNCT");
        if ((m & WP.clmod) != 0) s.append("CLMOD");
        if ((m & WP.qhead) != 0) s.append("QHEAD");
        if ((m & WP.beqhead) != 0) s.append("BEQHEAD");
        if ((m & WP.adv) != 0) s.append("ADV");
        if ((m & WP.num) != 0) s.append("NUM");
        if ((m & WP.attribution) != 0) s.append("ATTRIBUTION");
        return s.join(" ");
    }
}



