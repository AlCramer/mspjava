// Copyright 2012 Al Cramer
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
import java.util.*;
import msp.util.*;
import msp.lex.*;

/** term in a compiled reg. expr */
class ReTerm {
    static final int _isOption = 0x1;
    static final int _zeroOrMore = 0x2;
    static final int _oneOrMore = 0x4;
    int props;
    List<String> variants;
    boolean checkProp(int m) {
        return (props & m) != 0;
    }
    ReTerm() {
        super();
        variants = new ArrayList<String>();
    }
    public String toString() {
        SLst l = new SLst();
        if (props != 0) {
            l.append(String.format("(props:%d)", props));
        }
        for (String v: variants) {
            l.append(v);
        }
        return l.join(" ");
    }
}

/** This class implements regular-expression matching for the parser.
* The match method accepts a sequence of terms ("src") and a string
* representation of a regexpr ("re"). Each re term matches to zero or
* more src terms.
*
* Qualifiers:
*
* re terms accept the qualifiers "?" , "+", and "*". "?" means the
* term is optional. If including this term in the matched sequence
* yields a complete match, we include it; if excluding the term
* yields a complete match, we exclude it. "*" means "zero or more",
* "+" means "one or more". The match is semi-greedy. In general an re
* term consumes as many source terms as it can; but if consuming less
* allows us to complete the match, then it yields the minimal number
* of required terms to its successors.
*
* Variants:
*
* An re term containing bars ("a|b|c") specifies three match variants
* ("a", "b", or "c"). We always accept the first term in the variants
* list that yields a match.Note that if a qualifier appears at the
* end of a variants list, it applies to the list as a whole. It's
* illegal to qualify a term inside a variants list: you can't say
* "A|B?|C".
*
* Nested re's:
*
* Surrounding one or more terms with square brackets specifies a
* nested re. You can also declare an re ("%myName") using "declRe"
* and then refer to it in another re. class ReMatch is abstract: you
* must implement the "matchTerm" method. */
public class ReMatch {
    public PnLst src;
    public LstPnLst matchResult;
    HashMap<String, List<ReTerm>> reDict = new HashMap<String, List<ReTerm>>();
    Vcb vcb;
    // test/dev code: enable "trace" to trace match operations
    boolean trace = false;
    int depth;
    int maxTraceDepth;
    void printTrace(String msg){
        if (!trace || depth > maxTraceDepth) {
            return;
        }
        String indent = "";
        for (int i=0; i<depth; i++) {
            indent += " ";
        }
        System.out.printf("%s%s\n", indent, msg);
    }
    void printReLst(List<ReTerm> l) {
        for (ReTerm t: l) {
            System.out.println(t.toString());
        }
    }
    public ReMatch() {
        this.vcb = Vcb.vcb;
    }
    /** Match terms in src, against the reTerm. Returns null if
    * no-match; otherwise it returns a list of the src terms consumed
    * in the match. This method is a stub: derived classes should
    * override. */
    public PnLst matchTerm(int state, String reTerm) {
        return null;
    }
    
    /** Update state: "consumed" contains the source terms just
    * consumed in matching a term. Returns the updated state. */
    int updateState(int state, PnLst consumed) {
        return state + consumed.N;
    }
    
    /** match a list of terms in "src" against a regular expression.
    * Returns true if the match is complete, and writes the match
    * terms to "matchResult". There's one element in matchResult for
    * each element in the re. Each element is a list, and contains
    * the term(s) that matched the re term. */
    public boolean match(PnLst _src, String _re, int initialState) {
        this.src = _src;
        matchResult = new LstPnLst();
        List<ReTerm> reLst;
        if (reDict.containsKey(_re)) {
            reLst = reDict.get(_re);
        } else {
            // compile the re and install in the dictionary
            reLst = compileRe(_re);
            reDict.put(_re, reLst);
        }
        if (trace) {
            System.out.printf("\n**start match. initState: %d\n", initialState);
            System.out.printf("**re: %s\n", _re);
        }
        depth = 0;
        return matchLst(initialState, reLst, matchResult);
    }
    
    /** find close for nested re */
    public static int findCloser(char src[], int i){
        char closer = ']';
        i += 1;
        while (i <= src.length-1) {
            if (src[i] == closer) {
                return i;
            }
            if (src[i] == '[') {
                int E = findCloser(src, i);
                if (E == -1) {
                    i += 1;
                } else {
                    i = E + 1;
                }
                continue;
            }
            i += 1;
        }
        return -1;
    }
    
    
    /** Helper for "compileRe": compile a term and add to variants list */
    int compileReTerm(List<String> variants, char[] src, int i) {
        int lsrc = src.length;
        char c0 = src[i];
        if (c0 == '[') {
            // nested re
            int E = findCloser(src, i);
            assert E != -1;
            String reName = '%' + new String(src, i, E-i+1);
            declRe(reName, new String(src, i+1, E-i-1));
            variants.add(reName);
            return E+1;
        }
        // id's can start with "%" (that's the name of a nested re).
        // We also allow elements in {, :!_}
        if ((c0 == '%') || (c0 == '_') || Character.isLetterOrDigit(c0)||
        (c0 == '!') || (c0 == ':')) {
            // grab id chars
            int E = i;
            while ((E+1)<lsrc &&
            (Character.isLetterOrDigit(src[E+1]) ||
            (src[E+1] == '_') ||
            (src[E+1] == ':') ||
            (src[E+1] == ',') || (src[E+1] == '!'))) {
                E += 1;
            }
            variants.add(new String(src, i, E-i+1));
            return E+1;
        }
        if (c0 == '.') {
            // match any
            variants.add(".");
            return i+1;
        }
        // error
        assert false;
        return -1;
    }
    
