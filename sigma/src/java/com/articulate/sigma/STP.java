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

package com.articulate.sigma;

import java.io.*;
import java.util.*;

import com.sun.org.apache.bcel.internal.generic.NEW;

/** ***************************************************************
 * The Sigma theorem prover. A simple resolution prover in Java.
 */
public class STP extends InferenceEngine {

     /** The knowledge base */
    ArrayList<Formula> formulas = new ArrayList();

    /** Previously solved lemmas that is used to prevent cycles  */
    TreeMap<String, ArrayList<Formula>> lemmas = new TreeMap();

    /** To Be Used - also has a list of axioms used to derive the
     *  clause. */
    ArrayList<AnotherAVP> TBU = new ArrayList();

    //TreeSet<String> current = new TreeSet();
    //TreeSet<String> failed = new TreeSet();

    /** The indexes */
    TreeMap<String, Formula> negLits = new TreeMap();  // Note that (not (a b c)) will be stored as (a b c)
    TreeMap<String, Formula> posLits = new TreeMap();  // Appearance of positive clauses
    TreeMap<String, Integer> termCounts = new TreeMap();
    TreeMap<String, ArrayList<Formula>> posTermPointers = new TreeMap(); // appearance of a term in a positive literal
    TreeMap<String, ArrayList<Formula>> negTermPointers = new TreeMap(); // appearance of a term in a negative literal

    /** ***************************************************************
     * Convert to a String.
     */
    public String toString() {

        return "An STP instance";
    }

    /** ***************************************************************
     * Convert to a String.
     */
    public static class STPEngineFactory extends EngineFactory {

        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {  
            return new STP(formulaSource);
        }

        public InferenceEngine createFromKBFile(String kbFileName) {
            return STP.getNewInstance(kbFileName);
        }
    }

    /** *************************************************************
     */
    private STP(String kbFileName) throws Exception {
    
        String error = null;
               
        File kbFile = null;
        if (error == null) {
            kbFile = new File(kbFileName);
            if (!kbFile.exists() ) {
                error = ("The file " + kbFileName + " does not exist");
                System.out.println("INFO in STP(): " + error);
                KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                             + "\n<br/>" + error + "\n<br/>");
            }
        }
        
        if (error == null) {
            KIF kif = new KIF();
            kif.readFile(kbFileName);

            Iterator it = kif.formulas.values().iterator();
            while (it.hasNext()) {
                ArrayList<Formula> al = (ArrayList) it.next();
                formulas.addAll(al);
            }
        }
        
