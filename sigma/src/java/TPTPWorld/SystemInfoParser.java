package TPTPWorld;

import java.util.*;
import java.io.*;
import java.util.regex.*;

public class SystemInfoParser {

  public static final String SYSTEM_TAG = "System";
  public static final String VERSION_TAG = "Version";
  public static final String URL_TAG = "URL";
  public static final String PRE_COMMAND_TAG = "PreCommand";
  public static final String COMMAND_TAG = "Command";
  public static final String SOLVED_TAG = "Solved";
  public static final String START_SOLN_TAG = "StartSoln";
  public static final String END_SOLN_TAG = "EndSoln";
  public static final String STATUS_TAG = "Status";


  private Vector<ATPSystem> atpSystemList;

  /* remove leading whitespace */
  public static String ltrim(String source) {
    return source.replaceAll("^\\s+", "");
  }

  /* remove trailing whitespace */
  public static String rtrim(String source) {
    return source.replaceAll("\\s+$", "");
  }

  public SystemInfoParser(FileReader file) throws Exception {
    atpSystemList = new Vector();
    BufferedReader bin = new BufferedReader(file);
    String line = "";

    String name = "";
    String version = "";
    String preCommand = "";
    String command = "";
    String url = "";
    Vector<String> solved[];
    Vector<String> startSoln[];
    Vector<String> endSoln[];
    Vector<String> status;

    // initial vectors
    solved = new Vector[2];
    solved[0] = new Vector();
    solved[1] = new Vector();
    startSoln = new Vector[2];
    startSoln[0] = new Vector();
    startSoln[1] = new Vector();
    endSoln = new Vector[2];
    endSoln[0] = new Vector();
    endSoln[1] = new Vector();
    status = new Vector();

    int count = 0;

    while ((line = bin.readLine()) != null) {      
      count++;
      if (!line.equals("")) {
        int split = line.indexOf(":");
        if (split == -1) {
          System.err.println("Unable to parse SystemInfo line: " + count + ".  Please use a colon delimiter.");
          System.exit(0);
        }
        String tag = line.substring(0, split);
        String value = ltrim(line.substring(split+1, line.length()));
        if (value.equals("")) {
          continue;
        }
        // System tag
        if (tag.equals(SystemInfoParser.SYSTEM_TAG)) {
          if (!name.equals("")) {
            System.err.println("Unable to parse SystemInfo line: " + count + ".  Please separate each System in SystemInfo by a blank line");
            System.exit(0);
          }
          name = value;
        }
        // Version tag
        if (tag.equals(SystemInfoParser.VERSION_TAG)) {
          version = value;
        }
        // URL tag
        if (tag.equals(SystemInfoParser.URL_TAG)) {
          url = value;
        }
        // PreCommand tag
        if (tag.equals(SystemInfoParser.PRE_COMMAND_TAG)) {
          preCommand = value;
        }
        // Command tag
        if (tag.equals(SystemInfoParser.COMMAND_TAG)) {
          command = value;
        }
        // Solved tag
        if (tag.equals(SystemInfoParser.SOLVED_TAG)) {
          int sp = value.indexOf("=");
          if (sp == -1) {
            System.err.println("Unable to parse SystemInfo line: " + count + ".  Please separate solved values with an equal sign: =");
            System.exit(0);
          }
          solved[0].add(rtrim(value.substring(0,sp)));
          solved[1].add(ltrim(value.substring(sp+1,value.length())));
        }

        // Start Solution tag
        if (tag.equals(SystemInfoParser.START_SOLN_TAG)) {
          int sp = value.indexOf("=");
          if (sp == -1) {
            System.err.println("Unable to parse SystemInfo line: " + count + ".  Please separate startSoln values with an equal sign: =");
            System.exit(0);
          }
          startSoln[0].add(rtrim(value.substring(0,sp)));
          startSoln[1].add(ltrim(value.substring(sp+1,value.length())));
        }

        // End Solution tag
        if (tag.equals(SystemInfoParser.END_SOLN_TAG)) {
          int sp = value.indexOf("=");
          if (sp == -1) {
            System.err.println("Unable to parse SystemInfo line: " + count + ".  Please separate endSoln values with an equal sign: =");
            System.exit(0);
          }
          endSoln[0].add(rtrim(value.substring(0,sp)));
          endSoln[1].add(ltrim(value.substring(sp+1,value.length())));
        }

        // Status tag
        if (tag.equals(SystemInfoParser.STATUS_TAG)) {
          StringTokenizer st = new StringTokenizer(value, " ");
          while (st.hasMoreTokens()) {
            status.add(st.nextToken());
          }
        }
      } else {
        // reach empty line, add last system
        atpSystemList.add(new ATPSystem(name, 
                                        version, 
                                        preCommand, 
                                        command, 
                                        url, 
                                        solved, 
                                        startSoln, 
                                        endSoln,
                                        status));
        // reset values
        name = "";
        version = "";
        preCommand = "";
        command = "";
        url = "";
        solved = new Vector[2];
        solved[0] = new Vector();
        solved[1] = new Vector();
        startSoln = new Vector[2];
        startSoln[0] = new Vector();
        startSoln[1] = new Vector();
        endSoln = new Vector[2];
        endSoln[0] = new Vector();
        endSoln[1] = new Vector();
        status = new Vector();
      }
    }
    // just incase there was no empty last line, add last system
    if (!name.equals("")) {
      atpSystemList.add(new ATPSystem(name, 
                                      version, 
                                      preCommand, 
                                      command, 
                                      url, 
                                      solved, 
                                      startSoln, 
                                      endSoln,
                                      status));

    }
  }

  Vector<ATPSystem> getSystemList () {
    return atpSystemList;
  }

  public static void main (String[] args) throws Exception {
    String filename = args[0];
    FileReader file = new FileReader(new File(filename));
    SystemInfoParser sp = new SystemInfoParser(file);
  }

}
