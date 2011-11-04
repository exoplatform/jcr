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
package org.exoplatform.services.jcr.util;

/**
 * Created by The eXo Platform SAS
 * 
 * 31.08.2006
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: StringNumberParser.java 34445 2009-07-24 07:51:18Z dkatayev $
 */
public class StringNumberParser
{

   /**
    * Parse given text as long. <br/>
    * 
    * <br/>E.g. '2k' will be returned as 2048 number.
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * @param numberText
    * @return
    * @throws NumberFormatException
    */
   static public long parseLong(final String longText) throws NumberFormatException
   {
      return parseNumber(longText).longValue();
   }

   /**
    * Serialize given long as text. <br/>
    * 
    * <br/>E.g. 2048 long will be returned as "2kb".
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * @param longValue
    *        - long
    * @return String
    *        long representation as text 
    * @throws NumberFormatException
    */
   static public String serializeLong(final long longValue) throws NumberFormatException
   {
      return serializeNumber(longValue);
   }

   /**
    * Parse given text as int. <br/>
    * 
    * <br/>E.g. '2k' will be returned as 2048 number.
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * @param numberText
    * @return
    * @throws NumberFormatException
    */
   static public int parseInt(final String integerText) throws NumberFormatException
   {
      return parseNumber(integerText).intValue();
   }

   /**
    * Serialize given int as text. <br/>
    * 
    * <br/>E.g. 2048 nubber will be returned as "2kb".
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * @param integerValue
    *        - int
    * @return String
    *        integer representation as text 
    * @throws NumberFormatException
    */
   static public String serializeInt(final int integerValue) throws NumberFormatException
   {
      return serializeNumber(integerValue);
   }

   /**
    * Parse given text as double. <br/>
    * 
    * <br/>E.g. '2k' will be returned as 2048 number.
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * <br/>NOTE: floating point supported, e.g. 1.5m = 1.5 * 1024 * 1024
    * 
    * @param doubleText
    * @return
    * @throws NumberFormatException
    */
   static public double parseDouble(final String doubleText) throws NumberFormatException
   {
      return parseNumber(doubleText).doubleValue();
   }

   /**
    * Parse given text as number representation. <br/>
    * 
    * <br/>E.g. '2k' will be returned as 2048 number.
    * 
    * <br/>Next formats are supported (case insensitive): <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * <br/>NOTE: floating point supported, e.g. 1.5m = 1.5 * 1024 * 1024, <br/>WARN: floating point
    * delimiter depends on OS settings
    * 
    * @param numberText
    * @return
    * @throws NumberFormatException
    */
   static public Number parseNumber(final String numberText) throws NumberFormatException
   {
      final String text = numberText.toLowerCase().toUpperCase();
      if (text.endsWith("K"))
      {
         return Double.valueOf(text.substring(0, text.length() - 1)) * 1024d;
      }
      else if (text.endsWith("KB"))
      {
         return Double.valueOf(text.substring(0, text.length() - 2)) * 1024d;
      }
      else if (text.endsWith("M"))
      {
         return Double.valueOf(text.substring(0, text.length() - 1)) * 1048576d; // 1024 * 1024
      }
      else if (text.endsWith("MB"))
      {
         return Double.valueOf(text.substring(0, text.length() - 2)) * 1048576d; // 1024 * 1024
      }
      else if (text.endsWith("G"))
      {
         return Double.valueOf(text.substring(0, text.length() - 1)) * 1073741824d; // 1024 * 1024 *
         // 1024
      }
      else if (text.endsWith("GB"))
      {
         return Double.valueOf(text.substring(0, text.length() - 2)) * 1073741824d; // 1024 * 1024 *
         // 1024
      }
      else if (text.endsWith("T"))
      {
         return Double.valueOf(text.substring(0, text.length() - 1)) * 1099511627776d; // 1024 * 1024 *
         // 1024 * 1024
      }
      else if (text.endsWith("TB"))
      {
         return Double.valueOf(text.substring(0, text.length() - 2)) * 1099511627776d; // 1024 * 1024 *
         // 1024 * 1024
      }
      else
      {
         return Double.valueOf(text);
      }
   }

