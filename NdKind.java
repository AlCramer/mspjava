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

/** Enumerators for a node's "kind" attribute. */
public class NdKind{
    // Root nodes have one of these values:
    public final static int x = 0;
    public final static int punct = 1;
    public final static int query = 2;
    public final static int imper = 3;
    public final static int assertion = 4;
    public final static int quote = 5;
    public final static int paren = 6;
    // Child nodes of verbs are given one of these values: these
    // define thematic relations.
    public final static int agent = 7;
    public final static int topic = 8;
    public final static int exper = 9;
    public final static int theme = 10;
    public final static int auxtheme = 11;
    // The "qualification" relation. Any node (root or child)
    // can have a child node that qualifies it.
    public final static int qual = 12;
    // This node kind identifies the attribution for a quote
    public final static int attribution = 13;
    // Total number of NdKind.public final static int x values
    public final static int nkinds = 14;
    // mapping, kind enumerator -> text descriptor
    public final static String[] ids = new String[] {"X",
    "punct", "query", "imperative", "assert", "quote",
    "paren", "agent", "topic", "exper", "theme",
    "auxtheme", "qual", "attribution"};
}


