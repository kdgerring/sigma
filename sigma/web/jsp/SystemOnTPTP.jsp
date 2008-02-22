<%@include file="Prelude.jsp" %>
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
August 9, Acapulco, Mexico.
*/
   //System.out.println("INFO in SystemOnTPTP.jsp");
   
  String hostname = KBmanager.getMgr().getPref("hostname");
  if (hostname == null) {
    hostname = "localhost";
  }
//-----------------------------------------------------------------------------
//----Check if SystemOnTPTP exists in a local copy of TPTPWorld
  String TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
  String systemsDir = KBmanager.getMgr().getPref("systemsDir");
//  String systemsInfo = KBmanager.getMgr().getPref("baseDir") + "/KBs/systemsInfo.xml";
  String systemsInfo = systemsDir + "/SystemInfo";
  String SoTPTP =  TPTPWorld + "/SystemExecution/SystemOnTPTP";
  String tptp4X = TPTPWorld + "/ServiceTools/tptp4X";
  boolean tptpWorldExists = (new File(SoTPTP)).exists();
  boolean builtInExists = (new File(systemsDir)).exists() && (new File(systemsInfo)).exists();
  String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";

//----Code for getting the list of systems
  String responseLine;
  String defaultSystemLocal = "";
  String defaultSystemRemote = "";
  String defaultSystemBuiltIn = "";
  ArrayList<String> systemListLocal = new ArrayList<String>();
  ArrayList<String> systemListRemote = new ArrayList<String>();
  ArrayList<String> systemListBuiltIn = new ArrayList<String>();
  BufferedReader reader;
  BufferedWriter writer;
        

//----If local copy of TPTPWorld exists, call local SystemOnTPTP
  if (tptpWorldExists) {
    String command = SoTPTP + " " + "-w" + " " + "SoTPTP";
    Process proc = Runtime.getRuntime().exec(command);
    systemListLocal.add("Choose system");
    try {
      reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//----Read List of Local Systems
      while ((responseLine = reader.readLine()) != null) {
        systemListLocal.add(responseLine);
//----Try use EP as the default system
        if (responseLine.startsWith("EP---")) {
          defaultSystemLocal = responseLine;
        }
      }
      reader.close();
    } catch (Exception ioe) {
      System.err.println("Exception: " + ioe.getMessage());
    }
  }

//----If built in Systems Directory exist, call built-in SystemOnTPTP
  if (builtInExists) {
//    out.println("SystemsDir: " + SystemOnTPTP.getSystemsDir());
//    out.println("SystemsInfo: " + SystemOnTPTP.getSystemsInfo());

    systemListBuiltIn = SystemOnTPTP.listSystems(systemsDir, "SoTPTP");
    defaultSystemBuiltIn = "EP---0.999";
  }        

//----Call RemoteSoT to retrieve remote list of systems
  Hashtable URLParameters = new Hashtable();

