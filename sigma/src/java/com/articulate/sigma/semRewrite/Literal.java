package com.articulate.sigma.semRewrite;

/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
*/

import java.text.ParseException;
import java.util.*;

import com.articulate.sigma.*;

/** *************************************************************
 * pred(arg1,arg2).  
 * arg1 and/or arg2 can be variables which are denoted by a 
 * leading '?'
 * The literal can be negated.  It also has flags for whether it
 * needs to be preserved, rather than consumed, during unification,
 * and whether it has already been successfully bound to another
 * literal during unification.
 */
public class Literal {

    public boolean negated = false;
    public boolean preserve = false;
    public boolean bound = false; // bound clauses in a delete rule get deleted
    public String pred;
    public String arg1;
    public String arg2;
    
    /** ***************************************************************
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        if (bound)
            sb.append("X");
        if (negated)
            sb.append("~");
        if (preserve)
            sb.append("+");
        sb.append(pred + "(" + arg1 + "," + arg2 + ")");
        return sb.toString();
    }
    
    /** ***************************************************************
     */
    public Literal deepCopy() {
        
        Literal newc = new Literal();
        newc.negated = negated;
        newc.preserve = preserve;
        newc.bound = bound;
        newc.pred = pred;
        newc.arg1 = arg1;
        newc.arg2 = arg2;
        return newc;
    }
    
    /** ***************************************************************
     */
    @Override
    public boolean equals(Object o) {
    
        if (!(o instanceof Literal))
            return false;
        Literal c = (Literal) o;
        if (negated != c.negated)
            return false;
        if (!pred.equals(c.pred))
            return false;
        if (!arg1.equals(c.arg1))
            return false;
        if (!arg2.equals(c.arg2))
            return false;
        return true;
    }
    
    /** ***************************************************************
     * @return true if the clause does not contain any variables
     */
    public boolean isGround() {
        
        if (!arg1.startsWith("?") && !arg2.startsWith("?"))
            return true;
        else
            return false;
    }
    
    /** *************************************************************
     * If the tokens in the literal are derived from words parsed
     * from the Stanford dependency parser, and therefore in the form
     * word-xx, where xx are digits, prepend a '?' to signify that
     * it's a variable.
     */
    public void preProcessQuestionWords(List<String> qwords) {
        
        for (String s: qwords) {
            System.out.println("INFO in Clause.preProcessQuestionWords(): " + s + " " + arg1 + " " + arg2);
            if (arg1.toLowerCase().matches(s.toLowerCase() + "-\\d+"))
                arg1 = "?" + arg1;
            if (arg2.toLowerCase().matches(s.toLowerCase() + "-\\d+"))
                arg2 = "?" + arg2;
        }
    }
    
    /** ***************************************************************
     * Apply variable substitutions to a literal  
     */
    public void applyBindingSelf(HashMap<String,String> bindings) {
        
        if (arg1.startsWith("?")) {
            if (bindings.containsKey(arg1))
                arg1 = bindings.get(arg1);
        }
        if (arg2.startsWith("?")) {
            if (bindings.containsKey(arg2))
                arg2 = bindings.get(arg2);
        }
    }
    
    /** ***************************************************************
     * @return a literal after applying variable substitutions to a literal  
     */
    public Literal applyBindings(HashMap<String,String> bindings) {
        
        //System.out.println("INFO in Clause.applyBindings(): this: " + this);
        //System.out.println("INFO in Clause.applyBindings(): bindings: " + bindings);
        Literal c = new Literal();
        c.pred = pred;
        c.negated = negated;
        c.preserve = preserve;
        if (StringUtil.emptyString(arg1) || StringUtil.emptyString(arg2)) {
            System.out.println("Error in Clause.applyBindings(): Empty argument(s): " + this);
            c.arg1 = arg1;
            c.arg2 = arg2;
            return c;
        }
        if (arg1.startsWith("?")) {
            if (bindings.containsKey(arg1))
                c.arg1 = bindings.get(arg1);
            else
                c.arg1 = arg1;
        }
        else
            c.arg1 = arg1;
        if (arg2.startsWith("?")) {
            if (bindings.containsKey(arg2))
                c.arg2 = bindings.get(arg2);
            else
                c.arg2 = arg2;
        }
        else
            c.arg2 = arg2;
        //System.out.println("INFO in Clause.applyBindings(): returning this: " + c);
        return c;
    }
    
