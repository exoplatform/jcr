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
package org.exoplatform.services.jcr.ext.replication.recovery;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: FileNameFactory.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class FileNameFactory
{
   /**
    * The date format to name of file.
    */
   private DateFormat datefName = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

   /**
    * Definition the max sub folders.
    */
   private static final int SUB_PATH_LENGTH = 7;

   /**
    * Definition the constant for 10.
    */
   private static final int PATTERN_10 = 10;

   /**
    * Definition the constant for 100.
    */
   private static final int PATTERN_100 = 100;

   /**
    * Definition the constant for 1000.
    */
   private static final int PATTERN_1000 = 1000;

   /**
    * Definition the chars of path.
    */
   private final String pathCharSequence = "0123456789abcdef";

   /**
    * getStrDate.
    * 
    * @param c
    *          the date
    * @return String the string of date
    */
   public String getStrDate(Calendar c)
   {
      // Returns as a String (YYYYMMDD) a Calendar date
      int m = c.get(Calendar.MONTH) + 1;
      int d = c.get(Calendar.DATE);
      return "" + c.get(Calendar.YEAR) + (m < PATTERN_10 ? "0" + m : m) + (d < PATTERN_10 ? "0" + d : d);
   }

   /**
    * getStrTime.
    * 
    * @param c
    *          the date
    * @return String the string of time
    */
   public String getStrTime(Calendar c)
   {
      // Returns as a String (YYYYMMDD_MS) a Calendar date
      int h = c.get(Calendar.HOUR_OF_DAY);
      int m = c.get(Calendar.MINUTE);
      int s = c.get(Calendar.SECOND);
      int ms = c.get(Calendar.MILLISECOND);

      return "" + (h < PATTERN_10 ? "0" + h : h) + (m < PATTERN_10 ? "0" + m : m) + (s < PATTERN_10 ? "0" + s : s)
         + "_" + (ms < PATTERN_100 ? (ms < PATTERN_10 ? "00" + ms : "0" + ms) : ms);
   }

   /**
    * getTimeStampName.
    * 
    * @param c
    *          the date
    * @return String return the date and time as string
    */
   public String getTimeStampName(Calendar c)
   {
      return (getStrDate(c) + "_" + getStrTime(c));
   }

   /**
    * getRandomSubPath.
    * 
    * @return String return the sub path as string
    */
   public String getRandomSubPath()
   {
      StringBuilder subPath = new StringBuilder();

      for (int i = 0; i < SUB_PATH_LENGTH; i++)
      {
         int index = (int)(Math.random() * PATTERN_1000) % pathCharSequence.length();

         if (i != SUB_PATH_LENGTH - 1)
         {
            subPath.append(pathCharSequence.charAt(index)).append(File.separator);
         }
         else
         {
            subPath.append(pathCharSequence.charAt(index));
         }
      }

      return subPath.toString();
   }

   /**
    * getDateFromFileName.
    * 
    * @param fName
    *          the name of file
    * @return Calendar return the date from name of file
    * @throws ParseException
    *           will be generated the ParseException
    */
   public Calendar getDateFromFileName(String fName) throws ParseException
   {
      Calendar c = Calendar.getInstance();

      String[] sArray = fName.split("_");

      Date d = datefName.parse(sArray[0] + "_" + sArray[1] + "_" + sArray[2]);
      c.setTime(d);

      return c;
   }
}
