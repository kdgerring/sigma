package com.articulate.sigma;

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

import java.util.*;
import java.io.*;
import java.text.ParseException;
//import java.util.logging.Logger;
//import java.util.logging.SimpleFormatter;
//import java.util.logging.FileHandler;
//import java.util.logging.Level;

import java.util.logging.Logger;
import java.util.regex.*;

/** Process results from the Vampire inference engine.
 */
public class ProofProcessor {

	
	private static Logger logger;
	//private static FileHandler file;
	
     /** An ArrayList of BasicXMLelement (s). */
    private ArrayList xml = null;

    /** ***************************************************************
     * Take an ArrayList of BasicXMLelement (s) and process them as
     * needed
     */
    public ProofProcessor(ArrayList xmlInput) {

    	if (logger == null)
    		logger = Logger.getLogger(this.getClass().getName());
    	    	
        xml = new ArrayList(xmlInput);
        //System.out.print("INFO in ProofProcessor(): Number of XML elements is: ");
        //System.out.println(xmlInput.size());
        
        //LOGGER = Logger.getLogger("SIGMA_LOGGER");

        //LOGGER.finest("-----------ProofProcessor Created-----------");
        //LOGGER.finest("XML INPUT: " + xmlInput.toString());
        //LOGGER.finest("------------------------------------");
    }
    
