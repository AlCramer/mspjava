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

/** Verb properties */
public class VP {
    public final static int neg = 0x1;
    public final static int adj = 0x2;
    public final static int past = 0x4;
    public final static int present = 0x8;
    public final static int future = 0x10;
    public final static int perfect = 0x20;
    public final static int subjunctive = 0x40;
    public final static int inf = 0x80;
    public final static int root = 0x100;
    public final static int gerund = 0x200;
    public final static int passive = 0x400;
    public final static int negcontraction = 0x800;
    public final static int prelude = 0x1000;
    public final static int vpq = 0x2000;
    public final static int avgt = 0x4000;
    public final static int ave = 0x8000;
    public final static int evt = 0x10000;
    public final static int isq = 0x20000;
    public final static int notmodified = 0x40000;
    public final static int nosubject = 0x80000;
    public final static int participle = 0x100000;
    public final static int query = 0x200000;
    public final static int farprep = 0x400000;
    public final static int tensemask = past|present|future|subjunctive;
    public final static int semanticmask = neg|prelude;
    
    /** Dump verb props */
    public static String tostr(int m, String delim){
        SLst s = new SLst();
        if ((m & VP.neg) != 0) s.append("not");
        if ((m & VP.adj) != 0) s.append("adj");
        if ((m & VP.past) != 0) s.append("past");
        if ((m & VP.present) != 0) s.append("present");
        if ((m & VP.future) != 0) s.append("future");
        if ((m & VP.perfect) != 0) s.append("perfect");
        if ((m & VP.subjunctive) != 0) s.append("subj");
        if ((m & VP.inf) != 0) s.append("inf");
        if ((m & VP.root) != 0) s.append("root");
        if ((m & VP.gerund) != 0) s.append("ger");
        if ((m & VP.passive) != 0) s.append("passive");
        if ((m & VP.negcontraction) != 0) s.append("NegContraction");
        if ((m & VP.prelude) != 0) s.append("prelude");
        if ((m & VP.vpq) != 0) s.append("vpq");
        if ((m & VP.avgt) != 0) s.append("avgt");
        if ((m & VP.ave) != 0) s.append("ave");
        if ((m & VP.evt) != 0) s.append("evt");
        if ((m & VP.isq) != 0) s.append("isQ");
        if ((m & VP.notmodified) != 0) s.append("notModified");
        if ((m & VP.nosubject) != 0) s.append("noSubject");
        if ((m & VP.participle) != 0) s.append("participle");
        if ((m & VP.query) != 0) s.append("query");
        if ((m & VP.farprep) != 0) s.append("farprep");
        return s.join(" ");
    }
}




