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
package org.exoplatform.services.jcr.api.i18n;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: TestI18nValues.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestI18nValues extends JcrAPIBaseTest
{

   static protected String TEST_I18N = "I18N Node";

   static public String TEST_I18N_PROP = "testProp";

   // Cyrillic, 'Hello world' + some chars
   static public String TEST_I18N_CONTENT_CYR =
      "\u041f\u0440\u0438\u0432\u0457\u0442\u0490\u044a\u0020\u043c\u0438\u0440\u0462";

   // Hebrew, 'Hello world'
   static public String TEST_I18N_CONTENT_IL = "\u05e9\u05dc\u05d5\u05dd\u0020\u05e2\u05d5\u05dc\u05dd";

   // European funny characters
   static public String TEST_I18N_CONTENT_LAT = "H\u0158l\u1e37o \u1e84\u00F6r\u013b\u01fc";

   // Some Arabic characters (characters mix, it's not a word)
   static public String TEST_I18N_CONTENT_ARAB = "Hello w\u0680r\u0628\u069d";

   // Some Japan characters (Katakana, Hiragana mix, it's not a word)
   static public String TEST_I18N_CONTENT_JP = "H\u30a9l\u30ddo w\u3060r\u308b\u304a";

   // Some China characters (Bopomofo, Bopomofo ext. mix, it's not a word)
   static public String TEST_I18N_CONTENT_CH = "H\u3115l\u312bo w\u3108r\u31a9\u31ae";

   // Some African characters (Ethiopic chars, it's not a word)
   static public String TEST_I18N_CONTENT_AFR = "Hello w\u1233r\u127b\u1383";

   // Some Indic characters (Bengali, Kannada, Devanagara mix, it's not a word)
   static public String TEST_I18N_CONTENT_INDIA = "H\u09a3l\u09edo \u0cb2\u0c8br\u0911\u090c";

   protected Node testNode = null;

   public void setUp() throws Exception
   {

      super.setUp();

      testNode = session.getRootNode().addNode(TEST_I18N);
      session.save();
   }

   private boolean equalsBinary(byte[] target, byte[] source)
   {

      if (target.length != source.length)
         return false;

      for (int i = 0; i < source.length; i++)
      {
         if (source[i] != target[i])
            return false;
      }
      return true;
   }

   public void setString(String source) throws RepositoryException
   {

      if (log.isDebugEnabled())
         log.debug("String as string value: '" + source + "'");

      try
      {
         testNode.setProperty(TEST_I18N_PROP, source);
         testNode.save();
      }
      catch (RepositoryException e)
      {
         fail("Error create proeprty '" + TEST_I18N_PROP + "': " + e);
      }

      Session session1 = repository.login(credentials, WORKSPACE);
      Node test = session1.getRootNode().getNode(TEST_I18N);

      try
      {
         Property prop = test.getProperty(TEST_I18N_PROP);
         assertEquals("Content must be identical", prop.getString(), source);
      }
      catch (RepositoryException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "': " + e);
      }
   }

   /**
    * Tests property values with I18n content stored in BINARY values using
    * Node.setProperty(String, String, int) method
    */
   public void setBinaryAsString(String source) throws RepositoryException
   {

      if (log.isDebugEnabled())
         log.debug("Binnary string as string value: '" + source + "'");

      try
      {
         testNode.setProperty(TEST_I18N_PROP, source, PropertyType.BINARY);
         testNode.save();
      }
      catch (RepositoryException e)
      {
         fail("Error create proeprty '" + TEST_I18N_PROP + "': " + e);
      }

      Session session1 = repository.login(credentials, WORKSPACE);
      Node test = session1.getRootNode().getNode(TEST_I18N);

      try
      {
         Property prop = test.getProperty(TEST_I18N_PROP);
         assertEquals("Content must be identical", source, prop.getString());
      }
      catch (RepositoryException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "': " + e);
      }

      try
      {
         Property prop = test.getProperty(TEST_I18N_PROP);
         InputStream is = prop.getStream();
         byte[] buf = new byte[1024];
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         while (is.available() > 0)
         {
            int rec = is.read(buf);
            baos.write(buf, 0, rec);
         }
         byte[] valueBytes = baos.toByteArray();
         String content = new String(valueBytes, "UTF-8");
         assertTrue("Content must be identical. '" + source + "' = '" + content + "'", equalsBinary(valueBytes, source
            .getBytes("UTF-8")));
         assertEquals("Content must be identical", source, content);
      }
      catch (RepositoryException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "': " + e);
      }
      catch (IOException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "' value stream: " + e);
      }
   }

   /**
    * Tests property values with I18n content stored in BINARY values using
    * Node.setProperty(String, InputStream) method
    */
   public void setBinaryAsStream(String source) throws RepositoryException
   {

      System.out.println("Binnary string as stream value: '" + source + "'");

      try
      {
         testNode.setProperty(TEST_I18N_PROP, new ByteArrayInputStream(source.getBytes("UTF-8")));
         testNode.save();
      }
      catch (RepositoryException e)
      {
         fail("Error create property '" + TEST_I18N_PROP + "': " + e);
      }
      catch (UnsupportedEncodingException e)
      {
         fail("Error encode string: " + e);
      }

      Session session1 = repository.login(credentials, WORKSPACE);
      Node test = session1.getRootNode().getNode(TEST_I18N);

      try
      {
         Property prop = test.getProperty(TEST_I18N_PROP);
         assertEquals("Content must be identical", source, prop.getString());
      }
      catch (RepositoryException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "': " + e);
      }

      try
      {
         Property prop = test.getProperty(TEST_I18N_PROP);
         InputStream is = prop.getStream();
         byte[] buf = new byte[1024];
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         while (is.available() > 0)
         {
            int rec = is.read(buf);
            baos.write(buf, 0, rec);
         }
         byte[] valueBytes = baos.toByteArray();
         String content = new String(valueBytes, "UTF-8");
         assertTrue("Content must be identical. '" + source + "' = '" + content + "'", equalsBinary(valueBytes, source
            .getBytes("UTF-8")));
         assertEquals("Content must be identical", source, content);
      }
      catch (RepositoryException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "': " + e);
      }
      catch (IOException e)
      {
         fail("Error read property '" + TEST_I18N_PROP + "' value stream: " + e);
      }
   }

   // ------ Cyrillic ------
   public void testSetStringCYR() throws RepositoryException
   {

      if (log.isDebugEnabled())
         log.debug("Testing Cyrillic STRING as STRING");
      setString(TEST_I18N_CONTENT_CYR);
   }

   public void testSetBinaryAsStringCYR() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Cyrillic BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_CYR);
   }

   public void testSetBinaryAsStreamCYR() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Cyrillic BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_CYR);
   }

   // ------ Latin ------
   public void testSetStringLAT() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Latin STRING as STRING");
      setString(TEST_I18N_CONTENT_LAT);
   }

   public void testSetBinaryAsStringLAT() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Latin BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_LAT);
   }

   public void testSetBinaryAsStreamLAT() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Latin BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_LAT);
   }

   // ------ Japan ------
   public void testSetStringJP() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Japan STRING as STRING");
      setString(TEST_I18N_CONTENT_JP);
   }

   public void testSetBinaryAsStringJP() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Japan BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_JP);
   }

   public void testSetBinaryAsStreamJP() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Japan BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_JP);
   }

   // ------ Chines ------
   public void testSetStringCH() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Chines STRING as STRING");
      setString(TEST_I18N_CONTENT_CH);
   }

   public void testSetBinaryAsStringCH() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Chines BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_CH);
   }

   public void testSetBinaryAsStreamCH() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Chines BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_CH);
   }

   // ------ Arabic ------
   public void testSetStringARAB() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Arabic STRING as STRING");
      setString(TEST_I18N_CONTENT_ARAB);
   }

   public void testSetBinaryAsStringARAB() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Arabic BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_ARAB);
   }

   public void testSetBinaryAsStreamARAB() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Arabic BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_ARAB);
   }

   // ------ Hebrew ------
   public void testSetStringIL() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Hebrew STRING as STRING");
      setString(TEST_I18N_CONTENT_IL);
   }

   public void testSetBinaryAsStringIL() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Hebrew BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_IL);
   }

   public void testSetBinaryAsStreamIL() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Hebrew BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_IL);
   }

   // ------ Indian ------
   public void testSetStringINDIA() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Indian STRING as STRING");
      setString(TEST_I18N_CONTENT_INDIA);
   }

   public void testSetBinaryAsStringINDIA() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Indian BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_INDIA);
   }

   public void testSetBinaryAsStreamINDIA() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing Indian BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_INDIA);
   }

   // ------ African ------
   public void testSetStringAFR() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing African STRING as STRING");
      setString(TEST_I18N_CONTENT_AFR);
   }

   public void testSetBinaryAsStringAFR() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing African BINARY string as STRING");
      setBinaryAsString(TEST_I18N_CONTENT_AFR);
   }

   public void testSetBinaryAsStreamAFR() throws RepositoryException
   {
      if (log.isDebugEnabled())
         log.debug("Testing African BINARY string as STREAM");
      setBinaryAsStream(TEST_I18N_CONTENT_AFR);
   }

   // ************* path
   public void testPath() throws RepositoryException
   {
      testNode.addNode(TEST_I18N_CONTENT_CYR).setProperty(TEST_I18N_CONTENT_LAT, "aaa");

      testNode.save();

      Session s1 = repository.login(credentials);
      Node tr = (Node)s1.getItem(testNode.getPath());

      try
      {
         Node iNode = tr.getNode(TEST_I18N_CONTENT_CYR);
         // log.info(iNode.getPath()); // wrong out in eclipse due to fonts
         Property iProp = iNode.getProperty(TEST_I18N_CONTENT_LAT);
         // log.info(iProp.getPath());
      }
      catch (RepositoryException e)
      {
         fail(e.getMessage());
      }

      s1.logout();
   }

}
