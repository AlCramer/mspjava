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
import java.util.ArrayList;
import java.util.List;

/** Test program for msparse. See usage msg in main for details. */
// This class needed for the test of Msp.processFile. It finds
// top-level parse nodes that have an experiencer ("I saw ....", "She
// thought ....") and writes them to an output file.
class ProcessFileTest implements IProcessFile {
    PrintWriter fp;
    ProcessFileTest(PrintWriter fp) {
        super();
        this.fp = fp;
    }

    public void processParse(List<MSNode> nds) {
        for (MSNode nd:nds) {
            if (nd.getSubnodes("exper").size() > 0) {
                    fp.print(nd.text+"\n");
                    fp.print(nd.toXml(false)+"\n");
            }
        }
    }
}

/** test harness for msp */
public class MspTest {
    // compare 2 collections of strings and return index of
    // divergence. -1 means they're the same.
    static int compStrArrays(String[] ref, String[] test) {
        int i = 0;
        while (i < ref.length) {
            if (i >= test.length) {
                return i;
            }
            String li = ref[i].trim();
            if ((li.length() != 0) && !li.equals(test[i].trim())) {
                return i;
            }
            i += 1;
        }
        return -1;
    }
    // read file into array of strings
    static String[] readLines(String fn) throws IOException {
        FileReader fileReader = new FileReader(fn);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines.toArray(new String[lines.size()]);
    }
    // write out file for a failed comparison
    static void writeFailFile(String fn,String xml) throws IOException {
        FileOutputStream outStream = new FileOutputStream(fn);
        PrintWriter fpOut = new PrintWriter(
            new BufferedOutputStream(outStream),true);
        fpOut.println(xml);
        fpOut.close();
    }
    // qa test: parse a test source & compare against canned results.
    static void qaTest(Msp msp) throws  IOException{
        String path =
            MspTest.class.getResource("MspTest.class").getPath();
        path = path.replace("MspTest.class","_fn_");
        // get reference file as array of strings
        String[] refLines = readLines(path.replace("_fn_","qaref.xml"));
        // test1: parse "qasrc.txt"
        FileInputStream inStream = new FileInputStream(
            path.replace("_fn_","qasrc.txt"));
        String xml =
            msp.toXml(msp.parseFile(inStream),false);
        // compare
        int iDiverge = compStrArrays(refLines,xml.split("\n"));
        if (iDiverge != -1) {
            String fn = path.replace("_fn_","qa.xml");
            System.out.println(String.format(
                "Fail parseFile test. See %s, line %d", fn, iDiverge+1));
            writeFailFile(fn,xml);
            return;
        }
        // test2: read in "qasrc.txt", join the lines to form a single
        // string, and parse.
        String[] srcLines = readLines(path.replace("_fn_","qasrc.txt"));
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<srcLines.length; i++) {
            sb.append(srcLines[i].trim());
            sb.append("\n");
        }
        xml = msp.toXml(msp.parseString(sb.toString()),false);
        // compare
        iDiverge = compStrArrays(refLines,xml.split("\n"));
        if (iDiverge != -1) {
            String fn = path.replace("_fn_","qa.xml");
            System.out.println(String.format(
                "Fail parseString test. See %s, line %d", fn, iDiverge+1));
            writeFailFile(fn,xml);
            return;
        }
        System.out.println("Pass QA test");
    }

    public static void main(String[] args) {
        String usage =
"Usage:\n" +
"MspTest opt* [-i] [-f inFile outFile]\n" +
"    \"opt*\" specifies 0 or more options. These are:\n" +
"-loc: include source location attributes in xml nodes \n" +
"-trace: trace the parse (dev/test)\n " +
"    \"-i\" means loop interactively, displaying the parse \n" +
"for text entered by the user. \n" +
"    \"-f\" means parse contents of file \"inFile\", writing \n" +
"results to \"outFile\" as XML. \n" +
"    \"-qa\" parses \"qasrc.txt\" and compares against \"qaref.xml\".";
        // Create an Msp object.
        Msp msp = null;
        try {
            msp = new Msp(null);
        } catch (IOException e) {
            System.out.println("Msp initialization error:");
            e.printStackTrace();
            System.exit(1);
        }
        // Process the args to main
        boolean showloc = false;
        if (args.length == 0) {
            System.out.println(usage);
            System.exit(1);
        }
        String inFile = null;
        String outFile = null;
        String action = null;
        int i = 0;
        while (i<args.length) {
            String a = args[i];
            if (a.equals("-h") || a.equals("-help") || a.equals("-0") ||
                a.equals("h") || a.equals("help") || a.equals("0")) {
                System.out.println(usage);
                System.exit(1);
            }
           if (a.equals("-f") || a.equals("-process")) {
                action = a;
                i++;
                if (i+1 >= args.length) {
                    System.out.println( "Error: Expected file names");
                    System.out.println(usage);
                    System.exit(1);
                }
                inFile = args[i++];
                outFile = args[i++];
                continue;
            }
            if (a.equals("-loc")) {
                showloc = true;
            } else if (a.equals("-trace")) {
                msp.setTraceParse(true);
            } else if (a.equals("-utol")) {
                // undocumented option: run the unit test in
                // ObjLst.java
                ObjLst._ut();
                System.exit(1);
            } else if (a.equals("-print")) {
                // undocumented: write parse rules to stdout
                PrintWriter fp = new PrintWriter(System.out);
                try {
                    msp.printParser(fp);
                } catch (IOException e) {
                    System.out.println("Msp printParser error:");
                    e.printStackTrace();
                    System.exit(1);
                }
            } else if (a.equals("-vocab")) {
                // undocumented: loop and show vocab info
                action = a;
            } else if (a.equals("-i") || a.equals("-qa")) {
                action = a;
            } else {
                System.out.println("Unknown option: " + a);
                System.out.println(usage);
        System.exit(1);
            }
            i += 1;
        }
        if (action == null) {
            System.out.println(usage);
            System.exit(1);
        }
        try {
            if (action.equals("-f") || action.equals("-process")) {
                FileInputStream inStream = new FileInputStream(inFile);
                FileOutputStream outStream = new FileOutputStream(outFile);
                PrintWriter fpOut = new PrintWriter(
                    new BufferedOutputStream(outStream),true);
                if (action.equals("-f")) {
                    fpOut.println(
                        msp.toXml(msp.parseFile(inStream),showloc));
                } else {
                    msp.processFile(inStream,
                        new ProcessFileTest(fpOut),2);
                }
                inStream.close();
                fpOut.close();
                fpOut.close();
                System.out.println("Created " + outFile);
                return;
            }
            if (action.equals("-qa")) {
                qaTest(msp);
                return;
            }
            // We're in interactive mode: loop, accepting user input.
            // and displaying the parse result.
            System.out.println("Enter text (\"q\" to quit):");
            BufferedReader bufferRead =
                new BufferedReader( new InputStreamReader(System.in));
            while (true) {
                String src = bufferRead.readLine();
                if (src != null) {
                    if (src.equals("q") || src.equals("quit")) {
                        break;
                    }
                    if (action.equals("-i")) {
                        System.out.println(
                            msp.toXml(msp.parseString(src),showloc));
                    } else if (action.equals("-vocab")) {
                           msp.parser.vcb.printWrdInfo(src);
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
