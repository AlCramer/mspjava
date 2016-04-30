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

public class UtLstILst {
    public static void ut() {
        ILst il1 = new ILst();
        il1.append(1);
        il1.append(2);
        ILst il2 = new ILst();
        il2.append(3);
        il2.append(4);
        LstILst l1 = new LstILst();
        l1.append(il1);
        l1.append(il2);
        
        ILst il1x = il1.clone();
        ILst il2x = il2.clone();
        LstILst l2 = new LstILst();
        l2.append(il1x);
        l2.append(il2x);
        
        if (!l1.equals(l2)) {
            System.out.println("FAIL LstILst ut");
            return;
        }
        System.out.println("PASS LstILst ut");
    }
    public static void main(String[] args) {
        ut();
    }
    
}



