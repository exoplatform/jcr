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

import java.io.CharArrayWriter;
import java.util.BitSet;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
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
      CharArrayWriter out = new CharArrayWriter(string.length());
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
               throw new IllegalArgumentException(e);
            }
            i += 2;
         }
         else
         {
            out.write(c);
         }
      }
      return new String(out.toCharArray());
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
      return relativizePath(path, true);
   }

   /**
    * Creates relative path from string.
    * 
    * @param path path
    * @param withIndex indicates whether we should keep the index or not
    * @return relative path 
    */
   public static String relativizePath(String path, boolean withIndex)
   {

      if (path.startsWith("/"))
         path = path.substring(1);
      if (!withIndex && path.endsWith("]"))
      {
         int index = path.lastIndexOf('[');
         return index == -1 ? path : path.substring(0, index);
      }
      return path;
   }

   /**
    * Removes the index from the path if it has an index defined
    */
   public static String removeIndexFromPath(String path)
   {
      if (path.endsWith("]"))
      {
         int index = path.lastIndexOf('[');
         if (index != -1)
         {
            return path.substring(0, index);
         }
      }
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
    * @param path 
    *          path
    * @return parentPath
    */
   public static String parentPath(final String path)
   {
      int index = path.lastIndexOf("/");
      if (index == -1)
      {
         throw new IllegalArgumentException("Invalid path, it must contain at least one '/'");
      }
      String curPath = path.substring(0, index);
      if (curPath.length() == 0)
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
      int index = path.lastIndexOf('/');
      String name = index == -1 ? path : path.substring(index + 1);
      if (name.endsWith("]"))
      {
         index = name.lastIndexOf('[');
         return index == -1 ? name : name.substring(0, index);
      }
      return name;
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
    * @return extension or emtpy string if file has no extension
    */
   public static String getExtension(String filename)
   {
      int index = filename.lastIndexOf('.');

      if (index >= 0)
      {
         return filename.substring(index + 1);
      }

      return "";
   }
}
