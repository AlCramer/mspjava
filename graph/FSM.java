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
package msp.graph;
import java.util.*;
import java.io.*;
import msp.util.*;

/**
* FSM: finite state machine. We use fsm's to recognize sequences
* of int values. Each recognized sequence has some associated value
* (an int). FSM's come in two flavors: left-to-right, and
* right-to-left. Left-to-right machines walk left-to-right thru
* a list of inputs, recognizing sequences. Right-to-left machines walk
* right-to-left thru the inputs list.
* Our main method is "get_sequences(inputs, i)". This returns a list
* of the value associated with each recognized sequences. For a
* left-to-right machine, these are the sequences that start at
* inputs[i]. For a right-to-left machine, these are the sequences
* that end at inputs[i].
*/
public class FSM{
    // for serialization
    int nbitsSeqTerm = 8;
    // states. Each state is a set of inputs: on the i_th input,
    // we to that state.
    public LstISet states;
    // are sequences recognized left-to-right?
    boolean leftToRight = true;
    // mapping, seq->V
    public HashMap<String, Integer> seqToV;
    public FSM(int nbitsSeqTerm, boolean leftToRight) {
        super();
        this.nbitsSeqTerm = nbitsSeqTerm;
        this.leftToRight = leftToRight;
    }
    public void setMaxSeqLen(int maxSeqLen){
        states = new LstISet(maxSeqLen);
    }
    public void serialize(Serialize serializer) throws IOException {
        if (serializer.mode.equals("w")) {
            serializer.encodeLstISet(states, nbitsSeqTerm);
        } else {
            LstISet _states = serializer.decodeLstISet(nbitsSeqTerm);
            if (_states != null) {
                states = _states;
            }
        }
    }
    
    /** Add a sequence */
    public void addSeq(ILst seq){
        assert seq.N <= states.N;
        if (!leftToRight) {
            seq = seq.clone();
            seq.reverse();
        }
        for (int i=0; i<seq.N; i++) {
            states.addElement(i, seq.a[i]);
        }
    }
    
    /**
    * Walk thru "inputs", finding all sequences that start at
    * input[i]. Returns a list of the values assigned to the
    * recognized sequences.
    */
    public ILst getSequencesLtoR(ILst inputs, int i){
        // our result: list of values assigned to recognized sequences
        ILst hits = new ILst();
        // spelling for sequences
        String seqSp = "";
        // index into states
        int j = 0;
        while (i< inputs.N &&
        j< states.N &&
        states.a[j] != null) {
            if (!states.contains(j, inputs.a[i])) {
                // cannot enter state "j": done
                break;
            }
            if (j == 0) {
                seqSp = Integer.toString(inputs.a[i]);
            } else {
                seqSp = seqSp + " " + Integer.toString(inputs.a[i]);
            }
            Integer iobj = seqToV.get(seqSp);
            if (iobj != null) {
                hits.append( iobj.intValue() );
            }
            i += 1;
            j += 1;
        }
        return hits;
    }
    
    /**
    * Walk thru "inputs", finding all sequences that end at
    * input[i]. Returns a list of the values assigned to the
    * recognized sequences.
    */
    public ILst getSequencesRtoL(ILst inputs, int i){
        // our result: list of values assigned to recognized sequences
        ILst hits = new ILst();
        // spelling for sequences
        String seqSp = "";
        // index into states
        int j = 0;
        while (i >= 0 &&
        j< states.N &&
        states.a[j] != null) {
            if (!states.contains(j, inputs.a[i])) {
                // cannot enter state "j": done
                break;
            }
            if (j == 0) {
                seqSp = Integer.toString(inputs.a[i]);
            } else {
                seqSp = Integer.toString(inputs.a[i]) + " " + seqSp;
            }
            Integer iobj = seqToV.get(seqSp);
            if (iobj != null) {
                hits.append( iobj.intValue() );
            }
            i -= 1;
            j += 1;
        }
        return hits;
    }
    
    /**
    * Recognize sequences contained in "inputs", returning
    * list of the values associated the recognized sequences.
    * If this machine is left-to-right, each recognized sequence
    * starts at inputs[i]. If this machine is right-to-left, each
    * ends at inputs[i].
    */
    public ILst getSequences(ILst inputs, int i){
        if (states == null) {
            return new ILst();
        }
        if (leftToRight) {
            return getSequencesLtoR(inputs, i);
        } else {
            return getSequencesRtoL(inputs, i);
        }
    }
    
    /**
    * "e" is a node in a doubly linked list. Each node has an "sc"
    * attribute, drawn from the same enumeration set as our
    * sequences. We're interested in node-sequences whose "sc"
    * values match the sequences known to the SeqMap. This method
    * finds all such sequences that start at "e". It returns a list
    * of [node-sequence, value] pairs. If "leftToRight", we start at
    * "e" and procede left-to-right; otherwise we start at e and
    * move right-to-left.
    */
    public List<PnLstVPair> getMatches(Pn e, boolean leftToRight){
        // our result: a list of [node-sequence, value] pairs
        List<PnLstVPair> matches = new ArrayList<>();
        if (states.N == 0 || e == null) {
            return matches;
        }
        // sequence of nodes
        PnLst ndSeq = new PnLst();
        // spelling for sc-sequence
        String seqSp = "";
        // index into states
        int i = 0;
        while (e != null &&
        i< states.N &&
        states.a[i] != null) {
            if (!states.contains(i, e.sc)) {
                // cannot enter state "i": done
                break;
            }
            ndSeq.append(e);
            if (i != 0) {
                seqSp += ' ';
            }
            seqSp += Integer.toString(e.sc);
            Integer iobj = seqToV.get(seqSp);
            if (iobj != null) {
                matches.add(new PnLstVPair(
                ndSeq.clone(), iobj.intValue() ));
            }
            i += 1;
            if (leftToRight) {
                e = e.nxt;
            } else {
                e = e.prv;
            }
        }
        return matches;
    }
    
    public void printMatch(int i, PnLstVPair m) {
        ILst tmp = new ILst();
        PnLstIterator iter = m.pnLst.getIterator();
        while (iter.hasNext()) {
            tmp.append(iter.next().sc);
        }
        System.out.printf("match %d. %s -> %s\n", i, tmp.toString(), m.v);
    }
    
    public void printMatches(List<PnLstVPair> matches) {
        if (matches.size() == 0) {
            System.out.println("no matches");
            return;
        }
        System.out.println(String.format(
        "N match results: %d", matches.size() ));
        int i = 0;
        for (PnLstVPair m : matches) {
            System.out.printf("match %d:\n", i++);
            printMatch(i++, m);
        }
    }
    
    public void printme(PrintStream fp) {
        if (states.N == 0) {
            return;
        }
        fp.print("States:\n");
        for (int i=0; i<states.N; i++) {
            HashSet<Integer> iset = (HashSet<Integer>)states.a[i];
            if (iset.size() == 0) {
                continue;
            }
            ILst tmp = new ILst();
            for (int v : iset) {
                tmp.append(v);
            }
            fp.printf("state %d. inputs: %s", i, tmp.toString());
        }
        fp.print("mappings. seq->v:\n");
        List<String> lst = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : seqToV.entrySet()) {
            String key = entry.getKey();
            if (key.equals("_null_")) {
                continue;
            }
            lst.add(String.format("%s -> %d\n", key, entry.getValue()));
        }
        java.util.Collections.sort(lst);
        for (String s: lst) {
            fp.print(s);
        }
    }
    
}


