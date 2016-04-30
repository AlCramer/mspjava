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


public class UtParseBlk {
    public static void main(String[] args) {
        String src =
        "\"Hello, \" John F. Kennedy said.\"That's $12, 000.00 (approximately)!\"";
        Lexer lex = new Lexer();
        List<ParseBlk> blks = lex.getParseBlks(src, 1);
        ParseBlk.printList(blks, 0);
    }
}


