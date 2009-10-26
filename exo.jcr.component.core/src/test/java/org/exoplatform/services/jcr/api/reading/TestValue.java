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
package org.exoplatform.services.jcr.api.reading;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.value.BinaryValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestValue.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestValue extends JcrAPIBaseTest
{

   public void testGetString() throws RepositoryException
   {
      Value value = session.getValueFactory().createValue("text");
      assertEquals("text", value.getString());
   }

   public void testGetDouble() throws RepositoryException
   {
      Value value = session.getValueFactory().createValue("text");
      try
      {
         value.getDouble();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      value = session.getValueFactory().createValue("20");
      assertEquals(20, (int)value.getDouble());

      try
      {
         value.getStream();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }
   }

   public void testGetLong() throws RepositoryException
   {
      Value value = session.getValueFactory().createValue("text");
      try
      {
         value.getDouble();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      value = session.getValueFactory().createValue("15");
      assertEquals(15, value.getLong());

      try
      {
         value.getStream();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }
   }

   public void testGetStream() throws RepositoryException, IOException
   {
      Value value = new BinaryValue(new String("inputStream"));
      InputStream iS = value.getStream();
      int aval = iS.available();
      byte[] bytes = new byte[iS.available()];
      iS.read(bytes);
      assertEquals("inputStream", new String(bytes));

      // assertEquals(aval, value.getStream().available());
      assertEquals(aval, bytes.length);

      value = session.getValueFactory().createValue("text");
      iS = value.getStream();
      bytes = new byte[2];
      iS.read(bytes);
      assertEquals("te", new String(bytes));
      // Once a Value object has been read once using getStream(),
      // all subsequent calls to getStream() will return the same stream object.
      iS = value.getStream();
      bytes = new byte[2];
      iS.read(bytes);
      assertEquals("xt", new String(bytes));

   }

   public void testGetDate() throws RepositoryException
   {
      Value value = session.getValueFactory().createValue("text");
      try
      {
         value.getDate();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      Calendar calendar = new GregorianCalendar();
      value = session.getValueFactory().createValue(calendar);
      assertEquals(calendar, value.getDate());

      try
      {
         value.getStream();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }
   }

   public void testGetDateFromString() throws RepositoryException
   {
      // try set value as string with ISO date
      Value value = session.getValueFactory().createValue("2007-07-04T14:24:03.123+01:00");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date (Zulu TZ - i.e. GMT/UTC)
      value = session.getValueFactory().createValue("1985-07-04T14:24:03.123Z");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date + RFC 822 TZ
      value = session.getValueFactory().createValue("2007-07-04T14:24:03.123-0800");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("2007-07-04T14:24:03+01:00");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date (Zulu TZ - i.e. GMT/UTC)
      value = session.getValueFactory().createValue("2007-07-04T14:24:03Z");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date + RFC 822 TZ
      value = session.getValueFactory().createValue("2007-07-04T14:24:03+0200");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with simple date
      value = session.getValueFactory().createValue("2007-07-04T14:24:03");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("2007-07-04T14:24+03:00");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date + RFC 822 TZ
      value = session.getValueFactory().createValue("2007-07-04T14:24-0700");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("2007-07-04T14:24");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("2007-07-04");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("1988-11");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue(2017);
      try
      {
         value.getDate(); // value.getDate().getTime()
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // ERA tests

      // try set value as string with ISO date
      value = session.getValueFactory().createValue("-0104-07-04T14:24:03.123+04:00");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date
      value = session.getValueFactory().createValue(-117);
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      // try set value as string with ISO date + RFC 822 TZ
      value = session.getValueFactory().createValue("-0027-07-04T14:24:03-0400");
      try
      {
         value.getDate();
      }
      catch (ValueFormatException e)
      {
         fail("Exception should not have been thrown");
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }
   }

   public void testGetBoolean() throws RepositoryException
   {
      Value value = valueFactory.createValue(10l);
      try
      {
         value.getBoolean();
         fail("exception should have been thrown");
      }
      catch (ValueFormatException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }

      value = session.getValueFactory().createValue(true);
      assertTrue(value.getBoolean());

      try
      {
         value.getStream();
         fail("exception should have been thrown");
      }
      catch (IllegalStateException e)
      {
      }
      catch (RepositoryException e)
      {
         fail("not good exception thrown");
      }
   }

   public void tesGetType() throws Exception
   {
      Value value = valueFactory.createValue("");
      assertEquals(PropertyType.STRING, value.getType());
      value = valueFactory.createValue(10l);
      assertEquals(PropertyType.LONG, value.getType());
      value = valueFactory.createValue(10.0);
      assertEquals(PropertyType.DOUBLE, value.getType());
      value = valueFactory.createValue(true);
      assertEquals(PropertyType.BOOLEAN, value.getType());
      value = valueFactory.createValue(new GregorianCalendar());
      assertEquals(PropertyType.DATE, value.getType());
      value = valueFactory.createValue("", PropertyType.BINARY);
      assertEquals(PropertyType.BINARY, value.getType());
      value = valueFactory.createValue("uuid", PropertyType.REFERENCE);
      assertEquals(PropertyType.REFERENCE, value.getType());
      value = valueFactory.createValue("jcr:content", PropertyType.NAME);
      assertEquals(PropertyType.NAME, value.getType());
      value = valueFactory.createValue("/content", PropertyType.PATH);
      assertEquals(PropertyType.PATH, value.getType());

   }

   public void testEquals() throws Exception
   {

      assertTrue(valueFactory.createValue("test").equals(valueFactory.createValue("test")));
      assertFalse(valueFactory.createValue("2").equals(valueFactory.createValue(2l)));
   }

}
