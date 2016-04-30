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
import java.util.*;
import msp.xfrm.Xfrm;

/** Main class for the msp package. To parse text represented as a
* String, create an Msp object and call its "parseString(String src)"
* method. To parse the entire contents of a file, use the "parseFile"
* method. To parse a very big file, use "processFile". */
public class Msp {
    /** the parser */
    Parser parser;
    /** At startup we read a binary file of initialization data,
    * "msp.dat". This is included in the distribution. The
    * constructor accepts an arg giving an InputStream opened to this
    * file. If you supply a null value, it will try to open a stream
    * to this file, expecting to find it in the directory containing
    * "Msp.class". This works so long as you include
    * "msp.dat" in the directory containing the msp class file.
    * If you're working in Android, you shoult treat the initialization
    * an an app resource -- see README.txt for sample code. */
    public Msp(InputStream mspDat) throws IOException {
        if (mspDat != null) {
            parser = new Parser(mspDat);
        } else {
            String path = Msp.class.getResource("Msp.class").getPath();
            String fn = path.replace("Msp.class", "msp.dat");
            mspDat = new FileInputStream(fn);
            parser = new Parser(mspDat);
            mspDat.close();
        }
    }
    
    /** Parse text represented as a string. Returns list of parse
    * nodes. */
    public List<Nd> parseString(String text) {
        try {
            return parser.parseSrc(text, null, -1);
        } catch (IOException e) {
            // This exception cannot occur: the try/catch is required
            // by Java's checked-exception scheme, but this code will
            // never be executed.
            return null;
        }
    }
    
    /** Parse contents of a file. Returns list of parse nodes. */
    public List<Nd> parseFile(InputStream src) throws IOException {
        List<Nd> nds = parser.parseSrc(src, null, -1);
        src.close();
        return nds;
    }
    
    /** Read and parse a file in sections, passing the parse of each
    * section over to a delegate for processing. "maxlines"
    * determines the size of the sections: if a given section exceeds
    * "maxlines", we continue reading and parsing until we hit a
    * blank or indented line, then declare the section complete and
    * parse it. The object here is to support the processing of very
    * large files, without blowing the host memory resources. */
    public void processFile(InputStream src,
    IProcessFile delegate, int maxlines) throws IOException {
        parser.parseSrc(src, delegate, maxlines);
        src.close();
    }
    
    /** Convert a list of parse nodes into XML. "loc" means include
    * location attributes in the xml. */
    public String toXml(List<Nd> nds, boolean loc) {
        StringBuilder xml = new StringBuilder(
        "<?xml version=\"1.0\" standalone=\"yes\"?>\n");
        xml.append("<msp>\n");
        for (Nd nd: nds) {
            xml.append(nd.toXml(loc));
            xml.append("\n");
        }
        xml.append("</msp>\n");
        return xml.toString();
    }
    
    /** dev/text: print the parse rules to "msp.lst" */
    public void printParser(){
        parser.printme();
        
    }
    
    /** dev/test: enable/disable trace */
    public void setTraceParse(boolean enable) {
        Xfrm.traceparse = enable;
    }
}


