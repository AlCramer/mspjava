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
import msp.lex.*;
import msp.graph.*;


public class UtPnLst {
    public static void main(String[] args) {
        PnLst l1 = new PnLst();
        for (int i=0; i<50; i++) {
            l1.append(new Pn(-1, i, i));
        }
        Pn pnx = l1.a[25];
        if (l1.a[25].S != 24 || !l1.contains(pnx) ) {
            System.out.println("FAIL PnLst.ut.1");
            return;
        }
        
        PnLstIterator iter = l1.getIterator();
        int j = 0;
        while (iter.hasNext()) {
            if (iter.next().S != j) {
                System.out.println("FAIL PnLst.ut.2");
                return;
            }
            j += 1;
        }
        
        l1.remove(pnx);
        if (l1.a[25].S != 25 || l1.contains(pnx) ) {
            System.out.println("FAIL PnLst.ut.3");
            return;
        }
        
        l1.insert(2, new Pn(-1, 200, 200));
        if (l1.a[2].S != 200 || l1.a[3].S != 2) {
            System.out.println("FAIL PnLst.ut.4");
            return;
        }
        
        System.out.println("PASS PnLst ut");
    }
}


