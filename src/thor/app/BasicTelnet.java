package thor.app;

import java.awt.*;
import java.io.*;
import java.net.*;
import thor.net.*;

/** BasicTelnet is a simple AWT Component that implements a
 *  simple UNIX type terminal, and allows easy extension
 *  to more complex terminals. In particular VT100Telnet
 *  implements a large portion of the VT100 terminal
 *  with little more than the VT100 tables from the X Consortium.
 *
 *  <pre>
 *  use:
 *    java thor.app.BasicTelnet myhost [myport]
 *  </pre>
 *
 *  ANSI is just a little different from VT100 and if
 *  you decide to implement it send me a copy :)
 *
 *  This was originally written in 1996, before the Java 1.1
 *  event model. I haven't made many changes before releasing
 *  this to the public so it should be easy to port to Java 1.1
 *
 *  -- Daniel Kristjansson   May 27, 2000
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

public class BasicTelnet extends Canvas implements Runnable {
  final static boolean debug=false;
  final static int columns = 80, lines = 24;
  final static int telnetPort = 23;
  int PTWidth, PTHeight, charOffset, lineOffset, topOffset;
  char screen [] [];
  Color screenfg [] [];
  Color screenbg [] [];
  boolean redraw = true;
  boolean lineRedraw [];
  Color bgcolor = Color.white;
  Color fgcolor = Color.black;
  Color d_bgcolor = Color.white;
  Color d_fgcolor = Color.black;
  Font PTFont;
  //
  boolean wrap=true;
  int xloc=0, yloc=0;
  InputStream sIn;
  OutputStream sOut;
  Thread receive;
  int telnetState=0;
  boolean option [];
  DefaultTelnetTerminalHandler telnetHandler;
  Image backBuffer;

  {
    telnetHandler = new BasicTelnetTerminalHandler();
  }

  class BasicTelnetTerminalHandler extends DefaultTelnetTerminalHandler {
    public void LineFeed() {
      yloc++;
      xloc=0;
      scrValid();
      lineRedraw[yloc]=true;
    }
    public void BackSpace() {
      xloc--;
      if (xloc<0) {
	yloc--; xloc=columns-1;
	if (yloc<0) yloc=0;
      }
    }
    public void HorizontalTab() {
      int n = (8 - xloc % 8);
      for (int j=0; j<n; j++) normal((byte) 32);
    }
    public void CarriageReturn() {}
    public void Null() {}
    public void FormFeed() {}
    public void ClearScreen() {}
    public void VerticalTab() {}
  }

  public static void main(String[] args) {
    try {
      String host = (args.length>0)?args[0]:"graphics.nyu.edu";
      int port = (args.length>1)?Integer.parseInt(args[1]):telnetPort;	
      Component t = new BasicTelnet(host, port);
      Frame f = new Frame(host) {
	public void update(Graphics g) {}
	public void paint(Graphics g) {}
      };
      f.setLayout(new BorderLayout());
      f.add(t, BorderLayout.CENTER);
      f.setResizable(false);
      f.pack();
      f.show();
    } catch (Exception e) { e.printStackTrace(); }
  }

    /** openConnection() uses thor.net.URLStreamHandler to
     *	open a "telnet://host:port" connection.
     */
  void openConnection(String host, int port) throws IOException {
    URL url=new URL("telnet", host, port, "", new thor.net.URLStreamHandler());
    URLConnection urlConnection=url.openConnection();
    urlConnection.connect();
    ((TelnetURLConnection)urlConnection).
      setTelnetTerminalHandler(telnetHandler);
    sOut=urlConnection.getOutputStream();
    sIn=urlConnection.getInputStream();
 
    telnetHandler.addWantedOption(1); //char by char mode
    ((TelnetURLConnection)urlConnection).updateOptions();

    receive = new Thread(this);
    receive.start();
  }

    /** scrValid() checks that the cursor is on the screen and
     *  scrolls when the text runs off the end of the screen
     */
  protected final void scrValid() {
    if (xloc >= columns) 
      if (wrap) {xloc=0; yloc++;}
      else xloc=columns-1;
    if (yloc >= lines) {
      /* normally a screen buffer like this is keeps a "current top"
	 variable and modifies drawing rutines appropriatelly, but
	 since Java impliments multidimentional arrays with arrays
	 of references to the bottom level data, it is not terribly
	 expensive to actually scroll the data.  I have done it here
	 to simplify the draw rutines.
      */
      for (int j=0;j<lines-1;j++) {
	screen[j]=screen[j+1];
	screenfg[j]=screenfg[j+1];
	screenbg[j]=screenbg[j+1];
	lineRedraw [j] = true;
      }
      screen[lines-1]=new char [columns];
      screenfg[lines-1]=new Color [columns];
      screenbg[lines-1]=new Color [columns];
      for (int j=0;j<columns;j++) {
	screen   [lines-1][j]=' ';
	screenfg [lines-1][j]=fgcolor;
	screenbg [lines-1][j]=bgcolor;
      }
      lineRedraw[lines-1] = true;
      yloc--;
    }
  }

    /** initProcess() is intended to initialize any processing */
  public void initProcess() {}
    /** process() in BasicTelnet simply calls normal() */
  public void process(byte c) {
    normal(c);
  }

    /** normal() puts a character on the screen and sets it's color
     *	to the current color, then increments the cursor location.
     */
  protected final void normal(byte c) {
    if (debug) System.out.print((char) c);
    lineRedraw[yloc]=true;
    screen[yloc][xloc] = (char) c;
    screenfg[yloc][xloc] = fgcolor;
    screenbg[yloc][xloc] = bgcolor;
    xloc++;
    scrValid();
    repaint();
  }
    
    /** run() handles characters coming from the telnet connection
     *  and calls process() on each character.
     */
  public void run() {
    int c;
    try {
      while (true) {
	c=sIn.read();
	if (c<0) System.exit(0);
	process((byte)c);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

    /** This constructor opens a telnet connection to host
     *  on port 23, the standard telnet port.
     */
  public BasicTelnet(String host) throws IOException {
    this(host, telnetPort);
  }

    /** This constructor opens a telnet connection to host
     *  at the specified port.
     */
  public BasicTelnet(String host, int port) throws IOException {
    //allocate 'screen'
    screen = new char [lines] [columns];

    lineRedraw = new boolean [lines];
    screenfg = new Color [lines] [columns];
    screenbg = new Color [lines] [columns];
    for (int i=0;i<lines;i++) {
      lineRedraw[i] = true;
      for (int j=0;j<columns;j++) {
        screenfg [i][j]=fgcolor;
        screenbg [i][j]=bgcolor;
      }
    }
    
    //start processing, needed for ANSI & VT100
    initProcess();

    //open connection etc.
    openConnection(host,port);

    //discover the font..decide how big telnet area has to be.
    PTFont = new Font("Courier", Font.PLAIN, 14);
    FontMetrics fm = getFontMetrics(PTFont);
    topOffset = fm.getMaxAscent() + 1;
    charOffset = fm.stringWidth("X");
    lineOffset = fm.getHeight();
    PTWidth = charOffset * columns;
    PTWidth = PTWidth+4;
    PTHeight = lineOffset*lines + 3;
    super.resize(PTWidth+1, PTHeight+1);
  }

    /** This is the old Java 1.1 way to get characters
     *  from the keyboard.
     */
  public boolean keyUp(Event evt, int key) {
    try {
      sOut.write((byte)key);
    } catch (Exception e) { e.printStackTrace(); }
    return(super.keyUp(evt, key));
  }

    /** update() is the real workhorse of drawing the
     *	component. It need not be changed for VT100
     *  emulation. This is double buffered and
     *  keeps track of which lines need to be
     *  redrawn. Scrolling could be improved
     *  but this is only a demo app...
     */
  public void update(Graphics realG) {
    if (backBuffer==null)
      backBuffer = createImage(PTWidth+1, PTHeight+1);
    Graphics g = backBuffer.getGraphics();
    int j,beg; boolean ok;
    g.setFont(PTFont);
    //draw lines
    for (int i=0;i<lines;i++) {
     if (lineRedraw[i] == true) {
      lineRedraw[i] = false;
      j=0;
      Color fg,bg;
      while (j < columns-1) {
       fg=screenfg[i][j];
       bg=screenbg[i][j];
       beg=j;ok=true;
       while (++j < columns && ok)
        if (fg!=screenfg[i][j] || bg!=screenbg[i][j]) ok = false;
       if (ok==false) j--;
       g.setColor(bg);
       g.fillRect(3+beg*charOffset, 2+i*lineOffset, charOffset*(j-beg), lineOffset);
       g.setColor(fg);
       g.drawChars(screen[i], beg, j-beg, 3+beg*charOffset, topOffset+i*lineOffset);
      }
     }
    }
    //draw cursor
    if (yloc<lines && xloc<columns && yloc>=0 && xloc>=0) {
      g.setColor(new Color(screenbg[yloc][xloc].getRGB() ^ 0xFFFFFF));
      g.fillRect(3+xloc*charOffset, 2+yloc*lineOffset, charOffset, lineOffset);
      g.setColor(new Color(screenfg[yloc][xloc].getRGB() ^ 0xFFFFFF));
      g.drawChars(screen[yloc], xloc, 1, 3+xloc*charOffset, topOffset+yloc*lineOffset);
      lineRedraw[yloc]=true;
    } else {
      if (debug) System.out.println("Illegal Cursor: "+xloc+", "+yloc);
    }
    g.setColor(d_fgcolor); // draw border
    g.drawRect(0,0,PTWidth,PTHeight);
    realG.drawImage(backBuffer, 0, 0, d_bgcolor, this);
  }

    /** paint() just calls update()
     */
  public void paint(Graphics g) {
    update(g);
  }
}