//----Note, using www.tptp.org does not work
  String SystemOnTPTPFormReplyURL =
    "http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTPTPFormReply";

  systemListRemote.add("Choose system");
  URLParameters.put("NoHTML","1");
  URLParameters.put("QuietFlag","-q2");
  URLParameters.put("SubmitButton","ListSystems");
  URLParameters.put("ListStatus","SoTPTP");

  try {
    reader = new BufferedReader(new InputStreamReader(
    ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
//----Read List of Remote Systems
    while ((responseLine = reader.readLine()) != null) {
      systemListRemote.add(responseLine);
//----Try use EP as the default system
      if (responseLine.startsWith("EP---")) {
        defaultSystemRemote = responseLine;
      }
    }
    reader.close();
  } catch (Exception ioe) {
    System.err.println("Exception: " + ioe.getMessage());
  }

//-----------------------------------------------------------------------------
//----Code for building the query part

  String kbName = request.getParameter("kb");
  KB kb;
  if (kbName == null) {
    kb = null;
  } else {
    kb = KBmanager.getMgr().getKB(kbName);
  }
  String language = request.getParameter("lang");
  language = HTMLformatter.processLanguage(language,kb);
  String stmt = request.getParameter("stmt");
  int maxAnswers = 1;
  int timeout = 30;
  Iterator systemIterator;
  String systemName;
  String quietFlag = request.getParameter("quietFlag");
  String systemChosenLocal = request.getParameter("systemChosenLocal");
  String systemChosenRemote = request.getParameter("systemChosenRemote");
  String systemChosenBuiltIn = request.getParameter("systemChosenBuiltIn");
  String location = request.getParameter("systemOnTPTP");  
  String tstpFormat = request.getParameter("tstpFormat");
  String sanitize = request.getParameter("sanitize");
  String systemChosen;

  if (request.getParameter("maxAnswers") != null) {
    maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
  }
  if (request.getParameter("timeout") != null) {
    timeout = Integer.parseInt(request.getParameter("timeout"));
  }
  if (quietFlag == null) {
    quietFlag = "-q4";
  }
  if (systemChosenLocal == null) {
    systemChosenLocal = defaultSystemLocal;
  }
  if (systemChosenRemote == null) {
    systemChosenRemote = defaultSystemRemote;
  }
  if (systemChosenBuiltIn == null) {
    systemChosenBuiltIn = defaultSystemBuiltIn;
  }
  if (location == null) {
    if (tptpWorldExists) { 
      location = "local";
    } else if (builtInExists) {
      location = "local";
    } else {
      location = "remote";
    }
  }

  if (location.equals("local")) {
    if (tptpWorldExists) { 
      systemChosen = systemChosenLocal;
    } else {
      systemChosen = systemChosenBuiltIn;
    }
  } else {
    systemChosen = systemChosenRemote;
  }

  if (tstpFormat == null) {
    tstpFormat = "";
  }
  if (sanitize == null) {
    sanitize = "no";
  }
  if (stmt == null || stmt.equalsIgnoreCase("null")) {
    stmt = "(exists (?X) (instance ?X Relation))";
  } else {
    System.out.println(stmt.trim());
  }
%>
  <HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - TPTP</TITLE>
  <!-- <style>@import url(kifb.css);</style> -->
  <script type="text/javascript">//<![CDATA[
    var tstp_dump;
    function openSoTSTP (dump) {
      var tstp_url = 'http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTSTP';
      var tstp_browser = window.open(tstp_url, '_blank');
      tstp_dump = dump;
    }
    function getTSTPDump () {
      return tstp_dump;
    }
<% if (tptpWorldExists && location.equals("local")) { %>
    var current_location = "Local";
<% } else if (builtInExists && location.equals("local")) { %>
    var current_location = "BuiltIn";
<% } else { %>
    var current_location = "Remote";
<% } %>
//----Toggle to either the local/builtin/remote list by showing new and hiding current
    function toggleList (location) {
      if (current_location == location) {
        return;
      }
      var obj;
      obj = window.document.getElementById("systemList" + current_location);
      if (obj) {
        obj.setAttribute("style","display:none");
      }
      current_location = location;
      obj = window.document.getElementById("systemList" + location);
      if (obj) {
        obj.setAttribute("style","display:inline");
      }
    }
  //]]></script>
  </HEAD>
  <BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF">

  <FORM name="SystemOnTPTP" ID="SystemOnTPTP" action="SystemOnTPTP.jsp" METHOD="POST">
  <TABLE width="95%" cellspacing="0" cellpadding="0">
  <TR>
  <TD ALIGN=LEFT VALIGN=TOP><IMG SRC="pixmaps/sigmaSymbol-gray.gif"></TD>
  <TD ALIGN=LEFT VALIGN=TOP><img src="pixmaps/logoText-gray.gif"><BR>
      <B>SystemOnTPTP Interface</B></TD>
  <TD VALIGN=BOTTOM></TD>
  <TD> <FONT FACE="Arial, Helvetica" SIZE=-1>
       [ <A HREF="KBs.jsp"><B>Home</B></A>&nbsp;|&nbsp;
         <A HREF="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>"><B>Graph</B></A>&nbsp;|&nbsp;
         <A HREF="Properties.jsp"><B>Prefs</B></A>&nbsp;]&nbsp;
         <B>KB</B>:&nbsp;
<%
         ArrayList kbnames = new ArrayList();
         kbnames.addAll(KBmanager.getMgr().getKBnames());
         out.println(HTMLformatter.createMenu("kb",kbName,kbnames));
%>
         <B>Language:</B>&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %>
         <BR></TD>
  </TR>
  </TABLE>

  <IMG SRC='pixmaps/1pixel.gif' WIDTH=1 HEIGHT=1 BORDER=0><BR>
  <TEXTAREA ROWS=5 COLS=70" NAME="stmt"><%=stmt%></TEXTAREA><BR>
  Query time limit:<INPUT TYPE=TEXT SIZE=3 NAME="timeout" VALUE="<%=timeout%>">
<!--  
Maximum answers: <INPUT TYPE=TEXT SIZE=3 NAME="maxAnswers" VALUE="<%=maxAnswers%>">
-->
  <BR>
  System:
<%
  String params;
  //----Create atp drop down list for local
  if (tptpWorldExists) {
    if (location.equals("local")) {
      params = "ID=systemListLocal style='display:inline'";
    } else {
      params = "ID=systemListLocal style='display:none'";
    }
    out.println(HTMLformatter.createMenu("systemChosenLocal",systemChosenLocal,
                                         systemListLocal, params)); 
  }
  //----Create atp drop down list for builtin
  if (builtInExists && !tptpWorldExists) {
    if (location.equals("local")) {
      params = "ID=systemListBuiltIn style='display:inline'";
    } else {
      params = "ID=systemListBuiltIn style='display:none'";
    }
    out.println(HTMLformatter.createMenu("systemChosenBuiltIn", systemChosenBuiltIn,
                                         systemListBuiltIn, params));
  }
  //----Create atp drop down list for remote
  if ((!tptpWorldExists && !builtInExists) || location.equals("remote")) {
    params = "ID=systemListRemote style='display:inline'";
  } else {
    params = "ID=systemListRemote style='display:none'";
  }
  out.println(HTMLformatter.createMenu("systemChosenRemote",systemChosenRemote,
                                       systemListRemote, params));
%>
  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="local"
<% if (!tptpWorldExists && !builtInExists) { out.print(" DISABLED"); } %>
<% if (location.equals("local")) { out.print(" CHECKED"); } %>
<% if (tptpWorldExists) {
     out.println("onClick=\"javascript:toggleList('Local');\"");
   } else {
     out.println("onClick=\"javascript:toggleList('BuiltIn');\"");     
   }
%>
  >Local SystemOnTPTP
  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="remote"
<% if (location.equals("remote")) { out.print(" CHECKED"); } %>
  onClick="javascript:toggleList('Remote');">Remote SystemOnTPTP
  <BR>
  <INPUT TYPE="hidden" NAME="sanitize" VALUE="yes"
<% if (sanitize.equalsIgnoreCase("yes")) { out.print(" CHECKED"); } %>
  >
  <INPUT TYPE="hidden" NAME="tstpFormat" VALUE="-S"
<% if (tstpFormat.equals("-S")) { out.print(" CHECKED"); } %>
  >
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q4"
<% if (quietFlag.equals("-q4")) { out.print(" CHECKED"); } %>
  >TPTP Proof
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="IDV"
<% if (quietFlag.equals("IDV")) { out.print(" CHECKED"); } %>
  >IDV-Proof tree
  <INPUT TYPE=RADIO NAME="quietFlag" ID="hyperlinkedKIF" VALUE="hyperlinkedKIF"
<% if (quietFlag.equals("hyperlinkedKIF")) { out.print(" CHECKED"); } %>
  >Hyperlinked KIF
  <BR>
  <INPUT TYPE=SUBMIT NAME="request" value="Ask">
<% if (KBmanager.getMgr().getPref("userName") != null && KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) { %>
    <INPUT type="submit" name="request" value="Tell"><BR>
<% } %>
  </FORM>
  <hr>

<%
//<APPLET CODE="IDVApplet" archive="http://web.cs.miami.edu/~strac/test/IDV/IDV.jar" WIDTH=800 HEIGHT=100 MAYSCRIPT=true>
//  <PARAM NAME="URL" VALUE="http://web.cs.miami.edu/~strac/test/IDV/files/PUZ001+1.tptp">
//  Hey, you cant see my applet!!!
//</APPLET>
%>

<%
//-----------------------------------------------------------------------------
//----Code for doing the query
  String TPTP_QUESTION_SYSTEM = "SNARK---";
  String TPTP_ANSWER_SYSTEM = "Metis---";
  String req = request.getParameter("request");
  boolean syntaxError = false;
  StringBuffer sbStatus = new StringBuffer();
  String kbFileName;
  Formula conjectureFormula;
//----Result of query (passed to tptp4X then passed to HTMLformatter.formatProofResult)
  String result = "";
  String newResult = "";
  String idvResult = "";
  String originalResult = "";
  String command;
  Process proc;
  boolean isQuestion = systemChosen.startsWith(TPTP_QUESTION_SYSTEM);
  String conjectureTPTPFormula = "";

//----If there has been a request, do it and report result
  if (req != null && !syntaxError) {
    try {
      if (req.equalsIgnoreCase("tell")) {
        Formula statement = new Formula();
        statement.theFormula = stmt;
        String port = KBmanager.getMgr().getPref("port");
        if ((port == null) || port.equals(""))
          port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
        out.println("Status: ");
        out.println(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
      } else if (req.equalsIgnoreCase("test")) {
        out.println("<PRE>");
        out.println(InferenceTestSuite.test(kb, systemChosen, out));
        out.println("</PRE>");
      } else if (req.equalsIgnoreCase("Ask")) {
//-----------------------------------------------------------------------------
//----Call RemoteSoT
        //----Add KB contents here
        conjectureFormula = new Formula();
        conjectureFormula.theFormula = stmt;
        conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
        //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.theFormula);
        conjectureFormula.tptpParse(true,kb);
        Iterator it = conjectureFormula.getTheTptpFormulas().iterator();
        String theTPTPFormula = (String) it.next();
        conjectureTPTPFormula =  "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
        //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.getTheTptpFormulas());
        kbFileName = kb.writeTPTPFile(null,
                                      conjectureFormula,
                                      sanitize.equalsIgnoreCase("yes"),
                                      systemChosen,
                                      isQuestion);
        if (location.equals("remote")) {
          if (systemChosen.equals("Choose%20system")) {
            out.println("No system chosen");
          } else {
//----Need to check the name exists
            URLParameters.clear();
            URLParameters.put("NoHTML","1");
            if (quietFlag.equals("IDV")) {
              URLParameters.put("IDV","-T");
              URLParameters.put("QuietFlag","-q4");
              URLParameters.put("X2TPTP",tstpFormat);
            } else if (quietFlag.equals("hyperlinkedKIF")) {
              URLParameters.put("QuietFlag","-q3");
              URLParameters.put("X2TPTP","-S");
            }else {
              URLParameters.put("QuietFlag",quietFlag);
              URLParameters.put("X2TPTP",tstpFormat);
            }
//----Need to offer automode
            URLParameters.put("System___System",systemChosen);
            URLParameters.put("TimeLimit___TimeLimit",
                              new Integer(timeout));
            URLParameters.put("ProblemSource","UPLOAD");
            URLParameters.put("UPLOADProblem",new File(kbFileName));
            URLParameters.put("SubmitButton","RunSelectedSystems");
  
            reader = new BufferedReader(new InputStreamReader(
                         ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
            out.println("(Remote SystemOnTPTP call)");
            out.println("<PRE>");
            boolean tptpEnd = false;
            while ((responseLine = reader.readLine()) != null) {
              if (responseLine.startsWith("Loading IDV")) {
                tptpEnd = true;
              }
              if (!responseLine.equals("") && !responseLine.substring(0,1).equals("%") && !tptpEnd) {
                result += responseLine + "\n";
              }           
              if (tptpEnd && quietFlag.equals("IDV")) {
                idvResult += responseLine + "\n";
              }
              originalResult += responseLine + "\n";
              if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) { out.println(responseLine); }
            }
            out.println("</PRE>");
            reader.close();
//-----------------------------------------------------------------------------
//----Calling remote tptp4X 
//----NOTE: remote tptp4x call phased out (using TPTP2SUMO.java for conversion)
            /*
            if (quietFlag.equals("hyperlinkedKIF")) {
              out.println("<hr>");
              URLParameters.clear();
              URLParameters.put("NoHTML","1");
              URLParameters.put("X2TPTP",tstpFormat);
              URLParameters.put("QuietFlag","-q0");
              URLParameters.put("System___System","tptp4X---0.0");
              URLParameters.put("TimeLimit___TimeLimit", new Integer(30));
              URLParameters.put("ProblemSource","FORMULAE");
              URLParameters.put("FORMULAEProblem",result);
              URLParameters.put("SubmitButton","RunSelectedSystems");
              reader = new BufferedReader(new InputStreamReader(
                           ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
              while ((responseLine = reader.readLine()) != null) {
                newResult += responseLine + "\n";
              }
              reader.close();
              out.println(HTMLformatter.formatProofResult(newResult,
                                                          stmt,
                                                          stmt,
                                                          lineHtml,
                                                          kbName,
                                                          language));       
              out.println("<hr>");
            }
            */
          }
        } else if (location.equals("local") && tptpWorldExists) {
//-----------------------------------------------------------------------------
//----Call local copy of TPTPWorld instead of using RemoteSoT
          if (systemChosen.equals("Choose%20system")) {
            out.println("No system chosen");
          } else {
            if (quietFlag.equals("hyperlinkedKIF")) {
              command = SoTPTP + " " +
                        "-q3"        + " " +  // quietFlag
                        systemChosen + " " + 
                        timeout      + " " +
                        "-S"         + " " +  //tstpFormat
                        kbFileName;
            } else if (quietFlag.equals("IDV")) {
              command = SoTPTP + " " +
                        "-q4"        + " " +  // quietFlag
                        systemChosen + " " + 
                        timeout      + " " +
                        "-S"           + " " +  //tstpFormat
                        kbFileName;            
            } else {
              command = SoTPTP + " " + 
                        quietFlag    + " " + 
                        systemChosen + " " + 
                        timeout      + " " + 
                        tstpFormat   + " " +
                        kbFileName;
            }
            out.println("(Local SystemOnTPTP call)");
            proc = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            out.println("<PRE>");
            while ((responseLine = reader.readLine()) != null) {
              if (!responseLine.equals("") && !responseLine.substring(0,1).equals("%")) {
                result += responseLine + "\n";
              }
              originalResult += responseLine + "\n";
              if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) { out.println(responseLine); }
            }
            out.println("</PRE>");
            reader.close();
//-----------------------------------------------------------------------------
//----Calling local tptp4X (if tptpWorldExists and toggle button is on "local")
//----NOTE: local tptp4x call phased out (using TPTP2SUMO.java for conversion)
            /*
            if (quietFlag.equals("hyperlinkedKIF")) {
              out.println("<hr>");
              command = tptp4X    + " " + 
                        "-f sumo" + " " +
                        "--";
              proc = Runtime.getRuntime().exec(command);
              writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
              reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
              writer.write(result);
              writer.flush();
              writer.close();
              newResult = "";
              while ((responseLine = reader.readLine()) != null) {
                newResult += responseLine + "\n";
              }
              reader.close();
              out.println(HTMLformatter.formatProofResult(newResult,
                                                          stmt,
                                                          stmt,
                                                          lineHtml,
                                                          kbName,
                                                          language));       
              out.println("<hr>");
            }
            */
          }
        } else if (location.equals("local") && builtInExists && !tptpWorldExists) {
//-----------------------------------------------------------------------------
//----Call built in SystemOnTPTP instead of using RemoteSoT or local
          if (systemChosen.equals("Choose%20system")) {
            out.println("No system chosen");
          } else {
//----Set quiet flag
            String qq;
            String format;
            if (quietFlag.equals("IDV")) {
              qq = "-q4";
              format = "-S";
            } else if (quietFlag.equals("hyperlinkedKIF")) {
              qq = "-q4";
              format = "-S";
            } else {
              qq = quietFlag;
              format = tstpFormat;
            }
            //out.println("chosen system: " + systemChosen);
            result = SystemOnTPTP.SystemOnTPTP(systemChosen, systemsDir, timeout, qq, format, kbFileName);
            originalResult += result;
            out.println("(Built-In SystemOnTPTP call)");
            out.println("<PRE>");
            if (!quietFlag.equals("hyperlinkedKIF") && !quietFlag.equals("IDV")) {
              out.println(result);
            } 
            if (quietFlag.equals("IDV")) {
              StringTokenizer st = new StringTokenizer(result,"\n");
              String temp = "";
              while (st.hasMoreTokens()) {
                String next = st.nextToken(); 
                if (!next.equals("") && !next.substring(0,1).equals("%")) {
                  temp += next + "\n";   
                }
              }
              result = temp;
            }
            out.println("</PRE>");
          }            
        } else {
          out.println("INTERNAL ERROR: chosen option not valid: " + location + ".  Valid options are: 'Local SystemOnTPTP, Built-In SystemOnTPTP, or Remote SystemOnTPTP'.");
        }
      if (quietFlag.equals("IDV") && location.equals("remote")) {
        if (SystemOnTPTP.isTheorem(originalResult)) {
          int size = SystemOnTPTP.getTPTPFormulaSize(result);
          if (size == 0) {
            out.println("No solution output by system.  IDV tree unavaiable.");
          } else {
            out.println(idvResult);
          }
        } else {
          out.println("Not a theorem.  IDV tree unavailable.");
        }
      } else if (quietFlag.equals("IDV") && !location.equals("remote")) {
        if (SystemOnTPTP.isTheorem(originalResult)) {
          int size = SystemOnTPTP.getTPTPFormulaSize(result);
          if (size > 0) {
            String port = KBmanager.getMgr().getPref("port");
            if ((port == null) || port.equals(""))
              port = "8080";
            String libHref = "http://" + hostname + ":" + port + "/sigma/lib";
            out.println("<APPLET CODE=\"IDVApplet\" archive=\"" + libHref + "/IDV.jar," + libHref + "/TptpParser.jar," + libHref + "/antlr-2.7.5.jar," + libHref + "/ClientHttpRequest.jar\"");
            out.println("WIDTH=800 HEIGHT=100 MAYSCRIPT=true>");
            out.println("  <PARAM NAME=\"TPTP\" VALUE=\"" + result + "\">");
            out.println("  Hey, you cant see my applet!!!");
            out.println("</APPLET>");
          } else {
            out.println("No solution output by system.  IDV tree unavaiable.");
          }
        } else {
          out.println("Not a theorem.  IDV tree unavailable.");
        }
      } else if (quietFlag.equals("hyperlinkedKIF")) {
        boolean isTheorem = SystemOnTPTP.isTheorem(originalResult);
          try {
//----If selected prover is not an ANSWER system, send proof to default ANSWER system (Metis)
            if (!systemChosen.startsWith(TPTP_ANSWER_SYSTEM)) {
              String answerResult = AnswerFinder.findProofWithAnswers(result, systemsDir);
//----If answer is blank, ERROR, or WARNING, do not place in result
              if (!answerResult.equals("") && 
                  !answerResult.startsWith("% ERROR:") &&
                  !answerResult.startsWith("% WARNING:")) {
                result = answerResult;
              } 
//----If ERROR is answer result, report to user
              if (answerResult.startsWith("% ERROR:")) {
                out.println("==" + answerResult);
              } 
            }  
            if (systemChosen.startsWith(TPTP_QUESTION_SYSTEM)) {
              ArrayList<Binding> answer = SystemOnTPTP.getSZSBindings(conjectureTPTPFormula, originalResult);
              newResult = TPTP2SUMO.convert(result, answer);
            } else {
              newResult = TPTP2SUMO.convert(result);
            }
            if (!isTheorem) {
//----Not a theorem, print no
              newResult = "<queryResponse>\n <answer result='no'> \n  </answer> \n <summary proofs='0'/> \n </queryResponse>";
            } 
            out.println(HTMLformatter.formatProofResult(newResult,
                                                        stmt,
                                                        stmt,
                                                        lineHtml,
                                                        kbName,
                                                        language));       
           } catch (Exception e) {}      
        /*
        command = tptp4X    + " " + 
                  "-f sumo" + " " +
                  "--";
        proc = Runtime.getRuntime().exec(command);
        writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        writer.write(result);
        writer.flush();
        writer.close();
        result = "";
        while ((responseLine = reader.readLine()) != null) {
          result += responseLine + "\n";
        }
        reader.close();
        out.println(HTMLformatter.formatProofResult(result,
                                                    stmt,
                                                    stmt,
                                                    lineHtml,
                                                    kbName,
                                                    language));       
        */
      }
     }
//----Delete the kbFile
//      (new File(kbFileName)).delete();
    } catch (IOException ioe) {
    out.println(ioe.getMessage());
    }
  }
%>
<p>

<%@ include file="Postlude.jsp" %>
   </BODY>
   </HTML>
