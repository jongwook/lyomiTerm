package thor.net;
/** Debug class for printing Telnet Options
 *  <p>
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

class TelnetOptions implements TelnetConstants {
  public static String toString(int pnState) {
    switch(pnState) {
    default:
      return "unknown{"+pnState+"}";
    }
  }
}
