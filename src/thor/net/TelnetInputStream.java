package thor.net;
import java.io.*;
import java.net.*;
/*
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

class TelnetInputStream 
    extends PushbackInputStream 
    implements TelnetConstants {
  private TelnetTerminalHandler ioHandler;
  private int inTelnetState=NOP;
  private TelnetOutputStream ioOut;

  public TelnetInputStream(InputStream poIn, TelnetOutputStream poOut,
			   TelnetTerminalHandler poHandler) {
    super(poIn);
    ioOut=poOut;
    ioHandler=poHandler;
  }

  public int available() throws IOException {
    int c = super.read();
    if (inTelnetState==NOP && c!=IAC) {
      super.unread(c);
      return 1;
    } else {
      super.unread(c);
      return 0;
    }
  }

  public boolean markSupported() { return false; }

  public void reset() throws IOException { throw new IOException(); }

  public long skip(long n) throws IOException { throw new IOException(); }

  public int read(byte[] b,
		  int off,
		  int len) throws IOException {
    if (len<=0) return 0;
    int c = read();
    if (c<0) return c;
    b[0]=(byte)(0xff&c);
    return 1;
  }

  public int read() throws IOException {
    int c;
    while (true) {
      c = super.read();
      //System.out.println("["+c+"] s:"+inTelnetState);
      if (c<0) return c;
      if (inTelnetState==NOP) {
	if (c==IAC) {
	  inTelnetState=IAC;
	  continue;
	} else if (c<=CR) {
	  switch (c) {
	  case LF: ioHandler.LineFeed(); break;
	  case CR: ioHandler.CarriageReturn(); break;
	  case BS: ioHandler.BackSpace(); break;
	  case HT: ioHandler.HorizontalTab(); break;
	  case BEL: ioHandler.Bell(); break;
	  case NULL: ioHandler.Null(); break;
	  case VT: ioHandler.VerticalTab(); break;
          case FF: ioHandler.FormFeed(); break;
	  default: return c;
	  }
	} else {
	  return c;
	}
      } else {
	//System.out.println(TelnetCommands.toString(inTelnetState));
	switch (inTelnetState) {
	case IAC: // already saw IAC
	  if (c==IAC) { // escaped IAC
	    inTelnetState=NOP;
	    return c;
	  } else inTelnetState=c;
	  break;

	case SB: ioHandler.OtherCommands(SB); break;
		 // Indicates that what follows is
	         // subnegotiation of the indicated
	         // option.
	case SE: ioHandler.OtherCommands(SE); break;
	         // End of subnegotiation parameters.
	case DM: ioHandler.OtherCommands(DM); break;
	         // The data stream portion of a Synch.
        case BRK: ioHandler.OtherCommands(BRK); break;
	         // NVT character BRK.
        case IP: ioHandler.OtherCommands(IP); break;
	         // The function Interrupt Process.
        case AO: ioHandler.OtherCommands(AO); break;
	         // The function Abort Output.
        case AYT:ioHandler.OtherCommands(AYT); break;
	         // The function Are You There.
        case EC: ioHandler.OtherCommands(EC); break;
	         // The function Erase Character.
        case EL: ioHandler.OtherCommands(EL); break;
	         // The function Erase Line.
        case GA: ioHandler.OtherCommands(GA); break;
	         // The Go Ahead signal.

	case WONT: inTelnetState=NOP; 
	  TelnetOptions.toString(c);
	  if (ioHandler.isWantedOption(c))
	    ioHandler.removeWantedOption(c);
	  ioHandler.setOption(c, false);
	  break; 
	case DONT: inTelnetState=NOP; 
	  TelnetOptions.toString(c);
	  ioHandler.setOption(c, false);
	  break;
	case WILL: inTelnetState=NOP; 
	  TelnetOptions.toString(c);
	  if (ioHandler.isWantedOption(c))
	    ioHandler.setOption(c, true);
	  else ioOut.sendcmd((byte)DONT, (byte)c);
	  break;
	case DO: inTelnetState=NOP;
	  TelnetOptions.toString(c);
	  if (ioHandler.isWantedOption(c))
	    ioHandler.setOption(c, true);
	  else ioOut.sendcmd((byte)WONT, (byte)c);
	  break;
	}
      }
    }
  }
    
  void setTelnetTerminalHandler(TelnetTerminalHandler poHandler) {
    ioHandler=poHandler;
  }

  TelnetTerminalHandler getTelnetTerminalHandler() {
    return ioHandler;
  }
}
