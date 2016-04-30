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
package msp.lex;
import java.util.*;
import msp.util.*;

// helper class for "Lexer".
public class LexRec {
    // list of token values
    public ILst toks = new ILst();
    // parallel list: location of token "i" in the source
    // text.
    public ILst tokLoc = new ILst();
    public LexRec() {
        super();
    }
}



