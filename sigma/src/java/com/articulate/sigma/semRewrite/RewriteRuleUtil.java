package com.articulate.sigma.semRewrite;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.StringUtil;
import com.google.common.base.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static com.articulate.sigma.semRewrite.Interpreter.canon;

/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

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

public final class RewriteRuleUtil extends RuleSet {

    static String fileLocation = "/Users/peigenyou/workspace/sigma/KBs/WordNetMappings/SemRewrite.txt";

    private RewriteRuleUtil() {

    }

    /*************************************************************
     * update a Ruleset's indexed rule's CNF
     */
    public static void updateCNF(int index, CNF f, RuleSet rs) {

        Rule r = rs.rules.get(index);
        r.cnf = f;
        System.out.println(r);
        Rule newr = Rule.parse(new Lexer(r.toString()));
        System.out.println(newr);
        rs.rules.set(index, newr);
    }

    //TODO
    public void updateLHS(int index, LHS lhs) {

    }


    /*************************************************************
     * //TODO
     */
    public static CNF applyBindings(CNF cnf, HashMap<String, String> bindings) {

        CNF cnfnew = new CNF();
        for (int i = 0; i < cnf.clauses.size(); i++) {
            Clause d = cnf.clauses.get(i);
            Clause dnew = new Clause();
            for (int j = 0; j < d.disjuncts.size(); j++) {
                Literal c = d.disjuncts.get(j);
                Literal c2 = c.applyBindings(bindings);
                dnew.disjuncts.add(c2);
                //System.out.println("INFO in CNF.applyBindings(): 2 " + dnew);
            }
            //System.out.println("INFO in CNF.applyBindings(): 3 " + dnew);
            cnfnew.clauses.add(dnew);
        }
        return cnfnew;
    }


    /*************************************************************
     * //TODO
     */
    public static Literal applyBindings(Literal origin, HashMap<String, String> bindings) {

        //System.out.println("INFO in Clause.applyBindings(): this: " + this);
        //System.out.println("INFO in Clause.applyBindings(): bindings: " + bindings);
        Literal c = new Literal();
        c.pred = origin.pred;
        c.negated = origin.negated;
        c.preserve = origin.preserve;
        if (StringUtil.emptyString(origin.arg1) || StringUtil.emptyString(origin.arg2)) {
            System.out.println("Error in Clause.applyBindings(): Empty argument(s): " + origin);
            c.arg1 = origin.arg1;
            c.arg2 = origin.arg2;
            return c;
        }
        if (origin.arg1.startsWith("?")) {
            if (bindings.containsKey(origin.arg1))
                c.arg1 = bindings.get(origin.arg1);
            else
                c.arg1 = origin.arg1;
        }
        else
            c.arg1 = origin.arg1;
        if (origin.arg2.startsWith("?")) {
            if (bindings.containsKey(origin.arg2))
                c.arg2 = bindings.get(origin.arg2);
            else
                c.arg2 = origin.arg2;
        }
        else
            c.arg2 = origin.arg2;
        //System.out.println("INFO in Clause.applyBindings(): returning this: " + c);
        return c;
    }



    /*************************************************************
     */
    public static void updateCNFTest(RuleSet rs) {

        rs = new RuleSet().parse(new Lexer("prep_about(?X,?Y) ==> {refers(?X,?Y)}.\n" + "prep_about(?H,?Y) ==> {refers(?H,?Y)}.\n"));
        rs = Clausifier.clausify(rs);
        System.out.println(rs);
        for (int i = 0; i < rs.rules.size(); ++i) {
            Rule r = rs.rules.get(i);
            CNF unifier = r.cnf;
            for (int j = i + 1; j < rs.rules.size(); ++j) {

                CNF unified = rs.rules.get(j).cnf;

                System.out.println("unified = " + unified + "   " + unifier);
                HashMap<String, String> map = unifier.unify(unified);
                if (map == null || map.size() < 1) {
                    continue;
                }
                System.out.printf("Unification found between index %d and %d map = %s \n", i, j, map);
                System.out.println("Before unification---------" + unified + "   " + unifier + "\n------------");
                unified = unified.applyBindings(map);
                System.out.println("After unification---------" + unified + "   " + unifier + "\n-----------");
                boolean m = SemRewriteRuleCheck.isCNFSubsumed(unified, unifier);
                System.out.println("After update---------" + m + "\n-----------");

            }
        }

    }


    /*************************************************************
     * load RuleSet from "SemRewrite.txt"
     */
    private static RuleSet loadRuleSet() {

        KBmanager kb = KBmanager.getMgr();
        kb.initializeOnce();

        RuleSet rs;
        String f = KBmanager.getMgr().getPref("kbDir") + File.separator + "WordNetMappings" + File.separator + "SemRewrite.txt";
        String pref = KBmanager.getMgr().getPref("SemRewrite");
        if (!Strings.isNullOrEmpty(pref))
            f = pref;
        if (f.indexOf(File.separator.toString(), 2) < 0)
            f = "/home/apease/SourceForge/KBs/WordNetMappings" + f;
        try {
            RuleSet rsin = RuleSet.readFile(f);
            rs = canon(rsin);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
        rs = Clausifier.clausify(rs);
        return rs;
    }


    /*************************************************************
     */
    public static void main(String[] args) {

        ArrayList<Integer> getSubsumed = new ArrayList<Integer>();
        ArrayList<Integer> subsumer = new ArrayList<Integer>();
        RuleSet rs = loadRuleSet();
        String input = "";
        System.out.println("SemRewrite.txt loaded. There are " + rs.rules.size() + " rules.");
        SemRewriteRuleCheck.checkRuleSet(rs);
        System.out.println("Will check rules entered. Please enter rule.\n");
        Scanner scanner = new Scanner(System.in);
        do {
            try {
                System.out.print("\nEnter rule: ");
                input = scanner.nextLine().trim();
                if (!Strings.isNullOrEmpty(input) && !input.equals("exit") && !input.equals("quit")) {
                    Rule r = Rule.parseString(input);
                    System.out.println("The rule entered is :: " + r + "\n");
                    SemRewriteRuleCheck.isRuleSubsumedByRuleSet(r, rs, getSubsumed, subsumer);

                    System.out.println("Following " + getSubsumed.size() + " rules would subsume the rule entered: \n");
                    for (int k : getSubsumed) {
                        System.out.println("Line Number:" + rs.rules.get(k).startLine + " : " + rs.rules.get(k));
                    }
                    System.out.println("---------------------------------------------------------------");
                    System.out.println("Following " + subsumer.size() + " rules would be subsumed by the rule entered: \n");
                    for (int k : subsumer) {
                        System.out.println("Line Number:" + rs.rules.get(k).startLine + " : " + rs.rules.get(k));
                    }
                    System.out.println("\n");
                }
            }
            catch (Throwable e) {
                continue;
            }

        } while (!input.equals("exit") && !input.equals("quit"));
    }
}
