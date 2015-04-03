/*
 * Copyright 2014-2015 IPsoft
 *
 * Author: Andrei Holub andrei.holub@ipsoft.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA  02111-1307 USA
 */

package com.articulate.sigma.semRewrite.substitutor;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubstitutionUtil {

    // predicate(word-1, word-2)
    public static final Pattern CLAUSE_SPLITTER = Pattern.compile("([^\\(]+)\\((.+-\\d+),\\s*(.+-\\d+)\\)");
    // predicate(word, word-1)
    // predicate(word, word)
    // predicate(word-1, word)
    public static final Pattern GENERIC_CLAUSE_SPLITTER = Pattern.compile("([^\\(]+)\\((.+),\\s*(.+)\\)");
    public static final Pattern CLAUSE_PARAM = Pattern.compile("(.+)-(\\d+)");

    /** **************************************************************
     */
    public static void groupClauses(ClauseSubstitutor substitutor, List<String> clauses) {

        Iterator<String> clauseIterator = clauses.iterator();
        List<String> modifiedClauses = Lists.newArrayList();
        while (clauseIterator.hasNext()) {
            String clause = clauseIterator.next();
            Matcher m = CLAUSE_SPLITTER.matcher(clause);
            if (m.matches()) {
                String attr1 = m.group(2);
                String attr2 = m.group(3);
                if (substitutor.containsGroup(attr1) || substitutor.containsGroup(attr2)) {
                    String attr1Grouped = substitutor.getGrouped(attr1);
                    String attr2Grouped = substitutor.getGrouped(attr2);
                    clauseIterator.remove();
                    if (!attr1Grouped.equals(attr2Grouped)) {
                        String label = m.group(1);
                        modifiedClauses.add(label + "(" + attr1Grouped + "," + attr2Grouped + ")");
                    }
                }
            }
        }

        clauses.addAll(modifiedClauses);
    }

    /** *************************************************************
     */
    public static NounSubstitutor groupNouns(List<String> clauses) {

        NounSubstitutor cg = new NounSubstitutor(clauses);
        groupClauses(cg, clauses);

        return cg;
    }
}
