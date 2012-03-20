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
package org.exoplatform.services.jcr.impl.core;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.datamodel.QPathEntry;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Anatoliy Bazko
 */
public class TestLocationFactory extends TestCase
{

   private static int MAX_CREATE_PATH_TIME = 100000;

   private static String testJCRPathElementValid[][] =
      {{"jcr:name", "jcr", "name"}, {"jcr:name[30]", "jcr", "name"}, {"na me[1]", "", "na me"}, {"...", "", "..."},
         {"123", "", "123"}};

   private static String testJCRPathElementInvalid[] =
      {" na m e", "name[0]", " name[9]", "\n", "ddr:df:", "", "xml:na*me", "[1]", " ", "ddd:..", "&io:lala"};

   private static String testJCRPathValid[] =
      {"..", "jcr:ig[2]/aaa", "v/d/...", "/path", "/vv/fff", "ff", "/", "..", "|fff"};

   private static String testJCRPathInvalid[] = {"/.:./uuu", "/ ", "/./xml:name[0]", "xxx//fff", "//", " sdfas/", ""};

   private LocationFactory factory;

   private NamespaceRegistryImpl namespaceRegistry;

   @Override
   public void setUp() throws Exception
   {

      if (factory == null)
      {
         namespaceRegistry = new NamespaceRegistryImpl();
         factory = new LocationFactory(namespaceRegistry);
      }
   }

   /**
    * Test method for
    * {@link org.exoplatform.services.jcr.impl.core.LocationFactory#parseJCRPath(java.lang.String)} .
    */
   public void testParseValidJCRPath()
   {
      String testPath;
      for (String element : testJCRPathValid)
      {
         testPath = element;
         try
         {
            JCRPath path = factory.parseJCRPath(testPath);
            assertTrue(testPath.equals(path.getAsString(false)));
         }
         catch (RepositoryException e)
         {
            fail("exception should not have been thrown");
         }
      }
   }

   public void testParseInvalidJCRPath()
   {
      String testPath;
      for (String element : testJCRPathInvalid)
      {
         testPath = element;
         try
         {
            factory.parseJCRPath(testPath);
            fail("exception should have been thrown");
         }
         catch (RepositoryException e)
         {
         }
      }
   }

   /**
    * Test method for
    * {@link org.exoplatform.services.jcr.impl.core.LocationFactory#parseJCRName(java.lang.String)} .
    */
   public void testParseValidJCRName()
   {
      String testPathElement;
      String testPrefix;
      String testName;

      for (String[] element : testJCRPathElementValid)
      {
         testPathElement = element[0];
         testPrefix = element[1];
         testName = element[2];
         try
         {
            JCRName name = factory.parseJCRName(testPathElement);
            assertTrue(testPrefix.equals(name.getPrefix()));
            assertTrue(testName.equals(name.getName()));
         }
         catch (RepositoryException e)
         {
            fail("exception should not have been thrown");
         }
      }
   }

   public void testParseInvalidJCRName()
   {
      String testPathElement;

      for (String element : testJCRPathElementInvalid)
      {
         testPathElement = element;
         try
         {
            factory.parseJCRName(testPathElement);
            fail("exception should have been thrown");
         }
         catch (RepositoryException e)
         {
         }
      }
   }

   public void testCreateLongJCRPath() throws RepositoryException
   {
      for (int i = 1; i <= MAX_CREATE_PATH_TIME; i++)
      {
         factory
            .parseAbsPath("/jcr:namenamename/jcr:namenamename/jcr:namenamename/jcr:namenamename/jcr:namenamename/jcr:namenamename");
      }
   }

   public void testCreateMiddleJCRPath() throws RepositoryException
   {
      for (int i = 1; i <= MAX_CREATE_PATH_TIME; i++)
      {
         factory.parseAbsPath("/jcr:namename/jcr:namenamename/jcr:namenamename");
      }
   }

   public void testCreateShortJCRPath() throws RepositoryException
   {
      for (int i = 1; i <= MAX_CREATE_PATH_TIME; i++)
      {
         factory.parseAbsPath("/jcr:namenamename");
      }
   }

   public void testFormatPathElement() throws RepositoryException
   {
      assertEquals("test", factory.formatPathElement(new QPathEntry("", "test", 0)));
   }

   public void testParsePathEntryWhenParsNameIsNull()
   {
      try
      {
         factory.parseJCRName(null);
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

   public void testIsNotLocalName()
   {
      try
      {
         factory.parseJCRName("");
         fail();
      }
      catch (RepositoryException e)
      {
      }

      try
      {
         factory.parseJCRName(" ");
         fail();
      }
      catch (RepositoryException e)
      {
      }
   }

}