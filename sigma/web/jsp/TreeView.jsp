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
 String term = request.getParameter("term");
 if (!Formula.isNonEmptyString(smpl))
     term = "";
%>
  <TITLE>TreeView Knowledge Base Browser - <%=term%></TITLE>
<%
  String kbName = "";
  String language = "";
  StringBuffer show = null;
  KB kb = null;
  String parentPage = "TreeView.jsp";
  String simple = request.getParameter("simple");
  if (Formula.isNonEmptyString(simple) && simple.equals("yes")) {
%>
    <%@ include file="SimpleBrowseBody.jsp" %>
<%
  }
  else {
%>
    <%@ include file="BrowseBody.jsp" %>
<%
  }
  if (term == null) 
    term = TaxoModel.defaultTerm;
  TaxoModel.toggleNode(term);
%>

<%
  if (Formula.isNonEmptyString(simple) && simple.equals("yes")) {
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
        <% out.print(TaxoModel.toHTML(simple)); %><p>
        <!-- form>           
            <INPUT type="radio" name="simple" value="yes" <%   // default to simplified view
        		if (simple == null || simple.equalsIgnoreCase("yes")) 
        		    out.print("checked=yes"); 
        		%>>yes</input>
            <INPUT type="radio" name="simple" value="no" <%   // default to simplified view
        		if (simple != null && simple.equalsIgnoreCase("no")) 
        		    out.print("checked=no"); 
        		%>>no</input><br>Show simplified definition
            <input type="hidden" name="term" value=<%=term%>>
            <INPUT type="submit" value="Submit">
        </form -->
        </td>
        <td valign="top" width="1" BGCOLOR='#A8BACF'>
          <IMG SRC='pixmaps/1pixel.gif' width=1 border=0>
        </td>
        <td valign=top>
            <%=show.toString() %>
            <!-- iframe name=imageframe id=imageframe frameborder=0 width="100%" src="<%=TaxoModel.termPage%>?kb=<%=kbName%>&term=<%=term%>" height="100%">you 
                shouldn't see this</iframe -->
        </td>
    </tr>
</table><p>
<%
  if (Formula.isNonEmptyString(simple) && simple.equals("yes")) {
%>
    <a href="SimpleBrowse.jsp?kb=<%=kbName%>&simple=yes&term=<%=term%>">Show without tree</a><p>
<%
  }
  else {
%>
    <a href="Browse.jsp?kb=<%=kbName%>&term=<%=term%>">Show without tree</a><p>
<%
  }
%>

<%@ include file="Postlude.jsp" %>
