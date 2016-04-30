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
import java.util.*;
import msp.util.*;

public class UtILst {
    public static void ut() {
        ILst il1 = new ILst();
        ILst il2 = new ILst(4);
        for (int i=0; i<50; i++) {
            il1.append(i);
            il2.append(i);
        }
        if (!il1.equals(il2)) {
            System.out.println("FAIL LstILst.ut.1");
            return;
        }
        ILst il3 = new ILst();
        for (int i=1; i <= 5; i++) {
            il3.append(i);
        }
        ILst il4 = il1.slice(1, 6);
        if (!il3.equals(il4)) {
            System.out.println("FAIL LstILst.ut.2");
            return;
        }
        ILstIterator iter = il3.getIterator();
        int j = 1;
        while (iter.hasNext()) {
            if (j != iter.next()) {
                System.out.println("FAIL LstILst.ut.3");
                return;
            }
            j += 1;
        }
        
        il1 = new ILst(4);
        for (int i=1; i <= 4; i++) {
            il1.append(i);
        }
        il1.insert(0, 10);
        if (!(
        (il1.a[0] == 10) &&
        (il1.a[1] == 1) &&
        (il1.a[4] == 4))) {
            System.out.println("FAIL LstILst.ut.4");
            return;
        }
        
        il1 = new ILst();
        for (int i=1; i <= 4; i++) {
            il1.append(i);
        }
        il1.insert(3, 10);
        if (!(
        (il1.a[2] == 3) &&
        (il1.a[4] == 4) &&
        (il1.a[3] == 10))) {
            System.out.println("FAIL LstILst.ut.5");
            return;
        }
        
        System.out.println("PASS ILst ut");
    }
    
    public static void main(String[] args) {
        ut();
    }
}


