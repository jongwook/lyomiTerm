package thor.net;
/** Debugging class for printing Telnet Commands.
 *  <p>
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

class TelnetCommands implements TelnetConstants {
  public static String toString(int pnState) {
    switch(pnState) {
    case WILL:
      return "will";
    case WONT:
      return "won't";
    case DO:
      return "do";
    case DONT:
      return "don't";
    case IAC:
      return "interpret as command";
    case SB:
      return "subnegotiation";
    
    default:
      return "unknown("+pnState+")";
    }
  }
}
