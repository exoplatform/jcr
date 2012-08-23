/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.webdav.util;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

/**
 * Created by The eXo Platform SARL Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: PutCommand.java 12004 2007-01-17 12:03:57Z geaz $
 */

public class TextUtil
{

   /**
    * Unescapes string using escape symbol.
    * 
    * @param string string
    * @param escape escape symbol
    * @return unescaped string
    */
   public static String unescape(String string, char escape)
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream(string.length());
      for (int i = 0; i < string.length(); i++)
      {
         char c = string.charAt(i);
         if (c == escape)
         {
            try
            {
               out.write(Integer.parseInt(string.substring(i + 1, i + 3), 16));
            }
            catch (NumberFormatException e)
            {
               throw new IllegalArgumentException();
            }
            i += 2;
         }
         else
         {
            out.write(c);
         }
      }

      try
      {
         return new String(out.toByteArray(), "utf-8");
      }
      catch (Exception exc)
      {
         throw new InternalError(exc.toString());
      }
   }

   public static BitSet URISave;

   public static BitSet URISaveEx;

   static
   {
      URISave = new BitSet(256);
      int i;
      for (i = 'a'; i <= 'z'; i++)
      {
         URISave.set(i);
      }
      for (i = 'A'; i <= 'Z'; i++)
      {
         URISave.set(i);
      }
      for (i = '0'; i <= '9'; i++)
      {
         URISave.set(i);
      }
      URISave.set('-');
      URISave.set('_');
      URISave.set('.');
      URISave.set('!');
      URISave.set('~');
      URISave.set('*');
      URISave.set('\'');
      URISave.set('(');
      URISave.set(')');
      URISave.set(':');

      URISave.set('?');
      URISave.set('=');

      URISaveEx = (BitSet)URISave.clone();
      URISaveEx.set('/');
   }

   /**
    * Hexademical characters.
    */
   public static final char[] hexTable = "0123456789abcdef".toCharArray();

   /**
    * Escapes string using escape symbol.
    * 
    * @param string string
    * @param escape escape symbol
    * @param isPath if the string is path
    * 
    * @return escaped string
    */
   public static String escape(String string, char escape, boolean isPath)
   {
      try
      {
         BitSet validChars = isPath ? URISaveEx : URISave;
         byte[] bytes = string.getBytes("utf-8");
         StringBuffer out = new StringBuffer(bytes.length);
         for (int i = 0; i < bytes.length; i++)
         {
            int c = bytes[i] & 0xff;
            if (validChars.get(c) && c != escape)
            {
               out.append((char)c);
            }
            else
            {
               out.append(escape);
               out.append(hexTable[(c >> 4) & 0x0f]);
               out.append(hexTable[(c) & 0x0f]);
            }
         }
         return out.toString();
      }
      catch (Exception exc)
      {
         throw new InternalError(exc.toString());
      }
   }

   /**
    * Creates relative path from string.
    * 
    * @param path path
    * @return relative path 
    */
   public static String relativizePath(String path)
   {

      if (path.startsWith("/"))
         return path.substring(1);
      return path;
   }

   /**
    * Cuts the path from string.
    * 
    * @param path full path 
    * @return relative path.
    */
   public static String pathOnly(String path)
   {
      String curPath = path;
      curPath = curPath.substring(curPath.indexOf("/"));
      curPath = curPath.substring(0, curPath.lastIndexOf("/"));
      if ("".equals(curPath))
      {
         curPath = "/";
      }
      return curPath;
   }

   /**
    * Cuts the current name from the path.
    * 
    * @param path path
    * @return current name
    */
   public static String nameOnly(String path)
   {
      String[] curNames = path.split("/");
      return curNames[curNames.length - 1];
   }

   /**
    * Checks if Mime-Type is media mime type.
    *  
    * @param mimeType Mime-Type
    * @return true if is media false if not.
    */
   public static boolean isMediaFile(String mimeType)
   {
      return mimeType.contains("image");
   }
   
   /**
    * Extracts the extension of the file.
    *  
    * @param filename file name
    * @return extension or emtpy String if file has no extension
    */
   public static String getExtension(String filename)
   {
      if (filename.contains("."))
      {
         return filename.substring(filename.lastIndexOf(".") + 1);
      }

      return "";
   }

}
