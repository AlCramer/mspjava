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
import msp.*;


public class UtMsp {
    // args[0] gives file path + name for a text file to be parsed.
    // If none specified, we parse the string "src".
    public static void main(String[] args) {
        String src =
        "So Alice ventured to taste it";
        String fnsrc = args.length > 0? args[0] : null;
        // dev code
        // fnsrc = null;
        // end devode
        try {
            List<Nd> nds;
            InputStream mspdat =
            new FileInputStream("C:/Users/Al/mspDev/dev/mspJava/msp.dat");
            Msp msp = new Msp(mspdat);
            mspdat.close();
            //msp.setTraceParse(true);
            //msp.printParser();
            if (fnsrc == null) {
                nds = msp.parseString(src);
                System.out.println(msp.toXml(nds, false));
            } else {
                InputStream fp = new FileInputStream(fnsrc);
                nds = msp.parseFile(fp);
                fp.close();
                String fnout = fnsrc;
                int ixdot = fnout.lastIndexOf(".");
                if (ixdot != -1) {
                    fnout = fnout.substring(0, ixdot);
                }
                fnout += ".xml";
                FileWriter fpout = new FileWriter(fnout);
                fpout.write(msp.toXml(nds, false) + "\n");
                fpout.close();
                System.out.println("wrote " + fnout);
            }
        } catch (IOException e) {
            throw new RuntimeException("UtMsp failure", e);
        }
    }
}






