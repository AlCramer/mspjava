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
package msp.unittests;
import java.io.*;
import java.util.*;
import msp.util.*;

public class UtSerialize {
    
    public static void ut() throws IOException {
        // some data
        int i32 = 0xfffff;
        ILst lst = new ILst();
        lst.append(1);
        lst.append(2);
        SLst strLst = new SLst();
        strLst.append("a");
        strLst.append("ab");
        // list of lists
        LstILst lstLst = new LstILst();
        ILst l1 = new ILst();
        l1.append(257);
        l1.append(1);
        ILst l2 = new ILst();
        l2.append(256);
        l2.append(0);
        lstLst.append(l1);
        lstLst.append(l2);
        // list of [set of ints]
        LstISet lset = new LstISet(3);
        lset.addElement(0, 100);
        lset.addElement(2, 200);
        // serialize it
        BufferedOutputStream out =
        new BufferedOutputStream(new FileOutputStream("ut.dat"));
        Serialize ser = new Serialize(out, "w");
        ser.encodeInt(i32, 32);
        ser.encodeIntlst(lst, 32);
        ser.encodeStrlst(strLst);
        ser.encodeLstlst(lstLst, 32);
        ser.encodeLstISet(lset, 16);
        ser.fini();
        // deserialize
        BufferedInputStream in =
        new BufferedInputStream(new FileInputStream("ut.dat"));
        ser = new Serialize(in, "r");
        int _i32 = ser.decodeInt(32);
        assert i32 == _i32;
        ILst _lst = ser.decodeIntlst(32);
        assert lst.equals(_lst);
        SLst _strLst = ser.decodeStrlst();
        assert strLst.equals(_strLst);
        LstILst _lstLst = ser.decodeLstlst(32);
        assert lstLst.equals(_lstLst);
        LstISet _lset = ser.decodeLstISet(16);
        assert _lset.contains(0, 100);
        assert !_lset.contains(1, 100);
        assert _lset.contains(2, 200);
        ser.fini();
        System.out.println("Pass Serialize.ut");
    }
    
    public static void main(String[] args) {
        try {
            ut();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

