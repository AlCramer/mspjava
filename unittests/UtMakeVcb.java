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


public class UtMakeVcb {
    // args[0] gives file path + name for the ascii file of lexical
    // info ("lexicon.txt"). If none specified, we try to find it in
    // the directory containing the class file "UtMakeVcb.class"
    public static void main(String[] args) {
        String fnLexicon = args.length > 0? args[0] : null;
        if (fnLexicon == null) {
            String path = UtMakeVcb.class.getResource("UtMakeVcb.class").getPath();
            fnLexicon = path.replace("UtMakeVcb.class", "lexicon.txt");
        }
        Vcb vcb;
        try {
            vcb = new MakeVcb().createVcb(fnLexicon);
        } catch (IOException e) {
            throw new RuntimeException("MakeVcb::make IO failure", e);
        }
        System.out.println("Created vocabulary");
        System.out.println("Info for selected words:");
        vcb.ut();
    }
}

