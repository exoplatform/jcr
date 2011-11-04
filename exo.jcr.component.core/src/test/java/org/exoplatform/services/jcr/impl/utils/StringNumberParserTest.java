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
package org.exoplatform.services.jcr.impl.utils;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.util.StringNumberParser;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 18.06.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: StringNumberParserTest.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class StringNumberParserTest extends TestCase
{

   public void testParseInt()
   {
      assertEquals(1000, StringNumberParser.parseInt("1000"));

      assertEquals(1024, StringNumberParser.parseInt("1K"));

      assertEquals(5 * 1024, StringNumberParser.parseInt("5K"));

      assertEquals(127 * 1024 * 1024, StringNumberParser.parseInt("127M"));

      assertEquals(1 * 1024 * 1024 * 1024, StringNumberParser.parseInt("1g"));
   }

   public void testSerializeInt()
   {
      assertEquals("1000", StringNumberParser.serializeInt(1000));

      assertEquals("1KB", StringNumberParser.serializeInt(1024));

      assertEquals("5KB", StringNumberParser.serializeInt(5 * 1024));

      assertEquals("127MB", StringNumberParser.serializeInt(127 * 1024 * 1024));

      assertEquals("1GB", StringNumberParser.serializeInt(1 * 1024 * 1024 * 1024));
   }

   public void testParseLong()
   {
      assertEquals(1000l, StringNumberParser.parseLong("1000"));

      assertEquals(1024l, StringNumberParser.parseLong("1K"));

      assertEquals(5l * 1024, StringNumberParser.parseLong("5K"));

      assertEquals(127l * 1024 * 1024, StringNumberParser.parseLong("127M"));

      assertEquals(4l * 1024 * 1024 * 1024, StringNumberParser.parseLong("4g"));

      assertEquals(5l * 1024 * 1024 * 1024 * 1024, StringNumberParser.parseLong("5TB"));
   }

   public void testSerializeLong()
   {
      assertEquals("1000", StringNumberParser.serializeLong(1000l));

      assertEquals("1KB", StringNumberParser.serializeLong(1024l));

      assertEquals("5KB", StringNumberParser.serializeLong(5l * 1024));

      assertEquals("127MB", StringNumberParser.serializeLong(127l * 1024 * 1024));

      assertEquals("4GB", StringNumberParser.serializeLong(4l * 1024 * 1024 * 1024));

      assertEquals("5TB", StringNumberParser.serializeLong(5l * 1024 * 1024 * 1024 * 1024));
   }

   public void testParseNumber()
   {
      assertEquals(10.27d, StringNumberParser.parseNumber("10.27").doubleValue());

      assertEquals(233.4 * 1024 * 1024, StringNumberParser.parseNumber("233.4m").doubleValue());
   }

   public void testParseTime()
   {
      assertEquals(63l * 1000, StringNumberParser.parseTime("63"));

      assertEquals(2l * 60 * 1000, StringNumberParser.parseTime("2m"));

      assertEquals(15l * 60 * 60 * 1000, StringNumberParser.parseTime("15h"));

      assertEquals(3l * 24 * 60 * 60 * 1000, StringNumberParser.parseTime("3d"));

      assertEquals(5l * 7 * 24 * 60 * 60 * 1000, StringNumberParser.parseTime("5w"));

      assertEquals(12l, StringNumberParser.parseTime("12ms"));
   }

   public void testSerialiseTime()
   {
      assertEquals("63s", StringNumberParser.serializeTime(63l * 1000));

      assertEquals("2m", StringNumberParser.serializeTime(2l * 60 * 1000));

      assertEquals("15h", StringNumberParser.serializeTime(15l * 60 * 60 * 1000));

      assertEquals("3d", StringNumberParser.serializeTime(3l * 24 * 60 * 60 * 1000));

      assertEquals("5w", StringNumberParser.serializeTime(5l * 7 * 24 * 60 * 60 * 1000));

      assertEquals("12ms", StringNumberParser.serializeTime(12l));
   }

}