    /** ***************************************************************
     * Compare the answer with the expected answer.  Note that this method
     * is very unforgiving in that it requires the exact same format for the 
     * expected answer, including the order of variables.
     */
    public boolean equalsAnswer(int answerNum, String expectedAnswer) {

        StringBuffer result = new StringBuffer();
         /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("no")) 
            return false;
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("yes") &&
            (expectedAnswer.equalsIgnoreCase("yes"))) 
            return true;
        BasicXMLelement bindingSet = (BasicXMLelement) answer.subelements.get(0);
	if ( bindingSet != null ) {
	    String attr =  (String) bindingSet.attributes.get("type");
	    if ( (attr == null) || !(attr.equalsIgnoreCase("definite")) ) 
		return false;	    
	    BasicXMLelement binding = (BasicXMLelement) bindingSet.subelements.get(0); 
            // The bindingSet element should just have one subelement, since non-definite answers are rejected.
	    for (int j = 0; j < binding.subelements.size(); j++) {
		BasicXMLelement variableBinding = (BasicXMLelement) binding.subelements.get(j);
		String variable = (String) variableBinding.attributes.get("name");
		String value = (String) variableBinding.attributes.get("value");
		result = result.append("(" + variable + " " + value + ")");
		if (j < binding.subelements.size()-1) 
		    result = result.append(" ");
	    }
	}

        // System.out.println("INFO in ProofProcessor().equalsAnswer: answer: " + result.toString() + " expected answer: " + expectedAnswer);
        return result.toString().equalsIgnoreCase(expectedAnswer);
    }
    
    public String returnAnswer(int answerNum) {
    	return returnAnswer(answerNum, "");
    }
    
    /** ***************************************************************
     * Return the variable name and binding for the given answer.
     */
    public String returnAnswer(int answerNum, String query) {

    	//LOGGER.finest("---------- RETURN ANSWER -----------");
        StringBuffer result = new StringBuffer();
        ArrayList<String> skolemTypes = new ArrayList<String>();
         /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);
        if (((String) answer.attributes.get("result")).equalsIgnoreCase("no")) 
            return "no";
        BasicXMLelement bindingSet = (BasicXMLelement) answer.subelements.get(0);
        if (bindingSet.tagname.equalsIgnoreCase("proof")) {
            result = result.append("[" + (String) answer.attributes.get("result") + "] ");
            return result.toString();
        }
        result = result.append("[" + (String) bindingSet.attributes.get("type") + "] ");
        for (int i = 0; i < bindingSet.subelements.size(); i++) {
            BasicXMLelement binding = (BasicXMLelement) bindingSet.subelements.get(i);
            for (int j = 0; j < binding.subelements.size(); j++) {
                BasicXMLelement variableBinding = (BasicXMLelement) binding.subelements.get(j);
                String variable = (String) variableBinding.attributes.get("name");
                String value = (String) variableBinding.attributes.get("value");

                //see if a skolem function is present in the value (skolem functions are labeled sk[0-9]+
                if(value.matches(".*?sk[0-9]+.*?")) {
                	//LOGGER.finest("SKOLEM FUNCTION found!");
                	String skolemType = findSkolemType(answerNum, value, query, variable);
                	if (skolemType != ""){
                		skolemTypes.add("; " + value + " is of type " + skolemType);
                	}
                }

                result = result.append(variable + " = " + value);
                if (j < binding.subelements.size()-1) 
                    result = result.append(",  ");
            }
            if (i < bindingSet.subelements.size()-1) 
                result = result.append(" , ");
            while(skolemTypes.size() > 0) {
            	result = result.append(skolemTypes.get(0));
            	skolemTypes.remove(0);
            }
            result.append(";");
        }
        
        //LOGGER.finest("returned answer: " + result.toString());
        return result.toString();
    }

    //looks for skolem function from proofsteps if query is not given
    private ArrayList<String> returnSkolemStmt(String skolem, ArrayList proofSteps) {
    	// two types of skolem functions:
    	// one with arguments, for instance: (sk0 Human123) or
    	// one without, for example: sk2
    	// need to find either of these in the proofs to see what relationship it goes into
    	
    	if (skolem.startsWith("(") && skolem.endsWith(")")) 
    		skolem = skolem.substring(1, skolem.length()-1);
    	//LOGGER.finest("skolem value: " + skolem);
    	skolem = skolem.split(" ")[0];
    	Pattern pattern = Pattern.compile("(\\([^\\(|.]*?\\(" + skolem + " .+?\\).*?\\)|\\([^\\(|.]*?" + skolem + "[^\\)|.]*?\\))");
    	Matcher match;

    	ArrayList<String> matches = new ArrayList<String>();
    	for (int i=0; i<proofSteps.size(); i++) {
    		ProofStep step = (ProofStep) proofSteps.get(i);
	  		match = pattern.matcher(step.axiom);
	   		//LOGGER.finest("axiom: " + step.axiom);
			while (match.find()) {
				//LOGGER.finest("--- match found from axiom ---");
				for(int j=1; j<=match.groupCount(); j++) {
					if(!matches.contains(match.group(j)))
						matches.add(match.group(j));
				}
		   	}
    	}    	    	
    	//LOGGER.finest("returnSkolemStmt matches: " + matches);
    	if (matches.size()>0)
    		return matches;
    	return null;
    }
    
    //looks for skolem variable if a query string is given
    private ArrayList<String> returnSkolemStmt(String query, String variable) {
    	if (!StringUtil.emptyString(query)) {
    		query = query.replaceAll("\\" + variable, "_SKOLEM");
    		//LOGGER.finest("returnSkolemStmt VARIABLE: " + variable);
    		//LOGGER.finest("returnSkolemStmt QUERY: " + query);
    		Pattern pattern = Pattern.compile("(\\([^\\(\\)]*?_SKOLEM[^\\)\\(]*?\\))");
    		Matcher match = pattern.matcher(query);

    		while (match.find()) {
    			ArrayList<String> matches = new ArrayList<String>();
    			//LOGGER.finest("groupMatch: " + match.groupCount());
    			for (int i=1; i<=match.groupCount(); i++) {
    				//LOGGER.finest("returnSkolemStmt match: " + match.group(i));
    				if(!matches.contains(match.group(i)))
    					matches.add(match.group(i));
    			}
    			
    	    	//LOGGER.finest("returnSkolemStmt matches: " + matches);
    			return matches;   		
    		}
    	}    	
    	return null;
    }

    /** *********************************************************************************
     * 
     * @param answerNum The nth answer in the result set
     * @param value The value in the bindingSet being analyzed
     * @return
     */
    private String findSkolemType(int answerNum, String value, String query, String variable) {    	
    	ArrayList<ProofStep> proofSteps = getProofSteps(answerNum); 
    	ArrayList<String> skolemRelationArr;
    	// try and look for the skolem function in the proofSteps and determine the 
    	// relation statement it appears in
    	if (query == "")
    		skolemRelationArr = returnSkolemStmt(value, proofSteps);
    	else
    		skolemRelationArr = returnSkolemStmt(query, variable);

    	//LOGGER.finest("---------- FIND SKOLEM TYPE ------------");
    	
    	if (skolemRelationArr != null) {   		
    		for(int j=0; j<skolemRelationArr.size(); j++) {
    			String skolemRelation = skolemRelationArr.get(j);
	        	skolemRelation = skolemRelation.substring(1, skolemRelation.length()-1);
	
	        	// prepare skolem function to have the form sk0 .+? or sk0 (for skolem functions that don't have an argument)
	        	// because value from answer contains an instance and not necessarily a variable
	        	String skolem = value;
	        	if(skolem.startsWith("(") && skolem.endsWith(")"))
	        			skolem = value.substring(1, value.length()-1);
	        	skolem = skolem.split(" ")[0];
	        	               	       	
	           	// remove skolem and replace with temp variable
	        	skolemRelation = skolemRelation.replaceAll("\\("+ skolem + " [^\\)]+?\\)", "_SKOLEM");
	        	skolemRelation = skolemRelation.replaceAll(skolem, "_SKOLEM");
	        	// remove all other skolem functions in the skolemRelation (if present) and replace with temp variable
	        	skolemRelation = skolemRelation.replaceAll("\\(.+?\\)", "?TEMP");
	        	//LOGGER.finest("skolemRelation: " + skolemRelation);

	        	if(skolemRelation.matches("instance\\s_SKOLEM\\s[^\\s]+")) {
	        		String[] arguments = skolemRelation.split(" ");
	        		return arguments[arguments.length-1];
	        	}
	        	
			    // assemble regex for skolemRelation
	      	    String[] skolemArguments = skolemRelation.split(" ");
			    StringBuffer regexStmt = new StringBuffer();
			    		    
			    for (int i=0; i < skolemArguments.length; i++) {
			    	if (skolemArguments[i].equals("_SKOLEM")) {
			    		regexStmt.append("([^\\s\\(\\)]+) ");
			    	}
			    	else if (skolemArguments[i].startsWith("?") || i!=0) {
			    		regexStmt.append("[^\\s\\(\\)]+ ");
			    	}
			    	else {
			    		regexStmt.append(skolemArguments[i] + " ");
			    	}
			    }
			    	
			    regexStmt.deleteCharAt(regexStmt.length()-1);
			    regexStmt.insert(0, "\\(");
			    regexStmt.insert(regexStmt.length(), "\\)");
			    	
			    // resulting regexStmt from something like (relationshipName ?X0 _SKOLEM)
			    // should be \\(relationshipName [^\\s\\(\\)]+ ([^\\s\\(\\)]+)\\)
			    //LOGGER.finest("Regex Statement: " + regexStmt.toString());
			    	
			    Pattern pattern = Pattern.compile(regexStmt.toString());
			    Matcher match;
			    
			    // look for the presence of above pattern in each of the proof steps
			    for(int i=0; i<proofSteps.size(); i++){
			    	ProofStep proof = (ProofStep)proofSteps.get(i);
			    	String varName = "";
			    	
			    	//LOGGER.finest("axiom: " + proof.axiom);
		    		match = pattern.matcher(proof.axiom);
		    		
		    		boolean varNameFound = false;
		
		    		// if it is found, extract the variable name being used
		    		// and then see if an (instance ?VARNAME ?CLASS) relationship can be found
		    		// that defines the class membership of varName
		    		while (match.find()) {
		    			//LOGGER.finest("found " + match.groupCount() + " relationship statements in proof!");
		    			int k = 1;
		    			while(k <= match.groupCount() && !varNameFound) {
		    				varName = match.group(k);
		    				//LOGGER.finest("candidate varname: " + varName);
		    				if (varName.startsWith("?"))
		    					varNameFound = true;
		    				k++;
		    			
			    			if(varNameFound) {
				      			//LOGGER.finest("varname: " + varName);
				      			String regexString = ".*?\\(instance \\" + varName + " ([^\\s\\)]+)\\).*?";
				      			Pattern varPattern = Pattern.compile(regexString);
				      			match = varPattern.matcher(proof.axiom);
					    		if(match.find()) {
					    			//LOGGER.finest("match is found!");
					    			if (match.group(1) != null)
					    				return match.group(1);	    
					    			else varNameFound = false;					    		
					    		}
					    		else varNameFound = false;
				    		}
		    			}
		    		}
			    }
		    }
    	}
    	return "cannot be determined.";
    }
       


    /** ***************************************************************
     * Remove the $answer clause that Vampire returns, including any
     * surrounding "or".
     */
    private String removeAnswerClause(String st) {

    	//LOGGER.finest("---- removeAnswerClause----");
        if (st.indexOf("$answer") == -1)
        	return st;
        
       	// clean the substring with "answer" in it
        st = st.replaceAll("\\(\\$answer\\s[\\(sk[0-9]+\\s[^\\)]+?\\)|[^\\(\\)]+?]+?\\)", "");

        /**
        st = st.trim();
        if (st.indexOf("$answer") == 1)
            return st.substring(9,st.length()-1);
        if (st.substring(0,3).equalsIgnoreCase("(or")) {
            int answer = st.indexOf("$answer");
            int end = st.indexOf(")",answer);
            st = st.substring(0,answer-1) + st.substring(end+2,st.length());
            return st.substring(3,st.length());
        } **/
    	
        //count number of nested statements if statement starts with (or
        //if nested statements is more than 2, keep or. If it is exactly 2 --
        //which means it's just (or plus one other statement, remove or.

        if(st.substring(0,3).equalsIgnoreCase("(or")){
        	boolean done = false;

	        String substr = st.substring(4, st.length()-1);
	        while(!done) {
	        	String statement = " SUMO-AXIOM";
	        	substr = substr.replaceAll("\\([^\\(|^\\)]+\\)", statement);
	        	substr = substr.replaceAll("\\(not\\s[^\\(|^\\)]+\\)", statement);
	        	//LOGGER.finest("TEST: " + substr);
	        	if (substr.indexOf("(") == -1) 
	        		done = true;	        		
	        }
	        
	        substr = substr.trim();	        
	        if (substr.split(" ").length <= 2) {
	        	st = st.substring(4, st.length()-1);
	        }
        }        
        //LOGGER.finest("st: " + st); 
        return st; 
    	
    }

    /** ***************************************************************
     * Return an ArrayList of ProofSteps. It expects that the member variable
     * xml will contain a set of <answer> tags.
     */
    public ArrayList getProofSteps(int answerNum) {
        
    	//LOGGER.finest("--------- getProofSteps ---------");
        BasicXMLelement proof;
        /** An ArrayList of BasicXMLelements */
        ArrayList queryResponseElements = ((BasicXMLelement) xml.get(0)).subelements;

         /** An ArrayList of ProofSteps */
        ArrayList proofSteps = new ArrayList();
        BasicXMLelement answer = (BasicXMLelement) queryResponseElements.get(answerNum);

        //System.out.println("INFO in ProofProcessor.getProofSteps(): answer: " + answer.tagname);
        if (!((String) answer.attributes.get("result")).equalsIgnoreCase("no")) {
            BasicXMLelement bindingOrProof = (BasicXMLelement) answer.subelements.get(0);
            if (bindingOrProof.tagname.equalsIgnoreCase("proof")) 
                proof = bindingOrProof;            // No binding set if query is for a true/false answer
            else 
                proof = (BasicXMLelement) answer.subelements.get(1);
                

            //System.out.println("INFO in ProofProcessor.getProofSteps(): proof: " + proof.tagname);
            ArrayList steps = proof.subelements;

            for (int i = 0; i < steps.size(); i++) {
                BasicXMLelement step = (BasicXMLelement) steps.get(i);
                //System.out.println("INFO in ProofProcessor.getProofSteps(): step: " + step.tagname);
                BasicXMLelement premises = (BasicXMLelement) step.subelements.get(0);
                //System.out.println("INFO in ProofProcessor.getProofSteps(): premises: " + premises.tagname);
                BasicXMLelement conclusion = (BasicXMLelement) step.subelements.get(1);
                //System.out.println("INFO in ProofProcessor.getProofSteps(): conclusion: " + conclusion.tagname);
                BasicXMLelement conclusionFormula = (BasicXMLelement) conclusion.subelements.get(0);
                //System.out.println("INFO in ProofProcessor.getProofSteps(): conclusionFormula: " + conclusionFormula.tagname);
                ProofStep processedStep = new ProofStep();
                processedStep.formulaType = ((BasicXMLelement) conclusion.subelements.get(0)).tagname;
                //LOGGER.finest("Original axiom: " + conclusionFormula.contents);
                processedStep.axiom = Formula.postProcess(conclusionFormula.contents);
                //LOGGER.finest("Formula.postProcess: " + processedStep.axiom);
                
                if(i == steps.size() - 1) 
                	processedStep.axiom = processedStep.axiom.replaceAll("\\$answer[\\s|\\n|\\r]+", "");                	
                else
                	processedStep.axiom = removeAnswerClause(processedStep.axiom);
                //LOGGER.finest("removeAnswerClause: " + processedStep.axiom);
                //System.out.println("INFO in ProofProcessor.getProofSteps(): processedStep.axiom: " + processedStep.axiom);
                //----If there is a conclusion role, record
                if (conclusion.subelements.size() > 1) {
                    BasicXMLelement conclusionRole = (BasicXMLelement) conclusion.subelements.get(1);
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): conclusionRole: " + conclusionRole.tagname);
                    if (conclusionRole.attributes.containsKey("type")) 
                            processedStep.formulaRole = (String) conclusionRole.attributes.get("type");                        
                }
                if (conclusionFormula.attributes.containsKey("number")) {
                    processedStep.number = new Integer(Integer.parseInt((String) conclusionFormula.attributes.get("number")));
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): step number: " + processedStep.number);
                }
                for (int j = 0; j < premises.subelements.size(); j++) {
                    BasicXMLelement premise = (BasicXMLelement) premises.subelements.get(j);
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): premise: " + premise.tagname);
                    BasicXMLelement formula = (BasicXMLelement) premise.subelements.get(0);
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): formula: " + formula.tagname);
                    Integer premiseNum = new Integer(Integer.parseInt((String) formula.attributes.get("number"),10));
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): premiseNum: " + premiseNum);
                    processedStep.premises.add(premiseNum);
                    //System.out.println("INFO in ProofProcessor.getProofSteps(): premises: " + processedStep.premises);
                }
                //LOGGER.finest("processedStep: " + processedStep);
                proofSteps.add(processedStep);
            }
        }
        //LOGGER.finest("proofSteps: " + proofSteps);
        return proofSteps;
    }

    /** ***************************************************************
     * Return the number of answers contained in this proof.
     */
    public int numAnswers() {

        if (xml == null || xml.size() == 0) 
            return 0;
        BasicXMLelement queryResponse = (BasicXMLelement) xml.get(0);
        if (queryResponse.tagname.equalsIgnoreCase("queryResponse")) {
            return queryResponse.subelements.size()-1;   
        }      // Note that there is a <summary> element under the queryResponse element that shouldn't be counted, hence the -1
        else
            System.out.println("Error in ProofProcessor.numAnswers(): Bad tag: " + queryResponse.tagname);
        return 0;
    }

    /** ***************************************************************
     * Convert XML proof to TPTP format
     */
    public static String tptpProof(ArrayList proofSteps) {

        StringBuffer result = new StringBuffer();
        try {
        	//LOGGER.finest("--------- tptpProof ---------");
            for (int j = 0; j < proofSteps.size(); j++) {
            	//LOGGER.finest("ProofStep " + j);
                ProofStep step = (ProofStep) proofSteps.get(j);
                boolean isLeaf = step.premises.isEmpty() || 
                    (step.premises.size() == 1 && ((Integer)(step.premises.get(0))).intValue() == 0);
                //DEBUG result.append(step.formulaType);
                //----All are fof because the conversion from SUO-KIF quantifies the variables
                result.append("fof(");
                result.append(step.number);
                result.append(",");
                if (isLeaf) {
                    result.append("axiom");
                } else {
                    result.append("plain");
                }
                result.append(",");
                //DEBUG System.out.println("===\n" + step.axiom);

                result.append(Formula.tptpParseSUOKIFString(step.axiom));                       
                //LOGGER.finest("    " + step.axiom);

                if (!isLeaf) {
                    result.append(",inference(rule,[],[" + step.premises.get(0));
                    //LOGGER.finest("        ,inference(rule,[],[" + step.premises.get(0));
                    for (int parent = 1; parent < step.premises.size(); parent++) {
                    	//LOGGER.finest("        ," + step.premises.get(parent));
                        result.append("," + step.premises.get(parent));
                    }
                    result.append("])");
                }
                result.append("  ).\n");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return(result.toString());
    }

    /** ***************************************************************
     *  A method used only for testing.  It should not be called
     *  during normal operation.
     */
     public static void test (String[] args) {

        try {
            FileReader r = new FileReader(args[0]);
            LineNumberReader lr = new LineNumberReader(r);
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = lr.readLine()) != null) {
                result.append(line + "\n");
                //DEBUG System.out.println(line);
            }

            BasicXMLparser res = new BasicXMLparser(result.toString());
            result = new StringBuffer();
            ProofProcessor pp = new ProofProcessor(res.elements);
            for (int i = 0; i < pp.numAnswers(); i++) {
                ArrayList proofSteps = pp.getProofSteps(i);
                proofSteps = new ArrayList(ProofStep.normalizeProofStepNumbers(proofSteps));
                if (i != 0) 
                    result.append("\n");               
                result.append("%----Answer " + (i+1) + " " + pp.returnAnswer(i,"") + "\n");
                if (!pp.returnAnswer(i).equalsIgnoreCase("no")) 
                    result.append(tptpProof(proofSteps));               
            }
            System.out.println(result.toString());
         }
         catch (IOException ioe) {
             System.out.println("Error in ProofProcessor.main(): IOException: " + ioe.getMessage());
         }     
     }   
     
     /** ***************************************************************
      *  A main method, used only for testing.  It should not be called
      *  during normal operation.
      */
      public static void main (String[] args) {

          try {
              KBmanager.getMgr().initializeOnce();
              KB kb = KBmanager.getMgr().getKB("SUMO");
              String stmt = "(subclass ?X Entity)";
              String result = kb.ask(stmt, 30, 3);
              result = HTMLformatter.formatProofResult(result,stmt,stmt,"<hr>\n","SUMO","EnglishLanguage");
              System.out.println(result);
          } catch (Exception ex) {
              System.out.println(ex.getMessage());
          }

      }
}
