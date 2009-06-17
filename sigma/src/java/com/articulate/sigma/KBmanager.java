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
import java.text.*;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager {

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether type
     * prefixes (sortals) should be added during formula
     * preprocessing.
     */    
    public static final int USE_TYPE_PREFIX  = 1;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether holds
     * prefixes should be added during formula preprocessing.
     */    
    public static final int USE_HOLDS_PREFIX = 2;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether the closure
     * of instance and subclass relastions should be "cached out" for
     * use by the inference engine.
     */    
    public static final int USE_CACHE        = 4;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether formulas
     * should be translated to TPTP format during the processing of KB
     * constituent files.
     */    
    public static final int USE_TPTP         = 8;

    private static KBmanager manager = new KBmanager();
    protected static final String CONFIG_FILE = "config.xml";

    private HashMap preferences = new HashMap();
    protected HashMap kbs = new HashMap();
    private boolean initialized = false;
    private int oldInferenceBitValue = -1;
    private String error = "";

    /** ***************************************************************
     * Set an error string for file loading.
     */
    public void setError(String er) {
        error = er;
    }

    /** ***************************************************************
     * Get the error string for file loading.
     */
    public String getError() {
        return error;
    }

    /** ***************************************************************
     * Set default attribute values if not in the configuration file.
     */
    private void setDefaultAttributes() {

        try {
            String sep = File.separator;
            String base = System.getenv("SIGMA_HOME");
            String tptpHome = System.getenv("TPTP_HOME");
            String systemsHome = System.getenv("SYSTEMS_HOME");
            if (base == null || base == "") {
                base = System.getProperty("user.dir");
            }
            if (tptpHome == null || tptpHome == "") {
                tptpHome = System.getProperty("user.dir");
            }
            if (systemsHome == null || systemsHome == "") {
                systemsHome = System.getProperty("user.dir");
            }
            System.out.println("INFO in KBmanager.setDefaultAttributes(): base == " + base);
            String tomcatRoot = System.getenv("CATALINA_HOME");
            System.out.println("INFO in KBmanager.setDefaultAttributes(): CATALINA_HOME == " + tomcatRoot);
            if ((tomcatRoot == null) || tomcatRoot.equals("")) {
                tomcatRoot = System.getProperty("user.dir");
            }
            File tomcatRootDir = new File(tomcatRoot);
            File baseDir = new File(base);
            File tptpHomeDir = new File(tptpHome);
            File systemsDir = new File(systemsHome);
            File kbDir = new File(baseDir, "KBs");
            File inferenceTestDir = new File(kbDir, "tests");
            // The links for the test results files will be broken if
            // they are not put under [Tomcat]/webapps/sigma.
            // Unfortunately, we don't know where [Tomcat] is.
            File testOutputDir = new File(tomcatRootDir,("webapps" + sep + "sigma" + sep + "tests"));
            preferences.put("baseDir",baseDir.getCanonicalPath());
            preferences.put("tptpHomeDir",tptpHomeDir.getCanonicalPath());
            preferences.put("systemsDir",systemsDir.getCanonicalPath());
            preferences.put("kbDir",kbDir.getCanonicalPath());
            preferences.put("inferenceTestDir",inferenceTestDir.getCanonicalPath());  
            preferences.put("testOutputDir",testOutputDir.getCanonicalPath());
            // No way to determine the full inferenceEngine path without
            // asking the user.
            preferences.put("inferenceEngine", "kif");
            preferences.put("loadCELT","no");  
            preferences.put("showcached","yes");  
            preferences.put("typePrefix","yes");  
            preferences.put("holdsPrefix","no");  // if no then instantiate variables in predicate position
            preferences.put("cache","no");
            preferences.put("TPTP","yes");  
            preferences.put("userBrowserLimit","25");
            preferences.put("adminBrowserLimit","200");
            preferences.put("port","8080");
            
            preferences.put("hostname","localhost");  
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     */
    private String fromXML(SimpleElement configuration) {

        StringBuffer result = new StringBuffer();
        if (!configuration.getTagName().equals("configuration")) 
            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("preference")) {
                    String name = (String) element.getAttribute("name");
                    String value = (String) element.getAttribute("value");
                    preferences.put(name,value);
                }
                else {
                    if (element.getTagName().equals("kb")) {
                        String kbName = (String) element.getAttribute("name");
                        addKB(kbName);
                        KB kb = getKB(kbName);
                        List constituentsToAdd = new ArrayList();
                        boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
                        for (int j = 0; j < element.getChildElements().size(); j++) {
                            SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                            if (!kbConst.getTagName().equals("constituent")) 
                                System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
                            String filename = (String) kbConst.getAttribute("filename");
                            if ( Formula.isNonEmptyString(filename) ) {
                                if ( filename.endsWith(KB._cacheFileSuffix) ) {
                                    if ( useCacheFile ) {
                                        constituentsToAdd.add( filename );
                                    }
                                }
                                else {
                                    constituentsToAdd.add( filename );
                                }
                            }
                        }
                        if ( !(constituentsToAdd.isEmpty()) ) {
                            Iterator it = constituentsToAdd.iterator();
                            while ( it.hasNext() ) {
                                String filename = (String) it.next();
                                try {                            
                                    result.append(kb.addConstituent(filename, false, false)); 
                                } 
                                catch (Exception e1) {
                                    System.out.println("ERROR in KBmanager.fromXML()");
                                    System.out.println("  " + e1.getMessage());
                                }
                            }
                            kb.buildRelationCaches();
                            if (useCacheFile) {
                                result.append(kb.cache());
                            }
                            kb.loadVampire();
                        }
                    }
                    else {
                        System.out.println("Error in KBmanager.fromXML(): Bad tag: " + element.getTagName());
                    }
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. The method initializeOnce()
     * sets the preferences based on the contents of the configuration file.
     * This routine has the side effect of setting the variable 
     * called "configuration".  It also creates the KBs directory and an empty
     * configuration file if none exists.
     */

    private static void copyFile(File in, File out) {  
        FileInputStream fis  = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
            fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];  
            int i = 0;  
            while ((i = fis.read(buf)) != -1) {  
                fos.write(buf, 0, i);  
            }  
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {  
            try {
                if (fis != null) fis.close();  
                if (fos != null) fos.close();  
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }  
        return;
    }

    /** ***************************************************************
     * Reads an XML configuration file from the directory
     * configDirPath, and tries to find a configuration file elsewhere
     * if configDirPath is null.  The method initializeOnce() sets the
     * preferences based on the contents of the configuration file.
     * This routine has the side effect of setting the variable called
     * "configuration".  It also creates the KBs directory and an
     * empty configuration file if none exists.
     */
    private SimpleElement readConfiguration(String configDirPath) {
        SimpleElement configuration = null;
        BufferedReader br = null;
        try {
            System.out.println("ENTER KBmanager.readConfiguration(" 
                               + configDirPath
                               + ")"); 
            String kbDirStr = configDirPath;
            if (StringUtil.emptyString(kbDirStr)) {
                kbDirStr = (String) preferences.get("kbDir");
                if (StringUtil.emptyString(kbDirStr)) {
                    kbDirStr = System.getProperty("user.dir");
                }
            }
            File kbDir = new File(kbDirStr);
            if (!kbDir.exists()) {
                kbDir.mkdir();
                preferences.put("kbDir", kbDir.getCanonicalPath());
            }
            String username = (String) preferences.get("userName");
            String userrole = (String) preferences.get("userRole");
            String config_file = ((StringUtil.isNonEmptyString(username)
                                   && StringUtil.isNonEmptyString(userrole)
                                   && userrole.equalsIgnoreCase("administrator") 
                                   && !username.equalsIgnoreCase("admin"))
                                  ? (username + "_")
                                  : "") + CONFIG_FILE;
            File configFile = new File(kbDir, config_file);
            File global_config = new File(kbDir, CONFIG_FILE);
            //File configFile = new File(kbDir, CONFIG_FILE);
            if (!configFile.exists()) {
                if (global_config.exists()) {
                    copyFile(global_config, configFile);
                    configFile = global_config;
                }
                else writeConfiguration();
            }

            System.out.println("  username == " + username);
            System.out.println("  userrole == " + userrole);
            System.out.println("  configFile == " + configFile.getCanonicalPath());

            br = new BufferedReader(new FileReader(configFile));
            SimpleDOMParser sdp = new SimpleDOMParser();
            configuration = sdp.parse(br);
        }
        catch (Exception ex) {
            System.out.println("ERROR in KBmanager.readConfiguration("
                               + configDirPath
                               + "):\n"
                               + "  Exception parsing configuration file \n" 
                               + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (br != null) {
                    br.close();
                }
                System.out.println("EXIT KBmanager.readConfiguration(" 
                                   + configDirPath
                                   + ")");
                System.out.println("  => " + configuration); 
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
        return configuration;
    }

    /** ***************************************************************
     * Reads an XML configuration file from the most likely locations,
     * trying the value of the System property "user.dir" as a last
     * resort.  The method initializeOnce() sets the preferences based
     * on the contents of the configuration file.  This routine has
     * the side effect of setting the variable called "configuration".
     * It also creates the KBs directory and an empty configuration
     * file if none exists.
     */
    private SimpleElement readConfiguration() {
        return readConfiguration(null);
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  
     */
    public void initializeOnce() {
        initializeOnce(null);
        return;
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  If
     * configFileDir is not null and a configuration file can be read
     * from the directory, reinitialization is forced.
     */
    public void initializeOnce(String configFileDir) {
        try {
            System.out.println("ENTER KBmanager.initializeOnce("
                               + configFileDir
                               + ")");
            System.out.println("  initialized == " + initialized);
            if (!initialized || StringUtil.isNonEmptyString(configFileDir)) {
                setDefaultAttributes();
                SimpleElement configuration = readConfiguration(configFileDir);
                if (configuration == null) {
                    throw new Exception("Error reading configuration file");
                }
                // System.out.println( "configuration == " + configuration );
                String result = fromXML(configuration);
                if (StringUtil.isNonEmptyString(result)) {
                    error = result;
                }

                String kbDir = (String) preferences.get("kbDir");
                System.out.println("  kbDir == " + kbDir);

                PasswordService.getInstance();

                LanguageFormatter.readKeywordMap(kbDir);
            }
        }
        catch (Exception ex) {
                System.out.println("ERROR in KBmanager.initializeOnce("
                                   + configFileDir
                                   + ")");
                System.out.println(ex.getMessage());
                ex.printStackTrace();
        }
        initialized = true;
        System.out.println("  initialized == " + initialized);
        System.out.println("EXIT KBmanager.initializeOnce("
                           + configFileDir
                           + ")");
        return;
    }

    /** ***************************************************************
     * Double the backslash in a filename so that it can be saved to a text
     * file and read back properly.
     */
    public static String escapeFilename(String fname) {

        StringBuffer newstring = new StringBuffer("");
        
        for (int i = 0; i < fname.length(); i++) {
            if (fname.charAt(i) == 92 && fname.charAt(i+1) != 92) 
                newstring = newstring.append("\\\\");
            if (fname.charAt(i) == 92 && fname.charAt(i+1) == 92) {
                newstring = newstring.append("\\\\");
                i++;
            }
            if (fname.charAt(i) != 92)
                newstring = newstring.append(fname.charAt(i));
        }
        return newstring.toString();
    }

    /** ***************************************************************
     * Create a new empty KB with a name.
     * @param name - the name of the KB
     */

    public void addKB(String name) {

        KB kb = new KB(name,(String) preferences.get("kbDir"));
        kbs.put(name.intern(),kb); 
        System.out.println("INFO in KBmanager.addKB: Adding KB: " + name);
    }

    /** ***************************************************************
     * Remove a knowledge base.
     * @param name - the name of the KB
     */

    public void removeKB(String name) {

        KB kb = (KB) kbs.get(name);
        if (kb == null) {
            error = "KB " + name + " does not exist and cannot be removed.";
            return;
        }
        try {
            if (kb.inferenceEngine != null) 
                kb.inferenceEngine.terminate();
        }
        catch (IOException ioe) {
            System.out.println("Error in KBmanager.removeKB(): Error terminating inference engine: " + ioe.getMessage());
        }
        kbs.remove(name);
        try {
            writeConfiguration();
        }
        catch (IOException ioe) {
            System.out.println("Error in KBmanager.removeKB(): Error writing configuration file. " + ioe.getMessage());
        }

        System.out.println("INFO in KBmanager.removeKB: Removing KB: " + name);
    }

    /** ***************************************************************
     * Write the current configuration of the system.  Call 
     * writeConfiguration() on each KB object to write its manifest.
     */
    public void writeConfiguration() throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        Iterator it; 
        String dir = (String) preferences.get("kbDir");
        File fDir = new File(dir);
        String username = (String) preferences.get("userName");
        String userrole = (String) preferences.get("userRole");
        String config_file = ((username != null && userrole.equalsIgnoreCase("administrator") && !username.equalsIgnoreCase("admin"))?username + "_" :"") + CONFIG_FILE;
        File file = new File(fDir, config_file);
        // File file = new File(fDir, CONFIG_FILE);
        System.out.println("wrote "+fDir+"/"+"CONFIG_FILE");
        String key;
        String value;
        KB kb = null;

        SimpleElement configXML = new SimpleElement("configuration");

        it = preferences.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            value = (String) preferences.get(key);
            //System.out.println("INFO in KBmanager.writeConfiguration(): key, value: " + key + " " + value);
            if (key.compareTo("kbDir") == 0 || key.compareTo("celtdir") == 0 || 
                key.compareTo("inferenceEngine") == 0 || key.compareTo("inferenceTestDir") == 0)
                value = escapeFilename(value);
            if ((key.compareTo("userName") != 0) && (key.compareTo("userRole") != 0)) {
                SimpleElement preference = new SimpleElement("preference");
                preference.setAttribute("name",key);
                preference.setAttribute("value",value);
                configXML.addChildElement(preference);
            }
        }
        it = kbs.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            kb = (KB) kbs.get(key);
            SimpleElement kbXML = kb.writeConfiguration();            
            configXML.addChildElement(kbXML);
        }

        try {
            fw = new FileWriter( file );
            pw = new PrintWriter(fw);
            pw.println(configXML.toFileString());
        }
        catch (java.io.IOException e) {                                                  
            throw new IOException("Error writing file " + file.getCanonicalPath() + ".\n " + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    /** ***************************************************************
     * Get the KB that has the given name.
     */
    public KB getKB(String name) {

        if (!kbs.containsKey(name))
            System.out.println("Error in KBmanager.getKB(): KB " + name + " not found.");
        return (KB) kbs.get(name.intern());
    }

    /** ***************************************************************
     * Returns true if a KB with the given name exists.
     */
    public boolean existsKB(String name) {

        return kbs.containsKey(name);
    }

    
    /** ***************************************************************
     * Remove the KB that has the given name.
     */       
    public void remove(String name) {
        kbs.remove(name);
    }
    
    /** ***************************************************************
     * Get the one instance of KBmanager from its class variable.
     */
    public static KBmanager getMgr() {

        if (manager == null) {
            manager = new KBmanager();
        }
        return manager;
    }

    /** ***************************************************************
     * Reset the one instance of KBmanager from its class variable.
     */
    public static KBmanager newMgr(String username) {

        System.out.println("ENTER KBmanager.newMgr(" + username + ")");
        System.out.println("  old manager == " + manager);

        manager = new KBmanager();
        manager.initialized = false;

        System.out.println("  new manager == " + manager);
        String userRole = PasswordService.getInstance().getUser(username).getRole();

        manager.setPref("userName",username);
        manager.setPref("userRole", userRole);

        System.out.println("  userRole == " + userRole);
        System.out.println("EXIT KBmanager.newMgr(" + username + ")");

        return manager;
    }
    
    /** ***************************************************************
     * Get the Set of KB names in this manager.
     */
    public Set getKBnames() {
        return kbs.keySet();
    }
    
    /** ***************************************************************
     * Get the the complete list of languages available in all KBs
     */
    public ArrayList allAvailableLanguages() {

        ArrayList result = new ArrayList();
        Iterator it = kbs.keySet().iterator();
        while (it.hasNext()) {
            String kbName = (String) it.next();
            KB kb = (KB) getKB(kbName);
            result.addAll(kb.availableLanguages());
        }
        return result;
    }
    
    /** ***************************************************************
     * Get the preference corresponding to the given kef.
     */    
    public String getPref(String key) {
        String ans = (String) preferences.get(key);
        if ( ans == null ) {
            ans = "";
        }
        return ans;
    }
    
    /** ***************************************************************
     * Set the preference to the given value.
     */
    public void setPref(String key, String value) {
        preferences.put(key,value);
    }

    /** ***************************************************************
     * Returns an int value, the bitwise interpretation of which
     * indicates the current configuration of inference parameter
     * (preference) settings.  The int value is computed from the
     * KBmanager preferences at the time this method is evaluated.
     *
     * @return An int value indicating the current configuration of
     * inference parameters, according to KBmanager preference
     * settings.
     */
    public int getInferenceBitValue () {
        int bv = 0;
        String[] keys = { "typePrefix", "holdsPrefix", "cache", "TPTP" };
        int[] vals = { USE_TYPE_PREFIX, USE_HOLDS_PREFIX, USE_CACHE, USE_TPTP };
        String pref = null;
        for ( int i = 0 ; i < keys.length ; i++ ) {
            pref = this.getPref( keys[i] );
            if ( Formula.isNonEmptyString(pref) && pref.equalsIgnoreCase("yes") ) {
                bv += vals[i];
            }
        }
        return bv;
    }

    /** ***************************************************************
     * Returns the last cached inference bit value setting.
     *
     * @return An int value indicating the inference parameter
     * configuration at the time the value was set.
     */
    public int getOldInferenceBitValue () {
        return this.oldInferenceBitValue;
    }

    /** ***************************************************************
     * Sets the value of the private variable oldInferenceBitValue.
     *
     * @return void
     */
    public void setOldInferenceBitValue ( int bv ) {
        this.oldInferenceBitValue = bv;
        return;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        try {

            KBmanager.getMgr().initializeOnce();
        } catch (Exception e ) {
            System.out.println(e.getMessage());
        }

        KB kb = KBmanager.getMgr().getKB("SUMO");

        Formula f = new Formula();
        f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");
        System.out.println(f.preProcess(false,kb));

        //System.out.println(KBmanager.getMgr().getKBnames());
        //System.out.println(kb.name);
        //System.out.println(LanguageFormatter.htmlParaphrase("", "(or (instance ?X0 Relation) (not (instance ?X0 TotalValuedRelation)))", 
        //                                              kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), "EnglishLanguage"));

    }
}