    /** compile re from source */
    List<ReTerm> compileRe(String srcStr) {
        // "reLst" is a list of match-terms. Each term is a pair:
        // [props, variants]. props gives the qualifiers (if any) and
        // variants is a list of variants for the term.
        List<ReTerm> reLst = new ArrayList<ReTerm>();
        // canonicalize space
        srcStr = srcStr.replaceAll("\\s*\\|\\s*", "|");
        char[] src = srcStr.trim().toCharArray();
        int lsrc = src.length;
        int i = 0;
        while (i<lsrc) {
            while (src[i] == ' ') {
                i += 1;
                continue;
            }
            ReTerm term = new ReTerm();
            reLst.add(term);
            List<String> variants = term.variants;
            // collect alternatives for this term
            while (i<lsrc) {
                i = compileReTerm(variants, src, i);
                if (i >= lsrc) {
                    break;
                }
                char c = src[i];
                i += 1;
                if (c == '|') {
                    // get additional alternatives
                    continue;
                }
                // if c is a qualifier, it ends the term
                if (c == '*') {
                    term.props = ReTerm._zeroOrMore;
                } else if (c == '+') {
                    term.props = ReTerm._oneOrMore;
                } else if (c == '?') {
                    term.props = ReTerm._isOption;
                }
                // this term is complete: advance to next
                break;
            }
        }
        return reLst;
    }
    
    /** declare an re: it can then appears as a term in a larger re.
    * Our convention requires that name start with "%". */
    public void declRe(String reName, String _re) {
        assert reName.startsWith("%");
        reDict.put(reName, compileRe(_re));
    }
    
    /** Match terms in src against terms in "reLst". Returns true if
    * the match is complete, and writes the match terms to "matLst".
    * There's one element in matLst, for each element in the re. */
    boolean matchLst(int state, List<ReTerm> reLst, LstPnLst matLst) {
        int ixRe = matLst.N;
        if (ixRe == reLst.size()) {
            // the match is complete
            return true;
        }
        ReTerm reTerm = reLst.get(ixRe);
        // Loop thru match terms until we hit a qualified term (or are
        // match complete)
        while (true) {
            if (reTerm.props != 0) {
                break;
            }
            PnLst terms = matchVariants(state, reTerm.variants);
            if (terms == null) {
                // match failed
                return false;
            }
            matLst.append(terms);
            state = updateState(state, terms);
            ixRe += 1;
            if (ixRe == reLst.size()) {
                // the match is complete
                return true;
            }
            reTerm = reLst.get(ixRe);
        }
        // The match term is qualified, so there are multiple ways
        // source terms can be matched to it. Each way is called a
        // "mode". Find all possible modes.
        LstPnLst modes = new LstPnLst();
        PnLst termsConsumed = new PnLst();
        if (reTerm.checkProp(ReTerm._zeroOrMore|ReTerm._isOption)) {
            modes.append(new PnLst());
        }
        int statex = state;
        while (true) {
            PnLst terms = matchVariants(statex, reTerm.variants);
            if (terms == null) {
                break;
            }
            termsConsumed.extend(terms);
            PnLst mode = termsConsumed.clone();
            modes.append(mode);
            statex = updateState(statex, terms);
            if (reTerm.checkProp(ReTerm._isOption)) {
                break;
            }
        }
        if (modes.N == 0) {
            // There's no way to match this term: match has failed
            return false;
        }
        // Find the longest mode that completes the match.
        int nMatLst = matLst.N;
        int i = modes.N - 1;
        while (i >= 0) {
            // purge matLst of terms added in previous iterations
            matLst.N = nMatLst;
            // accept the match associated with this mode, then try to
            // complete the match.
            matLst.append(modes.a[i]);
            int newstate = updateState(state, modes.a[i]);
            depth += 1;
            if (matchLst(newstate, reLst, matLst)) {
                depth -= 1;
                return true;
            }
            depth -= 1;
            i -= 1;
        }
        // match failed
        return false;
    }
    
    /** Match a variant */
    PnLst matchVariant(int state, String v) {
        if (v.startsWith("%")) {
            // a nested re
            LstPnLst terms = new LstPnLst();
            depth += 1;
            if (!matchLst(state, reDict.get(v), terms)) {
                depth -= 1;
                return null;
            }
            depth -= 1;
            return terms.combine();
        }
        return matchTerm(state, v);
    }
    
    /** Match terms in src, starting at term specified by "state",
    * against the variants. Returns a list of the terms consumed in
    * the match: null means no-match. The method searches the
    * variants list in left-to-right order, and accepts the first
    * successful variant encountered. */
    PnLst matchVariants(int state, List<String> variants) {
        printTrace(String.format("matchVariants. state:%d", state));
        for (String v : variants) {
            printTrace(String.format("trying variant:%s", v));
            PnLst terms = matchVariant(state, v);
            if (terms != null) {
                printTrace(String.format("match SUCCESS for %s", v));
                return terms;
            }
        }
        printTrace("matchVariants. no matches");
        return null;
    }
    
    /** print match results */
    public void printResults() {
        System.out.println("matchResult:");
        if (matchResult.N == 0) {
            System.out.println("null results");
        }
        matchResult.printme();
    }
    
} // end class ReMatch



