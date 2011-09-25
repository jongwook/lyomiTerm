package thor.net;
import java.io.*;
import java.net.*;
/*
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

class TelnetOutputStream extends OutputStream
    implements TelnetConstants {
  OutputStream sOut;

  public TelnetOutputStream(OutputStream pOut) {
    sOut=pOut;
  }

  public void sendcmd(byte command, byte option) throws IOException {
    byte msg [] = {(byte) 255, command, option};
    sOut.write(msg);
  }

  public void write(int b) throws IOException {
    if (b==IAC) sOut.write(IAC);
    sOut.write(b);
  }

  public void flush() throws IOException {
    sOut.flush();
  }
}
