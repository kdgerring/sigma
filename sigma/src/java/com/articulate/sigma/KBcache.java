/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also
http://sigmakee.sourceforge.net

Note that this class, and therefore, Sigma, depends upon several terms
being present in the ontology in order to function as intended.  They are:
  domain
  domainSubclass
  Entity
  instance
  Relation
  subclass
  subrelation
  TransitiveRelation
*/

/*************************************************************************************************/

package com.articulate.sigma;

import java.io.*;
import java.util.*;

public class KBcache {

    public KB kb = null;
    
    /** The String constant that is the suffix for files of cached assertions. */
    public static final String _cacheFileSuffix      = "_Cache.kif";
    
    // all the relations in the kb 
    public HashSet<String> relations = new HashSet<String>();
    
    // all the transitive relations in the kb
    public HashSet<String> transRels = new HashSet<String>();

    // all the transitive relations between instances in the kb
    public HashSet<String> instTransRels = new HashSet<String>();
    
    /** All the cached "parent" relations of all transitive relations
     * meaning the relations between all first arguments and the 
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the parent
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of parents. */
    public HashMap<String,HashMap<String,HashSet<String>>> parents = 
            new HashMap<String,HashMap<String,HashSet<String>>>();
    
    /** Parent relations from instances, including those that are
     * transitive through (instance,instance) relations, such as
     * subAttribute and subrelation */
    public HashMap<String,HashSet<String>> instances = 
            new HashMap<String,HashSet<String>>();
    
    // A temporary list of instances built during creation of the
    // @see children map, in order to efficiently create the
    // @see instances map.
    // TODO: make private
    public HashSet<String> insts = new HashSet<String>();
    
    /** All the cached "child" relations of all transitive relations
     * meaning the relations between all first arguments and the 
     * transitive closure of second arguments.  The external HashMap
     * pairs relation name String keys to values that are the child
     * relationships.  The interior HashMap is the set of terms and
     * their transitive closure of children. */
    public HashMap<String,HashMap<String,HashSet<String>>> children = 
            new HashMap<String,HashMap<String,HashSet<String>>>();
    
    // Relation name keys and argument types with 0th arg always "".
    // Variable arity relations may have a type for the last argument,
    // which will be the type repeated for all extended arguments.
    // Note that types can be functions, rather than just terms.
    public HashMap<String,ArrayList<String>> signatures =
            new HashMap<String,ArrayList<String>>();
    
    // The number of arguments to each relation.  Variable arity is -1
    public HashMap<String,Integer> valences = new HashMap<String,Integer>();

    /** ***************************************************************
     * Constructor
     */
    public KBcache(KB kb) {
        
        this.kb = kb;
    }
    
    /** ***************************************************************
     * An ArrayList utility method
     */
    private void arrayListReplace(ArrayList<String> al, int index, String newEl) {
        
        if (index > al.size()) {
            System.out.println("Error in KBcache.arrayListReplace(): index " + index +
                    " out of bounds.");
            return;
        }
        al.remove(index);
        al.add(index,newEl);
    }
    
