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

/**
 * TreeView.jsp responds to several HTTPD parameters:
 * term     = <name>   - the SUMO term to browse
 * kb       = <name>   - the name of the knowledge base
 * lang     = <lang>   - the name of the language used to display axiom paraphrases
 * simple   = <yes/no> - whether to display a simplified view of the term
 * contract = <name>   - a term name appearing in the tree view that should have hidden children (not yet implemented)
 * expand   = <name>   - a term name appearing in the tree view that should have children displayed (not yet implemented)
 * up       = <name>   - a node that should have its parents displayed (not yet implemented)
 * down     = <name>   - a node that should not have its parents displayed (not yet implemented)
 * */
 TaxoModel.kbName = kbName;

 String nonRelTerm = request.getParameter("nonrelation");
 String relTerm = request.getParameter("relation");
 String relREmatch = request.getParameter("relREmatch");
 String nonRelREmatch = request.getParameter("nonRelREmatch");
 
 String term = request.getParameter("term");
 if (!StringUtil.isNonEmptyString(term)) 
   term = TaxoModel.defaultTerm;

 if (StringUtil.emptyString(nonRelTerm))
 	nonRelTerm = "";
 if (StringUtil.emptyString(relTerm))
    relTerm = "";
 
 String KBPOS = request.getParameter("KBPOS");
 if (KBPOS == null && term == null)
    KBPOS = "1";
 else if (KBPOS == null && term != null)
 	KBPOS = kb.REswitch(term);
 	
 String contract = request.getParameter("contract");
 if (StringUtil.isNonEmptyString(contract)) 
     TaxoModel.collapseNode(contract);
 String expand = request.getParameter("expand");
 if (StringUtil.isNonEmptyString(expand)) 
     TaxoModel.expandNode(expand);
 String up = request.getParameter("up");
 if (StringUtil.isNonEmptyString(up)) 
     TaxoModel.expandParentNodes(up);
 String down = request.getParameter("down");
 if (StringUtil.isNonEmptyString(down)) 
     TaxoModel.collapseParentNodes(down);
 
 String simple = request.getParameter("simple");
 String kbHref = "http://" + hostname + ":" + port + "/sigma/TreeView.jsp?kb=" + kbName + 
 "&simple=" + simple + "&lang=" + language + "&flang=" + flang + "&term=";

 TaxoModel.displayTerm(term);
%>
  <TITLE>TreeView Knowledge Base Browser - <%=term%></TITLE>
<%
  StringBuffer show = null;
  String parentPage = "TreeView.jsp";
  
  if (StringUtil.isNonEmptyString(simple) && simple.equals("yes")) {
%>
    <%@ include file="SimpleBrowseBody.jsp" %>
<%
  }
  else {
%>
    <%@ include file="BrowseBody.jsp" %>
<%
  }
  if (StringUtil.isNonEmptyString(simple) && simple.equals("yes")) {
%>
    <%@ include file="SimpleBrowseHeader.jsp" %>
<%
  }
  else {
%>
    <%@ include file="BrowseHeader.jsp" %>
<%
  }
%>

<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr>
</table><BR>

<table border=0 width='100%' height='100%'>
    <tr>
        <td height='100%' valign=top>
            <% out.print(TaxoModel.toHTML(kbHref)); %><p>
        </td>
        <td valign="top" width="1" BGCOLOR='#A8BACF'>
          <IMG SRC='pixmaps/1pixel.gif' width=1 border=0>
        </td>
        <td valign=top>
            <%=show.toString() %>
        </td>
    </tr>
</table><p>
<%
  if (StringUtil.isNonEmptyString(simple) && simple.equals("yes")) {
%>
    <small><a href="SimpleBrowse.jsp?kb=<%=kbName%>&simple=yes&lang=<%=language%>&term=<%=term%>">Show without tree</a></small><p>
<%
  }
  else {
%>
    <small><a href="Browse.jsp?kb=<%=kbName%>&term=<%=term%>">Show without tree</a></small><p>
<%
  }
%>

<%@ include file="Postlude.jsp" %>

