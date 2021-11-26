/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.util;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 15.08.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: UnicodeFormatter.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class UnicodeFormatter
{

   static public String byteToHex(byte b)
   {
      // Returns hex String representation of byte b
      char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
      char[] array = {hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f]};
      return new String(array);
   }

   static public String charToHex(char c)
   {
      // Returns hex String representation of char c
      byte hi = (byte)(c >>> 8);
      byte lo = (byte)(c & 0xff);
      return byteToHex(hi) + byteToHex(lo);
   }

   static public String charToUString(char c)
   {
      // Returns unicode String representation of char c - like \\uXXXX
return "\\u" + charToHex(c);
   }
}
