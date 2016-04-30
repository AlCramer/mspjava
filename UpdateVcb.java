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

/**
* Initialize vocabulary from "lexicon.txt", an ascii file containing
* information about words (lists of verbs, etc.).
*/
package msp;
import java.io.*;
import msp.lex.*;
public class UpdateVcb {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
"Usage: UpdateVcb fnlexicon fnMspDat\n" +
"   fnlexicon: filepath for \"lexicon.txt\"\n" +
"   fnMspDat: filepath for \"msp.dat\"\n" +
"The MSP parser is initialized from the binary file \"msp.dat\", " +
"which contains lexical data and parse rules. To expand the vocabulary\n" +
"of the parser, edit the ascii file \"lexicon.txt\", then run this program.\n" +
"It will rewrite \"msp.dat\" to reflect the changes made to \"lexicon.txt\"."
            );
            System.exit(1);
        }
        String fnlexicon = args[0];
        String fndat = args[1];
        try {
            // create a parser object: this reads in "msp.dat"
            InputStream in = new FileInputStream(fndat);
            Parser parser = new Parser(in);
            in.close();
            // re-create the vocabulary from the ascii file
            // "lexicon.txt" and assign to the parser.
            parser.vcb = (new MakeVcb()).createVcb(fnlexicon);
            // write out "msp.dat"
            OutputStream out = new FileOutputStream(fndat);
            parser.serialize(out, "w");
            out.close();
            System.out.println(String.format("rewrote %s", fndat));
        } catch (IOException e) {
            throw new RuntimeException("UpdateVcb::make IO failure", e);
        }
    }
} // end class UpdateVcb






