<%@ include file="Prelude.jsp" %>

<%
/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
if (!mgr.getPref("userRole").equalsIgnoreCase("administrator")) 
    response.sendRedirect("KBs.jsp");     
else {
    String kbDir = mgr.getPref("kbDir");
    File kbDirFile = new File(kbDir);
    MultipartParser mpp = null;
    int postSize = Integer.MAX_VALUE;
    Part requestPart = null;
    String fileName = "";
    String baseName = "";
    String overwrite = mgr.getPref("overwrite");
    boolean overwriteP = (StringUtil.isNonEmptyString(overwrite)
                          && overwrite.equalsIgnoreCase("yes"));
    String extension = "";
    File existingFile = null;
    File outfile = null;
    long writeCount = -1L;

    try {  
        boolean isError = false;
        mpp = new MultipartParser(request, postSize, true, true);
        while ((requestPart = mpp.readNextPart()) != null) {
            String paramName = requestPart.getName();
            if (paramName == null) 
                paramName = "";
            if (requestPart.isParam()) {
                ParamPart pp = (ParamPart) requestPart;
                if (paramName.equalsIgnoreCase("kb"))
                    kbName = pp.getStringValue();
            }
            else if (requestPart.isFile()) {
                FilePart fp = (FilePart) requestPart;
                fileName = fp.getFileName();
                int lidx = fileName.lastIndexOf(".");
                baseName = ((lidx != -1)
                            ? fileName.substring(0, lidx)
                            : fileName);
                extension = ((lidx != -1)
                            ? fileName.substring(lidx,fileName.length())
                            : ".kif");
                existingFile = new File(kbDirFile, (baseName + extension));

                System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);
                outfile = StringUtil.renameFileIfExists(existingFile);
                FileOutputStream fos = new FileOutputStream(outfile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                writeCount = -1L;
                try {
                    writeCount = fp.writeTo(bos);
                    bos.flush();
                    bos.close();
                    fos.close();
                }
                catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        String errStr = "";
        if (overwriteP && !existingFile.getCanonicalPath().equalsIgnoreCase(outfile.getCanonicalPath())) {
            boolean overwriteSucceeded = false;
            try {
                if (existingFile.delete() && outfile.renameTo(existingFile)) {
                    outfile = existingFile;
                    overwriteSucceeded = outfile.canRead();
                }
            }
            catch (Exception owex) {
                owex.printStackTrace();
            }
            if (!overwriteSucceeded)
                errStr = "Error: Could not overwrite existing consituent file";
        }
        if (StringUtil.emptyString(errStr)) {
            if (StringUtil.emptyString(kbName))
                errStr = "Error in AddConstituent.jsp: No knowledge base name specified";              
            else if ((outfile == null) || !outfile.canRead()) 
                errStr = "Error in AddConstituent.jsp: The constituent file could not be saved or cannot be read";
        }
        if (StringUtil.isNonEmptyString(errStr)) {
            mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
            System.out.println(errStr);
            isError = true;
            response.sendRedirect("KBs.jsp"); 
        }
        else {
            boolean newKB = false;          
            if (!mgr.existsKB(kbName))  
                newKB = true;
            else
                kb = mgr.getKB(kbName);
                           
            if (newKB) {
                ArrayList list = new ArrayList();
                list.add(outfile.getCanonicalPath());
                mgr.loadKB(kbName, list);
            } 
            else { // Remove the constituent, if it is already present.    
                ListIterator<String> lit = kb.constituents.listIterator();
                while (lit.hasNext()) {
                    String constituent = lit.next();
                    if (StringUtil.isNonEmptyString(baseName) && constituent.contains(baseName))
                        lit.remove();
                }                         
                kb.addNewConstituent(outfile.getCanonicalPath());
                if (mgr.getPref("cache").equalsIgnoreCase("yes")) 
                    kb.kbCache.cache();                          
                kb.loadVampire();
                KBmanager.getMgr().writeConfiguration();              
            }              
        }
        if (!isError) 
            response.sendRedirect("Manifest.jsp?kb=" + kbName);          
    }
    catch (Exception e) {
        String errStr = "ERROR in AddConstituent.jsp: " + e.getMessage();
        mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
        System.out.println(errStr);
        System.out.println("  kbName == " + kbName);
        System.out.println("  fileName == " + fileName);
        e.printStackTrace();
        response.sendRedirect("KBs.jsp"); 
    }
}
%>

