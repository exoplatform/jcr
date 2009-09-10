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
package org.exoplatform.services.jcr.impl.value;

import org.exoplatform.services.jcr.JcrImplBaseTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS 22.01.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestDateValueFormat.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestDateValueFormat extends JcrImplBaseTest
{

   private Node testRoot = null;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      testRoot = session.getRootNode().addNode("dateformathelper_test");
      session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      testRoot.remove();
      session.save();

      super.tearDown();
   }

   public void testTestDateValue() throws Exception
   {

      Calendar calendar = Calendar.getInstance();

      Node dateParent = testRoot.addNode("date node");
      dateParent.setProperty("calendar", calendar);

      assertEquals("Calendars must be equals", calendar, dateParent.getProperty("calendar").getDate());

      testRoot.save();

      assertEquals("Calendars must be equals", calendar, dateParent.getProperty("calendar").getDate());
   }

   /**
    * It's a pb found. If we will set property with date contains timezone different to the current.
    * And will get property as string after that. We will have a date with the current timezone,
    * actualy the date will be same but in different tz. "2023-07-05T19:28:00.000-0300" -->
    * "2023-07-06T01:28:00.000+0300" - it's same date, but... print is different. The pb can be
    * solved ib SimpleDateFormat be setting the formatter timezone before the format procedure.
    * TimeZone tz = TimeZone.getTimeZone("GMT-03:00"); Calendar cdate = Calendar.getInstance();
    * SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); sdf.setTimeZone(tz);
    * Date d = sdf.parse(javaDate); log.info("parse " + sdf.format(d)); // print date in GMT-03:00
    * timezone
    * 
    * @throws Exception
    */
   public void testTestStringDateValue() throws Exception
   {
      final String date = "2023-07-05T19:28:00.000-03:00"; // ISO8601, JCR
      // supported
      final String javaDate = "2023-07-05T19:28:00.000-0300"; // ISO8601 + RFC822,
      // jvm supported

      Node dateParent = testRoot.addNode("date node");
      dateParent.setProperty("calendar", date, PropertyType.DATE);

      // TimeZone tz = TimeZone.getTimeZone("GMT-03:00");
      Calendar cdate = Calendar.getInstance();

      // Calendar cdate = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
      // sdf.setTimeZone(tz);

      Date d = sdf.parse(javaDate);
      // log.info("parse " + sdf.format(d));
      // cdate.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));
      cdate.setTime(d);
      // log.info("calendar " + sdf.format(cdate.getTime()));

      // assertEquals("Dates must be equals", date,
      // dateParent.getProperty("calendar").getString());
      assertEquals("Dates must be equals", cdate, dateParent.getProperty("calendar").getDate());

      testRoot.save();

      assertEquals("Dates must be equals", cdate, dateParent.getProperty("calendar").getDate());
   }

   public void testArabicProblem() throws ParseException
   {
      SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
      System.out.println(format.parse("21-02-2008"));
   }
}
