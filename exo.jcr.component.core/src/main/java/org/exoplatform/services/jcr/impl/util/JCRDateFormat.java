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
package org.exoplatform.services.jcr.impl.util;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.Tools;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.text.ParseException;
import java.util.Calendar;

import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 23.08.2006
 * 
 * ISO 8601
 * 
 * Year: YYYY (eg 1997) Year and month: YYYY-MM (eg 1997-07) Complete date: YYYY-MM-DD (eg
 * 1997-07-16) Complete date plus hours and minutes: YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
 * Complete date plus hours, minutes and seconds: YYYY-MM-DDThh:mm:ssTZD (eg
 * 1997-07-16T19:20:30+01:00) Complete date plus hours, minutes, seconds and a decimal fraction of a
 * second YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
 * 
 * where:
 * 
 * YYYY = four-digit year MM = two-digit month (01=January, etc.) DD = two-digit day of month (01
 * through 31) hh = two digits of hour (00 through 23) (am/pm NOT allowed) mm = two digits of minute
 * (00 through 59) ss = two digits of second (00 through 59) s = one or more digits representing a
 * decimal fraction of a second TZD = time zone designator (Z or +hh:mm or -hh:mm) a RFC 822 time
 * zone is also accepted: For formatting, the RFC 822 4-digit time zone format is used:
 * RFC822TimeZone: Sign TwoDigitHours Minutes TwoDigitHours: Digit Digit like -8000
 * 
 * It's a pb found. If we will set property with date contains timezone different to the current.
 * And will get property as string after that. We will have a date with the current timezone,
 * actualy the date will be same but in different tz.
 * 
 * "2023-07-05T19:28:00.000-0300" --> "2023-07-06T01:28:00.000+0300" - it's same date, but... print
 * is different.
 * 
 * The pb can be solved in SimpleDateFormat be setting the formatter timezone before the format
 * procedure.
 * 
 * TimeZone tz = TimeZone.getTimeZone("GMT-03:00"); Calendar cdate = Calendar.getInstance();
 * SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); sdf.setTimeZone(tz);
 * Date d = sdf.parse(javaDate); log.info("parse " + sdf.format(d)); // print date in GMT-03:00
 * timezone
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: JCRDateFormat.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class JCRDateFormat
{

   public static Log log = ExoLogger.getLogger("exo.jcr.component.core.JCRDateFormat");

   /**
    * ISO 8601, RFC822 formats for JCR datas deserialization in order of priority of parse
    */
   protected static final String[] JCR_FORMATS =
      {ISO8601.COMPLETE_DATETIMEMSZ_FORMAT, ISO8601.COMPLETE_DATETIMEMSZRFC822_FORMAT,};

   protected static final String CALENDAR_FIELDS_DELIMITER = ";"; // hope

   // it
   // 's
   // unique
   // for
   // any
   // time
   // zone
   // ID
   // etc
   // .

   protected static final String CALENDAR_FIELDS_SEPARATOR = "--";

   /**
    * Format date using complete date plus hours, minutes, seconds and a decimal fraction of a second
    * format. I.e. format to JCR date value string representation.
    * 
    * @param date
    * @return
    */
   public static String format(Calendar date)
   {
      return ISO8601.format(date);
   }

   /**
    * Parse string using possible formats list.
    * 
    * @param dateString
    *          - date string
    * @return - calendar
    * @throws ValueFormatException
    */
   public static Calendar parse(String dateString) throws ValueFormatException
   {
      try
      {
         return ISO8601.parseEx(dateString);
      }
      catch (ParseException e)
      {
         throw new ValueFormatException("Can not parse date from [" + dateString + "]", e);
      }
      catch (NumberFormatException e)
      {
         throw new ValueFormatException("Can not parse date from [" + dateString + "]", e);
      }
   }

   /**
    * Deserialize string (of JCR Value) to the date.
    * 
    * @param serString
    * @return
    * @throws ValueFormatException
    */
   public Calendar deserialize(String serString) throws ValueFormatException
   {
      final String[] parts = serString.split(CALENDAR_FIELDS_SEPARATOR);
      if (parts.length == 2)
      {

         // try parse serialized string with two formats
         // 1. Complete ISO 8610 compliant
         // 2. Complete ISO 8610 + RFC822 (time zone) compliant (JCR 1.6 and prior)
         Calendar isoCalendar = null;
         try
         {
            isoCalendar = ISO8601.parse(parts[0], JCR_FORMATS);

            String[] calendarFields = parts[1].split(CALENDAR_FIELDS_DELIMITER);
            if (calendarFields.length == 4)
            {
               try
               {
                  isoCalendar.setLenient(Boolean.parseBoolean(calendarFields[0]));
                  isoCalendar.setFirstDayOfWeek(Integer.parseInt(calendarFields[1]));
                  isoCalendar.setMinimalDaysInFirstWeek(Integer.parseInt(calendarFields[2]));

                  isoCalendar.setTimeZone(Tools.getTimeZone(calendarFields[3]));
               }
               catch (Exception e)
               {
                  log.warn("Can't parse serialized fields for the calendar [" + parts[1] + "] but calendar has ["
                     + isoCalendar.getTime() + "]", e);
               }
            }
            else
            {
               log.warn("Fields of the calendar is not serialized properly [" + parts[1] + "] but calendar has ["
                  + isoCalendar.getTime() + "]");
            }
         }
         catch (ParseException e)
         {
            throw new ValueFormatException(e);
         }

         return isoCalendar;
      }
      throw new ValueFormatException("Can't deserialize calendar string [" + serString + "]");
   }

   /**
    * Serialize date (of JCR Value) to the string.
    * 
    * @param date
    * @return
    */
   public byte[] serialize(Calendar date)
   {
      final String calendarString =
         CALENDAR_FIELDS_SEPARATOR + date.isLenient() + CALENDAR_FIELDS_DELIMITER + date.getFirstDayOfWeek()
            + CALENDAR_FIELDS_DELIMITER + date.getMinimalDaysInFirstWeek() + CALENDAR_FIELDS_DELIMITER
            + date.getTimeZone().getID();

      return (format(date) + calendarString).getBytes();
   }
}
