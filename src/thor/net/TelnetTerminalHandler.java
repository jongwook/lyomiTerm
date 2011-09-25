package thor.net;
import java.io.*;
/** The TelnetTerminalHandler is the best way to communicate
 * with the low levels that implement the Telnet protocol.
 * <p>
 * Whenever the host on the other end asks for an option to
 * be turned on a getWantedOption will be called, you can decide
 * then whether to turn on an option.
 * <p>
 * You must implement getOption and setOption honestly.
 * <p>
 * LineFeed, CarriageReturn, BackSpace, Null, FormFeed,
 * Bell, VerticalTab, and HorizontalTab are called
 * whenever those command are in the Telnet stream.
 * They will not pass through as characters in the
 * InputStream.
 *  <p>
 *  <i>The <a href=http://www.gnu.org/copyleft/lgpl.html>LGPL</a>
 *  applies to this software.<br>
 *  Unless otherwise stated the software is
 *  Copyright 1996,2000 Daniel Kristjansson</i>
 */

public interface TelnetTerminalHandler {
  /** Line Feed  */
  public void LineFeed();
  /** CarriageReturn */
  public void CarriageReturn();
  /** BackSpace */
  public void BackSpace();
  /** Null */
  public void Null();
  /** Form Feed */
  public void FormFeed();
  /** Clears Screen */
  public void ClearScreen();
  /** Rings Bell */
  public void Bell();
  /** Vertical Tab */
  public void VerticalTab();
  /** Horizontal Tab -- normal tab */
  public void HorizontalTab();
  /** Handles Other Commands -- SB, SE, DM, BRK, IP, AO, AYT, EC, EL, GA */
  public void OtherCommands(int command);
  /** Tells asker if option is on */
  public boolean getOption(int pnIndex);
  /** Turns option on */
  public void setOption(int pnIndex, boolean pbValue);
  /** Remove option from the list of desired options */
  public void removeWantedOption(int pnIndex);
  /** Remove option from the list of options no longer desired */
  public void removeNotWantedOption(int pnIndex);
  /** Tells asker if option is requested */
  public boolean isWantedOption(int pnIndex);
  /** Tells asker if option is requested to be turned off */
  public boolean isNotWantedOption(int pnIndex);
  /** Returns list of options desired */
  public int[] getWantedOptions();
  /** Returns list of options no longer desired */
  public int[] getNotWantedOptions();
}

