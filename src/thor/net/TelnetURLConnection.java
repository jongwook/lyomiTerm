package thor.net;
import java.io.*;
import java.net.*;
/**
 * This is class creates a Telnet connection from a telnet URL, see the
 * thor.app classes for examples.
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

public class TelnetURLConnection extends URLConnection
    implements TelnetConstants {

  private TelnetInputStream ioTelnetIn;

  private TelnetOutputStream ioTelnetOut;

  private TelnetTerminalHandler ioHandler = new DefaultTelnetTerminalHandler();

  private Socket sock;

    /** Creates a TelnetURLConnection object, 
     *  call connect before using.
     */
  public TelnetURLConnection(URL poURL) {
    super(poURL);
  }

    /** This actually connects to the host at the specified port.
     *  The Input and Output streams are created in this step.
     */
  public void connect() throws IOException {
    sock = new Socket (url.getHost(), url.getPort());
    ioTelnetOut = new TelnetOutputStream(sock.getOutputStream());
    ioTelnetIn = new TelnetInputStream(sock.getInputStream(), 
				       ioTelnetOut, ioHandler);
  }

    /** Disconnect from the host. It would be safe to
     *  call connect again. 
     */
  public void disconnect() throws IOException {
    sock.close();
    ioTelnetOut=null;
    ioTelnetIn=null;
    sock=null;    
  }

    /** If true telnet session is currently open.
     *  Unlike a regular URLConnection, it can be
     *  closed once open with the disconnect method.
     */
  public boolean connected() { return sock!=null; }

    /** Fetches the OutputStream to the host.
     *  IAC (ascii 255) is escaped, all other
     *  characters pass through unaffected.
     */
  public OutputStream getOutputStream() { return ioTelnetOut; }

    /** Fetches the InputStream from the host.
     *  All telnet special characters are 
     *  stripped from the data stream and either
     *  handled directly or forwarded to the
     *  TelnetTerminalHandler. Generally
     *  options are requested in by the
     *  TelnetTerminalHandler, and it handles
     *  special characters LF, CR, BELL, BACKSPACE, etc
     *  itself. These are optional in the standard
     *  but are really important for a decent
     *  terminal program.
     *  <p>
     *  The DefaultTelnetTerminalHandler
     *  handles options gracefully, but doesn't
     *  handle the LF, CR, etc at all.
     *  See the examples in thor.app for
     *  examples on how to handle these.
     */
  public InputStream getInputStream() { return ioTelnetIn; }

    /** This allows you to set your own TelnetTerminalHandler.
     *  Implementing a TelnetTerminalHandler is generally
     *  the first step in writing a telnet program.
     */
  public void setTelnetTerminalHandler(TelnetTerminalHandler poHandler) {
    ioHandler=poHandler;
    ioTelnetIn.setTelnetTerminalHandler(ioHandler);
  }
    
    /** Returns the TelnetTerminalHandler currently in use.
     */
  public TelnetTerminalHandler getTelnetTerminalHandler() {
    return ioHandler;
  }
  
    /** Bypasses the escapes of IAC in the OutputStream and
     *  sends a command of the form [IAC,command,option]
     *  Use with caution.
     */
  public void sendCommand(int command, int option) throws IOException {
    ioTelnetOut.sendcmd((byte)command, (byte)option);
  }

    /** Call this to get options negotiated */
  public void updateOptions() {
    try {
      int wanted[]=getTelnetTerminalHandler().getWantedOptions();
      for (int i=0; i<wanted.length; i++) {
	sendCommand(DO, wanted[i]);
      }
      int nwanted[]=getTelnetTerminalHandler().getNotWantedOptions();
      for (int i=0; i<nwanted.length; i++) {
	sendCommand(DONT, nwanted[i]);
      }    
    } catch (Exception e) {}
  }
}
