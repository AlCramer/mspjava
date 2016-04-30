// Copyright 2011 Al Cramer
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
import java.io.*;
import java.util.*;
import msp.util.*;

/** A Node representing a block of text to be parsed. */
public class ParseBlk {
    // The block represents a subregion of the source text,
    // running from S to E inclusive.
    public int S = -1;
    public int E = -1;
    void setSp(int S, int E) {
        this.S = S;
        this.E = E;
    }
    // tokenized text, with location indices
    public ILst toks;
    public ILst tokLoc;
    // parenthesized text and quotes are represented as containers
    // "bracket" is the bracket character -- quote, left paren, etc.
    public List<ParseBlk> sublst = new ArrayList<>();
    public String bracket = "";
    public ParseBlk(ILst toks, ILst tokLoc) {
        super();
        this.toks = toks;
        this.tokLoc = tokLoc;
    }
    
    public static void printList(List<ParseBlk> lst, int indent) {
        String mar = "";
        for (int k=0; k<indent; k++) {
            mar += " ";
        }
        for (ParseBlk b : lst) {
            if (b.sublst.size() > 0) {
                System.out.println(String.format(
                "%sParseBlk. bracket:%s", mar, b.bracket));
            } else {
                System.out.println(String.format(
                "%sParseBlk:", mar));
                System.out.println(String.format(
                "%s%s", mar, Vcb.vcb.spell(b.toks)));
            }
        }
    }
}




