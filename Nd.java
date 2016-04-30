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
import java.util.*;
import msp.util.*;
import msp.lex.*;
import msp.graph.*;
import msp.xfrm.*;

/** Parse node for msp */
public class Nd{
    // What kind of node is this? Value is one of NdKind.xxx
    int kind;
    // syntax form: a noun, modifier, verb expression, etc.
    // Value is one of NdForm.xxx
    int form;
    // source text for this node
    String text;
    // tree structure
    Nd parent;
    List<Nd> subnodes = new ArrayList();
    // prepositions, etc. that immediately precede the phrase
    // represented by this node.
    String head = "";
    // These attributes are defined for verb expressions.
    // root form of the verb(s).
    String vroots = "";
    // qualifiers in a complex verb phrase ("couldn't go").
    String vqual = "";
    // adverbs in a verb phrase: "... left [very quickly]"
    String adverbs = "";
    // properties -- tense, negation, etc.
    int vprops = 0;
    // These attributes specify the location in the source of
    // the text associated with this node. We use a line/column
    // scheme: (lineS, colS) give the line and column numbers for
    // the start of the text, and (lineE, colE) give the line and
    // column numbers for the end of the text.
    int lineS = -1;
    int colS = -1;
    int lineE = -1;
    int colE = -1;
    int blank = -1;
    public Nd(int kind, int form, String text, Nd parent){
        this.kind = kind;
        this.form = form;
        this.text = text;
        this.parent = parent;
    }
    
    /** get depth of this node in the parse tree */
    int getDepth() {
        int depth = 0;
        Nd e = parent;
        while (e != null) {
            depth += 1;
            e = e.parent;
        }
        return depth;
    }
    /** test verb props */
    public boolean checkVp(int mask){
        return (vprops & mask) != 0;
    }
    
    /** Get child node(s) of specified kind. */
    public List<Nd> getSubnodes(int kind){
        List<Nd> nds = new ArrayList();
        for (Nd nd : subnodes) {
            if (nd.kind == kind) {
                nds.add(nd);
            }
        }
        return nds;
    }
    
    /**
    * Return a string containing an XML representation of the parse
    * tree rooted at this node. "loc" means: include location
    * information in nodes. This allows you to map a parse node back
    * to the location in the source text from which it came. If you
    * don't need this information, specify "false" to reduce visual
    * clutter.
    */
    public String toXml(boolean loc){
        return _toXml(loc);
    }
    
    /** Private implementation of the public method "toXml". */
    public String _toXml(boolean loc){
        // compute indentation
        String indent = " ";
        for (int i=0; i<getDepth(); i++) {
            indent += " ";
        }
        StringBuilder sb = new StringBuilder();
        // opener
        sb.append(String.format("%s<%s form=\"%s\"",
        indent, NdKind.ids[kind], NdForm.ids[form]));
        if (vroots.length() > 0) {
            sb.append(String.format(" vroots=\"%s\"" , vroots));
        }
        if (vqual.length() > 0) {
            sb.append(String.format(" vqual=\"%s\"" , vqual));
        }
        if (adverbs.length() > 0) {
            sb.append(String.format(" adverbs=\"%s\"" , adverbs));
        }
        if (vprops != 0) {
            sb.append(String.format(" vprops=\"%s\"", VP.tostr(vprops)));
        }
        if (head.length() > 0) {
            sb.append(String.format(" head=\"%s\"" , head));
        }
        if (loc) {
            sb.append(String.format(" loc=\"%d %d %d %d\"" , lineS, colS, lineE, colE));
            if (blank != -1) {
                sb.append(String.format(" blank=\"%d\"" , blank));
            }
        }
        sb.append(">");
        if (text.length() == 0) {
            sb.append("\n");
        }
        // compute the closer
        String closer = "</" + NdKind.ids[kind] + ">\n";
        if (subnodes.size() == 0) {
            if (text.length() > 0) {
                sb.append(" " + text + " ");
                sb.append(closer);
            }
            return sb.toString();
        }
        // text
        if (text.length() > 0) {
            sb.append(String.format("\n%s %s\n" , indent, text));
        }
        // subnodes
        for (Nd nd: subnodes) {
            sb.append(nd._toXml(loc));
        }
        // closer
        sb.append(indent + closer);
        return sb.toString();
    }
}



