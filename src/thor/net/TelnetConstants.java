package thor.net;
/*  Various constants used by telnet.
 *  <p>
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

public interface TelnetConstants {
  /** Null */
  public final static int NULL = (int) 0;
  /** Bell */
  public final static int BEL = (int) 7;
  /** Back Space  */
  public final static int BS = (int) 8;
  /** Horizontal Tab */
  public final static int HT = (int) 9;
  /** Line Feed */
  public final static int LF = (int) 10;
  /** Vertical Tab */
  public final static int VT = (int) 11;
  /** Form Feed */
  public final static int FF = (int) 12; 
  /** Carriage Return */
  public final static int CR = (int) 13;
 
  /** End of subnegotiation paramaters. */ 
  public final static int SE = (int) 240;
  /** No operation. */
  public final static int NOP = (int) 241;

  /** Data Mark, the data stream portion of a Synch.
      This should always be accompanied
      by a TCP Urgent notification. */
  public final static int DM = (int) 242;

  /**  NVT character Break. */
  public final static int BRK = (int) 243;
  /** The function Interrupt Process. */
  public final static int IP = (int) 244;

  /** The function Abort Output. */
  public final static int AO = (int) 245;
  /** The function Are You There. */
  public final static int AYT = (int) 246;
  /** The function Erase character. */
  public final static int EC = (int) 247; 
  /** The function Erase Line. */
  public final static int EL = (int) 248;
  /** The Go ahead signal. */
  public final static int GA = (int) 249;
  /** Indicates that what follows is
   *  subnegotiation of the indicated
   *  option. */
  public final static int SB = (int) 250;
  /** WILL see rfc854 */
  public final static int WILL = (int) 251;
  /** WONT see rfc854 */
  public final static int WONT = (int) 252;
  /** DO see rfc854 */
  public final static int DO   = (int) 253;
  /** DON'T see rfc854 */
  public final static int DONT = (int) 254;
  /** Interpret As Command */
  public final static int IAC  = (int) 255;

  /** terminal type option */
  public final static int TERMINAL_TYPE = (int) 24;
  /** end of record option rfc930 */  
  public final static int END_OF_RECORD  = (int) 25;
  /** User identification option rfc927 */
  public final static int TUID = (int) 26;
  /** Output marking telnet option rfc933 */
  public final static int OUTMRK  = (int) 27; 
}
