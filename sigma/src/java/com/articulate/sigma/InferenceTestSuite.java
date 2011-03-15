package com.articulate.sigma;

import java.util.*;
import java.io.*;
//import javax.servlet.jsp.*;
import TPTPWorld.SystemOnTPTP;
import TPTPWorld.InterfaceTPTP;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

/** A framework for doing a series of assertions and queries, and for comparing
 *  the actual result of queries against an expected result.  Also will 
 *  keep track of the time needed for each query.  Tests are expected in files
 *  with a .tq extension contained in a directory specified by the 
 *  "inferenceTestDir" parameter, and results are provided in the same directory 
 *  with a .res extension.
 * 
 *  The test files contain legal KIF expressions including several kinds of
 *  meta-information.  Meta-predicates include (note <String>), (query <Formula>),
 *  and (answer <term1>..<termn>).  There may be only one note and query statements,
 *  but there may be several answer statements, if multiple binding sets are
 *  expected.
 * 
 *  Comments are allowed in test files, and are signified by a ';', after which
 *  all content on the line is ignored.
 * 
 *  Note that since answers are provided in an ordered list, without reference
 *  to their respective variable names, that the inference engine is assumed to
 *  return bindings in the same order.
 */
public class InferenceTestSuite {

    /** Total time */
    public static long totalTime = 0;

