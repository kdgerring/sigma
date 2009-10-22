
package com.articulate.sigma;

import java.io.*;
import java.util.*;

/** This code is copyright Articulate Software (c) 2004.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.See also http://sigmakee.sourceforge.net

This class maps ontologies. It includes embedded subclasses that
implement specific mapping heuristics.

This class also includes utilities for converting other
ad-hoc formats to KIF
   */
public class Mapping {

    public static TreeMap mappings = new TreeMap();

    /** *************************************************************
     *  Write synonymousExternalConcept expressions for term pairs
     *  given in cbset.
     *  @return error messages if necessary
     */
    public static String writeEquivalences(TreeSet cbset, String kbname1, String kbname2) throws IOException {

        System.out.println("INFO in Mapping.writeEquivalences()");
        FileWriter fw = null;
        PrintWriter pw = null; 
        String dir = (String) KBmanager.getMgr().getPref("baseDir");
        String filename = dir + File.separator + kbname1 + "-links.kif";

        if (mappings.keySet().size() < 1) 
            return "Error: No mappings found";
        
        try {
            fw = new FileWriter(filename);
            pw = new PrintWriter(fw);        
            Iterator it = mappings.keySet().iterator();
            while (it.hasNext()) {
                String term1 = (String) it.next();
                TreeMap value = (TreeMap) mappings.get(term1);
                Iterator it2 = value.keySet().iterator();
                int counter = 0;
                while (it2.hasNext()) {
                    counter++;
                    Integer score = (Integer) it2.next();
                    String term2 = (String) value.get(score);
                    String topScoreFlag = "";
                    if (counter == 1) 
                        topScoreFlag = "T_";
                    String cbName = "checkbox_" + topScoreFlag + term1 + "_" + term2;
                    if (cbset.contains(cbName)) {
                        pw.println("(synonymousExternalConcept \"" + term2 + 
                                   "\" " + term1 + " " + kbname2 + ")");
                    }
                }
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
        return "Wrote: " + filename;
    }

    /** *************************************************************
     *  rename terms in KB kbname2 to conform to names in kbname1
     *  @return error messages if necessary
     */
    public static String merge(TreeSet cbset, String kbname1, String kbname2) {

        System.out.println("INFO in Mapping.merge()");
        if (mappings.keySet().size() < 1) 
            return "Error: No mappings found";

        KB kb1 = KBmanager.getMgr().getKB(kbname1);
        KB kb2 = KBmanager.getMgr().getKB(kbname2);
        Iterator it = mappings.keySet().iterator();
        while (it.hasNext()) {
            String term1 = (String) it.next();
            TreeMap value = (TreeMap) mappings.get(term1);
            System.out.println("INFO in Mapping.merge(): outer loop, examining " + term1);
            Iterator it2 = value.keySet().iterator();
            int counter = 0;
            while (it2.hasNext()) {
                counter++;
                Integer score = (Integer) it2.next();
                String term2 = (String) value.get(score);
                System.out.println("INFO in Mapping.merge(): inner loop, examiing " + term2);
                String topScoreFlag = "";
                if (counter == 1) 
                    topScoreFlag = "T_";
                String cbName = "checkbox_" + topScoreFlag + term1 + "-" + term2;
                String subName = "sub_checkbox_" + topScoreFlag + term1 + "-" + term2;
                if (cbset.contains(cbName) && !term2.equals(term1))
                    kb2.rename(term2,term1);                    
                if (cbset.contains(subName)) {
                    if (kb2.isInstance(term2)) {
                        kb2.tell("(instance " + term2 + " " + term1 + ")");
                        System.out.println("(instance " + term2 + " " + term1 + ")");
                    }
                    else {
                        kb2.tell("(subclass " + term2 + " " + term1 + ")");
                        System.out.println("(subclass " + term2 + " " + term1 + ")");
                    }
                }
            }           
        }
        String dir = (String) KBmanager.getMgr().getPref("baseDir");
        String filename = dir + File.separator + kbname2 + "-merged-" + kbname1;
        try {
            File f = new File(filename + ".kif");
            int counter = 0;
            while (f.exists()) {
                counter++;
                f = new File(filename + counter + ".kif");
            }
            if (counter == 0) 
                filename = filename + ".kif";
            else
                filename = filename + counter + ".kif";
            kb2.writeFile(filename);
            kb1.addConstituent(filename);
            KBmanager.getMgr().removeKB(kbname2);
        }
        catch (java.io.IOException e) {
            return "Error writing file " + filename + "\n" + e.getMessage();
        }
        return "Successful renaming of terms in " + kbname2 + " to those in " + kbname1;
    }

    /** *************************************************************
    *   Convert a YAGO file into KIF
     */
    public static void convertYAGO(String file, String relName) throws IOException {

        File f = new File(file);
        if (f == null) {
            System.out.println( "INFO in convertYAGO(): " 
                                + "The file " + file + " does not exist" );
            return;
        }
        FileReader r = new FileReader(f);
        LineNumberReader lr = new LineNumberReader(r);
        String line = null;
        while ((line = lr.readLine()) != null) {
            line = line.trim();
            if (line != null && line.length() > 0) {
                int tab1 = line.indexOf("\t");
                int tab2 = line.indexOf("\t",tab1+1);
                int tab3 = line.indexOf("\t",tab2+1);
                String term1 = line.substring(tab1+1,tab2);
                String term2 = line.substring(tab2+1,tab3);
                term1 = OWLtranslator.StringToKIFid(term1);
                term2 = OWLtranslator.StringToKIFid(term2);
                System.out.println("(" + relName + " " + term1 + " " + term2 + ")");
            }
        }
    }

    /** *************************************************************
    *   Get the termFormat label for a term.  Return only the first
    *   such label.  Return null if no label.
     */
    public static String getTermFormat(KB kb, String term) {

        if (kb != null) {
            ArrayList al = kb.askWithRestriction(0,"termFormat",2,term);
            if (al != null && al.size() > 0) {
                Formula f = (Formula) al.get(0);
                String t = f.getArgument(3);
                t = OWLtranslator.removeQuotes(t);
                return t;
            }
        }
        return null;
    }

    /** *************************************************************
    *   @return the minimum of two ints
     */
    private static int min(int n1, int n2) {

        if (n1<n2) 
            return n1;
        else
            return n2;
    }

    /** *************************************************************
     * Map ontologies through 4 methods:
     * (1) identical term names
     * (2) substrings of term names are equal
     * (3) terms align to words in the same WordNet synset
     * (4) extra "points" for having terms that align with the same
     * structural arrangement
     * 
     * @return a TreeMap where the key is a term from the first
     *         ontology and the value is another TreeMap.  The
     *         internal TreeMap has keys that are an integer mapping
     *         score and the values are terms from the second
     *         ontology.
     */
    public static void mapOntologies(String kbName1, String kbName2, int threshold) {

        System.out.println("INFO in Mapping.mapOntologies()");
        boolean jaroWinkler = false;    // map terms using Jaro-Winkler method or just substring
        TreeMap result = new TreeMap();
        int counter = 0;
        KB kb1,kb2;
        kb1 = KBmanager.getMgr().getKB(kbName1);
        kb2 = KBmanager.getMgr().getKB(kbName2);
        if (kb1 != null && kb2 != null) {
            Iterator it1 = kb1.terms.iterator();
            while (it1.hasNext()) {
                counter++;
                if (counter > 100) {
                    System.out.print(".");
                    counter = 0;
                }
                String term1 = (String) it1.next();
                if (isValidTerm(term1)) {
                    String normTerm1 = normalize(term1);
                    String normLabel1 = normalize(getTermFormat(kb1,term1));
                    TreeMap tm = (TreeMap) result.get(term1);
                    if (tm == null) 
                        tm = new TreeMap();
                    Iterator it2 = kb2.terms.iterator();
                    while (it2.hasNext()) {
                        String term2 = (String) it2.next();
                        if (isValidTerm(term2)) {
                            String normTerm2 = normalize(term2);
                            String normLabel2 = normalize(getTermFormat(kb2,term2));
                            int score = Integer.MAX_VALUE;
                            if (jaroWinkler) 
                                score = min(score,getJaroWinklerDistance(normTerm1, kb1, normTerm2, kb2));
                            else 
                                score = min(score,getSubstringDistance(normTerm1, kb1, normTerm2, kb2));
                            if (normLabel1 != null && isValidTerm(normLabel1)) {
                                if (jaroWinkler)
                                    score = min(score,getJaroWinklerDistance(normLabel1, kb1, normTerm2, kb2));
                                else 
                                    score = min(score,getSubstringDistance(normLabel1, kb1, normTerm2, kb2));
                            }
                            if (normLabel2 != null && isValidTerm(normLabel2)) {
                                if (jaroWinkler)
                                    score = min(score,getJaroWinklerDistance(normTerm1, kb1, normLabel2, kb2));
                                else 
                                    score = min(score,getSubstringDistance(normTerm1, kb1, normLabel2, kb2));
                            }
                            if (normLabel1 != null && normLabel2 != null && 
                                isValidTerm(normLabel1) && isValidTerm(normLabel2)) {
                                if (jaroWinkler)
                                    score = min(score,getJaroWinklerDistance(normLabel1, kb1, normLabel2, kb2));
                                else 
                                    score = min(score,getSubstringDistance(normLabel1, kb1, normLabel2, kb2));
                            }
                            if (score > 0 && score < Integer.MAX_VALUE)
                                if (score < threshold)                                     
                                    tm.put(new Integer(score), term2);
                        }
                    }
                    if (tm.keySet().size() > 0)
                        result.put(term1,tm);
                }
            }
        }
        else {
            if (kb1 == null)
                System.out.println(kbName1 + " not found<P>\n");
            if (kb2 == null)
                System.out.println(kbName2 + " not found<P>\n");            
        }
        System.out.println();
        mappings = result;
    }

    /** *************************************************************
     *   check whether a term is valid (worthy of being compared)
     */
    public static boolean isValidTerm(String term) {

        return term.length() > 2 && !Formula.isLogicalOperator(term);
    }

    /** *************************************************************
     *   Normalize a string by replacing all non-letter, non-digit
     *   characters with spaces, adding spaces on capitalization
     *   boundaries, and then converting to lower case
     */
    public static String normalize(String s) {

        if (s == null || s.length() < 1)
            return null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if ((Character.isLetter(s.charAt(i)) && Character.isLowerCase(s.charAt(i))) ||
                Character.isDigit(s.charAt(i))) 
                result.append(s.charAt(i));
            else {
                if (Character.isLetter(s.charAt(i)) && Character.isUpperCase(s.charAt(i))) {
                    if (result.length() < 1 || result.charAt(result.length()-1) != ' ') 
                        result.append(" ");
                    result.append(Character.toLowerCase(s.charAt(i)));
                }
                else 
                    if (result.length() < 1 || result.charAt(result.length()-1) != ' ') 
                        result.append(" ");             
            }
        }
        return result.toString();
    }

    /** *************************************************************
     *  Substring Mapping Method: returns 1 if the two strings
     *  are identical, scores >1 if one string is a substring of
     *  the other, and Integer.MAX_VALUE if there is no substring
     *  match
     *
     *  This approach is based on:
     *  John Li, "LOM: A Lexicon-based Ontology Mapping Tool",
     *  Proceedings of the Performance Metrics for Intelligent
     *  Systems  (PerMIS.'04), 2004.
     *
     *  *** This is not yet fully implemented here ***
     */
    public static int getSubstringDistance(String term1, KB kb1, String term2, KB kb2) {

        if (term1.equals(term2))
            return 1;
        else if (term1.indexOf(term2) > -1)
            return term1.indexOf(term2) + 
                    (term1.length() - term2.length());
        else if (term2.indexOf(term1) > -1)
            return term2.indexOf(term1) + (term2.length() - term1.length());
        else
            return Integer.MAX_VALUE;
    }

    /** *************************************************************
     *  Jaro-Winkler Mapping Method
     *  implemented by Gerard de Melo
     */
    public static int getJaroWinklerDistance(String s1, KB kb1, String s2, KB kb2) {

        int SCALING_FACTOR = 10000;
        int winklerMaxPrefixLen = 4;
        double winklerPrefixWeight = 0.1;

        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0)
            return SCALING_FACTOR;
        
        // without loss of generality assume s1 is longer
        if (len1 < len2) {
            String t = s1;
            s1 = s2;
            s2 = t;
            len1 = len2;
            len2 = s2.length();
        }
        
        // count and flag the matched pairs
        int maxDistance = (len1 >= 4) ? (int) Math.floor(len1 / 2) - 1 : 0;
        boolean[] s1Matches = new boolean[len1]; // initialized to false
        boolean[] s2Matches = new boolean[len2]; // initialized to false
        int nMatches = 0;
        for (int i = 0; i < len2; i++) { // index in s2
            char c = s2.charAt(i);
            int jStart = (i > maxDistance) ? i - maxDistance : 0;
            int jEnd = i + maxDistance + 1;
            if (jEnd > len1)
                jEnd = len1;
            for (int j = jStart; j < jEnd; j++) { // possible matching positions within s1
                if (!s1Matches[j] && c == s1.charAt(j)) {
                    s1Matches[j] = true;
                    s2Matches[i] = true;
                    nMatches++;
                    break;
                }
            }
        }
        
        if (nMatches == 0)
            return SCALING_FACTOR;
        
        // count transpositions
        int nTranspositions = 0;
        int k = 0;
        for (int i = 0; i < len2; i++) // index in s2
            if (s2Matches[i]) {
                int j;
                for (j = k; j < len1; j++)
                    if (s1Matches[j]) {
                        k = j + 1;
                        break;
                    }
                    if (s2.charAt(i) != s1.charAt(j))
                        nTranspositions++;
                }
        int halfTranspositions = nTranspositions / 2;

        double jaroScore = ((double) nMatches / len1 
                           +(double) nMatches / len2 
                           +(double) (nMatches - halfTranspositions) / nMatches)
                           / 3.0;

        // Winkler bias
        int cMaxPrefixLen = winklerMaxPrefixLen;
        if (len1 < cMaxPrefixLen)
            cMaxPrefixLen = len1;
        if (len2 < cMaxPrefixLen)
            cMaxPrefixLen = len2;
        int l = 0;
        while (l < cMaxPrefixLen)
            if (s1.charAt(l) == s2.charAt(l))
                l++;
            else
                break;
        double jaroWinklerScore = jaroScore + l * winklerPrefixWeight * (1.0 - jaroScore);
        
        // return as a distance value such that larger
        // values indicate greater distances
        return (int) (SCALING_FACTOR * (1.0 - jaroWinklerScore));
    }    

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {
        // read(args[0]);

        try {
          KBmanager.getMgr().initializeOnce();
        } 
        catch (Exception e ) {
          System.out.println(e.getMessage());
        }
        Mapping.mapOntologies("SUMO","OBO",10);

        //System.out.println(m.normalize("Philippe_Mex-s"));
        //System.out.println(m.normalize("AntiguaAndBarbuda"));
        //System.out.println(m.normalize("SUMO"));
/***
        try {
            convertYAGO("TypeExtractor.txt","citizen");
        } 
        catch (Exception e ) {
          System.out.println(e.getMessage());
          e.printStackTrace();
        }
        */
    }

}