    /** ***************************************************************
     * @return a boolean indicating whether a variable occurs in a literal.
     * This is a degenerate case of general case of occurs check during
     * unification, since we have no functions and argument lists are 
     * always of length 2.
     */
    private static boolean occursCheck(String t, Literal c) {
        
        if (t.equals(c.arg1) || t.equals(c.arg2))
            return true;
        else
            return false;
    }
    
    /** ***************************************************************
     * @return false if there are wildcards and they don't match (or 
     * there's an error) and true if there are no wildcards.  Match
     * is case-insensitive.  Wildcards only allow for ignoring the
     * word-number suffix as in wildcard-5 would match wildcard*.
     */
    private static boolean wildcardMatch(String t1, String t2) {
        
        //System.out.println("INFO in Clause.wildcardMatch(): attempting to match: " + t1 + " " + t2);
        String s1 = t1;
        String s2 = t2;
        if (!t1.contains("*") && !t2.contains("*")) // no wildcards case should fall through
            return true;
        if (t1.contains("*") && t2.contains("*")) {
            System.out.println("Error in Clause.wildcardMatch(): both arguments have wildcards: " + t1 + " " + t2);
            return false;
        }
        if (t2.contains("*")) {
            s1 = t2;
            s2 = t1;
        }
        if (s1.indexOf('*') > -1 && s2.indexOf('-') > -1) {  // when wildcard, both have to be matching variables
                                                             // except for suffix
            if (!s1.substring(0,s1.lastIndexOf('*')).equalsIgnoreCase(s2.substring(0,s2.lastIndexOf('-'))))
                return false;
        }
        return true;
    }
        
    /** ***************************************************************
     * Unify all terms in term1 with the corresponding terms in term2 with a
     * common substitution. Note that unlike general unification, we have
     * a fixed argument list of 2.   
     * @return the set of substitutions with the variable as the key and
     * the binding as the value in the HashMap.
     */
    public HashMap<String,String> mguTermList(Literal l2) {

        //System.out.println("INFO in Clause.mguTermList(): attempting to unify " + this + " and " + l2);
        HashMap<String,String> subst = new HashMap<String,String>();
        
        if (!pred.equals(l2.pred)) 
            return null;        
        for (int arg = 1; arg < 3; arg++) {           
            String t1 = arg1; // Pop the first term pair to unify off the lists            
            String t2 = l2.arg1; // (removes and returns the denoted elements).
            if (arg == 2) {
                t1 = arg2;            
                t2 = l2.arg2;
            }
            //System.out.println("INFO in Clause.mguTermList(): attempting to unify arguments " + t1 + " and " + t2); 
            if (t1.startsWith("?")) {
                //System.out.println("INFO in Clause.mguTermList(): here 1");
                if (t1.equals(t2))
                    // We could always test this upfront, but that would
                    // require an expensive check every time. 
                    // We descend recursively anyway, so we only check this on
                    // the terminal case.  
                    continue;
                if (occursCheck(t1,l2))
                    return null;
                // We now create a new substitution that binds t2 to t1, and
                // apply it to the remaining unification problem. We know
                // that every variable will only ever be bound once, because
                // we eliminate all occurrences of it in this step - remember
                // that by the failed occurs-check, t2 cannot contain t1.
                HashMap<String,String> newBinding = new HashMap<String,String>();
                if (!wildcardMatch(t1,t2)) 
                    return null;
                newBinding.put(t1,t2);                
                applyBindingSelf(newBinding);
                l2 = l2.applyBindings(newBinding);
                subst.put(t1, t2);
            }
            else if (t2.startsWith("?")) {
                //System.out.println("INFO in Clause.mguTermList(): here 2");
                // Symmetric case - We know that t1!=t2, so we can drop this check
                if (occursCheck(t2, this))
                    return null;
                HashMap<String,String> newBinding = new HashMap<String,String>();
                if (!wildcardMatch(t1,t2)) 
                    return null;
                newBinding.put(t2, t1);          
                applyBindingSelf(newBinding);
                l2 = l2.applyBindings(newBinding);
                subst.put(t2, t1);
            }
            else {
                //System.out.println("INFO in Clause.mguTermList(): t1 " + t1 + " t2 " + t2);
                if (!t1.equals(t2)) {
                    if (t1.indexOf('*') > -1 && t2.indexOf('-') > -1) {
                        if (!t1.substring(0,t1.lastIndexOf('*')).equalsIgnoreCase(t2.substring(0,t2.lastIndexOf('-'))))
                            return null;
                    }
                    else if (t2.indexOf('*') > -1 && t1.indexOf('-') > -1) {
                        if (!t2.substring(0,t2.lastIndexOf('*')).equalsIgnoreCase(t1.substring(0,t1.lastIndexOf('-'))))
                            return null;
                    }
                    else
                        return null;
                }
            }
        }
        //System.out.println("INFO in Clause.mguTermList(): subst on exit: " + subst);
        return subst;
    }
    
