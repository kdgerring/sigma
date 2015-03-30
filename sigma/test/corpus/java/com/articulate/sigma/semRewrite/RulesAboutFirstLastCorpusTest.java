/*
Copyright 2014-2015 IPsoft

Author: Sofia Athenikos sofia.athenikos@ipsoft.com

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
package com.articulate.sigma.semRewrite;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KBmanager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RulesAboutFirstLastCorpusTest {

    public static Interpreter interpreter;

    @BeforeClass
    public static void initInterpreter() {
        interpreter = new Interpreter();
        KBmanager.getMgr().initializeOnce();
        interpreter.loadRules();
    }

    @Test
    public void testSheWasTheFirstWomanToFly() {
        String input = "She was the first woman to fly.";

        String expectedKifString = "(exists (?she-1 ?fly-7)\n" +
                "  (and\n" +
                "  (instance ?she-1 Woman)\n" +
                "  (instance ?fly-7 Flying)\n" +
                "  (agent ?fly-7 ?she-1)\n" +
                "  (not\n" +
                "    (exists (?W2 ?V2)\n" +
                "      (and\n" +
                "        (instance ?W2 Woman)\n" +
                "        (instance ?V2 Flying)\n" +
                "        (agent ?V2 ?W2)\n" +
                "        (earlier\n" +
                "          (WhenFn ?V2)\n" +
                "          (WhenFn ?fly-7)))))))";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSheWasTheFirstWomanToFlyAPlaneByHerself() {
        String input = "She was the first woman to fly a plane by herself.";

        String expectedKifString = "(exists (?plane-9 ?she-1 ?fly-7)\n" +
                "  (and\n" +
                "  (instance ?she-1 Woman)\n" +
                "  (instance ?fly-7 Flying)\n" +
                "  (instance ?plane-9 Airplane)\n" +
                "  (agent ?fly-7 ?she-1)\n" +
                "  (patient ?fly-7 ?plane-9)\n" +
                "  (not\n" +
                "    (exists (?H)\n" +
                "      (and\n" +
                "        (instance ?H Human)\n" +
                "        (not\n" +
                "          (equal ?H ?she-1))\n" +
                "        (agent ?fly-7 ?H))))\n" +
                "  (not\n" +
                "    (exists (?W2 ?V2 ?O2)\n" +
                "      (and\n" +
                "        (instance ?W2 Woman)\n" +
                "        (instance ?V2 Flying)\n" +
                "        (instance ?O2 Airplane)\n" +
                "        (agent ?V2 ?W2)\n" +
                "        (patient ?V2 ?O2)\n" +
                "        (not\n" +
                "          (exists (?H2)\n" +
                "            (and\n" +
                "              (instance ?H2 Human)\n" +
                "              (not\n" +
                "                (equal ?H2 ?W2))\n" +
                "              (agent ?V2 ?H2))))\n" +
                "        (earlier\n" +
                "          (WhenFn ?V2)\n" +
                "          (WhenFn ?fly-7)))))))";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

    @Test
    public void testSheWasTheFirstWomanToFlyAPlaneByHerselfAcrossTheAtlanticOcean() {
        String input = "She was the first woman to fly a plane by herself across the Atlantic Ocean.";

        String expectedKifString = "(exists (?plane-9 ?she-1 ?AtlanticOcean-14 ?fly-7)\n" +
                "  (and\n" +
                "  (instance ?she-1 Woman)\n" +
                "  (instance ?fly-7 Flying)\n" +
                "  (instance ?plane-9 Airplane)\n" +
                "  (agent ?fly-7 ?she-1)\n" +
                "  (patient ?fly-7 ?plane-9)\n" +
                "  (traverses ?fly-7 ?AtlanticOcean-14)\n" +
                "  (not\n" +
                "    (exists (?H)\n" +
                "      (and\n" +
                "        (instance ?H Human)\n" +
                "        (not\n" +
                "          (equal ?H ?she-1))\n" +
                "        (agent ?fly-7 ?H))))\n" +
                "  (not\n" +
                "    (exists (?W2 ?V2 ?O2)\n" +
                "      (and\n" +
                "        (instance ?W2 Woman)\n" +
                "        (instance ?V2 Flying)\n" +
                "        (instance ?O2 Airplane)\n" +
                "        (agent ?V2 ?W2)\n" +
                "        (patient ?V2 ?O2)\n" +
                "        (traverses ?fly-7 ?AtlanticOcean-14)\n" +
                "        (not\n" +
                "          (exists (?H2)\n" +
                "            (and\n" +
                "              (instance ?H2 Human)\n" +
                "              (not\n" +
                "                (equal ?H2 ?W2))\n" +
                "              (agent ?V2 ?H2))))\n" +
                "        (earlier\n" +
                "          (WhenFn ?V2)\n" +
                "          (WhenFn ?fly-7)))))))";

        String actualKifString = interpreter.interpretSingle(input);

        Formula expectedKifFormula = new Formula(expectedKifString);
        Formula actualKifFormula = new Formula(actualKifString);

        assertEquals(expectedKifFormula.toString(), actualKifFormula.toString());
    }

}
