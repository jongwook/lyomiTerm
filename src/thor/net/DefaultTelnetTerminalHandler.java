package thor.net;
import java.io.*;
import java.awt.*;
import java.net.*;
/**  Basic implementation of TelnetTerminalHandler,
 *  many telnet clients can extand this and
 *  just implement the NOP's.
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */
public class DefaultTelnetTerminalHandler implements TelnetTerminalHandler {
  private boolean iabOptions[] = new boolean[256];
  private int ianWantedOptions[] = new int[256];
    // positive for wanted, negative, for not wanted, zero if blank

  /** NOP */
  public void LineFeed() {}
  /** NOP */
  public void CarriageReturn() {}
  /** NOP */
  public void BackSpace() {}
  /** NOP */
  public void Null() {}
  /** NOP */
  public void FormFeed() {}
  /** NOP */
  public void ClearScreen() {}
  /** uses default toolkit beep */
  public void Bell() {
    Toolkit.getDefaultToolkit().beep();
  }
  /** NOP */
  public void VerticalTab() {}
  /** NOP */
  public void HorizontalTab() {}
  /** NOP */
  public void OtherCommands(int command) {}
  public boolean getOption(int pnIndex) { return iabOptions[pnIndex]; }
  public void setOption(int pnIndex, boolean pbValue) {
    iabOptions[pnIndex]=pbValue;
    removeWantedOption(pnIndex);
    removeNotWantedOption(pnIndex);
  }
  /** Add option to the list of desired options */
  public void addWantedOption(int pnIndex) {
    int i;
    if (!getOption(pnIndex)) {
      for (i=0; i<256; i++) {
	if (ianWantedOptions[i]==pnIndex || ianWantedOptions[i]==-pnIndex) {
	  ianWantedOptions[i]=pnIndex;
	  i=-1;
	  break;
	}
      }
      if (i!=-1)
	for (i=0; i<256; i++) {
          if (ianWantedOptions[i]==0) {
	    ianWantedOptions[i]=pnIndex;
	    break;
          }
	}
    }
  }
  public boolean isWantedOption(int pnIndex) {
    for (int i=0; i<256; i++) {
      if (ianWantedOptions[i]==pnIndex) return true;
    }
    return false;
  }
  public boolean isNotWantedOption(int pnIndex) {
    for (int i=0; i<256; i++) {
      if (ianWantedOptions[i]==-pnIndex) return true;
    }
    return false;
  }
  public void addNotWantedOption(int pnIndex) {
    int i;
    if (getOption(pnIndex)) {
      for (i=0; i<256; i++) {
	if (ianWantedOptions[i]==pnIndex || ianWantedOptions[i]==-pnIndex) {
	  ianWantedOptions[i]=-pnIndex;
	  i=-1;
	  break;
	}
      }
      if (i!=-1)
        for (i=0; i<256; i++) {
          if (ianWantedOptions[i]==0) {
	    ianWantedOptions[i]=-pnIndex;
	    break;
          }
	}
    }
  }
  public void removeWantedOption(int pnIndex) {
    for (int i=0; i<256; i++) {
      if (ianWantedOptions[i]==pnIndex) {
	ianWantedOptions[i]=0;
	return;
      }
    }
    return;
  }
  public void removeNotWantedOption(int pnIndex) {
    removeWantedOption(-pnIndex);
  }
  public int[] getWantedOptions() {
    int size=0;
    for (int i=0; i<256; i++)
      if (ianWantedOptions[i]>0) size++;
    int[] list = new int[size];
    int cur=0;
    for (int i=0; i<256; i++) 
      if (ianWantedOptions[i]>0) {
        list[cur]=ianWantedOptions[i];
	cur++;
      }
    return list;
  }
  public int[] getNotWantedOptions() {
    int size=0;
    for (int i=0; i<256; i++)
      if (ianWantedOptions[i]<0) size++;
    int[] list = new int[size];
    int cur=0;
    for (int i=0; i<256; i++) 
      if (ianWantedOptions[i]<0) {
        list[cur]=-ianWantedOptions[i];
	cur++;
      }
    return list;
  }
}
