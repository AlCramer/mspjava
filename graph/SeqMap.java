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
package msp.graph;
import java.util.*;
import java.io.*;
import msp.util.*;

/**
* A SeqMap defines a set of (sequence->value) mappings. A sequence is
* a set of int's; a value is a int.
*/
public class SeqMap{
    // for serialization
    int nbitsSeqTerm = 8;
    // states. Each state is a set of inputs: on the i_th input,
    // we to that state.
    LstISet states;
    // mapping, seq->V
    HashMap<String, Integer> seqToV = new HashMap<>();
    // helper for print operations
    ISeqMapToStr toStr;
    public SeqMap(ISeqMapToStr toStr){
        super();
        this.toStr = toStr;
    }
    public void setMaxSeqLen(int maxSeqLen){
        states = new LstISet(maxSeqLen);
    }
    public void serialize(Serialize serializer) throws IOException{
        if (serializer.mode.equals("w")) {
            serializer.encodeLstISet(states, nbitsSeqTerm);
            serializer.encodeInt(seqToV.size(), 32);
            //for key, v in seqToV.iteritems():
            for (Map.Entry<String, Integer> entry : seqToV.entrySet()) {
                String key = entry.getKey();
                int v = entry.getValue();
                serializer.encodeStr(key);
                serializer.encodeInt(v, 16);
            }
        } else {
            states = serializer.decodeLstISet(nbitsSeqTerm);
            seqToV = new HashMap<String, Integer>();
            int N = serializer.decodeInt(32);
            for (int i=0; i<N; i++) {
                String key = serializer.decodeStr();
                seqToV.put(key, serializer.decodeInt(16));
            }
        }
    }
    
    /**
    * Add seq->V mapping.
    * To build a SeqMap, set the maximum sequence length, then
    * repeatedly call addSeqV to create the mappings
    */
    public void addSeqV(ILst seq, int v){
        assert seq.N <= states.N;
        SLst tmp = new SLst();
        for (int i=0; i<seq.N; i++) {
            states.addElement(i, seq.a[i]);
            tmp.append(Integer.toString(seq.a[i]));
        }
        seqToV.put(tmp.join(" "), v);
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
    
    public void printMatch(PnLstVPair m) {
        ILst tmp = new ILst();
        PnLstIterator iter = m.pnLst.getIterator();
        while (iter.hasNext()) {
            tmp.append(iter.next().sc);
        }
        System.out.printf("%s -> %s\n",
        toStr.seqToStr(tmp),
        toStr.vToStr(m.v)
        );
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
            printMatch(m);
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
            fp.print("state " + i + ".\n");
            fp.print(String.format(
            "inputs: %s\n", tmp.toString() ));
            fp.print(String.format(
            " %s\n", toStr.seqToStr(tmp) ));
            fp.print(String.format(
            "%d. %s\n", i, toStr.seqToStr(tmp) ));
        }
        fp.print("SeqToV:\n");
        int i = 0;
        for (Map.Entry<String, Integer> entry : seqToV.entrySet()) {
            String key = entry.getKey();
            int v = entry.getValue();
            ILst seq = new ILst();
            for (String term : key.split(" ")) {
                seq.append(Integer.parseInt(term));
            }
            fp.print(String.format(
            "%d. %s -> %s\n",
            i,
            toStr.seqToStr(seq),
            toStr.vToStr(v) ));
        }
    }
} // end class SeqMap