        clausifyFormulas();
        buildIndexes();
    }    

    /** *************************************************************
     */
    public STP(Iterable<String> formulaSource) { 
    
        Iterator it = formulaSource.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Formula f = new Formula();
            f.read(s);
            formulas.add(f);
        }

        clausifyFormulas();
        System.out.println("INFO in STP(): clausified formulas: " + formulas);
        buildIndexes();
    }
    
    /** *************************************************************
     */
    public static STP getNewInstance(String kbFileName) {

        STP res = null;
        try {
            res = new STP(kbFileName);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return res;
    }

    /** *************************************************************
     */
    private void clausifyFormulas() {

        ArrayList<Formula> newFormulas = new ArrayList();
        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
            f = f.clausify();
            if (f.car().equals("and")) {
                ArrayList<Formula> al = f.separateConjunctions();
                for (int i = 0; i < al.size(); i++) {
                    Formula f2 = (Formula) al.get(i);
                    newFormulas.add(f2);
                }
            }
            else
                newFormulas.add(f);
            //System.out.println("INFO in STP.clausifyFormulas(): " + f);
        }
        formulas = newFormulas;
    }

    /** *************************************************************
     *  Add a new term pointer to either posTermPointers or
     *  negTermPointers
     */
    private void addToPointers(TreeMap<String, ArrayList<Formula>> pointers, String clause, Formula f) {

        ArrayList<Formula> al = null;
        if (pointers.get(clause) == null) {
            al = new ArrayList();
            pointers.put(clause,al);
        }
        else
            al = (ArrayList) pointers.get(clause);
        al.add(f);
    }
    
    /** *************************************************************
     *  Side effect: Add pointers to f in the negTermPointers Map
     *  for the terms occurring in clause
     */
    private void addNegTermPointers(String clause, Formula f) {

        Formula c = new Formula();
        c.read(clause);
        while (!c.empty()) {
            String car = c.car();
            if (Formula.atom(car)) {
                if (!Formula.isVariable(car) && !Formula.LOGICAL_OPERATORS.contains(car)) 
                    addToPointers(negTermPointers,car,f);            
            }
            else 
                addNegTermPointers(car,f);
            c.read(c.cdr());
        }
    }
    
    /** *************************************************************
     *  Side effect: Add pointers to f in the posTermPointers Map
     *  for the terms occurring in clause
     */
    private void addPosTermPointers(String clause, Formula f) {

        Formula c = new Formula();
        c.read(clause);
        while (!c.empty()) {
            String car = c.car();
            if (Formula.atom(car)) {
                if (!Formula.isVariable(car) && !Formula.LOGICAL_OPERATORS.contains(car)) 
                    addToPointers(posTermPointers,car,f);            
            }
            else 
                addPosTermPointers(car,f);            
            c.read(c.cdr());
        }
    }


    /** *************************************************************
     */
    private void indexOneFormula(Formula f) {

        ArrayList<String> terms = f.collectTerms();                // count the appearances of terms
        Iterator it2 = terms.iterator();
        while (it2.hasNext()) {
            String s = (String) it2.next();
            if (termCounts.keySet().contains(s)) {
                Integer i = (Integer) termCounts.get(s);
                termCounts.put(s,new Integer(i.intValue() + 1));
            }
            else
                termCounts.put(s,new Integer(1));
        }

        Formula fclause = f.clausify();
        ArrayList<Formula> clauseList = new ArrayList();
        if (fclause.car().equals("and"))
            clauseList = fclause.separateConjunctions();
        else
            clauseList.add(fclause);                  // a formula that is not a conjunction will result in a clauseList of one element
        for (int i = 0; i < clauseList.size(); i++) {
            fclause = (Formula) clauseList.get(i);
            Formula clauses = new Formula();
            if (fclause.isSimpleClause() || fclause.isSimpleNegatedClause()) 
                clauses.read(fclause.theFormula);
            else
                clauses.read(fclause.cdr());  // should remove the initial "or"
            while (!clauses.empty()) {
                //System.out.println("INFO in STP.buildIndexes(): clauses: " + clauses);
                String clause = null;
                if (!clauses.isSimpleClause() && !clauses.isSimpleNegatedClause()) {
                    clause = clauses.car();
                }
                else {
                    clause = clauses.theFormula;
                    clauses.theFormula = "()";
                }
                clause = Formula.normalizeVariables(clause);
                Formula c = new Formula();
                c.read(clause);
                String negP = c.car();
                if (negP.equals("not")) {        // note that if there are multiple such formulas only one will be kept
                    negLits.put(c.cdr(),f);
                    addNegTermPointers(c.cdr(),f);
                }
                else {
                    posLits.put(clause,f);
                    addPosTermPointers(clause,f);
                }
                if (!clauses.empty())                 
                    clauses.read(clauses.cdr());
            }
        }
    }

    /** *************************************************************
     */
    private void buildIndexes() {

        Iterator<Formula> it = formulas.iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
            indexOneFormula(f);
        }
       // System.out.println("INFO in STP.buildIndexes(): negTermPointers: " + negTermPointers);
       // System.out.println("INFO in STP.buildIndexes(): posTermPointers: " + posTermPointers);
    }

    /** *************************************************************
     *  Note that this class will sort into reverse order, with the
     *  largest integer values first
     */
    public class AnotherAVP implements Comparable {

        public int intval = 0;
        public Formula form = null;

        public String toString() {
            return String.valueOf(intval) + "\n" + form.toString();
        }
        public boolean equals(AnotherAVP avp) {
            return form.equals(avp.form);
        }
        public int hashCode() {
            return form.hashCode();
        }
        public int compareTo(Object avp) throws ClassCastException {

            if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.STP$AnotherAVP")) 
                throw new ClassCastException("Error in AnotherAVP.compareTo(): "
                                             + "Class cast exception for argument of class: " 
                                             + avp.getClass().getName());
            return ((AnotherAVP) avp).intval - intval;
        }
    }

    /** *************************************************************
     *  Collect Formulas, ranked by string length (better would be
     *  number of clauses)
     *  @param negated indicates whether to select only those
     *                 Formulas in which the terms appear in negated
     *                 clauses, or vice versa
     */
    private ArrayList<AnotherAVP> collectCandidates(ArrayList<String> terms, String negated) {

        //System.out.println("INFO in STP.collectCandidates(): " + terms);
        //System.out.println("INFO in STP.collectCandidates(): negated " + negated);
        TreeMap<Formula, Integer> tm = new TreeMap();
        ArrayList result = new ArrayList();
        Iterator it = terms.iterator();
        while (it.hasNext()) {      // find formulas that have all the terms as the clause to be proven
            String term = (String) it.next();
            if (!Formula.LOGICAL_OPERATORS.contains(term) && !Formula.isVariable(term)) {
                ArrayList<Formula> pointers = null;
                if (negated.equals("true")) 
                    pointers = (ArrayList) posTermPointers.get(term);
                else if (negated.equals("false")) 
                    pointers = (ArrayList) negTermPointers.get(term);
                else if (negated.equals("both")) {
                    pointers = (ArrayList) posTermPointers.get(term);
                    ArrayList<Formula> morePointers = (ArrayList) negTermPointers.get(term);
                    if (pointers != null) {
                        if (morePointers != null) 
                            pointers.addAll(morePointers);                        
                    }
                    else
                        pointers = morePointers;
                    //System.out.println("INFO in STP.collectCandidates(): pointers " + pointers);
                }
                else
                    System.out.println("Error in STP.collectCandidates(): negated must be true, false or both " + negated);
                if (pointers != null) {
                    for (int i = 0; i < pointers.size(); i ++) {
                        Formula f = (Formula) pointers.get(i);
                        if (!tm.keySet().contains(f)) {
                            Integer count = new Integer(0);
                            tm.put(f,count);
                        }
                        Integer newCount = (Integer) tm.get(f);
                        newCount = new Integer(newCount.intValue() + 1);
                        tm.put(f,newCount);
                    }
                }
            }
        }

        it = tm.keySet().iterator();
        while (it.hasNext()) {      // find formulas ordered by the number of terms from the clause to be proven
            Formula f = (Formula) it.next();
            Integer num = (Integer) tm.get(f);
            AnotherAVP avp = new AnotherAVP();
            //avp.intval = num.intValue();
            avp.intval = 10000-f.theFormula.length();   // sort by smallest size axiom is best (first)
            avp.form = f;
            result.add(avp);
        }
        Collections.sort(result);

        System.out.println("INFO in STP.collectCandidates(): result " + result);
        return result;
    }

    /** *************************************************************
     *  Find support for a formula
     *  @return an ArrayList of Formulas that consitute support for
     *          the clause.  Return an empty ArrayList if no proof
     *          is found.
     */
    private ArrayList<Formula> prove() {

        ArrayList<Formula> result = new ArrayList();
        while (TBU.size() > 0) {
            System.out.println("\n\nINFO in STP.prove(): TBU: " + TBU);
            //System.out.println("INFO in STP.prove(): lemmas: " + lemmas);
            AnotherAVP avp = (AnotherAVP) TBU.remove(0);
            //if (!lemmas.containsKey(form)) {
            String norm = Formula.normalizeVariables(avp.form.theFormula); 

            //System.out.println("INFO in STP.prove(): attempting to prove: " + avp.form);
            Formula f = new Formula();
            f.read(norm);

            ArrayList<String> al = f.collectTerms();
            ArrayList<AnotherAVP> candidates = null;
            if (f.isSimpleClause()) 
                candidates = collectCandidates(al,"false");                
            else if (f.isSimpleNegatedClause()) 
                candidates = collectCandidates(al,"true");                
            else
                candidates = collectCandidates(al,"both");                

            if (candidates != null && candidates.size() > 0) {
                for (int i = 0; i < candidates.size(); i++) {
                    AnotherAVP avpCan = (AnotherAVP) candidates.get(i);
                    Formula candidate = avpCan.form;
                    //System.out.println("INFO in STP.prove(): checking candidate:\n" + candidate);
                    Formula resultForm = new Formula();
                    TreeMap mappings = f.resolve(candidate,resultForm);
                    if (resultForm != null && resultForm.empty()) {
                        ArrayList support = new ArrayList();
                        if (lemmas.get(avpCan.form.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(avpCan.form));
                        if (lemmas.get(f.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(f.theFormula));
                        support.add(f);
                        support.add(avpCan.form);
                        return support;
                    }
                    if (mappings != null && mappings.keySet().size() > 0) {
                        System.out.println("\nINFO in STP.prove(): resolve result:\n" + resultForm);
                        System.out.println("for candidate\n" + candidate + "\n with formula\n " + avp.form);
                        ArrayList support = new ArrayList();
                        if (lemmas.get(avpCan.form.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(avpCan.form));
                        if (lemmas.get(f.theFormula) != null) 
                            support.addAll((ArrayList) lemmas.get(f.theFormula));
                        support.add(f);
                        support.add(avpCan.form);
                        lemmas.put(resultForm.theFormula,support);
                        AnotherAVP avpNew = new AnotherAVP();
                        if (!formulas.contains(resultForm) && !TBU.contains(avpNew)) {
                            avpNew.form = resultForm;
                            avpNew.intval = 10000-resultForm.theFormula.length();
                            TBU.add(avpNew);
                            Collections.sort(TBU);
                        }
                    }
                    //else
                        //System.out.println("INFO in STP.prove(): candidate did not resolve\n" + candidate);                    
                }
            }
            //indexOneFormula(f);       // all lemmas must be added to the knowledge base for completeness
            //formulas.add(f);
            //}
        }
        return result;
    }

    /** *************************************************************
     * Submit a query.
     *
     * @param formula query in the KIF syntax (not negated)
     * @param timeLimit time limit for answering the query (in seconds)
     * @param bindingsLimit limit on the number of bindings
     * @return answer to the query (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String submitQuery (String formula,int timeLimit,int bindingsLimit) {

        ArrayList result = new ArrayList();
        Formula negQuery = new Formula();
        negQuery.read("(not " + formula + ")");
        negQuery = negQuery.clausify();     // negation will be pushed in
        System.out.println("INFO in STP.submitQuery(): clausified query: " + negQuery);
        AnotherAVP avp = null;
        if (negQuery.car().equals("and")) {
            ArrayList<Formula> al = negQuery.separateConjunctions();
            for (int i = 0; i < al.size(); i++) {
                Formula f2 = (Formula) al.get(i);
                avp = new AnotherAVP();
                avp.form = f2;
                avp.intval = f2.theFormula.length();
                TBU.add(avp);
                Collections.sort(TBU);
                System.out.println("INFO in STP.submitQuery(): adding to TBU: " + avp);
            }
        }
        else {
            avp = new AnotherAVP();
            avp.form = negQuery;
            avp.intval = negQuery.theFormula.length();
            TBU.add(avp);
            Collections.sort(TBU);
        }
        ArrayList<Formula> res = prove();
        if (res != null && res.size() > 0) 
            return "Success! " + res.toString();      // success if any clause in the disjunction is proven        
        return "fail";          // getting here means each clause failed to be proven
    }

    /** *************************************************************
     * Add an assertion.
     *
     * @param formula asserted formula in the KIF syntax
     * @return answer to the assertion (in the XML syntax)
     * @throws IOException should not normally be thrown
     */
    @Override
    public String assertFormula(String formula) throws IOException {

        //formulas.add(formula);
        //Formulas asserted through this method will always be used.
        
        return null;
    }
    
    /** *************************************************************
     * Terminates this instance of InferenceEngine. 
     * <font color='red'><b>Warning:</b></font>After calling this functions
     * no further assertions or queries can be done.
     * 
     * Some inference engines might not need/support termination. In that case this
     * method does nothing.
     *
     * @throws IOException should not normally be thrown
     */
    public void terminate()
    	throws IOException
    {
    }

    /** *************************************************************
     */
    public static void tq1Abbrev() {

        ArrayList al = new ArrayList();
        al.add("(=> (i ?X290 C) (exists (?X12) (m ?X12 ?X290)))");
        al.add("(s O C)");
        al.add("(i SOC SOC)");
        al.add("(i Org1-1 O)");
        al.add("(=> (s ?X403 ?X404) (and (i ?X403 SOC) (i ?X404 SOC)))");
        al.add("(=> (and (i ?X403 SOC) (i ?X404 SOC)) " +
               "(=> (and (s ?X403 ?X404) (i ?X405 ?X403)) (i ?X405 ?X404)))");
        Formula query = new Formula();
        query.read("(exists (?MEMBER) (m ?MEMBER Org1-1))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 

        /*
(or
  (not (instance ?X3 Collection))
  (member (SkFn 1 ?X3) ?X3)), 

(subclass Organization Collection), 

(or
  (instance ?X6 SetOrClass)
  (not (subclass ?X7 ?X6))), 

(or
  (instance ?X8 SetOrClass)
  (not (subclass ?X8 ?X9))), 

(or
  (not (instance ?X13 SetOrClass))
  (not (instance ?X14 SetOrClass))
  (not (subclass ?X13 ?X14))
  (not (instance ?X15 ?X13))
  (instance ?X15 ?X14))

query:
(not (member ?X33 Org1-1))

(not (instance Org1-1 Collection))

(or
  (not (instance ?X13 SetOrClass))
  (not (instance Collection SetOrClass))
  (not (subclass ?X13 Collection))
  (not (instance Org1-1 ?X13)))


        */
    }

    /** *************************************************************
     */
    public static void tq1() {

        ArrayList al = new ArrayList();
        al.add("(=> (instance ?X290 Collection) (exists (?X12) (member ?X12 ?X290)))");
        al.add("(subclass Organization Collection)");
        al.add("(instance SetOrClass SetOrClass)");
        al.add("(instance Org1-1 Organization)");
        al.add("(=> (subclass ?X403 ?X404) (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)))");
        al.add("(=> (and (instance ?X403 SetOrClass) (instance ?X404 SetOrClass)) " +
               "(=> (and (subclass ?X403 ?X404) (instance ?X405 ?X403)) (instance ?X405 ?X404)))");
        Formula query = new Formula();
        query.read("(exists (?MEMBER) (member ?MEMBER Org1-1))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    }

    /** *************************************************************
     */
    public static void tq2() {

        ArrayList al = new ArrayList();
        al.add("(=> (p ?X) (q ?X))");
        al.add("(=> (or (q ?X) (r ?X)) (t ?X))");
        al.add("(p a)");
        Formula query = new Formula();
        query.read("(or (t a) (r a))");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    }

    /** *************************************************************
     *  example from Russel and Norvig
     */
    public static void rnTest() {

        ArrayList<String> al = new ArrayList();
        al.add("(=> (and (attribute ?X American) (instance ?Y Weapon) (instance ?Z Nation) " +
                        "(attribute ?Z Hostile) (instance ?S Selling) (agent ?S ?X) (patient ?S ?Y) (recipient ?S ?Z))" +
                   "(attribute ?X Criminal))");
        al.add("(possesses Nono M1-Missile)");
        al.add("(instance M1-Missile Missile)");
        al.add("(=> (and (possesses Nono ?X) (instance ?X Missile))"+
                   "(and (instance ?S Selling) (agent ?S West) (patient ?S ?X) (recipient ?S Nono)))");
        al.add("(=> (instance ?X Missile) (instance ?X Weapon))");
        al.add("(=> (enemies ?X America) (attribute ?X Hostile))");
        al.add("(attribute West American)");
        al.add("(instance Nono Nation)");
        al.add("(enemies Nono America)");
        al.add("(instance America Nation)");
        Formula query = new Formula();
        query.read("(attribute ?X Criminal)");

        System.out.println("********************************************");
        Iterator<String> it = al.iterator();
        while (it.hasNext()) {
            Formula f = new Formula();
            String s = (String) it.next();
            f.read(s);
            System.out.println(f);
        }
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        /**
        ArrayList al = new ArrayList();
        al.add("(instance Adam Human)");
        al.add("(=> (instance ?Y Human) (attribute ?Y Mortal))");
        Formula query = new Formula();
        query.read("(attribute ?X Mortal)");
        STP stp = new STP(al);
        System.out.println(stp.submitQuery(query.theFormula,0,0)); 
    */
        //tq1();
        //tq2();
        rnTest();
/**
         ArrayList al = new ArrayList();
        STP stp = new STP(al);
        Formula f1 = new Formula();
        f1.read("(or (not (instance Org1-1 Collection)) (member (SkFn 1 Org1-1) Org1-1))");
        Formula f2 = new Formula();
        f2.read("(not (member (SkFn 1 Org1-1) Org1-1))");
        System.out.println(stp.removeClause(f1,f2));
        */
    }
}

