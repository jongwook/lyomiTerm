package thor.app;

import java.awt.*;
import java.io.*;
import java.net.*;
import thor.net.*;

/** VT100 Telnet just adds a partial VT100 terminal emulation
 *  on top of BasicTelnet, it uses a slightly different
 *  TelnetTerminalHandler and a very different process().
 *  <pre>
 *  use:
 *    java thor.app.VT100Telnet myhost [myport]
 *  </pre>
 *  -- Daniel Kristjansson   May 27, 2000
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

public class VT100Telnet extends BasicTelnet {
  static final int maxInt=Integer.MAX_VALUE;
  int begScroll, endScroll;
  int [] tabStop;
  {
    telnetHandler = new VT100TelnetTerminalHandler();
  }

  class VT100TelnetTerminalHandler extends DefaultTelnetTerminalHandler {
    public void LineFeed() {
      lineRedraw[yloc]=true;
      yloc++;
      scrValid();
      lineRedraw[yloc]=true;
    }
    public void BackSpace() {
      xloc--;
      if (xloc<0) {
	lineRedraw[yloc]=true;
	yloc--; xloc=columns-1;
	if (yloc<0) yloc=0;
      }
      scrValid();
      lineRedraw[yloc]=true;
    }
    public void HorizontalTab() {
      int n = (8 - xloc % 8);
      for (int j=0; j<n; j++) normal((byte) 32);
    }
    public void CarriageReturn() {
      xloc=0;
      scrValid();
      lineRedraw[yloc]=true;
    }
    public void Null() {}
    public void FormFeed() {}
    public void ClearScreen() { clearLines(0, yloc-1); }
    public void VerticalTab() {}
  }

  public static void main(String[] args) {
    try {
      String host = (args.length>0)?args[0]:"graphics.nyu.edu";
      int port = (args.length>1)?Integer.parseInt(args[1]):telnetPort;	
      Component t = new VT100Telnet(host, port);
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

  static byte ansiUp []    = {27,91,65};
  static byte ansiDown []  = {27,91,66};
  static byte ansiRight [] = {27,91,67};
  static byte ansiLeft []  = {27,91,68};

  public boolean keyUp(Event evt, int key) {
    if (key<1000) {
      try { sOut.write((byte) key); } catch (Exception e) {};
    } else {
      byte msg2 [] = {};
      switch (key) {
      case 1004: msg2 = ansiUp;
	break;
      case 1005: msg2 = ansiDown;
	break;
      case 1006: msg2 = ansiLeft;
	break;
      case 1007: msg2 = ansiRight;
	break;
      default:
        System.out.println("key...");
        System.out.println(key);
      }
      try { sOut.write(msg2); } catch (Exception e) {};
    }
    return(true);
  }

  void VTReset () {
    initProcess();
    begScroll=0;
    nparam=0;
    saveX=0;
    saveY=0;
    /* ?? */
    scstype=0;
    curss=0;
    curgl=0;
    curgr=0;
    /* ?? */
  }

  public void initProcess() {
    wrap=false;
    parseState = groundtable;
    param = new int [10];
    for (int i=0; i<10; i++) {param[i]=DEFAULT;}
    tabStop = new int [100];
    for (int i=0; i<columns/8; i++) {
      tabStop[i]=i*8;
    }
    for (int i=columns/8; i<100; i++) {
      tabStop[i]=maxInt;
    }
    endScroll=lines;
  }

  public VT100Telnet(String host) throws IOException {
    this(host, telnetPort);
  }

  public VT100Telnet(String host, int port) throws IOException {
    super(host, port);
  }

  public void tabClear() {
    for (int i=0; i<99; i++) {
      tabStop[i]=maxInt;
    }
  }

  public void tabSet() {
    int i=0;
    while (xloc>tabStop[i]) i++;
    if (xloc==tabStop[i]) return;
    if (tabStop[i]<maxInt)
      for (int j=99; j < i; j--) {
	tabStop[j]=tabStop[j-1];
      }
    tabStop[i]=xloc;
  }

  public void tabZonk() {
    tabClear();
  }

  public int tabNext (int col) {
    int i=0;
    while (col>=tabStop[i]) i++;
    int x = columns-1 < tabStop[i] ? columns-1 : tabStop[i];
    if (debug) System.out.println(x);
    return (x);
  }

  void scrollRegionDown (int beg, int end) {
    for (int j=end;j>beg;j--) {
      screen[j]=screen[j-1];
      screenfg[j]=screenfg[j-1];
      screenbg[j]=screenbg[j-1];
      lineRedraw [j] = true;
    }
    screen[beg]=new char [columns];
    screenfg[beg]=new Color [columns];
    screenbg[beg]=new Color [columns];
    for (int j=0;j<columns;j++) {
      screenfg [beg][j]=fgcolor;
      screenbg [beg][j]=bgcolor;
    }
    lineRedraw[beg] = true;
  }

  void scrollRegionUp (int beg, int end) {
    for (int j=beg;j<end;j++) {
      screen[j]=screen[j+1];
      screenfg[j]=screenfg[j+1];
      screenbg[j]=screenbg[j+1];
      lineRedraw [j] = true;
    }
    screen[end]=new char [columns];
    screenfg[end]=new Color [columns];
    screenbg[end]=new Color [columns];
    for (int j=0;j<columns;j++) {
      screenfg [end][j]=fgcolor;
      screenbg [end][j]=bgcolor;
    }
    lineRedraw[end] = true;
  }

  /*
   * If cursor not in scrolling region, returns.  Else,
   * inserts n blank lines at the cursor's position.  Lines above the
   * bottom margin are lost.
   */
  public void insertLine (int n) {
    if (!(yloc > begScroll && yloc < endScroll)) return;
    for (;0 < n; n--) scrollRegionDown(begScroll, endScroll);
  }

  /*
   * If cursor not in scrolling region, returns.  Else, deletes n lines
   * at the cursor's position, lines added at bottom margin are blank.
   */
  public void deleteLine(int n) {
    if (!(yloc > begScroll && yloc < endScroll)) return;
    for (;0 < n; n--) scrollRegionUp(begScroll, endScroll);
  }

  /*
   * Deletes n chars at the cursor's position, no wraparound.
   */
  public void deleteChar (int n) {
    int tmp=xloc;
    n=n+xloc < columns ? n+xloc : columns;
    while (xloc<n) normal((byte) 32);
    xloc=tmp;
  }

  public void insertChar (int n) {
    for (int j=xloc; (j<n) && (j<columns-1); j++) normal((byte) 32);
  }

  /*
   * Tracks mouse while it is in bounds
   */
  public void trackMouse(int func, int startrow, int startcol,
			 int firstrow, int lastrow) {
  }

  public void cursorSet(int y, int x) {
    yloc=y;
    xloc=x;
    if (yloc>=lines) yloc=lines-1;
    if (xloc>=columns) xloc=columns-1;
    if (yloc<0) yloc=0;
    if (xloc<0) xloc=0;
  }

  /* begin "defines" */

  /*
   *	$XConsortium: VTparse.h,v 1.6 92/09/15 15:28:31 gildea Exp $
   */

  /*
   * Copyright 1987 by Digital Equipment Corporation, Maynard, Massachusetts.
   *
   *                         All Rights Reserved
   *
   * Permission to use, copy, modify, and distribute this software and its
   * documentation for any purpose and without fee is hereby granted,
   * provided that the above copyright notice appear in all copies and that
   * both that copyright notice and this permission notice appear in
   * supporting documentation, and that the name of Digital Equipment
   * Corporation not be used in advertising or publicity pertaining to
   * distribution of the software without specific, written prior permission.
   *
   *
   * DIGITAL DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
   * ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL
   * DIGITAL BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR
   * ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
   * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
   * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
   * SOFTWARE.
   */

  /*
   * The following list of definitions is generated from VTparse.def using the
   * following command line:
   *
   * grep '^CASE_' VTparse.def | awk '{printf "static final int %s = %d;\n", $1, n++}'
   *
   * You you need to change something, change VTparse.def and regenerate the
   * definitions.  This would have been automatic, but since this doesn't change
   * very often, it isn't worth the makefile hassle.
   */
  static final int CASE_GROUND_STATE = 0;
  static final int CASE_IGNORE_STATE = 1;
  static final int CASE_IGNORE_ESC = 2;
  static final int CASE_IGNORE = 3;
  static final int CASE_BELL = 4;
  static final int CASE_BS = 5;
  static final int CASE_CR = 6;
  static final int CASE_ESC = 7;
  static final int CASE_VMOT = 8;
  static final int CASE_TAB = 9;
  static final int CASE_SI = 10;
  static final int CASE_SO = 11;
  static final int CASE_SCR_STATE = 12;
  static final int CASE_SCS0_STATE = 13;
  static final int CASE_SCS1_STATE = 14;
  static final int CASE_SCS2_STATE = 15;
  static final int CASE_SCS3_STATE = 16;
  static final int CASE_ESC_IGNORE = 17;
  static final int CASE_ESC_DIGIT = 18;
  static final int CASE_ESC_SEMI = 19;
  static final int CASE_DEC_STATE = 20;
  static final int CASE_ICH = 21;
  static final int CASE_CUU = 22;
  static final int CASE_CUD = 23;
  static final int CASE_CUF = 24;
  static final int CASE_CUB = 25;
  static final int CASE_CUP = 26;
  static final int CASE_ED = 27;
  static final int CASE_EL = 28;
  static final int CASE_IL = 29;
  static final int CASE_DL = 30;
  static final int CASE_DCH = 31;
  static final int CASE_DA1 = 32;
  static final int CASE_TRACK_MOUSE = 33;
  static final int CASE_TBC = 34;
  static final int CASE_SET = 35;
  static final int CASE_RST = 36;
  static final int CASE_SGR = 37;
  static final int CASE_CPR = 38;
  static final int CASE_DECSTBM = 39;
  static final int CASE_DECREQTPARM = 40;
  static final int CASE_DECSET = 41;
  static final int CASE_DECRST = 42;
  static final int CASE_DECALN = 43;
  static final int CASE_GSETS = 44;
  static final int CASE_DECSC = 45;
  static final int CASE_DECRC = 46;
  static final int CASE_DECKPAM = 47;
  static final int CASE_DECKPNM = 48;
  static final int CASE_IND = 49;
  static final int CASE_NEL = 50;
  static final int CASE_HTS = 51;
  static final int CASE_RI = 52;
  static final int CASE_SS2 = 53;
  static final int CASE_SS3 = 54;
  static final int CASE_CSI_STATE = 55;
  static final int CASE_OSC = 56;
  static final int CASE_RIS = 57;
  static final int CASE_LS2 = 58;
  static final int CASE_LS3 = 59;
  static final int CASE_LS3R = 60;
  static final int CASE_LS2R = 61;
  static final int CASE_LS1R = 62;
  static final int CASE_PRINT = 63;
  static final int CASE_XTERM_SAVE = 64;
  static final int CASE_XTERM_RESTORE = 65;
  static final int CASE_XTERM_TITLE = 66;
  static final int CASE_DECID = 67;
  static final int CASE_HP_MEM_LOCK = 68;
  static final int CASE_HP_MEM_UNLOCK = 69;
  static final int CASE_HP_BUGGY_LL = 70;
  static final int CASE_NUL = 71;
  /* end "defines" */
  static final int DEFAULT = -1;

  int nparam;
  int param [];
  int parseState [];
  int row, col, top, bot;
  int saveX, saveY;
  /* ?? */
  int scstype;
  int curss;
  int curgl;
  int curgr;
  /* ?? */

  public void process(byte c) {
    if (debug) {
      if (parseState[((int) c) & 0xFF] != CASE_PRINT) {
        System.out.print("parseState: ");
        System.out.print(parseState[((int) c) & 0xFF]);
        System.out.print(" ");
        System.out.println((char) c);
      } else
	if ((int) c < 32) {
	  System.out.print("parseState: ");
	  System.out.print(parseState[((int) c) & 0xFF]);
	  System.out.print(" ");
	  System.out.println((int) c);
	}
    }
    switch (parseState[((int) c) & 0xFF]) {
    case CASE_PRINT:
      normal(c);
      break;

    case CASE_GROUND_STATE:
      /* exit ignore mode */
      parseState = groundtable;
      break;

    case CASE_IGNORE_STATE:
      /* Ies: ignore anything else */
      parseState = igntable;
      break;
      
    case CASE_IGNORE_ESC:
      /* Ign: escape */
      parseState = iestable;
      break;

    case CASE_IGNORE:
      /* Ignore character */
      break;
      
    case CASE_BELL:
      /* bell */
      //Bell();
      break;
      
    case CASE_BS:
      /* backspace */
      xloc--;
      if (xloc<0) {
	yloc--; xloc=columns-1;
	if (yloc<0) yloc=0;
      }
      break;
      
    case CASE_CR:
      /* carriage return */
      xloc=0;
      parseState = groundtable;
      break;

    case CASE_NUL:
      break;
      
    case CASE_ESC:
      /* escape */
      parseState = esctable;
      break;
      
    case CASE_VMOT:
      /*
       * form feed, line feed, vertical tab
       */
      yloc++;
      scrValid();
      lineRedraw[yloc]=true;
      if (debug) System.out.println("$");
      parseState = groundtable;
      break;
      
    case CASE_TAB:
      /* tab */
      xloc = tabNext(xloc);
      break;
      
    case CASE_SI:
      curgl = 0;
      break;
      
    case CASE_SO:
      curgl = 1;
      break;
      
    case CASE_SCR_STATE:
      /* enter scr state */
      parseState = scrtable;
      break;
      
    case CASE_SCS0_STATE:
      /* enter scs state 0 */
      scstype = 0;
      parseState = scstable;
      break;
      
    case CASE_SCS1_STATE:
      /* enter scs state 1 */
      scstype = 1;
      parseState = scstable;
      break;
      
    case CASE_SCS2_STATE:
      /* enter scs state 2 */
      scstype = 2;
      parseState = scstable;
      break;
      
    case CASE_SCS3_STATE:
      /* enter scs state 3 */
      scstype = 3;
      parseState = scstable;
      break;
      
    case CASE_ESC_IGNORE:
      /* unknown escape sequence */
      parseState = eigtable;
      break;
      
    case CASE_ESC_DIGIT:
      /* digit in csi or dec mode */
      if((row = param[nparam - 1]) == DEFAULT)
	row = 0;
      param[nparam - 1] = 10 * row + (c - 48);
      break;
      
    case CASE_ESC_SEMI:
      /* semicolon in csi or dec mode */
      param[nparam++] = DEFAULT;
      break;
      
    case CASE_DEC_STATE:
      /* enter dec mode */
      parseState = dectable;
      break;
      
    case CASE_ICH:
      /* ICH */
      if((row = param[0]) < 1)
	row = 1;
      insertChar(row);
      parseState = groundtable;
      break;
      
    case CASE_CUU:
      /* CUU */
      if((param[0]) < 1)
	yloc--;
      else yloc-=param[0]; 
      if (yloc<0) yloc=0; 
      parseState = groundtable;
      break;
      
    case CASE_CUD:
      /* CUD */
      if((param[0]) < 1)
	yloc++;
      else yloc+=param[0]; 
      if (yloc>=lines) yloc=lines-1;
      parseState = groundtable;
      break;
      
    case CASE_CUF:
      /* CUF */
      if((param[0]) < 1)
	xloc++;
      else xloc+=param[0]; 
      if (xloc>=columns) xloc=columns-1;
      parseState = groundtable;
      break;
      
    case CASE_CUB:
      /* CUB */
      if((param[0]) < 1)
	xloc--;
      else xloc-=param[0]; 
      if (xloc<0) xloc=0;
      parseState = groundtable;
      break;
      
    case CASE_CUP:
      /* CUP | HVP */
      if (debug) {
        System.out.println("CASE_CUP");
        System.out.print(param[0]);
        System.out.print(" ");
        System.out.println(param[1]);
      }
      if((row = param[0]) < 1)
	row = 1;
      if(nparam < 2 || (col = param[1]) < 1)
	col = 1;
      cursorSet(row-1, col-1);
      parseState = groundtable;
      break;
      
    case CASE_HP_BUGGY_LL:
      /* Some HP-UX applications have the bug that they
	 assume ESC F goes to the lower left corner of
	 the screen, regardless of what terminfo says. */
      cursorSet(lines-1, 0);
      parseState = groundtable;
      break;
      
    case CASE_ED:
      // ED
      switch (param[0]) {
      case DEFAULT:
      case 0:
        clearLines(yloc, lines-1);
	break;
	
      case 1:
        clearLines(0, yloc-1);
	break;
	
      case 2:
	clearLines(0, lines-1);
	xloc=0;
        yloc=0;
	break;
      }
      parseState = groundtable;
      break;
      
    case CASE_EL:
      /* EL */
      switch (param[0]) {
      case DEFAULT:
      case 0:
	clearLine(xloc, columns-1);
	break;
      case 1:
	clearLine(0, xloc-1);
	break;
      case 2:
	clearLine(0, columns-1);
	break;
      }
      parseState = groundtable;
      break;
			
    case CASE_IL:
      /* IL */
      if((row = param[0]) < 1)
	row = 1;
      insertLine(row);
      parseState = groundtable;
      break;
      
    case CASE_DL:
      /* DL */
      if((row = param[0]) < 1)
	row = 1;
      deleteLine(row);
      parseState = groundtable;
      break;
      
    case CASE_DCH:
      /* DCH */
      if((row = param[0]) < 1)
	row = 1;
      deleteChar(row);
      parseState = groundtable;
      break;
      
    case CASE_TRACK_MOUSE:
      /* Track mouse as long as in window and between
	 specified rows */
      trackMouse(param[0], param[2]-1, param[1]-1,
		 param[3]-1, param[4]-2);
      break;
      
    case CASE_DECID:
      param[0] = -1;		/* Default ID parameter */
      /* Fall through into ... */
    case CASE_DA1:
      /* DA1 */
      if (param[0] <= 0) {	/* less than means DEFAULT */
	/**
	   reply.a_type   = CSI;
	   reply.a_pintro = '?';
	   reply.a_nparam = 2;
	   reply.a_param[0] = 1;		// VT102
	   reply.a_param[1] = 2;		// VT102
	   reply.a_inters = 0;
	   reply.a_final  = 'c';
	   unparseseq(&reply, screen->respond);
	**/
      }
      parseState = groundtable;
      break;
      
    case CASE_TBC:
      /* TBC */
      if ((row = param[0]) <= 0) /* less than means default */
	tabClear();
      else if (row == 3)
	tabZonk();
      parseState = groundtable;
      break;
      
    case CASE_SET:
      /* SET */
      /**
	 ansi_modes(term, bitset);
      **/
      parseState = groundtable;
      break;
      
    case CASE_RST:
      /* RST */
      /**
	 ansi_modes(term, bitclr);
      **/
      parseState = groundtable;
      break;
    case CASE_SGR:
      // SGR
      rendition(param, nparam);
      parseState = groundtable;
      break;
      
    case CASE_CPR:
      // CPR
      if ((row = param[0]) == 5) {
	/*
	  reply.a_type = CSI;
	  reply.a_pintro = 0;
	  reply.a_nparam = 1;
	  reply.a_param[0] = 0;
	  reply.a_inters = 0;
	  reply.a_final  = 'n';
	  unparseseq(&reply, screen->respond);
	*/
      } else if (row == 6) {
	/*
	  reply.a_type = CSI;
	  reply.a_pintro = 0;
	  reply.a_nparam = 2;
	  reply.a_param[0] = screen->cur_row+1;
	  reply.a_param[1] = screen->cur_col+1;
	  reply.a_inters = 0;
	  reply.a_final  = 'R';
	  unparseseq(&reply, screen->respond);
	*/
      }
      parseState = groundtable;
      break;

    case CASE_HP_MEM_LOCK:
    case CASE_HP_MEM_UNLOCK:
      /*
	if(screen->scroll_amt)
	FlushScroll(screen);
      */
      if (parseState[c] == CASE_HP_MEM_LOCK)
	begScroll = yloc;
      else
	begScroll = 0;
      parseState = groundtable;
      break;
      
    case CASE_DECSTBM:
      // DECSTBM - set scrolling region
      if((top = param[0]) < 1)
	top = 1;
      if(nparam < 2 || (bot = param[1]) == DEFAULT
	 || bot > lines + 1
	 || bot == 0)
	bot = lines+1;
      if (bot > top) {
	/*
	  if(screen->scroll_amt)
	  FlushScroll(screen);
	*/
	begScroll = top-1;
	endScroll = bot-1;
	cursorSet(0, 0);
      }
      parseState = groundtable;
      break;
      
    case CASE_DECREQTPARM:
      // DECREQTPARM
      if ((row = param[0]) == DEFAULT)
	row = 0;
      if (row == 0 || row == 1) {
	/*
	  reply.a_type = CSI;
	  reply.a_pintro = 0;
	  reply.a_nparam = 7;
	  reply.a_param[0] = row + 2;
	  reply.a_param[1] = 1;	// no parity    
	  reply.a_param[2] = 1;	// eight bits    
	  reply.a_param[3] = 112;	// transmit 9600 baud    
	  reply.a_param[4] = 112;	// receive 9600 baud   
	  reply.a_param[5] = 1;	// clock multiplier ?   
	  reply.a_param[6] = 0;	// STP flags ?   
	  reply.a_inters = 0;
	  reply.a_final  = 'x';
	  unparseseq(&reply, screen->respond);
	*/
      }
      parseState = groundtable;
      break;

    case CASE_DECSET:
      // DECSET   
      /*      
	      dpmodes(term, bitset);
      */
      parseState = groundtable;
      break;
      
    case CASE_DECRST:
      // DECRST    
      /*
	dpmodes(term, bitclr);
      */
      parseState = groundtable;
      break;
      
    case CASE_DECALN:
      // DECALN    
      /*
	if(screen->cursor_state)
	HideCursor();
	for(row = lines ; row >= 0 ; row--) {
	bzero(screen->buf[4 * row + 1],
	col = screen->max_col + 1);
	for(cp = (unsigned char *)screen->buf[4 * row] ; col > 0 ; col--)
	*cp++ = (unsigned char) 'E';
	}
      */
      parseState = groundtable;
      break;
      
    case CASE_GSETS:
      /*
	screen->gsets[scstype] = c;
      */
      parseState = groundtable;
      break;
      
    case CASE_DECSC:
      // DECSC     
      saveX=xloc;
      saveY=yloc;
      parseState = groundtable;
      break;
      
    case CASE_DECRC:
      // DECRC   
      xloc=saveX;
      yloc=saveY;
      parseState = groundtable;
      break;
      
    case CASE_DECKPAM:
      // DECKPAM   
      /* turns on numlock?
	 term->keyboard.flags |= KYPD_APL;
	 update_appkeypad();
      */
      parseState = groundtable;
      break;
      
    case CASE_DECKPNM:
      // DECKPNM   
      /* turns off numlock?
	 term->keyboard.flags &= ~KYPD_APL;
	 update_appkeypad();
      */
      parseState = groundtable;
      break;
      
    case CASE_IND:
      // IND   
      /*
	Index(screen, 1);
	if (QLength(screen->display) ||
	GetBytesAvailable (ConnectionNumber(screen->display)) > 0)
	xevents();
      */
      parseState = groundtable;
      break;
      
    case CASE_NEL:
      // NEL   
      /*
	Index(screen, 1);
	CarriageReturn(screen);
      
	if (QLength(screen->display) ||
	GetBytesAvailable (ConnectionNumber(screen->display)) > 0)
	xevents();
      */
      parseState = groundtable;
      break;
      
    case CASE_HTS:
      // HTS 
      tabSet();
      parseState = groundtable;
      break;
      
    case CASE_RI:
      // RI 
      /*
	RevIndex(screen, 1);
      */
      parseState = groundtable;
      break;
      
    case CASE_SS2:
      // SS2 
      curss = 2;
      parseState = groundtable;
      break;
      
    case CASE_SS3:
      // SS3 
      curss = 3;
      parseState = groundtable;
      break;

    case CASE_CSI_STATE:
      // enter csi state 
      nparam = 1;
      param[0] = DEFAULT;
      parseState = csitable;
      break;
      
    case CASE_OSC:
      // Operating System Command: ESC ] 
      /*
	do_osc(finput);
      */
      parseState = groundtable;
      break;
      
    case CASE_RIS:
      // RIS 
      VTReset();
      parseState = groundtable;
      break;
      
    case CASE_LS2:
      // LS2 
      curgl = 2;
      parseState = groundtable;
      break;
      
    case CASE_LS3:
      // LS3 
      curgl = 3;
      parseState = groundtable;
      break;
      
    case CASE_LS3R:
      // LS3R 
      curgr = 3;
      parseState = groundtable;
      break;
      
    case CASE_LS2R:
      // LS2R 
      curgr = 2;
      parseState = groundtable;
      break;
      
    case CASE_LS1R:
      // LS1R 
      curgr = 1;
      parseState = groundtable;
      break;
      
    case CASE_XTERM_SAVE:
      /*
	savemodes(term);
      */
      parseState = groundtable;
      break;
      
    case CASE_XTERM_RESTORE:
      /*
	restoremodes(term);
      */
      parseState = groundtable;
      break;
    }
  }

  void rendition(int mode [], int count) {
    if (count<=0) {mode[0]=0;count=1;}
    for (int j=0;j<count;j++) {
      if (debug) {
	System.out.print("mode: ");
	System.out.print(mode[j]);
	System.out.print(" ");
	System.out.println(j);
      }
      if (mode[j] < 10) {
	switch (mode[j]) {
	case -1:
	case 0: fgcolor=Color.black;
	  bgcolor=Color.white;
	  break;
	case 1: //bold
	case 2: //faint
	case 3: //italic
	case 4: //underscore
	case 5: //blinking
	case 6: //rapid blinking
	  break;
	case 7:
          Color tmp = fgcolor;
	  fgcolor=bgcolor;
	  bgcolor=tmp;
	  break;
	case 8: fgcolor=bgcolor;
	}
      } else
	if (mode[j] < 40) {
	  switch (mode[j]) {
	  case 27:
	  case 30:
	  case 31:
	  case 32:
	  case 33:
	  case 34:
	  case 35:
	  case 36:
	  case 37:
	  }
	} else
	  switch (mode[j]) {
	  case 40:
	  case 41:
	  case 42:
	  case 43:
	  case 44:
	  case 45:
	  case 46:
	  case 47:
	  case 48://subscript
	  case 49://superscript
	  }
    }
  }

  void clearLines(int beg, int end) {
    for (int j=beg;j<=end;j++) {
      lineRedraw [j] = true;
      for (int i=0;i<columns;i++) {
        screen   [j][i] = ' ';
        screenfg [j][i] = fgcolor;
        screenbg [j][i] = bgcolor;
      }
    }
  }

  void clearLine(int beg, int end) {
    lineRedraw [yloc] = true;
    for (int j=beg;j<=end;j++) {
      screen[yloc][j]=' ';
      screenfg[yloc][j]=fgcolor;
      screenbg[yloc][j]=bgcolor;
    }
  }


  /* The remainder of this file "VT100Telnet.java" is covered under the X/MIT
   * License, which only requires that you maintain the following DEC notice
   * in all versions derived from this source.  I've also left in the CVS 
   * header for the file, which shows from where in the XFree86 distribution 
   * I obtained it. -- Daniel Kristjansson
   */

  /* begin "tables" */
  /*
   *	$XConsortium: VTPrsTbl.c,v 1.9 92/09/15 15:28:28 gildea Exp $
   */

  /*
   * Copyright 1987 by Digital Equipment Corporation, Maynard, Massachusetts.
   *
   *                         All Rights Reserved
   *
   * Permission to use, copy, modify, and distribute this software and its
   * documentation for any purpose and without fee is hereby granted,
   * provided that the above copyright notice appear in all copies and that
   * both that copyright notice and this permission notice appear in
   * supporting documentation, and that the name of Digital Equipment
   * Corporation not be used in advertising or publicity pertaining to
   * distribution of the software without specific, written prior permission.
   *
   *
   * DIGITAL DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
   * ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL
   * DIGITAL BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR
   * ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
   * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
   * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
   * SOFTWARE.
   */

  static int groundtable[] =
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_NUL,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	CAN		EM		SUB		ESC	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	SP		!		"		#	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	$		%		&		'	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	(		)		*		+	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	,		-		.		/	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	0		1		2		3	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	4		5		6		7	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	8		9		:		;	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	<		=		>		?	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT, 
    /*	@		A		B		C	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	D		E		F		G	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	H		I		J		K	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	L		M		N		O	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	P		Q		R		S	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	T		U		V		W	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	X		Y		Z		[	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	\		]		^		_	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	`		a		b		c	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	d		e		f		g	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	h		i		j		k	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	l		m		n		o	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	p		q		r		s	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	t		u		v		w	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	x		y		z		{	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*	|		}		~		DEL	*/
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x84            0x85            0x86            0x87    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x90            0x91            0x92            0x93    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x94            0x95            0x96            0x97    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      currency        yen             brokenbar       section         */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      notsign         hyphen          registered      macron          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      eth             ntilde          ograve          oacute          */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
    CASE_PRINT,
  };

  static int csitable[] =		/* ESC [ */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	$		%		&		'	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	(		)		*		+	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	,		-		.		/	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	0		1		2		3	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    /*	4		5		6		7	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    /*	8		9		:		;	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_IGNORE,
    CASE_ESC_SEMI,
    /*	<		=		>		?	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_DEC_STATE,
    /*	@		A		B		C	*/
    CASE_ICH,
    CASE_CUU,
    CASE_CUD,
    CASE_CUF,
    /*	D		E		F		G	*/
    CASE_CUB,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_CUP,
    CASE_GROUND_STATE,
    CASE_ED,
    CASE_EL,
    /*	L		M		N		O	*/
    CASE_IL,
    CASE_DL,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	P		Q		R		S	*/
    CASE_DCH,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_TRACK_MOUSE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_DA1,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_CUP,
    CASE_TBC,
    /*	h		i		j		k	*/
    CASE_SET,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_RST,
    CASE_SGR,
    CASE_CPR,
    CASE_GROUND_STATE,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_DECSTBM,
    CASE_DECSC,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_DECRC,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_DECREQTPARM,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int dectable[] =		/* ESC [ ? */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	$		%		&		'	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	(		)		*		+	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	,		-		.		/	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	0		1		2		3	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    /*	4		5		6		7	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    /*	8		9		:		;	*/
    CASE_ESC_DIGIT,
    CASE_ESC_DIGIT,
    CASE_IGNORE,
    CASE_ESC_SEMI,
    /*	<		=		>		?	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	@		A		B		C	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	D		E		F		G	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	L		M		N		O	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	P		Q		R		S	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	h		i		j		k	*/
    CASE_DECSET,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_DECRST,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_XTERM_RESTORE,
    CASE_XTERM_SAVE,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int eigtable[] =		/* CASE_ESC_IGNORE */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	$		%		&		'	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	(		)		*		+	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	,
	-		.		/	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	0		1		2		3	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	4		5		6		7	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	8		9		:		;	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	<		=		>		?	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	@		A		B		C	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	D		E		F		G	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	L		M		N		O	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	P		Q		R		S	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	h		i		j		k	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int esctable[] =		/* ESC */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_SCR_STATE,
    /*	$		%		&		'	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	(		)		*		+	*/
    CASE_SCS0_STATE,
    CASE_SCS1_STATE,
    CASE_SCS2_STATE,
    CASE_SCS3_STATE,
    /*	,		-		.		/	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	0		1		2		3	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	4		5		6		7	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_DECSC,
    /*	8		9		:		;	*/
    CASE_DECRC,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	<		=		>		?	*/
    CASE_GROUND_STATE,
    CASE_DECKPAM,
    CASE_DECKPNM,
    CASE_GROUND_STATE,
    /*	@		A		B		C	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	D		E		F		G	*/
    CASE_IND,
    CASE_NEL,
    CASE_HP_BUGGY_LL,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_HTS,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	L		M		N		O	*/
    CASE_GROUND_STATE,
    CASE_RI,
    CASE_SS2,
    CASE_SS3,
    /*	P		Q		R		S	*/
    CASE_IGNORE_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_XTERM_TITLE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_DECID,
    CASE_CSI_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_OSC,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_RIS,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	h		i		j		k	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_HP_MEM_LOCK,
    CASE_HP_MEM_UNLOCK,
    CASE_LS2,
    CASE_LS3,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_LS3R,
    CASE_LS2R,
    CASE_LS1R,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int iestable[] =		/* CASE_IGNORE_ESC */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	BS		HT		NL		VT	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	NP		CR		SO		SI	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	SP		!		"		#	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	$		%		&		'	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	(		)		*		+	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	,		-		.		/	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	0		1		2		3	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	4		5		6		7	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	8		9		:		;	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	<		=		>		?	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	@		A		B		C	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	D		E		F		G	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	H		I		J		K	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	L		M		N		O	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	P		Q		R		S	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	T		U		V		W	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	X		Y		Z		[	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	`		a		b		c	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	d		e		f		g	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	h		i		j		k	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	l		m		n		o	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	p		q		r		s	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	t		u		v		w	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	x		y		z		{	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*	|		}		~		DEL	*/
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    CASE_IGNORE_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int igntable[] =		/* CASE_IGNORE_STATE */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	BS		HT		NL		VT	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	NP		CR		SO		SI	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_GROUND_STATE, 
    CASE_IGNORE,
    CASE_GROUND_STATE,
    CASE_IGNORE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	$		%		&		'	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	(		)		*		+	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	,		-		.		/	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	0		1		2		3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	4		5		6		7	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	8		9		:		;	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	<		=		>		?	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	@		A		B		C	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	D		E		F		G	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	H		I		J		K	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	L		M		N		O	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	P		Q		R		S	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	T		U		V		W	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	X		Y		Z		[	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	\		]		^		_	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	`		a		b		c	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	d		e		f		g	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	h		i		j		k	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	l		m		n		o	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	p		q		r		s	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	t		u		v		w	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	x		y		z		{	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	|		}		~		DEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int scrtable[] =		/* ESC # */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	$		%		&		'	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	(		)		*		+	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	,		-		.		/	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	0		1		2		3	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	4		5		6		7	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	8		9		:		;	*/
    CASE_DECALN,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	<		=		>		?	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	@		A		B		C	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	D		E		F		G	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	L		M		N		O	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	P		Q		R		S	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	h		i		j		k	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };

  static int scstable[] =		/* ESC ( etc. */
  {
    /*	NUL		SOH		STX		ETX	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	EOT		ENQ		ACK		BEL	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_BELL,
    /*	BS		HT		NL		VT	*/
    CASE_BS,
    CASE_TAB,
    CASE_VMOT,
    CASE_VMOT,
    /*	NP		CR		SO		SI	*/
    CASE_VMOT,
    CASE_CR,
    CASE_SO,
    CASE_SI,
    /*	DLE		DC1		DC2		DC3	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	DC4		NAK		SYN		ETB	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	CAN		EM		SUB		ESC	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_ESC,
    /*	FS		GS		RS		US	*/
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*	SP		!		"		#	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	$		%		&		'	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	(		)		*		+	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	,		-		.		/	*/
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    CASE_ESC_IGNORE,
    /*	0		1		2		3	*/
    CASE_GSETS,
    CASE_GSETS,
    CASE_GSETS,
    CASE_GROUND_STATE,
    /*	4		5		6		7	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	8		9		:		;	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	<		=		>		?	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	@		A		B		C	*/
    CASE_GROUND_STATE,
    CASE_GSETS,
    CASE_GSETS,
    CASE_GROUND_STATE,
    /*	D		E		F		G	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	H		I		J		K	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	L		M		N		O	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	P		Q		R		S	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	T		U		V		W	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	X		Y		Z		[	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	\		]		^		_	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	`		a		b		c	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	d		e		f		g	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	h		i		j		k	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	l		m		n		o	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	p		q		r		s	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	t		u		v		w	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	x		y		z		{	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*	|		}		~		DEL	*/
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      0x80            0x81            0x82            0x83    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x84            0x85            0x86            0x87    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x88            0x89            0x8a            0x8b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x8c            0x8d            0x8e            0x8f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x90            0x91            0x92            0x93    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x94            0x95            0x96            0x97    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x99            0x99            0x9a            0x9b    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      0x9c            0x9d            0x9e            0x9f    */
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    CASE_IGNORE,
    /*      nobreakspace    exclamdown      cent            sterling        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      currency        yen             brokenbar       section         */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      diaeresis       copyright       ordfeminine     guillemotleft   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      notsign         hyphen          registered      macron          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      degree          plusminus       twosuperior     threesuperior   */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      acute           mu              paragraph       periodcentered  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      cedilla         onesuperior     masculine       guillemotright  */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      onequarter      onehalf         threequarters   questiondown    */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Agrave          Aacute          Acircumflex     Atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Adiaeresis      Aring           AE              Ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Eth             Ntilde          Ograve          Oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      Udiaeresis      Yacute          Thorn           ssharp          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      agrave          aacute          acircumflex     atilde          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      adiaeresis      aring           ae              ccedilla        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      egrave          eacute          ecircumflex     ediaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      igrave          iacute          icircumflex     idiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      eth             ntilde          ograve          oacute          */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      ocircumflex     otilde          odiaeresis      division        */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      oslash          ugrave          uacute          ucircumflex     */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    /*      udiaeresis      yacute          thorn           ydiaeresis      */
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
    CASE_GROUND_STATE,
  };
  /* end "tables" */
}
