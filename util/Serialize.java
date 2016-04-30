// Copyright 2012, 2015 Al Cramer
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
/** Serialization. We use two files of serialized data structures.
* "vcb.dat" initializes the lexicon, and "grammar.dat" provides the
* grammar rules. These binary files are read by app's written in
* different languages (and/or run on different platforms), so we need
* need explicit control over the format. */
package msp.util;
import java.io.*;
import java.util.*;

/**
* Serialization. We use two files of serialized data structures.
* "vcb.init" initializes the lexicon, and "parser.dat" provides the
* parse rules. The msp package is cross-platform and cross- language
* (there are various ports), so it uses it own serialization scheme.
*/
public class Serialize {
    // "r" or "w"
    public String mode;
    // array of bytes, written to or read from file
    byte[] buf;
    // in- and out- streams
    BufferedInputStream in;
    BufferedOutputStream out;
    // debug tool
    int pos;
    void printpos(String title) {
        System.out.println(String.format(
        "%s %s. pos: %d", mode, title, pos));
    }
    
    /**
    * For a read (mode == "r") iostream should be an InputStream. For
    * a write (mode == "w") iostream should be an OutputStream.
    */
    public Serialize(Object iostream, String mode) {
        this.mode = mode;
        buf = new byte[256];
        in = null;
        out = null;
        if (mode.equals("r")) {
            in = new BufferedInputStream((InputStream)iostream);
        } else {
            out = new BufferedOutputStream((OutputStream)iostream);
        }
        pos = 0;
    }
    
    /**
    * complete serialization
    */
    public void fini() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Serialize IO failure", e);
        }
    }
    
    // int encodings
    public void encodeInt(int v, int nBits) throws IOException {
        if (nBits == 8) {
            out.write(v);
        } else if (nBits == 16) {
            out.write(v >> 8);
            out.write(v);
        } else {
            out.write(v >> 24);
            out.write(v >> 16);
            out.write(v >> 8);
            out.write(v);
        }
        pos += (nBits/8);
    }
    
    public int decodeInt(int nBits) throws IOException {
        pos += (nBits/8);
        if (nBits == 8) {
            return 0xff & in.read();
        } else if (nBits == 16) {
            in.read(buf, 0, 2);
            return (0xffff & (buf[0] << 8)) | (0xff & buf[1]);
        } else {
            in.read(buf, 0, 4);
            return ((0xff & (int)buf[0]) << 24) |
            ((0xff & (int)buf[1]) << 16) |
            ((0xff & (int)buf[2]) << 8) |
            (0xff & (int)buf[3]);
        }
    }
    
    // string encodings
    public void encodeStr(String s) throws IOException {
        assert (0xff & s.length()) == s.length();
        encodeInt(0xff & s.length(), 8);
        for (int i = 0; i < s.length(); i++) {
            out.write((byte)s.charAt(i));
        }
        pos += s.length();
    }
    
    public String decodeStr() throws IOException {
        int slen = decodeInt(8);
        char[] chary = new char[slen];
        for (int j = 0; j < slen; j++) {
            chary[j] = (char) in.read();
        }
        pos += slen;
        return new String(chary);
    }
    
    // list encodings
    
    public void encodeIntlst(ILst l, int nBits) throws IOException {
        encodeInt(l.N, 16);
        for (int i=0; i<l.N; i++ ) {
            encodeInt(l.a[i], nBits);
        }
    }
    
    public ILst decodeIntlst(int nBits) throws IOException {
        int N = decodeInt(16);
        if (N == 0) {
            return new ILst();
        }
        ILst l = new ILst(N);
        for (int i = 0; i < N; i++) {
            l.a[i] = decodeInt(nBits);
        }
        return l;
    }
    
    public void encodeStrlst(SLst l) throws IOException {
        encodeInt(l.N, 32);
        for (int i=0; i<l.N; i++ ) {
            encodeStr(l.a[i]);
        }
    }
    
    public SLst decodeStrlst() throws IOException {
        int N = decodeInt(32);
        if (N == 0) {
            return new SLst();
        }
        SLst l = new SLst(N);
        for (int i = 0; i < N; i++) {
            l.a[i] = decodeStr();
        }
        return l;
    }
    
    public void encodeLstlst(LstILst l, int nBits) throws IOException {
        // encode a list of [list of int]
        if (l == null) {
            encodeInt(0, 16);
            return;
        }
        encodeInt(l.N, 16);
        for (int i = 0; i < l.N; i++) {
            ILst v = l.a[i];
            int lenV = v == null? 0: v.N;
            encodeInt(lenV, 16);
            if (v != null) {
                for (int j=0; j<lenV; j++) {
                    encodeInt(v.a[j], nBits);
                }
            }
        }
    }
    
    public LstILst decodeLstlst(int nBits) throws IOException {
        // An empty list is decoded as "null" (not as an empty list).
        int N = decodeInt(16);
        if (N == 0) {
            return null;
        }
        LstILst l = new LstILst(N);
        l.N = N;
        for (int i = 0; i < N; i++) {
            int lenV = decodeInt(16);
            if (lenV == 0) {
                l.a[i] = null;
            } else {
                ILst v = new ILst(lenV);
                l.a[i] = v;
                for (int j = 0; j < lenV; j++) {
                    v.a[j] = decodeInt(nBits);
                }
            }
        }
        return l;
    }
    
    public void encodeLstISet(LstISet l, int nBits) throws IOException {
        // encode a list of [set of int]
        if (l == null) {
            encodeInt(0, 16);
            return;
        }
        int N = l.a.length;
        encodeInt(N, 16);
        for (int i=0; i<N; i++) {
            HashSet<Integer> s = (HashSet<Integer>) l.a[i];
            int lenV = s == null? 0: s.size();
            encodeInt(lenV, 16);
            if (lenV > 0) {
                for (Integer j: s) {
                    encodeInt(j.intValue(), nBits);
                }
            }
        }
    }
    
    public LstISet decodeLstISet(int nBits) throws IOException {
        // An empty list is decoded as "null" (not as an empty list).
        int N = decodeInt(16);
        if (N == 0) {
            return null;
        }
        LstISet l = new LstISet(N);
        for (int i=0; i<N; i++) {
            int lenV = decodeInt(16);
            for (int j=0; j<lenV; j++) {
                l.addElement(i, decodeInt(nBits));
            }
        }
        return l;
    }
    
    // Mapping, str->16-bit int. This is implemented as a hashtable.
    public void encodeStrToInt(HashMap<String, Integer> ht) throws IOException {
        for (Map.Entry<String, Integer> entry : ht.entrySet()) {
            String key = entry.getKey();
            int v = entry.getValue();
            encodeStr(key);
            encodeInt(v, 16);
        }
    }
    
    public HashMap<String, Integer> decodeStrToInt() throws IOException {
        HashMap<String, Integer> ht = new HashMap<String, Integer>();
        int N = decodeInt(32);
        for (int i=0; i<N; i++) {
            String key = decodeStr();
            ht.put(key, decodeInt(16));
        }
        return ht;
    }
    
}