    /** ***************************************************************
     * Find whether the given child has the given parent for the given
     * transitive relation.  Return false if they are equal
     */
    public boolean childOfP(String rel, String parent, String child) {
        
        if (parent.equals(child))
            return false;
        //System.out.println("INFO in KBcache.childOfP(): rel,parent,child: " + rel + " " + parent + " " + child);
        HashMap<String,HashSet<String>> childMap = children.get(rel);
        HashSet<String> childSet = childMap.get(parent);
        if (childSet == null) {
        	System.out.println("INFO in KBcache.childOfP(): null childset for rel, parent, child: "
                + rel + " " + parent + " " + child);
        	return false;
        }
        if (childSet.contains(child))
            return true;
        else
            return false;
    }

    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param i A String denoting an instance.
     * @param c A String denoting a Class.
     * @return true or false.
     */
    public boolean isInstanceOf(String i, String c) {

        if (instances.containsKey(i)) {
            HashSet<String> hashSet = instances.get(i);
            if (hashSet.contains(c))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    /** ***************************************************************
     * Find whether the given instance has the given parent class.  
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * Return false if they are equal.
     */
    public boolean transInstOf(String child, String parent) {
    
        HashSet<String> prents = instances.get(child);
        if (prents != null)
            return prents.contains(parent);
        else
            return false;
    }
    
    /** ***************************************************************
     * Find whether the given class has the given parent class.  
     */
    public boolean subclassOf(String child, String parent) {
    
    	HashMap<String,HashSet<String>> prentsForRel = parents.get("subclass");
    	if (prentsForRel != null) {
	    	HashSet<String> prents = prentsForRel.get(child);
	        if (prents != null)
	            return prents.contains(parent);
	        else
	            return false;
	    	}
    	return false;
    }
 
    /** ***************************************************************
     * Record instances and their explicitly defined parent classes
     */
    private void buildDirectInstances() {
    	
        ArrayList<Formula> forms = kb.ask("arg",0,"instance");
        for (int i = 0; i < forms.size(); i++) {
            Formula f = forms.get(i);
            String child = f.getArgument(1);
            String parent = f.getArgument(2);
            HashSet<String> iset = new HashSet<String>();
            if (instances.get(child) != null)
                iset = instances.get(child);
            iset.add(parent);
        	instances.put(child, iset);
        }
    }
    
    /** ***************************************************************
     * Cache whether a given instance has a given parent class.  
     * Include paths the have transitive relations between instances such
     * as an Attribute that is a subAttribute of another instance, which
     * in turn then is an instance of the given class.
     * TODO: make sure that direct instances are recorded too
     */
    private void buildTransInstOf() {
    
        Iterator<String> titer = insts.iterator();     // Iterate through the temporary list of instances built 
                                                       // during creation of the @see children map
        while (titer.hasNext()) {
            String child = titer.next();
            //System.out.println();
            //System.out.println("INFO in KBcache.buildTransInstOf(): child: " + child);
            ArrayList<Formula> forms = kb.ask("arg",1,child);
            for (int i = 0; i < forms.size(); i++) {
                Formula f = forms.get(i);
                String rel = f.getArgument(0);
                if (instTransRels.contains(rel) && !rel.equals("subclass")) {
                    //System.out.println("INFO in KBcache.buildTransInstOf(): considering formula: " + f);
                    HashMap<String,HashSet<String>> prentList = parents.get(rel);
                    if (prentList != null) {
                        HashSet<String> prents = prentList.get(f.getArgument(1));  // include all parents of the child 
                        if (prents != null) {
                            //System.out.println("INFO in KBcache.buildTransInstOf(): prents: " + prents);
                            Iterator<String> it = prents.iterator();
                            while (it.hasNext()) {
                                String p = it.next();
                                //System.out.println("INFO in KBcache.buildTransInstOf(): parent: " + p);
                                ArrayList<Formula> forms2 = kb.askWithRestriction(0,"instance",1,p);
                                for (int j = 0; j < forms2.size(); j++) {
                                    Formula f2 = forms2.get(j);
                                    //System.out.println("INFO in KBcache.buildTransInstOf(): formula: " + f2);
                                    String cl = f2.getArgument(2);
                                    HashMap<String,HashSet<String>> superclasses = parents.get("subclass");
                                    HashSet<String> pset = new HashSet<String>();
                                    if (instances.get(child) != null)
                                        pset = instances.get(child);
                                    pset.add(cl);
                                    pset.addAll(superclasses.get(cl));
                                    //System.out.println("INFO in KBcache.buildTransInstOf(): child,pset: " + child +
                                    //        ": " + pset);
                                    instances.put(child, pset);
                                    //System.out.println("INFO in KBcache.buildTransInstOf(): size: " + instances.keySet().size());
                                }
                            }
                        }
                    }
                }
                else if (rel.equals("instance")) {
                	if (child.equals("exhaustiveAttribute"))
                		System.out.println("INFO in KBcache.buildTransInstOf(): f: " + f);
                	String cl = f.getArgument(2);
                    HashSet<String> iset = new HashSet<String>();
                    if (instances.get(child) != null)
                        iset = instances.get(child);
                    iset.add(cl);
                	instances.put(child, iset);
                }
            }            
        }
        buildDirectInstances();
    }
    
    /** ***************************************************************    
     */
    public HashSet<String> getParentClasses(String cl) {
        
        HashMap<String,HashSet<String>> ps = parents.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }
    
    /** ***************************************************************    
     */
    public HashSet<String> getChildClasses(String cl) {
        
        HashMap<String,HashSet<String>> ps = children.get("subclass");
        if (ps != null)
            return ps.get(cl);
        else
            return null;
    }

    /** ***************************************************************    
     */
    public HashSet<String> getInstances(String cl) {
        
        HashSet<String> ps = instances.get(cl);
        if (ps != null)
            return ps;
        else
            return new HashSet<String>();
    }
    
    /** ***************************************************************
     * Get the HashSet of the given arguments from an ArrayList of Formulas.
     */
    public static HashSet<String> collectArgFromFormulas(int arg, ArrayList<Formula> forms) {
        
        HashSet<String> subs = new HashSet<String>();
        for (int i = 0; i < forms.size(); i++) {
            Formula f = forms.get(i);
            String sub = f.getArgument(arg);
            subs.add(sub);
        }
        return subs;
    }
   
    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildTransitiveRelationsSet() {
        
        HashSet<String> rels = new HashSet<String>();  
        rels.add("TransitiveRelation");
        while (!rels.isEmpty()) {
            //System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): rels: " + rels);
            //System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): transRels: " + transRels);
            HashSet<String> relSubs = new HashSet<String>();
            Iterator<String> it = rels.iterator();
            while (it.hasNext()){
                String rel = it.next();
                //System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): checking rel: " + rel);
                relSubs = new HashSet<String>();
                ArrayList<Formula> forms = kb.askWithRestriction(0,"subclass",2,rel);
                ArrayList<Formula> forms2 = kb.ask("arg",2,rel);
                //System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): formulas: " + forms2);

                if (forms != null) {
                    //System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): subclasses: " + forms);
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }
                //else
                //    System.out.println("INFO in KBcache.buildTransitiveRelationsSet(): no subclasses for : " + rels);
                forms = kb.askWithRestriction(0,"instance",2,rel);
                if (forms != null) 
                    transRels.addAll(collectArgFromFormulas(1,forms));
                forms = kb.askWithRestriction(0,"subrelation",2,rel);
                if (forms != null) 
                    transRels.addAll(collectArgFromFormulas(1,forms));
            }
            rels = new HashSet<String>();
            rels.addAll(relSubs);
        }
    }
    
    /** ***************************************************************
     * Do a proper search for relations (including Functions), utilizing
     * the formal definitions, rather than the convention of initial
     * lower case letter.  This means getting any instance of Relation
     * tracing back through subclasses as well.
     */
    public void buildRelationsSet() {
        
        HashSet<String> rels = new HashSet<String>();  
        rels.add("Relation");
        while (!rels.isEmpty()) {
            //System.out.println();
            //System.out.println("INFO in KBcache.buildRelationsSet(): rels: " + rels);
            HashSet<String> relSubs = new HashSet<String>();
            Iterator<String> it = rels.iterator();
            while (it.hasNext()) {
                String rel = it.next();
                //System.out.println("INFO in KBcache.buildRelationsSet(): rel: " + rel);
                ArrayList<Formula> forms = kb.askWithRestriction(0,"subclass",2,rel);
                if (forms != null) 
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                
                forms = kb.askWithRestriction(0,"instance",2,rel);
                if (forms != null) {
                    relations.addAll(collectArgFromFormulas(1,forms));
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }    
                forms = kb.askWithRestriction(0,"subrelation",2,rel);
                if (forms != null) { 
                    relations.addAll(collectArgFromFormulas(1,forms));
                    relSubs.addAll(collectArgFromFormulas(1,forms));
                }    
                //System.out.println("INFO in KBcache.buildRelations(): subs: " + relSubs);
            }
            rels = new HashSet<String>();
            rels.addAll(relSubs);
        }
    }

    /** ***************************************************************
     * Find the parent "roots" of any transitive relation - terms that
     * appear only as argument 2
     */
    private HashSet<String> findRoots(String rel) {
        
        HashSet<String> result = new HashSet<String>();
        ArrayList<Formula> forms = kb.ask("arg",0,rel);
        HashSet<String> arg1s = collectArgFromFormulas(1,forms);
        HashSet<String> arg2s = collectArgFromFormulas(2,forms);
        arg2s.removeAll(arg1s);
        result.addAll(arg2s);
        return result;
    }
    
    /** ***************************************************************
     * Find the child "roots" of any transitive relation - terms that
     * appear only as argument 1
     */
    private HashSet<String> findLeaves(String rel) {
        
        HashSet<String> result = new HashSet<String>();
        ArrayList<Formula> forms = kb.ask("arg",0,rel);
        HashSet<String> arg1s = collectArgFromFormulas(1,forms);
        HashSet<String> arg2s = collectArgFromFormulas(2,forms);
        arg1s.removeAll(arg2s);
        result.addAll(arg1s);
        //System.out.println("INFO in KBcache.findRoots(): " + result);
        return result;
    }
    
    /** ***************************************************************
     */
    private void breadthFirstBuildParents(String root, String rel) {
        
        HashMap<String,HashSet<String>> relParents = parents.get(rel);
        if (relParents == null) {
            System.out.println("Error in KBcache.breadthFirstBuildParents(): no relation " + rel);
            return;
        }
        //else
        //    System.out.println("INFO in KBcache.breadthFirst(): trying relation " + rel);
        ArrayDeque<String> Q = new ArrayDeque<String>();
        HashSet<String> V = new HashSet<String>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            //System.out.println("visiting " + t);
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(1,forms);
                //System.out.println("visiting subs of t: " + relSubs);
                Iterator<String> it = relSubs.iterator();
                while (it.hasNext()) {
                    String newTerm = it.next();
                    HashSet<String> newParents = new HashSet<String>();
                    HashSet<String> oldParents = relParents.get(t);
                    if (oldParents == null) {
                        oldParents = new HashSet<String>();
                        relParents.put(t, oldParents);        
                    }
                    newParents.addAll(oldParents);
                    newParents.add(t);
                    HashSet<String> newTermParents = relParents.get(newTerm);
                    if (newTermParents != null)
                        newParents.addAll(newTermParents);
                    relParents.put(newTerm, newParents);
                    //System.out.println(newTerm + ": " + newParents);
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }
    
    /** ***************************************************************
     */
    private void breadthFirstBuildChildren(String root, String rel) {
        
        HashMap<String,HashSet<String>> relChildren = children.get(rel);
        if (relChildren == null) {
            System.out.println("Error in KBcache.breadthFirstBuildChildren(): no relation " + rel);
            return;
        }
        //else
        //    System.out.println("INFO in KBcache.breadthFirst(): trying relation " + rel);
        ArrayDeque<String> Q = new ArrayDeque<String>();
        HashSet<String> V = new HashSet<String>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            //System.out.println("visiting " + t);
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,1,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(2,forms);
                //System.out.println("visiting subs of t: " + relSubs);
                Iterator<String> it = relSubs.iterator();
                while (it.hasNext()) {
                    String newTerm = it.next();
                    HashSet<String> newChildren = new HashSet<String>();
                    HashSet<String> oldChildren = relChildren.get(t);
                    if (oldChildren == null) {
                        oldChildren = new HashSet<String>();
                        relChildren.put(t, oldChildren);        
                    }
                    newChildren.addAll(oldChildren);
                    newChildren.add(t);
                    HashSet<String> newTermChildren = relChildren.get(newTerm);
                    if (newTermChildren != null)
                        newChildren.addAll(newTermChildren);
                    relChildren.put(newTerm, newChildren);
                    //System.out.println(newTerm + ": " + newParents);
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
        insts.addAll(relChildren.keySet());
    }
    
    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}.
     *     public HashMap<String,HashMap<String,ArrayList<String>>> parents = 
            new HashMap<String,HashMap<String,ArrayList<String>>>();
     */
    public void buildParents() {
    
        Iterator<String> it = transRels.iterator();
        while (it.hasNext()) {
            String rel = it.next();
            HashMap<String,HashSet<String>> value = new HashMap<String,HashSet<String>>();
            HashSet<String> roots = findRoots(rel);
            parents.put(rel, value);
            Iterator<String> it1 = roots.iterator();
            while (it1.hasNext()) {
                String root = it1.next();
                breadthFirstBuildParents(root,rel);
            }
        }
    }

    /** ***************************************************************
     * For each transitive relation, find its transitive closure.  If
     * rel is transitive, and (rel A B) and (rel B C) then the entry for
     * rel is a HashMap where the key A has value ArrayList of {B,C}.
     *     public HashMap<String,HashMap<String,ArrayList<String>>> children = 
            new HashMap<String,HashMap<String,ArrayList<String>>>();
     */
    public void buildChildren() {
    
        Iterator<String> it = transRels.iterator();
        while (it.hasNext()) {
            String rel = it.next();
            HashMap<String,HashSet<String>> value = new HashMap<String,HashSet<String>>();
            HashSet<String> leaves = findLeaves(rel);
            children.put(rel, value);
            Iterator<String> it1 = leaves.iterator();
            while (it1.hasNext()) {
                String root = it1.next();
                breadthFirstBuildChildren(root,rel);
            }
        }
    }
    
    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the 1st argument and
     * ignoring the 0th argument.
     */
    private static void fillArray(String st, String[] ar, int start, int end) {
    
        for (int i = start; i < end; i++) 
            if (StringUtil.emptyString(ar[i]))
                ar[i] = st;        
    }
    
    /** ***************************************************************
     * Fill an array of String with the specified String up to but
     * not including the index, starting from the end of the array
     */
    private static void fillArrayList(String st, ArrayList<String> ar, int start, int end) {
    
        for (int i = start; i < end; i++) 
            if (i > ar.size()-1 || StringUtil.emptyString(ar.get(i)))
                ar.add(st);        
    }
    
    /** ***************************************************************
     * Build the argument type list for every relation. If the argument
     * is a domain subclass, append a "+" to the argument type.  If
     * no domain is defined for the given relation and argument position,
     * inherit it from the parent.  If there is no argument type, send
     * an error to the Sigma error list.
     * Relation name keys and argument types with 0th arg always ""
     * public HashMap<String,ArrayList<String>> signatures =
     *      new HashMap<String,ArrayList<String>>();
     *      
     *      TODO: Function range
     *      TODO: Variable arity relations, which requires a new predicate
     *      in SUMO that will define the type of a row variable
     */
    public void collectDomains() {
        
        Iterator<String> it = relations.iterator();
        while (it.hasNext()) {
            String rel = it.next();
            //System.out.println("INFO in KBcache.collectDomains(): trying relation " + rel);
            String[] domainArray = new String[Formula.MAX_PREDICATE_ARITY];
            int maxIndex = 0;
            domainArray[0] = "";
            ArrayList<Formula> forms = kb.askWithRestriction(0,"domain",1,rel);
            if (forms != null) {
                for (int i = 0; i < forms.size(); i++) {
                    Formula form = forms.get(i);
                    //System.out.println("INFO in KBcache.collectDomains(): form " + form);
                    int arg = Integer.valueOf(form.getArgument(2));
                    String type = form.getArgument(3); 
                    domainArray[arg] = type; 
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }
            forms = kb.askWithRestriction(0,"domainSubclass",1,rel);
            if (forms != null) {
                for (int i = 0; i < forms.size(); i++) {
                    Formula form = forms.get(i);
                    //System.out.println("INFO in KBcache.collectDomains(): form " + form);
                    int arg = Integer.valueOf(form.getArgument(2));
                    String type = form.getArgument(3);                
                    domainArray[arg] = type + "+";
                    if (arg > maxIndex)
                        maxIndex = arg;
                }
            }
            //System.out.println("INFO in KBcache.collectDomains(): domains " + domains);
            fillArray("Entity",domainArray,1,maxIndex);                    
            ArrayList<String> domains = new ArrayList<String>();
            for (int i = 0; i <= maxIndex; i++)
                domains.add(domainArray[i]);
            signatures.put(rel,domains);
            valences.put(rel, new Integer(maxIndex));
        }
        inheritDomains();
    }
    
    /** ***************************************************************
     * Note that this routine forces child relations to have arguments
     * that are the same or more specific than their parent relations.
     */
    private void breadthFirstInheritDomains(String root) {
        
        //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): trying relation " + root);
        String rel = "subrelation";
        HashMap<String,HashSet<String>> relParents = parents.get("subrelation");
        if (relParents == null) {
            System.out.println("Error in KBcache.breadthFirst(): no relation subrelation");
            return;
        }
        //else
            //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): trying relation " + rel);
        ArrayDeque<String> Q = new ArrayDeque<String>();
        HashSet<String> V = new HashSet<String>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            String t = Q.remove();
            ArrayList<String> tdomains = signatures.get(t);
            //System.out.println("visiting " + t);
            ArrayList<Formula> forms = kb.askWithRestriction(0,rel,2,t);
            if (forms != null) {
                HashSet<String> relSubs = collectArgFromFormulas(1,forms);
                //System.out.println("visiting subs of t: " + relSubs);
                Iterator<String> it = relSubs.iterator();
                while (it.hasNext()) {
                    String newTerm = it.next();                    
                    ArrayList<String> newDomains = signatures.get(newTerm);
                    //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): valence for new term " + 
                    //    newTerm + ":" + valences.get(newTerm));
                    //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): valence for " + 
                    //        t + ":" + valences.get(t));
                    //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): " + newTerm + " : " + newDomains);
                    //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): " + t + ": " + tdomains);
                    if (valences.get(newTerm) == null || valences.get(newTerm) < valences.get(t)) {
                        //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): " + newDomains);
                        fillArrayList("Entity",newDomains,valences.get(newTerm)+1,valences.get(t)+1);
                        //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): " + newDomains);
                        valences.put(newTerm, valences.get(t));
                    }
                    for (int i = 1; i < valences.get(t); i++) {
                        String childArgType = newDomains.get(i);
                        String parentArgType = tdomains.get(i);
                        //System.out.println("INFO in KBcache.breadthFirstInheritDomains(): comparing child to parent: " + childArgType + " " + parentArgType);
                        // If child-relation does not have definition of argument-type, we use parent-relation's argument-type
                        // TODO: if parent-relation does not have definition of argument-type, we continue to find its parent until we find the definition of argument-type
                        if (kb.askWithTwoRestrictions(0, "domain", 1, newTerm, 3, childArgType).isEmpty()) {
                            arrayListReplace(newDomains,i,parentArgType);
                        }
                    }
                    if (!V.contains(newTerm)) {
                        V.add(newTerm);
                        Q.addFirst(newTerm);
                    }
                }
            }
        }
    }
    
    /** *************************************************************
     * Delete and writes the cache .kif file then call addConstituent() so
     * that the file can be processed and loaded by the inference engine.
     */
    public void writeCacheFile() {
                           
        FileWriter fw = null;
        try {
            File dir = new File(KBmanager.getMgr().getPref("kbDir"));
            File f = new File(dir, (kb.name + _cacheFileSuffix));
            if (f.exists()) 
                f.delete();                                           
            String filename = f.getCanonicalPath();
            fw = new FileWriter(f, true);
            Iterator<String> it = parents.keySet().iterator();
            while (it.hasNext()) {
                String rel = it.next();
                HashMap<String,HashSet<String>> valSet = parents.get(rel);
                Iterator<String> it2 = valSet.keySet().iterator();
                while (it2.hasNext()) {
                    String child = it2.next();
                    HashSet<String> prents = valSet.get(child);
                    Iterator<String> it3 = prents.iterator();
                    while (it3.hasNext()) {
                        String parent = it3.next();
                        String tuple = "(" + rel + " " + child + " " + parent + ")";
                        if (!kb.formulaMap.containsKey(tuple)) {
                            fw.write(tuple);
                            fw.write(System.getProperty("line.separator"));
                        }
                    }
                }                
            }
            if (fw != null) {
                fw.close();
                fw = null;
            }
            kb.constituents.remove(filename);
            kb.addConstituent(filename);
            //kb.addConstituent(filename, false, false, false);
            //KBmanager.getMgr().writeConfiguration();
        }                   
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (fw != null) 
                    fw.close();                
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /** ***************************************************************
     * Find domain and domainSubclass definitions that impact a child
     * relation.  If the type of an argument is less specific than
     * the same type of a parent's argument, use that of the parent.
     */
    public void inheritDomains() {
        
        HashSet<String> roots = findRoots("subrelation");
        Iterator<String> it = roots.iterator();
        while (it.hasNext()) {
            String root = it.next();
            breadthFirstInheritDomains(root);
        }
    }

    /** ***************************************************************
     * Compile the set of transitive relations that are between instances  
     */
    public void buildInstTransRels() {
        
        Iterator<String> it = transRels.iterator();
        while (it.hasNext()) {
            String rel = it.next();
            boolean instrel = true;
            ArrayList<String> sig = signatures.get(rel);
            if (sig == null) {
                System.out.println("Error in KBcache.buildInstTransRels(): Error " + rel + " not found.");
            }
            else {
                for (int i = 0; i < sig.size(); i++) {
                    if (sig.get(i).endsWith("+")) {
                        instrel = false;
                        break;
                    }
                }
                if (instrel)
                    instTransRels.add(rel);
            }
        }        
    }
    
    /** ***************************************************************
     * Main entry point for the class.  
     */
    public void buildCaches() {
        
        buildRelationsSet();
        buildTransitiveRelationsSet();
        buildParents();
        buildChildren(); // note that buildTransInstOf() depends on this
        collectDomains();  // note that buildInstTransRels() depends on this
        buildInstTransRels();
        buildTransInstOf();
        System.out.println("INFO in KBcache.buildCaches(): size: " + instances.keySet().size());
    }
    
    /** *************************************************************
     */
    public static void main(String[] args) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB("SUMO");
        System.out.println("**** Finished loading KB ***");
        //KBcache nkbc = new KBcache(kb);
        KBcache nkbc = kb.kbCache;
        //nkbc.buildCaches();
        //nkbc.buildRelationsSet();
        System.out.println("-------------- relations ----------------");
        Iterator<String> it = nkbc.relations.iterator();
        while (it.hasNext()) 
            System.out.print(it.next() + " ");
        System.out.println();
        //nkbc.buildTransitiveRelationsSet();
        System.out.println("-------------- transitives ----------------");
        it = nkbc.transRels.iterator();
        while (it.hasNext()) 
            System.out.print(it.next() + " ");
        System.out.println();
        System.out.println("-------------- parents ----------------");
        //nkbc.buildParents();
        it = nkbc.parents.keySet().iterator();
        while (it.hasNext()) {
            String rel = it.next();
            System.out.println("Relation: " + rel);
            HashMap<String,HashSet<String>> relmap = nkbc.parents.get(rel);
            Iterator<String> it2 = relmap.keySet().iterator();
            while (it2.hasNext()) {
                String term = it2.next();
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- children ----------------");
        //nkbc.buildChildren();
        it = nkbc.children.keySet().iterator();
        while (it.hasNext()) {
            String rel = it.next();
            System.out.println("Relation: " + rel);
            HashMap<String,HashSet<String>> relmap = nkbc.children.get(rel);
            Iterator<String> it2 = relmap.keySet().iterator();
            while (it2.hasNext()) {
                String term = it2.next();
                System.out.println(term + ": " + relmap.get(term));
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("-------------- domains ----------------");
        //nkbc.collectDomains();
        Iterator<String> it3 = nkbc.relations.iterator();
        while (it3.hasNext()) {
            String rel = it3.next();
            ArrayList<String> domains = nkbc.signatures.get(rel);
            System.out.println(rel + ": " + domains);
        }
        System.out.println();
        System.out.println("-------------- valences ----------------");
        Iterator<String> it4 = nkbc.valences.keySet().iterator();
        while (it4.hasNext()) {
            String rel = it4.next();
            Integer arity = nkbc.valences.get(rel);
            System.out.println(rel + ": " + arity);
        }
        System.out.println();
        System.out.println("-------------- insts ----------------");
        Iterator<String> it5 = nkbc.insts.iterator();
        while (it5.hasNext()) {
            String inst = it5.next();
            System.out.print(inst + ", ");
        }
        System.out.println();
        System.out.println();
        System.out.println("-------------- instances ----------------");
        Iterator<String> it6 = nkbc.instances.keySet().iterator();
        while (it6.hasNext()) {
            String inst = it6.next();
            System.out.println(inst + ": " + nkbc.instances.get(inst));
        }
        
    }
}