   /**
    * Serialize given number to text as number representation. <br/>
    * 
    * <br/>E.g. 2048 number will be returned as 2kb.
    * 
    * <br/>Next formats are supported: <br/>kilobytes - k,kb <br/>megabytes - m,mb
    * <br/>gigabytes - g,gb <br/>terabytes - t,tb
    * 
    * @param number
    *         - long
    * @return String
    *         - number representation
    * @throws NumberFormatException
    */
   static public String serializeNumber(final long number) throws NumberFormatException
   {
      if ((number >= 1099511627776l) && (number % 1099511627776l) == 0)
      {
         return String.valueOf(number / 1099511627776l) + "TB";
      }
      else if ((number >= 1073741824l) && (number % 1073741824l) == 0)
      {
         return String.valueOf(number / 1073741824l) + "GB";
      }
      else if ((number >= 1048576l) && (number % 1048576l) == 0)
      {
         return String.valueOf(number / 1048576l) + "MB";
      }
      
      else if ((number >= 1024l) && (number % 1024l) == 0)
      {
         return String.valueOf(number / 1024l) + "KB";
      }
      else
      {
         return String.valueOf(number);
      }
   }

   /**
    * Parse given text as formated time and return a time in milliseconds. <br/> <br/>Formats
    * supported: <br/>milliseconds - ms <br/>seconds - without sufix <br/>minutes - m <br/>hours - h
    * <br/>days - d <br/>weeks - w
    * 
    * <br/>TODO handle strings like 2d+4h, 2h+30m+15s+500 etc.
    * 
    * @param timeText
    *          - String
    * @return time in milliseconds
    * @throws NumberFormatException
    */
   static public long parseTime(final String text) throws NumberFormatException
   {
      if (text.endsWith("ms"))
      {
         return Long.valueOf(text.substring(0, text.length() - 2));
      }
      else if (text.endsWith("s"))
      {
         return Long.valueOf(text) * 1000;
      }
      else if (text.endsWith("m"))
      {
         return Long.valueOf(text.substring(0, text.length() - 1)) * 60000; // 1000 * 60
      }
      else if (text.endsWith("h"))
      {
         return Long.valueOf(text.substring(0, text.length() - 1)) * 3600000; // 1000 * 60 * 60
      }
      else if (text.endsWith("d"))
      {
         return Long.valueOf(text.substring(0, text.length() - 1)) * 86400000; // 1000 * 60 * 60 * 24
      }
      else if (text.endsWith("w"))
      {
         return Long.valueOf(text.substring(0, text.length() - 1)) * 604800000; // 1000 * 60 * 60 * 24
         // * 7
      }
      else
      { // seconds by default
         return Long.valueOf(text) * 1000;
      }
   }

   /**
    * Serialize given time in milliseconds and return a time as formated time in ms, s, h,d,w.
    * Formats : ms - milliseconds, s - seconds, h hours, w - weeks. 
    * 
    * @param millisecondTime
    *          - time in milliseconds
    * @return String
    *          - time as formated time in milliseconds
    * @throws NumberFormatException
    */
   static public String serializeTime(final long millisecondTime) throws NumberFormatException
   {
      if (millisecondTime >= 604800000 && (millisecondTime % 604800000) == 0)
      {
         return String.valueOf(millisecondTime / 604800000) + "w";
      }
      else if (millisecondTime >= 86400000 && (millisecondTime % 86400000) == 0)
      {
         return String.valueOf(millisecondTime / 86400000) + "d";
      }
      else if (millisecondTime >= 3600000 && (millisecondTime % 3600000) == 0)
      {
         return String.valueOf(millisecondTime / 3600000) + "h";
      }
      else if (millisecondTime >= 60000 && (millisecondTime % 60000) == 0)
      {
         return String.valueOf(millisecondTime / 60000) + "m";
      }
      else if (millisecondTime >= 1000 && (millisecondTime % 1000) == 0)
      {
         return String.valueOf(millisecondTime / 1000) + "s";
      }

      return String.valueOf(millisecondTime) + "ms";
   }
}
