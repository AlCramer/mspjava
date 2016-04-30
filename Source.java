// Copyright 2012 Al Cramer
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
package msp;
import java.io.*;

/** This class encapsulates access to the text be parsed: either
* contents of a file, or a string representation of the text. */
class Source {
    // The source is either a String representation of the text, or an
    // input stream to an ascii file.
    String string;
    BufferedReader fp;
    // index into string
    int ix;
    // end of source?
    boolean eofsrc;
    // line number and indent for current line
    int lno;
    int indent;
    // text for current section
    String sectText;
    // line number and indent for current sect
    int sectLno;
    int sectIndent;
    // number of blank lines preceding the section
    int sectBlank;
    // look-ahead line. This line belongs to the NEXT section that
    // will be returned by "getSection".
    String peekLi;
    int peekLiLno;
    int peekLiIndent;
    // "contentProvider" can be either a String, or a file.
    Source(Object contentProvider) {
        if (contentProvider instanceof String) {
            this.string = (String)contentProvider;
        } else {
            InputStreamReader inputStreamReader =
            new InputStreamReader((InputStream)contentProvider);
            this.fp = new BufferedReader(inputStreamReader);
        }
    }
    
    /** Get (stripped) next line from source (null if at
    * end-of-source). this.lno and this.indent give the line-number
    * and indent of the line. */
    String getline() throws IOException {
        if (eofsrc) {
            return null;
        }
        String li;
        if (fp != null) {
            li = fp.readLine();
            if (li == null) {
                eofsrc = true;
                return null;
            }
        } else {
            int S = ix;
            int E = S;
            while ((E<string.length()) && (string.charAt(E) != '\n')) {
                E += 1;
            }
            li = string.substring(S, E);
            // the newline is considered part of this line
            ix = E + 1;
            if (ix >= string.length()) {
                eofsrc = true;
            }
        }
        lno += 1;
        indent = 0;
        for (int i=0; i<li.length(); i++) {
            char c = li.charAt(i);
            if (c == ' ') {
                indent += 1;
            } else if (c == '\t') {
                indent += 4;
            } else {
                break;
            }
        }
        return li.trim();
    }
    // Get section of source for parsing: returns false if
    // end-of-source. Text for section is written to "sectText".
    boolean getSection() throws IOException {
        // get "li": first line in this section. It may have been
        // read-in in the preceding call to "getSection".
        String li;
        if (peekLi == null) {
            li = getline();
            if (li == null) {
                // source has been exhausted
                return false;
            }
            sectLno = lno;
            sectIndent = indent;
        } else {
            li = peekLi;
            sectLno = peekLiLno;
            sectIndent = peekLiIndent;
        }
        // skip over initial blank lines (but keep count)
        sectBlank = 0;
        while ((li != null) && (li.length() == 0)) {
            li = getline();
            sectLno = lno;
            sectIndent = indent;
            sectBlank += 1;
        }
        if (li == null) {
            // source has been exhausted
            return false;
        }
        // "li" is the first line of the section
        StringBuilder sb = new StringBuilder(li.trim());
        while (true) {
            li = getline();
            if (li == null) {
                // done
                break;
            }
            // if the line is blank or indented, it will be the first
            // line of the next section.
            if ((li.length() == 0) || (indent > sectIndent)) {
                peekLi = li;
                peekLiLno = lno;
                peekLiIndent = indent;
                break;
            }
            // this line is part of the current section
            sb.append("\n");
            sb.append(li.trim());
        }
        sectText = sb.toString();
        return true;
    }
}


