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
package msp.lex;
import java.util.*;
import java.io.*;
import msp.util.*;

/**
* This class encapsulates 3 mappings: word->index, index->word, and
* index->props. "word" is a the spelling for an entry; "index" is
* the index assigned to an entry; "props" is a bitmask.
*/
public class Dict {
    // spelling->index
    HashMap<String, Integer> spToIx = new HashMap<String, Integer>();
    // index->spelling
    SLst spelling = new SLst();
    // index->props
    ILst props = new ILst();
    
    public Dict(){
        super();
    }
    public int getN(){
        // get number of entries
        return spelling.N;
    }
    
    /** lookup "sp", returning the index for its entry */
    public int lkup(String sp, boolean createIfMissing){
        if (spToIx.containsKey(sp)) {
            return spToIx.get(sp);
        }
        if (!createIfMissing) {
            return 0;
        }
        int ix = spelling.N;
        spToIx.put(sp, ix);
        spelling.append(sp);
        props.append(0);
        return ix;
    }
    
    /** serialize the dictionary */
    public void serialize(Serialize serializer) throws IOException {
        if (serializer.mode.equals("w")) {
            serializer.encodeStrlst(spelling);
            serializer.encodeIntlst(props, 32);
        } else {
            spelling = serializer.decodeStrlst();
            props = serializer.decodeIntlst(32);
            for (int i=0; i<spelling.N; i++) {
                String sp = (String)spelling.a[i];
                spToIx.put(sp, i);
            }
        }
    }
    
    /** get spelling */
    public String spell(int ix){
        return spelling.a[ix];
    }
    
    public String spell(ILst lst){
        SLst tmp = new SLst();
        ILstIterator iter = lst.getIterator();
        while (iter.hasNext()) {
            tmp.append(spelling.a[iter.next()]);
        }
        return tmp.join(" ");
    }
    
    /** set prop */
    public void setProp(int ix, int v){
        props.a[ix] |= v;
    }
    
    /** check prop */
    public boolean checkProp(int ix, int v){
        return (ix != 0) && ((props.a[ix] & v) != 0);
    }
    
    /** get props */
    public int getProps(int ix){
        return props.a[ix];
    }
}