    /** ***************************************************************
     * @param lex is a Lexer which has been initialized with the 
     * textual version of the Literal
     * @param startLine is the line in the text file at which the
     * literal appears.  If it is in a large rule the start line
     * could be different than the actual line of text for the literal.
     * If the literal is just from a string rather than directly from
     * a text file then the startLine will be 0.
     * @return a Literal corresponding to the input text passed to the
     * Lexer.  Note that the predicate in this literal must already 
     * have been read
     */
    public static Literal parse(Lexer lex, int startLine) {

        String errStart = "Parsing error in " + RuleSet.filename;
        String errStr;
        Literal cl = new Literal();
        try {
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (lex.testTok(Lexer.Plus)) {
                cl.preserve = true;
                lex.next();
            }
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.pred = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.OpenPar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine + " on input " + lex.line);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg1 = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.Comma)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine + " on input " + lex.line);
                throw new ParseException(errStr, startLine);
            }
            lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            cl.arg2 = lex.next();
            //System.out.println("INFO in Clause.parse(): " + lex.look());
            if (!lex.testTok(Lexer.ClosePar)) {
                errStr = (errStart + ": Invalid token '" + lex.look() + "' near line " + startLine + " on input " + lex.line);
                throw new ParseException(errStr, startLine);
            } 
            lex.next();
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }    
        //System.out.println("INFO in Clause.parse(): returning " + cl);
        return cl;
    }
    
    /** *************************************************************
     * A test method for unification
     */
    public static void testUnify() {
        
        String s1 = "sumo(Human,Mary-1)";
        String s2 = "sumo(?O,Mary-1)";
        Literal c1 = null;
        Literal c2 = null;
        try {
            Lexer lex = new Lexer(s1);
            lex.look();
            c1 = Literal.parse(lex, 0);
            lex.look();
            lex = new Lexer(s2);
            c2 = Literal.parse(lex, 0);
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
        System.out.println("INFO in Clause.testUnify(): " + c1.mguTermList(c2));
        System.out.println("INFO in Clause.testUnify(): " + c2.mguTermList(c1));
    }
    
    /** *************************************************************
     * A test method for wildcard unification
     */
    public static void testRegexUnify() {
        
        String s1 = "pobj(at-1,Mary-1).";
        String s2 = "pobj(at*,?M).";
        String s3 = "pobj(boo-3,?M).";
        System.out.println("INFO in Clause.testRegexUnify(): attempting parses ");
        Literal c1 = null;
        Literal c2 = null;
        Literal c3 = null;
        try {
            Lexer lex = new Lexer(s1);
            lex.look();
            c1 = Literal.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c1);
            lex.look();
            lex = new Lexer(s2);
            c2 = Literal.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c2);
            lex = new Lexer(s3);
            c3 = Literal.parse(lex, 0);
            System.out.println("INFO in Clause.testRegexUnify(): parsed " + c3);
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
        System.out.println("INFO in Clause.testRegexUnify(): " + c1.mguTermList(c2));
        System.out.println("INFO in Clause.testRegexUnify(): " + c2.mguTermList(c1));
        System.out.println("INFO in Clause.testRegexUnify(): should fail: " + c2.mguTermList(c3));
    }
    
    /** *************************************************************
     * A test method for parsing a Literal
     */
    public static void testParse() {
        
        try {
            String input = "+det(bank-2, The-1).";
            Lexer lex = new Lexer(input);
            lex.look();
            System.out.println(Literal.parse(lex, 0));
        }
        catch (Exception ex) {
            String message = ex.getMessage();
            System.out.println("Error in Clause.parse() " + message);
            ex.printStackTrace();
        }   
    }
    
    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        
        testParse();
        //testUnify();
        //testRegexUnify();
    }
}
