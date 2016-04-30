// Copyright 2014 Al Cramer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

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

/** Enumerators for a node's "form" attribute. */
public class NdForm{
    public final static int x = 0;
    public final static int action = 1;
    public final static int verbclause = 2;
    public final static int queryclause = 3;
    public final static int queryphrase = 4;
    public final static int queryword = 5;
    public final static int mod = 6;
    public final static int conjphrase = 7;
    public final static int conjword = 8;
    public final static int n = 9;
    // We provide three differant forms for punctuation. "terminator" is
    // any in the set {.!/;:}. "comma" is a single ", "; all other
    // punctuation is lumped together into "punct".
    public final static int terminator = 10;
    public final static int comma = 11;
    public final static int punct = 12;
    // mapping, form enumerator -> text descriptor
    public final static String[] ids = new String[] {"X",
    "action", "verbclause", "queryclause", "queryphrase",
    "queryword", "mod", "conjphrase", "conjword", "N",
    "terminator", "comma", "punct"};
}