    /** Default timeout for queries with unspecified timeouts */
    public static int _DEFAULT_TIMEOUT = 600;

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if any pair of answers is different.  Return false otherwise.
     */
    private static boolean compareAnswers(ProofProcessor pp, ArrayList answerList) {

        System.out.println("INFO in InferenceTestSuite.compareAnswers(): num answers: " + String.valueOf(pp.numAnswers()));
        for (int j = 0; j < pp.numAnswers(); j++) {
            System.out.println("INFO in InferenceTestSuite.compareAnswers(): result: " + pp.returnAnswer(j) + " expected: " + (String) answerList.get(j));
            if (!pp.equalsAnswer(j,(String) answerList.get(j)))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     */
    private static File setOutputDir() throws IOException {

        String outputDirPath = KBmanager.getMgr().getPref("testOutputDir");
        if ((outputDirPath != null) && !outputDirPath.equals(""))            
            return new File(outputDirPath);  // testOutputDir is set.     
        else
            return null;
    }

    /** ***************************************************************
     *  Note that files is modified as a side effect.
     *  @return error messages, or null if none
     */
    private static String getTestFiles(ArrayList<File> files, File outputDir) throws IOException  {

        String inferenceTestDirPath = KBmanager.getMgr().getPref("inferenceTestDir");
        if ((inferenceTestDirPath == null) || inferenceTestDirPath.equals("")) 
            return("Error in InferenceTestSuite.getTestFiles(): The Sigma preference \"inferenceTestDir\" has not been set");
        
        File inferenceTestDir = new File(inferenceTestDirPath);
        if (!inferenceTestDir.isDirectory() && !inferenceTestDir.mkdir()) 
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " + inferenceTestDir.getCanonicalPath());
        
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            File baseDir = new File(KBmanager.getMgr().getPref("baseDir"));
            if (baseDir.isDirectory()) {
                File webappsDir = new File(baseDir,"webapps");
                if (webappsDir.isDirectory()) {
                    File waSigmaDir = new File(webappsDir,"sigma");
                    if (waSigmaDir.isDirectory()) 
                        outputDir = new File(waSigmaDir,"tests");                    
                }
            }
        }
        if ((outputDir == null) || !outputDir.isDirectory()) 
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " + outputDir);        

        // At this point, inferenceTestDir and outputDir shouild be
        // set to viable values.
        File[] newfiles = inferenceTestDir.listFiles();
        System.out.println("INFO in InferenceTestSuite.getTestFiles(): number of files: " + newfiles.length); 
        if (newfiles == null || newfiles.length == 0) {
            System.out.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        Arrays.sort(newfiles);
        for (int i = 0; i < newfiles.length; i++) {
            if (newfiles[i].getName().endsWith(".tq")) 
                files.add(newfiles[i]);            
        }
        if (files.size() < 1) {
            System.out.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        return null;  // Normal exit, with "files" modified as a side effect
    }

    /** ***************************************************************
     */
    private static String askSoTPTP(String processedStmt, int timeout, int maxAnswers,
                                    String systemChosen, KB kb, String TPTPlocation) throws Exception {

        System.out.println("INFO in InferenceTestSuite.askSoTPTP()"); 
        String tptpresult = "";
        Formula conjectureFormula;
        conjectureFormula = new Formula();
        conjectureFormula.theFormula = processedStmt;
        conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
        conjectureFormula.tptpParse(true,kb);
        String kbFileName = kb.writeTPTPFile(null,conjectureFormula,true,
                                             systemChosen,false);
        InterfaceTPTP.init();
        String res = InterfaceTPTP.callTPTP(TPTPlocation, systemChosen, kbFileName,
                                            timeout, "-q3", "-S");
        StringTokenizer st = new StringTokenizer(res,"\n");
        String temp = "";
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!next.equals("") && !next.substring(0,1).equals("%")) 
                temp += next + "\n";                                
        }
        tptpresult = res;
        res = temp;
        if (SystemOnTPTP.isTheorem(tptpresult)) 
            return TPTP2SUMO.convert(res,false);                            
        else 
            return "<queryResponse>\n<answer result=\"no\" number=\"0\">\n</answer>\n" +
                   "<summary proofs=\"0\"/>\n</queryResponse>\n";                            
    }

    /** ***************************************************************
     * Convenience method that sets default parameters
    */
    public static String test(KB kb) throws IOException  {
        return test(kb, null, _DEFAULT_TIMEOUT, "");
    }

    /** ***************************************************************
     * Convenience method that sets some default parameters
     * @param timeout is a default timeout that is likely to be
     *                overwritten by a specification in the test
     *                data
    */
    public static String test(KB kb, String systemChosen, int timeout) throws IOException  {
        return test(kb, systemChosen, timeout, "");
    }

    /** ***************************************************************
     * The main method that controls running a set of tests and returning
     * the result as an HTML page showing test results and links to proofs.
     * Note that this procedure deletes any prior user assertions.
     */
    public static String test(KB kb, String systemChosen, int defaultTimeout, String TPTPlocation) 
        throws IOException {

        if (TPTPlocation == "" || TPTPlocation == null) {
            TPTPlocation = KBmanager.getMgr().getPref("systemsDir");
        }
        System.out.println("INFO in InferenceTestSuite.test(): Note that any prior user assertions will be deleted.");
        System.out.println("INFO in InferenceTestSuite.test(): Prover: " + systemChosen);
        StringBuffer result = new StringBuffer();
        FileWriter fw = null;
        int fail = 0;
        int pass = 0;
        PrintWriter pw = null;
        String proof = null;
        String processedStmt = null;

        String language = "EnglishLanguage";
        int maxAnswers = 1;
        totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File outputDir = setOutputDir();
        ArrayList<File> files = new ArrayList();
        String error = getTestFiles(files,outputDir);
        if (error != null) 
            return error;

        File allResultsFile = new File(outputDir, "TestSuiteResults");
        FileWriter afw = new FileWriter(allResultsFile);
        PrintWriter apw = new PrintWriter(afw);
        if (systemChosen.equals("STP2")) {
            TreeSet<String> processedForms = kb.preProcess(kb.formulaMap.keySet());
            STP2 stp2 = new STP2(processedForms);
            kb.engineMap.put("STP2",stp2);
        }

        System.out.println("INFO in InferenceTestSuite.test(): number of files: " + files.size());
        for (int i = 0; i < files.size(); i++) {
            // This probably isn't necessary.  Make this a preferences parameter.
            int timeout = defaultTimeout;
            kb.deleteUserAssertions();   
            File f = (File) files.get(i);
            if (f.getCanonicalPath().endsWith(".tq")) {
                System.out.println();
                System.out.println("STARTING TEST #" + i + " of " + files.size() + " : " + f.getName());
                System.out.println();

                KIF test = new KIF();
                try {
                    test.readFile(f.getCanonicalPath());
                }
                catch (Exception e) {
                    return("Error in InferenceTestSuite.test(): exception reading file: " + f.getCanonicalPath() + ". " + e.getMessage());
                }
                File outfile = new File(outputDir, f.getName());
                try {
                    test.writeFile(outfile.getCanonicalPath());
                }
                catch (IOException ioe) {
                    return("Error in InferenceTestSuite.test(): exception writing file: " + outfile.getCanonicalPath() + ". " + ioe.getMessage());
                }
                System.out.println("INFO in InferenceTestSuite.test(): num formulas: " + String.valueOf(test.formulaSet.size()));
                Iterator it = test.formulaSet.iterator();
                String note = f.getName();
                String query = null;
                ArrayList answerList = new ArrayList();
                while (it.hasNext()) {
                    String formula = (String) it.next();
                    if (formula.indexOf(";") != -1)
                        formula = formula.substring(0,formula.indexOf(";"));
                    System.out.println("INFO in InferenceTestSuite.test(): Formula: " + formula);
                    if (formula.startsWith("(note")) 
                        note = formula.substring(6,formula.length()-1);
                    else if (formula.startsWith("(query")) 
                        query = formula.substring(7,formula.length()-1);
                    else if (formula.startsWith("(answer")) 
                        answerList.add(formula.substring(8,formula.length()-1));
                    else if (formula.startsWith("(time")) 
                        timeout = Integer.parseInt(formula.substring(6,formula.length()-1));
                    else {
                        if (systemChosen.equals("STP2")) 
                            kb.tellSTP2(formula);   
                        else
                            kb.tell(formula);
                    }
                }
                maxAnswers = answerList.size();
                try {
                    System.out.println("INFO in InferenceTestSuite.test(): Query: " + query);

                    Formula theQuery = new Formula();
                    ArrayList theQueries = null;
                    theQuery.theFormula = query;

                    theQueries = theQuery.preProcess(true,kb);
                    Iterator q = theQueries.iterator();
                    while (q.hasNext()) {
                        processedStmt = ((Formula)q.next()).theFormula;
                        long start = System.currentTimeMillis();
                        if (systemChosen != null && systemChosen.equals("KIF-Vampire")) {
                            System.out.println("INFO in InferenceTestSuite.test(): Query is posed to Vampire ");
                            proof = kb.ask(processedStmt,timeout,maxAnswers);                        
                        }
                        else if (systemChosen.equals("STP2")) 
                            proof = kb.askSTP2(processedStmt,timeout,maxAnswers);                       
                        else  // SoTPTP:
                            proof = askSoTPTP(processedStmt,timeout,maxAnswers,systemChosen,kb, TPTPlocation);                       

                        duration = System.currentTimeMillis() - start;                        
                        System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                        System.out.println(duration);
                        totalTime = totalTime + duration;
                    }
                }
                catch (Exception ex) {
                    result = result.append("<br>Error in InferenceTestSuite.test() while executing query " +
					   f.getName() + ": " + ex.getMessage() + "<br>");
                }
                String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
                String rfn = f.getName();
                String resultsFilename = rfn.substring(0,rfn.length()-3) + "-res.html";
                File resultsFile = new File(outputDir, resultsFilename);
                try {
                    fw = new FileWriter(resultsFile);
                    pw = new PrintWriter(fw);
                    pw.println(HTMLformatter.formatProofResult(proof,query,processedStmt,lineHtml,kb.name,language));
                }
                catch (java.io.IOException e) {
                    throw new IOException("Error writing file " + resultsFile.getCanonicalPath());
                }
                finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                        if (fw != null) {
                            fw.close();
                        }
                    }
                    catch (Exception ex) {
                    }
                }
                boolean different = true;
                if (proof != null) {
                    BasicXMLparser res = new BasicXMLparser(proof);
                    ProofProcessor pp = new ProofProcessor(res.elements);
                    different = compareAnswers(pp,answerList);
                }
                String resultString = "";
                if (different) {
                    resultString = "fail";
                    fail++;
                }
                else {
                    resultString = "succeed";
                    pass++;
                }
                result = result.append("<tr><td>" + note + "</td><td><a href=\"" + outputDir.getName() + 
                                       "/" + f.getName() + "\">" + f.getName() + "</a></td>");
                result = result.append("<td><a href=\"" + outputDir.getName() + "/" + resultsFile.getName() + 
                                       "\">" + resultString + "</a></td>");
                result = result.append("<td>" + String.valueOf(duration) + "</td></tr>\n");
            }
            System.out.println("FINISHED TEST #" + i + " of " + files.size() + " : " + f.getName());
        }
        System.out.println();
        System.out.println("ALL TEST QUERIES FINISHED");
        System.out.println();

        result = result.append("</table><P>\n");
        result = result.append("Total time: ");
        result = result.append(String.valueOf(totalTime/1000));
        result = result.append(" seconds<P>\n");

        result = result.append("Total correct: ");
        result = result.append(String.valueOf(pass));
        result = result.append("<P>\n");
        
        result = result.append("Total failed: ");
        result = result.append(String.valueOf(fail));
        result = result.append("<P>\n");
        kb.deleteUserAssertions();
        return result.toString();
    }

    /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
            KBmanager.getMgr().setPref("inferenceTestDir","/home/apease/infTest");
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println(InferenceTestSuite.test(kb, "STP2", 10));
        } catch (Exception e) {
            System.out.println("Error in InferenceTestSuite.main()");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }
}
 
