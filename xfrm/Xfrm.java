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
package msp.xfrm;
import java.util.*;
import java.io.*;
import msp.util.*;
import msp.lex.*;
import msp.graph.*;

/**
* Parsing is implemented as a series of transforms of the
* parse graph. Each transform is implemented as a class, whose
* "doXfrm" method does the work. Some transforms are purely
* programmatic, while others use data tables: these implement
* "serialize".
*/
public class Xfrm{
    public String name;
    // all xfrm's have access to the vocab, parse-graph and reg.expr
    // machinery.
    public Vcb vcb;
    public Pg pg;
    public PnRE pnRE;
    // dev/test toggle
    public static boolean traceparse;
    public Xfrm(String name){
        this.name = name;
        this.vcb = Vcb.vcb;
        this.pg = Pg.pg;
        this.pnRE = PnRE.pnRE;
    }
    public void doXfrm(){
    }
    public void serialize(Serialize serializer) throws IOException {
    }
    public void printme(PrintStream fp) {
        fp.printf("Xfrm %s\n", name);
    }
}


